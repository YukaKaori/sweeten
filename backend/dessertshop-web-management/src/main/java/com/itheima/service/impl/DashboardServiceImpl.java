package com.itheima.service.impl;

import com.itheima.mapper.DessertMapper;
import com.itheima.mapper.OrderMapper;
import com.itheima.service.DashboardService;
import lombok.RequiredArgsConstructor;
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
