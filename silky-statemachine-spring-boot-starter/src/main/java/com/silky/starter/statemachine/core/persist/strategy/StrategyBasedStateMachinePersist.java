package com.silky.starter.statemachine.core.persist.strategy;

import com.silky.starter.statemachine.core.context.StateMachineContext;
import com.silky.starter.statemachine.core.persist.StateMachinePersist;
import com.silky.starter.statemachine.core.persist.strategy.router.PersistenceStrategyRouter;
import com.silky.starter.statemachine.exception.StateMachineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于策略的状态机持久化实现
 *
 * @author zy
 * @date 2025-09-26 14:56
 **/
public class StrategyBasedStateMachinePersist implements StateMachinePersist {

    private static final Logger logger = LoggerFactory.getLogger(StrategyBasedStateMachinePersist.class);

    private final PersistenceStrategyRouter strategyRouter;

    public StrategyBasedStateMachinePersist(PersistenceStrategyRouter strategyRouter) {
        this.strategyRouter = strategyRouter;
    }

    /**
     * 读取状态
     *
     * @param context 状态机上下文
     * @return 状态值
     */
    @Override
    public String readState(StateMachineContext context) {
        String machineType = context.getMachineType();
        BusinessPersistenceStrategy strategy = strategyRouter.getStrategy(machineType);
        if (logger.isDebugEnabled()) {
            logger.debug("Reading state for machine type: {} using strategy: {}", machineType, strategy.getStrategyName());
        }
        try {
            return strategy.readState(context);
        } catch (Exception e) {
            logger.error("Error reading state for machine type: {} with strategy: {}", machineType, strategy.getStrategyName(), e);
            throw new StateMachineException("Failed to read state", e);
        }
    }

    /**
     * 写入状态
     *
     * @param state   状态值
     * @param context 状态机上下文
     */
    @Override
    public void writeState(String state, StateMachineContext context) {
        String machineType = context.getMachineType();
        BusinessPersistenceStrategy strategy = strategyRouter.getStrategy(machineType);
        if (logger.isDebugEnabled()) {
            logger.debug("Writing state {} for machine type: {} using strategy: {}", state, machineType, strategy.getStrategyName());
        }
        try {
            strategy.writeState(state, context);
            if (logger.isDebugEnabled()) {
                logger.debug("Successfully wrote state {} for machine type: {}", state, machineType);
            }
        } catch (Exception e) {
            logger.error("Error writing state {} for machine type: {} with strategy: {}", state, machineType, strategy.getStrategyName(), e);
            throw new StateMachineException("Failed to write state", e);
        }
    }

    /**
     * 读取扩展数据
     *
     * @param context 状态机上下文
     * @return 扩展数据JSON字符串
     */
    @Override
    public String readExtendedState(StateMachineContext context) {
        String machineType = context.getMachineType();
        BusinessPersistenceStrategy strategy = strategyRouter.getStrategy(machineType);
        return strategy.readExtendedState(context);
    }

    /**
     * 写入扩展数据
     *
     * @param extendedState 扩展数据JSON字符串
     * @param context       状态机上下文
     */
    @Override
    public void writeExtendedState(String extendedState, StateMachineContext context) {
        String machineType = context.getMachineType();
        BusinessPersistenceStrategy strategy = strategyRouter.getStrategy(machineType);

        strategy.writeExtendedState(extendedState, context);
    }

    /**
     * 判断是否支持指定的状态机类型
     *
     * @param machineType 状态机类型
     */
    @Override
    public boolean supports(String machineType) {
        return strategyRouter.supports(machineType);
    }

    /**
     * 获取支持的所有机器类型
     */
    public String[] getSupportedMachineTypes() {
        return strategyRouter.getSupportedMachineTypes();
    }
}
