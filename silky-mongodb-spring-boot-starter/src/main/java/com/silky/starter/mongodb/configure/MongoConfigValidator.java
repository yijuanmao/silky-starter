package com.silky.starter.mongodb.configure;

import com.silky.starter.mongodb.properties.SilkyMongoProperties;
import com.silky.starter.mongodb.template.DynamicMongoTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * mongodb配置校验
 *
 * @author: zy
 * @date: 2025-11-28
 */
@Configuration
public class MongoConfigValidator {

    private static final Logger logger = LoggerFactory.getLogger(MongoConfigValidator.class);

    @Autowired
    private SilkyMongoProperties properties;

    @PostConstruct
    public void validatePrimaryConfig() {
        if (properties.getDatasource().isEmpty()) {
            logger.warn("No MongoDB data sources configured");
            return;
        }

        String primaryDataSource = properties.getPrimaryDataSourceName();
        logger.info("Primary data source configured as: '{}'", primaryDataSource);

        // 如果显式配置了primary但数据源不存在，记录警告
        if (properties.getPrimary() != null &&
                !properties.getDatasource().containsKey(properties.getPrimary())) {
            logger.warn("Configured primary data source '{}' not found in datasource list, " +
                    "using '{}' as primary", properties.getPrimary(), primaryDataSource);
        }
    }

    @Bean
    public CommandLineRunner validateMongoConfig(DynamicMongoTemplate dynamicMongoTemplate,
                                                 SilkyMongoProperties properties) {
        return args -> {
            logger.info("Validating MongoDB configuration...");
            logger.info("Number of configured data sources: {}", properties.getDatasource().size());

            String[] dataSourceNames = dynamicMongoTemplate.getDataSourceNames();
            logger.info("Available data sources in DynamicMongoTemplate: {}", String.join(", ", dataSourceNames));

            if (dataSourceNames.length == 0) {
                logger.warn("No data sources found in DynamicMongoTemplate!");
            } else {
                logger.info("MongoDB configuration validated successfully");
            }
        };
    }
}
