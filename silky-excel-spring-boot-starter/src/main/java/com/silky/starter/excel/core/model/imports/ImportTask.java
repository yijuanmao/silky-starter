package com.silky.starter.excel.core.model.imports;

import com.silky.starter.excel.core.async.BaseAsyncTask;
import com.silky.starter.excel.enums.TaskType;
import lombok.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 导入任务
 *
 * @author zy
 * @date 2025-10-24 11:23
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ImportTask<T> extends BaseAsyncTask {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 任务类型
     */
    private TaskType taskType;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 导入请求
     */
    private ImportRequest<T> request;

    /**
     * 任务创建时间
     */
    private Long createTime;

    /**
     * 任务开始时间
     */
    private Long startTime;

    /**
     * 任务完成时间
     */
    private Long finishTime;

    /**
     * 任务超时时间（毫秒）
     */
    private Long timeout;

    /**
     * 任务优先级
     */
    private Integer priority = 5;

    /**
     * 扩展属性
     */
    private Map<String, Object> attributes = new ConcurrentHashMap<>();

    /**
     * 检查任务是否超时
     */
    public boolean isTimeout() {
        return isTimeout(this.timeout);
    }

    /**
     * 检查任务是否超时
     */
    public boolean isTimeout(Long customTimeout) {
        if (startTime == null || customTimeout == null || customTimeout <= 0) {
            return false;
        }
        return System.currentTimeMillis() - startTime > customTimeout;
    }

    /**
     * 开始任务
     */
    public void start() {
        this.startTime = System.currentTimeMillis();
    }

    /**
     * 完成任务
     */
    public void finish() {
        this.finishTime = System.currentTimeMillis();
    }

    /**
     * 获取任务执行时间
     */
    public Long getExecutionTime() {
        if (startTime == null) {
            return null;
        }
        Long endTime = finishTime != null ? finishTime : System.currentTimeMillis();
        return endTime - startTime;
    }

    /**
     * 设置扩展属性
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * 获取扩展属性
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * 获取扩展属性（带默认值）
     */
    public Object getAttribute(String key, Object defaultValue) {
        return attributes.getOrDefault(key, defaultValue);
    }
}
