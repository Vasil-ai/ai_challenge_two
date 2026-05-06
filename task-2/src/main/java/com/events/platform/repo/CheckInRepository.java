package com.events.platform.repo;

import com.events.platform.domain.CheckIn;
import com.events.platform.domain.Ticket;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckInRepository extends JpaRepository<CheckIn, Long> {

    boolean existsByTicketAndUndoneAtIsNull(Ticket ticket);

    Optional<CheckIn> findTopByEventIdAndSessionIdAndUndoneAtIsNullOrderByCheckedInAtDesc(
            Long eventId, String sessionId);

    long countByEventIdAndUndoneAtIsNull(Long eventId);

    Optional<CheckIn> findByTicketIdAndUndoneAtIsNull(Long ticketId);
}
