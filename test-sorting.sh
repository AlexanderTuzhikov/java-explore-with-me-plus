echo "=== Testing Stats Sorting ==="
echo ""

echo "1. Adding test data..."
# Добавляем данные с разным количеством просмотров
URIS=("/high-traffic" "/medium-traffic" "/low-traffic")
for uri in "${URIS[@]}"; do
  # Для каждого URI разное количество хитов
  if [[ "$uri" == "/high-traffic" ]]; then hits=5; fi
  if [[ "$uri" == "/medium-traffic" ]]; then hits=3; fi
  if [[ "$uri" == "/low-traffic" ]]; then hits=1; fi
  
  for ((i=1; i<=hits; i++)); do
    curl -X POST http://localhost:9090/hit \
      -H "Content-Type: application/json" \
      -d "{\"app\":\"test-app\",\"uri\":\"$uri\",\"ip\":\"192.168.1.$i\",\"timestamp\":\"2026-01-21 10:$i:00\"}" \
      -s > /dev/null
  done
  echo "  Added $hits hits for $uri"
done

echo ""
echo "2. Getting statistics (should be sorted by hits descending):"
curl -s "http://localhost:9090/stats?start=2020-01-01%2000:00:00&end=2030-01-01%2000:00:00&unique=false"

echo ""
echo ""
echo "3. Expected order:"
echo "   - /high-traffic: 5 hits"
echo "   - /medium-traffic: 3 hits"
echo "   - /low-traffic: 1 hit"
