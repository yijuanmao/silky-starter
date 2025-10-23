package com.silky.starter.redis.test.ratelimit.service;

import com.silky.starter.redis.ratelimiter.annotation.RateLimit;
import com.silky.starter.redis.ratelimiter.enums.RateLimitAlgorithm;
import com.silky.starter.redis.test.ratelimit.entity.User;
import com.silky.starter.redis.test.ratelimit.request.PaymentRequest;
import com.silky.starter.redis.test.ratelimit.request.UserRegisterRequest;
import com.silky.starter.redis.test.ratelimit.response.ApiResponse;
import com.silky.starter.redis.test.redission.result.PaymentResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * API限流服务
 *
 * @author zy
 * @date 2025-10-23 16:11
 **/
@Slf4j
@Service
public class ApiRateLimitService {

    @Autowired
    private PaymentService paymentService;

    /**
     * 用户注册接口, API限流 - 令牌桶算法（平滑限流）
     *
     * @param request UserRegisterRequest
     * @return ApiResponse<User>
     */
    @RateLimit(
            key = "'api:user:register:' + #request.ip",  // 支持SpEL
            algorithm = RateLimitAlgorithm.TOKEN_BUCKET,
            capacity = 100,     // 桶容量
            refillRate = 10,    // 每秒10个令牌
            timeUnit = TimeUnit.SECONDS,
            fallbackMethod = "registerFallback" //限流后的降级方法名，必须和目标方法在同一个类中
    )
    public ApiResponse<User> userRegister(UserRegisterRequest request) {
        log.info("用户注册: {}", request.getUsername());
        User user = this.register(request);
        return ApiResponse.success(user);
    }


    /**
     * 注册限流降级处理
     *
     * @param request UserRegisterRequest
     * @return ApiResponse<User>
     */
    public ApiResponse<User> registerFallback(UserRegisterRequest request) {
        log.warn("用户注册限流，IP: {}", request.getIp());
        return ApiResponse.fail(429, "注册请求过于频繁，请稍后再试");
    }

    /**
     * 发送短信验证码,场景2：短信发送 - 固定窗口算法（简单计数）
     *
     * @param phone
     */
    @RateLimit(
            key = "'sms:send:' + #phone",
            algorithm = RateLimitAlgorithm.FIXED_WINDOW,
            windowSize = 60,     // 60秒窗口
            maxRequests = 5,     // 最多5条
            fallbackMethod = "sendSmsFallback" //这里会调用下面的sendSmsFallback方法
    )
    public void sendVerificationCode(String phone) {
        log.info("发送短信验证码: {}", phone);
        // 发送短信验证码逻辑
//        smsService.sendCode(phone);
    }

    /**
     * 短信发送限流降级处理
     *
     * @param phone
     */
    public void sendSmsFallback(String phone) {
        log.warn("短信发送限流，手机号: {}", phone);
        // 可以记录日志或进行其他处理
    }


    /**
     * 场景3：支付接口 - 滑动窗口算法（精准控制）
     *
     * @param userId  用户ID
     * @param request PaymentRequest
     * @return
     */
    @RateLimit(
            key = "'payment:create:' + #userId",
            algorithm = RateLimitAlgorithm.SLIDING_WINDOW,
            windowSize = 60,     // 60秒窗口
            maxRequests = 30,    // 最多30次
            block = true,        // 阻塞等待
            timeout = 5,         // 等待5秒
            fallbackMethod = "paymentFallback"
    )
    @Transactional
    public PaymentResult createPayment(Long userId, PaymentRequest request) {
        log.info("创建支付订单，用户ID: {}", userId);
        return paymentService.createPayment(userId, request);
    }

    public PaymentResult paymentFallback(Long userId, PaymentRequest request) {
        log.warn("支付接口限流，用户ID: {}", userId);
        throw new RuntimeException("支付操作过于频繁，请稍后再试");
    }

    /**
     * 场景4：重量级操作 - 按权重限流
     *
     * @param batchId
     * @param dataList
     */
    @RateLimit(
            key = "'batch:process:' + #batchId",
            algorithm = RateLimitAlgorithm.TOKEN_BUCKET,
            capacity = 1000,
            refillRate = 100,
            block = true,
            timeout = 30,        // 等待30秒
            fallbackMethod = "batchProcessFallback"
    )
    public void processLargeBatch(String batchId, List<Object> dataList) {
        log.info("处理大批量数据，批次ID: {}, 数据量: {}", batchId, dataList.size());
        // 根据数据量动态计算资源消耗

    }

    public void batchProcessFallback(String batchId, List<Object> dataList) {
        log.warn("批量处理限流，转为异步处理，批次ID: {}", batchId);
        // 可以将任务放入消息队列，异步处理
    }

    /**
     * 用户注册
     *
     * @return User
     */
    private User register(UserRegisterRequest request) {
        log.info("执行用户注册逻辑");
        User user = new User();
        user.setId(1L);
        user.setName("张三");
        return user;
    }
}
