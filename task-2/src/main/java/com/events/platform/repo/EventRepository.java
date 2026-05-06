package com.events.platform.repo;

import com.events.platform.domain.Event;
import com.events.platform.domain.EventLifecycle;
import com.events.platform.domain.EventVisibility;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") Long id);

    Optional<Event> findByHostIdAndSlug(Long hostId, String slug);

    @Query(
            """
            SELECT e FROM Event e
            WHERE e.lifecycle = :lifecycle
            AND e.moderationHidden = false
            AND e.visibility = :visibility
            AND (:includePast = true OR e.endAt >= :now)
            AND e.startAt >= COALESCE(:from, e.startAt)
            AND e.startAt <= COALESCE(:to, e.startAt)
            AND (COALESCE(:query, '') = ''
                OR LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(e.description, '')) LIKE LOWER(CONCAT('%', :query, '%')))
            AND (COALESCE(:location, '') = ''
                OR LOWER(COALESCE(e.venueText, '')) LIKE LOWER(CONCAT('%', :location, '%')))
            ORDER BY e.startAt ASC
            """)
    List<Event> searchExplore(
            @Param("lifecycle") EventLifecycle lifecycle,
            @Param("visibility") EventVisibility visibility,
            @Param("includePast") boolean includePast,
            @Param("now") Instant now,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("query") String query,
            @Param("location") String location);

    @Query(
            """
            SELECT DISTINCT e FROM Event e, HostMembership m
            WHERE e.host.id = m.hostId AND m.userId = :userId
            AND e.host.id = COALESCE(:hostId, e.host.id)
            AND e.startAt >= COALESCE(:from, e.startAt)
            AND e.startAt <= COALESCE(:to, e.startAt)
            AND (COALESCE(:q, '') = '' OR LOWER(e.title) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY e.startAt DESC
            """)
    List<Event> findEventsForMember(
            @Param("userId") Long userId,
            @Param("hostId") Long hostId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("q") String q);

    List<Event> findByHostIdAndLifecycleOrderByStartAtDesc(Long hostId, EventLifecycle lifecycle);

    List<Event> findByHostIdOrderByStartAtDesc(Long hostId);
}
