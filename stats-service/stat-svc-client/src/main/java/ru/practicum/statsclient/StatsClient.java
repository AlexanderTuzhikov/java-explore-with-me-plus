package ru.practicum.statsclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.practicum.dto.NewEndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
public class StatsClient {
    private final RestClient restClient;
    private final String serverUrl;

    private static final String HIT_ENDPOINT = "/hit";
    private static final String STATS_ENDPOINT = "/stats";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatsClient(@Value("${stats-service.url:http://localhost:9090}") String serverUrl) {
        this.serverUrl = serverUrl;
        this.restClient = RestClient.builder()
                .baseUrl(serverUrl)
                .defaultStatusHandler(
                        HttpStatusCode::is4xxClientError,
                        (request, response) -> {
                            throw new StatsClientException(
                                    "Client error: " + response.getStatusCode() +
                                            " - " + request.getURI()
                            );
                        }
                )
                .defaultStatusHandler(
                        HttpStatusCode::is5xxServerError,
                        (request, response) -> {
                            throw new StatsClientException(
                                    "Server error: " + response.getStatusCode() +
                                            " - " + request.getURI()
                            );
                        }
                )
                .build();
        log.info("StatsClient initialized for: {}", serverUrl);
    }

    /**
     * Сохраняет информацию о посещении.
     * Возвращает ResponseEntity<Void> со статусом 201 от сервера.
     */
    public ResponseEntity<Void> saveHit(NewEndpointHitDto hitDto) {
        log.debug("Sending hit to stats-service: {}", hitDto);

        try {
            return restClient.post()
                    .uri(HIT_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(hitDto)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new StatsClientException("Failed to save hit: " + e.getMessage(), e);
        }
    }

    /**
     * Получает статистику посещений.
     * Параметры даты кодируются согласно спецификации.
     */
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                       List<String> uris, boolean unique) {
        log.debug("Getting stats: {} to {}, uris: {}, unique: {}", start, end, uris, unique);

        try {
            return restClient.get()
                    .uri(uriBuilder -> {
                        // Кодируем даты как указано в спецификации
                        String encodedStart = URLEncoder.encode(
                                start.format(FORMATTER),
                                StandardCharsets.UTF_8
                        );
                        String encodedEnd = URLEncoder.encode(
                                end.format(FORMATTER),
                                StandardCharsets.UTF_8
                        );

                        uriBuilder.path(STATS_ENDPOINT)
                                .queryParam("start", encodedStart)
                                .queryParam("end", encodedEnd)
                                .queryParam("unique", unique);

                        if (uris != null && !uris.isEmpty()) {
                            // URI тоже кодируем
                            String encodedUris = URLEncoder.encode(
                                    String.join(",", uris),
                                    StandardCharsets.UTF_8
                            );
                            uriBuilder.queryParam("uris", encodedUris);
                        }

                        return uriBuilder.build();
                    })
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            throw new StatsClientException("Failed to get stats: " + e.getMessage(), e);
        }
    }

    /**
     * Получает статистику посещений без фильтрации по URI.
     */
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, boolean unique) {
        return getStats(start, end, null, unique);
    }
}
