package ru.practicum.statsclient.controller.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.practicum.statsclient.StatsClientException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice(basePackages = "ru.practicum.statsclient.controller")
public class StatsClientExceptionHandler {

    /**
     * Обработка ошибок валидации.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "BAD_REQUEST");
        response.put("reason", "Validation failed");
        response.put("message", "Invalid request parameters");
        response.put("timestamp", LocalDateTime.now());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        response.put("errors", errors);

        log.warn("Validation failed: {}", errors);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Обработка IllegalArgumentException (наша бизнес-валидация).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "BAD_REQUEST");
        response.put("reason", "Invalid request");
        response.put("message", ex.getMessage());
        response.put("timestamp", LocalDateTime.now());

        log.warn("Business validation failed: {}", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Обработка ошибок StatsClient.
     */
    @ExceptionHandler(StatsClientException.class)
    public ResponseEntity<Map<String, Object>> handleStatsClientException(StatsClientException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "BAD_GATEWAY");
        response.put("reason", "Statistics service unavailable");
        response.put("message", ex.getMessage());
        response.put("timestamp", LocalDateTime.now());

        log.error("Stats service error: {}", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_GATEWAY);
    }

    /**
     * Обработка всех остальных исключений.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "INTERNAL_SERVER_ERROR");
        response.put("reason", "Internal server error");
        response.put("message", "An unexpected error occurred");
        response.put("timestamp", LocalDateTime.now());

        log.error("Internal error: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
