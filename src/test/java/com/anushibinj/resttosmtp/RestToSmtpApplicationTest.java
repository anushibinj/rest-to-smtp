package com.anushibinj.resttosmtp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test that verifies the Spring application context loads successfully,
 * and covers the {@link RestToSmtpApplication#main} entry point.
 */
@SpringBootTest
@DisplayName("RestToSmtpApplication")
class RestToSmtpApplicationTest {

    @Test
    @DisplayName("Spring application context loads without errors")
    void contextLoads() {
        // If the context fails to start, Spring will throw and fail this test.
        // No explicit assertions needed — the @SpringBootTest lifecycle is the assertion.
    }

    @Test
    @DisplayName("main() method starts without throwing")
    void mainMethodRuns() {
        // Calling main() boots an embedded context; it should complete without exception.
        // The context may or may not fully start depending on port availability, but
        // the invocation itself must not throw during the call.
        RestToSmtpApplication.main(new String[]{"--server.port=0", "--spring.main.web-application-type=none"});
    }
}
