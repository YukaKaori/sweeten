package com.sweeten.customer.mapper;

import com.sweeten.customer.model.Customer;
import com.sweeten.customer.model.CustomerQueryParam;
import com.sweeten.order.model.Order;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CustomerMapper {

    List<Customer> list(CustomerQueryParam queryParam);

    @Select("select * from customers where id = #{id}")
    Customer getById(Integer id);

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("insert into customers(name, phone, gender, address, remark, create_time, update_time) " +
            "values(#{name}, #{phone}, #{gender}, #{address}, #{remark}, #{createTime}, #{updateTime})")
    void insert(Customer customer);

    void updateById(Customer customer);

    @Delete("delete from customers where id = #{id}")
    void deleteById(Integer id);

    @Select("select * from orders where customer_name = #{customerName} order by create_time desc")
    List<Order> selectOrdersByCustomerName(@Param("customerName") String customerName);
}
