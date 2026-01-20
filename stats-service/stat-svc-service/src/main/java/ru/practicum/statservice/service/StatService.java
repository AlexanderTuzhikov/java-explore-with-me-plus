package ru.practicum.statservice.service;

import ru.practicum.dto.NewEndpointHitDto;

public interface StatService {
    void saveHit(NewEndpointHitDto hitDto);
}
