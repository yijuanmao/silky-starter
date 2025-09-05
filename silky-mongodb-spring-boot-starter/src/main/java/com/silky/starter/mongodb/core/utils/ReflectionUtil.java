package com.silky.starter.mongodb.core.utils;

import cn.hutool.core.util.StrUtil;
import com.silky.starter.mongodb.support.SerializableFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.beans.Introspector;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 反射工具类
 *
 * @author zy
 * @date 2022-11-22 10:59
 **/
public class ReflectionUtil {

    private static final Logger logger = LoggerFactory.getLogger(ReflectionUtil.class);

    /**
     * 缓存
     */
    private static final Map<SerializableFunction<?, ?>, Field> CACHE = new ConcurrentHashMap<>(10);

    /**
     * 获取列名
     *
     * @param function 函数接口
     * @param <E>      对象
     * @param <R>      对象参数
     * @return String
     */
    public static <E, R> String getFieldName(SerializableFunction<E, R> function) {
        Field field = ReflectionUtil.getField(function);
        return field.getName();
    }

    /**
     * 获取列对象
     *
     * @param function 函数接口
     * @return Field
     */
    public static Field getField(SerializableFunction<?, ?> function) {
        return CACHE.computeIfAbsent(function, ReflectionUtil::findField);
    }

    /**
     * 查询列
     *
     * @param function 函数接口
     * @return Field
     */
    public static Field findField(SerializableFunction<?, ?> function) {
        Field field = null;
        String fieldName = null;
        try {
            // 第1步 获取SerializedLambda
            Method method = function.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(Boolean.TRUE);
            SerializedLambda serializedLambda = (SerializedLambda) method.invoke(function);
            // 第2步 implMethodName 即为Field对应的Getter方法名
            String implMethodName = serializedLambda.getImplMethodName();
            if (implMethodName.startsWith("get") && implMethodName.length() > 3) {
                fieldName = Introspector.decapitalize(implMethodName.substring(3));

            } else if (implMethodName.startsWith("is") && implMethodName.length() > 2) {
                fieldName = Introspector.decapitalize(implMethodName.substring(2));
            } else if (implMethodName.startsWith("lambda$")) {
                throw new IllegalArgumentException("SerializableFunction不能传递lambda表达式,只能使用方法引用");

            } else {
                throw new IllegalArgumentException(implMethodName + "不是Getter方法引用");
            }
            // 第3步 获取的Class是字符串，并且包名是“/”分割，需要替换成“.”，才能获取到对应的Class对象
            String declaredClass = serializedLambda.getImplClass().replace("/", ".");
            Class<?> aClass = Class.forName(declaredClass, false, ClassUtils.getDefaultClassLoader());

            // 第4步 Spring 中的反射工具类获取Class中定义的Field
            field = ReflectionUtils.findField(aClass, fieldName);
        } catch (Exception e) {
            logger.error("反射工具类异常：错误信息为：{}", e.getMessage(), e);
        }
        // 第5步 如果没有找到对应的字段应该抛出异常
        if (field != null) {
            return field;
        }
        throw new NoSuchFieldError(fieldName);
    }

    /**
     * 根据类获取集合名
     *
     * @param clazz 类
     * @return String 集合名
     */
    public static String getCollectionName(Class<?> clazz) {
        org.springframework.data.mongodb.core.mapping.Document document = clazz.getAnnotation(org.springframework.data.mongodb.core.mapping.Document.class);
        if (document != null) {
            if (StrUtil.isNotEmpty(document.value())) {
                return document.value();
            }
            if (StrUtil.isNotEmpty(document.collection())) {
                return document.collection();
            }
        }
        return StrUtil.lowerFirst(clazz.getSimpleName());
    }
}
