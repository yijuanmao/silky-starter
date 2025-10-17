package com.silky.starter.rabbitmq.config;

import com.silky.starter.rabbitmq.properties.SilkyRabbitMQProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Silky配置验证器
 *
 * @author zy
 * @date 2025-10-15 15:43
 **/
@Component
public class SilkyRabbitConfigValidator {

    private static final Logger logger = LoggerFactory.getLogger(SilkyRabbitConfigValidator.class);

    private final SilkyRabbitMQProperties properties;

    public SilkyRabbitConfigValidator(SilkyRabbitMQProperties properties) {
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateConfiguration() {
        if (!properties.isEnabled()) {
            logger.info("Silky RabbitMQ is disabled");
            return;
        }

        logger.info("=== Silky RabbitMQ Configuration ===");
        logger.info("Silky Enabled: {}", properties.isEnabled());
        logger.info("Send Enabled: {}", properties.getSend().isEnabled());
        logger.info("Default Send Mode: {}", properties.getSend().getDefaultSendMode());
        logger.info("Sync Timeout: {}ms", properties.getSend().getSyncTimeout());
        logger.info("Async Thread Pool: {}", properties.getSend().getAsyncThreadPoolSize());
        logger.info("Retry Enabled: {} (max: {}, interval: {}ms)",
                properties.getSend().isEnableRetry(),
                properties.getSend().getMaxRetryCount(),
                properties.getSend().getRetryInterval());
        logger.info("Persistence Enabled: {} ", properties.getPersistence().isEnabled());
        logger.info("====================================");
    }
}
