package com.anushibinj.resttosmtp.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link AsyncConfig}.
 *
 * <p>Boots the full Spring context to verify that the virtual-thread executor bean is
 * registered correctly and that it can actually submit work.
 */
@SpringBootTest
@DisplayName("AsyncConfig")
class AsyncConfigTest {

    @Autowired
    @Qualifier("virtualThreadExecutor")
    private Executor virtualThreadExecutor;

    @Test
    @DisplayName("virtualThreadExecutor bean is present in context")
    void beanIsPresent() {
        assertThat(virtualThreadExecutor).isNotNull();
    }

    @Test
    @DisplayName("virtualThreadExecutor can execute a Runnable without throwing")
    void executorCanRunTask() throws InterruptedException {
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        virtualThreadExecutor.execute(latch::countDown);
        boolean completed = latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(completed).isTrue();
    }

    @Test
    @DisplayName("tasks submitted to virtualThreadExecutor run on virtual threads")
    void tasksRunOnVirtualThreads() throws InterruptedException {
        java.util.concurrent.atomic.AtomicBoolean isVirtual = new java.util.concurrent.atomic.AtomicBoolean(false);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

        virtualThreadExecutor.execute(() -> {
            isVirtual.set(Thread.currentThread().isVirtual());
            latch.countDown();
        });

        latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(isVirtual.get()).isTrue();
    }
}
