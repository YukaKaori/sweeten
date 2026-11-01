package com.sweeten.identity.mapper;

import com.sweeten.identity.model.StaffAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 员工账号凭证（emp 表的认证相关列）。员工档案的增删改仍由 staff 模块的 EmpMapper 负责。
 */
@Mapper
public interface AccountMapper {

    /** 登录：按用户名取账号（含密码哈希，仅供服务端校验） */
    @Select("select id, username, password, name, image from emp where username = #{username}")
    StaffAccount findByUsername(String username);

    @Select("select id, username, password, name, image from emp where id = #{id}")
    StaffAccount findById(Integer id);

    /** 账号是否仍存在（TokenFilter 每次请求以数据库为准校验，见 UserRoleLoader） */
    @Select("select count(*) from emp where id = #{id}")
    int countById(Integer id);

    /** 全部账号（仅密码格式迁移使用） */
    @Select("select id, username, password from emp")
    List<StaffAccount> listAll();

    @Update("update emp set password = #{passwordHash}, update_time = now() where id = #{id}")
    int updatePassword(@Param("id") Integer id, @Param("passwordHash") String passwordHash);
}
