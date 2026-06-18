package com.itheima.controller;

import com.itheima.annotation.LogOperation;
import com.itheima.pojo.Order;
import com.itheima.pojo.OrderQueryParam;
import com.itheima.pojo.PageResult;
import com.itheima.pojo.Result;
import com.itheima.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequestMapping("/orders")
@RestController
@Validated
@RequiredArgsConstructor
@Tag(name = "05-订单管理", description = "订单的增删改查与状态流转管理接口")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "分页查询订单", description = "支持按订单号、客户名、状态、日期范围等条件分页查询订单列表")
    @GetMapping
    public Result page(@Valid OrderQueryParam queryParam) {
        log.info("分页查询订单:{}", queryParam);
        PageResult<Order> pageResult = orderService.page(queryParam);
        return Result.success(pageResult);
    }

    @Operation(summary = "根据ID查询订单详情", description = "根据订单ID查询订单的详细信息，含订单明细")
    @GetMapping("/{id}")
    public Result getById(@Parameter(description = "订单ID") @PathVariable Integer id) {
        log.info("根据id查询订单:{}", id);
        Order order = orderService.getById(id);
        return Result.success(order);
    }

    @Operation(summary = "新增订单", description = "创建一个新订单")
    @LogOperation
    @PostMapping
    public Result save(@Valid @RequestBody Order order) {
        log.info("新增订单,orderNo:{}", order.getOrderNo());
        orderService.save(order);
        return Result.success();
    }

    @Operation(summary = "修改订单", description = "修改订单信息（含状态流转）")
    @LogOperation
    @PutMapping
    public Result update(@Valid @RequestBody Order order) {
        log.info("修改订单,id:{}", order.getId());
        orderService.update(order);
        return Result.success();
    }

    @Operation(summary = "删除订单", description = "根据ID删除指定订单")
    @LogOperation
    @DeleteMapping
    public Result delete(@Parameter(description = "订单ID") Integer id) {
        log.info("删除订单,id:{}", id);
        orderService.deleteById(id);
        return Result.success();
    }
}
