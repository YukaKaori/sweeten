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

    // ---------- 数据报表 API ----------

    /** 报表统计卡片 */
    @GetMapping("/report-stats")
    public Result reportStats(@RequestParam(defaultValue = "") String begin,
                              @RequestParam(defaultValue = "") String end) {
        log.info("查询报表统计卡片,begin:{},end:{}", begin, end);
        return Result.success(dashboardService.getReportStats(begin, end));
    }

    /** 报表营收趋势 */
    @GetMapping("/report-revenue")
    public Result reportRevenue(@RequestParam(defaultValue = "") String begin,
                                @RequestParam(defaultValue = "") String end) {
        log.info("查询报表营收趋势,begin:{},end:{}", begin, end);
        return Result.success(dashboardService.getReportRevenue(begin, end));
    }

    /** 报表分类销售占比 */
    @GetMapping("/report-category")
    public Result reportCategory(@RequestParam(defaultValue = "") String begin,
                                 @RequestParam(defaultValue = "") String end) {
        log.info("查询报表分类销售,begin:{},end:{}", begin, end);
        return Result.success(dashboardService.getReportCategory(begin, end));
    }

    /** 报表工作日订单分布 */
    @GetMapping("/report-weekday")
    public Result reportWeekday(@RequestParam(defaultValue = "") String begin,
                                @RequestParam(defaultValue = "") String end) {
        log.info("查询报表工作日分布,begin:{},end:{}", begin, end);
        return Result.success(dashboardService.getReportWeekday(begin, end));
    }

    /** 报表热销排行 */
    @GetMapping("/report-ranking")
    public Result reportRanking(@RequestParam(defaultValue = "") String begin,
                                @RequestParam(defaultValue = "") String end,
                                @RequestParam(defaultValue = "7") Integer limit) {
        log.info("查询报表排行,limit:{},begin:{},end:{}", limit, begin, end);
        return Result.success(dashboardService.getReportRanking(begin, end, limit));
    }
}
