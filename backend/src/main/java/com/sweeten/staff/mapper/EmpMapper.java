package com.sweeten.staff.mapper;

import com.sweeten.staff.model.Emp;
import com.sweeten.staff.model.EmpQueryParam;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 员工档案。账号凭证（按用户名查账号、改密码）见 identity 模块的 AccountMapper；
 * 员工统计报表见 analytics 模块的 AnalyticsMapper。
 */
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

    /**
     * 根据ID查询员工详情（包含部门名称）
     */
    @Select("select e.*, d.name as deptName from emp e left join dept d on e.dept_id = d.id where e.id = #{id}")
    Emp selectById(Integer id);
}
