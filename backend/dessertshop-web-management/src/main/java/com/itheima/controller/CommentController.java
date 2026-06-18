package com.itheima.controller;

import com.itheima.annotation.LogOperation;
import com.itheima.pojo.Comment;
import com.itheima.pojo.PageResult;
import com.itheima.pojo.Result;
import com.itheima.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequestMapping("/comments")
@RestController
@Validated
@RequiredArgsConstructor
@Tag(name = "11-评价管理", description = "甜品评价的查询、新增与删除管理接口")
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "分页查询评论", description = "根据甜品ID分页查询其评论列表")
    @GetMapping
    public Result page(@Parameter(description = "甜品ID") @RequestParam Integer dessertId,
                       @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
                       @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("查询评论,dessertId:{},page:{},pageSize:{}", dessertId, page, pageSize);
        PageResult<Comment> pageResult = commentService.pageByDessertId(dessertId, page, pageSize);
        return Result.success(pageResult);
    }

    @Operation(summary = "新增评论", description = "为指定甜品新增一条评论")
    @LogOperation
    @PostMapping
    public Result save(@Valid @RequestBody Comment comment) {
        log.info("新增评论,dessertId:{},rating:{}", comment.getDessertId(), comment.getRating());
        commentService.save(comment);
        return Result.success();
    }

    @Operation(summary = "删除评论", description = "根据ID删除指定评论")
    @LogOperation
    @DeleteMapping("/{id}")
    public Result delete(@Parameter(description = "评论ID") @PathVariable Integer id) {
        log.info("删除评论,id:{}", id);
        commentService.deleteById(id);
        return Result.success();
    }
}
