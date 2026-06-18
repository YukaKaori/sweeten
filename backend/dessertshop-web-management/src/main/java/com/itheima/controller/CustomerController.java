package com.itheima.controller;

import com.itheima.annotation.LogOperation;
import com.itheima.pojo.*;
import com.itheima.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "10-客户CRM", description = "客户关系管理的增删改查与订单历史查询接口")
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "分页查询客户", description = "支持按姓名、电话、等级等条件分页查询客户列表")
    @GetMapping
    public Result page(@Valid CustomerQueryParam queryParam) {
        log.info("分页查询客户:{}", queryParam);
        PageResult<Customer> pageResult = customerService.page(queryParam);
        return Result.success(pageResult);
    }

    @Operation(summary = "根据ID查询客户", description = "查询单个客户的详细信息")
    @GetMapping("/{id}")
    public Result getById(@Parameter(description = "客户ID") @PathVariable Integer id) {
        log.info("查询客户详情,id:{}", id);
        Customer customer = customerService.getById(id);
        return Result.success(customer);
    }

    @Operation(summary = "查询客户订单历史", description = "根据客户ID查询其历史订单记录，支持分页")
    @GetMapping("/{id}/orders")
    public Result getOrders(@Parameter(description = "客户ID") @PathVariable Integer id,
                            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
                            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize) {
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

    @Operation(summary = "新增客户", description = "新增一个客户信息")
    @LogOperation
    @PostMapping
    public Result save(@Valid @RequestBody Customer customer) {
        log.info("新增客户:{}", customer.getName());
        customerService.save(customer);
        return Result.success();
    }

    @Operation(summary = "修改客户", description = "修改客户信息")
    @LogOperation
    @PutMapping
    public Result update(@Valid @RequestBody Customer customer) {
        log.info("修改客户,id:{}", customer.getId());
        customerService.update(customer);
        return Result.success();
    }

    @Operation(summary = "删除客户", description = "根据ID删除指定客户")
    @LogOperation
    @DeleteMapping("/{id}")
    public Result delete(@Parameter(description = "客户ID") @PathVariable Integer id) {
        log.info("删除客户,id:{}", id);
        customerService.deleteById(id);
        return Result.success();
    }
}
