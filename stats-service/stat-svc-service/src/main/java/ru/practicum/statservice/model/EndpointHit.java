package ru.practicum.statservice.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "endpoint_hits")
public class EndpointHit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "app", nullable = false)
    private String app;

    @Column(name = "uri", nullable = false, length = 2048)
    private String uri;

    @Column(name = "ip", nullable = false, length = 45)
    private String ip;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    public EndpointHit() {
    }

    public EndpointHit(Long id, String app, String uri, String ip, LocalDateTime timestamp) {
        this.id = id;
        this.app = app;
        this.uri = uri;
        this.ip = ip;
        this.timestamp = timestamp;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getApp() { return app; }
    public void setApp(String app) { this.app = app; }

    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
