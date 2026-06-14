package com.itheima.controller;

import com.itheima.annotation.LogOperation;
import com.itheima.pojo.*;
import com.itheima.service.InventoryService;
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
public class InventoryController {

    private final InventoryService inventoryService;

    /** 分页查询库存 */
    @GetMapping
    public Result page(@Valid InventoryQueryParam queryParam) {
        log.info("分页查询库存:{}", queryParam);
        PageResult<Inventory> pageResult = inventoryService.page(queryParam);
        return Result.success(pageResult);
    }

    /** 库存预警 */
    @GetMapping("/alerts")
    public Result alerts() {
        log.info("查询库存预警");
        List<Inventory> alerts = inventoryService.getAlerts();
        return Result.success(alerts);
    }

    /** 新增物料 */
    @LogOperation
    @PostMapping
    public Result save(@Valid @RequestBody Inventory inventory) {
        log.info("新增库存物料:{}", inventory.getName());
        inventoryService.save(inventory);
        return Result.success();
    }

    /** 更新物料 */
    @LogOperation
    @PutMapping
    public Result update(@Valid @RequestBody Inventory inventory) {
        log.info("更新库存物料,id:{}", inventory.getId());
        inventoryService.update(inventory);
        return Result.success();
    }

    /** 删除物料 */
    @LogOperation
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        log.info("删除库存物料,id:{}", id);
        inventoryService.deleteById(id);
        return Result.success();
    }

    /** 库存调整（出入库） */
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
