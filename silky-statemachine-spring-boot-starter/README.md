# silky-statemachine-spring-boot-starter

<p align="center">
  <img src="https://img.shields.io/badge/Silky%20Starter-State%20Machine-brightgreen" alt="Silky Starter State Machine"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-2.7+-blue" alt="Spring Boot 2.7+"/>
  <img src="https://img.shields.io/badge/JDK-1.8+-orange" alt="JDK 1.8+"/>
  <img src="https://img.shields.io/badge/License-Apache%202.0-green" alt="License"/>
</p>

> 轻量级状态机组件，支持状态转换、守卫条件、动作执行、事件监听，适用于订单、支付等业务状态流转。

## 特性

- **简洁易用**：基于 Spring 事件机制，零侵入业务代码
- **灵活配置**：支持代码配置和外部配置
- **守卫条件**：状态转换前条件检查
- **动作执行**：状态转换时执行业务逻辑
- **事件监听**：状态变更事件监听与处理
- **持久化支持**：可自定义状态持久化策略

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>top.silky</groupId>
    <artifactId>silky-statemachine-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 定义状态机配置

```java
import com.silky.starter.statemachine.core.config.StateMachineConfig;
import com.silky.starter.statemachine.core.transition.StateTransition;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class OrderStateMachineConfig implements StateMachineConfig {
    
    @Override
    public String getMachineType() {
        return "ORDER";  // 状态机类型标识
    }
    
    @Override
    public String getInitialState() {
        return "CREATED";  // 初始状态
    }
    
    @Override
    public Map<String, StateTransition> getTransitions() {
        Map<String, StateTransition> transitions = new HashMap<>();
        
        // 创建 -> 支付中（支付事件）
        transitions.put("PAY", new StateTransition("CREATED", "PAY", "PAYING"));
        
        // 支付中 -> 已支付（支付成功事件）
        transitions.put("PAY_SUCCESS", new StateTransition("PAYING", "PAY_SUCCESS", "PAID"));
        
        // 支付中 -> 已取消（支付失败/取消事件）
        transitions.put("PAY_FAIL", new StateTransition("PAYING", "PAY_FAIL", "CANCELLED"));
        
        // 已支付 -> 已发货（发货事件）
        transitions.put("SHIP", new StateTransition("PAID", "SHIP", "SHIPPED"));
        
        // 已发货 -> 已完成（确认收货事件）
        transitions.put("RECEIVE", new StateTransition("SHIPPED", "RECEIVE", "COMPLETED"));
        
        // 已支付 -> 退款中（退款事件）
        transitions.put("REFUND", new StateTransition("PAID", "REFUND", "REFUNDING"));
        
        // 退款中 -> 已退款（退款成功事件）
        transitions.put("REFUND_SUCCESS", new StateTransition("REFUNDING", "REFUND_SUCCESS", "REFUNDED"));
        
        return transitions;
    }
}
```

### 3. 实现状态持久化

```java
import com.silky.starter.statemachine.core.context.StateMachineContext;
import com.silky.starter.statemachine.core.persist.StateMachinePersist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderStateMachinePersist implements StateMachinePersist {
    
    @Autowired
    private OrderService orderService;
    
    @Override
    public String readState(StateMachineContext context) {
        // 从数据库读取当前状态
        String orderId = context.getBusinessId();
        Order order = orderService.getById(orderId);
        return order != null ? order.getStatus() : null;
    }
    
    @Override
    public void writeState(String state, StateMachineContext context) {
        // 将新状态写入数据库
        String orderId = context.getBusinessId();
        orderService.updateStatus(orderId, state);
    }
    
    @Override
    public boolean supports(String machineType) {
        return "ORDER".equals(machineType);
    }
}
```

### 4. 使用状态机

