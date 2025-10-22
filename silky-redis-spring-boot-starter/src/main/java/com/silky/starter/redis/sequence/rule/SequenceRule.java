package com.silky.starter.redis.sequence.rule;

import cn.hutool.core.date.DatePattern;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * redis序列号生成规则
 *
 * @author zy
 * @date 2025-10-22 14:42
 **/
@Data
@ToString
@Accessors(chain = true)
public class SequenceRule {

    /**
     * 前缀
     */
    private String prefix;

    /**
     * 时间格式, 默认yyyyMMddHHmmss
     */
    private String datePattern = DatePattern.PURE_DATETIME_PATTERN;

    /**
     * 序列号长度
     */
    private int sequenceLength = 6;

    /**
     * 随机数长度
     */
    private int randomLength = 3;

    /**
     * 过期天数
     */
    private int expireDays = 1;

    public SequenceRule() {
    }

    public SequenceRule(String prefix, String datePattern, int sequenceLength, int randomLength, int expireDays) {
        this.prefix = prefix;
        this.datePattern = datePattern;
        this.sequenceLength = sequenceLength;
        this.randomLength = randomLength;
        this.expireDays = expireDays;
    }
}
