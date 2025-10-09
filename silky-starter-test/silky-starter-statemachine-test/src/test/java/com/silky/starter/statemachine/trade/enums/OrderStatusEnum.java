package com.silky.starter.statemachine.trade.enums;

/**
 * 统一下单状态枚举类
 */
public enum OrderStatusEnum {
    /**
     * 支付成功
     */
    SUCCESS("支付成功"),

    /**
     * 未支付
     */
    NOT_PAY("未支付"),

    /**
     * 支付中
     */
    PAYING("支付中"),

    /**
     * 失败
     */
    FAIL("失败"),
    ;

    /**
     * 消息
     */
    private final String msg;

    public String getMsg() {
        return msg;
    }

    OrderStatusEnum(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "OrderStatusEnum{" +
                "msg='" + msg + '\'' +
                "} " + super.toString();
    }
}