```java
import com.silky.starter.statemachine.factory.StateMachineFactory;
import com.silky.starter.statemachine.core.generic.GenericStateMachine;
import com.silky.starter.statemachine.core.context.StateMachineContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    
    @Autowired
    private StateMachineFactory stateMachineFactory;
    
    /**
     * 支付订单
     */
    public void payOrder(String orderId) {
        // 创建状态机实例
        GenericStateMachine stateMachine = stateMachineFactory.create("ORDER");
        
        // 构建上下文
        StateMachineContext context = new StateMachineContext("ORDER", orderId);
        
        // 触发支付事件
        stateMachine.trigger("PAY", context);
    }
    
    /**
     * 支付成功回调
     */
    public void onPaySuccess(String orderId) {
        GenericStateMachine stateMachine = stateMachineFactory.create("ORDER");
        StateMachineContext context = new StateMachineContext("ORDER", orderId);
        stateMachine.trigger("PAY_SUCCESS", context);
    }
    
    /**
     * 发货
     */
    public void shipOrder(String orderId, String trackingNo) {
        GenericStateMachine stateMachine = stateMachineFactory.create("ORDER");
        StateMachineContext context = new StateMachineContext("ORDER", orderId);
        context.setVariable("trackingNo", trackingNo);
        stateMachine.trigger("SHIP", context);
    }
}
```

## 核心API详解

### StateMachineFactory 状态机工厂

```java
public class StateMachineFactory {
    
    /**
     * 创建状态机实例
     * 根据状态机类型获取对应的配置并创建实例
     * 
     * @param machineType 状态机类型，如 "ORDER", "PAYMENT"
     * @return 状态机实例
     * @throws IllegalArgumentException 如果找不到对应的配置
     */
    public GenericStateMachine create(String machineType);
    
    /**
     * 检查是否支持指定的状态机类型
     * 
     * @param machineType 状态机类型
     * @return true 表示支持
     */
    public boolean supports(String machineType);
    
    /**
     * 获取所有支持的状态机类型
     * 
     * @return 状态机类型数组
     */
    public String[] getSupportedMachineTypes();
}
```

### GenericStateMachine 通用状态机

```java
public class GenericStateMachine {
    
    /**
     * 触发状态转换事件
     * 执行完整的转换流程：检查当前状态 -> 验证守卫条件 -> 执行动作 -> 更新状态 -> 发布事件
     * 
     * @param event   事件名称，如 "PAY", "SHIP"
     * @param context 状态机上下文
     * @return 上下文对象（包含最新状态）
     * @throws StateMachineException 转换失败时抛出
     */
    public StateMachineContext trigger(String event, StateMachineContext context);
    
    /**
     * 获取当前状态
     * 从持久化层读取当前状态
     * 
     * @param context 状态机上下文
     * @return 当前状态值
     */
    public String getCurrentState(StateMachineContext context);
    
    /**
     * 获取状态机配置
     */
    public StateMachineConfig getConfig();
    
    /**
     * 获取持久化实现
     */
    public StateMachinePersist getPersist();
}
```

### StateMachineConfig 状态机配置接口

```java
public interface StateMachineConfig {
    
    /**
     * 获取初始状态
     * 新创建的业务对象初始状态
     * 
     * @return 初始状态值
     */
    String getInitialState();
    
    /**
     * 获取所有状态转换定义
     * 
     * @return 事件 -> 转换定义的 Map
     */
    Map<String, StateTransition> getTransitions();
    
    /**
     * 获取状态机类型
     * 用于区分不同的状态机，如 "ORDER", "PAYMENT"
     * 
     * @return 状态机类型
     */
    String getMachineType();
    
    /**
     * 获取持久化 Bean 名称
     * 默认 "stateMachinePersist"
     * 
     * @return Bean 名称
     */
    default String getPersistBeanName();
    
    /**
     * 根据事件获取转换定义
     * 
     * @param event 事件名称
     * @return 转换定义
     */
    default StateTransition getTransition(String event);
    
    /**
     * 验证配置有效性
     * 
     * @return true 表示配置有效
     */
    default boolean isValid();
}
```

### StateTransition 状态转换

```java
@Data
@ToString
public class StateTransition {
    
    /**
     * 当前状态（转换前）
     */
    private String currentState;
    
    /**
     * 触发事件
     */
    private String event;
    
    /**
     * 目标状态（转换后）
     */
    private String nextState;
    
    /**
     * 守卫条件
     * 只有条件满足时才允许转换
     */
    private TransitionGuard guard;
    
    /**
     * 触发动作列表
     * 状态转换时执行的业务逻辑
     */
    private List<TransitionAction> actions = new ArrayList<>();
    
    /**
     * 转换描述（可选）
     */
    private String description;
    
    // 便捷构造函数
    public StateTransition(String currentState, String event, String nextState) {
        this.currentState = currentState;
        this.event = event;
        this.nextState = nextState;
    }
    
    /**
     * 验证转换是否有效
     */
    public boolean isValid() {
        return currentState != null && event != null && nextState != null;
    }
}
```

