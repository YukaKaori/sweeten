package com.itheima.service;

import com.itheima.pojo.Comment;
import com.itheima.pojo.PageResult;

public interface CommentService {

    PageResult<Comment> pageByDessertId(Integer dessertId, Integer page, Integer pageSize);

    void save(Comment comment);

    void deleteById(Integer id);
}
