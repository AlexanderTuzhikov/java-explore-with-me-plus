package ru.practicum.statservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.NewEndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.statservice.mapper.EndpointHitMapper;
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
    private final EndpointHitMapper endpointHitMapper;

    @Override
    @Transactional
    public void saveHit(NewEndpointHitDto hitDto) {
        // Используем правильные геттеры
        log.info("Получен HIT: app={}, uri={}, ip={}, timestamp={}",
                hitDto.getApp(), hitDto.getUri(), hitDto.getIp(), hitDto.getTimestamp());

        EndpointHit hit = endpointHitMapper.mapToEndpointHit(hitDto);
        repository.save(hit);
    }

    @Override
    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, boolean unique) {
        log.info("Запрошена статистика: start={}, end={}, uris={}, unique={}",
                start, end, uris, unique);

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
