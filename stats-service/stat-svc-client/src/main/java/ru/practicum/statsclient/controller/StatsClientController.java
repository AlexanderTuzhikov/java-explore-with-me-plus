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

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/stats")
public class StatsClientController {
    
    private final StatsClient statsClient;
    
    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Void> saveHit(@Valid @RequestBody NewEndpointHitDto hitDto) {
        log.info("StatsClient: Saving hit - {}", hitDto);
        
        ResponseEntity<Void> response = statsClient.saveHit(hitDto);
        
        if (response.getStatusCode() == HttpStatus.CREATED) {
            log.debug("Hit saved successfully via stats-service");
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    
    @GetMapping
    public List<ViewStatsDto> getStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") boolean unique) {
        
        log.info("StatsClient: Getting stats from {} to {}, uris: {}, unique: {}", 
                start, end, uris, unique);
        
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        
        List<ViewStatsDto> stats = statsClient.getStats(start, end, uris, unique);
        
        log.debug("StatsClient: Retrieved {} records", stats.size());
        return stats;
    }
}
