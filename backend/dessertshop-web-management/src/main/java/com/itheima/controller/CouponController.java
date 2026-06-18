package com.itheima.controller;

import com.itheima.annotation.LogOperation;
import com.itheima.pojo.CouponTemplate;
import com.itheima.pojo.Result;
import com.itheima.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "15-优惠券管理", description = "优惠券模板的创建、查询与管理接口")
public class CouponController {

    private final CouponService couponService;

    @Operation(summary = "券模板列表", description = "获取当前生效的优惠券模板列表")
    @GetMapping
    public Result list() {
        log.info("查询优惠券模板列表");
        return Result.success(couponService.listActive());
    }

    @Operation(summary = "查询单个模板", description = "根据ID查询优惠券模板的详细信息")
    @GetMapping("/{id}")
    public Result getById(@Parameter(description = "模板ID") @PathVariable Integer id) {
        log.info("查询优惠券模板: id={}", id);
        return Result.success(couponService.getTemplate(id));
    }

    @Operation(summary = "创建券模板", description = "创建一个新的优惠券模板")
    @LogOperation
    @PostMapping
    public Result create(@Valid @RequestBody CouponTemplate template) {
        log.info("创建优惠券模板: {}", template.getName());
        couponService.createTemplate(template);
        return Result.success();
    }
}
