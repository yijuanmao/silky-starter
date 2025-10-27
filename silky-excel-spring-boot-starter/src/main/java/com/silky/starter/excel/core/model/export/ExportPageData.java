package com.silky.starter.excel.core.model.export;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页数据
 *
 * @author zy
 * @date 2025-10-24 11:17
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportPageData<T> {

    /**
     * 当前页的数据列表
     * 不能为null，如果无数据应为空列表
     */
    private List<T> data;

    /**
     * 是否还有下一页数据
     * true表示还有更多数据，false表示这是最后一页
     */
    private boolean hasNext;
}
