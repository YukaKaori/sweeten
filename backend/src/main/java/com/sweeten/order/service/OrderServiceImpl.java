package com.sweeten.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sweeten.catalog.service.DessertService;
import com.sweeten.common.exception.BusinessException;
import com.sweeten.common.exception.NotFoundException;
import com.sweeten.common.paging.PageHelperUtils;
import com.sweeten.common.paging.PageResult;
import com.sweeten.order.event.OrderStatusChangedEvent;
import com.sweeten.order.mapper.OrderMapper;
import com.sweeten.order.model.Order;
import com.sweeten.order.model.OrderQueryParam;
import com.sweeten.order.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 订单管理与状态流转。
 * <p>
 * 各环节的一致性约定（同一事务内完成，任一步失败整体回滚，可安全重试）：
 * <ul>
 *   <li>下单（CheckoutServiceImpl）：扣库存、记销量；查找/注册会员；<b>不发积分</b></li>
 *   <li>→ 已完成：发放积分、成长值、累计消费与订单数（每单只发一次，由 customer 模块监听 {@link OrderStatusChangedEvent} 完成）</li>
 *   <li>→ 已取消（仅待支付/待接单可取消）：回补商城订单扣减的库存；若该单曾发放积分（旧规则历史订单）则扣回</li>
 *   <li>重复提交同一目标状态：直接成功、无副作用；并发流转靠"带原状态条件的 update"保证只有一个请求生效</li>
 * </ul>
 * 流转规则见 {@link OrderStatus}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final DessertService dessertService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public PageResult<Order> page(OrderQueryParam queryParam) {
        return PageHelperUtils.executePage(queryParam.getPage(), queryParam.getPageSize(),
                () -> orderMapper.list(queryParam));
    }

    @Override
    public Order getById(Integer id) {
        return orderMapper.getById(id);
    }

    @Override
    public Order findByOrderNoAndPhone(String orderNo, String phone) {
        return orderMapper.getByOrderNoAndPhone(orderNo, phone);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void save(Order order) {
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void update(Order order) {
        Order current = orderMapper.getById(order.getId());
        if (current == null) {
            throw new NotFoundException("订单", order.getId());
        }
        // 状态只能经 PUT /orders/status 流转（那里负责库存/积分联动），整单修改不得绕过
        if (order.getStatus() != null && !order.getStatus().equals(current.getStatus())) {
            throw new BusinessException("请使用订单状态流转操作修改状态");
        }
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void updateStatus(Integer id, Integer status) {
        Order order = orderMapper.getById(id);
        if (order == null) {
            throw new NotFoundException("订单", id);
        }
        Integer from = order.getStatus() == null ? OrderStatus.PENDING_PAYMENT.code() : order.getStatus();
        if (from.equals(status)) {
            return;
        }
        Optional<OrderStatus> current = OrderStatus.fromCode(from);
        Optional<OrderStatus> target = OrderStatus.fromCode(status);
        if (current.isEmpty() || target.isEmpty() || !current.get().canTransitionTo(target.get())) {
            throw new BusinessException("订单当前为「" + OrderStatus.labelOf(from) + "」，不能变更为「"
                    + OrderStatus.labelOf(status) + "」");
        }
        if (orderMapper.updateStatus(id, from, status) == 0) {
            throw new BusinessException("订单状态已被他人更新，请刷新后重试");
        }
        if (target.get() == OrderStatus.CANCELLED) {
            restoreStock(order);
        }
        // 同步事件、同一事务：会员积分发放 / 扣回在监听方执行，失败则整次流转回滚
        eventPublisher.publishEvent(new OrderStatusChangedEvent(order, current.get(), target.get()));
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void deleteById(Integer id) {
        Order order = orderMapper.getById(id);
        if (order == null) {
            return; // 已不存在：删除是幂等的
        }
        // 进行中的订单可能占用着库存：先取消（会回补库存）再删除；已完成/已取消的订单可直接删除
        OrderStatus status = OrderStatus.fromCode(order.getStatus()).orElse(null);
        if (status != OrderStatus.COMPLETED && status != OrderStatus.CANCELLED) {
            throw new BusinessException("进行中的订单不能删除，请先取消订单");
        }
        orderMapper.deleteById(id);
    }

    /**
     * 取消订单时回补库存。只有商城下单（JSON 明细，含 productId）才在下单时扣过库存；
     * 历史/手工订单的明细是纯文本（如 "提拉米苏 x1"），下单时未扣库存，因此不回补。
     */
    private void restoreStock(Order order) {
        String items = order.getItems();
        if (items == null || !items.trim().startsWith("[")) {
            return;
        }
        try {
            for (JsonNode line : objectMapper.readTree(items)) {
                JsonNode productId = line.get("productId");
                JsonNode qty = line.get("qty");
                if (productId != null && productId.canConvertToInt() && qty != null && qty.asInt() > 0) {
                    dessertService.restoreStock(productId.asInt(), qty.asInt());
                }
            }
        } catch (Exception e) {
            // 明细无法解析时不阻断取消，记录日志供人工核对库存
            log.warn("订单 {} 明细解析失败，未回补库存: {}", order.getOrderNo(), e.getMessage());
        }
    }
}
