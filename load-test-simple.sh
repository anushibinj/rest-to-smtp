#!/bin/bash
#
# Simple curl-based load test for REST-to-SMTP proxy microservice
#
# This script generates concurrent email send requests using curl.
# Useful for quick testing without installing Apache JMeter.
#
# Prerequisites:
#   - curl installed
#   - REST-to-SMTP service running on localhost:8080
#
# Usage:
#   chmod +x load-test-simple.sh
#   ./load-test-simple.sh [num_requests] [concurrent_requests]
#
# Examples:
#   ./load-test-simple.sh 100 10       # 100 requests, 10 concurrent
#   ./load-test-simple.sh 1000 50      # 1000 requests, 50 concurrent
#

set -e

NUM_REQUESTS=${1:-100}
CONCURRENT_REQUESTS=${2:-10}
SERVICE_HOST="${SERVICE_HOST:-localhost}"
SERVICE_PORT="${SERVICE_PORT:-8080}"
ENDPOINT_URL="http://${SERVICE_HOST}:${SERVICE_PORT}/api/v1/send"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="${SCRIPT_DIR}/load-test-results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RESULTS_FILE="${RESULTS_DIR}/curl_results_${TIMESTAMP}.txt"

# Create results directory
mkdir -p "$RESULTS_DIR"

# Check if curl is installed
if ! command -v curl &> /dev/null; then
    echo "ERROR: curl is not installed"
    exit 1
fi

echo "=========================================="
echo "REST-to-SMTP Load Test (curl-based)"
echo "=========================================="
echo "Service URL:           $ENDPOINT_URL"
echo "Total Requests:        $NUM_REQUESTS"
echo "Concurrent Requests:   $CONCURRENT_REQUESTS"
echo "Results File:          $RESULTS_FILE"
echo "=========================================="
echo ""

# Create test payload
PAYLOAD='{
  "smtpHost": "smtp.gmail.com",
  "smtpPort": 587,
  "smtpUsername": "test@example.com",
  "smtpPassword": "testpassword",
  "from": "sender@example.com",
  "to": "recipient@example.com",
  "subject": "Load Test Email",
  "text": "This is a load test email"
}'

# Initialize counters
SUCCESS_COUNT=0
FAILURE_COUNT=0
START_TIME=$(date +%s)

echo "Starting load test..."
echo ""

# Function to send a request
send_request() {
    local request_num=$1
    local http_code=$(curl -s -w "%{http_code}" -o /dev/null \
        -X POST "$ENDPOINT_URL" \
        -H "Content-Type: application/json" \
        -d "$PAYLOAD" \
        --connect-timeout 5 \
        --max-time 10)
    
    if [ "$http_code" = "202" ]; then
        echo "[$(date +'%H:%M:%S')] Request $request_num: SUCCESS (HTTP 202)"
        ((SUCCESS_COUNT++))
    else
        echo "[$(date +'%H:%M:%S')] Request $request_num: FAILED (HTTP $http_code)"
        ((FAILURE_COUNT++))
    fi
}

export -f send_request
export SUCCESS_COUNT FAILURE_COUNT ENDPOINT_URL PAYLOAD

# Send requests using GNU parallel or sequential bash
if command -v parallel &> /dev/null; then
    # Use GNU parallel if available for true concurrency
    seq 1 "$NUM_REQUESTS" | parallel -j "$CONCURRENT_REQUESTS" send_request >> "$RESULTS_FILE" 2>&1
else
    # Fallback to xargs or sequential loop
    if command -v xargs &> /dev/null; then
        # Use xargs for concurrency
        seq 1 "$NUM_REQUESTS" | xargs -P "$CONCURRENT_REQUESTS" -I {} bash -c "send_request {}" >> "$RESULTS_FILE" 2>&1
    else
        # Sequential fallback
        echo "Warning: GNU parallel not found, running sequentially"
        for i in $(seq 1 "$NUM_REQUESTS"); do
            send_request "$i" >> "$RESULTS_FILE" 2>&1
        done
    fi
fi

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo ""
echo "=========================================="
echo "Load Test Complete"
echo "=========================================="
echo "Total Requests:      $NUM_REQUESTS"
echo "Duration:            ${DURATION}s"
echo "Requests/sec:        $(echo "scale=2; $NUM_REQUESTS / $DURATION" | bc)"
echo "Results saved to:    $RESULTS_FILE"
echo ""

# Check results
SUCCESS_COUNT=$(grep -c "SUCCESS" "$RESULTS_FILE" 2>/dev/null || echo 0)
FAILURE_COUNT=$(grep -c "FAILED" "$RESULTS_FILE" 2>/dev/null || echo 0)

echo "Results Summary:"
echo "  Successful (202):    $SUCCESS_COUNT"
echo "  Failed:              $FAILURE_COUNT"
echo "  Success Rate:        $(echo "scale=2; ($SUCCESS_COUNT * 100) / $NUM_REQUESTS" | bc)%"
echo ""
