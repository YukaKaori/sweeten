package com.sweeten.storefront.web;

import com.sweeten.common.exception.NotFoundException;
import com.sweeten.common.web.Result;
import com.sweeten.customer.service.MemberService;
import com.sweeten.promotion.service.CouponService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 顾客商城会员中心接口（/mobile/** 在 TokenFilter 白名单内，无需员工令牌）。
 * <p>
 * 已知限制：会员目前只凭手机号识别（无短信验证），上线公众服务前需要接入顾客身份认证，见 docs/ARCHITECTURE.md。
 */
@Slf4j
@RequestMapping("/mobile/member")
@RestController
@RequiredArgsConstructor
@Tag(name = "16-顾客商城会员接口", description = "会员资料、积分流水、可用券与积分兑换；路径前缀 /mobile 为历史兼容保留")
public class StorefrontMemberController {

    private final MemberService memberService;
    private final CouponService couponService;

    /**
     * 查询会员完整信息（用于"我的"页面）
     * GET /mobile/member/profile?phone=13800138001
     */
    @GetMapping("/profile")
    public Result getProfile(@RequestParam String phone) {
        log.info("商城查询会员信息: phone={}", phone);
        Map<String, Object> profile = memberService.getMemberProfile(phone);
        return Result.success(profile);
    }

    /**
     * 积分兑换优惠券
     * POST /mobile/member/redeem
     */
    @PostMapping("/redeem")
    public Result redeem(@RequestBody Map<String, Object> body) {
        Integer memberId = body.get("memberId") instanceof Number n ? n.intValue() : null;
        Integer templateId = body.get("templateId") instanceof Number n ? n.intValue() : null;
        String phone = body.get("phone") instanceof String s ? s : null;
        log.info("积分兑换: memberId={}, templateId={}", memberId, templateId);

        if (memberId == null || templateId == null || phone == null) {
            return Result.error("参数不完整");
        }

        Map<String, Object> result = memberService.redeemCoupon(memberId, phone, templateId);
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
    public Result getCoupons(@RequestParam Integer memberId, @RequestParam String phone) {
        log.info("查询会员优惠券: memberId={}", memberId);
        if (!memberService.memberMatchesPhone(memberId, phone)) {
            throw new NotFoundException("会员不存在");
        }
        return Result.success(memberService.getUsableCoupons(memberId));
    }

    /**
     * 获取积分日志
     * GET /mobile/member/points-log?memberId=1
     */
    @GetMapping("/points-log")
    public Result getPointsLog(@RequestParam Integer memberId, @RequestParam String phone) {
        log.info("查询积分日志: memberId={}", memberId);
        if (!memberService.memberMatchesPhone(memberId, phone)) {
            throw new NotFoundException("会员不存在");
        }
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
