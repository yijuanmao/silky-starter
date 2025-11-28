package com.silky.starter.mongodb.configure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 事务配置
 *
 * @author: zy
 * @date: 2025-11-19
 */
@Configuration
@EnableTransactionManagement
@ConditionalOnProperty(prefix = "silky.mongodb", name = "transaction-enabled", havingValue = "false")
public class TransactionConfig {

    /**
     * MongoDB 事务管理器
     */
    @Bean
    public PlatformTransactionManager transactionManager(MongoDatabaseFactory mongoDatabaseFactory) {
        return new MongoTransactionManager(mongoDatabaseFactory);
    }

    /**
     * 编程式事务模板
     */
    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
