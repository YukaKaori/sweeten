package com.sweeten.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sweeten.catalog.model.Dessert;
import com.sweeten.catalog.service.DessertService;
import com.sweeten.common.exception.BusinessException;
import com.sweeten.order.event.OrderPlacedEvent;
import com.sweeten.order.mapper.OrderMapper;
import com.sweeten.order.model.CheckoutRequest;
import com.sweeten.order.model.Order;
import com.sweeten.order.model.OrderLine;
import com.sweeten.order.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
 * 顾客商城结算：把购物车转为订单并保存。
 * <p>
 * 商品库（dessert 表）是价格与库存的唯一来源：客户端传来的单价只用于核对，
 * 金额由服务端按当前价格重算；库存在同一事务内原子扣减，任一商品失败整单回滚。
 * 下单成功后发布 {@link OrderPlacedEvent}（会员注册等在监听方完成，本类不依赖会员模块）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

    /**
     * 订单号前缀。已有订单号均为 DS 开头（顾客凭订单号查单），属于兼容标识，暂不更改；
     * 若要启用新前缀，需同时确认前端查单输入、对账与报表口径。
     */
    static final String ORDER_NO_PREFIX = "DS";
    private static final int ORDER_NO_MAX_ATTEMPTS = 5;

    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;
    private final DessertService dessertService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public Order placeOrder(CheckoutRequest request) {
        // 1. 合并同一商品的多行（客户端异常提交时防止绕过库存校验）
        Map<Integer, Integer> qtyById = new LinkedHashMap<>();
        for (OrderLine item : request.getItems()) {
            if (item.getQty() == null || item.getQty() < 1) {
                throw new BusinessException("商品数量至少为 1");
            }
            qtyById.merge(item.getProductId(), item.getQty(), Integer::sum);
        }

        // 2. 逐个商品按库内价格核算并扣减库存
        List<OrderLine> lines = new ArrayList<>();
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

            OrderLine line = new OrderLine();
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
            log.warn("商城订单金额不一致: client={}, server={}", clientTotal, total);
            throw new BusinessException("商品价格已更新，请返回确认最新价格后再下单");
        }

        // 4. 明细以服务端核算结果序列化（商品名、单价均取自商品库）
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
        // 尚未接入在线支付：下单直接进入「待接单」。接入支付后改为 PENDING_PAYMENT，由支付确认推进（见 OrderStatus）
        order.setStatus(OrderStatus.AWAITING_ACCEPTANCE.code());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        insertWithUniqueOrderNo(order);
        log.info("商城订单创建成功: orderNo={}, amount={}", order.getOrderNo(), total);

        // 5. 下单后续（会员查找 / 注册、新人券）：失败只记日志，不影响下单。积分在订单完成时发放
        try {
            eventPublisher.publishEvent(new OrderPlacedEvent(order));
        } catch (Exception e) {
            log.error("下单后续处理失败（不影响下单）: {}", e.getMessage());
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
        return ORDER_NO_PREFIX + now + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
