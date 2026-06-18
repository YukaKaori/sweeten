package com.itheima.controller;

import com.itheima.annotation.LogOperation;
import com.itheima.pojo.*;
import com.itheima.service.CouponService;
import com.itheima.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 管理后台 · 会员管理
 */
@Slf4j
@RequestMapping("/members")
@RestController
@Validated
@RequiredArgsConstructor
@Tag(name = "14-会员管理", description = "会员信息查询、优惠券发放与积分日志管理接口")
public class MemberController {

    private final MemberService memberService;
    private final CouponService couponService;

    @Operation(summary = "分页查询会员", description = "支持按姓名、手机号、等级等条件分页查询会员列表")
    @GetMapping
    public Result page(@Valid MemberQueryParam param) {
        log.info("分页查询会员:{}", param);
        return Result.success(memberService.page(param));
    }

    @Operation(summary = "根据ID查询会员", description = "查询单个会员的详细信息")
    @GetMapping("/{id}")
    public Result getById(@Parameter(description = "会员ID") @PathVariable Integer id) {
        log.info("查询会员详情: id={}", id);
        return Result.success(memberService.getById(id));
    }

    @Operation(summary = "根据手机号查询会员", description = "通过手机号查找会员信息")
    @GetMapping("/by-phone")
    public Result getByPhone(@Parameter(description = "手机号") @RequestParam String phone) {
        log.info("按手机号查询会员: phone={}", phone);
        Member member = memberService.getByPhone(phone);
        return member != null ? Result.success(member) : Result.error("会员不存在");
    }

    @Operation(summary = "发放优惠券", description = "给指定会员发放一张优惠券模板生成的优惠券")
    @LogOperation
    @PostMapping("/{id}/issue-coupon")
    public Result issueCoupon(@Parameter(description = "会员ID") @PathVariable Integer id,
                              @Parameter(description = "优惠券模板ID") @RequestParam Integer templateId) {
        log.info("给会员发券: memberId={}, templateId={}", id, templateId);
        couponService.issueCoupon(id, templateId);
        return Result.success();
    }

    @Operation(summary = "查询会员积分日志", description = "查询指定会员的积分变动历史记录")
    @GetMapping("/{id}/points-log")
    public Result getPointsLog(@Parameter(description = "会员ID") @PathVariable Integer id) {
        log.info("查询积分日志: memberId={}", id);
        return Result.success(memberService.getPointsLog(id));
    }
}
