package ru.practicum.statsclient.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.NewEndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.statsclient.StatsClient;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Контроллер для работы со статистикой.
 * Предоставляет API для основного сервиса.
 */
@Slf4j
@Validated  // Активируем валидацию параметров методов
@RestController
@RequiredArgsConstructor
@RequestMapping("/stats")
public class StatsClientController {

    private final StatsClient statsClient;

    /**
     * Сохраняет информацию о посещении эндпоинта.
     * Валидация DTO происходит автоматически благодаря @Valid.
     *
     * @param hitDto DTO с информацией о посещении
     * @return ResponseEntity со статусом 201 (CREATED)
     */
    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Void> saveHit(@Valid @RequestBody NewEndpointHitDto hitDto) {
        log.info("StatsClient: Saving hit - app: {}, uri: {}, ip: {}",
                hitDto.getApp(), hitDto.getUri(), hitDto.getIp());

        // Дополнительная бизнес-валидация
        validateHitDto(hitDto);

        ResponseEntity<Void> response = statsClient.saveHit(hitDto);

        if (response.getStatusCode() == HttpStatus.CREATED) {
            log.debug("Hit saved successfully via stats-service");
        }

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Получает статистику посещений.
     *
     * @param start  Дата и время начала диапазона
     * @param end    Дата и время окончания диапазона
     * @param uris   Список URI для фильтрации (опционально)
     * @param unique Учитывать только уникальные посещения
     * @return Список статистики
     */
    @GetMapping
    public List<ViewStatsDto> getStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") boolean unique) {

        log.info("StatsClient: Getting stats from {} to {}, uris: {}, unique: {}",
                start, end, uris, unique);

        // Валидация временного диапазона
        validateTimeRange(start, end);

        List<ViewStatsDto> stats = statsClient.getStats(start, end, uris, unique);

        log.debug("StatsClient: Retrieved {} records", stats.size());
        return stats;
    }

    /**
     * Дополнительная бизнес-валидация DTO.
     */
    private void validateHitDto(NewEndpointHitDto hitDto) {
        if (hitDto.getTimestamp().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Timestamp cannot be in the future");
        }

        // Можно добавить другие проверки
        if (hitDto.getApp().length() > 100) {
            throw new IllegalArgumentException("App name too long (max 100 characters)");
        }

        if (hitDto.getUri().length() > 500) {
            throw new IllegalArgumentException("URI too long (max 500 characters)");
        }
    }

    /**
     * Валидация временного диапазона.
     */
    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        // Максимальный диапазон - 1 год
        if (start.plusYears(1).isBefore(end)) {
            throw new IllegalArgumentException("Time range cannot exceed 1 year");
        }

        if (start.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Start time cannot be in the future");
        }
    }
}
