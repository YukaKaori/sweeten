package com.sweeten.catalog.service;

import com.sweeten.catalog.model.Comment;
import com.sweeten.common.paging.PageResult;

public interface CommentService {

    PageResult<Comment> pageByDessertId(Integer dessertId, Integer page, Integer pageSize);

    void save(Comment comment);

    void deleteById(Integer id);
}
