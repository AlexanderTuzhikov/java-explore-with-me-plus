FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/stats-service-app.jar app.jar
EXPOSE 9090

RUN echo '#!/bin/sh' > /entrypoint.sh && \
    echo 'echo "=== Starting Stats Server ==="' >> /entrypoint.sh && \
    echo 'echo "Working directory: $(pwd)"' >> /entrypoint.sh && \
    echo 'echo "Files in /app:"' >> /entrypoint.sh && \
    echo 'ls -la' >> /entrypoint.sh && \
    echo 'echo "Java version:"' >> /entrypoint.sh && \
    echo 'java -version' >> /entrypoint.sh && \
    echo 'echo "=== Launching Application ==="' >> /entrypoint.sh && \
    echo 'exec java -jar app.jar' >> /entrypoint.sh && \
    chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
