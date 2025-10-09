package com.silky.starter.statemachine.core.context;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 状态机上下文
 *
 * @author zy
 * @date 2025-09-26 10:06
 **/
@Data
@ToString
@Accessors(chain = true)
public class StateMachineContext implements Serializable {

    private static final long serialVersionUID = 960379424306831931L;

    /**
     * 状态机类型,"ORDER", "PAYMENT"
     */
    private String machineType;

    /**
     * 业务ID，如订单ID、支付ID
     */
    private String businessId;

    /**
     * 扩展变量
     */
    private Map<String, Object> variables;


    public void setVariable(String key, Object value) {
        this.variables.put(key, value);
    }

    public Object getVariable(String key) {
        return this.variables.get(key);
    }


    public StateMachineContext() {
        this.variables = new HashMap<>(10);
    }

    public StateMachineContext(String machineType, String businessId) {
        this();
        this.machineType = machineType;
        this.businessId = businessId;
    }

    public StateMachineContext(String machineType, String businessId, Map<String, Object> variables) {
        this.machineType = machineType;
        this.businessId = businessId;
        this.variables = variables;
    }

}
