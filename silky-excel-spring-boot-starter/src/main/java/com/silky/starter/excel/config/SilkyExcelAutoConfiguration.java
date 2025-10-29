package com.silky.starter.excel.config;

import com.silky.starter.excel.core.async.executor.AsyncExecutor;
import com.silky.starter.excel.core.async.factory.AsyncProcessorFactory;
import com.silky.starter.excel.core.async.impl.export.ExportSyncAsyncProcessor;
import com.silky.starter.excel.core.async.impl.export.ExportThreadPoolAsyncProcessor;
import com.silky.starter.excel.core.async.impl.imports.ImportSyncAsyncProcessor;
import com.silky.starter.excel.core.async.impl.imports.ImportThreadPoolAsyncProcessor;
import com.silky.starter.excel.core.async.impl.unified.UnifiedSyncAsyncProcessor;
import com.silky.starter.excel.core.async.impl.unified.UnifiedThreadPoolAsyncProcessor;
import com.silky.starter.excel.core.engine.ExportEngine;
import com.silky.starter.excel.core.engine.ImportEngine;
import com.silky.starter.excel.core.storage.StorageStrategy;
import com.silky.starter.excel.core.storage.factory.StorageStrategyFactory;
import com.silky.starter.excel.properties.SilkyExcelProperties;
import com.silky.starter.excel.service.export.ExportRecordService;
import com.silky.starter.excel.service.export.impl.InMemoryExportRecordService;
import com.silky.starter.excel.service.imports.ImportRecordService;
import com.silky.starter.excel.service.imports.impl.InMemoryImportRecordService;
import com.silky.starter.excel.service.storage.StorageService;
import com.silky.starter.excel.service.storage.impl.DefaultStorageService;
import com.silky.starter.excel.template.ExcelExportTemplate;
import com.silky.starter.excel.template.impl.DefaultExcelExportTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Silky Excel 自动配置类
 *
 * @author zy
 * @date 2025-10-24 11:14
 **/
@Slf4j
@Configuration
@EnableConfigurationProperties(SilkyExcelProperties.class)
@ConditionalOnProperty(prefix = "silky.excel", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SilkyExcelAutoConfiguration {

    @Bean("silkyExcelTaskExecutor")
    @ConditionalOnMissingBean(name = "silkyExcelTaskExecutor")
    public ThreadPoolTaskExecutor silkyExcelTaskExecutor(SilkyExcelProperties properties) {
        SilkyExcelProperties.Async.ThreadPool threadPoolConfig = properties.getAsync().getThreadPool();

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadPoolConfig.getCorePoolSize());
        executor.setMaxPoolSize(threadPoolConfig.getMaxPoolSize());
        executor.setQueueCapacity(threadPoolConfig.getQueueCapacity());
        executor.setKeepAliveSeconds(threadPoolConfig.getKeepAliveSeconds());
        executor.setThreadNamePrefix("silky-excel-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        log.info("Silky Excel任务线程池初始化完成: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                threadPoolConfig.getCorePoolSize(), threadPoolConfig.getMaxPoolSize(), threadPoolConfig.getQueueCapacity());

        return executor;
    }

    @Bean
    @ConditionalOnMissingBean
    public ExportSyncAsyncProcessor exportSyncAsyncProcessor(ExportEngine exportEngine) {
        return new ExportSyncAsyncProcessor(exportEngine);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExportThreadPoolAsyncProcessor exportThreadPoolAsyncProcessor(
            ExportEngine exportEngine,
            ThreadPoolTaskExecutor silkyExcelTaskExecutor) {
        return new ExportThreadPoolAsyncProcessor(exportEngine, silkyExcelTaskExecutor);
    }

    @Bean
    @ConditionalOnMissingBean
    public ImportSyncAsyncProcessor importSyncAsyncProcessor(ImportEngine importEngine) {
        return new ImportSyncAsyncProcessor(importEngine);
    }

    @Bean
    @ConditionalOnMissingBean
    public ImportThreadPoolAsyncProcessor importThreadPoolAsyncProcessor(
            ImportEngine importEngine,
            ThreadPoolTaskExecutor silkyExcelTaskExecutor) {
        return new ImportThreadPoolAsyncProcessor(importEngine, silkyExcelTaskExecutor);
    }

    @Bean
    @ConditionalOnMissingBean
    @Primary
    public UnifiedSyncAsyncProcessor unifiedSyncAsyncProcessor(
            ExportEngine exportEngine,
            ImportEngine importEngine) {
        return new UnifiedSyncAsyncProcessor(exportEngine, importEngine);
    }

    @Bean
    @ConditionalOnMissingBean
    @Primary
    public UnifiedThreadPoolAsyncProcessor unifiedThreadPoolAsyncProcessor(
            ExportEngine exportEngine,
            ImportEngine importEngine,
            ThreadPoolTaskExecutor silkyExcelTaskExecutor) {
        return new UnifiedThreadPoolAsyncProcessor(exportEngine, importEngine, silkyExcelTaskExecutor);
    }

    @Bean
    @ConditionalOnMissingBean
    public AsyncProcessorFactory asyncProcessorFactory(SilkyExcelProperties properties) {
        return new AsyncProcessorFactory(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public AsyncExecutor asyncExecutor(AsyncProcessorFactory asyncProcessorFactory, SilkyExcelProperties properties) {
        return new AsyncExecutor(asyncProcessorFactory, properties);
    }

    @Bean
    public StorageStrategyFactory storageStrategyFactory(List<StorageStrategy> storageStrategies) {
        return new StorageStrategyFactory(storageStrategies);
    }

    @Bean
    @ConditionalOnMissingBean
    public StorageService storageService(StorageStrategyFactory strategyFactory,
                                         SilkyExcelProperties properties) {
        return new DefaultStorageService(strategyFactory, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExportRecordService exportRecordService() {
        return new InMemoryExportRecordService();
    }

    @Bean
    @ConditionalOnMissingBean
    public ExportEngine exportEngine(StorageService storageService,
                                     ExportRecordService recordService,
                                     AsyncExecutor asyncExecutor) {
        return new ExportEngine(storageService, recordService, asyncExecutor);
    }

    @Bean
    @ConditionalOnMissingBean
    public ImportRecordService importRecordService() {
        return new InMemoryImportRecordService();
    }

    @Bean
    @ConditionalOnMissingBean
    public ImportEngine importEngine(ImportRecordService importRecordService, AsyncExecutor asyncExecutor) {
        return new ImportEngine(importRecordService, asyncExecutor);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExcelExportTemplate excelExportTemplate(AsyncExecutor asyncExecutor) {
        return new DefaultExcelExportTemplate(asyncExecutor);
    }
}
