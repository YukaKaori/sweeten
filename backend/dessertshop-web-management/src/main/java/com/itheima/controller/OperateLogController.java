package com.itheima.controller;

import com.itheima.pojo.*;
import com.itheima.service.EmpLogService;
import com.itheima.service.OperateLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
public class OperateLogController {

    @Autowired
    private OperateLogService operateLogService;

    @Autowired
    private EmpLogService empLogService;

    /**
     * 分页查询操作日志
     */
    @GetMapping("/operateLogs")
    public Result pageOperateLog(OperateLogQueryParam queryParam){
        log.info("分页查询操作日志:{}",queryParam);
        PageResult<OperateLog> pageResult = operateLogService.page(queryParam);
        return Result.success(pageResult);
    }

    /**
     * 根据ID查询操作日志详情
     */
    @GetMapping("/operateLogs/{id}")
    public Result getOperateLogById(@PathVariable Integer id){
        log.info("根据id查询操作日志:{}",id);
        OperateLog operateLog = operateLogService.getById(id);
        return Result.success(operateLog);
    }

    /**
     * 分页查询员工日志
     */
    @GetMapping("/empLogs")
    public Result pageEmpLog(@RequestParam(defaultValue = "1") Integer page,
                             @RequestParam(defaultValue = "10") Integer pageSize){
        log.info("分页查询员工日志,page:{},pageSize:{}",page,pageSize);
        PageResult<EmpLog> pageResult = empLogService.page(page, pageSize);
        return Result.success(pageResult);
    }
}
