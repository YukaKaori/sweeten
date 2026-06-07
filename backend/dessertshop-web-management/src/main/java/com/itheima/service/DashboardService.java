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
}
