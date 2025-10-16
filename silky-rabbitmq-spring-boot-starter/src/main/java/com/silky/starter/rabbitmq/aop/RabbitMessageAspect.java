package com.silky.starter.rabbitmq.aop;

import cn.hutool.core.util.StrUtil;
import com.silky.starter.rabbitmq.annotation.RabbitMessage;
import com.silky.starter.rabbitmq.core.model.BaseMassageSend;
import com.silky.starter.rabbitmq.core.model.SendResult;
import com.silky.starter.rabbitmq.enums.MessageStatus;
import com.silky.starter.rabbitmq.exception.RabbitMessageSendException;
import com.silky.starter.rabbitmq.persistence.MessagePersistenceService;
import com.silky.starter.rabbitmq.template.RabbitSendTemplate;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

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

    private final MessagePersistenceService persistenceService;


    public RabbitMessageAspect(RabbitSendTemplate rabbitSendTemplate, MessagePersistenceService persistenceService) {
        this.rabbitSendTemplate = rabbitSendTemplate;
        this.persistenceService = persistenceService;
    }

    @Around("@annotation(rabbitMessage)")
    public Object aroundRabbitMessage(ProceedingJoinPoint joinPoint,
                                      RabbitMessage rabbitMessage) throws Throwable {
        Object[] args = joinPoint.getArgs();
        BaseMassageSend message = findMessageParameter(args);
        if (message == null) {
            log.warn("No BaseMassageSend parameter found, proceeding without message sending");
            return joinPoint.proceed();
        }
        // 执行消息发送（支持重试）
        SendResult result = this.sendMessageWithRetry(joinPoint, rabbitMessage, message);

        String messageId = message.getMessageId();
        if (Objects.nonNull(persistenceService)) {
            if (result.isSuccess()) {
                persistenceService.updateMessageAfterSend(messageId, MessageStatus.SENT, result.getCostTime(), "");
            } else {
                persistenceService.updateMessageAfterSend(messageId, MessageStatus.FAILED, result.getCostTime(), result.getErrorMessage());
            }
        }
        // 处理发送结果
        if (!result.isSuccess()) {
            //失败处理
            this.handleSendFailure(joinPoint, result, rabbitMessage);
            if (rabbitMessage.throwOnFailure()) {
                throw new RabbitMessageSendException(String.format("RabbitMQ message send failed after %d retries: %s",
                        rabbitMessage.retryCount(), result.getErrorMessage()), result);
            }
        }
        // 继续执行原方法
        return joinPoint.proceed();
    }

    /**
     * 支持重试的消息发送
     *
     * @param joinPoint     切点
     * @param rabbitMessage 注解
     * @param message       消息体
     */
    private SendResult sendMessageWithRetry(ProceedingJoinPoint joinPoint, RabbitMessage rabbitMessage, BaseMassageSend message) {
        int retryCount = 0;
        int maxRetries = Math.max(0, rabbitMessage.retryCount());
        long retryInterval = Math.max(0, rabbitMessage.retryInterval());

        SendResult lastResult = null;

        while (retryCount <= maxRetries) {
            try {
                lastResult = this.doSendMessage(rabbitMessage, message);
                if (lastResult.isSuccess()) {
                    log.debug("Message sent successfully on attempt {}/{}", retryCount + 1, maxRetries + 1);
                    return lastResult;
                }
                log.warn("Message send failed on attempt {}/{}: {}", retryCount + 1, maxRetries + 1, lastResult.getErrorMessage());
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
            // 发送延迟消息
            return rabbitSendTemplate.sendDelay(
                    rabbitMessage.exchange(),
                    rabbitMessage.routingKey(),
                    message,
                    rabbitMessage.delay(),
                    getBusinessType(rabbitMessage, message),
                    getDescription(rabbitMessage, message)
            );
        } else {
            // 发送普通消息
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

    /**
     * 处理发送失败情况
     *
     * @param joinPoint     切点
     * @param result        发送结果
     * @param rabbitMessage 注解
     */
    private void handleSendFailure(ProceedingJoinPoint joinPoint, SendResult result, RabbitMessage rabbitMessage) {
        String errorMsg = String.format("RabbitMQ message send failed: %s, exchange: %s, routingKey: %s",
                result.getErrorMessage(), rabbitMessage.exchange(), rabbitMessage.routingKey());

        log.error(errorMsg);
        // 根据注解配置决定是否抛出异常
        if (rabbitMessage.throwOnFailure()) {
            throw new RabbitMessageSendException(errorMsg, result);
        }
        // 如果配置了失败回调，可以在这里调用
        if (rabbitMessage.enableFailureCallback()) {
            this.handleFailureCallback(joinPoint, result, rabbitMessage);
        }
    }

    /**
     * 处理失败回调
     *
     * @param joinPoint     切点
     * @param result        发送结果
     * @param rabbitMessage 注解
     */
    private void handleFailureCallback(ProceedingJoinPoint joinPoint, SendResult result, RabbitMessage rabbitMessage) {
        log.warn("Message send failure callback triggered: {}", result.getErrorMessage());

        // 如果启用了持久化，可以通过持久化服务记录详细失败信息
        if (persistenceService != null) {
            // 这里可以添加额外的失败处理逻辑，比如发送告警等,后期扩展
        }
    }
}
