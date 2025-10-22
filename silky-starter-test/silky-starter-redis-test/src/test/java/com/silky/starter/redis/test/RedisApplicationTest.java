package com.silky.starter.redis.test;

import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

/**
 * RabbitMq应用测试类
 */
@SpringBootTest(classes = RedisApplicationTest.class)
@ComponentScan({"com.silky.**"})
@SpringBootApplication
public class RedisApplicationTest {

    protected final Logger log = org.slf4j.LoggerFactory.getLogger(this.getClass());

}
