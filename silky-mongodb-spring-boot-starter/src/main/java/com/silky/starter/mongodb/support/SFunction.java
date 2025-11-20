package com.silky.starter.mongodb.support;

import cn.hutool.core.lang.func.Func1;

import java.io.Serializable;
import java.util.function.Function;

/**
 * 函数式接口
 *
 * @param <E> 对象
 * @param <R> 对象参数
 * @author zy
 * @date 2025-09-04 10:40
 */
@FunctionalInterface
public interface SFunction<E, R> extends Func1<E, R>, Serializable {

}
