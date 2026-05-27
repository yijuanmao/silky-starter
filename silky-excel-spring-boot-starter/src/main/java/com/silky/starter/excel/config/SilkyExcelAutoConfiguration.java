package com.silky.starter.excel.config;

import com.silky.starter.excel.core.resolve.DictFieldResolver;
import com.silky.starter.excel.core.resolve.DictionaryProvider;
import com.silky.starter.excel.core.resolve.EnumFieldResolver;
import com.silky.starter.excel.core.resolve.ExcelFieldResolver;
import com.silky.starter.excel.core.resolve.ExcelFieldResolverPipeline;
import com.silky.starter.excel.core.resolve.MaskFieldResolver;
import com.silky.starter.excel.core.storage.StorageStrategy;
import com.silky.starter.excel.core.storage.factory.StorageStrategyFactory;
import com.silky.starter.excel.core.storage.impl.LocalStorageStrategy;
import com.silky.starter.excel.properties.SilkyExcelProperties;
import com.silky.starter.excel.service.export.ExportRecordService;
import com.silky.starter.excel.service.export.impl.InMemoryExportRecordService;
import com.silky.starter.excel.service.imports.ImportRecordService;
import com.silky.starter.excel.service.imports.impl.InMemoryImportRecordService;
import com.silky.starter.excel.template.ExcelTemplate;
import com.silky.starter.excel.template.impl.DefaultExcelTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Silky Excel 自动配置类。
 *
 * @author zy
 * @since 1.0.0
 */
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
    public StorageStrategyFactory storageStrategyFactory(List<StorageStrategy> storageStrategies,
                                                         SilkyExcelProperties properties) {
        return new StorageStrategyFactory(storageStrategies, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExportRecordService exportRecordService() {
        return new InMemoryExportRecordService();
    }

    @Bean
    @ConditionalOnMissingBean
    public ImportRecordService importRecordService() {
        return new InMemoryImportRecordService();
    }

    @Bean
    @ConditionalOnMissingBean
    public ExcelFieldResolverPipeline excelFieldResolverPipeline(ObjectProvider<DictionaryProvider> dictionaryProvider) {
        List<ExcelFieldResolver<?>> resolvers = new ArrayList<>();
        resolvers.add(new EnumFieldResolver());
        resolvers.add(new DictFieldResolver(dictionaryProvider.getIfAvailable()));
        resolvers.add(new MaskFieldResolver());
        return new ExcelFieldResolverPipeline(resolvers);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExcelTemplate excelTemplate(StorageStrategyFactory storageStrategyFactory,
                                       ExportRecordService exportRecordService,
                                       ImportRecordService importRecordService,
                                       SilkyExcelProperties properties,
                                       ThreadPoolTaskExecutor silkyExcelTaskExecutor,
                                       ExcelFieldResolverPipeline fieldResolverPipeline) {
        return new DefaultExcelTemplate(storageStrategyFactory, exportRecordService, importRecordService,
                properties, silkyExcelTaskExecutor, fieldResolverPipeline);
    }
}
