package com.contextengine.configuration.beans;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Provides a bounded asynchronous executor for future non-blocking infrastructure work.
 *
 * <p>The executor does not schedule work by itself. It is available for future components that
 * explicitly use asynchronous execution and participates in graceful application shutdown.</p>
 */
@Configuration(proxyBeanMethods = false)
public class ExecutorConfiguration {

    /**
     * Creates the default executor used by future asynchronous Spring infrastructure.
     *
     * @return a bounded, gracefully shutting down asynchronous task executor
     */
    @Bean(name = {"applicationTaskExecutor", "taskExecutor"})
    public AsyncTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("context-engine-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }
}
