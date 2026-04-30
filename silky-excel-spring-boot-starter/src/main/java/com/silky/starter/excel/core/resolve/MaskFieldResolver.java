package com.silky.starter.excel.core.resolve;

import cn.hutool.core.util.DesensitizedUtil;
import com.silky.starter.excel.core.annotation.ExcelMask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据脱敏解析器
 * 对敏感字段进行脱敏处理
 * <p>
 * 执行顺序：原值 -> 枚举翻译 -> 字典翻译 -> 脱敏
 *
 * @author zy
 * @since 1.1.0
 */
@Slf4j
public class MaskFieldResolver implements ExcelFieldResolver {

    /**
     * 支持的注解类型
     */
    @Override
    public boolean supports(Field field, java.lang.annotation.Annotation annotation) {
        return annotation instanceof ExcelMask;
    }

    /**
     * 对字段进行脱敏处理
     *
     * @param field      字段
     * @param annotation 注解
     * @param fieldValue 字段值
     * @param context    解析上下文
     * @return 脱敏后的字段值
     */
    @Override
    public Object resolve(Field field, java.lang.annotation.Annotation annotation, Object fieldValue, ResolveContext context) {
        if (fieldValue == null) {
            return null;
        }

        String strValue = String.valueOf(fieldValue);
        if (strValue.isEmpty()) {
            return strValue;
        }

        ExcelMask excelMask = (ExcelMask) annotation;
        try {
            return doMask(strValue, excelMask);
        } catch (Exception e) {
            log.warn("脱敏处理失败: field={}, value={}", field.getName(), fieldValue, e);
            return fieldValue;
        }
    }

    /**
     * 获取执行顺序
     * 脱敏处理优先级最高
     *
     * @return 执行顺序
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }

    /**
     * 执行脱敏处理
     *
     * @param value 待处理字段值
     * @param mask  脱敏注解
     * @return 脱敏后的字段值
     */
    private String doMask(String value, ExcelMask mask) {
        char maskChar = mask.maskChar();
        ExcelMask.MaskStrategy strategy = mask.strategy();

        switch (strategy) {
            case PHONE:
                return DesensitizedUtil.mobilePhone(value);
            case ID_CARD:
                return DesensitizedUtil.idCardNum(value, 3, 4);
            case EMAIL:
                return DesensitizedUtil.email(value);
            case NAME:
                if (value.length() <= 1) {
                    return String.valueOf(maskChar);
                }
                return value.charAt(0) + repeat(maskChar, value.length() - 1);
            case BANK_CARD:
                return DesensitizedUtil.bankCard(value);
            case PARTIAL:
                int keepPrefix = mask.keepPrefix();
                int keepSuffix = mask.keepSuffix();
                if (value.length() <= keepPrefix + keepSuffix) {
                    return value;
                }
                return maskByRange(value, keepPrefix, value.length() - keepSuffix, maskChar);
            case CUSTOM:
                String pattern = mask.customPattern();
                if (pattern == null || pattern.isEmpty()) {
                    return value;
                }
                return maskByPattern(value, pattern, maskChar);
            default:
                return value;
        }
    }

    /**
     * 按指定范围进行脱敏
     *
     * @param value      待处理字段值
     * @param keepPrefix 保留前缀长度
     * @param maskFrom   脱敏起始位置
     * @param maskChar   脱敏字符
     * @return 脱敏后的字段值
     */
    private String maskByRange(String value, int keepPrefix, int maskFrom, char maskChar) {
        if (value.length() <= keepPrefix) {
            return value;
        }
        int maskEnd = Math.max(maskFrom, keepPrefix);
        if (maskEnd >= value.length()) {
            return value;
        }
        return value.substring(0, keepPrefix)
                + repeat(maskChar, maskEnd - keepPrefix)
                + value.substring(maskEnd);
    }

    /**
     * 按指定正则进行脱敏
     *
     * @param value    待处理字段值
     * @param pattern  正则表达式
     * @param maskChar 脱敏字符
     * @return 脱敏后的字段值
     */
    private String maskByPattern(String value, String pattern, char maskChar) {
        Matcher matcher = Pattern.compile(pattern).matcher(value);
        if (!matcher.matches()) {
            return value;
        }
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        for (int i = 1; i <= matcher.groupCount(); i++) {
            sb.append(value, lastEnd, matcher.start(i));
            lastEnd = matcher.end(i);
        }
        // 中间部分替换
        int maskStart = matcher.start(1) + matcher.group(1).length();
        int maskEnd = matcher.start(matcher.groupCount());
        if (maskEnd > maskStart) {
            sb.append(repeat(maskChar, maskEnd - maskStart));
        }
        // 最后一个分组
        sb.append(matcher.group(matcher.groupCount()));
        return sb.toString();
    }

    /**
     * 重复指定字符
     *
     * @param ch    字符
     * @param count 重复次数
     * @return 重复的字符串
     */
    private String repeat(char ch, int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }
}
