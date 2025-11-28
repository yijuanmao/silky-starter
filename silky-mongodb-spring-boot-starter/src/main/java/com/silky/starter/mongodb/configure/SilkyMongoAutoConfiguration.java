package com.silky.starter.mongodb.configure;

import com.silky.starter.mongodb.aspect.DataSourceAspect;
import com.silky.starter.mongodb.aspect.MongoLogAspect;
import com.silky.starter.mongodb.properties.SilkyMongoProperties;
import com.silky.starter.mongodb.template.DynamicMongoTemplate;
import com.silky.starter.mongodb.template.SilkyMongoTemplate;
import com.silky.starter.mongodb.template.impl.DefaultMongodbTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

/**
 * mongodb自动配置类
 *
 * @author zy
 * @date 2025-09-04 17:57
 **/
@Configuration
@ConditionalOnClass(name = "org.springframework.data.mongodb.core.MongoTemplate")
@EnableConfigurationProperties(SilkyMongoProperties.class)
@Import({MultiDataSourceConfig.class, TransactionConfig.class, MongoConfigValidator.class})
@EnableAspectJAutoProxy
public class SilkyMongoAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(SilkyMongoAutoConfiguration.class);


    /**
     * MongoLogAspect - 确保切面被注册
     */
    @Bean
    @ConditionalOnMissingBean
    public MongoLogAspect mongoLogAspect(SilkyMongoProperties properties,
                                         MappingMongoConverter mappingMongoConverter) {
        return new MongoLogAspect(properties, mappingMongoConverter);
    }

    /**
     * mongodbTemplate
     *
     * @param dynamicMongoTemplate 多数据源模板
     * @return MongodbTemplate
     */
    @Bean
    @ConditionalOnMissingBean
    public SilkyMongoTemplate silkyMongoTemplate(DynamicMongoTemplate dynamicMongoTemplate) {
        return new DefaultMongodbTemplate(dynamicMongoTemplate);
    }

    /**
     * DataSourceAspect - 添加条件确保 DynamicMongoTemplate 存在
     */
    @Bean
    @ConditionalOnMissingBean
    public DataSourceAspect dataSourceAspect(DynamicMongoTemplate dynamicMongoTemplate) {
        logger.info("Creating DataSourceAspect bean");
        return new DataSourceAspect(dynamicMongoTemplate);
    }


}
