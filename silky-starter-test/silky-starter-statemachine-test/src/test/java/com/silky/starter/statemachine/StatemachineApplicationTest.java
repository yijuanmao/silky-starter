package com.silky.starter.statemachine;

import org.slf4j.Logger;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

/**
 * OSS应用测试类
 */
@SpringBootTest(classes = StatemachineApplicationTest.class)
@ComponentScan({"com.silky.**"})
public class StatemachineApplicationTest {

    protected final Logger log = org.slf4j.LoggerFactory.getLogger(this.getClass());


}
