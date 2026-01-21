package ru.practicum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDateTime;

public class NewEndpointHitDto {
    @NotBlank(message = "app is blank")
    private String app;

    @NotBlank(message = "uri is blank")
    private String uri;

    @NotBlank(message = "ip is blank")
    private String ip;

    @PastOrPresent(message = "timestamp is future")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    public NewEndpointHitDto(String app, String uri, String ip, LocalDateTime timestamp) {
        this.app = app;
        this.uri = uri;
        this.ip = ip;
        this.timestamp = timestamp;
    }

    public NewEndpointHitDto() {
    }

    // Геттеры
    public String getApp() {
        return app;
    }

    public String getUri() {
        return uri;
    }

    public String getIp() {
        return ip;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    // Сеттеры
    public void setApp(String app) {
        this.app = app;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
