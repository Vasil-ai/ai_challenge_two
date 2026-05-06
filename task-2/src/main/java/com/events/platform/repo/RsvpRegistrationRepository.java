package com.events.platform.repo;

import com.events.platform.domain.RsvpRegistration;
import com.events.platform.domain.RsvpStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RsvpRegistrationRepository extends JpaRepository<RsvpRegistration, Long> {

    long countByEventIdAndStatus(Long eventId, RsvpStatus status);

    Optional<RsvpRegistration> findByEventIdAndUserIdAndStatusNot(
            Long eventId, Long userId, RsvpStatus cancelled);

    @Query(
            """
            SELECT r FROM RsvpRegistration r
            WHERE r.event.id = :eventId AND r.status = :status
            ORDER BY r.waitlistPosition ASC
            """)
    List<RsvpRegistration> findWaitlistOrdered(@Param("eventId") Long eventId, @Param("status") RsvpStatus status);

    @Query(
            """
            SELECT r FROM RsvpRegistration r
            JOIN FETCH r.event e
            JOIN FETCH e.host h
            WHERE r.user.id = :userId
            AND r.status = 'CONFIRMED'
            AND e.endAt >= :now
            ORDER BY e.startAt ASC
            """)
    List<RsvpRegistration> findConfirmedUpcomingForUser(@Param("userId") Long userId, @Param("now") Instant now);

    @Query(
            """
            SELECT r FROM RsvpRegistration r
            JOIN FETCH r.event e
            WHERE r.user.id = :userId
            AND r.status IN ('CONFIRMED', 'WAITLISTED')
            AND e.id = :eventId
            """)
    Optional<RsvpRegistration> findActiveForUserAndEvent(
            @Param("userId") Long userId, @Param("eventId") Long eventId);

    Optional<RsvpRegistration> findByEventIdAndUserIdAndStatus(
            Long eventId, Long userId, RsvpStatus status);

    List<RsvpRegistration> findByEventIdOrderByCreatedAtAsc(Long eventId);

    @Query(
            """
            SELECT r FROM RsvpRegistration r
            JOIN FETCH r.user u
            WHERE r.event.id = :eventId AND r.status <> 'CANCELLED'
            ORDER BY r.createdAt ASC
            """)
    List<RsvpRegistration> findExportRows(@Param("eventId") Long eventId);

    @Query(
            """
            SELECT COALESCE(MAX(r.waitlistPosition), 0)
            FROM RsvpRegistration r
            WHERE r.event.id = :eventId AND r.status = 'WAITLISTED'
            """)
    int maxWaitlistPosition(@Param("eventId") Long eventId);

    @Query(
            """
            SELECT r FROM RsvpRegistration r
            JOIN FETCH r.event e
            JOIN FETCH e.host
            WHERE r.id = :id
            """)
    Optional<RsvpRegistration> findByIdWithEventAndHost(@Param("id") Long id);
}
