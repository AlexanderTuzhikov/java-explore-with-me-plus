package ru.practicum.statservice.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.NewEndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.statservice.service.StatService;

import java.util.List;

@RestController
@RequestMapping
@Validated
public class StatController {  // Убираем @RequiredArgsConstructor, делаем явный конструктор
    private final StatService statService;

    // Явный конструктор вместо @RequiredArgsConstructor
    public StatController(StatService statService) {
        this.statService = statService;
    }

    @PostMapping("/hit")
    public ResponseEntity<Void> hit(@Valid @RequestBody NewEndpointHitDto hitDto) {
        statService.saveHit(hitDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/stats")
    public ResponseEntity<List<ViewStatsDto>> getStats(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") boolean unique
    ) {
        return ResponseEntity.ok().body(statService.getStats(start, end, uris, unique));
    }

    @GetMapping("/")
    public ResponseEntity<String> root() {
        return ResponseEntity.ok("Stats Service is running");
    }

    // Добавляем эндпоинты для health check
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Stats Service OK");
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}
