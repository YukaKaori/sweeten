package com.itheima.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * RBAC 角色/权限查询 Mapper
 */
@Mapper
public interface SysRoleMapper {

    /**
     * 查询用户绑定的全部角色码 (登录时写入 JWT claims)
     */
    @Select("select r.code from sys_user_role ur join sys_role r on ur.role_id = r.id where ur.user_id = #{userId}")
    List<String> selectRoleCodesByUserId(Integer userId);

    /**
     * 查询单个角色拥有的全部权限码 (PermissionAspect 鉴权用, 结果进内存缓存)
     */
    @Select("select p.code from sys_role r " +
            "join sys_role_permission rp on rp.role_id = r.id " +
            "join sys_permission p on p.id = rp.permission_id " +
            "where r.code = #{roleCode}")
    List<String> selectPermissionCodesByRoleCode(String roleCode);

    /**
     * 根据角色码查角色ID
     */
    @Select("select id from sys_role where code = #{roleCode}")
    Integer selectRoleIdByCode(String roleCode);

    /**
     * 给用户绑定角色 (新增员工时绑定默认角色)
     */
    @Insert("insert into sys_user_role(user_id, role_id) values (#{userId}, #{roleId})")
    void insertUserRole(Integer userId, Integer roleId);

    /**
     * 删除用户的全部角色绑定 (删除员工时清理)
     */
    @Delete("<script>delete from sys_user_role where user_id in " +
            "<foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    void deleteUserRolesByUserIds(List<Integer> userIds);
}
