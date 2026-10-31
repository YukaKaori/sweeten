package com.itheima.util;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.itheima.pojo.PageResult;

import java.util.List;
import java.util.function.Supplier;

/**
 * PageHelper 分页查询工具 — 消除 Service 中的重复分页样板代码
 */
public final class PageHelperUtils {

    private PageHelperUtils() {}

    /**
     * 执行分页查询
     * @param page     页码
     * @param pageSize 每页条数
     * @param queryFn  查询函数
     * @return 分页结果
     */
    @SuppressWarnings("unchecked")
    public static <T> PageResult<T> executePage(int page, int pageSize, Supplier<List<T>> queryFn) {
        PageHelper.startPage(page, pageSize);
        List<T> list = queryFn.get();
        Page<T> p = (Page<T>) list;
        return new PageResult<>(p.getTotal(), p.getResult());
    }
}
