package com.itheima.controller;

import com.itheima.annotation.LogOperation;
import com.itheima.pojo.*;
import com.itheima.service.CouponService;
import com.itheima.service.MemberService;
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
public class MemberController {

    private final MemberService memberService;
    private final CouponService couponService;

    /** 分页查询会员 */
    @GetMapping
    public Result page(@Valid MemberQueryParam param) {
        log.info("分页查询会员:{}", param);
        return Result.success(memberService.page(param));
    }

    /** 根据ID查询 */
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        log.info("查询会员详情: id={}", id);
        return Result.success(memberService.getById(id));
    }

    /** 根据手机号查询 */
    @GetMapping("/by-phone")
    public Result getByPhone(@RequestParam String phone) {
        log.info("按手机号查询会员: phone={}", phone);
        Member member = memberService.getByPhone(phone);
        return member != null ? Result.success(member) : Result.error("会员不存在");
    }

    /** 发放优惠券 */
    @LogOperation
    @PostMapping("/{id}/issue-coupon")
    public Result issueCoupon(@PathVariable Integer id, @RequestParam Integer templateId) {
        log.info("给会员发券: memberId={}, templateId={}", id, templateId);
        couponService.issueCoupon(id, templateId);
        return Result.success();
    }

    /** 查询会员积分日志 */
    @GetMapping("/{id}/points-log")
    public Result getPointsLog(@PathVariable Integer id) {
        log.info("查询积分日志: memberId={}", id);
        return Result.success(memberService.getPointsLog(id));
    }
}
