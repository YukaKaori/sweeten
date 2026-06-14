package com.itheima.controller;

import com.itheima.annotation.LogOperation;
import com.itheima.pojo.*;
import com.itheima.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequestMapping("/customers")
@RestController
@Validated
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    /** 分页查询客户 */
    @GetMapping
    public Result page(@Valid CustomerQueryParam queryParam) {
        log.info("分页查询客户:{}", queryParam);
        PageResult<Customer> pageResult = customerService.page(queryParam);
        return Result.success(pageResult);
    }

    /** 根据ID查询客户 */
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        log.info("查询客户详情,id:{}", id);
        Customer customer = customerService.getById(id);
        return Result.success(customer);
    }

    /** 查询客户订单历史 */
    @GetMapping("/{id}/orders")
    public Result getOrders(@PathVariable Integer id,
                            @RequestParam(defaultValue = "1") Integer page,
                            @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("查询客户订单,id:{},page:{},pageSize:{}", id, page, pageSize);
        List<Order> orders = customerService.getCustomerOrders(id);
        // 手动分页
        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, orders.size());
        List<Order> paged = from < orders.size() ? orders.subList(from, to) : List.of();
        PageResult<Order> pr = new PageResult<>();
        pr.setTotal((long) orders.size());
        pr.setRows(paged);
        return Result.success(pr);
    }

    /** 新增客户 */
    @LogOperation
    @PostMapping
    public Result save(@Valid @RequestBody Customer customer) {
        log.info("新增客户:{}", customer.getName());
        customerService.save(customer);
        return Result.success();
    }

    /** 修改客户 */
    @LogOperation
    @PutMapping
    public Result update(@Valid @RequestBody Customer customer) {
        log.info("修改客户,id:{}", customer.getId());
        customerService.update(customer);
        return Result.success();
    }

    /** 删除客户 */
    @LogOperation
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        log.info("删除客户,id:{}", id);
        customerService.deleteById(id);
        return Result.success();
    }
}
