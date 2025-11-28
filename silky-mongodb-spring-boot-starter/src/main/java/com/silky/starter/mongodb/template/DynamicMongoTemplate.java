package com.silky.starter.mongodb.template;

import cn.hutool.core.util.StrUtil;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Set;

/**
 * 动态数据源切换
 *
 * @author: zy
 * @date: 2025-11-19
 */
public class DynamicMongoTemplate {

    private final String primaryDataSource;

    private final Map<Object, Object> targetDataSources;

    private static final ThreadLocal<String> DATA_SOURCE_HOLDER = new ThreadLocal<>();

    public DynamicMongoTemplate(Map<Object, Object> targetDataSources, String primaryDataSource) {
        this.targetDataSources = targetDataSources;
        this.primaryDataSource = primaryDataSource;
    }

    /**
     * 设置数据源
     *
     * @param dataSource 数据源名称
     */
    public void setDataSource(String dataSource) {
        if (StringUtils.hasText(dataSource) && targetDataSources.containsKey(dataSource)) {
            DATA_SOURCE_HOLDER.set(dataSource);
        } else {
            throw new IllegalArgumentException("Data source '" + dataSource + "' not found");
        }
    }

    public MongoTemplate getTargetTemplate() {
        return getTargetTemplate(false, null);
    }

    /**
     * 获取目标数据源
     *
     * @param readOnly   是否只读
     * @param dataSource 数据源名称
     * @return MongoTemplate
     */
    public MongoTemplate getTargetTemplate(boolean readOnly, String dataSource) {
        String lookupKey;
        if (StringUtils.hasText(dataSource)) {
            lookupKey = dataSource;
        } else {
            String currentDataSource = getDataSource();
            if (StringUtils.hasText(currentDataSource)) {
                lookupKey = currentDataSource;
                if (readOnly && targetDataSources.containsKey(currentDataSource + "_read")) {
                    lookupKey = currentDataSource + "_read";
                }
            } else if (readOnly) {
                // 如果是读操作，尝试使用读库
                lookupKey = getReadDataSource();
            } else {
                lookupKey = getDefaultDataSource();
            }
        }
        MongoTemplate template = (MongoTemplate) targetDataSources.get(lookupKey);
        if (template == null) {
            throw new IllegalStateException("Cannot determine target MongoTemplate for lookup key [" + lookupKey + "]");
        }
        return template;
    }

    /**
     * 获取默认数据源名称
     */
    private String getDefaultDataSource() {
        if (StrUtil.isNotBlank(primaryDataSource)) {
            return this.primaryDataSource;
        }
        // 返回第一个数据源作为默认数据源
        return targetDataSources.keySet().stream()
                .findFirst()
                .map(Object::toString)
                .orElseThrow(() -> new IllegalStateException("No data source configured"));
    }

    /**
     * 获取读数据源名称
     */
    private String getReadDataSource() {
        Set<Object> objects = targetDataSources.keySet();
        // 查找读数据源，如果没有配置则使用默认数据源
        return targetDataSources.keySet().stream()
                .filter(key -> key.toString().endsWith("_read"))
                .findFirst()
                .map(Object::toString)
                .orElse(getDefaultDataSource());
    }

    /**
     * 获取所有数据源名称
     */
    public String[] getDataSourceNames() {
        return targetDataSources.keySet().stream()
                .map(Object::toString)
                .toArray(String[]::new);
    }

    /**
     * 检查数据源是否存在
     */
    public boolean containsDataSource(String dataSourceName) {
        return targetDataSources.containsKey(dataSourceName);
    }

    public String getDataSource() {
        return DATA_SOURCE_HOLDER.get();
    }

    public void clearDataSource() {
        DATA_SOURCE_HOLDER.remove();
    }
}
