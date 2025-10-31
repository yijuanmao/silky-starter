package com.silky.starter.excel;

import org.slf4j.Logger;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

/**
 * OSS应用测试类
 */
@SpringBootTest(classes = ExcelApplicationTest.class)
@ComponentScan({"com.silky.**"})
public class ExcelApplicationTest {

    protected final Logger log = org.slf4j.LoggerFactory.getLogger(this.getClass());


}
