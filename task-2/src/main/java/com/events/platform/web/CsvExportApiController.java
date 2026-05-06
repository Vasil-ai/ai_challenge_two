package com.events.platform.web;

import com.events.platform.domain.User;
import com.events.platform.service.CsvExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events/{eventId}/export")
@RequiredArgsConstructor
public class CsvExportApiController {

    private final CsvExportService csvExportService;

    @GetMapping(value = "/rsvps.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> export(@PathVariable Long eventId) {
        User user = SecurityUtils.requireUser();
        byte[] body = csvExportService.exportRsvps(eventId, user);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rsvps.csv\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(body);
    }
}
