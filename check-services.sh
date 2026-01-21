#!/bin/bash
set -e

echo "=== Starting all services ==="
docker-compose up -d

echo "=== Waiting for services to start ==="
sleep 30

echo "=== Checking stats-server container ==="
docker-compose ps

echo "=== Stats-server logs (last 50 lines) ==="
docker-compose logs stats-server --tail=50

echo "=== Testing connection to stats-server ==="
if curl -f http://localhost:9090/actuator/health; then
    echo "✓ stats-server is responding"
elif curl -f http://localhost:9090/ping; then
    echo "✓ stats-server ping endpoint responding"
elif curl -f http://localhost:9090; then
    echo "✓ stats-server root endpoint responding"
else
    echo "✗ stats-server not responding"
    echo "=== Full logs ==="
    docker-compose logs stats-server
    exit 1
fi

echo "=== All services started successfully ==="
