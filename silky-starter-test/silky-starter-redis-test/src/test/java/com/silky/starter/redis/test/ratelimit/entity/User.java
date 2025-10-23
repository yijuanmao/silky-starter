package com.silky.starter.redis.test.ratelimit.entity;

import lombok.Data;
import lombok.ToString;

/**
 * 用户实体
 *
 * @author zy
 * @date 2025-10-23 16:14
 **/
@Data
@ToString
public class User {

    private Long id;

    private String name;

    private String password;
}
