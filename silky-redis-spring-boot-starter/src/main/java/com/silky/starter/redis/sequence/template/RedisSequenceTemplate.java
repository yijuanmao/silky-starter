package com.silky.starter.redis.sequence.template;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * redis序列号生成器默认实现
 *
 * @author zy
 * @date 2025-10-22 14:56
 **/
public class RedisSequenceTemplate {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisSequenceTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 生成序列号
     *
     * @param redisKey       redis 缓存key
     * @param prefix         前缀
     * @param sequenceLength 序列号长度
     */
    public String generate(String redisKey, String prefix, int sequenceLength) {
        return this.generate(redisKey, prefix, DatePattern.PURE_DATETIME_PATTERN, sequenceLength, 6);
    }

    /**
     * 生成序列号（带过期时间）
     *
     * @param redisKey       redis 缓存key
     * @param prefix         前缀
     * @param pattern        序列号规则
     * @param sequenceLength 序列号长度
     * @param randomLength   随机数长度
     */
    public String generate(String redisKey, String prefix, String pattern, int sequenceLength, int randomLength) {
        return this.generate(redisKey, prefix, pattern, sequenceLength, randomLength, 1, TimeUnit.DAYS);
    }


    /**
     * 生成序列号（带过期时间）
     *
     * @param redisKey       redis 缓存key
     * @param prefix         前缀
     * @param pattern        序列号规则
     * @param sequenceLength 序列号长度
     * @param randomLength   随机数长度
     * @param expire         过期时间
     * @param timeUnit       过期时间单位
     */
    @SuppressWarnings("all")
    public String generate(String redisKey, String prefix, String pattern, int sequenceLength, int randomLength, long expire, TimeUnit timeUnit) {
        // 解析pattern中的时间格式
        String timePart = LocalDateTimeUtil.format(LocalDateTime.now(), pattern);
        RedisAtomicLong counter = new RedisAtomicLong(redisKey, redisTemplate.getConnectionFactory());
        // 获取自增序列
        long sequence = counter.incrementAndGet();
/*        if (sequence == 1) {
            // 设置过期时间
            counter.expire(expire, timeUnit);
        }
        String sequenceStr = Convert.toStr(sequence);
        if (sequenceStr.length() > sequenceLength) {
            sequenceStr = StrUtil.sub(sequenceStr, sequenceStr.length() - sequenceLength, sequenceStr.length());
        }
        return prefix + timePart + StrUtil.fillBefore(sequenceStr, '0', sequenceLength);*/

        if (sequence == 1) {
            // 设置过期时间
            counter.expire(expire, timeUnit);
        }
        // 格式化序列号
        String sequenceStr = String.format("%0" + sequenceLength + "d", sequence);

        // 生成随机数部分
        String randomStr = "";
        if (randomLength > 0) {
            long random = (long) (Math.random() * Math.pow(10, randomLength));
            randomStr = String.format("%0" + randomLength + "d", random);
        }
        return prefix + timePart + sequenceStr + randomStr;
    }

    /**
     * 设置当天过期时间 （在当天23:59:59过期）
     *
     * @param redisKey 缓存key
     * @param prefix   前缀
     * @param len      长度
     * @return String
     */
    public String genNowSerialNumber(String redisKey, String prefix, int len) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = LocalDateTimeUtil.endOfDay(now);
        String serial = Convert.toStr(redisTemplate.opsForValue().increment(redisKey, 1));
        redisTemplate.expire(redisKey, LocalDateTimeUtil.between(now, endTime).toMillis(), TimeUnit.MILLISECONDS);
        return prefix + StrUtil.fillBefore(serial, '0', len);
    }
}
