package com.silky.starter.excel.service.export.impl;

import cn.hutool.core.util.StrUtil;
import com.silky.starter.excel.entity.ExportRecord;
import com.silky.starter.excel.enums.ExportStatus;
import com.silky.starter.excel.service.export.ExportRecordService;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 基于内存的导出记录服务实现,注意：此实现仅用于示例和测试，生产环境应该使用数据库存储
 *
 * @author zy
 * @date 2025-10-24 15:19
 **/
@Slf4j
public class InMemoryExportRecordService implements ExportRecordService {

    /**
     * 导出记录存储
     * key: taskId, value: ExportRecord
     */
    private final Map<String, ExportRecord> recordMap = new ConcurrentHashMap<>();

    /**
     * 保存导出记录
     *
     * @param record 导出记录
     */
    @Override
    public void save(ExportRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("导出记录不能为null");
        }
        if (record.getTaskId() == null || record.getTaskId().trim().isEmpty()) {
            throw new IllegalArgumentException("任务ID不能为空");
        }

        recordMap.put(record.getTaskId(), record);
        log.debug("保存导出记录成功: {}, 状态: {}", record.getTaskId(), record.getStatus());
    }

    /**
     * 根据任务ID查询导出记录
     *
     * @param taskId 任务ID
     * @return 导出记录，如果不存在返回null
     */
    @Override
    public ExportRecord getByTaskId(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            return null;
        }

        ExportRecord record = recordMap.get(taskId);
        if (record == null) {
            log.debug("导出记录不存在: {}", taskId);
        }

        return record;
    }

    /**
     * 更新导出状态
     *
     * @param taskId 任务ID
     * @param status 新的状态
     */
    @Override
    public void updateStatus(String taskId, ExportStatus status) {
        update(taskId, record -> {
            record.setStatus(status);

            // 设置开始时间（如果是第一次进入处理状态）
            if (status == ExportStatus.PROCESSING && record.getStartTime() == null) {
                record.setStartTime(LocalDateTime.now());
            }

            // 设置完成时间（如果是最终状态）
            if (status.isFinal()) {
                record.setFinishTime(LocalDateTime.now());
            }

            log.debug("更新任务状态: {} -> {}", taskId, status);
        });
    }

    /**
     * 更新导出进度
     *
     * @param taskId         任务ID
     * @param processedCount 已处理数据量
     */
    @Override
    public void updateProgress(String taskId, long processedCount) {
        update(taskId, record -> {
            record.setProcessedCount(processedCount);

            // 如果没有设置总数，使用当前处理数作为总数（估算）
            if (record.getTotalCount() == null || record.getTotalCount() == 0) {
                record.setTotalCount(processedCount);
            }
            log.debug("更新任务进度: {} -> {}/{}", taskId, processedCount, record.getTotalCount());
        });
    }

    /**
     * 更新导出成功
     *
     * @param taskId  任务ID
     * @param fileUrl 文件URL
     */
    @Override
    public void updateSuccess(String taskId, String fileUrl) {
        update(taskId, record -> {
            record.setStatus(ExportStatus.COMPLETED);
            record.setFileUrl(fileUrl);
            record.setFinishTime(LocalDateTime.now());

            log.info("标记任务成功: {}, 文件URL: {}", taskId, fileUrl);
        });
    }

    /**
     * 更新导出失败
     *
     * @param taskId   任务ID
     * @param errorMsg 错误信息
     */
    @Override
    public void updateFailed(String taskId, String errorMsg) {
        update(taskId, record -> {
            record.setStatus(ExportStatus.FAILED);
            record.setErrorMsg(errorMsg);
            record.setFinishTime(LocalDateTime.now());

            log.error("标记任务失败: {}, 错误信息: {}", taskId, errorMsg);
        });
    }

    /**
     * 更新导出记录
     *
     * @param taskId  任务ID
     * @param updater 更新函数
     */
    @Override
    public void update(String taskId, Consumer<ExportRecord> updater) {
        if (StrUtil.isBlank(taskId)) {
            throw new IllegalArgumentException("任务ID不能为空");
        }
        if (updater == null) {
            throw new IllegalArgumentException("更新函数不能为null");
        }

        ExportRecord record = recordMap.get(taskId);
        if (record != null) {
            updater.accept(record);
            recordMap.put(taskId, record);
        } else {
            log.warn("尝试更新不存在的导出记录: {}", taskId);
        }
    }

    /**
     * 删除导出记录
     *
     * @param taskId 任务ID
     */
    @Override
    public void delete(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            return;
        }

        ExportRecord removed = recordMap.remove(taskId);
        if (removed != null) {
            log.info("删除导出记录: {}", taskId);
        } else {
            log.debug("导出记录不存在，无需删除: {}", taskId);
        }
    }

    /**
     * 根据状态查询导出记录列表
     *
     * @param status 状态
     * @return 导出记录列表
     */
    @Override
    public List<ExportRecord> listByStatus(ExportStatus status) {
        if (status == null) {
            return new ArrayList<>(recordMap.values());
        }

        return recordMap.values().stream()
                .filter(record -> status.equals(record.getStatus()))
                .sorted(Comparator.comparing(ExportRecord::getCreateTime).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 根据业务类型查询导出记录列表
     *
     * @param businessType 业务类型
     * @return 导出记录列表
     */
    @Override
    public List<ExportRecord> listByBusinessType(String businessType) {
        if (StrUtil.isBlank(businessType)) {
            return Collections.emptyList();
        }
        return recordMap.values().stream()
                .filter(record -> businessType.equals(record.getBusinessType()))
                .sorted(Comparator.comparing(ExportRecord::getCreateTime).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 清理过期的导出记录
     *
     * @param expireDays 过期天数
     * @return 清理的记录数量
     */
    @Override
    public int cleanExpiredRecords(int expireDays) {
        if (expireDays <= 0) {
            return 0;
        }

        LocalDateTime expireDate = LocalDateTime.now().plusDays(-expireDays);

        List<String> expiredTaskIds = recordMap.values().stream()
                .filter(record -> record.getCreateTime().isBefore(expireDate))
                .map(ExportRecord::getTaskId)
                .collect(Collectors.toList());

        expiredTaskIds.forEach(recordMap::remove);

        log.info("清理过期导出记录完成: 过期天数={}, 清理数量={}", expireDays, expiredTaskIds.size());
        return expiredTaskIds.size();
    }

    /**
     * 获取所有导出记录数量
     *
     * @return 记录总数
     */
    public int getTotalCount() {
        return recordMap.size();
    }

    /**
     * 获取各种状态的记录数量统计
     *
     * @return 状态统计Map
     */
    public Map<ExportStatus, Long> getStatusStatistics() {
        return recordMap.values().stream()
                .collect(Collectors.groupingBy(
                        ExportRecord::getStatus,
                        Collectors.counting()
                ));
    }

    /**
     * 获取各种业务类型的记录数量统计
     *
     * @return 业务类型统计Map
     */
    public Map<String, Long> getBusinessTypeStatistics() {
        return recordMap.values().stream()
                .collect(Collectors.groupingBy(
                        ExportRecord::getBusinessType,
                        Collectors.counting()
                ));
    }

    /**
     * 获取最近N条导出记录
     *
     * @param limit 记录条数
     * @return 导出记录列表
     */
    public List<ExportRecord> getRecentRecords(int limit) {
        return recordMap.values().stream()
                .sorted(Comparator.comparing(ExportRecord::getCreateTime).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 获取服务状态信息
     *
     * @return 状态信息描述
     */
    public String getServiceStatus() {
        int totalCount = getTotalCount();
        Map<ExportStatus, Long> statusStats = getStatusStatistics();

        return String.format("导出记录服务状态: 总记录数=%d, 进行中=%d, 已完成=%d, 已失败=%d",
                totalCount,
                statusStats.getOrDefault(ExportStatus.PROCESSING, 0L),
                statusStats.getOrDefault(ExportStatus.COMPLETED, 0L),
                statusStats.getOrDefault(ExportStatus.FAILED, 0L));
    }
}