### StateMachineContext 状态机上下文

```java
@Data
@ToString
@Accessors(chain = true)
public class StateMachineContext implements Serializable {
    
    private static final long serialVersionUID = 960379424306831931L;
    
    /**
     * 状态机类型
     * 如 "ORDER", "PAYMENT"
     */
    private String machineType;
    
    /**
     * 业务 ID
     * 如订单 ID、支付 ID
     */
    private String businessId;
    
    /**
     * 扩展变量
     * 用于在状态转换过程中传递额外数据
     */
    private Map<String, Object> variables;
    
    /**
     * 设置变量（链式调用）
     */
    public void setVariable(String key, Object value);
    
    /**
     * 获取变量
     */
    public Object getVariable(String key);
    
    // 构造函数
    public StateMachineContext(String machineType, String businessId);
    
    public StateMachineContext(String machineType, String businessId, Map<String, Object> variables);
}
```

### StateMachinePersist 状态持久化接口

```java
public interface StateMachinePersist {
    
    /**
     * 读取状态
     * 从持久化存储中读取当前状态
     * 
     * @param context 状态机上下文
     * @return 状态值
     */
    String readState(StateMachineContext context);
    
    /**
     * 写入状态
     * 将新状态写入持久化存储
     * 
     * @param state   新状态值
     * @param context 状态机上下文
     */
    void writeState(String state, StateMachineContext context);
    
    /**
     * 读取扩展数据（可选）
     * 
     * @param context 状态机上下文
     * @return 扩展数据 JSON 字符串
     */
    default String readExtendedState(StateMachineContext context);
    
    /**
     * 写入扩展数据（可选）
     * 
     * @param extendedState 扩展数据 JSON 字符串
     * @param context       状态机上下文
     */
    default void writeExtendedState(String extendedState, StateMachineContext context);
    
    /**
     * 判断是否支持指定的状态机类型
     * 
     * @param machineType 状态机类型
     * @return true 表示支持
     */
    default boolean supports(String machineType);
}
```

### TransitionGuard 守卫条件接口

```java
@FunctionalInterface
public interface TransitionGuard {
    
    /**
     * 评估是否允许状态转换
     * 
     * @param context 状态机上下文
     * @return true 表示允许转换，false 表示不允许
     */
    boolean evaluate(StateMachineContext context);
    
    /**
     * 组合多个守卫条件（AND 逻辑）
     * 所有条件都满足才允许转换
     */
    static TransitionGuard and(TransitionGuard... guards);
    
    /**
     * 组合多个守卫条件（OR 逻辑）
     * 任一条件满足就允许转换
     */
    static TransitionGuard or(TransitionGuard... guards);
    
    /**
     * 取反守卫条件
     */
    static TransitionGuard not(TransitionGuard guard);
}
```

### TransitionAction 转换动作接口

```java
@FunctionalInterface
public interface TransitionAction {
    
    /**
     * 执行转换动作
     * 
     * @param fromState 转换前的状态
     * @param event     触发转换的事件
     * @param context   状态机上下文
     */
    void execute(String fromState, String event, StateMachineContext context);
    
    /**
     * 组合动作（顺序执行）
     * 多个动作按顺序执行
     */
    static TransitionAction sequence(TransitionAction... actions);
    
    /**
     * 空动作
     * 什么都不做
     */
    static TransitionAction noop();
}
```

## 使用示例

### 带守卫条件的状态机

```java
@Component
public class OrderStateMachineConfig implements StateMachineConfig {
    
    @Override
    public Map<String, StateTransition> getTransitions() {
        Map<String, StateTransition> transitions = new HashMap<>();
        
        // 支付事件带守卫条件：检查库存
        StateTransition payTransition = new StateTransition("CREATED", "PAY", "PAYING");
        payTransition.setGuard(context -> {
            String orderId = context.getBusinessId();
            Order order = orderService.getById(orderId);
            // 检查库存是否充足
            return inventoryService.hasEnoughStock(order.getProductId(), order.getQuantity());
        });
        transitions.put("PAY", payTransition);
        
        // 发货事件带守卫条件：检查支付状态
        StateTransition shipTransition = new StateTransition("PAID", "SHIP", "SHIPPED");
        shipTransition.setGuard(context -> {
            String orderId = context.getBusinessId();
            Payment payment = paymentService.getByOrderId(orderId);
            // 检查是否已支付
            return payment != null && payment.isPaid();
        });
        transitions.put("SHIP", shipTransition);
        
        return transitions;
    }
    
    // ... 其他方法
}
```

