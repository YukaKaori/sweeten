package com.itheima.service.impl;

import com.itheima.mapper.CommentMapper;
import com.itheima.pojo.Comment;
import com.itheima.pojo.PageResult;
import com.itheima.service.CommentService;
import com.itheima.util.PageHelperUtils;
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
