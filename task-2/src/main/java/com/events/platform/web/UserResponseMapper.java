package com.events.platform.web;

import com.events.platform.domain.User;
import com.events.platform.repo.HostMembershipRepository;
import com.events.platform.web.dto.HostMembershipBrief;
import com.events.platform.web.dto.UserResponse;

public final class UserResponseMapper {

    private UserResponseMapper() {}

    public static UserResponse from(User user, HostMembershipRepository hostMembershipRepository) {
        var memberships =
                hostMembershipRepository.findAllByUserId(user.getId()).stream()
                        .map(m -> new HostMembershipBrief(m.getHostId(), m.getRole().name()))
                        .toList();
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), memberships);
    }
}
