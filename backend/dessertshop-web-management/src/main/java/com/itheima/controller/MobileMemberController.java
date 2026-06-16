package com.itheima.controller;

import com.itheima.pojo.*;
import com.itheima.service.CouponService;
import com.itheima.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 移动端会员接口（需经过 TokenFilter 但 /mobile 前缀公开）
 */
@Slf4j
@RequestMapping("/mobile/member")
@RestController
@RequiredArgsConstructor
public class MobileMemberController {

    private final MemberService memberService;
    private final CouponService couponService;

    /**
     * 查询会员完整信息（用于"我的"页面）
     * GET /mobile/member/profile?phone=13800138001
     */
    @GetMapping("/profile")
    public Result getProfile(@RequestParam String phone) {
        log.info("移动端查询会员信息: phone={}", phone);
        Map<String, Object> profile = memberService.getMemberProfile(phone);
        return Result.success(profile);
    }

    /**
     * 积分兑换优惠券
     * POST /mobile/member/redeem
     */
    @PostMapping("/redeem")
    public Result redeem(@RequestBody Map<String, Integer> body) {
        Integer memberId = body.get("memberId");
        Integer templateId = body.get("templateId");
        log.info("积分兑换: memberId={}, templateId={}", memberId, templateId);

        if (memberId == null || templateId == null) {
            return Result.error("参数不完整");
        }

        Map<String, Object> result = memberService.redeemCoupon(memberId, templateId);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return Result.success(result);
        }
        return Result.error((String) result.get("message"));
    }

    /**
     * 获取会员可用优惠券列表
     * GET /mobile/member/coupons?memberId=1
     */
    @GetMapping("/coupons")
    public Result getCoupons(@RequestParam Integer memberId) {
        log.info("查询会员优惠券: memberId={}", memberId);
        return Result.success(memberService.getUsableCoupons(memberId));
    }

    /**
     * 获取积分日志
     * GET /mobile/member/points-log?memberId=1
     */
    @GetMapping("/points-log")
    public Result getPointsLog(@RequestParam Integer memberId) {
        log.info("查询积分日志: memberId={}", memberId);
        return Result.success(memberService.getPointsLog(memberId));
    }

    /**
     * 获取可兑换的优惠券列表
     * GET /mobile/member/exchange-list
     */
    @GetMapping("/exchange-list")
    public Result getExchangeList() {
        log.info("查询积分兑换券列表");
        return Result.success(couponService.listActive());
    }
}
