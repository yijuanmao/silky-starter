package com.silky.starter.mongodb.model.page;

import java.util.List;
import java.util.Objects;

/**
 * 分页
 *
 * @author zy
 * @date 2022-11-22 17:56
 **/
public class Page<T> {

    /**
     * 当前页码
     */
    private Integer pageNum;

    /**
     * 一页数据默认10条
     */
    private Integer pageSize;

    /**
     * 一共有多少条数据
     */
    private Long total;

    /**
     * 数据列表
     */
    private List<T> list;

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        if (Objects.isNull(pageNum)) {
            pageNum = 1;
        }
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        if (Objects.isNull(pageSize)) {
            pageSize = 10;
        }
        this.pageSize = pageSize;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public Page() {
    }

    public Page(Integer pageNum, Integer pageSize) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    public Page(Integer pageNum, Integer pageSize, List<T> list) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.list = list;
    }

    @Override
    public String toString() {
        return "Page{" +
                "pageNum=" + pageNum +
                ", pageSize=" + pageSize +
                ", total=" + total +
                ", list=" + list +
                '}';
    }
}
