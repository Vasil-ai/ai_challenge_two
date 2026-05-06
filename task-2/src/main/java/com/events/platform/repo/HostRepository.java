package com.events.platform.repo;

import com.events.platform.domain.Host;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HostRepository extends JpaRepository<Host, Long> {

    Optional<Host> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
