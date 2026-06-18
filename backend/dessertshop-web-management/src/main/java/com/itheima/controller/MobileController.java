package com.itheima.controller;

import com.itheima.pojo.Dessert;
import com.itheima.pojo.MobileOrderRequest;
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

    @Operation(summary = "商品列表", description = "获取移动端商品列表，支持按分类筛选（all/cake/bread/drink/icecream/snack/gift）")
    @GetMapping("/products")
    public Result listProducts(@Parameter(description = "商品分类") @RequestParam(defaultValue = "all") String category) {
        log.info("移动端查询商品, category={}", category);

        List<Dessert> list;
        if ("all".equals(category)) {
            list = getAllOnSale();
        } else {
            list = dessertService.listByCategory(category);
        }

        // 过滤已下架的商品
        list = list.stream()
                .filter(d -> d.getStatus() == null || d.getStatus() == 1)
                .collect(Collectors.toList());

        // 转为移动端需要的格式
        List<Map<String, Object>> result = list.stream().map(d -> {
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
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 获取所有上架商品
     */
    private List<Dessert> getAllOnSale() {
        // 逐个分类查询再合并
        String[] categories = {"cake", "bread", "drink", "icecream", "snack", "gift"};
        List<Dessert> all = new ArrayList<>();
        for (String cat : categories) {
            all.addAll(dessertService.listByCategory(cat));
        }
        return all;
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

    @Operation(summary = "移动端下单", description = "移动端用户提交订单，返回订单号")
    @PostMapping("/orders")
    public Result createOrder(@Valid @RequestBody MobileOrderRequest request) {
        log.info("移动端下单: customer={}, amount={}", request.getCustomerName(), request.getTotalAmount());
        String orderNo = mobileOrderService.createOrder(request);
        return Result.success(Map.of("orderNo", orderNo, "message", "下单成功"));
    }
}
