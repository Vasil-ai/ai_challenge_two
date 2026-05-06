package com.events.platform.service;

import com.events.platform.domain.Event;
import com.events.platform.domain.User;
import com.events.platform.web.EventMapper;
import com.events.platform.web.dto.CheckInStatsResponse;
import com.events.platform.web.dto.DashboardRowResponse;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final EventService eventService;
    private final CheckInService checkInService;

    public List<DashboardRowResponse> dashboard(Long hostId, User user) {
        List<Event> events = eventService.listForHostDashboard(hostId, user);
        Instant now = Instant.now();
        return events.stream()
                .map(
                        e -> {
                            CheckInStatsResponse stats = checkInService.statsForEvent(e);
                            return new DashboardRowResponse(EventMapper.toResponse(e, now), stats);
                        })
                .toList();
    }
}
