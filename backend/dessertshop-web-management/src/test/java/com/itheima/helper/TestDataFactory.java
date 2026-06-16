package com.itheima.helper;

import com.itheima.pojo.*;
import java.time.LocalDateTime;

/**
 * 测试数据工厂
 */
public class TestDataFactory {

    public static Dessert createDessert() {
        Dessert d = new Dessert();
        d.setName("测试甜品-" + System.currentTimeMillis());
        d.setCategory("cake");
        d.setPrice(48.00);
        d.setOriginalPrice(58.00);
        d.setSales(0);
        d.setStock(100);
        d.setStatus(1);
        d.setCreateTime(LocalDateTime.now());
        d.setUpdateTime(LocalDateTime.now());
        return d;
    }

    public static Dessert createDessert(String name, String category, double price) {
        Dessert d = new Dessert();
        d.setName(name);
        d.setCategory(category);
        d.setPrice(price);
        d.setOriginalPrice(price + 10);
        d.setSales(0);
        d.setStock(50);
        d.setStatus(1);
        d.setCreateTime(LocalDateTime.now());
        d.setUpdateTime(LocalDateTime.now());
        return d;
    }

    public static Order createOrder() {
        Order o = new Order();
        o.setOrderNo("DS" + System.currentTimeMillis());
        o.setCustomerName("测试用户");
        o.setPhone("13800000001");
        o.setItems("测试甜品 x1");
        o.setAmount(48.00);
        o.setStatus(1);
        o.setAddress("北京市测试区");
        o.setCreateTime(LocalDateTime.now());
        o.setUpdateTime(LocalDateTime.now());
        return o;
    }

    public static Order createOrder(String customerName, double amount, Integer status) {
        Order o = new Order();
        o.setOrderNo("DS" + System.currentTimeMillis() + ((int)(Math.random() * 1000)));
        o.setCustomerName(customerName);
        o.setPhone("13900000001");
        o.setItems("测试商品 x1");
        o.setAmount(amount);
        o.setStatus(status);
        o.setAddress("测试地址");
        o.setCreateTime(LocalDateTime.now());
        o.setUpdateTime(LocalDateTime.now());
        return o;
    }

    public static Emp createEmp() {
        Emp e = new Emp();
        e.setUsername("testuser-" + System.currentTimeMillis());
        e.setName("测试员工");
        e.setPassword("123456");
        e.setGender(1);
        e.setPhone("13800000000");
        e.setJob(1);
        e.setSalary(5000);
        e.setDeptId(1);
        e.setEntryDate(LocalDateTime.now().toLocalDate());
        return e;
    }

    public static OrderQueryParam createOrderQueryParam() {
        OrderQueryParam param = new OrderQueryParam();
        param.setPage(1);
        param.setPageSize(10);
        return param;
    }

    public static DessertQueryParam createDessertQueryParam() {
        DessertQueryParam param = new DessertQueryParam();
        param.setPage(1);
        param.setPageSize(10);
        return param;
    }
}
