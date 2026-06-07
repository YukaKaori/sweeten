package com.itheima.controller;

import com.itheima.annotation.LogOperation;
import com.itheima.pojo.*;
import com.itheima.service.DessertService;
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
public class DessertController {

    private final DessertService dessertService;

    /**
     * 分页查询甜品
     */
    @GetMapping
    public Result page(@Valid DessertQueryParam queryParam) {
        log.info("分页查询甜品:{}", queryParam);
        PageResult<Dessert> pageResult = dessertService.page(queryParam);
        return Result.success(pageResult);
    }

    /**
     * 根据分类查询甜品列表
     */
    @GetMapping("/category/{category}")
    public Result listByCategory(@PathVariable String category) {
        log.info("根据分类查询甜品:{}", category);
        List<Dessert> dessertList = dessertService.listByCategory(category);
        return Result.success(dessertList);
    }

    /**
     * 查询各分类数量
     */
    @GetMapping("/categoryCount")
    public Result countByCategory() {
        log.info("查询各分类数量");
        List<Map<String, Object>> countList = dessertService.countByCategory();
        return Result.success(countList);
    }

    /**
     * 根据ID查询甜品
     */
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        log.info("根据id查询甜品:{}", id);
        Dessert dessert = dessertService.getById(id);
        return Result.success(dessert);
    }

    /**
     * 新增甜品
     */
    @LogOperation
    @PostMapping
    public Result save(@Valid @RequestBody Dessert dessert) {
        log.info("新增甜品:{}", dessert.getName());
        dessertService.save(dessert);
        return Result.success();
    }

    /**
     * 修改甜品（含调价）
     */
    @LogOperation
    @PutMapping
    public Result update(@Valid @RequestBody Dessert dessert) {
        log.info("修改甜品,id:{}", dessert.getId());
        dessertService.update(dessert);
        return Result.success();
    }

    /**
     * 删除甜品
     */
    @LogOperation
    @DeleteMapping
    public Result delete(Integer id) {
        log.info("删除甜品,id:{}", id);
        dessertService.deleteById(id);
        return Result.success();
    }

    /**
     * 甜品上下架
     */
    @LogOperation
    @PutMapping("/status")
    public Result updateStatus(@RequestParam Integer id, @RequestParam Integer status) {
        log.info("甜品上下架,id:{},status:{}", id, status);
        Dessert dessert = new Dessert();
        dessert.setId(id);
        dessert.setStatus(status);
        dessertService.update(dessert);
        return Result.success();
    }
}
