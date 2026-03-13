package com.silky.starter.rabbitmq.template.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.silky.starter.rabbitmq.core.model.MassageSendParam;
import com.silky.starter.rabbitmq.core.model.SendResult;
import com.silky.starter.rabbitmq.enums.SendMode;
import com.silky.starter.rabbitmq.enums.SendStatus;
import com.silky.starter.rabbitmq.persistence.MessagePersistenceService;
import com.silky.starter.rabbitmq.properties.SilkyRabbitMQProperties;
import com.silky.starter.rabbitmq.serialization.RabbitMqMessageSerializer;
import com.silky.starter.rabbitmq.service.SendCallback;
import com.silky.starter.rabbitmq.template.SkRabbitMqTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * RabbitMQ消息发送模板实现
 *
 * @author zy
 * @date 2025-10-12 08:13
 **/
@Service
public class DefaultSkRabbitMqTemplate implements SkRabbitMqTemplate {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultSkRabbitMqTemplate.class);

    /**
     * 默认发送模式
     */
    private final SendMode defaultSendMode;

    /**
     * 同步发送超时时间 默认3秒
     */
    private final long syncTimeout;

    /**
     * 是否启用重试机制 默认启用
     */
    private final boolean enableRetry;

    /**
     * 最大重试次数 默认3次
     */
    private final int maxRetryCount;

    /**
     * 重试间隔时间（毫秒） 默认1000毫秒
     */
    private final long retryInterval;

    /**
     * 持久化开关
     */
    private final boolean enabledPersistence;

    private final RabbitTemplate rabbitTemplate;

    private final SilkyRabbitMQProperties properties;

    private final RabbitMqMessageSerializer messageSerializer;

    private final ExecutorService asyncExecutor;

    private final MessagePersistenceService persistenceService;

    public DefaultSkRabbitMqTemplate(RabbitTemplate rabbitTemplate, RabbitMqMessageSerializer messageSerializer,
                                     SilkyRabbitMQProperties silkyRabbitMQProperties, MessagePersistenceService persistenceService) {
        this.rabbitTemplate = rabbitTemplate;
        this.messageSerializer = messageSerializer;
        this.persistenceService = persistenceService;
        this.properties = silkyRabbitMQProperties;
        this.defaultSendMode = silkyRabbitMQProperties.getSend().getDefaultSendMode();
        this.syncTimeout = silkyRabbitMQProperties.getSend().getSyncTimeout();
        this.enableRetry = silkyRabbitMQProperties.getSend().isEnableRetry();
        this.maxRetryCount = silkyRabbitMQProperties.getSend().getMaxRetryCount();
        this.retryInterval = silkyRabbitMQProperties.getSend().getRetryInterval();
        this.asyncExecutor = Executors.newFixedThreadPool(silkyRabbitMQProperties.getSend().getAsyncThreadPoolSize());
        this.enabledPersistence = silkyRabbitMQProperties.getPersistence().isEnabled();

        log.info("DefaultRabbitSenderTemplate initialized with persistence: {}", persistenceService != null ? "enabled" : "disabled");
    }

    /**
     * 发送消息
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息参数
     */
    @Override
    public SendResult send(String exchange, String routingKey, Object message) {
        return this.send(exchange, routingKey, message, "", defaultSendMode);
    }

    /**
     * 发送消息
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息参数
     * @param messageId  消息id
     */
    @Override
    public SendResult send(String exchange, String routingKey, Object message, String messageId) {
        return this.send(exchange, routingKey, message, messageId, defaultSendMode);
    }

    /**
     * 发送消息（指定发送模式）
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息体
     * @param sendMode   发送模式
     */
    @Override
    public SendResult send(String exchange, String routingKey, Object message, String messageId, SendMode sendMode) {
        return this.send(exchange, routingKey, message, messageId, "DEFAULT", StrUtil.EMPTY, sendMode);
    }

    /**
     * 发送消息
     *
     * @param exchange     交换机
     * @param routingKey   路由键
     * @param message      消息体
     * @param messageId    消息id
     * @param businessType 业务类型
     * @param description  描述
     * @param sendMode     发送模式
     */
    @Override
    public SendResult send(String exchange, String routingKey, Object message, String messageId, String businessType, String description, SendMode sendMode) {
        MassageSendParam param = new MassageSendParam();
        param.setMsg(message);
        param.setMessageId(messageId);
        param.setExchange(exchange);
        param.setRoutingKey(routingKey);
        param.setSendMode(sendMode);
        param.setBusinessType(businessType);
        param.setDescription(description);
        param.setSendDelay(false);
        return this.send(param);
    }

    /**
     * 发送延迟消息
     *
     * @param exchange     交换机
     * @param routingKey   路由键
     * @param message      消息体
     * @param messageId    消息id
     * @param delayMillis  延迟时间
     * @param businessType 业务类型
     * @param description  描述
     */
    @Override
    public SendResult sendDelay(String exchange, String routingKey, Object message, String messageId, long delayMillis, String businessType, String description) {
        MassageSendParam param = new MassageSendParam();
        param.setMsg(message);
        param.setMessageId(messageId);
        param.setExchange(exchange);
        param.setRoutingKey(routingKey);
        param.setSendMode(SendMode.SYNC);
        param.setBusinessType(businessType);
        param.setDescription(description);
        param.setSendDelay(true);
        param.setDelayMillis(delayMillis);
        return this.send(param);
    }

    /**
     * 异步发送消息
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息体
     */
    @Override
    public void sendAsync(String exchange, String routingKey, Object message) {
        this.sendAsync(exchange, routingKey, message, null);
    }

    /**
     * 异步发送消息（带回调）
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息体
     * @param callback   回调
     */
    @Override
    public void sendAsync(String exchange, String routingKey, Object message, SendCallback callback) {
        CompletableFuture.runAsync(() -> {
            SendResult result = send(exchange, routingKey, message, "", SendMode.ASYNC);
            if (callback != null) {
                if (result.isSuccess()) {
                    callback.onSuccess(result);
                } else {
                    callback.onFailure(result);
                }
            }
        }, asyncExecutor);
    }


    /**
     * 发送消息
     *
     * @param param 消息体
     */
    @Override
    public SendResult send(MassageSendParam param) {
        //检查必要的依赖
        this.checkDependencies(param);

        String messageId = this.generateMessageId(param.getMessageId());
        String exchange = param.getExchange();
        String routingKey = param.getRoutingKey();
        SendMode sendMode = param.getSendMode();
        String businessType = param.getBusinessType();
        String description = param.getDescription();

        param.setSendTime(Objects.isNull(param.getSendTime()) ? LocalDateTime.now() : param.getSendTime());
        param.setMessageId(messageId);
        // 如果Silky发送功能被禁用，直接使用原生RabbitTemplate
        if (!properties.getSend().isEnabled()) {
            log.debug("Silky send is disabled, using native RabbitTemplate");
            return doNativeSend(exchange, routingKey, param.getMessageId(), param.getMessageId());
        }

        SendMode actualMode = sendMode == SendMode.AUTO ? this.determineSendMode() : sendMode;

        long startTime = System.currentTimeMillis();

        if (StrUtil.isNotBlank(businessType)) {
            param.setBusinessType(businessType);
        }
        if (StrUtil.isNotBlank(description)) {
            param.setDescription(description);
        }

        if (isPersistenceEnabled()) {
            persistenceService.saveMessageBeforeSend(param, exchange, routingKey, actualMode, businessType, description);
        }

        try {
            Message rabbitMessage = this.buildMessage(param, messageId);

            SendResult result;

            long costTime = System.currentTimeMillis() - startTime;

            if (param.isSendDelay()) {
                // 设置延迟属性
                rabbitMessage.getMessageProperties().setHeader("x-delay", param.getDelayMillis());
                // 延迟消息使用同步发送
                CorrelationData correlationData = new CorrelationData(messageId);
                rabbitTemplate.convertAndSend(exchange, routingKey, rabbitMessage, correlationData);
                result = SendResult.success(messageId, costTime);
            } else {
                if (actualMode == SendMode.SYNC) {
                    result = doSyncSend(exchange, routingKey, rabbitMessage, messageId, startTime);
                } else {
                    result = doAsyncSend(exchange, routingKey, rabbitMessage, messageId, startTime).get();
                }
                if (isPersistenceEnabled()) {
                    if (result.isSuccess()) {
                        persistenceService.updateMessageAfterSend(messageId, SendStatus.SENT, result.getCostTime(), "");
                    } else {
                        persistenceService.updateMessageAfterSend(messageId, SendStatus.FAILED, result.getCostTime(), result.getErrorMessage());
                    }
                }
            }
            return result;
        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            log.error("Send message failed, exchange: {}, routingKey: {}, messageId: {}", exchange, routingKey, messageId, e);
            // 3. 持久化发送失败记录
            if (isPersistenceEnabled()) {
                persistenceService.updateMessageAfterSend(messageId, SendStatus.FAILED, costTime, e.getMessage());
            }
            return SendResult.failure(e.getMessage(), costTime);
        }
    }

    /**
     * 构建消息对象
     *
     * @param message   消息体
     * @param messageId 消息ID
     * @return 消息对象
     */
    private Message buildMessage(Object message, String messageId) {
        byte[] body = messageSerializer.serialize(message);
        return MessageBuilder.withBody(body)
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setMessageId(messageId)
                .setTimestamp(new Date())
                .build();
    }

    /**
     * 带超时控制的同步发送
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息体
     * @param messageId  消息ID
     * @param startTime  发送开始时间
     * @return 发送结果
     */
    private SendResult doSyncSend(String exchange, String routingKey, Message message,
                                  String messageId, long startTime) {
        if (!isUseTimeout()) {
            // 不使用超时控制的简单发送
            return doSimpleSyncSend(exchange, routingKey, message, messageId, startTime);
        }
        // 使用超时控制的发送
        return doTimeoutSyncSend(exchange, routingKey, message, messageId, startTime);
    }

    /**
     * 简单的同步发送（无超时控制）
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息体
     * @param messageId  消息ID
     * @param startTime  发送开始时间
     * @return 发送结果
     */
    private SendResult doSimpleSyncSend(String exchange, String routingKey, Message message, String messageId, long startTime) {
        try {
            CorrelationData correlationData = new CorrelationData(messageId);
            // 带重试机制的同步发送
            if (enableRetry) {
                return doSyncSendWithRetry(exchange, routingKey, message, correlationData, startTime);
            } else {
                rabbitTemplate.convertAndSend(exchange, routingKey, message, correlationData);
                long costTime = System.currentTimeMillis() - startTime;
                return SendResult.success(messageId, costTime);
            }
        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            return SendResult.failure(e.getMessage(), costTime);
        }
    }

    /**
     * 带超时控制的同步发送
     */
    private SendResult doTimeoutSyncSend(String exchange, String routingKey, Message message,
                                         String messageId, long startTime) {
        long timeout = getSyncTimeout();
        CompletableFuture<SendResult> future = CompletableFuture.supplyAsync(() -> {
            try {
                CorrelationData correlationData = new CorrelationData(messageId);
                if (enableRetry) {
                    return doSyncSendWithRetry(exchange, routingKey, message, correlationData, startTime);
                } else {
                    rabbitTemplate.convertAndSend(exchange, routingKey, message, correlationData);
                    long costTime = System.currentTimeMillis() - startTime;
                    return SendResult.success(messageId, costTime);
                }
            } catch (Exception e) {
                long costTime = System.currentTimeMillis() - startTime;
                return SendResult.failure(e.getMessage(), costTime);
            }
        }, asyncExecutor);
        try {
            // 等待发送完成，最多等待 timeout 毫秒
            return future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            // 取消任务
            future.cancel(true);
            long costTime = System.currentTimeMillis() - startTime;
            String errorMsg = String.format("Send timeout after %dms", timeout);
            log.warn("Message send timeout: exchange={}, routingKey={}, messageId={}", exchange, routingKey, messageId);
            return SendResult.failure(errorMsg, costTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            long costTime = System.currentTimeMillis() - startTime;
            return SendResult.failure("Send interrupted: " + e.getMessage(), costTime);
        } catch (ExecutionException e) {
            long costTime = System.currentTimeMillis() - startTime;
            return SendResult.failure("Send execution failed: " + e.getMessage(), costTime);
        }
    }


    /**
     * 带重试机制的同步发送
     *
     * @param exchange        交换机
     * @param routingKey      路由键
     * @param message         消息体
     * @param correlationData 唯一标识
     * @param startTime       发送开始时间
     * @return 发送结果
     */
    private SendResult doSyncSendWithRetry(String exchange, String routingKey, Message message,
                                           CorrelationData correlationData, long startTime) {
        int retryCount = 0;
        while (retryCount <= maxRetryCount) {
            try {
                rabbitTemplate.convertAndSend(exchange, routingKey, message, correlationData);
                long costTime = System.currentTimeMillis() - startTime;
                return SendResult.success(correlationData.getId(), costTime);

            } catch (Exception e) {
                retryCount++;
                if (retryCount > maxRetryCount) {
                    long costTime = System.currentTimeMillis() - startTime;
                    return SendResult.failure("Send failed after " + maxRetryCount + " retries: " + e.getMessage(), costTime);
                }

                log.warn("Send message failed, retry {}/{}, exchange: {}, routingKey: {}",
                        retryCount, maxRetryCount, exchange, routingKey, e);

                try {
                    Thread.sleep(retryInterval);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    long costTime = System.currentTimeMillis() - startTime;
                    return SendResult.failure("Send interrupted: " + ie.getMessage(), costTime);
                }
            }
        }

        long costTime = System.currentTimeMillis() - startTime;
        return SendResult.failure("Send failed after all retries", costTime);
    }

    /**
     * 异步发送消息
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息体
     * @param messageId  消息ID
     * @return 发送结果
     */
    private CompletableFuture<SendResult> doAsyncSend(String exchange, String routingKey,
                                                      Message message, String messageId, long startTime) {
        return CompletableFuture.supplyAsync(() -> {
            CorrelationData correlationData = new CorrelationData(messageId);
            try {
                rabbitTemplate.convertAndSend(exchange, routingKey, message, correlationData);
                long costTime = System.currentTimeMillis() - startTime;
                return SendResult.success(messageId, costTime);
            } catch (Exception e) {
                long costTime = System.currentTimeMillis() - startTime;
                return SendResult.failure(e.getMessage(), costTime);
            }
        }, asyncExecutor);
    }


    /**
     * 使用原生RabbitTemplate发送（当Silky功能禁用时）
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息体
     */
    private <T> SendResult doNativeSend(String exchange, String routingKey, Object message, String messageId) {
        long startTime = System.currentTimeMillis();
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            long costTime = System.currentTimeMillis() - startTime;
            return SendResult.success(messageId, costTime);
        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            return SendResult.failure(e.getMessage(), costTime);
        }
    }

    private boolean isUseTimeout() {
        return this.properties != null && this.properties.getSend().isUseTimeout();
    }

    private long getSyncTimeout() {
        return this.syncTimeout;
    }


    /**
     * 检查必要的依赖
     */
    private void checkDependencies(MassageSendParam param) {
        if (rabbitTemplate == null) {
            throw new IllegalStateException("RabbitTemplate is not available.");
        }
        if (messageSerializer == null) {
            throw new IllegalStateException("MessageSerializer is not available.");
        }
        if (Objects.isNull(param)) {
            throw new IllegalStateException("MassageSendParam is null");
        }
        if (Objects.isNull(param.getMsg()) || (param.getMsg() instanceof String && StrUtil.isBlank((String) param.getMsg()))) {
            throw new IllegalStateException("Message body is null");
        }
        if (StrUtil.isBlank(param.getExchange())) {
            throw new IllegalStateException("Exchange is null");
        }
        if (StrUtil.isBlank(param.getRoutingKey())) {
            throw new IllegalStateException("Routing key is null");
        }
        if (param.isSendDelay() && (Objects.isNull(param.getDelayMillis()) || param.getDelayMillis() <= 0)) {
            throw new IllegalStateException("Delay millis is null or less than or equal to 0");
        }
    }

    /**
     * 确定发送模式
     *
     * @return 发送模式
     */
    private SendMode determineSendMode() {
        // 可以根据消息大小、业务类型等逻辑决定发送模式.这里暂时简单返回配置的默认发送模式
        return defaultSendMode;
    }

    /**
     * 检查是否启用消息持久化
     *
     * @return 是否启用消息持久化
     */
    private boolean isPersistenceEnabled() {
        return persistenceService != null && enabledPersistence;
    }

    /**
     * 生成消息ID
     *
     * @param messageId 消息ID
     */
    private String generateMessageId(String messageId) {
        return StrUtil.isBlank(messageId) ? IdUtil.simpleUUID() : messageId;
    }
}
