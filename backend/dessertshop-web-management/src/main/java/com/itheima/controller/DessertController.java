package com.itheima.controller;

import com.itheima.annotation.LogOperation;
import com.itheima.pojo.*;
import com.itheima.service.DessertService;
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
@RequestMapping("/desserts")
@RestController
@Validated
@RequiredArgsConstructor
@Tag(name = "04-甜品管理", description = "甜品商品的增删改查与上下架管理接口")
public class DessertController {

    private final DessertService dessertService;

    @Operation(summary = "分页查询甜品", description = "支持按名称、分类、状态等条件分页查询甜品列表")
    @GetMapping
    public Result page(@Valid DessertQueryParam queryParam) {
        log.info("分页查询甜品:{}", queryParam);
        PageResult<Dessert> pageResult = dessertService.page(queryParam);
        return Result.success(pageResult);
    }

    @Operation(summary = "根据分类查询甜品", description = "根据甜品分类(cake/bread/drink/icecream/snack/gift)查询甜品列表")
    @GetMapping("/category/{category}")
    public Result listByCategory(@Parameter(description = "分类标识") @PathVariable String category) {
        log.info("根据分类查询甜品:{}", category);
        List<Dessert> dessertList = dessertService.listByCategory(category);
        return Result.success(dessertList);
    }

    @Operation(summary = "查询各分类数量", description = "统计各甜品分类下的商品数量")
    @GetMapping("/categoryCount")
    public Result countByCategory() {
        log.info("查询各分类数量");
        List<Map<String, Object>> countList = dessertService.countByCategory();
        return Result.success(countList);
    }

    @Operation(summary = "根据ID查询甜品", description = "根据甜品ID查询单个甜品的详细信息")
    @GetMapping("/{id}")
    public Result getById(@Parameter(description = "甜品ID") @PathVariable Integer id) {
        log.info("根据id查询甜品:{}", id);
        Dessert dessert = dessertService.getById(id);
        return Result.success(dessert);
    }

    @Operation(summary = "新增甜品", description = "新增一个甜品商品信息")
    @LogOperation
    @PostMapping
    public Result save(@Valid @RequestBody Dessert dessert) {
        log.info("新增甜品:{}", dessert.getName());
        dessertService.save(dessert);
        return Result.success();
    }

    @Operation(summary = "修改甜品", description = "修改甜品信息（含价格调整）")
    @LogOperation
    @PutMapping
    public Result update(@Valid @RequestBody Dessert dessert) {
        log.info("修改甜品,id:{}", dessert.getId());
        dessertService.update(dessert);
        return Result.success();
    }

    @Operation(summary = "删除甜品", description = "根据ID删除指定甜品")
    @LogOperation
    @DeleteMapping
    public Result delete(@Parameter(description = "甜品ID") Integer id) {
        log.info("删除甜品,id:{}", id);
        dessertService.deleteById(id);
        return Result.success();
    }

    @Operation(summary = "甜品上下架", description = "设置甜品的上架(1)或下架(0)状态")
    @LogOperation
    @PutMapping("/status")
    public Result updateStatus(@Parameter(description = "甜品ID") @RequestParam Integer id,
                               @Parameter(description = "状态：1-上架，0-下架") @RequestParam Integer status) {
        log.info("甜品上下架,id:{},status:{}", id, status);
        Dessert dessert = new Dessert();
        dessert.setId(id);
        dessert.setStatus(status);
        dessertService.update(dessert);
        return Result.success();
    }
}
