package com.silky.starter.mongodb.aspect;

import com.silky.starter.mongodb.properties.SilkyMongoProperties;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

/**
 * Mongo日志切面
 *
 * @author: zy
 * @date: 2025-11-19
 */
@Aspect
@Component
public class MongoLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(MongoLogAspect.class);

    @Autowired
    private SilkyMongoProperties properties;

    @Pointcut("execution(* com.silky.starter.mongodb.template.SilkyMongoTemplate.*(..))")
    public void mongoOperation() {
    }

//    @Pointcut("execution(* com.silky.starter.mongodb.service.AsyncMongoService.*(..))")
//    public void asyncOperation() {
//    }

    @Around("mongoOperation() ")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!properties.isPrintLog()) {
            return joinPoint.proceed();
        }
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            if (properties.isShowSql()) {
                logOperation(className, methodName, joinPoint.getArgs(), executionTime, result);
            } else {
                logger.info("{}.{} executed in {} ms", className, methodName, executionTime);
            }
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("{}.{} failed in {} ms with error: {}",
                    className, methodName, executionTime, e.getMessage());
            throw e;
        }
    }

    /**
     * 记录操作日志
     *
     * @param className     类名
     * @param methodName    方法名
     * @param args          参数
     * @param executionTime 执行时间
     * @param result        结果
     */
    private void logOperation(String className, String methodName, Object[] args,
                              long executionTime, Object result) {
        StringBuilder logMsg = new StringBuilder();
        logMsg.append(className).append(".").append(methodName).append(" - ");
        logMsg.append("Time: ").append(executionTime).append("ms");

        // 记录参数信息
        if (args != null && args.length > 0) {
            logMsg.append(" | Args: ");
            for (Object arg : args) {
                if (arg instanceof Query) {
                    logMsg.append("Query: ").append(arg).append("; ");
                } else if (arg instanceof Update) {
                    logMsg.append("Update: ").append(arg).append("; ");
                } else if (arg instanceof Class) {
                    logMsg.append("EntityClass: ").append(((Class<?>) arg).getSimpleName()).append("; ");
                } else {
                    logMsg.append(arg).append("; ");
                }
            }
        }

        logger.info(logMsg.toString());
    }
}
