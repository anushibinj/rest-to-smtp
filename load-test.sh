#!/bin/bash
#
# Load test script for REST-to-SMTP proxy microservice
#
# This script uses Apache JMeter to generate load against the REST-to-SMTP endpoint.
# It simulates concurrent email send requests and collects performance metrics.
#
# Prerequisites:
#   - Apache JMeter installed (https://jmeter.apache.org/)
#   - REST-to-SMTP service running on localhost:8080
#   - jmeter command in PATH
#
# Usage:
#   chmod +x load-test.sh
#   ./load-test.sh [num_threads] [ramp_up_time] [duration_seconds]
#
# Examples:
#   ./load-test.sh 100 30 60        # 100 threads, 30s ramp-up, 60s test duration
#   ./load-test.sh 500 60 120       # 500 threads, 60s ramp-up, 120s test duration
#

set -e

# Configuration
NUM_THREADS=${1:-100}
RAMP_UP_SECONDS=${2:-30}
TEST_DURATION_SECONDS=${3:-60}
SERVICE_HOST="${SERVICE_HOST:-localhost}"
SERVICE_PORT="${SERVICE_PORT:-8080}"
ENDPOINT_URL="http://${SERVICE_HOST}:${SERVICE_PORT}/api/v1/send"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="${SCRIPT_DIR}/load-test-results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
TEST_PLAN="${RESULTS_DIR}/test-plan_${TIMESTAMP}.jmx"
RESULTS_FILE="${RESULTS_DIR}/results_${TIMESTAMP}.jtl"

# Create results directory
mkdir -p "$RESULTS_DIR"

echo "=========================================="
echo "REST-to-SMTP Load Test Configuration"
echo "=========================================="
echo "Service URL:         $ENDPOINT_URL"
echo "Thread Count:        $NUM_THREADS"
echo "Ramp-up Time:        ${RAMP_UP_SECONDS}s"
echo "Test Duration:       ${TEST_DURATION_SECONDS}s"
echo "Results Directory:   $RESULTS_DIR"
echo "Test Plan:           $TEST_PLAN"
echo "Results File:        $RESULTS_FILE"
echo "=========================================="
echo ""

