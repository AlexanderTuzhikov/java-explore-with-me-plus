package ru.practicum.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestState;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findByRequesterId(Long requesterId);

    List<Request> findByEventId(Long eventId);

    Optional<Request> findByRequesterIdAndEventId(Long requesterId, Long eventId);

    boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    Integer countByEventIdAndStatus(Long eventId, RequestState status);

    List<Request> findByIdIn(List<Long> ids);
}
