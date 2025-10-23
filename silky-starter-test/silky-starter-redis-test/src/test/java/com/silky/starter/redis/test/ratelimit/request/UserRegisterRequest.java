package com.silky.starter.redis.test.ratelimit.request;

import lombok.Data;
import lombok.ToString;

/**
 * 用户注册请求
 *
 * @author zy
 * @date 2025-10-23 16:18
 **/
@Data
@ToString
public class UserRegisterRequest {

    private String username;

    private String ip;
}
