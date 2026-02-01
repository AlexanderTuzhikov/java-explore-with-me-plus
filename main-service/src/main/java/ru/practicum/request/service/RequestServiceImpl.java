package ru.practicum.request.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.handler.exception.ConflictException;
import ru.practicum.handler.exception.NotFoundException;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.dto.RequestStatusUpdateDto;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestState;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;

    @Override
    public List<ParticipationRequestDto> getRequests(Long userId) {
        log.info("GET requests: user ID={}", userId);

        checkUserExists(userId);
        List<Request> requests = requestRepository.findByRequesterId(userId);
        log.debug("FOUND {} requests for user ID={}", requests.size(), userId);

        return requests.stream()
                .map(requestMapper::mapToRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto postRequest(Long userId, Long eventId) {
        log.info("POST request: user ID={}, event ID={}", userId, eventId);

        User user = checkUserExists(userId);
        Event event = checkEventExists(eventId);

        // Проверка на дублирование
        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Duplicate requests are not allowed");
        }

        // Проверка, что пользователь не инициатор
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Initiator cannot participate in own event");
        }

        // Проверка, что событие опубликовано
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot participate in unpublished event");
        }

        // Проверка лимита участников
        Integer confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestState.CONFIRMED);
        if (event.getParticipantLimit() > 0 && confirmedCount != null && confirmedCount >= event.getParticipantLimit()) {
            throw new ConflictException("Participant limit reached");
        }

        // Определяем статус запроса
        RequestState status = RequestState.PENDING;
        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            status = RequestState.CONFIRMED;
        }

        // Создаем запрос - БЕЗ БИЛДЕРА
        Request request = new Request();
        request.setRequester(user);
        request.setEvent(event);
        request.setStatus(status);
        request.setCreated(LocalDateTime.now());

        Request savedRequest = requestRepository.save(request);
        log.info("CREATED request ID={} for event ID={}", savedRequest.getId(), eventId);

        return requestMapper.mapToRequestDto(savedRequest);
    }

    @Override
    @Transactional
    public ParticipationRequestDto patchRequest(Long userId, Long requestId) {
        log.info("PATCH cancel request ID={} by user ID={}", requestId, userId);

        checkUserExists(userId);
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request ID=" + requestId + " not found"));

        // Проверка, что запрос принадлежит пользователю
        if (!request.getRequester().getId().equals(userId)) {
            throw new ConflictException("User cannot cancel another user's request");
        }

        // Проверка, что запрос еще не отменен
        if (request.getStatus() == RequestState.CANCELED) {
            throw new ConflictException("Request already canceled");
        }

        request.setStatus(RequestState.CANCELED);
        Request updatedRequest = requestRepository.save(request);
        log.info("CANCELED request ID={}", requestId);

        return requestMapper.mapToRequestDto(updatedRequest);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.info("GET requests for event ID={} by user ID={}", eventId, userId);

        checkUserExists(userId);
        Event event = checkEventExists(eventId);

        // Проверка, что пользователь - инициатор события
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Only event initiator can view event requests");
        }

        List<Request> requests = requestRepository.findByEventId(eventId);
        log.info("FOUND {} requests for event ID={}", requests.size(), eventId);

        return requests.stream()
                .map(requestMapper::mapToRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public List<ParticipationRequestDto> patchEventRequestsStatus(Long userId, Long eventId, RequestStatusUpdateDto statusUpdateDto) {
        log.info("PATCH status for requests: {}", statusUpdateDto.getRequestIds());

        checkUserExists(userId);
        Event event = checkEventExists(eventId);

        // Проверка, что пользователь - инициатор события
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Only event initiator can change request statuses");
        }

        // Получаем запросы
        List<Request> requests = requestRepository.findByIdIn(statusUpdateDto.getRequestIds());
        if (requests.isEmpty()) {
            throw new NotFoundException("No requests found with provided IDs");
        }

        // Проверяем, что все запросы в статусе PENDING
        for (Request request : requests) {
            if (request.getStatus() != RequestState.PENDING) {
                throw new ConflictException("Cannot change status: request ID=" + request.getId() + " is not in PENDING state");
            }
        }

        RequestState newStatus = RequestState.valueOf(statusUpdateDto.getStatus());
        List<ParticipationRequestDto> result = new ArrayList<>();

        if (newStatus == RequestState.REJECTED) {
            // Отклоняем все запросы
            for (Request request : requests) {
                request.setStatus(RequestState.REJECTED);
                result.add(requestMapper.mapToRequestDto(requestRepository.save(request)));
            }
        } else if (newStatus == RequestState.CONFIRMED) {
            // Подтверждаем с учетом лимита
            Integer confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestState.CONFIRMED);
            int availableSlots = event.getParticipantLimit() - (confirmedCount != null ? confirmedCount : 0);

            if (event.getParticipantLimit() == 0) {
                // Если лимит 0 - подтверждаем все
                for (Request request : requests) {
                    request.setStatus(RequestState.CONFIRMED);
                    result.add(requestMapper.mapToRequestDto(requestRepository.save(request)));
                }
            } else {
                // Подтверждаем в пределах доступных слотов
                for (int i = 0; i < requests.size(); i++) {
                    Request request = requests.get(i);
                    if (availableSlots > 0) {
                        request.setStatus(RequestState.CONFIRMED);
                        availableSlots--;
                    } else {
                        request.setStatus(RequestState.REJECTED);
                    }
                    result.add(requestMapper.mapToRequestDto(requestRepository.save(request)));
                }
            }
        }

        log.info("UPDATED {} requests with status {}", result.size(), newStatus);
        return result;
    }

    // === Вспомогательные методы ===

    private User checkUserExists(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User ID=" + userId + " not found"));
    }

    private Event checkEventExists(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event ID=" + eventId + " not found"));
    }
}
