package com.silky.starter.excel.core.async;

import com.silky.starter.excel.enums.TaskType;
import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * 异步任务基类
 *
 * @author zy
 * @date 2025-10-27 21:35
 **/
@Data
public abstract class BaseAsyncTask implements AsyncTask {

    /**
     * 任务ID
     */
    protected String taskId;

    /**
     * 任务类型
     */
    protected TaskType taskType;

    /**
     * 业务类型
     */
    protected String businessType;

    /**
     * 验证任务
     */
    @Override
    public void validate() {
        if (!StringUtils.hasText(taskId)) {
            throw new IllegalArgumentException("任务ID不能为空");
        }
        if (taskType == null) {
            throw new IllegalArgumentException("任务类型不能为空");
        }
        if (!StringUtils.hasText(businessType)) {
            throw new IllegalArgumentException("业务类型不能为空");
        }
    }

    /**
     * 获取任务摘要
     */
    public String getSummary() {
        return String.format("%s[taskId=%s, type=%s, businessType=%s]",
                getClass().getSimpleName(), taskId, taskType, businessType);
    }
}
