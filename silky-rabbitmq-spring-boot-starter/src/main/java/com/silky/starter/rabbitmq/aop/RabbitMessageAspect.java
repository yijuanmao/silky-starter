package com.silky.starter.rabbitmq.aop;

import cn.hutool.core.util.StrUtil;
import com.silky.starter.rabbitmq.annotation.RabbitMessage;
import com.silky.starter.rabbitmq.core.BaseMassageSend;
import com.silky.starter.rabbitmq.core.SendResult;
import com.silky.starter.rabbitmq.exception.RabbitMessageSendException;
import com.silky.starter.rabbitmq.template.RabbitSendTemplate;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RabbitMessage 切面
 *
 * @author zy
 * @date 2025-10-12 08:08
 **/
@Aspect
public class RabbitMessageAspect {

    private final Logger log = LoggerFactory.getLogger(RabbitMessageAspect.class);

    private final RabbitSendTemplate rabbitSendTemplate;

    public RabbitMessageAspect(RabbitSendTemplate rabbitSendTemplate) {
        this.rabbitSendTemplate = rabbitSendTemplate;
    }

    @Around("@annotation(rabbitMessage)")
    public Object aroundRabbitMessageWithRetry(ProceedingJoinPoint joinPoint,
                                               RabbitMessage rabbitMessage) throws Throwable {
        Object[] args = joinPoint.getArgs();
        BaseMassageSend message = findMessageParameter(args);
        if (message == null) {
            log.warn("No BaseMassageSend parameter found, proceeding without message sending");
            return joinPoint.proceed();
        }
        // 执行消息发送（支持重试）
        SendResult result = this.sendMessageWithRetry(rabbitMessage, message);
        // 处理发送结果
        if (!result.isSuccess() && rabbitMessage.throwOnFailure()) {
            throw new RabbitMessageSendException(
                    String.format("RabbitMQ message send failed after %d retries: %s",
                            rabbitMessage.retryCount(), result.getErrorMessage()), result
            );
        }
        // 继续执行原方法
        return joinPoint.proceed();
    }

    /**
     * 支持重试的消息发送
     *
     * @param rabbitMessage 注解
     * @param message       消息体
     */
    private SendResult sendMessageWithRetry(RabbitMessage rabbitMessage, BaseMassageSend message) {
        int retryCount = 0;
        int maxRetries = Math.max(0, rabbitMessage.retryCount());
        long retryInterval = Math.max(0, rabbitMessage.retryInterval());

        SendResult lastResult = null;

        while (retryCount <= maxRetries) {
            try {
                lastResult = doSendMessage(rabbitMessage, message);

                if (lastResult.isSuccess()) {
                    log.debug("Message sent successfully on attempt {}/{}",
                            retryCount + 1, maxRetries + 1);
                    return lastResult;
                }

                log.warn("Message send failed on attempt {}/{}: {}",
                        retryCount + 1, maxRetries + 1, lastResult.getErrorMessage());

            } catch (Exception e) {
                log.warn("Message send exception on attempt {}/{}: {}",
                        retryCount + 1, maxRetries + 1, e.getMessage());
                lastResult = SendResult.failure(e.getMessage(), 0);
            }

            // 判断是否继续重试
            if (retryCount < maxRetries) {
                retryCount++;
                if (retryInterval > 0) {
                    try {
                        Thread.sleep(retryInterval);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } else {
                break;
            }
        }
        return lastResult;
    }

    /**
     * 执行消息发送
     *
     * @param rabbitMessage 注解
     * @param message       消息体
     */
    private SendResult doSendMessage(RabbitMessage rabbitMessage, BaseMassageSend message) {
        if (rabbitMessage.delay() > 0) {
            return rabbitSendTemplate.sendDelay(
                    rabbitMessage.exchange(),
                    rabbitMessage.routingKey(),
                    message,
                    rabbitMessage.delay(),
                    getBusinessType(rabbitMessage, message),
                    getDescription(rabbitMessage, message)
            );
        } else {
            return rabbitSendTemplate.send(
                    rabbitMessage.exchange(),
                    rabbitMessage.routingKey(),
                    message,
                    getBusinessType(rabbitMessage, message),
                    getDescription(rabbitMessage, message),
                    rabbitMessage.sendMode()
            );
        }
    }

    /**
     * 查找方法参数中类型为 BaseMassageSend 的参数
     *
     * @param args 方法参数
     */
    private BaseMassageSend findMessageParameter(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof BaseMassageSend) {
                return (BaseMassageSend) arg;
            }
        }
        return null;
    }

    /**
     * 获取业务类型
     *
     * @param rabbitMessage 注解
     * @param message       消息体
     */
    private String getBusinessType(RabbitMessage rabbitMessage, BaseMassageSend message) {
        if (StrUtil.isNotBlank(rabbitMessage.businessType())) {
            return rabbitMessage.businessType();
        }
        return StrUtil.isBlank(message.getBusinessType()) ? "DEFAULT" : message.getBusinessType();
    }

    /**
     * 获取描述
     *
     * @param rabbitMessage 注解
     * @param message       消息体
     * @return 描述
     */
    private String getDescription(RabbitMessage rabbitMessage, BaseMassageSend message) {
        if (!rabbitMessage.description().isEmpty()) {
            return rabbitMessage.description();
        }
        return message.getDescription() != null ? message.getDescription() : "";
    }
}
