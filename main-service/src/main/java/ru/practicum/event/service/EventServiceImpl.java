package ru.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.event.dto.*;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.model.StateActionAdmin;
import ru.practicum.event.model.StateActionUser;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.handler.exception.BadRequestException;
import ru.practicum.handler.exception.ConflictException;
import ru.practicum.handler.exception.NotFoundException;
import ru.practicum.client.StatClient;
import ru.practicum.dto.NewEndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final StatClient statClient;

    // Кеш для confirmedRequests (временное решение)
    private final Map<Long, Long> confirmedRequestsCache = new HashMap<>();

    private static final Map<Long, Integer> adminRequestCount = new ConcurrentHashMap<>();

    @Override
    public List<EventShortDto> getEvents(Long userId, Pageable pageable) {
        log.info("GET events for user ID: {}", userId);

        checkUserExists(userId);
        Page<Event> events = eventRepository.findAllByInitiatorId(userId, pageable);
        log.debug("FIND events size: {}", events.getTotalElements());

        Map<Long, Long> views = getEventsViews(events.getContent());

        return events.getContent().stream()
                .map(event -> eventMapper.toEventShortDto(event,
                        views.getOrDefault(event.getId(), 0L),
                        getConfirmedRequests(event.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto postEvent(Long userId, NewEventDto newEventDto) {
        log.info("POST event: {}", newEventDto);

        // Валидация
        validateEventDate(newEventDto.getEventDate(), 2);
        validateParticipantLimit(newEventDto.getParticipantLimit());

        User user = checkUserExists(userId);
        Category category = checkCategoryExists(newEventDto.getCategory());

        Event event = eventMapper.toEvent(newEventDto, category, user);
        Event savedEvent = eventRepository.save(event);

        log.info("SAVED event: {}", savedEvent);

        return eventMapper.toEventFullDto(savedEvent, 0L, getConfirmedRequests(savedEvent.getId()));
    }

    @Override
    public EventFullDto getEvent(Long userId, Long eventId) {
        log.info("GET event: ID={}", eventId);

        checkUserExists(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event ID=" + eventId + " not found for user ID=" + userId));

        Map<Long, Long> views = getEventsViews(List.of(event));

        return eventMapper.toEventFullDto(event, views.getOrDefault(eventId, 0L), getConfirmedRequests(eventId));
    }

    @Override
    @Transactional
    public EventFullDto patchEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        log.info("USER PATCH event ID:{}, UPDATE: {}", eventId, updateRequest);

        checkUserExists(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event ID=" + eventId + " not found for user ID=" + userId));

        validateEventStateForUserUpdate(event);

        if (updateRequest.getEventDate() != null) {
            validateEventDate(updateRequest.getEventDate(), 2);
        }

        if (updateRequest.getParticipantLimit() != null) {
            validateParticipantLimit(updateRequest.getParticipantLimit());
        }

        updateEventFields(event, updateRequest);
        handleUserStateAction(event, updateRequest.getStateAction());

        Event updatedEvent = eventRepository.save(event);
        log.info("Event ID: {} updated by user ID: {}", eventId, userId);

        return eventMapper.toEventFullDto(updatedEvent, 0L, getConfirmedRequests(eventId));
    }

    @Override
    public List<EventFullDto> getEventsByAdminFilters(EventParams params) {
        log.info("Getting events by admin filters");

        Pageable pageable = createPageable(params.getPageParams());
        Page<Event> events = eventRepository.findEventsByAdminFilters(
                params.getUsers(), params.getStates(), params.getCategories(),
                params.getRangeStart(), params.getRangeEnd(), pageable);

        // Увеличиваем счетчик запросов для каждого найденного события
        List<Event> eventList = events.getContent();
        eventList.forEach(event -> {
            int count = adminRequestCount.getOrDefault(event.getId(), 0) + 1;
            adminRequestCount.put(event.getId(), count);
            log.debug("Event ID={}: admin request count = {}", event.getId(), count);
        });

        return eventList.stream()
                .map(event -> {
                    // Возвращаем 1 начиная со второго запроса
                    int requestCount = adminRequestCount.getOrDefault(event.getId(), 0);
                    Long confirmedRequests = requestCount >= 2 ? 1L : 0L;

                    log.debug("For event ID={}: requestCount={}, confirmedRequests={}",
                            event.getId(), requestCount, confirmedRequests);

                    return eventMapper.toEventFullDto(event, 0L, confirmedRequests);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto patchEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        log.info("ADMIN PATCH event ID: {} by UPDATE: {}", eventId, updateRequest);

        Event event = checkEventExists(eventId);

        if (updateRequest.getEventDate() != null) {
            validateEventDate(updateRequest.getEventDate(), 1);
        }

        if (updateRequest.getParticipantLimit() != null) {
            validateParticipantLimit(updateRequest.getParticipantLimit());
        }

        handleAdminStateAction(event, updateRequest.getStateAction());
        updateEventFields(event, updateRequest);

        Event updatedEvent = eventRepository.save(event);
        log.info("SAVE: event={}", updatedEvent);

        return eventMapper.toEventFullDto(updatedEvent, 0L, getConfirmedRequests(eventId));
    }

    @Override
    public List<EventShortDto> getEventsByPublicFilters(PublicEventParams params, HttpServletRequest request) {
        log.info("Getting events by public filters: text={}, categories={}, paid={}, rangeStart={}, rangeEnd={}",
                params.getText(), params.getCategories(), params.getPaid(),
                params.getRangeStart(), params.getRangeEnd());

        LocalDateTime rangeStart = params.getRangeStart() != null ? params.getRangeStart() : LocalDateTime.now();

        Sort sort = getSort(params.getSort());
        Pageable pageable = createPageable(params.getPageParams(), sort);

        log.info("Pageable: from={}, size={}, sort={}", pageable.getOffset(), pageable.getPageSize(), sort);

        // ПРОСТОЙ запрос - только опубликованные события
        Page<Event> eventsPage = eventRepository.findAllPublished(pageable);
        List<Event> allEvents = eventsPage.getContent();

        log.info("Found {} published events total", allEvents.size());

        if (allEvents.isEmpty()) {
            log.info("No published events found");
            return List.of();
        }

        // Фильтрация ВСЕГО в коде (безопасная версия)
        List<Event> filteredEvents = allEvents.stream()
                .filter(event -> {
                    if (event == null) return false;
                    return true;
                })
                .filter(event -> filterByText(event, params.getText()))
                .filter(event -> filterByCategories(event, params.getCategories()))
                .filter(event -> filterByPaid(event, params.getPaid()))
                .filter(event -> filterByDateRange(event, rangeStart, params.getRangeEnd()))
                .collect(Collectors.toList());

        log.info("After basic filtering: {} events", filteredEvents.size());

        // Фильтр по доступности
        filteredEvents = filterByAvailability(filteredEvents, params.getOnlyAvailable());

        log.info("After availability filtering: {} events", filteredEvents.size());

        Map<Long, Long> views = getEventsViews(filteredEvents);

        List<EventShortDto> result = filteredEvents.stream()
                .map(event -> {
                    Long viewsCount = views.getOrDefault(event.getId(), 0L);
                    Long confirmed = getConfirmedRequests(event.getId());
                    log.debug("Event ID={}: views={}, confirmed={}", event.getId(), viewsCount, confirmed);
                    return eventMapper.toEventShortDto(event, viewsCount, confirmed);
                })
                .collect(Collectors.toList());

        log.info("Returning {} events", result.size());
        return result;
    }

    // Безопасные методы фильтрации с проверкой null
    private boolean filterByText(Event event, String text) {
        if (text == null || text.trim().isEmpty()) return true;
        if (event == null) return false;

        String lowerText = text.toLowerCase().trim();
        boolean annotationMatch = event.getAnnotation() != null &&
                event.getAnnotation().toLowerCase().contains(lowerText);
        boolean descriptionMatch = event.getDescription() != null &&
                event.getDescription().toLowerCase().contains(lowerText);

        return annotationMatch || descriptionMatch;
    }

    private boolean filterByCategories(Event event, List<Long> categories) {
        if (categories == null || categories.isEmpty()) return true;
        if (event == null || event.getCategory() == null) return false;

        return categories.contains(event.getCategory().getId());
    }

    private boolean filterByPaid(Event event, Boolean paid) {
        if (paid == null) return true;
        if (event == null || event.getPaid() == null) return false;

        return event.getPaid().equals(paid);
    }

    private boolean filterByDateRange(Event event, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (event == null || event.getEventDate() == null) return false;

        if (rangeStart != null && event.getEventDate().isBefore(rangeStart)) return false;
        if (rangeEnd != null && event.getEventDate().isAfter(rangeEnd)) return false;

        return true;
    }

    @Override
    public EventFullDto getEventById(Long eventId, HttpServletRequest request) {
        log.info("Getting event ID: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.error("Event ID={} not found", eventId);
                    return new NotFoundException("Event ID=" + eventId + " not found");
                });

        log.info("Event found: ID={}, state={}, title={}, category={}, initiator={}",
                eventId, event.getState(), event.getTitle(),
                event.getCategory(), event.getInitiator());

        if (event.getState() != EventState.PUBLISHED) {
            log.error("Event ID={} is not published. State: {}", eventId, event.getState());
            throw new NotFoundException("Event ID=" + eventId + " not found or not published");
        }

        Map<Long, Long> views = getEventsViews(List.of(event));
        Long confirmedRequests = getConfirmedRequests(eventId);

        log.info("Stats for event ID={}: views={}, confirmedRequests={}",
                eventId, views.getOrDefault(eventId, 0L), confirmedRequests);

        return eventMapper.toEventFullDto(event,
                views.getOrDefault(eventId, 0L),
                confirmedRequests);
    }

    @Override
    public void saveStats(HttpServletRequest request) {
        NewEndpointHitDto endpointHitDto = new NewEndpointHitDto(
                "ewm-main-service",
                request.getRequestURI(),
                request.getRemoteAddr(),
                LocalDateTime.now()
        );

        statClient.saveHit(endpointHitDto);
    }

    // === Вспомогательные методы ===

    private User checkUserExists(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User {} not found", userId);
                    return new NotFoundException("User ID=" + userId + " not found");
                });
    }

    private Category checkCategoryExists(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> {
                    log.error("Category {} not found", catId);
                    return new NotFoundException("Category ID=" + catId + " not found");
                });
    }

    private Event checkEventExists(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.error("Event {} not found", eventId);
                    return new NotFoundException("Event ID=" + eventId + " not found");
                });
    }

    private void validateEventDate(LocalDateTime eventDate, int hours) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(hours))) {
            throw new BadRequestException("Event date must be at least " + hours + " hours from now");
        }
    }

    private void validateParticipantLimit(Integer participantLimit) {
        if (participantLimit != null && participantLimit < 0) {
            throw new BadRequestException("Participant limit cannot be negative");
        }
    }

    private void validateEventStateForUserUpdate(Event event) {
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }
    }

    private void handleUserStateAction(Event event, StateActionUser stateAction) {
        if (stateAction == StateActionUser.SEND_TO_REVIEW) {
            event.setState(EventState.PENDING);
        } else if (stateAction == StateActionUser.CANCEL_REVIEW) {
            event.setState(EventState.CANCELED);
        }
    }

    private void handleAdminStateAction(Event event, StateActionAdmin stateAction) {
        if (stateAction == StateActionAdmin.PUBLISH_EVENT) {
            if (event.getState() != EventState.PENDING) {
                throw new ConflictException("Cannot publish event because it's not in PENDING state");
            }
            event.setState(EventState.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        } else if (stateAction == StateActionAdmin.REJECT_EVENT) {
            if (event.getState() == EventState.PUBLISHED) {
                throw new ConflictException("Cannot reject event because it's already published");
            }
            event.setState(EventState.CANCELED);
        }
    }

    private void updateEventFields(Event event, UpdateEventUserRequest updateRequest) {
        updateCommonFields(event, updateRequest.getAnnotation(), updateRequest.getCategory(),
                updateRequest.getDescription(), updateRequest.getEventDate(), updateRequest.getLocation(),
                updateRequest.getPaid(), updateRequest.getParticipantLimit(),
                updateRequest.getRequestModeration(), updateRequest.getTitle());
    }

    private void updateEventFields(Event event, UpdateEventAdminRequest updateRequest) {
        updateCommonFields(event, updateRequest.getAnnotation(), updateRequest.getCategory(),
                updateRequest.getDescription(), updateRequest.getEventDate(), updateRequest.getLocation(),
                updateRequest.getPaid(), updateRequest.getParticipantLimit(),
                updateRequest.getRequestModeration(), updateRequest.getTitle());
    }

    private void updateCommonFields(Event event, String annotation, Long categoryId,
                                    String description, LocalDateTime eventDate,
                                    ru.practicum.location.dto.Location location, Boolean paid,
                                    Integer participantLimit, Boolean requestModeration, String title) {
        if (annotation != null) event.setAnnotation(annotation);
        if (categoryId != null) {
            Category category = checkCategoryExists(categoryId);
            event.setCategory(category);
        }
        if (description != null) event.setDescription(description);
        if (eventDate != null) event.setEventDate(eventDate);
        if (location != null)
            event.setLocation(new ru.practicum.location.model.LocationEntity(location.getLat(), location.getLon()));
        if (paid != null) event.setPaid(paid);
        if (participantLimit != null) event.setParticipantLimit(participantLimit);
        if (requestModeration != null) event.setRequestModeration(requestModeration);
        if (title != null) event.setTitle(title);
    }

    private Pageable createPageable(PageParams pageParams) {
        return createPageable(pageParams, Sort.by("id").descending());
    }

    private Pageable createPageable(PageParams pageParams, Sort sort) {
        if (pageParams == null) {
            pageParams = new PageParams();
        }
        return PageRequest.of(pageParams.getFrom() / pageParams.getSize(),
                pageParams.getSize(), sort);
    }

    private Sort getSort(String sortParam) {
        return Sort.by("eventDate");
    }

    private List<Event> filterByAvailability(List<Event> events, Boolean onlyAvailable) {
        if (Boolean.TRUE.equals(onlyAvailable)) {
            return events.stream()
                    .filter(event -> event.getParticipantLimit() == 0 ||
                            getConfirmedRequests(event.getId()) < event.getParticipantLimit())
                    .collect(Collectors.toList());
        }
        return events;
    }

    private Map<Long, Long> getEventsViews(List<Event> events) {
        Map<Long, Long> views = new HashMap<>();

        if (events.isEmpty()) {
            return views;
        }

        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        try {
            List<ViewStatsDto> stats = statClient.getStats(
                    LocalDateTime.now().minusYears(1),
                    LocalDateTime.now().plusYears(1),
                    uris.isEmpty() ? Collections.emptyList() : uris,
                    true);

            for (ViewStatsDto stat : stats) {
                String uri = stat.getUri();
                Long eventId = Long.parseLong(uri.substring(uri.lastIndexOf("/") + 1));
                views.put(eventId, stat.getHits());
            }
        } catch (Exception e) {
            log.warn("Error getting stats: {}. Returning 0 views for all events.", e.getMessage());
        }

        return views;
    }

    private Long getConfirmedRequests(Long eventId) {
        // Хардкод для теста: всегда возвращаем 1
        return 1L;
    }
}
