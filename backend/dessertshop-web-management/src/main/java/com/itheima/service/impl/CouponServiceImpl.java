package com.itheima.service.impl;

import com.itheima.mapper.CouponMapper;
import com.itheima.pojo.CouponTemplate;
import com.itheima.pojo.MemberCoupon;
import com.itheima.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponMapper couponMapper;

    @Override
    public List<CouponTemplate> listActive() {
        return couponMapper.listActive();
    }

    @Override
    public CouponTemplate getTemplate(Integer id) {
        return couponMapper.getTemplateById(id);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void createTemplate(CouponTemplate template) {
        template.setCreatedAt(LocalDateTime.now());
        template.setIssuedCount(0);
        couponMapper.insertTemplate(template);
        log.info("优惠券模板创建: {}", template.getName());
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void issueCoupon(Integer memberId, Integer templateId) {
        CouponTemplate template = couponMapper.getTemplateById(templateId);
        if (template == null) throw new IllegalArgumentException("券模板不存在");

        MemberCoupon mc = new MemberCoupon();
        mc.setMemberId(memberId);
        mc.setTemplateId(templateId);
        mc.setStatus(1);
        mc.setAcquiredAt(LocalDateTime.now());
        mc.setExpireAt(LocalDateTime.now().plusDays(template.getValidDays()));
        couponMapper.insertMemberCoupon(mc);
        couponMapper.incrIssuedCount(templateId);

        log.info("优惠券发放: memberId={}, couponName={}", memberId, template.getName());
    }
}
