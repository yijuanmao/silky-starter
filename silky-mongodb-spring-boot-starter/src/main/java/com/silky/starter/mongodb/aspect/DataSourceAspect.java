package com.silky.starter.mongodb.aspect;

import com.silky.starter.mongodb.annotation.DataSource;
import com.silky.starter.mongodb.configure.DynamicMongoTemplate;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;

/**
 * 数据源切面
 *
 * @author: zy
 * @date: 2025-11-19
 */
@Aspect
public class DataSourceAspect {

    private final DynamicMongoTemplate dynamicMongoTemplate;

    public DataSourceAspect(DynamicMongoTemplate dynamicMongoTemplate) {
        this.dynamicMongoTemplate = dynamicMongoTemplate;
    }

    /**
     * 环绕通知
     *
     * @param point 切点
     * @return
     * @throws Throwable
     */
    @Around("@annotation(com.silky.starter.mongodb.annotation.DataSource) || " +
            "@within(com.silky.starter.mongodb.annotation.DataSource)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        // 获取数据源注解
        DataSource dataSource = getDataSource(point);
        if (dataSource != null) {
            String dataSourceName = dataSource.value();
            boolean readOnly = dataSource.readOnly();

            // 保存原始数据源
            String originalDataSource = dynamicMongoTemplate.getDataSource();

            try {
                // 切换到目标数据源
                dynamicMongoTemplate.setDataSource(dataSourceName);

                // 执行目标方法
                return point.proceed();
            } finally {
                // 恢复原始数据源
                if (originalDataSource != null) {
                    dynamicMongoTemplate.setDataSource(originalDataSource);
                } else {
                    dynamicMongoTemplate.clearDataSource();
                }
            }
        }

        return point.proceed();
    }

    /**
     * 获取数据源注解
     *
     * @param point 切点
     * @return 数据源注解
     */
    private DataSource getDataSource(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        // 先检查方法上的注解
        DataSource dataSource = AnnotationUtils.findAnnotation(method, DataSource.class);
        if (dataSource != null) {
            return dataSource;
        }
        // 再检查类上的注解
        return AnnotationUtils.findAnnotation(signature.getDeclaringType(), DataSource.class);
    }
}
