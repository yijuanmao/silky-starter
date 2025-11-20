package com.silky.starter.mongodb.configure;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.silky.starter.mongodb.core.utils.MongoUriParser;
import com.silky.starter.mongodb.properties.SilkyMongoProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 多数据源配置
 *
 * @author: zy
 * @date: 2025-11-19
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "silky.mongodb", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MultiDataSourceConfig {

    @Autowired
    private SilkyMongoProperties properties;

    /**
     * 创建主 MongoDatabaseFactory（使用第一个数据源作为默认）
     */
    @Bean
    @Primary
    public MongoDatabaseFactory mongoDatabaseFactory() {
        log.info("Creating primary MongoDatabaseFactory");
        if (properties.getDatasource().isEmpty()) {
            throw new IllegalStateException("No MongoDB data source configured");
        }

        String firstDataSourceName = properties.getDatasource().keySet().iterator().next();
        SilkyMongoProperties.SilkyDataSourceProperties firstDataSource = properties.getDatasource().get(firstDataSourceName);

        String databaseName = StringUtils.hasText(firstDataSource.getDatabase())
                ? firstDataSource.getDatabase()
                : MongoUriParser.getDatabaseFromUri(firstDataSource.getUri());

        return createMongoFactory(firstDataSource.getUri(), databaseName);
    }

    @Bean
    public MongoMappingContext mongoMappingContext() {
        return new MongoMappingContext();
    }

    @Bean
    public MappingMongoConverter mappingMongoConverter(MongoDatabaseFactory mongoDatabaseFactory,
                                                       MongoMappingContext mongoMappingContext) {
        DbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoDatabaseFactory);
        MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mongoMappingContext);
        return converter;
    }

    /**
     * 创建动态多数据源模板
     */
    @Bean
    public DynamicMongoTemplate dynamicMongoTemplate(MappingMongoConverter mappingMongoConverter) {
        log.info("Creating DynamicMongoTemplate with {} data sources", properties.getDatasource().size());

        Map<Object, Object> targetDataSources = new HashMap<>();

        properties.getDatasource().forEach((dataSourceName, dataSourceProps) -> {
            log.info("Configuring data source: {}", dataSourceName);

            String databaseName = StringUtils.hasText(dataSourceProps.getDatabase())
                    ? dataSourceProps.getDatabase()
                    : MongoUriParser.getDatabaseFromUri(dataSourceProps.getUri());

            // 主数据源
            MongoDatabaseFactory primaryFactory = createMongoFactory(dataSourceProps.getUri(), databaseName);
            MongoTemplate primaryTemplate = new MongoTemplate(primaryFactory, mappingMongoConverter);
            targetDataSources.put(dataSourceName, primaryTemplate);
            log.info("Added primary data source: {}", dataSourceName);

            // 读写分离的读数据源
            if (dataSourceProps.getReadWriteSeparation() != null &&
                    dataSourceProps.getReadWriteSeparation().isEnabled()) {

                String readDatabaseName = StringUtils.hasText(
                        dataSourceProps.getReadWriteSeparation().getReadDatabase())
                        ? dataSourceProps.getReadWriteSeparation().getReadDatabase()
                        : MongoUriParser.getDatabaseFromUri(
                        dataSourceProps.getReadWriteSeparation().getReadUri());

                MongoDatabaseFactory readFactory = createMongoFactory(
                        dataSourceProps.getReadWriteSeparation().getReadUri(),
                        readDatabaseName);
                MongoTemplate readTemplate = new MongoTemplate(readFactory, mappingMongoConverter);
                targetDataSources.put(dataSourceName + "_read", readTemplate);
                log.info("Added read-only data source: {}", dataSourceName + "_read");
            }
        });

        DynamicMongoTemplate dynamicTemplate = new DynamicMongoTemplate(targetDataSources);
        log.info("DynamicMongoTemplate created successfully with {} templates", targetDataSources.size());
        return dynamicTemplate;
    }

    private MongoDatabaseFactory createMongoFactory(String uri, String database) {
        try {
            MongoClient mongoClient = MongoClients.create(uri);
            return new SimpleMongoClientDatabaseFactory(mongoClient, database);
        } catch (Exception e) {
            log.error("Failed to create MongoDatabaseFactory for URI: {}", uri, e);
            throw new RuntimeException("Failed to create MongoDatabaseFactory", e);
        }
    }
}
