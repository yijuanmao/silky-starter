package com.silky.starter.excel.config;

import com.silky.starter.excel.core.engine.ExportEngine;
import com.silky.starter.excel.core.engine.ImportEngine;
import com.silky.starter.excel.core.storage.StorageStrategy;
import com.silky.starter.excel.core.storage.factory.StorageStrategyFactory;
import com.silky.starter.excel.core.storage.impl.LocalStorageStrategy;
import com.silky.starter.excel.properties.SilkyExcelProperties;
import com.silky.starter.excel.service.compression.CompressionService;
import com.silky.starter.excel.service.compression.impl.CompressionServiceImpl;
import com.silky.starter.excel.service.export.ExportRecordService;
import com.silky.starter.excel.service.export.impl.InMemoryExportRecordService;
import com.silky.starter.excel.service.imports.ImportRecordService;
import com.silky.starter.excel.service.imports.impl.InMemoryImportRecordService;
import com.silky.starter.excel.service.storage.StorageService;
import com.silky.starter.excel.service.storage.impl.DefaultStorageService;
import com.silky.starter.excel.template.ExcelTemplate;
import com.silky.starter.excel.template.impl.DefaultExcelTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
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
@AutoConfiguration
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
    public StorageStrategy localStorageStrategy(SilkyExcelProperties properties) {
        return new LocalStorageStrategy(properties);
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
                                     SilkyExcelProperties properties,
                                     ThreadPoolTaskExecutor silkyExcelTaskExecutor,
                                     CompressionService compressionService) {
        return new ExportEngine(storageService, recordService, properties, silkyExcelTaskExecutor, compressionService);
    }

    @Bean
    @ConditionalOnMissingBean
    public ImportRecordService importRecordService() {
        return new InMemoryImportRecordService();
    }

    @Bean
    @ConditionalOnMissingBean
    public ImportEngine importEngine(ImportRecordService recordService,
                                     ThreadPoolTaskExecutor silkyExcelTaskExecutor,
                                     CompressionService compressionService) {
        return new ImportEngine(recordService, silkyExcelTaskExecutor, compressionService);
    }

    @Bean
    @ConditionalOnMissingBean
    public CompressionService compressionService(SilkyExcelProperties properties) {
        return new CompressionServiceImpl(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExcelTemplate excelTemplate(ExportEngine exportEngine,
                                       ImportEngine importEngine,
                                       SilkyExcelProperties properties,
                                       ThreadPoolTaskExecutor silkyExcelTaskExecutor) {
        return new DefaultExcelTemplate(exportEngine, importEngine, properties, silkyExcelTaskExecutor);
    }
}
