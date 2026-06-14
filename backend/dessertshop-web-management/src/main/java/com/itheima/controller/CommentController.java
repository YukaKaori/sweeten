package com.itheima.controller;

import com.itheima.annotation.LogOperation;
import com.itheima.pojo.Comment;
import com.itheima.pojo.PageResult;
import com.itheima.pojo.Result;
import com.itheima.service.CommentService;
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
public class CommentController {

    private final CommentService commentService;

    /** 分页查询评论 */
    @GetMapping
    public Result page(@RequestParam Integer dessertId,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("查询评论,dessertId:{},page:{},pageSize:{}", dessertId, page, pageSize);
        PageResult<Comment> pageResult = commentService.pageByDessertId(dessertId, page, pageSize);
        return Result.success(pageResult);
    }

    /** 新增评论 */
    @LogOperation
    @PostMapping
    public Result save(@Valid @RequestBody Comment comment) {
        log.info("新增评论,dessertId:{},rating:{}", comment.getDessertId(), comment.getRating());
        commentService.save(comment);
        return Result.success();
    }

    /** 删除评论 */
    @LogOperation
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        log.info("删除评论,id:{}", id);
        commentService.deleteById(id);
        return Result.success();
    }
}
