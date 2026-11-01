package com.sweeten.catalog.service;

import com.sweeten.catalog.mapper.CommentMapper;
import com.sweeten.catalog.model.Comment;
import com.sweeten.common.paging.PageHelperUtils;
import com.sweeten.common.paging.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;

    @Override
    public PageResult<Comment> pageByDessertId(Integer dessertId, Integer page, Integer pageSize) {
        return PageHelperUtils.executePage(page, pageSize,
                () -> commentMapper.selectByDessertId(dessertId));
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void save(Comment comment) {
        comment.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(comment);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void deleteById(Integer id) {
        commentMapper.deleteById(id);
    }
}
