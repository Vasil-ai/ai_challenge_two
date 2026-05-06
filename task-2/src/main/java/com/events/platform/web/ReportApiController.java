package com.events.platform.web;

import com.events.platform.domain.User;
import com.events.platform.service.ReportService;
import com.events.platform.web.dto.ReportCreateRequest;
import com.events.platform.web.dto.ReportQueueResponse;
import com.events.platform.web.dto.ReportResolveRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReportApiController {

    private final ReportService reportService;

    @PostMapping("/api/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody ReportCreateRequest req, HttpServletRequest request) {
        User reporter = SecurityUtils.currentUser();
        reportService.create(req.targetType(), req.targetId(), reporter, request);
    }

    @GetMapping("/api/hosts/{hostId}/reports")
    public List<ReportQueueResponse> queue(@PathVariable Long hostId) {
        User user = SecurityUtils.requireUser();
        return reportService.queue(hostId, user);
    }

    @PostMapping("/api/reports/{reportId}/resolve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resolve(@PathVariable Long reportId, @Valid @RequestBody ReportResolveRequest req) {
        User user = SecurityUtils.requireUser();
        reportService.resolve(reportId, user, Boolean.TRUE.equals(req.hide()));
    }
}
