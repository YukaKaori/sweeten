package com.itheima.service.impl;

import com.itheima.mapper.CustomerMapper;
import com.itheima.pojo.Customer;
import com.itheima.pojo.CustomerQueryParam;
import com.itheima.pojo.Order;
import com.itheima.pojo.PageResult;
import com.itheima.service.CustomerService;
import com.itheima.util.PageHelperUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerMapper customerMapper;

    @Override
    public PageResult<Customer> page(CustomerQueryParam queryParam) {
        return PageHelperUtils.executePage(queryParam.getPage(), queryParam.getPageSize(),
                () -> customerMapper.list(queryParam));
    }

    @Override
    public Customer getById(Integer id) {
        return customerMapper.getById(id);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void save(Customer customer) {
        customer.setCreateTime(LocalDateTime.now());
        customer.setUpdateTime(LocalDateTime.now());
        customerMapper.insert(customer);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void update(Customer customer) {
        customer.setUpdateTime(LocalDateTime.now());
        customerMapper.updateById(customer);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void deleteById(Integer id) {
        customerMapper.deleteById(id);
    }

    @Override
    public List<Order> getCustomerOrders(Integer customerId) {
        Customer customer = customerMapper.getById(customerId);
        if (customer == null) {
            return Collections.emptyList();
        }
        return customerMapper.selectOrdersByCustomerName(customer.getName());
    }
}
