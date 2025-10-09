package com.silky.starter.statemachine.trade.enums;

/**
 * 订单事件枚举
 *
 * @author zy
 * @date 2025-09-26 15:59
 **/
public enum OrderEventEnum {

    CREATE,           // 创建订单
    PAY_SUCCESS,      // 支付成功
    PAY_FAILED,       // 支付失败
}
