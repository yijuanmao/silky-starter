package com.silky.starter.oss.autoconfigure;

import com.silky.starter.oss.adapter.OssProviderAdapter;
import com.silky.starter.oss.adapter.impl.AliyunOssAdapter;
import com.silky.starter.oss.adapter.impl.HuaWeiObsAdapter;
import com.silky.starter.oss.adapter.registry.OssAdapterRegistry;
import com.silky.starter.oss.core.constant.OssConstants;
import com.silky.starter.oss.properties.OssProperties;
import com.silky.starter.oss.service.impl.DefaultMultiCloudPartImpl;
import com.silky.starter.oss.service.strategy.MultiCloudPartStrategy;
import com.silky.starter.oss.template.OssTemplate;
import com.silky.starter.oss.template.impl.DefaultOssTemplateImpl;
import com.silky.starter.oss.thread.OssThreadPoolConfig;
import com.silky.starter.oss.thread.ThreadPoolManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;

/**
 * OSS自动配置类
 *
 * @author zy
 * @date 2025-08-11 17:18
 **/
@Configuration
@ConditionalOnClass(OssTemplate.class)
@EnableConfigurationProperties(OssProperties.class)
@Import({
        OssThreadPoolConfig.class, // 引入线程池配置
})
public class OssAutoConfiguration {

    private static final String OSS_PROVIDER_PROPERTY = "silky.oss.provider";


    @Bean
    public OssAdapterRegistry ossAdapterRegistry(List<OssProviderAdapter> adapterList) {
        OssAdapterRegistry registry = new OssAdapterRegistry();
        for (OssProviderAdapter adapter : adapterList) {
            registry.register(adapter.getProviderName(), adapter);
        }
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public MultiCloudPartStrategy multiCloudPartStrategy() {
        return new DefaultMultiCloudPartImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public ThreadPoolManager ossThreadPoolManager(ThreadPoolTaskExecutor ossThreadPoolTaskExecutor) {
        return new ThreadPoolManager(ossThreadPoolTaskExecutor);
    }

    @Bean
    @ConditionalOnMissingBean
    public OssTemplate ossTemplate(OssProperties properties,
                                   OssAdapterRegistry registry,
                                   MultiCloudPartStrategy partitionStrategy,
                                   ThreadPoolManager threadPoolManager) {
        return new DefaultOssTemplateImpl(registry, properties, partitionStrategy, threadPoolManager);
    }

    /**
     * 阿里云适配器
     *
     * @param properties 配置属性
     * @return AliyunOssAdapter
     */
    @Bean
    @ConditionalOnProperty(name = OSS_PROVIDER_PROPERTY, havingValue = OssConstants.A_LI_YUN)
    public AliyunOssAdapter aliyunOssAdapter(OssProperties properties) {
        return new AliyunOssAdapter(properties);
    }

    /**
     * 华为云适配器
     *
     * @param properties 配置属性
     * @return HuaWeiObsAdapter
     */
    @Bean
    @ConditionalOnProperty(name = OSS_PROVIDER_PROPERTY, havingValue = OssConstants.HUA_WEI)
    public HuaWeiObsAdapter huaWeiObsAdapter(OssProperties properties) {
        return new HuaWeiObsAdapter(properties);
    }

}
