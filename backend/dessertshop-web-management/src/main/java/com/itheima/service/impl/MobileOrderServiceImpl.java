package com.itheima.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.exception.BusinessException;
import com.itheima.mapper.OrderMapper;
import com.itheima.pojo.Dessert;
import com.itheima.pojo.MobileOrderRequest;
import com.itheima.pojo.Order;
import com.itheima.service.DessertService;
import com.itheima.service.MemberService;
import com.itheima.service.MobileOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 移动端订单服务实现 — 将移动端请求转为系统 Order 模型并保存
 * <p>
 * 商品库（dessert 表）是价格与库存的唯一来源：客户端传来的单价只用于核对，
 * 金额由服务端按当前价格重算；库存在同一事务内原子扣减，任一商品失败整单回滚。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MobileOrderServiceImpl implements MobileOrderService {

    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;
    private final MemberService memberService;
    private final DessertService dessertService;

    private static final int ORDER_NO_MAX_ATTEMPTS = 5;

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public Order createOrder(MobileOrderRequest request) {
        // 1. 合并同一商品的多行（客户端异常提交时防止绕过库存校验）
        Map<Integer, Integer> qtyById = new LinkedHashMap<>();
        for (MobileOrderRequest.OrderItem item : request.getItems()) {
            if (item.getQty() == null || item.getQty() < 1) {
                throw new BusinessException("商品数量至少为 1");
            }
            qtyById.merge(item.getProductId(), item.getQty(), Integer::sum);
        }

        // 2. 逐个商品按库内价格核算并扣减库存
        List<MobileOrderRequest.OrderItem> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Integer, Integer> entry : qtyById.entrySet()) {
            Integer productId = entry.getKey();
            int qty = entry.getValue();
            Dessert dessert = dessertService.getById(productId);
            if (dessert == null) {
                throw new BusinessException("商品不存在或已删除，请刷新后重新选择");
            }
            if (dessert.getStatus() == null || dessert.getStatus() != 1) {
                throw new BusinessException("「" + dessert.getName() + "」已下架，请从购物车移除后再下单");
            }
            if (!dessertService.deductStock(productId, qty)) {
                int left = dessert.getStock() == null ? 0 : Math.max(dessert.getStock(), 0);
                throw new BusinessException("「" + dessert.getName() + "」库存不足，当前仅剩 " + left + " 件");
            }

            MobileOrderRequest.OrderItem line = new MobileOrderRequest.OrderItem();
            line.setProductId(productId);
            line.setProductName(dessert.getName());
            line.setPrice(dessert.getPrice());
            line.setQty(qty);
            lines.add(line);
            total = total.add(BigDecimal.valueOf(dessert.getPrice()).multiply(BigDecimal.valueOf(qty)));
        }
        total = total.setScale(2, RoundingMode.HALF_UP);

        // 3. 价格已变动时拒单（回滚已扣库存），让顾客看到最新价格后再确认，避免"下单价≠扣款价"
        BigDecimal clientTotal = BigDecimal.valueOf(request.getTotalAmount()).setScale(2, RoundingMode.HALF_UP);
        if (clientTotal.compareTo(total) != 0) {
            log.warn("移动端订单金额不一致: client={}, server={}", clientTotal, total);
            throw new BusinessException("商品价格已更新，请返回确认最新价格后再下单");
        }

        // 5. 明细以服务端核算结果序列化（商品名、单价均取自商品库）
        String itemsJson;
        try {
            itemsJson = objectMapper.writeValueAsString(lines);
        } catch (JsonProcessingException e) {
            log.error("序列化订单明细失败", e);
            throw new BusinessException("订单数据格式错误");
        }
        if (itemsJson.length() > Order.ITEMS_MAX_LENGTH) {
            throw new BusinessException("单笔订单商品种类过多，请分开下单");
        }

        Order order = new Order();
        order.setCustomerName(request.getCustomerName());
        order.setPhone(request.getPhone());
        order.setAddress(request.getAddress());
        order.setItems(itemsJson);
        order.setAmount(total.doubleValue());
        order.setStatus(1); // 1 = 待接单
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        insertWithUniqueOrderNo(order);
        log.info("移动端订单创建成功: orderNo={}, amount={}", order.getOrderNo(), total);

        // ── 会员：查找/注册（新人券）。积分在订单完成时发放，见 OrderServiceImpl ──
        try {
            memberService.ensureMember(request.getCustomerName(), request.getPhone());
        } catch (Exception e) {
            log.error("会员注册失败（不影响下单）: {}", e.getMessage());
        }

        return order;
    }

    /** 订单号碰撞（同一秒内随机后缀相同）时重新生成，依赖 orders.order_no 唯一索引兜底 */
    private void insertWithUniqueOrderNo(Order order) {
        for (int attempt = 1; ; attempt++) {
            order.setOrderNo(generateOrderNo());
            try {
                orderMapper.insert(order);
                return;
            } catch (DuplicateKeyException e) {
                if (attempt >= ORDER_NO_MAX_ATTEMPTS) {
                    throw new BusinessException("下单繁忙，请稍后重试");
                }
                log.warn("订单号冲突，重新生成: {}", order.getOrderNo());
            }
        }
    }

    /** 订单号：DS + yyyyMMddHHmmss + 4位随机数（共 20 位） */
    private static String generateOrderNo() {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "DS" + now + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    @Override
    public Order findOrder(String orderNo, String phone) {
        return orderMapper.getByOrderNoAndPhone(orderNo, phone);
    }
}
