package ru.practicum.statservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.NewEndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.statservice.model.EndpointHit;
import ru.practicum.statservice.repository.StatRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatServiceImpl implements StatService {
    
    private final StatRepository repository;
    
    @Override
    @Transactional
    public void saveHit(NewEndpointHitDto hitDto) {
        try {
            log.info("Attempting to save hit: app={}, uri={}", hitDto.getApp(), hitDto.getUri());
            
            EndpointHit hit = new EndpointHit();
            hit.setApp(hitDto.getApp());
            hit.setUri(hitDto.getUri());
            hit.setIp(hitDto.getIp());
            hit.setTimestamp(hitDto.getTimestamp());
            
            EndpointHit saved = repository.save(hit);
            log.info("Successfully saved hit with id={}", saved.getId());
            
        } catch (Exception e) {
            log.error("Error saving hit: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка сохранения статистики: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, boolean unique) {
        log.info("Getting stats: start={}, end={}, uris={}, unique={}", start, end, uris, unique);
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime startTime = LocalDateTime.parse(start, formatter);
        LocalDateTime endTime = LocalDateTime.parse(end, formatter);
        
        if (unique) {
            return repository.findUniqueHits(startTime, endTime, uris);
        } else {
            return repository.findAllHits(startTime, endTime, uris);
        }
    }
}
