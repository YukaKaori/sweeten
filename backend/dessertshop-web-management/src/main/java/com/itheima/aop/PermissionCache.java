package com.itheima.aop;

import com.itheima.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 角色→权限码 内存缓存
 * <p>
 * 按角色缓存权限码集合, TTL 5 分钟, 过期后下次访问时重新从数据库加载。
 * 调整角色权限 (改 sys_role_permission) 最迟 5 分钟生效; 需要立即生效可重启或调 evictAll。
 */
@Component
@RequiredArgsConstructor
public class PermissionCache {

    private static final long TTL_MILLIS = 5 * 60 * 1000;

    private final SysRoleMapper sysRoleMapper;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * 获取角色的权限码集合 (缓存未命中或过期时查库)
     */
    public Set<String> getPermissions(String roleCode) {
        CacheEntry entry = cache.get(roleCode);
        if (entry == null || entry.isExpired()) {
            Set<String> permissions = new HashSet<>(sysRoleMapper.selectPermissionCodesByRoleCode(roleCode));
            entry = new CacheEntry(permissions, System.currentTimeMillis() + TTL_MILLIS);
            cache.put(roleCode, entry);
        }
        return entry.permissions();
    }

    public void evictAll() {
        cache.clear();
    }

    private record CacheEntry(Set<String> permissions, long expireAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }
}
