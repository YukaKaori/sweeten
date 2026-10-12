package com.itheima.context;

import com.itheima.mapper.EmpMapper;
import com.itheima.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按用户 ID 读取"当前"角色（数据库为准），短时缓存。
 * <p>
 * JWT 有效期 12 小时，若只信任 token 里的 roles：员工被删除后旧 token 仍可继续访问、
 * 角色降级要等重新登录才生效。TokenFilter 每次请求经此校验账号仍存在并取最新角色，
 * 最长延迟为缓存时长（删除员工时本实例立即失效）。
 */
@Component
@RequiredArgsConstructor
public class UserRoleLoader {

    static final long TTL_MILLIS = 30_000;

    private final EmpMapper empMapper;
    private final SysRoleMapper sysRoleMapper;

    private record Entry(Optional<List<String>> roles, long expiresAt) {
    }

    private final ConcurrentHashMap<Integer, Entry> cache = new ConcurrentHashMap<>();

    /**
     * @return 账号不存在时为 Optional.empty()；存在则为其当前角色（可能为空列表）
     */
    public Optional<List<String>> currentRoles(Integer userId) {
        if (userId == null) {
            return Optional.empty();
        }
        long now = System.currentTimeMillis();
        Entry entry = cache.get(userId);
        if (entry != null && entry.expiresAt() > now) {
            return entry.roles();
        }
        Optional<List<String>> roles = empMapper.countById(userId) > 0
                ? Optional.of(List.copyOf(sysRoleMapper.selectRoleCodesByUserId(userId)))
                : Optional.empty();
        cache.put(userId, new Entry(roles, now + TTL_MILLIS));
        return roles;
    }

    /** 员工被删除或角色变更后立即失效 */
    public void evict(Collection<Integer> userIds) {
        if (userIds != null) {
            userIds.forEach(cache::remove);
        }
    }
}