### 带动作的状态机

```java
@Component
public class OrderStateMachineConfig implements StateMachineConfig {
    
    @Autowired
    private InventoryService inventoryService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Override
    public Map<String, StateTransition> getTransitions() {
        Map<String, StateTransition> transitions = new HashMap<>();
        
        // 支付成功事件：扣减库存 + 发送通知
        StateTransition paySuccessTransition = new StateTransition("PAYING", "PAY_SUCCESS", "PAID");
        paySuccessTransition.setActions(Arrays.asList(
            // 扣减库存
            (fromState, event, context) -> {
                String orderId = context.getBusinessId();
                Order order = orderService.getById(orderId);
                inventoryService.decrease(order.getProductId(), order.getQuantity());
            },
            // 发送支付成功通知
            (fromState, event, context) -> {
                String orderId = context.getBusinessId();
                notificationService.sendPaySuccessNotification(orderId);
            }
        ));
        transitions.put("PAY_SUCCESS", paySuccessTransition);
        
        // 发货事件：记录物流信息
        StateTransition shipTransition = new StateTransition("PAID", "SHIP", "SHIPPED");
        shipTransition.setAction((fromState, event, context) -> {
            String orderId = context.getBusinessId();
            String trackingNo = (String) context.getVariable("trackingNo");
            logisticsService.createShipment(orderId, trackingNo);
        });
        transitions.put("SHIP", shipTransition);
        
        return transitions;
    }
    
    // ... 其他方法
}
```

### 状态变更事件监听

```java
@Component
public class OrderStateChangeHandler implements StateChangeHandler {
    
    @Autowired
    private OrderLogService orderLogService;
    
    @Autowired
    private MetricsService metricsService;
    
    @Override
    public void handle(StateChangeEvent event) {
        String machineType = event.getMachineType();
        if (!"ORDER".equals(machineType)) {
            return;
        }
        
        String orderId = event.getContext().getBusinessId();
        String fromState = event.getFromState();
        String toState = event.getToState();
        String triggerEvent = event.getEvent();
        
        // 记录状态变更日志
        OrderLog log = new OrderLog();
        log.setOrderId(orderId);
        log.setFromStatus(fromState);
        log.setToStatus(toState);
        log.setEvent(triggerEvent);
        log.setCreateTime(LocalDateTime.now());
        orderLogService.save(log);
        
        // 统计指标
        metricsService.recordStateChange(machineType, fromState, toState);
        
        // 特定状态变更的特殊处理
        if ("PAID".equals(toState)) {
            // 支付成功后的额外处理
            onOrderPaid(orderId);
        } else if ("COMPLETED".equals(toState)) {
            // 订单完成后的额外处理
            onOrderCompleted(orderId);
        }
    }
    
    private void onOrderPaid(String orderId) {
        // 触发积分发放、优惠券发放等
    }
    
    private void onOrderCompleted(String orderId) {
        // 触发评价邀请、售后服务开通等
    }
}
```

### 支付状态机示例

```java
@Component
public class PaymentStateMachineConfig implements StateMachineConfig {
    
    @Override
    public String getMachineType() {
        return "PAYMENT";
    }
    
    @Override
    public String getInitialState() {
        return "INIT";
    }
    
    @Override
    public Map<String, StateTransition> getTransitions() {
        Map<String, StateTransition> transitions = new HashMap<>();
        
        // 初始化 -> 处理中（提交支付）
        transitions.put("SUBMIT", new StateTransition("INIT", "SUBMIT", "PROCESSING"));
        
        // 处理中 -> 成功（支付成功回调）
        StateTransition successTransition = new StateTransition("PROCESSING", "SUCCESS_CALLBACK", "SUCCESS");
        successTransition.setAction((fromState, event, context) -> {
            String paymentId = context.getBusinessId();
            // 更新订单支付状态
            String orderId = (String) context.getVariable("orderId");
            orderService.onPaySuccess(orderId, paymentId);
        });
        transitions.put("SUCCESS_CALLBACK", successTransition);
        
        // 处理中 -> 失败（支付失败回调）
        StateTransition failTransition = new StateTransition("PROCESSING", "FAIL_CALLBACK", "FAILED");
        failTransition.setAction((fromState, event, context) -> {
            String paymentId = context.getBusinessId();
            String errorCode = (String) context.getVariable("errorCode");
            String errorMsg = (String) context.getVariable("errorMsg");
            paymentService.recordFailReason(paymentId, errorCode, errorMsg);
        });
        transitions.put("FAIL_CALLBACK", failTransition);
        
        // 处理中 -> 关闭（超时关闭）
        transitions.put("TIMEOUT", new StateTransition("PROCESSING", "TIMEOUT", "CLOSED"));
        
        // 失败 -> 处理中（重新支付）
        transitions.put("RETRY", new StateTransition("FAILED", "RETRY", "PROCESSING"));
        
        return transitions;
    }
}
```

