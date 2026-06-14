package com.itheima.service;

import java.util.List;
import java.util.Map;

/**
 * 仪表盘数据服务接口
 */
public interface DashboardService {

    /** 统计卡片数据（营收、订单、客单价、完成率）+ 库存预警 */
    Map<String, Object> getStats();

    /** 营收趋势图表数据（近7天/30天） */
    Map<String, Object> getRevenueTrend(String range);

    /** 热销甜品排行 */
    List<Map<String, Object>> getTopSalesRanking(Integer limit);

    /** 库存预警 */
    List<Map<String, Object>> getStockAlerts();

    // ---------- 数据报表 ----------
    List<Map<String, Object>> getReportStats(String begin, String end);
    Map<String, Object> getReportRevenue(String begin, String end);
    List<Map<String, Object>> getReportCategory(String begin, String end);
    Map<String, Object> getReportWeekday(String begin, String end);
    List<Map<String, Object>> getReportRanking(String begin, String end, Integer limit);
}
