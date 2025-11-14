package com.silky.starter.excel.core.model;

import com.silky.starter.excel.core.model.imports.ImportRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Excel导入监听器上下文
 *
 * @author zy
 * @date 2025-11-14 15:44
 **/
@Data
@Builder
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisListenersContext<T> {

    /**
     * 最大错误数量
     */
    private Integer maxErrorCount;

    /**
     * 最大读取数量
     */
    private Integer pageSize;

    /**
     * 导入请求参数
     */
    private ImportRequest<T> request;

    /**
     * 总记录数
     */
    private long TotalCount;

    /**
     * 成功记录数
     */
    private long SuccessCount;

    /**
     * 失败记录数
     */
    private long FailCount;

    /**
     * 校验参数
     */
    public void validate() {
        if (this.pageSize == null || this.pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be greater than 0");
        }
        if (this.maxErrorCount == null || this.maxErrorCount <= 0) {
            throw new IllegalArgumentException("maxErrorCount must be greater than 0");
        }
        if (this.request == null) {
            throw new IllegalArgumentException("request is null");
        }
    }
}
