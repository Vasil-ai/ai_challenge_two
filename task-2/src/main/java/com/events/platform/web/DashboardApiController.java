package com.events.platform.web;

import com.events.platform.domain.User;
import com.events.platform.service.DashboardService;
import com.events.platform.web.dto.DashboardRowResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hosts/{hostId}/dashboard")
@RequiredArgsConstructor
public class DashboardApiController {

    private final DashboardService dashboardService;

    @GetMapping
    public List<DashboardRowResponse> dashboard(@PathVariable Long hostId) {
        User user = SecurityUtils.requireUser();
        return dashboardService.dashboard(hostId, user);
    }
}
