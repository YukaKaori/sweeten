package com.itheima.service.impl;

import com.itheima.mapper.CouponMapper;
import com.itheima.mapper.MemberMapper;
import com.itheima.mapper.PointsLogMapper;
import com.itheima.pojo.*;
import com.itheima.service.MemberService;
import com.itheima.util.PageHelperUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberMapper memberMapper;
    private final PointsLogMapper pointsLogMapper;
    private final CouponMapper couponMapper;

    @Override
    public PageResult<Member> page(MemberQueryParam param) {
        return PageHelperUtils.executePage(param.getPage(), param.getPageSize(),
                () -> memberMapper.list(param));
    }

    @Override
    public Member getById(Integer id) {
        return memberMapper.getById(id);
    }

    @Override
    public Member getByPhone(String phone) {
        return memberMapper.getByPhone(phone);
    }

    /** 积分流水类型（与 points_log.type 注释一致；7 为本次新增） */
    public static final int TYPE_CONSUME = 1;
    public static final int TYPE_BIRTHDAY = 2;
    public static final int TYPE_REDEEM = 4;
    public static final int TYPE_ORDER_REVOKE = 7;

    @Override
    // NESTED：在下单事务内以保存点执行，会员注册失败只回滚自身，不会把整笔订单标记为 rollback-only
    @Transactional(rollbackFor = {Exception.class}, propagation = Propagation.NESTED)
    public Member ensureMember(String customerName, String phone) {
        Member member = memberMapper.getByPhone(phone);
        if (member != null) {
            return member;
        }
        member = new Member();
        member.setName(customerName);
        member.setPhone(phone);
        member.setLevel(0);
        member.setPoints(0);
        member.setTotalPoints(0);
        member.setGrowth(0);
        member.setTotalSpent(BigDecimal.ZERO);
        member.setOrderCount(0);
        member.setRegisteredAt(LocalDateTime.now());
        member.setUpdatedAt(LocalDateTime.now());
        memberMapper.insert(member);
        log.info("新会员注册: phone={}, name={}", phone, customerName);

        // 新人自动发放新人券
        issueNewMemberCoupons(member.getId());
        return member;
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public int awardOrderPoints(Order order) {
        if (order.getPhone() == null || order.getPhone().isBlank() || order.getAmount() == null) {
            return 0;
        }
        // 幂等：旧规则下单即发放过积分的历史订单、或重复调用，都不再发放
        if (pointsLogMapper.findOrderAward(order.getId()) != null) {
            log.info("订单 {} 已发放过积分，跳过", order.getOrderNo());
            return 0;
        }
        Member member = ensureMember(order.getCustomerName(), order.getPhone());

        BigDecimal amount = BigDecimal.valueOf(order.getAmount());
        int basePoints = amount.intValue();  // 1元 = 1积分
        int growth = amount.intValue();       // 1元 = 1成长值

        // 生日月双倍积分
        boolean isBirthdayMonth = member.getBirthday() != null
                && member.getBirthday().getMonthValue() == LocalDate.now().getMonthValue();
        int awardedPoints = isBirthdayMonth ? basePoints * 2 : basePoints;

        memberMapper.addPointsAndGrowth(member.getId(), awardedPoints, growth, amount);
        memberMapper.refreshLevel(member.getId());

        Member refreshed = memberMapper.getById(member.getId());
        PointsLog logEntry = new PointsLog();
        logEntry.setMemberId(member.getId());
        logEntry.setType(isBirthdayMonth ? TYPE_BIRTHDAY : TYPE_CONSUME);
        logEntry.setPoints(awardedPoints);
        logEntry.setBalance(refreshed.getPoints());
        logEntry.setReason((isBirthdayMonth ? "生日月双倍积分 · 订单 " : "消费积分 · 订单 ") + order.getOrderNo());
        logEntry.setRelatedId(order.getId());
        logEntry.setCreatedAt(LocalDateTime.now());
        pointsLogMapper.insert(logEntry);

        log.info("订单完成发放积分: orderNo={}, memberId={}, points=+{}, balance={}, level={}",
                order.getOrderNo(), member.getId(), awardedPoints, refreshed.getPoints(), refreshed.getLevel());
        return awardedPoints;
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public int revokeOrderPoints(Order order) {
        PointsLog award = pointsLogMapper.findOrderAward(order.getId());
        if (award == null || pointsLogMapper.countByOrderAndType(order.getId(), TYPE_ORDER_REVOKE) > 0) {
            return 0; // 从未发放，或已扣回过
        }
        Member member = memberMapper.getById(award.getMemberId());
        if (member == null) {
            log.warn("订单 {} 的积分会员已不存在，无法扣回", order.getOrderNo());
            return 0;
        }
        int awarded = award.getPoints();
        // 积分可能已被兑换消费：最多扣到 0，差额记日志供人工核对
        int deduct = Math.min(awarded, Math.max(member.getPoints(), 0));
        BigDecimal amount = order.getAmount() == null ? BigDecimal.ZERO : BigDecimal.valueOf(order.getAmount());
        if (memberMapper.revokeOrderStats(member.getId(), deduct, awarded, amount.intValue(), amount) == 0) {
            // 并发下余额刚被消费：按最新余额重算一次
            member = memberMapper.getById(member.getId());
            deduct = Math.min(awarded, Math.max(member.getPoints(), 0));
            memberMapper.revokeOrderStats(member.getId(), deduct, awarded, amount.intValue(), amount);
        }
        memberMapper.refreshLevel(member.getId());
        if (deduct < awarded) {
            log.warn("订单 {} 取消：应扣回 {} 积分，会员余额不足，实际扣回 {}", order.getOrderNo(), awarded, deduct);
        }

        Member refreshed = memberMapper.getById(member.getId());
        PointsLog logEntry = new PointsLog();
        logEntry.setMemberId(member.getId());
        logEntry.setType(TYPE_ORDER_REVOKE);
        logEntry.setPoints(-deduct);
        logEntry.setBalance(refreshed.getPoints());
        logEntry.setReason("订单取消扣回 · 订单 " + order.getOrderNo()
                + (deduct < awarded ? "（应扣 " + awarded + "，余额不足）" : ""));
        logEntry.setRelatedId(order.getId());
        logEntry.setCreatedAt(LocalDateTime.now());
        pointsLogMapper.insert(logEntry);
        return deduct;
    }

    @Override
    public boolean memberMatchesPhone(Integer memberId, String phone) {
        if (memberId == null || phone == null) {
            return false;
        }
        Member member = memberMapper.getById(memberId);
        return member != null && phone.equals(member.getPhone());
    }

    /**
     * 新会员自动发放新人券
     */
    private void issueNewMemberCoupons(Integer memberId) {
        List<CouponTemplate> templates = couponMapper.listActive();
        for (CouponTemplate t : templates) {
            if (t.getName().contains("新人") && t.getPointsCost() == 0) {
                MemberCoupon mc = new MemberCoupon();
                mc.setMemberId(memberId);
                mc.setTemplateId(t.getId());
                mc.setStatus(1);
                mc.setAcquiredAt(LocalDateTime.now());
                mc.setExpireAt(LocalDateTime.now().plusDays(t.getValidDays()));
                couponMapper.insertMemberCoupon(mc);
                couponMapper.incrIssuedCount(t.getId());
                log.info("新人券发放: memberId={}, couponName={}", memberId, t.getName());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Map<String, Object> redeemCoupon(Integer memberId, String phone, Integer templateId) {
        Map<String, Object> result = new HashMap<>();

        if (!memberMatchesPhone(memberId, phone)) {
            result.put("success", false);
            result.put("message", "会员信息不匹配，请重新查询会员");
            return result;
        }

        CouponTemplate template = couponMapper.getTemplateById(templateId);
        if (template == null || template.getStatus() != 1) {
            result.put("success", false);
            result.put("message", "优惠券不存在或已下架");
            return result;
        }
        if (template.getPointsCost() <= 0) {
            result.put("success", false);
            result.put("message", "该券不支持积分兑换");
            return result;
        }

        Member member = memberMapper.getById(memberId);
        if (member.getPoints() < template.getPointsCost()) {
            result.put("success", false);
            result.put("message", "积分不足，当前积分：" + member.getPoints());
            return result;
        }

        // 检查库存
        if (template.getTotalStock() > 0 && template.getIssuedCount() >= template.getTotalStock()) {
            result.put("success", false);
            result.put("message", "该券已被抢光啦～");
            return result;
        }

        // 检查重复领取
        int already = couponMapper.countByMemberAndTemplate(memberId, templateId);
        if (already > 0) {
            result.put("success", false);
            result.put("message", "您已领取过该券，请先使用再兑换");
            return result;
        }

        // 扣积分（条件更新：并发兑换时余额不足的那一次失败，不能白送券）
        if (memberMapper.deductPoints(memberId, template.getPointsCost()) == 0) {
            result.put("success", false);
            result.put("message", "积分不足，兑换失败");
            return result;
        }

        // 记录日志
        PointsLog logEntry = new PointsLog();
        logEntry.setMemberId(memberId);
        logEntry.setType(TYPE_REDEEM);
        logEntry.setPoints(-template.getPointsCost());
        Member after = memberMapper.getById(memberId);
        logEntry.setBalance(after.getPoints());
        logEntry.setReason("兑换优惠券：" + template.getName());
        logEntry.setCreatedAt(LocalDateTime.now());
        pointsLogMapper.insert(logEntry);

        // 发券
        MemberCoupon mc = new MemberCoupon();
        mc.setMemberId(memberId);
        mc.setTemplateId(templateId);
        mc.setStatus(1);
        mc.setAcquiredAt(LocalDateTime.now());
        mc.setExpireAt(LocalDateTime.now().plusDays(template.getValidDays()));
        couponMapper.insertMemberCoupon(mc);
        couponMapper.incrIssuedCount(templateId);

        result.put("success", true);
        result.put("message", "兑换成功！" + template.getName() + " 已放入您的卡包");
        result.put("remainingPoints", after.getPoints());
        return result;
    }

    @Override
    public List<MemberCoupon> getUsableCoupons(Integer memberId) {
        return couponMapper.listUsable(memberId);
    }

    @Override
    public List<PointsLog> getPointsLog(Integer memberId) {
        return pointsLogMapper.listByMemberId(memberId);
    }

    @Override
    public Map<String, Object> getMemberProfile(String phone) {
        Map<String, Object> result = new HashMap<>();

        Member member = memberMapper.getByPhone(phone);
        if (member == null) {
            result.put("exists", false);
            return result;
        }

        result.put("exists", true);
        result.put("member", member);
        result.put("coupons", couponMapper.listUsable(member.getId()));
        result.put("recentPointsLog", pointsLogMapper.listByMemberId(member.getId())
                .stream().limit(20).toList());

        // 等级名称
        String[] levelNames = {"普通会员", "银卡会员", "金卡会员", "钻石会员"};
        result.put("levelName", levelNames[Math.min(member.getLevel(), 3)]);

        // 下一级所需成长值
        int[] nextGrowth = {1000, 3000, 10000, Integer.MAX_VALUE};
        int currentLevel = Math.min(member.getLevel(), 3);
        result.put("nextLevelGrowth", nextGrowth[currentLevel]);
        result.put("growthProgress", currentLevel < 3
                ? (int) ((double) member.getGrowth() / nextGrowth[currentLevel] * 100)
                : 100);

        return result;
    }
}
