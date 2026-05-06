package com.events.platform.web.dto;

import java.util.List;

public record UserResponse(Long id, String email, String name, List<HostMembershipBrief> hostMemberships) {}
