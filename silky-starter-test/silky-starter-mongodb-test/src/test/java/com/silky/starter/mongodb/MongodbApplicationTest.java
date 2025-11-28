package com.silky.starter.mongodb;

import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Mongodb应用测试类
 */
//@SpringBootApplication(exclude = {
//        MongoAutoConfiguration.class,
//        MongoDataAutoConfiguration.class
//})
//@EnableAutoConfiguration(exclude = {MongoDataAutoConfiguration.class, MongoRepositoriesAutoConfiguration.class})
//@EnableMongoRepositories
@SpringBootTest(classes = MongodbApplicationTest.class)
@ComponentScan({"com.silky"})
public class MongodbApplicationTest {

    protected final Logger log = org.slf4j.LoggerFactory.getLogger(this.getClass());

}
