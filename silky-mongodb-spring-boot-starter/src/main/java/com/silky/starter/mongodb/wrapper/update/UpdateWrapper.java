package com.silky.starter.mongodb.wrapper.update;

import com.silky.starter.mongodb.support.SerializableFunction;
import com.silky.starter.mongodb.utils.ReflectionUtil;
import org.springframework.data.mongodb.core.query.Update;

/**
 * 修改构造器
 *
 * @author zy
 * @date 2022-11-22 11:42
 **/
public class UpdateWrapper {

    private final Update update = new Update();

    public UpdateWrapper() {}

    /**
     * 有参构造
     *
     * @param key   对象
     * @param value value
     * @param <E>   对象
     * @param <R>   列
     */
    public <E, R> UpdateWrapper(SerializableFunction<E, R> key, Object value) {
        update.set(ReflectionUtil.getFieldName(key), value);
    }

    /**
     * set
     *
     * @param key   对象
     * @param value value
     * @param <E>   对象
     * @param <R>   列
     * @return
     */
    public <E, R> UpdateWrapper set(SerializableFunction<E, R> key, Object value) {
        update.set(ReflectionUtil.getFieldName(key), value);
        return this;
    }

    /**
     * 进行增减的操作
     *
     * @param key   对象
     * @param count value
     * @param <E>   对象
     * @param <R>   列
     * @return
     */
    public <E, R> UpdateWrapper inc(SerializableFunction<E, R> key, Number count) {
        update.inc(ReflectionUtil.getFieldName(key), count);
        return this;
    }

    public Update toUpdate() {
        return update;
    }
}
