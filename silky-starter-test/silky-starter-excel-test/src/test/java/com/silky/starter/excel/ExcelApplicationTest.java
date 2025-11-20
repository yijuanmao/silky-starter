package com.silky.starter.excel;

import org.slf4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

/**
 * Excel应用测试类
 */
@SpringBootApplication
@SpringBootTest(classes = ExcelApplicationTest.class)
@ComponentScan({"com.silky", "com.silky.**"})
public class ExcelApplicationTest {

    protected final Logger log = org.slf4j.LoggerFactory.getLogger(this.getClass());

    public static void main(String[] args) {
        SpringApplication.run(ExcelApplicationTest.class, args);
    }

}
