package ru.practicum.statsclient;

import lombok.Getter;

@Getter
public class StatsClientException extends RuntimeException {
    private final String endpoint;
    private final String method;
    private final Integer statusCode;

    public StatsClientException(String message) {
        super(message);
        this.endpoint = null;
        this.method = null;
        this.statusCode = null;
    }

    public StatsClientException(String message, Throwable cause) {
        super(message, cause);
        this.endpoint = null;
        this.method = null;
        this.statusCode = null;
    }

    public StatsClientException(String message, String endpoint, String method, Integer statusCode) {
        super(message);
        this.endpoint = endpoint;
        this.method = method;
        this.statusCode = statusCode;
    }
}
