## v1.0.7 (2026-04-30)
### 💎 功能优化

- **silky-rabbitmq**: 完善 `RabbitMQListenerContainer` 消息消费失败时的可观测性
    - 当 `enableDlx = true` 且重试次数用尽时，增加 `recordPersistenceFailure()` 调用，确保消息消费失败事件被完整记录
    - 保证开启/关闭死信队列两种场景下，消费失败统计和监控的一致性
    - 关联 Issue：[#6](https://github.com/yijuanmao/silky-starter/issues/6)

## v1.0.6 (2026-04-29)
### 🐛 Bug Fixes

- **silky-rabbitmq**: 修复 `RabbitMQListenerContainer#sendToDeadLetterExchange()` 死信队列发送消息，序列化异常
    - 关联 Issue：[#5](https://github.com/yijuanmao/silky-starter/issues/5)

## v1.0.5 (2026-04-28)
### 💎 功能优化
* hutool工具类 =》 5.8.44版本


### 🐛 Bug Fixes
- **silky-rabbitmq**: 修复 `SimpleRabbitListenerContainerFactory` 未加载 yml 手动确认配置，导致重复消费及 `PRECONDITION_FAILED - unknown delivery tag` 报错
    - 关联 Issue：[#4](https://github.com/yijuanmao/silky-starter/issues/4)


## v1.0.4 (2026-03-13)
### 💎 功能优化
* 重命名RabbitSendTemplate为SkRabbitMqTemplate及相关实现类
* 添加RabbitPayload注解用于标记消息体参数
* 重构消息发送方法，支持延迟消息和异步发送
