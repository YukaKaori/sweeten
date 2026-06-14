package com.itheima.service;

import com.itheima.pojo.Customer;
import com.itheima.pojo.CustomerQueryParam;
import com.itheima.pojo.PageResult;
import com.itheima.pojo.Order;

import java.util.List;

public interface CustomerService {

    PageResult<Customer> page(CustomerQueryParam queryParam);

    Customer getById(Integer id);

    void save(Customer customer);

    void update(Customer customer);

    void deleteById(Integer id);

    List<Order> getCustomerOrders(Integer customerId);
}