### 退款状态机示例

```java
@Component
public class RefundStateMachineConfig implements StateMachineConfig {
    
    @Override
    public String getMachineType() {
        return "REFUND";
    }
    
    @Override
    public String getInitialState() {
        return "INIT";
    }
    
    @Override
    public Map<String, StateTransition> getTransitions() {
        Map<String, StateTransition> transitions = new HashMap<>();
        
        // 申请退款
        transitions.put("APPLY", new StateTransition("INIT", "APPLY", "PENDING_REVIEW"));
        
        // 审核通过
        StateTransition approveTransition = new StateTransition("PENDING_REVIEW", "APPROVE", "APPROVED");
        approveTransition.setGuard(context -> {
            String refundId = context.getBusinessId();
            Refund refund = refundService.getById(refundId);
            // 检查退款金额是否合法
            return refund.getAmount().compareTo(refund.getOrder().getPaidAmount()) <= 0;
        });
        transitions.put("APPROVE", approveTransition);
        
        // 审核拒绝
        transitions.put("REJECT", new StateTransition("PENDING_REVIEW", "REJECT", "REJECTED"));
        
        // 退款处理中
        transitions.put("PROCESS", new StateTransition("APPROVED", "PROCESS", "PROCESSING"));
        
        // 退款成功
        StateTransition successTransition = new StateTransition("PROCESSING", "SUCCESS", "SUCCESS");
        successTransition.setAction((fromState, event, context) -> {
            String refundId = context.getBusinessId();
            // 恢复库存
            Refund refund = refundService.getById(refundId);
            inventoryService.increase(refund.getProductId(), refund.getQuantity());
        });
        transitions.put("SUCCESS", successTransition);
        
        // 退款失败
        transitions.put("FAIL", new StateTransition("PROCESSING", "FAIL", "FAILED"));
        
        return transitions;
    }
}
```

### 复杂业务场景：订单全流程

```java
@Service
public class OrderLifecycleService {
    
    @Autowired
    private StateMachineFactory stateMachineFactory;
    
    /**
     * 创建订单
     */
    public Order createOrder(CreateOrderRequest request) {
        // 创建订单（初始状态 CREATED）
        Order order = new Order();
        order.setStatus("CREATED");
        order = orderDao.save(order);
        
        return order;
    }
    
    /**
     * 用户支付
     */
    public void pay(String orderId, PaymentRequest request) {
        GenericStateMachine stateMachine = stateMachineFactory.create("ORDER");
        StateMachineContext context = new StateMachineContext("ORDER", orderId);
        context.setVariable("paymentMethod", request.getPaymentMethod());
        context.setVariable("amount", request.getAmount());
        
        stateMachine.trigger("PAY", context);
    }
    
    /**
     * 支付回调
     */
    public void onPaymentCallback(String orderId, PaymentCallback callback) {
        GenericStateMachine stateMachine = stateMachineFactory.create("ORDER");
        StateMachineContext context = new StateMachineContext("ORDER", orderId);
        context.setVariable("transactionId", callback.getTransactionId());
        context.setVariable("paymentTime", callback.getPaymentTime());
        
        if (callback.isSuccess()) {
            stateMachine.trigger("PAY_SUCCESS", context);
        } else {
            context.setVariable("errorCode", callback.getErrorCode());
            stateMachine.trigger("PAY_FAIL", context);
        }
    }
    
    /**
     * 商家发货
     */
    public void ship(String orderId, ShipRequest request) {
        GenericStateMachine stateMachine = stateMachineFactory.create("ORDER");
        StateMachineContext context = new StateMachineContext("ORDER", orderId);
        context.setVariable("trackingNo", request.getTrackingNo());
        context.setVariable("logisticsCompany", request.getLogisticsCompany());
        
        stateMachine.trigger("SHIP", context);
    }
    
    /**
     * 确认收货
     */
    public void confirmReceive(String orderId) {
        GenericStateMachine stateMachine = stateMachineFactory.create("ORDER");
        StateMachineContext context = new StateMachineContext("ORDER", orderId);
        
        stateMachine.trigger("RECEIVE", context);
    }
    
    /**
     * 申请退款
     */
    public void applyRefund(String orderId, RefundRequest request) {
        // 先检查订单状态是否允许退款
        GenericStateMachine orderMachine = stateMachineFactory.create("ORDER");
        StateMachineContext orderContext = new StateMachineContext("ORDER", orderId);
        String currentState = orderMachine.getCurrentState(orderContext);
        
        if (!Arrays.asList("PAID", "SHIPPED").contains(currentState)) {
            throw new BusinessException("当前状态不允许退款");
        }
        
        // 触发订单退款事件
        orderMachine.trigger("REFUND", orderContext);
        
        // 创建退款单并启动退款状态机
        Refund refund = refundService.create(orderId, request);
        GenericStateMachine refundMachine = stateMachineFactory.create("REFUND");
        StateMachineContext refundContext = new StateMachineContext("REFUND", refund.getId());
        refundMachine.trigger("APPLY", refundContext);
    }
    
    /**
     * 查询订单当前状态
     */
    public String getOrderStatus(String orderId) {
        GenericStateMachine stateMachine = stateMachineFactory.create("ORDER");
        StateMachineContext context = new StateMachineContext("ORDER", orderId);
        return stateMachine.getCurrentState(context);
    }
}
```

