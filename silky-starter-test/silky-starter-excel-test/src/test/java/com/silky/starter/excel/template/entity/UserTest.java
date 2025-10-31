package com.silky.starter.excel.template.entity;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 用户测试实体类
 * @author zy
 * @date 2025-10-31 16:57
 **/
@Data
public class UserTest {

    /**
     * 姓名
     */
    @ExcelProperty(value = "姓名")
    private String name;

    /**
     * 手机号
     */
    @ExcelProperty(value = "手机号")
    private String phone;

    public UserTest() {
    }

    public UserTest(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }
}
