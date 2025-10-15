package com.silky.starter.rabbitmq;

import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

/**
 * RabbitMq应用测试类
 */
//@EnableAutoConfiguration
@SpringBootTest(classes = RabbitMqApplicationTest.class)
@ComponentScan({"com.silky.**"})
@SpringBootApplication
public class RabbitMqApplicationTest {

    protected final Logger log = org.slf4j.LoggerFactory.getLogger(this.getClass());


}
