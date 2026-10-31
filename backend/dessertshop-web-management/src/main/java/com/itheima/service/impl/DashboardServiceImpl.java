package com.itheima.service.impl;

import com.itheima.mapper.DessertMapper;
import com.itheima.mapper.OrderMapper;
import com.itheima.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final OrderMapper orderMapper;
    private final DessertMapper dessertMapper;

    @Override
    @Cacheable(value = "dashboard:stats", key = "'overview'")
    public Map<String, Object> getStats() {
        Map<String, Object> data = new HashMap<>();

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        Double revenue = orderMapper.sumRevenue(monthStart, now);
        Long totalOrders = orderMapper.countOrders(monthStart, now);
        Long completedOrders = orderMapper.countCompletedOrders(monthStart, now);
        Double avgOrder = totalOrders > 0 ? Math.round(revenue / totalOrders * 100.0) / 100.0 : 0;
        Double completionRate = totalOrders > 0 ? Math.round(completedOrders * 10000.0 / totalOrders) / 100.0 : 0;

        List<Map<String, Object>> statsCards = new ArrayList<>();
        statsCards.add(buildCard("本月营收", revenue, "¥", "", 2, "+12.5%", "up", 78, "较上月增长"));
        statsCards.add(buildCard("本月订单", totalOrders.doubleValue(), "", " 单", 0, "+8.3%", "up", 65, "订单量稳步增长"));
        statsCards.add(buildCard("客单价", avgOrder, "¥", "", 2, "+5.2%", "up", 52, "消费水平提升"));
        statsCards.add(buildCard("完成率", completionRate, "", "%", 1,
                completionRate >= 80 ? "+达标" : "-未达标",
                completionRate >= 80 ? "up" : "down",
                completionRate.intValue(), "订单完成情况"));

        data.put("statsCards", statsCards);
        data.put("stockAlerts", dessertMapper.selectLowStockAlerts(20));
        data.put("campaigns", new ArrayList<>());
        data.put("reviews", new ArrayList<>());

        return data;
    }

    @Override
    @Cacheable(value = "dashboard:stats", key = "'trend_' + #range")
    public Map<String, Object> getRevenueTrend(String range) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start;
        DateTimeFormatter labelFmt;

        if ("week".equals(range)) {
            start = end.minusDays(6).toLocalDate().atStartOfDay();
        } else {
            start = end.minusDays(29).toLocalDate().atStartOfDay();
        }
        labelFmt = DateTimeFormatter.ofPattern("MM-dd");

        List<Map<String, Object>> dailyData = orderMapper.selectDailyRevenue(start, end);

        List<String> labels = new ArrayList<>();
        List<Double> revenueList = new ArrayList<>();
        List<Integer> ordersList = new ArrayList<>();

        Map<String, Map<String, Object>> dataMap = new LinkedHashMap<>();
        for (Map<String, Object> row : dailyData) {
            dataMap.put(row.get("d").toString(), row);
        }

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

        return result;
    }

    @Override
    public List<Map<String, Object>> getTopSalesRanking(Integer limit) {
        return dessertMapper.selectTopSalesRanking(limit);
    }

    @Override
    public List<Map<String, Object>> getStockAlerts() {
        return dessertMapper.selectLowStockAlerts(20);
    }

    @Override
    public List<Map<String, Object>> getReportStats(String begin, String end) {
        LocalDateTime start = parseDateTime(begin, true);
        LocalDateTime finish = parseDateTime(end, false);

        Double revenue = orderMapper.sumRevenue(start, finish);
        Long totalOrders = orderMapper.countOrders(start, finish);
        Long completedOrders = orderMapper.countCompletedOrders(start, finish);
        Double avgOrder = totalOrders > 0 ? Math.round(revenue / totalOrders * 100.0) / 100.0 : 0;
        Double repurchaseRate = totalOrders > 0 ? Math.round(completedOrders * 10000.0 / totalOrders) / 100.0 : 0;

        List<Map<String, Object>> stats = new ArrayList<>();
        stats.add(buildCard("营收总额", revenue, "¥", "", 2, "+增长", "up", 78, "期间营收"));
        stats.add(buildCard("订单总数", totalOrders.doubleValue(), "", " 单", 0, "+增长", "up", 65, "期间订单"));
        stats.add(buildCard("客单价", avgOrder, "¥", "", 2, "稳定", "up", 52, "平均消费"));
        stats.add(buildCard("完成率", repurchaseRate, "", "%", 1,
                repurchaseRate >= 80 ? "+达标" : "待提升",
                repurchaseRate >= 80 ? "up" : "down",
                repurchaseRate.intValue(), "订单完成率"));
        return stats;
    }

    @Override
    public Map<String, Object> getReportRevenue(String begin, String end) {
        LocalDateTime start = parseDateTime(begin, true);
        LocalDateTime finish = parseDateTime(end, false);
        return getRevenueTrendData(start, finish);
    }

    @Override
    public List<Map<String, Object>> getReportCategory(String begin, String end) {
        List<Map<String, Object>> categoryData = dessertMapper.countByCategory();
        String[] colorMap = {"cake", "bread", "drink", "dessert", "icecream"};
        String[] colors = {"#e8637a", "#f0a35c", "#6b8cce", "#5cb88a", "#a78bfa"};

        for (int i = 0; i < categoryData.size(); i++) {
            Map<String, Object> item = categoryData.get(i);
            String category = (String) item.get("category");
            for (int j = 0; j < colorMap.length; j++) {
                if (colorMap[j].equals(category)) {
                    Map<String, Object> style = new java.util.LinkedHashMap<>();
                    style.put("color", colors[j]);
                    item.put("itemStyle", style);
                    break;
                }
            }
        }
        return categoryData;
    }

    @Override
    public Map<String, Object> getReportWeekday(String begin, String end) {
        LocalDateTime start = parseDateTime(begin, true);
        LocalDateTime finish = parseDateTime(end, false);

        List<java.util.Map<String, Object>> weekdayData = orderMapper.selectWeekdayOrders(start, finish);

        String[] weekdayNames = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        int[] orders = new int[7];
        for (java.util.Map<String, Object> row : weekdayData) {
            int wd = ((Number) row.get("wd")).intValue() - 1; // MySQL: 1=Sun..7=Sat
            // Convert to 0=Mon
            if (wd < 0 || wd > 6) continue;
            int mondayBased = (wd + 6) % 7; // 0=Sun→6, 1=Mon→0, ..., 6=Sat→5
            orders[mondayBased] = ((Number) row.get("cnt")).intValue();
        }

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        List<String> labels = new ArrayList<>();
        List<Integer> data = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            labels.add(weekdayNames[i]);
            data.add(orders[i]);
        }
        result.put("xAxis", labels);
        result.put("orders", data);
        return result;
    }

    @Override
    public List<Map<String, Object>> getReportRanking(String begin, String end, Integer limit) {
        return dessertMapper.selectTopSalesRanking(limit);
    }

    private LocalDateTime parseDateTime(String dateStr, boolean isStart) {
        if (dateStr == null || dateStr.isEmpty()) {
            return isStart
                    ? java.time.LocalDate.now().withDayOfMonth(1).atStartOfDay()
                    : java.time.LocalDateTime.now();
        }
        try {
            if (dateStr.length() == 10) {
                java.time.LocalDate d = java.time.LocalDate.parse(dateStr);
                return isStart ? d.atStartOfDay() : d.plusDays(1).atStartOfDay();
            }
            return java.time.LocalDateTime.parse(dateStr);
        } catch (Exception e) {
            return isStart
                    ? java.time.LocalDate.now().withDayOfMonth(1).atStartOfDay()
                    : java.time.LocalDateTime.now();
        }
    }

    private Map<String, Object> getRevenueTrendData(LocalDateTime start, LocalDateTime finish) {
        java.time.format.DateTimeFormatter labelFmt =
                java.time.format.DateTimeFormatter.ofPattern("MM-dd");

        List<Map<String, Object>> dailyData = orderMapper.selectDailyRevenue(start, finish);

        List<String> labels = new ArrayList<>();
        List<Double> revenueList = new ArrayList<>();
        List<Integer> ordersList = new ArrayList<>();

        Map<String, Map<String, Object>> dataMap = new LinkedHashMap<>();
        for (Map<String, Object> row : dailyData) {
            dataMap.put(row.get("d").toString(), row);
        }

        java.time.LocalDate current = start.toLocalDate();
        java.time.LocalDate endDay = finish.toLocalDate();
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

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("xAxis", labels);
        result.put("revenue", revenueList);
        result.put("orders", ordersList);
        return result;
    }

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
