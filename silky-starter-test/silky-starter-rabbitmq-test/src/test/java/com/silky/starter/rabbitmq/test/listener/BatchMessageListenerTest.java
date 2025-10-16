package com.silky.starter.rabbitmq.test.listener;

import com.rabbitmq.client.Channel;
import com.silky.starter.rabbitmq.listener.AbstractRabbitMQListener;
import com.silky.starter.rabbitmq.listener.util.ManualAckHelper;
import com.silky.starter.rabbitmq.test.config.RabbitMqBindConfig;
import com.silky.starter.rabbitmq.test.entity.BatchTradeOrder;
import org.springframework.amqp.core.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量消息监听器（手动确认模式）
 *
 * @author zy
 * @date 2025-10-16 11:32
 **/
public class BatchMessageListenerTest extends AbstractRabbitMQListener<BatchTradeOrder> {

    private final List<Long> processedDeliveryTags = new ArrayList<>();

    public BatchMessageListenerTest() {
        super(RabbitMqBindConfig.EXAMPLE_ORDER_QUEUE);
    }


    /**
     * 处理消息
     *
     * @param message     消息对象
     * @param channel     RabbitMQ通道
     * @param amqpMessage 原始AMQP消息
     */
    @Override
    public void onMessage(BatchTradeOrder message, Channel channel, Message amqpMessage) {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();

        try {
            // 处理消息
            processBatchMessage(message);

            // 记录已处理的消息
            processedDeliveryTags.add(deliveryTag);

            // 每处理10条消息批量确认一次
            if (processedDeliveryTags.size() >= 10) {
                batchAckMessages(channel);
            }

        } catch (Exception e) {
            logger.error("Failed to process batch message: {}", message.getBatchId(), e);
            // 单个消息失败，只拒绝当前消息
            ManualAckHelper.rejectToDlx(channel, amqpMessage, "BATCH_PROCESS_FAILED");
        }
    }

    /**
     * 是否自动确认消息
     *
     * @return true: 自动确认, false: 手动确认
     */
    @Override
    public boolean autoAck() {
        return false; // 手动确认模式
    }

    /**
     * 消费失败时的重试次数
     *
     * @return 重试次数，默认3次
     */
    @Override
    public int retryTimes() {
        return super.retryTimes();
    }

    /**
     * 消费失败时的重试间隔（毫秒）
     *
     * @return 重试间隔，默认1000ms
     */
    @Override
    public long retryInterval() {
        return super.retryInterval();
    }

    /**
     * 是否启用死信队列
     *
     * @return true: 启用, false: 不启用
     */
    @Override
    public boolean enableDlx() {
        return super.enableDlx();
    }

    /**
     * 批量确认消息
     */
    private void batchAckMessages(Channel channel) {
        if (processedDeliveryTags.isEmpty()) {
            return;
        }

        // 获取最大的deliveryTag进行批量确认
        long maxDeliveryTag = processedDeliveryTags.stream()
                .max(Long::compareTo)
                .orElse(0L);

        if (maxDeliveryTag > 0) {
            ManualAckHelper.batchAck(channel, maxDeliveryTag, true);
            processedDeliveryTags.clear();
            logger.info("Batch acknowledged messages up to deliveryTag: {}", maxDeliveryTag);
        }
    }

    private void processBatchMessage(BatchTradeOrder message) {
        // 批量处理逻辑
        // batchService.process(message);
    }
}
