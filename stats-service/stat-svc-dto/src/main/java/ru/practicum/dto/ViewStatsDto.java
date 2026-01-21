package ru.practicum.dto;

public class ViewStatsDto {
    private String app;
    private String uri;
    private Long hits;

    public ViewStatsDto(String app, String uri, Long hits) {
        this.app = app;
        this.uri = uri;
        this.hits = hits;
    }

    public ViewStatsDto() {
    }

    // Геттеры
    public String getApp() {
        return app;
    }

    public String getUri() {
        return uri;
    }

    public Long getHits() {
        return hits;
    }

    // Сеттеры
    public void setApp(String app) {
        this.app = app;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setHits(Long hits) {
        this.hits = hits;
    }
}
