package com.events.platform.service;

import com.events.platform.domain.Event;
import com.events.platform.domain.MembershipRole;
import com.events.platform.domain.User;
import com.events.platform.repo.HostMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final HostMembershipRepository membershipRepository;

    public boolean isMember(Long hostId, User user) {
        if (user == null) {
            return false;
        }
        return membershipRepository.existsByHostIdAndUserId(hostId, user.getId());
    }

    public boolean hasRole(Long hostId, User user, MembershipRole role) {
        if (user == null) {
            return false;
        }
        return membershipRepository.existsByHostIdAndUserIdAndRole(hostId, user.getId(), role);
    }

    public boolean canManageHost(Long hostId, User user) {
        return hasRole(hostId, user, MembershipRole.HOST);
    }

    public boolean canCheckIn(Event event, User user) {
        return hasRole(event.getHost().getId(), user, MembershipRole.HOST)
                || hasRole(event.getHost().getId(), user, MembershipRole.CHECKER);
    }
}
