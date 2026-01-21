FROM alpine:latest

# Устанавливаем netcat
RUN apk add --no-cache netcat-openbsd

# Создаем рабочую директорию
RUN mkdir -p /app
WORKDIR /app

# Создаем скрипт health check
RUN echo '#!/bin/sh' > health-server.sh && \
    echo 'echo "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n\r\n{\"status\":\"UP\"}" > /tmp/response' >> health-server.sh && \
    echo 'while true; do' >> health-server.sh && \
    echo '  cat /tmp/response | nc -l -p 9090' >> health-server.sh && \
    echo 'done' >> health-server.sh && \
    chmod +x health-server.sh

EXPOSE 9090

CMD ["/app/health-server.sh"]
