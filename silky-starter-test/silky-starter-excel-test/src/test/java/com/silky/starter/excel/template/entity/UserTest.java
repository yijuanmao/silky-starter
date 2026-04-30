package com.silky.starter.excel.template.entity;

import cn.idev.excel.annotation.ExcelProperty;
import com.silky.starter.excel.core.annotation.ExcelDict;
import com.silky.starter.excel.core.annotation.ExcelEnum;
import com.silky.starter.excel.core.annotation.ExcelMask;
import lombok.Data;

/**
 * 用户测试实体类 - 包含注解驱动的字段转换示例
 *
 * @author zy
 */
@Data
public class UserTest {

    @ExcelProperty(value = "姓名")
    @ExcelMask(strategy = ExcelMask.MaskStrategy.NAME)
    private String name;

    @ExcelProperty(value = "手机号")
    @ExcelMask(strategy = ExcelMask.MaskStrategy.PHONE)
    private String phone;

    @ExcelProperty(value = "邮箱")
    @ExcelMask(strategy = ExcelMask.MaskStrategy.EMAIL)
    private String email;

    @ExcelProperty(value = "状态")
    @ExcelEnum(enumClass = UserStatus.class, codeField = "code", labelField = "label")
    private Integer status;

    @ExcelProperty(value = "性别")
    @ExcelDict(dictCode = "gender")
    private Integer gender;

    @ExcelProperty(value = "身份证号")
    @ExcelMask(strategy = ExcelMask.MaskStrategy.ID_CARD)
    private String idCard;

    @ExcelProperty(value = "银行卡号")
    @ExcelMask(strategy = ExcelMask.MaskStrategy.BANK_CARD)
    private String bankCard;

    public UserTest() {
    }

    public UserTest(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }
}
