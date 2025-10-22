package com.silky.starter.redis.lock.aspect;

import com.silky.starter.redis.lock.annotation.RedisLock;
import com.silky.starter.redis.lock.enums.LockType;
import com.silky.starter.redis.lock.exception.RedissonException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Objects;

/**
 * redis分布式锁切面
 *
 * @author zy
 * @date 2025-10-21 15:34
 **/
@Aspect
@Order(Ordered.LOWEST_PRECEDENCE - 1) // 在事务注解之前执行
public class RedisLockAspect {

    private static final Logger log = LoggerFactory.getLogger(RedisLockAspect.class);

    private final RedissonClient redissonClient;

    public RedisLockAspect(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * redis分布式锁切面
     *
     * @param joinPoint joinPoint
     * @param redisLock redisLock
     * @return Object
     */
    @Around("@annotation(redisLock)")
    public Object around(ProceedingJoinPoint joinPoint, RedisLock redisLock) throws Throwable {
        String lockKey = resolveKey(joinPoint, redisLock);
        RLock lock = getLock(redisLock.lockType(), lockKey);

        boolean isLocked = false;
        try {
            // 尝试获取锁
            isLocked = lock.tryLock(redisLock.waitTime(), redisLock.leaseTime(), redisLock.timeUnit());

            if (!isLocked) {
                throw new RedissonException("获取分布式锁失败, key: " + lockKey);
            }
            final RLock finalLock = lock;
            final boolean requiresTransactionRelease = redisLock.releaseAfterTransaction() &&
                    TransactionSynchronizationManager.isActualTransactionActive();

            if (requiresTransactionRelease) {
                // 如果有事务，在事务提交后释放锁
                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCompletion(int status) {
                                if (lock.isLocked() && finalLock.isHeldByCurrentThread()) {
                                    finalLock.unlock();
                                }
                            }
                        }
                );
                return joinPoint.proceed();
            } else {
                // 没有事务，直接执行
                try {
                    return joinPoint.proceed();
                } finally {
                    if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            }
        } catch (Exception e) {
            if (isLocked && lock.isHeldByCurrentThread() &&
                    !(redisLock.releaseAfterTransaction() && TransactionSynchronizationManager.isActualTransactionActive())) {
                lock.unlock();
            }
            throw e;
        }
    }

    /**
     * 解析锁key
     *
     * @param joinPoint joinPoint
     * @param redisLock redisLock
     * @return lockKey
     */
    private String resolveKey(ProceedingJoinPoint joinPoint, RedisLock redisLock) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String keyExpression = redisLock.key();
        // 如果key不包含SpEL表达式标记，直接返回
        if (!keyExpression.contains("#")) {
            return keyExpression;
        }
        return parseSpelExpression(keyExpression, signature, joinPoint.getArgs());
    }

    /**
     * 获取锁对象
     *
     * @param lockType 锁类型
     * @param key      key
     * @return RLock
     */
    private RLock getLock(LockType lockType, String key) {
        switch (lockType) {
            case FAIR:
                return redissonClient.getFairLock(key);
            case READ:
                RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(key);
                return readWriteLock.readLock();
            case WRITE:
                RReadWriteLock writeLock = redissonClient.getReadWriteLock(key);
                return writeLock.writeLock();
            case REENTRANT:
            default:
                return redissonClient.getLock(key);
        }
    }

    /**
     * 解析SpEL表达式
     */
    private String parseSpelExpression(String expression, MethodSignature signature, Object[] args) {
        try {
            // 创建表达式解析器
            ExpressionParser parser = new SpelExpressionParser();
            StandardEvaluationContext context = new StandardEvaluationContext();

            // 设置方法参数到上下文
            String[] parameterNames = signature.getParameterNames();
            if (parameterNames != null) {
                for (int i = 0; i < parameterNames.length; i++) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }
            // 设置其他常用变量
            context.setVariable("methodName", signature.getMethod().getName());
            context.setVariable("className", signature.getDeclaringType().getSimpleName());

            // 解析表达式
            Expression expr = parser.parseExpression(expression);
            Object value = expr.getValue(context);

            return Objects.isNull(value) ? expression : value.toString();

        } catch (Exception e) {
            // 解析失败时返回原始表达式并记录警告
            log.warn("SpEL表达式解析失败: {}, 使用原始表达式: {}", expression, e.getMessage());
            return expression;
        }
    }
}
