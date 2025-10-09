package com.silky.starter.statemachine.autoconfigure;

import com.silky.starter.statemachine.core.config.StateMachineConfig;
import com.silky.starter.statemachine.core.handler.StateChangeHandler;
import com.silky.starter.statemachine.core.listener.StateChangeEventListener;
import com.silky.starter.statemachine.core.persist.StateMachinePersist;
import com.silky.starter.statemachine.core.persist.strategy.BusinessPersistenceStrategy;
import com.silky.starter.statemachine.core.persist.strategy.StrategyBasedStateMachinePersist;
import com.silky.starter.statemachine.core.persist.strategy.impl.DefaultPersistenceStrategy;
import com.silky.starter.statemachine.core.persist.strategy.router.PersistenceStrategyRouter;
import com.silky.starter.statemachine.factory.StateMachineFactory;
import com.silky.starter.statemachine.registry.StateMachineRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 状态机自动配置类
 *
 * @author zy
 * @date 2025-09-12 16:30
 **/
@Configuration
public class StateMachineAutoConfiguration {

    @Bean
    public StateMachineRegistry ossAdapterRegistry(List<StateMachineConfig> stateMachineConfigs) {
        StateMachineRegistry registry = new StateMachineRegistry();
        for (StateMachineConfig config : stateMachineConfigs) {
            registry.register(config.getMachineType(), config);
        }
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public StateMachineFactory stateMachineFactory(StateMachineRegistry registry,
                                                   ApplicationEventPublisher eventPublisher,
                                                   ApplicationContext applicationContext) {
        return new StateMachineFactory(registry, eventPublisher, applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public StateChangeEventListener stateChangeEventListener(List<StateChangeHandler> handlers) {
        return new StateChangeEventListener(handlers);
    }

    @Bean
    public DefaultPersistenceStrategy defaultPersistenceStrategy() {
        return new DefaultPersistenceStrategy();
    }

    @Bean
    @ConditionalOnMissingBean
    public PersistenceStrategyRouter persistenceStrategyRouter(
            List<BusinessPersistenceStrategy> strategies,
            DefaultPersistenceStrategy defaultStrategy) {
        return new PersistenceStrategyRouter(strategies, defaultStrategy);
    }

    @Bean
    @ConditionalOnMissingBean
    public StateMachinePersist stateMachinePersist(PersistenceStrategyRouter strategyRouter) {
        return new StrategyBasedStateMachinePersist(strategyRouter);
    }

}
