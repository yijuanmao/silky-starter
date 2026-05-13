package com.silky.starter.excel.core.engine;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * Excel引擎公共基类
 * 抽取导出引擎和导入引擎的共同逻辑：统计变量、缓存管理、清理任务、引擎状态
 *
 * @author zy
 * @since 1.1.0
 */
@Slf4j
public abstract class AbstractExcelEngine {

    /**
     * 总处理任务数
     */
    protected final AtomicLong totalProcessed = new AtomicLong(0);
    /**
     * 成功任务数
     */
    protected final AtomicLong successCount = new AtomicLong(0);
    /**
     * 失败任务数
     */
    protected final AtomicLong failedCount = new AtomicLong(0);

    /**
     * 引擎启动时间戳
     */
    private final long engineStartTime = System.currentTimeMillis();
    /**
     * 引擎启动日期时间
     */
    private final LocalDateTime engineStartDateTime = LocalDateTime.now();

    /**
     * 缓存清理定时器
     */
    protected final ScheduledExecutorService cleanupExecutor;
    /**
     * 引擎名称（用于日志和线程命名）
     */
    private final String engineName;

    /**
     * 构造函数
     *
     * @param engineName       引擎名称
     * @param threadNamePrefix 清理线程名前缀
     */
    protected AbstractExcelEngine(String engineName, String threadNamePrefix) {
        this.engineName = engineName;
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, threadNamePrefix));
        startCacheCleanupTask();
    }

    /**
     * 构造函数（使用共享的清理执行器）
     *
     * @param engineName      引擎名称
     * @param cleanupExecutor 共享的清理执行器
     */
    protected AbstractExcelEngine(String engineName, ScheduledExecutorService cleanupExecutor) {
        this.engineName = engineName;
        this.cleanupExecutor = cleanupExecutor;
        startCacheCleanupTask();
    }

    /**
     * 启动缓存清理定时任务
     */
    private void startCacheCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            try {
                cleanupExpiredCaches();
            } catch (Exception e) {
                log.error("{}缓存清理任务执行失败", engineName, e);
            }
        }, 10, 10, TimeUnit.MINUTES);
    }

    /**
     * 清理过期缓存（子类实现具体逻辑）
     */
    protected abstract void cleanupExpiredCaches();

    /**
     * 获取当前缓存大小
     *
     * @return 缓存任务数量
     */
    protected abstract int getCacheSize();

    /**
     * 获取引擎启动时间
     *
     * @return 启动日期时间
     */
    public LocalDateTime getEngineStartDateTime() {
        return engineStartDateTime;
    }

    /**
     * 获取引擎运行时长（毫秒）
     *
     * @return 运行时长
     */
    public long getUptime() {
        return System.currentTimeMillis() - engineStartTime;
    }

    /**
     * 递增成功计数
     */
    protected void incrementSuccess() {
        successCount.incrementAndGet();
    }

    /**
     * 递增失败计数
     */
    protected void incrementFailed() {
        failedCount.incrementAndGet();
    }

    /**
     * 递增总处理计数
     */
    protected void incrementTotalProcessed() {
        totalProcessed.incrementAndGet();
    }

    /**
     * 优雅关闭引擎
     */
    public void shutdown() {
        log.info("开始关闭{}...", engineName);
        // 注意：如果是共享 cleanupExecutor，不在此处关闭
        cleanupExpiredCaches();
        log.info("{}已关闭, 统计: 总={}, 成功={}, 失败={}",
                engineName, totalProcessed.get(), successCount.get(), failedCount.get());
    }

    /**
     * 安全关闭清理执行器（仅持有独占执行器时调用）
     */
    protected void shutdownCleanupExecutor() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 构建通用引擎状态数据
     *
     * @return 状态数据数组：[启动时间, 总处理数, 成功数, 失败数, 缓存数, 运行时长]
     */
    protected Object[] buildStatusData() {
        return new Object[]{
                engineStartDateTime,
                totalProcessed.get(),
                successCount.get(),
                failedCount.get(),
                getCacheSize(),
                getUptime()
        };
    }

    /**
     * 计算成功率
     *
     * @return 成功率
     */
    public double getSuccessRate() {
        long total = totalProcessed.get();
        return total > 0 ? (double) successCount.get() / total : 0.0;
    }

    /**
     * 清理过期缓存条目的通用方法
     *
     * @param cache     缓存Map
     * @param isExpired 过期判断逻辑
     * @param <T>       任务类型
     */
    protected <T> void cleanExpiredEntries(ConcurrentMap<String, T> cache, Predicate<T> isExpired) {
        int before = cache.size();
        cache.entrySet().removeIf(entry -> isExpired.test(entry.getValue()));
        if (before != cache.size()) {
            log.debug("{}缓存清理完成: 任务缓存 {}->{}", engineName, before, cache.size());
        }
    }
}
