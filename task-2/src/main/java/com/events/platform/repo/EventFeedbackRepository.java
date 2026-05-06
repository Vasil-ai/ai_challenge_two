package com.events.platform.repo;

import com.events.platform.domain.EventFeedback;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventFeedbackRepository extends JpaRepository<EventFeedback, Long> {

    Optional<EventFeedback> findByEventIdAndUserId(Long eventId, Long userId);
}
