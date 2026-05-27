package com.silky.starter.excel.core.model.imports;

import lombok.Data;

/**
 * 导入任务内部模型。
 *
 * @author zy
 * @date 2025-10-24 11:23
 **/
@Data
public class ImportTask<T> {

    private String taskId;

    private String businessType;

    private ImportRequest<T> request;

    private boolean async;

    private long createTime;

    private Long startTime;

    private Long finishTime;

    public void start() {
        this.startTime = System.currentTimeMillis();
    }

    public void finish() {
        this.finishTime = System.currentTimeMillis();
    }

    public boolean isTimeout(long timeoutMillis) {
        if (startTime == null || timeoutMillis <= 0) {
            return false;
        }
        return System.currentTimeMillis() - startTime > timeoutMillis;
    }
}
