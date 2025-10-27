package com.silky.starter.excel.config;

import com.silky.starter.excel.core.async.AsyncExecutor;
import com.silky.starter.excel.core.async.AsyncProcessor;
import com.silky.starter.excel.core.async.SyncAsyncProcessor;
import com.silky.starter.excel.core.async.ThreadPoolAsyncProcessor;
import com.silky.starter.excel.core.async.factory.AsyncProcessorFactory;
import com.silky.starter.excel.core.engine.ExportEngine;
import com.silky.starter.excel.core.storage.StorageStrategy;
import com.silky.starter.excel.core.storage.factory.StorageStrategyFactory;
import com.silky.starter.excel.core.storage.impl.LocalStorageStrategy;
import com.silky.starter.excel.properties.SilkyExcelProperties;
import com.silky.starter.excel.service.export.ExportRecordService;
import com.silky.starter.excel.service.export.impl.InMemoryExportRecordService;
import com.silky.starter.excel.service.storage.StorageService;
import com.silky.starter.excel.service.storage.impl.DefaultStorageService;
import com.silky.starter.excel.template.ExcelExportTemplate;
import com.silky.starter.excel.template.impl.DefaultExcelExportTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Silky Excel 自动配置类
 *
 * @author zy
 * @date 2025-10-24 11:14
 **/
@Configuration
@EnableConfigurationProperties(SilkyExcelProperties.class)
@ConditionalOnClass(name = "cn.idev.excel.FastExcelWriter")
@ConditionalOnProperty(prefix = "silky.excel", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SilkyExcelAutoConfiguration {

    /**
     * 配置导出记录服务
     * 默认使用内存存储，生产环境应该使用数据库存储
     *
     * @return 导出记录服务实例
     */
    @Bean
    @ConditionalOnMissingBean
    public ExportRecordService exportRecordService() {
        return new InMemoryExportRecordService();
    }

    /**
     * 配置异步处理器工厂
     *
     * @return 异步处理器工厂实例
     */
    @Bean
    @ConditionalOnMissingBean
    public AsyncProcessorFactory asyncProcessorFactory(SilkyExcelProperties properties, @Autowired(required = false) List<AsyncProcessor> processors) {
        return new AsyncProcessorFactory(properties, processors);
    }

    /**
     * 配置异步执行器
     *
     * @return 异步执行器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public AsyncExecutor asyncExecutor(AsyncProcessorFactory factory, SilkyExcelProperties properties) {
        return new AsyncExecutor(factory, properties);
    }

    /**
     * 配置线程池异步处理器
     *
     * @return 线程池异步处理器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public AsyncProcessor threadPoolAsyncProcessor(ExportEngine exportEngine,
                                                   ThreadPoolTaskExecutor taskExecutor) {
        return new ThreadPoolAsyncProcessor(exportEngine, taskExecutor);
    }

    /**
     * 配置同步处理器
     *
     * @return 同步处理器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public AsyncProcessor syncAsyncProcessor(ExportEngine exportEngine) {
        return new SyncAsyncProcessor(exportEngine);
    }

    /**
     * 配置导出线程池任务执行器
     *
     * @return 导出线程池任务执行器实例
     */
    @Bean(name = "exportThreadPoolTaskExecutor")
    @ConditionalOnMissingBean
    public ThreadPoolTaskExecutor exportThreadPoolTaskExecutor(SilkyExcelProperties properties) {
        ThreadPoolTaskExecutor defaultExecutor = new ThreadPoolTaskExecutor();
        defaultExecutor.setCorePoolSize(properties.getThreadPool().getCoreSize());
        defaultExecutor.setMaxPoolSize(properties.getThreadPool().getMaxSize());
        defaultExecutor.setQueueCapacity(properties.getThreadPool().getQueueCapacity());
        defaultExecutor.setKeepAliveSeconds(properties.getThreadPool().getKeepAliveSeconds());
        defaultExecutor.setThreadNamePrefix(properties.getThreadPool().getThreadNamePrefix());
        defaultExecutor.setAllowCoreThreadTimeOut(properties.getThreadPool().isAllowCoreThreadTimeOut());
        defaultExecutor.setWaitForTasksToCompleteOnShutdown(properties.getThreadPool().isWaitForTasksToCompleteOnShutdown());
        defaultExecutor.setAwaitTerminationSeconds(properties.getThreadPool().getAwaitTerminationSeconds());
        defaultExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return defaultExecutor;
    }

    /**
     * 配置Spring线程池任务异步处理器
     * 只有在Spring线程池可用时才创建
     *
     * @return Spring线程池任务异步处理器实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor")
    public AsyncProcessor threadPoolTaskExecutorAsyncProcessor(ExportEngine exportEngine,
                                                               ThreadPoolTaskExecutor taskExecutor) {
        return new ThreadPoolAsyncProcessor(exportEngine, taskExecutor);
    }

    /**
     * 配置导出引擎
     *
     * @return 导出引擎实例
     */
    @Bean
    @ConditionalOnMissingBean
    public ExportEngine exportEngine(StorageService storageService,
                                     ExportRecordService recordService,
                                     AsyncExecutor asyncExecutor) {
        return new ExportEngine(storageService, recordService, asyncExecutor);
    }

    /**
     * 配置本地存储策略
     * 默认必须的存储策略
     *
     * @return 本地存储策略实例
     */
    @Bean
    @ConditionalOnMissingBean
    public StorageStrategy localStorageStrategy(SilkyExcelProperties properties) {
        return new LocalStorageStrategy(properties);
    }

    /**
     * 配置存储策略工厂
     *
     * @return 存储策略工厂实例
     */
    @Bean
    @ConditionalOnMissingBean
    public StorageStrategyFactory storageStrategyFactory(@Autowired(required = false) List<StorageStrategy> storageStrategies) {
        return new StorageStrategyFactory(storageStrategies);
    }

    /**
     * 配置主导出器（入口类）
     *
     * @return 主导出器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public ExcelExportTemplate excelExporter(ExportEngine exportEngine) {
        return new DefaultExcelExportTemplate(exportEngine);
    }

    /**
     * 配置存储服务
     *
     * @return 存储服务实例
     */
    @Bean
    @ConditionalOnMissingBean
    public StorageService storageService(StorageStrategyFactory strategyFactory,
                                         SilkyExcelProperties properties) {
        return new DefaultStorageService(strategyFactory, properties);
    }

}
