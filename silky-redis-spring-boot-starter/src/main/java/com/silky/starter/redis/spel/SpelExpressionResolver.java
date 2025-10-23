package com.silky.starter.redis.spel;

import cn.hutool.core.util.StrUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

/**
 * spel表达式解析器
 *
 * @author zy
 * @date 2025-10-23 09:56
 **/
public class SpelExpressionResolver {

    private static final ExpressionParser parser = new SpelExpressionParser();

    /**
     * 解析SpEL表达式
     *
     * @param expression SpEL表达式
     * @param joinPoint  切点
     */
    public String resolve(String expression, ProceedingJoinPoint joinPoint) {
        if (StrUtil.isBlank(expression) || !expression.contains("#")) {
            return expression;
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return resolve(expression, signature.getMethod(), joinPoint.getArgs());
    }

    /**
     * 解析SpEL表达式
     *
     * @param method     方法
     * @param expression SpEL表达式
     * @param args       方法参数
     */
    public String resolve(String expression, Method method, Object[] args) {
        if (expression == null || !expression.contains("#")) {
            return expression;
        }

        try {
            StandardEvaluationContext context = new StandardEvaluationContext();

            // 设置方法参数
            String[] parameterNames = getParameterNames(method);
            if (args != null) {
                for (int i = 0; i < parameterNames.length && i < args.length; i++) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }
            // 设置常用变量
            context.setVariable("methodName", method.getName());
            context.setVariable("className", method.getDeclaringClass().getSimpleName());

            Expression expr = parser.parseExpression(expression);
            Object value = expr.getValue(context);

            return value != null ? value.toString() : expression;

        } catch (Exception e) {
            // 解析失败返回原表达式
            return expression;
        }
    }

    /**
     * 获取方法参数名
     *
     * @param method 方法
     */
    private String[] getParameterNames(Method method) {
        try {
            java.lang.reflect.Parameter[] parameters = method.getParameters();
            String[] names = new String[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                names[i] = parameters[i].getName();
            }
            return names;
        } catch (Exception e) {
            // 如果获取失败，生成默认参数名
            int paramCount = method.getParameterCount();
            String[] names = new String[paramCount];
            for (int i = 0; i < paramCount; i++) {
                names[i] = "arg" + i;
            }
            return names;
        }
    }
}
