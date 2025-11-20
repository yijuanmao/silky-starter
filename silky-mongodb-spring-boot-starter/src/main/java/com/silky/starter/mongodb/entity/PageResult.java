package com.silky.starter.mongodb.entity;

import lombok.Data;

import java.util.List;

/**
 * 分页结果
 *
 * @author: zy
 * @date: 2025-11-19
 */
@Data
public class PageResult<T> {

    /**
     * 总记录数
     */
    private long total;

    /**
     * 数据列表
     */
    private List<T> records;

    /**
     * 当前页码 从1开始计数，表示当前请求的是第几页
     */
    private long pageNum;

    /**
     * 每页显示条数
     */
    private long pageSize;

    /**
     * 总页数
     * 根据总记录数和每页大小计算得出的总页面数量
     * 计算公式：pages = (total + size - 1) / size
     * 示例：100条数据，每页10条，pages = 10页
     */
    private long pages;

    public PageResult() {
    }

    public PageResult(long pageNum, long pageSize, long total, List<T> records) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.records = records;
        this.pages = (total + pageSize - 1) / pageSize;
    }

}
