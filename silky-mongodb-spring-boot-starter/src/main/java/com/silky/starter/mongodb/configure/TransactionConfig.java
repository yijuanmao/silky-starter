package com.silky.starter.mongodb.configure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 事务配置
 *
 * @author: zy
 * @date: 2025-11-19
 */
//@Configuration
//public class TransactionConfig {
//
//    @Bean
//    public MongoTransactionManager transactionManager(MongoTemplate mongoTemplate) {
//        return new MongoTransactionManager(mongoTemplate.getMongoDatabaseFactory());
//    }
//
//    @Bean
//    public TransactionTemplate transactionTemplate(MongoTransactionManager transactionManager) {
//        return new TransactionTemplate(transactionManager);
//    }
//}
