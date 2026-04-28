## v1.0.4 (2026-03-13)
### 💎 功能优化
* 重命名RabbitSendTemplate为SkRabbitMqTemplate及相关实现类
* 添加RabbitPayload注解用于标记消息体参数
* 重构消息发送方法，支持延迟消息和异步发送

## v1.0.5 (2026-04-28)
### 💎 功能优化
* hutool工具类 =》 5.8.44版本


### 🐛 Bug Fixes

- **silky-rabbitmq**: 修复 `SimpleRabbitListenerContainerFactory` 未加载 yml 手动确认配置，导致重复消费及 `PRECONDITION_FAILED - unknown delivery tag` 报错
    - 关联 Issue：[#4](https://github.com/yijuanmao/silky-starter/issues/4)

