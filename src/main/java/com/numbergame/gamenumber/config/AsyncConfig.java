package com.numbergame.gamenumber.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async Configuration - Production-grade thread pool setup
 *
 * Benefits:
 * - Non-blocking operations (Kafka, audit logs, batch sync)
 * - Better resource utilization
 * - Improved response times
 *
 * Industry standard: Netflix, Shopee, Grab
 */
@Configuration
@EnableAsync
@EnableScheduling
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Main async executor for background tasks
     * Pool size optimized for game operations
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool size: Always active threads
        executor.setCorePoolSize(10);

        // Max pool size: Maximum threads during peak load
        executor.setMaxPoolSize(50);

        // Queue capacity: Pending tasks buffer
        executor.setQueueCapacity(100);

        // Thread name prefix for debugging
        executor.setThreadNamePrefix("Async-");

        // Graceful shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        log.info("✅ Async Task Executor initialized: core={}, max={}, queue={}",
            10, 50, 100);

        return executor;
    }

    /**
     * Dedicated executor for batch operations
     * Separate pool to prevent starvation
     */
    @Bean(name = "batchExecutor")
    public Executor batchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("Batch-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();

        log.info("✅ Batch Executor initialized: core={}, max={}", 5, 10);

        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return taskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error("Async execution error in method: {}", method.getName(), ex);
            log.error("Parameters: {}", (Object) params);
        };
    }
}

