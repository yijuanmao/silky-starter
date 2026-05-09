package com.silky.starter.redis.stream.consumer;

import com.silky.starter.redis.stream.annotation.RedisStreamListener;
import com.silky.starter.redis.stream.config.RedisStreamProperties;
import com.silky.starter.redis.stream.core.StreamMessage;
import com.silky.starter.redis.stream.registry.MessageListenerContainerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * {@link RedisStreamListener} 注解处理器
 * <p>
 * 在 Spring 所有单例 Bean 初始化完成后（{@link SmartInitializingSingleton#afterSingletonsInstantiated}），
 * 扫描所有 Bean 的方法，找到带 {@link RedisStreamListener} 注解的方法，
 * 自动包装为 {@link com.silky.starter.redis.stream.core.MessageListener} 并注册到容器注册器。
 * </p>
 *
 * <p>消费方法签名要求：方法参数只有一个，且类型为 {@link StreamMessage}。</p>
 *
 * @author zy
 * @date 2026-05-09
 */
public class RedisStreamAnnotationProcessor implements SmartInitializingSingleton, ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(RedisStreamAnnotationProcessor.class);

    private ApplicationContext applicationContext;
    private final MessageListenerContainerRegistry registry;
    private final RedisStreamProperties properties;

    public RedisStreamAnnotationProcessor(MessageListenerContainerRegistry registry,
                                          RedisStreamProperties properties) {
        this.registry = registry;
        this.properties = properties;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 所有单例 Bean 初始化完成后，扫描并注册 {@link RedisStreamListener} 方法
     */
    @Override
    public void afterSingletonsInstantiated() {
        log.debug("[RedisStream] 开始扫描 @RedisStreamListener 注解...");
        Map<String, Object> beans = applicationContext.getBeansOfType(Object.class, false, false);
        int count = 0;
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            count += processBean(entry.getKey(), entry.getValue());
        }
        log.info("[RedisStream] @RedisStreamListener 扫描完成，共注册 {} 个消费者方法", count);
    }

    private int processBean(String beanName, Object bean) {
        int count = 0;
        Class<?> targetClass = getTargetClass(bean);
        for (Method method : targetClass.getDeclaredMethods()) {
            RedisStreamListener annotation = method.getAnnotation(RedisStreamListener.class);
            if (annotation == null) {
                continue;
            }
            validateMethod(beanName, method);
            registerListener(bean, method, annotation);
            count++;
        }
        return count;
    }

    private void validateMethod(String beanName, Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length != 1 || !StreamMessage.class.isAssignableFrom(paramTypes[0])) {
            throw new IllegalArgumentException(
                    "[RedisStream] @RedisStreamListener 方法参数必须只有一个 StreamMessage 类型: "
                            + beanName + "#" + method.getName()
                            + "，当前参数类型: " + (paramTypes.length > 0 ? paramTypes[0].getName() : "空"));
        }
    }

    private void registerListener(Object bean, Method method, RedisStreamListener annotation) {
        String streamKey = annotation.stream();
        String groupName = annotation.group().isEmpty() ? properties.getDefaultGroup() : annotation.group();
        String consumerPrefix = annotation.consumer().isEmpty()
                ? resolveConsumerPrefix(bean.getClass().getSimpleName())
                : annotation.consumer();
        int concurrency = annotation.concurrency() > 0 ? annotation.concurrency()
                : properties.getConsumer().getConcurrency();

        method.setAccessible(true);

        // 包装为 MessageListener
        com.silky.starter.redis.stream.core.MessageListener listener = message -> {
            try {
                return method.invoke(bean, message);
            } catch (java.lang.reflect.InvocationTargetException e) {
                // 解包反射异常，让框架感知业务异常
                Throwable cause = e.getCause();
                if (cause instanceof Exception) {
                    throw (Exception) cause;
                }
                throw new RuntimeException(cause);
            }
        };

        registry.register(streamKey, groupName, consumerPrefix, concurrency, listener);

        log.info("[RedisStream] 注册 @RedisStreamListener: bean={}.{}, stream={}, group={}, concurrency={}",
                bean.getClass().getSimpleName(), method.getName(), streamKey, groupName, concurrency);
    }

    /**
     * 解包 CGLIB/JDK 代理，获取真实目标类
     */
    private Class<?> getTargetClass(Object bean) {
        Class<?> clazz = bean.getClass();
        // 检测 Spring CGLIB 代理
        if (clazz.getName().contains("$$")) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null && superClass != Object.class) {
                return superClass;
            }
        }
        return clazz;
    }

    private String resolveConsumerPrefix(String beanSimpleName) {
        try {
            String host = java.net.InetAddress.getLocalHost().getHostName();
            String shortName = beanSimpleName.length() > 15
                    ? beanSimpleName.substring(0, 15) : beanSimpleName;
            return host + "-" + shortName;
        } catch (Exception e) {
            return "silky-" + beanSimpleName;
        }
    }
}
