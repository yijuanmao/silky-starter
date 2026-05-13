package com.silky.starter.excel.core.engine;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AbstractExcelEngine 单元测试
 *
 * @author zy
 * @since 1.1.0
 */
class AbstractExcelEngineTest {

    /**
     * 测试引擎启动时间正确存储
     */
    @Test
    void testEngineStartDateTime_ShouldNotChange() throws InterruptedException {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        TestEngine engine = new TestEngine(executor);
        LocalDateTime startTime = engine.getEngineStartDateTime();

        // 等待一小段时间，确保 getEngineStartDateTime 返回的是固定值
        Thread.sleep(100);

        // 再次获取应该返回相同的时间
        assertEquals(startTime, engine.getEngineStartDateTime());

        // uptime 应该增加
        assertTrue(engine.getUptime() >= 100);

        engine.shutdown();
        executor.shutdown();
    }

    /**
     * 测试统计计数器
     */
    @Test
    void testCounters() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        TestEngine engine = new TestEngine(executor);

        engine.incrementSuccess();
        engine.incrementSuccess();
        engine.incrementFailed();
        engine.incrementTotalProcessed();
        engine.incrementTotalProcessed();
        engine.incrementTotalProcessed();

        assertEquals(2.0 / 3.0, engine.getSuccessRate(), 0.001);

        engine.shutdown();
        executor.shutdown();
    }

    /**
     * 测试共享清理执行器模式
     */
    @Test
    void testSharedCleanupExecutor() {
        ScheduledExecutorService shared = Executors.newSingleThreadScheduledExecutor();
        TestEngine engine1 = new TestEngine("引擎1", shared);
        TestEngine engine2 = new TestEngine("引擎2", shared);

        // 两个引擎不应抛出异常
        assertDoesNotThrow(() -> {
            engine1.cleanupExpiredCaches();
            engine2.cleanupExpiredCaches();
        });

        engine1.shutdown();
        engine2.shutdown();
        shared.shutdown();
    }

    /** 测试用引擎实现 */
    private static class TestEngine extends AbstractExcelEngine {

        TestEngine(ScheduledExecutorService cleanupExecutor) {
            super("测试引擎", cleanupExecutor);
        }

        TestEngine(String name, ScheduledExecutorService cleanupExecutor) {
            super(name, cleanupExecutor);
        }

        @Override
        protected void cleanupExpiredCaches() {
            // 测试空实现
        }

        @Override
        protected int getCacheSize() {
            return 0;
        }
    }
}
