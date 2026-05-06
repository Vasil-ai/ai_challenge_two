package com.events.platform.web.dto;

import java.time.Instant;

public record ReportQueueResponse(
        Long id, String targetType, Long targetEventId, Long targetPhotoId, String status, Instant createdAt) {}
