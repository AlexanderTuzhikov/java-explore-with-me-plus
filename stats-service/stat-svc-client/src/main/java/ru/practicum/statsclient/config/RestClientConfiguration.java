package ru.practicum.statsclient.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.practicum.statsclient.StatsClientException;

import java.time.Duration;

@Configuration
public class RestClientConfiguration {

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .requestInterceptor((request, body, execution) -> {
                    // Логирование запросов
                    System.out.println("Making request to: " + request.getURI());
                    return execution.execute(request, body);
                })
                .defaultStatusHandler(
                        status -> status.is5xxServerError(),
                        (request, response) -> {
                            throw new StatsClientException("Server error: " + response.getStatusCode());
                        }
                )
                .defaultStatusHandler(
                        status -> status.is4xxClientError(),
                        (request, response) -> {
                            throw new StatsClientException("Client error: " + response.getStatusCode());
                        }
                )
                .requestFactory(new BufferingClientHttpRequestFactory(
                        new SimpleClientHttpRequestFactory()
                ))
                .build();
    }

    // Вспомогательный класс для буферизации запросов
    private static class BufferingClientHttpRequestFactory
            extends org.springframework.http.client.BufferingClientHttpRequestFactory {
        public BufferingClientHttpRequestFactory(org.springframework.http.client.ClientHttpRequestFactory requestFactory) {
            super(requestFactory);
        }
    }
}
