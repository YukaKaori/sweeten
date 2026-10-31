package com.itheima.controller;

import com.itheima.exception.NotFoundException;
import com.itheima.pojo.Dessert;
import com.itheima.pojo.MobileOrderRequest;
import com.itheima.pojo.Order;
import com.itheima.pojo.Result;
import com.itheima.service.DessertService;
import com.itheima.service.MobileOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 移动端公开接口（无需 JWT 认证）
 */
@Slf4j
@RequestMapping("/mobile")
@RestController
@Validated
@RequiredArgsConstructor
@Tag(name = "13-移动端接口", description = "移动端H5/小程序公开接口，无需JWT认证")
public class MobileController {

    private final DessertService dessertService;
    private final MobileOrderService mobileOrderService;

    @Operation(summary = "商品列表", description = "获取移动端商品列表，支持按分类筛选（all/cake/bread/drink/dessert/icecream/snack/gift）")
    @GetMapping("/products")
    public Result listProducts(@Parameter(description = "商品分类") @RequestParam(defaultValue = "all") String category) {
        log.info("移动端查询商品, category={}", category);

        List<Dessert> list = "all".equals(category)
                ? dessertService.listOnSale()
                : dessertService.listByCategory(category);

        // 过滤已下架的商品，并转为移动端需要的格式
        List<Map<String, Object>> result = list.stream()
                .filter(MobileController::isOnSale)
                .map(MobileController::toMobileProduct)
                .collect(Collectors.toList());

        return Result.success(result);
    }

    @Operation(summary = "商品详情", description = "获取单个在售商品详情；已下架或不存在返回 404")
    @GetMapping("/products/{id}")
    public Result getProduct(@Parameter(description = "商品ID") @PathVariable Integer id) {
        Dessert dessert = dessertService.getById(id);
        if (dessert == null || !isOnSale(dessert)) {
            throw new NotFoundException("商品不存在或已下架");
        }
        return Result.success(toMobileProduct(dessert));
    }

    private static boolean isOnSale(Dessert d) {
        return d.getStatus() == null || d.getStatus() == 1;
    }

    private static Map<String, Object> toMobileProduct(Dessert d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("name", d.getName());
        m.put("category", d.getCategory());
        m.put("price", d.getPrice());
        m.put("originalPrice", d.getOriginalPrice());
        m.put("image", d.getImage());
        m.put("description", d.getDescription() != null ? d.getDescription() : "");
        m.put("icon", d.getIcon() != null ? d.getIcon() : "🍰");
        m.put("sales", d.getSales());
        m.put("stock", d.getStock());
        return m;
    }

    @Operation(summary = "分类列表", description = "获取移动端商品分类列表及各分类商品数量")
    @GetMapping("/categories")
    public Result listCategories() {
        log.info("移动端查询分类");
        List<Map<String, Object>> counts = dessertService.countByCategory();

        // 构建分类映射
        Map<String, String> catNames = new LinkedHashMap<>();
        catNames.put("cake", "蛋糕");
        catNames.put("bread", "面包");
        catNames.put("drink", "饮品");
        catNames.put("dessert", "甜点");
        catNames.put("icecream", "冰淇淋");
        catNames.put("snack", "小食");
        catNames.put("gift", "礼盒");

        List<Map<String, Object>> result = new ArrayList<>();
        result.add(Map.of("key", "all", "label", "全部"));
        catNames.forEach((key, label) -> {
            long cnt = counts.stream()
                    .filter(m -> key.equals(m.get("category")))
                    .mapToLong(m -> ((Number) m.get("count")).longValue())
                    .findFirst().orElse(0);
            result.add(Map.of("key", key, "label", label, "count", cnt));
        });

        return Result.success(result);
    }

    @Operation(summary = "顾客查单", description = "按订单号 + 下单手机号查询订单状态（两者都匹配才返回，仅返回订单本身字段）")
    @GetMapping("/orders/{orderNo}")
    public Result getOrder(@Parameter(description = "订单号") @PathVariable String orderNo,
                           @Parameter(description = "下单手机号") @RequestParam String phone) {
        Order order = mobileOrderService.findOrder(orderNo, phone);
        if (order == null) {
            throw new NotFoundException("未找到该订单，请核对订单号与手机号");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderNo", order.getOrderNo());
        m.put("status", order.getStatus());
        m.put("amount", order.getAmount());
        m.put("items", order.getItems());
        m.put("createTime", order.getCreateTime());
        return Result.success(m);
    }

    @Operation(summary = "移动端下单", description = "移动端用户提交订单，返回订单号")
    @PostMapping("/orders")
    public Result createOrder(@Valid @RequestBody MobileOrderRequest request) {
        log.info("移动端下单: customer={}, amount={}", request.getCustomerName(), request.getTotalAmount());
        Order order = mobileOrderService.createOrder(request);
        return Result.success(Map.of("orderNo", order.getOrderNo(), "amount", order.getAmount(), "message", "下单成功"));
    }
}
