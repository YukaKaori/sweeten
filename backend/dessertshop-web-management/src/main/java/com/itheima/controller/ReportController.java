package com.itheima.controller;

import com.itheima.pojo.JobOption;
import com.itheima.pojo.Result;
import com.itheima.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RequestMapping("/report")
@RestController
@RequiredArgsConstructor
@Tag(name = "06-数据报表", description = "员工数据统计报表相关接口")
public class ReportController {
    private final ReportService reportService;

    @Operation(summary = "统计各职位员工人数", description = "按职位维度统计员工人数分布")
    @GetMapping("/empJobData")
    public Result getEmpJobData(){
        log.info("统计各个职位的员工人数");
        JobOption jopOption = reportService.getEmpJobDate();
        return Result.success(jopOption);
    }
    @Operation(summary = "统计员工性别分布", description = "按性别维度统计员工人数")
    @GetMapping("/empGenderData")
    public Result getEmpGenderData(){
        log.info("统计员工性别信息");
        List<Map> genderList = reportService.getEmpGenderData();
        return Result.success(genderList);
    }
}
