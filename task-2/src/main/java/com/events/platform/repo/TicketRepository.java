package com.events.platform.repo;

import com.events.platform.domain.Ticket;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByRegistrationId(Long registrationId);

    void deleteByRegistrationId(Long registrationId);

    Optional<Ticket> findByPublicCodeIgnoreCase(String publicCode);
}
