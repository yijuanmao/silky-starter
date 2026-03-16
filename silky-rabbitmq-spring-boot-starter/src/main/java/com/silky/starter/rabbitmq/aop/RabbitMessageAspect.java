package com.silky.starter.rabbitmq.aop;

import com.silky.starter.rabbitmq.annotation.RabbitMessage;
import com.silky.starter.rabbitmq.annotation.RabbitPayload;
import com.silky.starter.rabbitmq.core.model.MassageSendParam;
import com.silky.starter.rabbitmq.core.model.SendResult;
import com.silky.starter.rabbitmq.enums.SendStatus;
import com.silky.starter.rabbitmq.exception.RabbitMessageSendException;
import com.silky.starter.rabbitmq.persistence.MessagePersistenceService;
import com.silky.starter.rabbitmq.template.SkRabbitMqTemplate;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Parameter;
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

    private final SkRabbitMqTemplate skRabbitMqTemplate;

    private final MessagePersistenceService persistenceService;


    public RabbitMessageAspect(SkRabbitMqTemplate skRabbitMqTemplate, MessagePersistenceService persistenceService) {
        this.skRabbitMqTemplate = skRabbitMqTemplate;
        this.persistenceService = persistenceService;
    }

    @Around("@annotation(rabbitMessage)")
    public Object aroundRabbitMessage(ProceedingJoinPoint joinPoint,
                                      RabbitMessage rabbitMessage) throws Throwable {
//        MassageSendParam message = findMessageParameter(args);
        // 1. 解析消息体
        Object payload = this.resolvePayload(joinPoint.getArgs(), joinPoint.getSignature());
        if (payload == null) {
            log.warn("No message payload found, proceeding without sending");
            return joinPoint.proceed();
        }

        MassageSendParam message = MassageSendParam.builder()
                .body(payload)
                .exchange(rabbitMessage.exchange())
                .routingKey(rabbitMessage.routingKey())
                .sendDelay(rabbitMessage.delay() > 0)
                .delayMillis(rabbitMessage.delay())
                .sendMode(rabbitMessage.sendMode())
                .description(rabbitMessage.description())
                .build();

        // 执行消息发送（支持重试）
        SendResult result = this.sendMessageWithRetry(joinPoint, rabbitMessage, message);

        String messageId = message.getMessageId();
        if (Objects.nonNull(persistenceService)) {
            if (result.isSuccess()) {
                persistenceService.updateMessageAfterSend(messageId, SendStatus.SENT, result.getCostTime(), "");
            } else {
                persistenceService.updateMessageAfterSend(messageId, SendStatus.FAILED, result.getCostTime(), result.getErrorMessage());
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
    private SendResult sendMessageWithRetry(ProceedingJoinPoint joinPoint, RabbitMessage rabbitMessage, MassageSendParam message) {
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
                lastResult = SendResult.failure(Objects.isNull(lastResult) ? "" : lastResult.getMessageId(), e.getMessage(), 0);
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
    private SendResult doSendMessage(RabbitMessage rabbitMessage, MassageSendParam message) {
        if (rabbitMessage.delay() > 0) {
            // 发送延迟消息
            return skRabbitMqTemplate.sendDelay(
                    rabbitMessage.exchange(),
                    rabbitMessage.routingKey(),
                    message.getBody(),
                    rabbitMessage.delay(),
                    rabbitMessage.businessType(),
                    rabbitMessage.description()
            );
        } else {
            // 发送普通消息
            return skRabbitMqTemplate.send(
                    rabbitMessage.exchange(),
                    rabbitMessage.routingKey(),
                    message.getBody(),
                    rabbitMessage.businessType(),
                    rabbitMessage.description(),
                    rabbitMessage.sendMode()
            );
        }
    }

    /**
     * 解析消息体：优先找 @RabbitPayload 参数，其次找 MassageSendParam 类型，最后可扩展返回值
     */
    private Object resolvePayload(Object[] args, Signature signature) {
        // 优先查找被 @RabbitPayload 标记的参数
        MethodSignature methodSignature = (MethodSignature) signature;
        Parameter[] parameters = methodSignature.getMethod().getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(RabbitPayload.class)) {
                return args[i];
            }
        }
        // 回退：查找类型为 MassageSendParam 的参数（兼容旧版）
        for (Object arg : args) {
            if (arg instanceof MassageSendParam) {
                return arg;
            }
        }
        return null;
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
