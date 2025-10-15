package com.silky.starter.rabbitmq;

import org.slf4j.Logger;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

/**
 * RabbitMq应用测试类
 */
@SpringBootTest(classes = RabbitMqApplicationTest.class)
@ComponentScan({"com.silky.**"})
public class RabbitMqApplicationTest {

    protected final Logger log = org.slf4j.LoggerFactory.getLogger(this.getClass());


}
