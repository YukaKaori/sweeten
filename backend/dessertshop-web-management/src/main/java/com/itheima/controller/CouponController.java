package com.itheima.controller;

import com.itheima.annotation.LogOperation;
import com.itheima.pojo.CouponTemplate;
import com.itheima.pojo.Result;
import com.itheima.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 管理后台 · 优惠券模板管理
 */
@Slf4j
@RequestMapping("/coupons")
@RestController
@Validated
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    /** 券模板列表 */
    @GetMapping
    public Result list() {
        log.info("查询优惠券模板列表");
        return Result.success(couponService.listActive());
    }

    /** 查询单个模板 */
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        log.info("查询优惠券模板: id={}", id);
        return Result.success(couponService.getTemplate(id));
    }

    /** 创建券模板 */
    @LogOperation
    @PostMapping
    public Result create(@Valid @RequestBody CouponTemplate template) {
        log.info("创建优惠券模板: {}", template.getName());
        couponService.createTemplate(template);
        return Result.success();
    }
}
