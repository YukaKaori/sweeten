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

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public int afterOrder(String customerName, String phone, BigDecimal amount, Integer orderId) {
        // 1. 查找或创建会员
        Member member = memberMapper.getByPhone(phone);
        if (member == null) {
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
        }

        // 2. 计算积分
        int basePoints = amount.intValue();  // 1元 = 1积分
        int growth = amount.intValue();

        // 生日月双倍积分
        boolean isBirthdayMonth = member.getBirthday() != null
                && member.getBirthday().getMonthValue() == LocalDate.now().getMonthValue();
        int awardedPoints = isBirthdayMonth ? basePoints * 2 : basePoints;
        if (isBirthdayMonth) {
            log.info("生日月双倍积分: memberId={}, basePoints={}, awardedPoints={}",
                    member.getId(), basePoints, awardedPoints);
        }

        // 3. 更新会员数据
        memberMapper.addPointsAndGrowth(member.getId(), awardedPoints, growth, amount);
        memberMapper.refreshLevel(member.getId());

        // 4. 记录积分日志
        PointsLog logEntry = new PointsLog();
        logEntry.setMemberId(member.getId());
        logEntry.setType(isBirthdayMonth ? 2 : 1);
        logEntry.setPoints(awardedPoints);
        logEntry.setReason(isBirthdayMonth
                ? "生日月双倍积分 · 订单 #" + orderId
                : "消费积分 · 订单 #" + orderId);
        logEntry.setRelatedId(orderId);
        logEntry.setCreatedAt(LocalDateTime.now());

        // 查余额
        Member refreshed = memberMapper.getById(member.getId());
        logEntry.setBalance(refreshed.getPoints());
        pointsLogMapper.insert(logEntry);

        log.info("会员积分更新: memberId={}, phone={}, points=+{}, balance={}, level={}",
                member.getId(), phone, awardedPoints, refreshed.getPoints(), refreshed.getLevel());

        return awardedPoints;
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
    public Map<String, Object> redeemCoupon(Integer memberId, Integer templateId) {
        Map<String, Object> result = new HashMap<>();

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

        // 扣积分
        memberMapper.deductPoints(memberId, template.getPointsCost());

        // 记录日志
        PointsLog logEntry = new PointsLog();
        logEntry.setMemberId(memberId);
        logEntry.setType(4);
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
