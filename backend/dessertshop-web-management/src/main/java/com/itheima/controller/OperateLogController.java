package com.itheima.controller;

import com.itheima.pojo.*;
import com.itheima.service.EmpLogService;
import com.itheima.service.OperateLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@Tag(name = "08-日志管理", description = "操作审计日志与员工日志查询接口")
public class OperateLogController {

    private final OperateLogService operateLogService;

    private final EmpLogService empLogService;

    @Operation(summary = "分页查询操作日志", description = "支持按操作人、类型、时间范围等条件分页查询操作审计日志")
    @GetMapping("/operateLogs")
    public Result pageOperateLog(@Valid OperateLogQueryParam queryParam){
        log.info("分页查询操作日志:{}",queryParam);
        PageResult<OperateLog> pageResult = operateLogService.page(queryParam);
        return Result.success(pageResult);
    }

    @Operation(summary = "根据ID查询操作日志详情", description = "查询单条操作日志的详细信息")
    @GetMapping("/operateLogs/{id}")
    public Result getOperateLogById(@Parameter(description = "日志ID") @PathVariable Integer id){
        log.info("根据id查询操作日志:{}",id);
        OperateLog operateLog = operateLogService.getById(id);
        return Result.success(operateLog);
    }

    @Operation(summary = "分页查询员工日志", description = "分页查询员工登录登出日志")
    @GetMapping("/empLogs")
    public Result pageEmpLog(@Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
                             @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize){
        log.info("分页查询员工日志,page:{},pageSize:{}",page,pageSize);
        PageResult<EmpLog> pageResult = empLogService.page(page, pageSize);
        return Result.success(pageResult);
    }
}
