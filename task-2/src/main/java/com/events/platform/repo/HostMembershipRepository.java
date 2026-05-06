package com.events.platform.repo;

import com.events.platform.domain.HostMembership;
import com.events.platform.domain.MembershipRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HostMembershipRepository extends JpaRepository<HostMembership, HostMembership.HostMembershipId> {

    boolean existsByHostIdAndUserIdAndRole(Long hostId, Long userId, MembershipRole role);

    @Query("SELECT m FROM HostMembership m WHERE m.userId = :userId")
    List<HostMembership> findAllByUserId(@Param("userId") Long userId);

    List<HostMembership> findAllByHostId(Long hostId);

    boolean existsByHostIdAndUserId(Long hostId, Long userId);
}
