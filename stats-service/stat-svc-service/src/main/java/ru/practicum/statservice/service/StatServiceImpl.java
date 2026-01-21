package ru.practicum.statservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Service
@Transactional(readOnly = true)
public class StatServiceImpl implements StatService {
    private static final Logger log = LoggerFactory.getLogger(StatServiceImpl.class);

    private final StatRepository repository;
    private final EndpointHitMapper endpointHitMapper;

    // Конструктор
    public StatServiceImpl(StatRepository repository, EndpointHitMapper endpointHitMapper) {
        this.repository = repository;
        this.endpointHitMapper = endpointHitMapper;
    }

    @Override
    @Transactional
    public void saveHit(NewEndpointHitDto hitDto) {
        try {
            log.info("Сохранение hit: app={}, uri={}, ip={}, timestamp={}",
                    hitDto.getApp(), hitDto.getUri(), hitDto.getIp(), hitDto.getTimestamp());

            // Простой маппинг без Mapstruct
            EndpointHit hit = new EndpointHit();
            hit.setApp(hitDto.getApp());
            hit.setUri(hitDto.getUri());
            hit.setIp(hitDto.getIp());
            hit.setTimestamp(hitDto.getTimestamp());

            log.debug("Entity создана: {}", hit);

            EndpointHit saved = repository.save(hit);
            log.info("Успешно сохранено в БД с id={}", saved.getId());

        } catch (Exception e) {
            log.error("Ошибка при сохранении hit в БД: ", e);
            throw new RuntimeException("Ошибка сохранения статистики", e);
        }
    }

    @Override
    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, boolean unique) {
        log.info("Запрошена статистика: start={}, end={}, uris={}, unique={}",
                start, end, uris, unique);

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime startTime = LocalDateTime.parse(start, formatter);
            LocalDateTime endTime = LocalDateTime.parse(end, formatter);

            if (unique) {
                return repository.findUniqueHits(startTime, endTime, uris);
            } else {
                return repository.findAllHits(startTime, endTime, uris);
            }
        } catch (Exception e) {
            log.error("Ошибка при получении статистики: ", e);
            throw new RuntimeException("Ошибка получения статистики", e);
        }
    }
}
