package com.anushibinj.resttosmtp.benchmark;

import com.anushibinj.resttosmtp.config.AsyncConfig;
import com.anushibinj.resttosmtp.dto.EmailProxyRequest;
import com.anushibinj.resttosmtp.service.EmailService;
import com.anushibinj.resttosmtp.service.MailSenderFactory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.concurrent.*;

/**
 * JMH benchmarks for virtual thread performance in the REST-to-SMTP proxy.
 *
 * Measures:
 * - Virtual thread creation and task execution time
 * - Request queuing performance
 * - Throughput under concurrent virtual thread load
 *
 * Run with: mvn verify -Dtest=VirtualThreadPerformanceBenchmark
 */
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(value = 2)
@Threads(100)
@State(Scope.Benchmark)
public class VirtualThreadPerformanceBenchmark {

    private Executor virtualThreadExecutor;
    private CountDownLatch taskCompletionLatch;

    @Setup(Level.Trial)
    public void setup() {
        virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Setup(Level.Invocation)
    public void setupInvocation() {
        taskCompletionLatch = new CountDownLatch(1);
    }

    @TearDown(Level.Trial)
    public void teardown() {
        if (virtualThreadExecutor instanceof ExecutorService executor) {
            executor.shutdown();
        }
    }

    /**
     * Benchmark: Submit a lightweight task to virtual thread executor and wait for completion.
     * This simulates the async email dispatch overhead.
     */
    @Benchmark
    public void benchmarkVirtualThreadTaskSubmission() throws InterruptedException {
        virtualThreadExecutor.execute(() -> {
            // Simulate minimal work (no I/O)
            int sum = 0;
            for (int i = 0; i < 1000; i++) {
                sum += i;
            }
            taskCompletionLatch.countDown();
        });

        taskCompletionLatch.await(1, TimeUnit.SECONDS);
    }

    /**
     * Benchmark: Simulate high-concurrency email request queuing.
     * 100 concurrent threads submitting tasks to the virtual thread executor.
     */
    @Benchmark
    @Threads(100)
    public void benchmarkHighConcurrencyTaskQueuing() {
        CountDownLatch latch = new CountDownLatch(1);
        virtualThreadExecutor.execute(() -> {
            // Simulate minimal I/O delay
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            latch.countDown();
        });

        try {
            latch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Benchmark: Task submission without waiting for completion.
     * Simulates fire-and-forget behavior where the HTTP response is sent immediately.
     */
    @Benchmark
    public void benchmarkFireAndForgetTaskSubmission() {
        virtualThreadExecutor.execute(() -> {
            // Simulate minimal background work
            int sum = 0;
            for (int i = 0; i < 500; i++) {
                sum += i;
            }
        });
    }

    /**
     * Main method to run benchmarks programmatically.
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(VirtualThreadPerformanceBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}