# Create JMeter test plan
cat > "$TEST_PLAN" << 'JMETER_PLAN'
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2" properties="5.0" jmeter="5.6.3">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="REST-to-SMTP Load Test" enabled="true">
      <elementProp name="TestPlan.user_defined_variables" elementType="Arguments" guiclass="ArgumentsPanel" testclass="Arguments" testname="User Defined Variables" enabled="true">
        <collectionProp name="Arguments.arguments"/>
      </elementProp>
      <stringProp name="TestPlan.user_define_classpath"></stringProp>
      <boolProp name="TestPlan.functional_mode">false</boolProp>
      <boolProp name="TestPlan.serialize_threadgroups">false</boolProp>
      <elementProp name="TestPlan.variable_list" elementType="Arguments" guiclass="ArgumentsPanel" testclass="Arguments" testname="Variables" enabled="true">
        <collectionProp name="Arguments.arguments">
          <elementProp name="SERVICE_URL" elementType="Argument">
            <stringProp name="Argument.name">SERVICE_URL</stringProp>
            <stringProp name="Argument.value">http://localhost:8080/api/v1/send</stringProp>
            <stringProp name="Argument.desc">Target service endpoint</stringProp>
          </elementProp>
        </collectionProp>
      </elementProp>
      <stringProp name="TestPlan.comments">Load test for REST-to-SMTP proxy - simulates concurrent email send requests</stringProp>
    </TestPlan>
    <hashTree>
      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="Email Send Thread Group" enabled="true">
        <elementProp name="ThreadGroup.main_controller" elementType="LoopController" guiclass="LoopControlPanel" testclass="LoopController" testname="Loop Controller" enabled="true">
          <boolProp name="LoopController.continue_forever">false</boolProp>
          <stringProp name="LoopController.loops">-1</stringProp>
        </elementProp>
        <stringProp name="ThreadGroup.num_threads">${NUM_THREADS}</stringProp>
        <stringProp name="ThreadGroup.ramp_time">${RAMP_UP_SECONDS}</stringProp>
        <boolProp name="ThreadGroup.scheduler">true</boolProp>
        <stringProp name="ThreadGroup.duration">${TEST_DURATION_SECONDS}</stringProp>
        <stringProp name="ThreadGroup.delay"></stringProp>
        <boolProp name="ThreadGroup.same_user_on_next_iteration">true</boolProp>
      </ThreadGroup>
      <hashTree>
        <HTTPSampler guiclass="HttpTestSampleGui" testclass="HTTPSampler" testname="POST /api/v1/send" enabled="true">
          <elementProp name="HTTPsampler.Arguments" elementType="Arguments" guiclass="HTTPArgumentsPanel" testclass="Arguments" testname="User Defined Variables" enabled="true">
            <collectionProp name="Arguments.arguments"/>
          </elementProp>
          <stringProp name="HTTPSampler.domain">${SERVICE_HOST}</stringProp>
          <stringProp name="HTTPSampler.port">${SERVICE_PORT}</stringProp>
          <stringProp name="HTTPSampler.protocol">http</stringProp>
          <stringProp name="HTTPSampler.contentEncoding"></stringProp>
          <stringProp name="HTTPSampler.path">/api/v1/send</stringProp>
          <stringProp name="HTTPSampler.method">POST</stringProp>
          <boolProp name="HTTPSampler.follow_redirects">true</boolProp>
          <boolProp name="HTTPSampler.auto_redirects">false</boolProp>
          <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
          <boolProp name="HTTPSampler.DO_MULTIPART_POST">false</boolProp>
          <stringProp name="HTTPSampler.embedded_url_re"></stringProp>
          <stringProp name="HTTPSampler.connect_timeout"></stringProp>
          <stringProp name="HTTPSampler.response_timeout"></stringProp>
        </HTTPSampler>
        <hashTree>
          <HeaderManager guiclass="HeaderPanel" testclass="HeaderManager" testname="HTTP Header Manager" enabled="true">
            <collectionProp name="HeaderManager.headers">
              <elementProp name="" elementType="Header">
                <stringProp name="Header.name">Content-Type</stringProp>
                <stringProp name="Header.value">application/json</stringProp>
              </elementProp>
            </collectionProp>
          </HeaderManager>
          <hashTree/>
          <BodyElement guiclass="BodyElementPanel" testclass="BodyElement" testname="Body Data" enabled="true">
            <stringProp name="BodyElement.body">{
  "smtpHost": "smtp.gmail.com",
  "smtpPort": 587,
  "smtpUsername": "test@example.com",
  "smtpPassword": "testpassword",
  "from": "sender@example.com",
  "to": "recipient@example.com",
  "subject": "Load Test Email",
  "text": "This is a load test email from thread ${__threadNum} at ${__time(HH:mm:ss)}"
}</stringProp>
          </BodyElement>
          <hashTree/>
        </hashTree>
        <ResponseAssertion guiclass="AssertionGui" testclass="ResponseAssertion" testname="Response Assertion" enabled="true">
          <collectionProp name="Asserions">
            <elementProp name="" elementType="ResponseAssertion">
              <boolProp name="Assertion.assume_success">false</boolProp>
              <intProp name="Assertion.test_type">1</intProp>
              <stringProp name="Assertion.test_strings">202</stringProp>
              <stringProp name="Assertion.test_pattern"></stringProp>
              <stringProp name="Assertion.test_type_string">status</stringProp>
              <boolProp name="Assertion.test_across_all">false</boolProp>
              <boolProp name="Assertion.assume_success">false</boolProp>
            </elementProp>
          </collectionProp>
          <stringProp name="Asserions"></stringProp>
        </ResponseAssertion>
        <hashTree/>
        <ResultCollector guiclass="SimpleDataWriter" testclass="ResultCollector" testname="Simple Data Writer" enabled="true">
          <elementProp name="collectorProperties" elementType="CollectionProp">
            <stringProp name="filename">${RESULTS_FILE}</stringProp>
            <stringProp name="variableNames"></stringProp>
            <stringProp name="print_first_datafile"></stringProp>
            <boolProp name="print_headers">true</boolProp>
            <boolProp name="save_as_xml">true</boolProp>
            <boolProp name="csv_print_headers">true</boolProp>
            <boolProp name="csv_print_headers_once">false</boolProp>
            <stringProp name="csv_delimiter">,</stringProp>
            <stringProp name="csv_print_headers_once">false</stringProp>
            <boolProp name="ignore_scheduling">false</boolProp>
            <stringProp name="formatVersion">2.1</stringProp>
          </elementProp>
        </ResultCollector>
        <hashTree/>
      </hashTree>
    </hashTree>
    <ResultCollector guiclass="TableVisualizer" testclass="ResultCollector" testname="View Results Table" enabled="true">
      <elementProp name="collectorProperties" elementType="CollectionProp">
        <stringProp name="filename"></stringProp>
        <stringProp name="variableNames"></stringProp>
        <stringProp name="print_first_datafile"></stringProp>
        <boolProp name="print_headers">true</boolProp>
        <boolProp name="save_as_xml">false</boolProp>
        <boolProp name="csv_print_headers">true</boolProp>
        <boolProp name="csv_print_headers_once">false</boolProp>
        <stringProp name="csv_delimiter">,</stringProp>
        <stringProp name="csv_print_headers_once">false</stringProp>
        <boolProp name="ignore_scheduling">false</boolProp>
        <stringProp name="formatVersion">2.1</stringProp>
      </elementProp>
    </ResultCollector>
    <hashTree/>
  </hashTree>
</jmeterTestPlan>
JMETER_PLAN

# Update variables in test plan
sed -i "s|\${NUM_THREADS}|$NUM_THREADS|g" "$TEST_PLAN"
sed -i "s|\${RAMP_UP_SECONDS}|$RAMP_UP_SECONDS|g" "$TEST_PLAN"
sed -i "s|\${TEST_DURATION_SECONDS}|$TEST_DURATION_SECONDS|g" "$TEST_PLAN"
sed -i "s|\${RESULTS_FILE}|$RESULTS_FILE|g" "$TEST_PLAN"
sed -i "s|SERVICE_HOST|$SERVICE_HOST|g" "$TEST_PLAN"
sed -i "s|SERVICE_PORT|$SERVICE_PORT|g" "$TEST_PLAN"

# Check if jmeter is installed
if ! command -v jmeter &> /dev/null; then
    echo "ERROR: Apache JMeter is not installed or not in PATH"
    echo "Install JMeter from: https://jmeter.apache.org/download_jmeter.cgi"
    exit 1
fi

# Run the test
echo "Starting load test..."
echo ""

jmeter -n -t "$TEST_PLAN" -l "$RESULTS_FILE" -j "${RESULTS_DIR}/jmeter_${TIMESTAMP}.log"

echo ""
echo "=========================================="
echo "Load Test Complete"
echo "=========================================="
echo "Results saved to: $RESULTS_FILE"
echo ""
echo "To view results:"
echo "  jmeter -g $RESULTS_FILE"
echo ""
echo "To analyze results programmatically:"
echo "  cat $RESULTS_FILE | grep -c 'true' # successful requests"
echo "  cat $RESULTS_FILE | grep -c 'false' # failed requests"
echo ""
