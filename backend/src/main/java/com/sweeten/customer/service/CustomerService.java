package com.sweeten.customer.service;

import com.sweeten.common.paging.PageResult;
import com.sweeten.customer.model.Customer;
import com.sweeten.customer.model.CustomerQueryParam;
import com.sweeten.order.model.Order;

import java.util.List;

public interface CustomerService {

    PageResult<Customer> page(CustomerQueryParam queryParam);

    Customer getById(Integer id);

    void save(Customer customer);

    void update(Customer customer);

    void deleteById(Integer id);

    List<Order> getCustomerOrders(Integer customerId);
}
