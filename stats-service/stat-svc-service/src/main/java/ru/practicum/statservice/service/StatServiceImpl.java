package ru.practicum.statservice.service;

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
@Transactional(readOnly = true)
public class StatServiceImpl implements StatService {

    private final StatRepository repository;

    public StatServiceImpl(StatRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void saveHit(NewEndpointHitDto hitDto) {
        try {
            log.info("Saving hit: {}", hitDto);

            EndpointHit hit = new EndpointHit();
            hit.setApp(hitDto.getApp());
            hit.setUri(hitDto.getUri());
            hit.setIp(hitDto.getIp());
            hit.setTimestamp(hitDto.getTimestamp());

            repository.save(hit);
            log.info("Hit saved successfully");

        } catch (Exception e) {
            log.error("Failed to save hit: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка сохранения статистики: " + e.getMessage());
        }
    }

    @Override
    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, boolean unique) {
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
