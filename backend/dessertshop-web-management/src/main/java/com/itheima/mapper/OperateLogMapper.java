package com.itheima.mapper;

import com.itheima.pojo.OperateLog;
import com.itheima.pojo.OperateLogQueryParam;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OperateLogMapper {

    //插入日志数据
    @Insert("insert into operate_log (operate_emp_id, operate_time, class_name, method_name, method_params, return_value, cost_time) " +
            "values (#{operateEmpId}, #{operateTime}, #{className}, #{methodName}, #{methodParams}, #{returnValue}, #{costTime});")
    public void insert(OperateLog log);

    //分页查询操作日志
    List<OperateLog> list(OperateLogQueryParam queryParam);

    //根据ID查询操作日志详情
    @Select("select * from operate_log where id = #{id}")
    OperateLog getById(Integer id);
}
