package com.itheima.mapper;

import com.itheima.pojo.Emp;
import com.itheima.pojo.EmpQueryParam;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface EmpMapper {

    List<Emp> list(EmpQueryParam empQueryParam);

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("insert into emp(username, password, name, gender, phone, job, salary, image, entry_date, dept_id, create_time, update_time)" +
            "values (#{username}, #{password}, #{name}, #{gender}, #{phone}, #{job}, #{salary}, #{image}, #{entryDate}, #{deptId}, #{createTime}, #{updateTime})")
    void insert(Emp emp);

    void deleteByIds(List<Integer> ids);

    Emp getById(Integer id);

    void updateById(Emp emp);

    @MapKey("pos")
    List<Map<String, Object>> countEmpJobData();

    @MapKey("name")
    List<Map> countEmpGenderData();

    /**
     * 根据用户名查询员工（用于登录验证）
     */
    @Select("select * from emp where username = #{username}")
    Emp getByUsername(String username);

    /**
     * 根据ID查询员工详情（包含部门名称）
     */
    @Select("select e.*, d.name as deptName from emp e left join dept d on e.dept_id = d.id where e.id = #{id}")
    Emp selectById(Integer id);

    /**
     * 更新密码
     */
    @Update("update emp set password = #{newPassword}, update_time = now() where id = #{id}")
    int updatePassword(@Param("id") Integer id, @Param("newPassword") String newPassword);
}
