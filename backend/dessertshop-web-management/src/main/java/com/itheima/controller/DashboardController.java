package com.itheima.controller;

import com.itheima.pojo.Result;
import com.itheima.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequestMapping("/dashboard")
@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /** 仪表盘统计数据（统计卡片 + 库存预警 + 营销活动 + 评价） */
    @GetMapping("/stats")
    public Result stats() {
        log.info("查询仪表盘统计数据");
        return Result.success(dashboardService.getStats());
    }

    /** 营收趋势图表数据 */
    @GetMapping("/revenue")
    public Result revenue(@RequestParam(defaultValue = "month") String range) {
        log.info("查询营收趋势数据,range:{}", range);
        return Result.success(dashboardService.getRevenueTrend(range));
    }

    /** 热销甜品排行 */
    @GetMapping("/ranking")
    public Result ranking(@RequestParam(defaultValue = "5") Integer limit) {
        log.info("查询热销甜品排行,limit:{}", limit);
        return Result.success(dashboardService.getTopSalesRanking(limit));
    }

    /** 库存预警 */
    @GetMapping("/stock-alert")
    public Result stockAlert() {
        log.info("查询库存预警");
        return Result.success(dashboardService.getStockAlerts());
    }
}
