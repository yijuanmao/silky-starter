package com.silky.starter.redis.test.redission.server;

import com.silky.starter.redis.lock.template.RedisLockTemplate;
import com.silky.starter.redis.test.redission.RedisLockTemplateTest;
import com.silky.starter.redis.test.redission.entity.Payment;
import com.silky.starter.redis.test.redission.entity.TradeOrder;
import com.silky.starter.redis.test.redission.enums.OrderStatus;
import com.silky.starter.redis.test.redission.mapper.OrderRepository;
import com.silky.starter.redis.test.redission.result.PaymentResult;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 支付服务
 *
 * @author zy
 * @date 2025-10-23 14:57
 **/
public class PaymentService extends RedisLockTemplateTest {

    @Autowired
    private RedisLockTemplate redisLockTemplate;
    @Autowired
    private OrderRepository orderRepository;

    /**
     * 处理支付请求
     *
     * @param orderNo 订单号
     * @param amount  支付金额
     * @return PaymentResult
     */
    @Transactional
    public PaymentResult processPayment(String orderNo, BigDecimal amount) {
        return redisLockTemplate.lock(
                "payment:process:" + orderNo,
                10, 30, TimeUnit.SECONDS,
                true,
                () -> {
                    log.info("开始处理支付，订单号: {}, 金额: {}", orderNo, amount);

                    // 检查订单状态
                    TradeOrder tradeOrder = orderRepository.findByOrderNo(orderNo);

                    if (Objects.isNull(tradeOrder)) {
                        throw new RuntimeException("订单不存在");
                    }
                    if (tradeOrder.getStatus() != OrderStatus.CREATED.name()) {
                        throw new RuntimeException("订单状态异常");
                    }
                    // 执行支付
                    Payment payment = this.pay(tradeOrder);
                    // 更新订单状态
                    tradeOrder.setStatus(OrderStatus.PAID.name());
                    orderRepository.save(tradeOrder);

                    log.info("支付处理成功，订单号: {}, 支付ID: {}", orderNo, payment.getId());
                    return new PaymentResult(payment.getId());
                }
        );
    }

    /**
     * 批量处理订单
     *
     * @param orderNos 订单号列表
     */
    public void batchProcessOrders(List<String> orderNos) {
        // 获取原生RLock对象，最大灵活性
        RLock lock = redisLockTemplate.getRLock("batch:order:process");

        try {
            if (lock.tryLock(5, 60, TimeUnit.SECONDS)) {
                try {
                    log.info("开始批量处理订单，订单数量: {}", orderNos.size());

                    for (String orderNo : orderNos) {
                        processSingleOrder(orderNo);
                    }

                    log.info("批量处理订单完成");
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                throw new RuntimeException("系统繁忙，请稍后重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("处理被中断");
        }
    }

    /**
     * 处理单个订单的具体逻辑
     *
     * @param orderNo 订单号
     */
    private void processSingleOrder(String orderNo) {

        log.info("处理订单: {}", orderNo);
        // 具体处理逻辑
    }


    /**
     * 模拟支付操作
     *
     * @return Payment
     */
    private Payment pay(TradeOrder tradeOrder) {
        log.info("模拟支付操作，订单号: {}", tradeOrder.getOrderId());

        return new Payment();
    }
}
