package com.itheima.controller;

import com.itheima.annotation.LogOperation;
import com.itheima.pojo.Dept;
import com.itheima.pojo.Result;
import com.itheima.service.DeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequestMapping("/depts")
@RestController
@RequiredArgsConstructor
@Tag(name = "02-部门管理", description = "组织架构部门信息的增删改查接口")
public class DeptController {

    private final DeptService deptService;

    @Operation(summary = "查询部门列表", description = "获取所有部门信息列表")
    @GetMapping
    public Result list() {
        log.info("查询部门列表");
        List<Dept> deptList = deptService.findAll();
        return Result.success(deptList);
    }

    @Operation(summary = "删除部门", description = "根据部门ID删除指定部门")
    @LogOperation
    @DeleteMapping
    public Result delete(@Parameter(description = "部门ID") Integer id) {
        log.info("根据id删除部门,id:{}", id);
        deptService.deleteById(id);
        return Result.success();
    }

    @Operation(summary = "新增部门", description = "新增一个部门信息")
    @LogOperation
    @PostMapping
    public Result save(@Valid @RequestBody Dept dept) {
        log.info("新增部门,name:{}", dept.getName());
        deptService.save(dept);
        return Result.success();
    }

    @Operation(summary = "根据ID查询部门", description = "根据部门ID查询单个部门详细信息")
    @GetMapping("/{id}")
    public Result getById(@Parameter(description = "部门ID") @PathVariable Integer id) {
        log.info("根据id查询部门,id:{}", id);
        Dept dept = deptService.getById(id);
        return Result.success(dept);
    }

    @Operation(summary = "修改部门", description = "根据部门ID修改部门信息")
    @LogOperation
    @PutMapping
    public Result update(@Valid @RequestBody Dept dept) {
        log.info("修改部门,id:{}", dept.getId());
        deptService.update(dept);
        return Result.success();
    }
}
