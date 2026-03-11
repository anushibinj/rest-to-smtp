package com.anushibinj.resttosmtp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Configures Spring's {@code @Async} infrastructure to use a virtual-thread-per-task executor.
 *
 * <p>By default Spring uses {@code SimpleAsyncTaskExecutor} (platform threads). Overriding it
 * with {@link Executors#newVirtualThreadPerTaskExecutor()} means every asynchronous email
 * dispatch gets its own lightweight virtual thread, allowing hundreds of thousands of
 * concurrent in-flight sends without exhausting OS thread resources.
 *
 * <p>This executor is also exposed as a named bean ({@code "virtualThreadExecutor"}) so
 * callers can inject it where needed (e.g., custom {@code CompletableFuture} chains).
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Primary async executor — virtual thread per task.
     * Spring picks this up automatically as the default {@code @Async} executor.
     */
    @Override
    @Bean(name = "virtualThreadExecutor")
    public Executor getAsyncExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
