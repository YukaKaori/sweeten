package com.itheima.controller;

import com.itheima.annotation.LogOperation;
import com.itheima.pojo.Order;
import com.itheima.pojo.OrderQueryParam;
import com.itheima.pojo.PageResult;
import com.itheima.pojo.Result;
import com.itheima.service.OrderService;
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
public class OrderController {

    private final OrderService orderService;

    /**
     * 分页查询订单
     */
    @GetMapping
    public Result page(@Valid OrderQueryParam queryParam) {
        log.info("分页查询订单:{}", queryParam);
        PageResult<Order> pageResult = orderService.page(queryParam);
        return Result.success(pageResult);
    }

    /**
     * 根据ID查询订单详情
     */
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        log.info("根据id查询订单:{}", id);
        Order order = orderService.getById(id);
        return Result.success(order);
    }

    /**
     * 新增订单
     */
    @LogOperation
    @PostMapping
    public Result save(@Valid @RequestBody Order order) {
        log.info("新增订单,orderNo:{}", order.getOrderNo());
        orderService.save(order);
        return Result.success();
    }

    /**
     * 修改订单
     */
    @LogOperation
    @PutMapping
    public Result update(@Valid @RequestBody Order order) {
        log.info("修改订单,id:{}", order.getId());
        orderService.update(order);
        return Result.success();
    }

    /**
     * 删除订单
     */
    @LogOperation
    @DeleteMapping
    public Result delete(Integer id) {
        log.info("删除订单,id:{}", id);
        orderService.deleteById(id);
        return Result.success();
    }
}
