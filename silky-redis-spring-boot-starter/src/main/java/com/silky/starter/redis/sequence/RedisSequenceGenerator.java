package com.silky.starter.redis.sequence;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
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
public class RedisSequenceGenerator {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisSequenceGenerator(RedisTemplate<String, Object> redisTemplate) {
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
        return this.generate(redisKey, prefix, DatePattern.PURE_DATETIME_PATTERN, sequenceLength, 24, TimeUnit.HOURS);
    }

    /**
     * 设置当天过期时间 （在当天23:59:59过期）
     *
     * @param prefix 前缀
     * @param len    长度
     * @param key    缓存key
     * @return String
     */
    public String genNowSerialNumber(String prefix, int len, String key) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = LocalDateTimeUtil.endOfDay(now);
        String serial = Convert.toStr(redisTemplate.opsForValue().increment(key, 1));
        redisTemplate.expire(key, LocalDateTimeUtil.between(now, endTime).toMillis(), TimeUnit.MILLISECONDS);
        return prefix + StrUtil.fillBefore(serial, '0', len);
    }

    /**
     * 生成序列号（带过期时间）
     *
     * @param key            redis 缓存key
     * @param prefix         前缀
     * @param pattern        序列号规则
     * @param sequenceLength 序列号长度
     * @param expire         过期时间
     * @param timeUnit       过期时间单位
     */
    @SuppressWarnings("all")
    public String generate(String redisKey, String prefix, String pattern, int sequenceLength, long expire, TimeUnit timeUnit) {
        // 解析pattern中的时间格式
        String timePart = LocalDateTimeUtil.format(LocalDateTime.now(), pattern);
        RedisAtomicLong counter = new RedisAtomicLong(redisKey, redisTemplate.getConnectionFactory());
        // 获取自增序列
        long sequence = counter.incrementAndGet();
        if (sequence == 1) {
            // 设置过期时间
            counter.expire(expire, timeUnit);
        }
        String sequenceStr = Convert.toStr(sequence);
        if (sequenceStr.length() > sequenceLength) {
            sequenceStr = StrUtil.sub(sequenceStr, sequenceStr.length() - sequenceLength, sequenceStr.length());
        }
        return prefix + timePart + StrUtil.fillBefore(sequenceStr, '0', sequenceLength);
    }
}
