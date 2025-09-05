package com.silky.starter.mongodb.autoconfigure;

import com.silky.starter.mongodb.properties.MongodbProperties;
import com.silky.starter.mongodb.template.MongodbTemplate;
import com.silky.starter.mongodb.template.impl.DefaultMongodbTemplateImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;

/**
 * mongodb自动配置类
 *
 * @author zy
 * @date 2025-09-04 17:57
 **/
@Configuration
@ConditionalOnClass(MongodbTemplate.class)
@EnableConfigurationProperties(MongodbProperties.class)
public class MongodbAutoConfiguration {

    /**
     * mongodbTemplate
     *
     * @param properties     MongodbProperties
     * @param mongoConverter MongoConverter
     * @param mongoTemplate  MongoTemplate
     * @return MongodbTemplate
     */
    @Bean
    @ConditionalOnMissingBean
    public MongodbTemplate mongodbTemplate(MongodbProperties properties,
                                           MongoConverter mongoConverter,
                                           MongoTemplate mongoTemplate) {
        return new DefaultMongodbTemplateImpl(properties, mongoConverter, mongoTemplate);
    }
}
