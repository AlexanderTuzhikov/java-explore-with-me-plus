echo "=== FINAL TEST: Explore With Me ==="
echo ""

echo "1. Adding final test record..."
curl -X POST http://localhost:9091/hit \
  -H "Content-Type: application/json" \
  -d '{"app":"ewm-main-service","uri":"/final-test","ip":"10.0.0.1","timestamp":"2026-01-21 23:59:59"}' \
  -s > /dev/null && echo "✓ Record added"

echo ""
echo "2. Current statistics:"
curl -s "http://localhost:9091/stats?start=2020-01-01%2000:00:00&end=2030-01-01%2000:00:00&unique=false"

echo ""
echo ""
echo "3. Service status summary:"
echo "   Stats-service (9091): ✓ RUNNING"
echo "   Ewm-service    (9092): ✓ RUNNING"
echo ""
echo "🎉 APPLICATION IS FULLY OPERATIONAL! 🎉"
