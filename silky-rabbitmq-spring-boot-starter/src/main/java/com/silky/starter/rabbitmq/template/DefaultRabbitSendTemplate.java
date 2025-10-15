package com.silky.starter.rabbitmq.template;

import cn.hutool.core.util.StrUtil;
import com.silky.starter.rabbitmq.core.BaseMassageSend;
import com.silky.starter.rabbitmq.core.SendResult;
import com.silky.starter.rabbitmq.enums.MessageStatus;
import com.silky.starter.rabbitmq.enums.SendMode;
import com.silky.starter.rabbitmq.persistence.MessagePersistenceService;
import com.silky.starter.rabbitmq.properties.SilkyRabbitMQProperties;
import com.silky.starter.rabbitmq.serialization.RabbitMqMessageSerializer;
import com.silky.starter.rabbitmq.service.SendCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.*;

/**
 * RabbitMQ消息发送模板实现
 *
 * @author zy
 * @date 2025-10-12 08:13
 **/
@Slf4j
@Service
public class DefaultRabbitSendTemplate implements RabbitSendTemplate {

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

    public DefaultRabbitSendTemplate(RabbitTemplate rabbitTemplate, RabbitMqMessageSerializer messageSerializer,
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
     * @param message    消息体
     */
    @Override
    public <T extends BaseMassageSend> SendResult send(String exchange, String routingKey, T message) {
        return this.send(exchange, routingKey, message, defaultSendMode);
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
    public <T extends BaseMassageSend> SendResult send(String exchange, String routingKey, T message, SendMode sendMode) {
        String businessType = StrUtil.isBlank(message.getBusinessType()) ? "DEFAULT" : message.getBusinessType();
        String description = StrUtil.isBlank(message.getDescription()) ? StrUtil.EMPTY : message.getDescription();
        return this.send(exchange, routingKey, message, businessType, description, sendMode);
    }

    /**
     * 发送消息（带业务类型）
     *
     * @param exchange     交换机
     * @param routingKey   路由键
     * @param message      消息体
     * @param businessType 业务类型
     * @param description  描述
     */
    @Override
    public <T extends BaseMassageSend> SendResult send(String exchange, String routingKey, T message, String businessType, String description) {
        return this.send(exchange, routingKey, message, businessType, description, defaultSendMode);
    }

    /**
     * 发送消息
     *
     * @param exchange     交换机
     * @param routingKey   路由键
     * @param message      消息体
     * @param businessType 业务类型
     * @param description  描述
     * @param sendMode     发送模式
     */
    @Override
    public <T extends BaseMassageSend> SendResult send(String exchange, String routingKey, T message, String businessType, String description, SendMode sendMode) {
        //检查必要的依赖
        this.checkDependencies();
        // 如果Silky发送功能被禁用，直接使用原生RabbitTemplate
        if (!properties.getSend().isEnabled()) {
            log.debug("Silky send is disabled, using native RabbitTemplate");
            return doNativeSend(exchange, routingKey, message);
        }

        if (StrUtil.isNotBlank(businessType)) {
            message.setBusinessType(businessType);
        }
        if (StrUtil.isNotBlank(description)) {
            message.setDescription(description);
        }
        SendMode actualMode = sendMode == SendMode.AUTO ? this.determineSendMode() : sendMode;
        String messageId = message.getMessageId();
        long startTime = System.currentTimeMillis();

        if (isPersistenceEnabled()) {
            persistenceService.saveMessageBeforeSend(message, exchange, routingKey,
                    actualMode.name(), businessType, description);
        }

        try {
            Message rabbitMessage = this.buildMessage(message, messageId);
            SendResult result;
            if (actualMode == SendMode.SYNC) {
                result = doSyncSend(exchange, routingKey, rabbitMessage, messageId, startTime);
            } else {
                result = doAsyncSend(exchange, routingKey, rabbitMessage, messageId, startTime).get();
            }

            if (isPersistenceEnabled()) {
                if (result.isSuccess()) {
                    persistenceService.updateMessageAfterSend(messageId, MessageStatus.SENT, result.getCostTime(), "");
                } else {
                    persistenceService.updateMessageAfterSend(messageId, MessageStatus.FAILED, result.getCostTime(), result.getErrorMessage());
                }
            }
            return result;
        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            log.error("Send message failed, exchange: {}, routingKey: {}, messageId: {}", exchange, routingKey, messageId, e);
            // 3. 持久化发送失败记录
            if (isPersistenceEnabled()) {
                persistenceService.updateMessageAfterSend(messageId, MessageStatus.FAILED, costTime, e.getMessage());
            }
            return SendResult.failure(e.getMessage(), costTime);
        }
    }

    /**
     * 发送延迟消息
     *
     * @param exchange     交换机
     * @param routingKey   路由键
     * @param message      消息体
     * @param delayMillis  延迟时间
     * @param businessType 业务类型
     * @param description  描述
     */
    @Override
    public <T extends BaseMassageSend> SendResult sendDelay(String exchange, String routingKey, T message, long delayMillis, String businessType, String description) {
        //检查必要的依赖
        this.checkDependencies();

        if (StrUtil.isNotBlank(businessType)) {
            message.setBusinessType(businessType);
        }
        if (StrUtil.isNotBlank(description)) {
            message.setDescription(description);
        }
        String messageId = message.getMessageId();
        long startTime = System.currentTimeMillis();

        if (isPersistenceEnabled()) {
            persistenceService.saveMessageBeforeSend(message, exchange, routingKey, SendMode.SYNC.name(), businessType, description);
        }

        try {
            Message rabbitMessage = buildMessage(message, messageId);
            // 设置延迟属性
            rabbitMessage.getMessageProperties().setHeader("x-delay", delayMillis);

            // 延迟消息使用同步发送
            CorrelationData correlationData = new CorrelationData(messageId);
            rabbitTemplate.convertAndSend(exchange, routingKey, rabbitMessage, correlationData);
            long costTime = System.currentTimeMillis() - startTime;

            if (isPersistenceEnabled()) {
                persistenceService.updateMessageAfterSend(messageId, MessageStatus.SENT, costTime, null);
            }
            return SendResult.success(messageId, costTime);
        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            log.error("Send delay message failed, exchange: {}, routingKey: {}, messageId: {}",
                    exchange, routingKey, messageId, e);

            // 3. 持久化发送失败记录
            if (isPersistenceEnabled()) {
                persistenceService.updateMessageAfterSend(messageId, MessageStatus.FAILED, costTime, e.getMessage());
            }

            return SendResult.failure(e.getMessage(), costTime);
        }
    }

    /**
     * 异步发送消息
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息体
     */
    @Override
    public <T extends BaseMassageSend> void sendAsync(String exchange, String routingKey, T message) {
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
    public <T extends BaseMassageSend> void sendAsync(String exchange, String routingKey, T message, SendCallback callback) {
        CompletableFuture.runAsync(() -> {
            SendResult result = send(exchange, routingKey, message, SendMode.ASYNC);
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
     * 构建消息对象
     *
     * @param message   消息体
     * @param messageId 消息ID
     * @param <T>       消息类型
     * @return 消息对象
     */
    private <T extends BaseMassageSend> Message buildMessage(T message, String messageId) {
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
    private <T extends BaseMassageSend> SendResult doNativeSend(String exchange, String routingKey, T message) {
        long startTime = System.currentTimeMillis();
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            long costTime = System.currentTimeMillis() - startTime;
            return SendResult.success(message.getMessageId(), costTime);
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
    private void checkDependencies() {
        if (rabbitTemplate == null) {
            throw new IllegalStateException("RabbitTemplate is not available.");
        }
        if (messageSerializer == null) {
            throw new IllegalStateException("MessageSerializer is not available.");
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

}