## 常见问题

### Q1: 如何处理状态转换失败？

A: 状态转换失败会抛出 `StateMachineException`，可以在业务层捕获处理：

```java
try {
    stateMachine.trigger("PAY", context);
} catch (StateMachineException e) {
    // 转换失败处理
    log.error("状态转换失败: {}", e.getMessage());
    // 返回友好错误信息给用户
    throw new BusinessException("当前状态不允许进行此操作");
}
```

### Q2: 如何实现状态历史记录？

A: 通过事件监听器记录状态变更历史：

```java
@Component
public class StateHistoryRecorder implements StateChangeHandler {
    
    @Autowired
    private StateHistoryDao stateHistoryDao;
    
    @Override
    public void handle(StateChangeEvent event) {
        StateHistory history = new StateHistory();
        history.setMachineType(event.getMachineType());
        history.setBusinessId(event.getContext().getBusinessId());
        history.setFromState(event.getFromState());
        history.setToState(event.getToState());
        history.setEvent(event.getEvent());
        history.setCreateTime(LocalDateTime.now());
        
        stateHistoryDao.save(history);
    }
}
```

### Q3: 支持并行状态吗？

A: 当前版本支持单状态机模式。如需并行状态（如订单同时有支付状态和物流状态），建议创建多个状态机实例：

```java
// 订单支付状态机
GenericStateMachine paymentMachine = stateMachineFactory.create("ORDER_PAYMENT");

// 订单物流状态机
GenericStateMachine logisticsMachine = stateMachineFactory.create("ORDER_LOGISTICS");
```

### Q4: 如何实现定时状态转换？

A: 结合 Spring Scheduler 实现：

```java
@Component
public class OrderTimeoutScheduler {
    
    @Autowired
    private StateMachineFactory stateMachineFactory;
    
    @Autowired
    private OrderService orderService;
    
    /**
     * 检查超时未支付订单
     */
    @Scheduled(fixedRate = 60000)  // 每分钟执行
    public void checkTimeoutOrders() {
        List<Order> timeoutOrders = orderService.findTimeoutOrders(30);  // 30分钟超时
        
        for (Order order : timeoutOrders) {
            try {
                GenericStateMachine stateMachine = stateMachineFactory.create("ORDER");
                StateMachineContext context = new StateMachineContext("ORDER", order.getId());
                stateMachine.trigger("TIMEOUT", context);
            } catch (Exception e) {
                log.error("订单超时处理失败: {}", order.getId(), e);
            }
        }
    }
}
```

## 相关链接

- [GitHub 仓库](https://github.com/yijuanmao/silky-starter)
- [Gitee 仓库](https://gitee.com/zeng_er/silky-starter)
- [Spring Statemachine 文档](https://docs.spring.io/spring-statemachine/docs/current/reference/)

## License

Apache License 2.0
