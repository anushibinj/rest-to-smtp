# REST-to-SMTP Proxy

[![CI — Test, Build & Push Docker Image](https://github.com/anushibinj/rest-to-smtp/actions/workflows/ci.yml/badge.svg)](https://github.com/anushibinj/rest-to-smtp/actions/workflows/ci.yml)
[![Docker Hub](https://img.shields.io/docker/pulls/anushibin007/rest-to-smtp)](https://hub.docker.com/r/anushibin007/rest-to-smtp)

A high-scale, stateless REST-to-SMTP proxy microservice built with Spring Boot 3.4 and Java 21 Virtual Threads.

Every HTTP request carries its own SMTP credentials in the JSON body. The service validates the payload, dispatches the email asynchronously on a virtual thread, and returns **HTTP 202 Accepted** immediately — the caller never waits for SMTP delivery to complete.

---

## Requirements

| Tool | Minimum version |
|---|---|
| Java | 21 |
| Maven | 3.9+ |

---

## Build

```bash
mvn clean package -DskipTests
```

To build **and** run the full test suite with coverage enforcement (≥ 90% instruction coverage):

```bash
mvn clean verify
```

The JaCoCo HTML coverage report is generated at:

```
target/site/jacoco/index.html
```

---

## Run

```bash
java -jar target/rest-to-smtp-1.0.0.jar
```

The server starts on port **8080** by default.

Override the port:

```bash
java -jar target/rest-to-smtp-1.0.0.jar --server.port=9090
```

---

## Docker Hub

The pre-built image is published automatically on every push to `master` (tests must pass first):

```
anushibin007/rest-to-smtp:latest
```

Pull and run directly without building locally:

```bash
docker pull anushibin007/rest-to-smtp:latest
docker run -p 8080:8080 anushibin007/rest-to-smtp:latest
```

Tags published per push:

| Tag | Description |
|---|---|
| `latest` | Most recent passing build on `master` |
| `sha-<short-sha>` | Immutable tag for the exact commit, e.g. `sha-a1b2c3d` |

---

## CI / CD

Every push to `master` triggers the GitHub Actions workflow (`.github/workflows/ci.yml`):

1. **Test** — `mvn clean verify` runs all 118 tests with a JaCoCo ≥ 90% instruction-coverage gate.
2. **Build** — Docker image is built from the multi-stage `Dockerfile`.
3. **Push** — Image is pushed to Docker Hub as `anushibin007/rest-to-smtp:latest` and `anushibin007/rest-to-smtp:sha-<short-sha>`.

The Docker push step is skipped if any test fails.

The workflow uses the repository **variable** `DOCKER_HUB_TOKEN` (set under *Settings → Secrets and variables → Actions → Variables*) as the Docker Hub access token.

---

## Docker

### Build the Image

```bash
docker build -t rest-to-smtp:latest .
```

### Run with Docker

```bash
docker run -p 8080:8080 rest-to-smtp:latest
```

The service listens on port `8080`. Override environment variables:

```bash
docker run \
  -p 8080:8080 \
  -e JAVA_OPTS="-Xmx512m -Xms256m" \
  rest-to-smtp:latest
```

### Run with Docker Compose

The included `docker-compose.yml` orchestrates the service with logging, health checks, and networking:

```bash
# Start in background
docker-compose up -d

# View logs
docker-compose logs -f

# Stop and clean up
docker-compose down
```

The service will be available at `http://localhost:8080` and `http://localhost:8080/swagger-ui/index.html` for the API documentation.

### Docker Image Details

- **Base**: Eclipse Temurin 21 JRE (Alpine Linux)
- **Size**: ~200 MB (multi-stage build)
- **User**: Non-root `appuser` for security
- **Health Check**: Built-in liveness probe via `/actuator/health`
- **Logging**: JSON logging driver with log rotation

---

## Swagger UI

Once the application is running, open:

```
http://localhost:8080/swagger-ui/index.html
```

The raw OpenAPI JSON is available at:

```
http://localhost:8080/api-docs
```

---

## API

### `POST /api/v1/send`

Accepts a JSON payload containing SMTP credentials and the email message. Returns **202 Accepted** immediately; delivery is asynchronous.

**Request body**

```json
{
  "smtpHost":     "smtp.gmail.com",
  "smtpPort":     587,
  "smtpUsername": "you@gmail.com",
  "smtpPassword": "your-app-password",
  "to":           "recipient@example.com",
  "from":         "you@gmail.com",
  "subject":      "Hello from the proxy",
  "text":         "Plain-text fallback body.",
  "html":         "<h1>Hello!</h1><p>HTML body.</p>",
  "icalEvent":    "BEGIN:VCALENDAR\nMETHOD:REQUEST\n...\nEND:VCALENDAR"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `smtpHost` | string | yes | Hostname of the SMTP server |
| `smtpPort` | integer | yes | 1–65535 |
| `smtpUsername` | string | yes | SMTP auth username |
| `smtpPassword` | string | yes | SMTP auth password / app password |
| `to` | string | yes | Valid email address |
| `from` | string | yes | Valid email address; must be authorised on the SMTP server |
| `subject` | string | yes | |
| `text` | string | yes | Plain-text body (always required as fallback) |
| `html` | string | no | If present, sends `multipart/alternative` |
| `icalEvent` | string | no | Raw `BEGIN:VCALENDAR` string; attached as `invite.ics` |

**Responses**

| Status | Meaning |
|---|---|
| 202 | Email accepted and queued for async delivery |
| 400 | Validation failed — response body contains per-field errors |

**Example — plain curl**

```bash
curl -s -o /dev/null -w "%{http_code}" \
  -X POST http://localhost:8080/api/v1/send \
  -H "Content-Type: application/json" \
  -d '{
    "smtpHost":     "smtp.gmail.com",
    "smtpPort":     587,
    "smtpUsername": "you@gmail.com",
    "smtpPassword": "your-app-password",
    "to":           "recipient@example.com",
    "from":         "you@gmail.com",
    "subject":      "Test",
    "text":         "Hello from the proxy"
  }'
# → 202
```

---

## Architecture

```
HTTP Client
    │
    │  POST /api/v1/send  (JSON body with SMTP credentials + email payload)
    ▼
EmailController          — validates request (@Valid), returns 202 immediately
    │
    │  emailService.sendEmail(request)   [non-blocking, @Async]
    ▼
EmailService             — runs on a Java 21 virtual thread
    │
    │  mailSenderFactory.createMailSender(request)
    ▼
MailSenderFactory        — builds a fresh JavaMailSenderImpl (stateless, no cache)
    │
    ▼
SMTP Server              — delivers the email
```

### Why Virtual Threads?

Traditional thread-pool servers block a platform thread during SMTP I/O (TCP handshake, AUTH, DATA transfer). With Virtual Threads (`spring.threads.virtual.enabled=true` + `Executors.newVirtualThreadPerTaskExecutor()` for `@Async`), the JVM parks the virtual thread during blocking calls and re-schedules it on a free carrier thread. This allows tens of thousands of simultaneous in-flight sends on a single JVM instance without exhausting OS resources.

### Stateless Design

No credentials are stored on the server. Each request is fully self-contained, making every node in a cluster identical — horizontal scaling requires nothing more than adding instances behind a load balancer.

---

## OS-Level Tuning for High Network Scale

Default OS settings will become the bottleneck long before the JVM does. Apply the following on Linux hosts running this service.

### 1. File Descriptor Limits

Each open TCP connection consumes one file descriptor. The default limit (often 1 024) is far too low.

```bash
# Temporary (current session)
ulimit -n 1048576

# Permanent — add to /etc/security/limits.conf
*    soft nofile 1048576
*    hard nofile 1048576

# For systemd services, add to the unit file:
# LimitNOFILE=1048576
```

### 2. TCP Connection Backlog

```bash
# Accept queue depth (connections waiting to be accepted by the app)
sysctl -w net.core.somaxconn=65535
sysctl -w net.ipv4.tcp_max_syn_backlog=65535
```

### 3. Ephemeral Port Range

Outbound SMTP connections each consume a local ephemeral port. Expand the range:

```bash
sysctl -w net.ipv4.ip_local_port_range="1024 65535"
```

### 4. TIME_WAIT Recycling

Under sustained traffic, `TIME_WAIT` sockets can exhaust the port range:

```bash
sysctl -w net.ipv4.tcp_tw_reuse=1
```

### 5. Socket Buffer Sizes

```bash
sysctl -w net.core.rmem_max=16777216
sysctl -w net.core.wmem_max=16777216
```

Make these permanent by adding them to `/etc/sysctl.conf` and running `sysctl -p`.

---

## JVM Tuning

For latency-sensitive, high-throughput deployments:

```bash
java \
  -XX:+UseZGC \
  -XX:MaxGCPauseMillis=10 \
  -Xms512m -Xmx2g \
  -jar target/rest-to-smtp-1.0.0.jar
```

| Flag | Purpose |
|---|---|
| `-XX:+UseZGC` | Z Garbage Collector — sub-millisecond GC pauses at any heap size |
| `-XX:MaxGCPauseMillis=10` | GC pause target |
| `-Xms` / `-Xmx` | Set min = max to avoid heap resize pauses |

---

## Configuration Reference

All settings in `application.yml` can be overridden at runtime via environment variables or `--` flags.

| Property | Default | Description |
|---|---|---|
| `server.port` | `8080` | HTTP listen port |
| `server.tomcat.max-connections` | `20000` | Max simultaneous TCP connections |
| `server.tomcat.accept-count` | `500` | OS-level TCP backlog |
| `server.tomcat.max-threads` | `1000` | Tomcat worker threads (virtual at runtime) |
| `spring.threads.virtual.enabled` | `true` | Enable Java 21 virtual threads for Tomcat |
| `logging.level.com.anushibinj.resttosmtp` | `DEBUG` | App log level |

---

## Spring Boot Actuator — Monitoring & Health Checks

The service exposes Spring Boot Actuator endpoints for health checks, metrics, and monitoring.

### Health Endpoints

Check overall service health:

```bash
curl http://localhost:8080/actuator/health
```

**Response:**

```json
{
  "status": "UP",
  "components": {
    "diskSpace": { "status": "UP", ... },
    "livenessState": { "status": "UP" },
    "readinessState": { "status": "UP" }
  }
}
```

Kubernetes-style probes:

```bash
# Liveness probe (is the service alive?)
curl http://localhost:8080/actuator/health/liveness

# Readiness probe (is the service ready to accept traffic?)
curl http://localhost:8080/actuator/health/readiness
```

### Metrics Endpoints

View all available metrics:

```bash
curl http://localhost:8080/actuator/metrics
```

Query specific metrics:

```bash
# Total email send requests
curl http://localhost:8080/actuator/metrics/email.send.requests.total

# Successful email dispatches
curl http://localhost:8080/actuator/metrics/email.dispatch.success.total

# Failed email dispatches
curl http://localhost:8080/actuator/metrics/email.dispatch.failure.total

# Email dispatch latency (in milliseconds)
curl http://localhost:8080/actuator/metrics/email.dispatch.latency

# Request processing time
curl http://localhost:8080/actuator/metrics/email.request.processing.time

# Validation errors
curl http://localhost:8080/actuator/metrics/email.validation.errors.total
```

**Example metric response:**

```json
{
  "name": "email.send.requests.total",
  "description": "Total number of email send requests received",
  "baseUnit": null,
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 42.0
    }
  ],
  "availableTags": [
    { "tag": "service", "values": ["rest-to-smtp"] }
  ]
}
```

### Application Info

View application version and metadata:

```bash
curl http://localhost:8080/actuator/info
```

### Exposed Endpoints

By default, the following endpoints are exposed:

| Endpoint | Purpose |
|---|---|
| `/actuator/health` | Overall service health |
| `/actuator/health/liveness` | Kubernetes liveness probe |
| `/actuator/health/readiness` | Kubernetes readiness probe |
| `/actuator/info` | Application metadata |
| `/actuator/metrics` | List of available metrics |
| `/actuator/metrics/{name}` | Query specific metric |
| `/actuator/prometheus` | Prometheus-format metrics (if Prometheus dependency is added) |

To modify exposed endpoints, edit `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

---

## Load Testing

Two load testing scripts are provided: **Apache JMeter** (advanced) and **curl-based** (simple).

### Quick Load Test (curl-based)

For rapid testing without dependencies:

```bash
# Make the script executable
chmod +x load-test-simple.sh

# Run 100 requests with 10 concurrent connections
./load-test-simple.sh 100 10

# Run 1000 requests with 50 concurrent connections
./load-test-simple.sh 1000 50
```

Results are saved to `load-test-results/curl_results_*.txt`.

**Sample output:**

```
==========================================
REST-to-SMTP Load Test (curl-based)
==========================================
Service URL:           http://localhost:8080/api/v1/send
Total Requests:        100
Concurrent Requests:   10
Results File:          load-test-results/curl_results_20240115_143022.txt
==========================================

Starting load test...

Results Summary:
  Successful (202):    98
  Failed:              2
  Success Rate:        98.00%
```

### Advanced Load Test (Apache JMeter)

For detailed performance analysis and metrics:

**Prerequisites:**

```bash
# Install Apache JMeter
# macOS
brew install jmeter

# Linux
apt-get install jmeter  # or similar for your distribution

# Windows
# Download from https://jmeter.apache.org/download_jmeter.cgi
```

**Run the load test:**

```bash
chmod +x load-test.sh

# 100 threads, 30s ramp-up, 60s test duration
./load-test.sh 100 30 60

# 500 threads, 60s ramp-up, 120s test duration (heavy load)
./load-test.sh 500 60 120
```

Results are saved to `load-test-results/results_*.jtl`.

**View results in JMeter GUI:**

```bash
jmeter -g load-test-results/results_20240115_143022.jtl
```

### Analyzing Results

**Check success rate from JMeter results:**

```bash
cat load-test-results/results_*.jtl | grep 'true' | wc -l   # successful
cat load-test-results/results_*.jtl | grep 'false' | wc -l  # failed
```

**Monitor metrics during load test:**

In a separate terminal, query metrics in real-time:

```bash
# Watch request counter
watch -n 1 'curl -s http://localhost:8080/actuator/metrics/email.send.requests.total | jq ".measurements[0].value"'

# Watch success rate
watch -n 1 'curl -s http://localhost:8080/actuator/metrics/email.dispatch.success.total | jq ".measurements[0].value"'

# Watch failure rate
watch -n 1 'curl -s http://localhost:8080/actuator/metrics/email.dispatch.failure.total | jq ".measurements[0].value"'
```

### Virtual Thread Performance

To benchmark virtual thread overhead and throughput:

```bash
mvn test -Dtest=VirtualThreadPerformanceBenchmark
```

JMH will measure:
- Thread creation latency
- Task submission overhead
- Fire-and-forget throughput
- High-concurrency queuing performance

---
