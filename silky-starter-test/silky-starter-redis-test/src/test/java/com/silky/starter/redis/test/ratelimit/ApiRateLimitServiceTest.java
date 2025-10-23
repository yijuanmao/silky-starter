package com.silky.starter.redis.test.ratelimit;

import com.silky.starter.redis.test.RedisApplicationTest;
import com.silky.starter.redis.test.ratelimit.entity.User;
import com.silky.starter.redis.test.ratelimit.request.UserRegisterRequest;
import com.silky.starter.redis.test.ratelimit.response.ApiResponse;
import com.silky.starter.redis.test.ratelimit.service.ApiRateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * API限流服务测试类
 *
 * @author zy
 * @date 2025-10-23 16:11
 **/
public class ApiRateLimitServiceTest extends RedisApplicationTest {
    @Autowired
    private ApiRateLimitService apiRateLimitService;

    /**
     * 用户注册测试
     */
    @Test
    public void userRegisterTest() {
        // 模拟用户注册请求
        ApiResponse<User> response = apiRateLimitService.userRegister(new UserRegisterRequest("测试", "127.0.0.1"));
        log.info("用户注册响应: {}", response);
    }
}
