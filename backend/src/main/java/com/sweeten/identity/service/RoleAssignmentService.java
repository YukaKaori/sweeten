package com.sweeten.identity.service;

import java.util.List;

/**
 * 员工角色绑定（RBAC 写操作的唯一入口）。
 * <p>
 * staff 模块新增 / 删除员工时调用；默认角色策略集中在这里，灰度退出（改为按职位映射角色）时只需改这一处。
 */
public interface RoleAssignmentService {

    /** 为新员工绑定默认角色 */
    void assignDefaultRole(Integer userId);

    /** 删除员工时解除全部角色绑定，并让这些账号已签发的令牌立即失效 */
    void revokeAll(List<Integer> userIds);
}
