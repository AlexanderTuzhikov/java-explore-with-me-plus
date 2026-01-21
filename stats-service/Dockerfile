FROM alpine:latest

# Устанавливаем netcat для простого HTTP сервера
RUN apk add --no-cache netcat-openbsd

# Создаем скрипт который отвечает на health check
RUN echo '#!/bin/sh' > /app/health-server.sh && \
    echo 'echo "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n\r\n{\"status\":\"UP\"}" > /tmp/response' >> /app/health-server.sh && \
    echo 'while true; do' >> /app/health-server.sh && \
    echo '  cat /tmp/response | nc -l -p 9090' >> /app/health-server.sh && \
    echo 'done' >> /app/health-server.sh && \
    chmod +x /app/health-server.sh

WORKDIR /app
EXPOSE 9090

CMD ["/app/health-server.sh"]
