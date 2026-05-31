package com.itheima.controller;

import com.itheima.mapper.DessertMapper;
import com.itheima.mapper.OrderMapper;
import com.itheima.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@RequestMapping("/dashboard")
@RestController
public class DashboardController {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private DessertMapper dessertMapper;

    /**
     * 仪表盘统计数据（统计卡片 + 库存预警 + 营销活动 + 评价）
     */
    @GetMapping("/stats")
    public Result stats() {
        log.info("查询仪表盘统计数据");
        Map<String, Object> data = new HashMap<>();

        // 本月时间范围
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        // 查询统计数据
        Double revenue = orderMapper.sumRevenue(monthStart, now);
        Long totalOrders = orderMapper.countOrders(monthStart, now);
        Long completedOrders = orderMapper.countCompletedOrders(monthStart, now);
        Double avgOrder = totalOrders > 0 ? Math.round(revenue / totalOrders * 100.0) / 100.0 : 0;
        Double completionRate = totalOrders > 0 ? Math.round(completedOrders * 10000.0 / totalOrders) / 100.0 : 0;

        // 构建统计卡片
        List<Map<String, Object>> statsCards = new ArrayList<>();
        statsCards.add(buildCard("本月营收", revenue, "¥", "", 2, "+12.5%", "up", 78, "较上月增长"));
        statsCards.add(buildCard("本月订单", totalOrders.doubleValue(), "", " 单", 0, "+8.3%", "up", 65, "订单量稳步增长"));
        statsCards.add(buildCard("客单价", avgOrder, "¥", "", 2, "+5.2%", "up", 52, "消费水平提升"));
        statsCards.add(buildCard("完成率", completionRate, "", "%", 1, completionRate >= 80 ? "+达标" : "-未达标", completionRate >= 80 ? "up" : "down", completionRate.intValue(), "订单完成情况"));

        data.put("statsCards", statsCards);

        // 库存预警
        data.put("stockAlerts", dessertMapper.selectLowStockAlerts(20));

        // 营销活动（前端有硬编码 fallback，返回空列表）
        data.put("campaigns", new ArrayList<>());

        // 评价（前端有硬编码 fallback，返回空列表）
        data.put("reviews", new ArrayList<>());

        return Result.success(data);
    }

    /**
     * 营收趋势图表数据
     */
    @GetMapping("/revenue")
    public Result revenue(@RequestParam(defaultValue = "month") String range) {
        log.info("查询营收趋势数据,range:{}", range);

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start;
        DateTimeFormatter labelFmt;

        if ("week".equals(range)) {
            start = end.minusDays(6).toLocalDate().atStartOfDay();
            labelFmt = DateTimeFormatter.ofPattern("MM-dd");
        } else {
            start = end.minusDays(29).toLocalDate().atStartOfDay();
            labelFmt = DateTimeFormatter.ofPattern("MM-dd");
        }

        // 查询每日营收数据
        List<Map<String, Object>> dailyData = orderMapper.selectDailyRevenue(start, end);

        // 构建日期范围内所有日期的列表
        List<String> labels = new ArrayList<>();
        List<Double> revenueList = new ArrayList<>();
        List<Integer> ordersList = new ArrayList<>();

        // 将查询结果转为 Map 方便查找
        Map<String, Map<String, Object>> dataMap = new LinkedHashMap<>();
        for (Map<String, Object> row : dailyData) {
            String dateStr = row.get("d").toString();
            dataMap.put(dateStr, row);
        }

        // 填充每一天的数据（无数据的日期填0）
        LocalDate current = start.toLocalDate();
        LocalDate endDay = end.toLocalDate();
        while (!current.isAfter(endDay)) {
            labels.add(current.format(labelFmt));
            String dateKey = current.toString();
            if (dataMap.containsKey(dateKey)) {
                Map<String, Object> row = dataMap.get(dateKey);
                revenueList.add(((Number) row.get("revenue")).doubleValue());
                ordersList.add(((Number) row.get("orders")).intValue());
            } else {
                revenueList.add(0.0);
                ordersList.add(0);
            }
            current = current.plusDays(1);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("revenue", revenueList);
        result.put("orders", ordersList);

        return Result.success(result);
    }

    /**
     * 热销甜品排行
     */
    @GetMapping("/ranking")
    public Result ranking(@RequestParam(defaultValue = "5") Integer limit) {
        log.info("查询热销甜品排行,limit:{}", limit);
        List<Map<String, Object>> rankingList = dessertMapper.selectTopSalesRanking(limit);
        return Result.success(rankingList);
    }

    /**
     * 库存预警
     */
    @GetMapping("/stock-alert")
    public Result stockAlert() {
        log.info("查询库存预警");
        List<Map<String, Object>> alerts = dessertMapper.selectLowStockAlerts(20);
        return Result.success(alerts);
    }

    /**
     * 构建统计卡片数据
     */
    private Map<String, Object> buildCard(String title, Double value, String prefix, String suffix,
                                           Integer decimals, String trend, String trendType,
                                           Integer progress, String desc) {
        Map<String, Object> card = new HashMap<>();
        card.put("title", title);
        card.put("value", value);
        card.put("prefix", prefix);
        card.put("suffix", suffix);
        card.put("decimals", decimals);
        card.put("trend", trend);
        card.put("trendType", trendType);
        card.put("progress", progress);
        card.put("desc", desc);
        return card;
    }
}
