package com.itheima.controller;

import com.itheima.annotation.LogOperation;
import com.itheima.pojo.*;
import com.itheima.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RequestMapping("/inventory")
@RestController
@Validated
@RequiredArgsConstructor
@Tag(name = "09-库存管理", description = "原料物料的库存增删改查与出入库调整接口")
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "分页查询库存", description = "支持按物料名称、分类、库存状态等条件分页查询库存列表")
    @GetMapping
    public Result page(@Valid InventoryQueryParam queryParam) {
        log.info("分页查询库存:{}", queryParam);
        PageResult<Inventory> pageResult = inventoryService.page(queryParam);
        return Result.success(pageResult);
    }

    @Operation(summary = "库存预警", description = "查询库存量低于安全库存阈值的物料列表")
    @GetMapping("/alerts")
    public Result alerts() {
        log.info("查询库存预警");
        List<Inventory> alerts = inventoryService.getAlerts();
        return Result.success(alerts);
    }

    @Operation(summary = "新增物料", description = "新增一个库存物料信息")
    @LogOperation
    @PostMapping
    public Result save(@Valid @RequestBody Inventory inventory) {
        log.info("新增库存物料:{}", inventory.getName());
        inventoryService.save(inventory);
        return Result.success();
    }

    @Operation(summary = "更新物料", description = "更新库存物料的基本信息")
    @LogOperation
    @PutMapping
    public Result update(@Valid @RequestBody Inventory inventory) {
        log.info("更新库存物料,id:{}", inventory.getId());
        inventoryService.update(inventory);
        return Result.success();
    }

    @Operation(summary = "删除物料", description = "根据ID删除指定库存物料")
    @LogOperation
    @DeleteMapping("/{id}")
    public Result delete(@Parameter(description = "物料ID") @PathVariable Integer id) {
        log.info("删除库存物料,id:{}", id);
        inventoryService.deleteById(id);
        return Result.success();
    }

    @Operation(summary = "库存调整", description = "执行出入库操作，调整物料的库存数量")
    @LogOperation
    @PutMapping("/stock")
    public Result adjustStock(@RequestBody Map<String, Object> params) {
        Integer id = (Integer) params.get("id");
        Integer quantity = (Integer) params.get("quantity");
        String remark = (String) params.getOrDefault("remark", "");
        log.info("库存调整,id:{},quantity:{},remark:{}", id, quantity, remark);
        inventoryService.adjustStock(id, quantity, remark);
        return Result.success();
    }
}
