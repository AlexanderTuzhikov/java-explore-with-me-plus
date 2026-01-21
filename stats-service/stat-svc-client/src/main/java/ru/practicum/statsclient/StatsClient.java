package ru.practicum.statsclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.NewEndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * HTTP-клиент для взаимодействия с сервисом статистики.
 */
@Slf4j
@Component
public class StatsClient {
    private final RestTemplate restTemplate;
    private final String serverUrl;
    
    private static final String HIT_ENDPOINT = "/hit";
    private static final String STATS_ENDPOINT = "/stats";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatsClient(@Value("${stats-service.url:http://localhost:9090}") String serverUrl, 
                      RestTemplateBuilder builder) {
        this.serverUrl = serverUrl;
        this.restTemplate = builder.build();
        log.info("StatsClient initialized with server URL: {}", serverUrl);
    }

    /**
     * Сохраняет информацию о посещении эндпоинта.
     */
    public void saveHit(NewEndpointHitDto hitDto) {
        validateHitDto(hitDto);
        
        log.debug("Saving hit: app={}, uri={}, ip={}", 
                 hitDto.getApp(), hitDto.getUri(), hitDto.getIp());
        
        try {
            ResponseEntity<Object> response = restTemplate.postForEntity(
                    serverUrl + HIT_ENDPOINT, 
                    hitDto, 
                    Object.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("Hit successfully saved");
            } else {
                throw new StatsClientException("Failed to save hit, status: " + response.getStatusCode());
            }
        } catch (HttpStatusCodeException e) {
            log.error("HTTP error saving hit: {}", e.getStatusCode());
            throw new StatsClientException("HTTP error: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("Unexpected error saving hit: {}", e.getMessage());
            throw new StatsClientException("Failed to save hit", e);
        }
    }

    /**
     * Получает статистику посещений.
     */
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, 
                                      List<String> uris, boolean unique) {
        validateTimeRange(start, end);
        
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(serverUrl + STATS_ENDPOINT)
                .queryParam("start", start.format(FORMATTER))
                .queryParam("end", end.format(FORMATTER))
                .queryParam("unique", unique);

        if (uris != null && !uris.isEmpty()) {
            builder.queryParam("uris", String.join(",", uris));
        }

        String url = builder.toUriString();
        log.debug("Requesting stats from URL: {}", url);

        try {
            ResponseEntity<ViewStatsDto[]> response = restTemplate.getForEntity(
                    url, 
                    ViewStatsDto[].class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<ViewStatsDto> stats = Arrays.asList(response.getBody());
                log.debug("Received {} stats records", stats.size());
                return stats;
            } else {
                throw new StatsClientException("Failed to get stats, status: " + response.getStatusCode());
            }
        } catch (HttpStatusCodeException e) {
            log.error("HTTP error getting stats: {}", e.getStatusCode());
            throw new StatsClientException("HTTP error: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("Unexpected error getting stats: {}", e.getMessage());
            throw new StatsClientException("Failed to get stats", e);
        }
    }

    /**
     * Получает статистику посещений без фильтрации по URI.
     */
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, boolean unique) {
        return getStats(start, end, null, unique);
    }

    private void validateHitDto(NewEndpointHitDto hitDto) {
        if (hitDto == null) {
            throw new StatsClientException("Hit DTO cannot be null");
        }
        if (hitDto.getApp() == null || hitDto.getApp().isBlank()) {
            throw new StatsClientException("App cannot be null or blank");
        }
        if (hitDto.getUri() == null || hitDto.getUri().isBlank()) {
            throw new StatsClientException("URI cannot be null or blank");
        }
        if (hitDto.getIp() == null || hitDto.getIp().isBlank()) {
            throw new StatsClientException("IP cannot be null or blank");
        }
        if (hitDto.getTimestamp() == null) {
            throw new StatsClientException("Timestamp cannot be null");
        }
        if (hitDto.getTimestamp().isAfter(LocalDateTime.now())) {
            throw new StatsClientException("Timestamp cannot be in the future");
        }
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start == null) {
            throw new StatsClientException("Start time cannot be null");
        }
        if (end == null) {
            throw new StatsClientException("End time cannot be null");
        }
        if (end.isBefore(start)) {
            throw new StatsClientException("End time must be after start time");
        }
        if (start.isAfter(LocalDateTime.now())) {
            throw new StatsClientException("Start time cannot be in the future");
        }
    }
}
