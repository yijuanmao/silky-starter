package com.silky.starter.redis.sequence.aspect;

import com.silky.starter.redis.sequence.annotation.RedisSequence;
import com.silky.starter.redis.sequence.template.RedisSequenceTemplate;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * redis分布式单号切面
 *
 * @author zy
 * @date 2025-10-23 15:17
 **/
@Aspect
public class RedisSequenceAspect {

    private final RedisSequenceTemplate redisSequenceTemplate;

    public RedisSequenceAspect(RedisSequenceTemplate redisSequenceTemplate) {
        this.redisSequenceTemplate = redisSequenceTemplate;
    }

    /**
     * redis分布式单号切面
     *
     * @param joinPoint joinPoint
     * @param sequence  sequence
     * @return Object
     */
    @Around("@annotation(sequence)")
    public Object generateSequence(ProceedingJoinPoint joinPoint, RedisSequence sequence) {
        return redisSequenceTemplate.generate(sequence.redisKey(), sequence.prefix(), sequence.datePattern(),
                sequence.sequenceLength(), sequence.randomLength(), sequence.expire(), sequence.timeUnit());
    }
}
