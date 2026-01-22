#!/bin/bash

echo "=== Explore With Me - Service Status ==="
echo ""

echo "1. Stats-Service (port 9091):"
echo "-----------------------------"
STATS_HEALTH=$(curl -s http://localhost:9091/actuator/health)
if echo "$STATS_HEALTH" | grep -q "UP"; then
    echo "✓ RUNNING - Status: UP"
    echo "  Data count: $(curl -s "http://localhost:9091/stats?start=2020-01-01%2000:00:00&end=2030-01-01%2000:00:00&unique=false" | grep -o '"uri"' | wc -l) records"
else
    echo "✗ NOT RUNNING"
fi
echo ""

echo "2. EWM-Service (port 9092):"
echo "---------------------------"
EWM_HEALTH=$(curl -s http://localhost:9092/actuator/health 2>/dev/null)
if echo "$EWM_HEALTH" | grep -q "UP"; then
    echo "✓ RUNNING - Status: UP"
    # Проверка базового эндпоинта
    curl -s http://localhost:9092/ 2>/dev/null && echo "  Root endpoint: OK" || echo "  Root endpoint: Not configured"
else
    echo "✗ NOT RUNNING on port 9092"
    echo "  Trying other ports..."
    for port in 8080 9090 9093 9094; do
        if curl -s --connect-timeout 1 http://localhost:$port/actuator/health 2>/dev/null | grep -q "UP"; then
            echo "  Found on port: $port"
            break
        fi
    done
fi
echo ""

echo "3. Integration Test:"
echo "-------------------"
echo "Adding test record to stats-service..."
curl -X POST http://localhost:9091/hit \
  -H "Content-Type: application/json" \
  -d '{"app":"ewm-main-service","uri":"/test-integration","ip":"192.168.1.99","timestamp":"2026-01-21 12:50:00"}' \
  -s > /dev/null && echo "✓ Record added successfully"
echo ""

echo "4. Final Statistics:"
echo "-------------------"
curl -s "http://localhost:9091/stats?start=2020-01-01%2000:00:00&end=2030-01-01%2000:00:00&unique=false" | python -m json.tool 2>/dev/null || curl -s "http://localhost:9091/stats?start=2020-01-01%2000:00:00&end=2030-01-01%2000:00:00&unique=false"
