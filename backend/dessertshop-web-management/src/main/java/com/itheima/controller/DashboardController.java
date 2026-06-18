package com.itheima.controller;

import com.itheima.pojo.Result;
import com.itheima.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequestMapping("/dashboard")
@RestController
@RequiredArgsConstructor
@Tag(name = "12-仪表盘", description = "Dashboard数据看板与数据报表分析接口")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "仪表盘统计数据", description = "获取仪表盘统计卡片数据、库存预警、营销活动、评价等综合信息")
    @GetMapping("/stats")
    public Result stats() {
        log.info("查询仪表盘统计数据");
        return Result.success(dashboardService.getStats());
    }

    @Operation(summary = "营收趋势图表数据", description = "获取营收趋势数据，支持day/week/month范围")
    @GetMapping("/revenue")
    public Result revenue(@Parameter(description = "时间范围：day/week/month") @RequestParam(defaultValue = "month") String range) {
        log.info("查询营收趋势数据,range:{}", range);
        return Result.success(dashboardService.getRevenueTrend(range));
    }

    @Operation(summary = "热销甜品排行", description = "获取销量排行前N名的甜品列表")
    @GetMapping("/ranking")
    public Result ranking(@Parameter(description = "返回排行数量") @RequestParam(defaultValue = "5") Integer limit) {
        log.info("查询热销甜品排行,limit:{}", limit);
        return Result.success(dashboardService.getTopSalesRanking(limit));
    }

    @Operation(summary = "库存预警", description = "获取当前库存量低于安全阈值的物料预警列表")
    @GetMapping("/stock-alert")
    public Result stockAlert() {
        log.info("查询库存预警");
        return Result.success(dashboardService.getStockAlerts());
    }

    // ---------- 数据报表 API ----------

    @Operation(summary = "报表统计卡片", description = "获取指定日期范围内的报表统计卡片数据")
    @GetMapping("/report-stats")
    public Result reportStats(@Parameter(description = "开始日期") @RequestParam(defaultValue = "") String begin,
                              @Parameter(description = "结束日期") @RequestParam(defaultValue = "") String end) {
        log.info("查询报表统计卡片,begin:{},end:{}", begin, end);
        return Result.success(dashboardService.getReportStats(begin, end));
    }

    @Operation(summary = "报表营收趋势", description = "获取指定日期范围内的营收趋势图表数据")
    @GetMapping("/report-revenue")
    public Result reportRevenue(@Parameter(description = "开始日期") @RequestParam(defaultValue = "") String begin,
                                @Parameter(description = "结束日期") @RequestParam(defaultValue = "") String end) {
        log.info("查询报表营收趋势,begin:{},end:{}", begin, end);
        return Result.success(dashboardService.getReportRevenue(begin, end));
    }

    @Operation(summary = "报表分类销售占比", description = "获取指定日期范围内的各分类销售占比数据")
    @GetMapping("/report-category")
    public Result reportCategory(@Parameter(description = "开始日期") @RequestParam(defaultValue = "") String begin,
                                 @Parameter(description = "结束日期") @RequestParam(defaultValue = "") String end) {
        log.info("查询报表分类销售,begin:{},end:{}", begin, end);
        return Result.success(dashboardService.getReportCategory(begin, end));
    }

    @Operation(summary = "报表工作日订单分布", description = "获取指定日期范围内的工作日-周末订单分布数据")
    @GetMapping("/report-weekday")
    public Result reportWeekday(@Parameter(description = "开始日期") @RequestParam(defaultValue = "") String begin,
                                @Parameter(description = "结束日期") @RequestParam(defaultValue = "") String end) {
        log.info("查询报表工作日分布,begin:{},end:{}", begin, end);
        return Result.success(dashboardService.getReportWeekday(begin, end));
    }

    @Operation(summary = "报表热销排行", description = "获取指定日期范围内的热销甜品排行")
    @GetMapping("/report-ranking")
    public Result reportRanking(@Parameter(description = "开始日期") @RequestParam(defaultValue = "") String begin,
                                @Parameter(description = "结束日期") @RequestParam(defaultValue = "") String end,
                                @Parameter(description = "返回排行数量") @RequestParam(defaultValue = "7") Integer limit) {
        log.info("查询报表排行,limit:{},begin:{},end:{}", limit, begin, end);
        return Result.success(dashboardService.getReportRanking(begin, end, limit));
    }
}
