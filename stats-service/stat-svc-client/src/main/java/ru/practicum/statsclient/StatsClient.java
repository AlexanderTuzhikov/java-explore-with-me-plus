package ru.practicum.statsclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.practicum.dto.NewEndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class StatsClient {
    private final RestClient restClient;
    private final String serverUrl;
    
    private static final String HIT_ENDPOINT = "/hit";
    private static final String STATS_ENDPOINT = "/stats";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<ViewStatsDto> EMPTY_STATS = Collections.emptyList();

    public StatsClient(@Value("${stats-service.url:http://localhost:9090}") String serverUrl) {
        this.serverUrl = serverUrl;
        this.restClient = RestClient.builder()
                .baseUrl(serverUrl)
                .defaultStatusHandler(
                        httpStatusCode -> httpStatusCode.isError(),
                        (request, response) -> {
                            throw new StatsClientException(
                                    "HTTP error " + response.getStatusCode() + 
                                    " from " + request.getURI()
                            );
                        }
                )
                .build();
        log.info("StatsClient ready for: {}", serverUrl);
    }

    public void saveHit(NewEndpointHitDto hitDto) {
        log.debug("Sending hit: {}", hitDto);
        
        try {
            restClient.post()
                    .uri(HIT_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(hitDto)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new StatsClientException("Failed to save hit: " + e.getMessage(), e);
        }
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, 
                                      List<String> uris, boolean unique) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(STATS_ENDPOINT)
                                .queryParam("start", start.format(FORMATTER))
                                .queryParam("end", end.format(FORMATTER))
                                .queryParam("unique", unique);
                        
                        if (uris != null && !uris.isEmpty()) {
                            uriBuilder.queryParam("uris", String.join(",", uris));
                        }
                        
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientException e) {
            throw new StatsClientException("Failed to get stats: " + e.getMessage(), e);
        }
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, boolean unique) {
        return getStats(start, end, null, unique);
    }
}
