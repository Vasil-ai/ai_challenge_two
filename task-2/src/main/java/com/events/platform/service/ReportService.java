package com.events.platform.service;

import com.events.platform.domain.ContentReport;
import com.events.platform.domain.Event;
import com.events.platform.domain.GalleryPhoto;
import com.events.platform.domain.ReportStatus;
import com.events.platform.domain.ReportTargetType;
import com.events.platform.domain.User;
import com.events.platform.repo.ContentReportRepository;
import com.events.platform.repo.EventRepository;
import com.events.platform.repo.GalleryPhotoRepository;
import com.events.platform.web.dto.ReportQueueResponse;
import com.events.platform.web.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ContentReportRepository contentReportRepository;
    private final EventRepository eventRepository;
    private final GalleryPhotoRepository galleryPhotoRepository;
    private final AccessControlService accessControlService;

    @Transactional
    public ContentReport create(
            ReportTargetType type, long targetId, User reporter, HttpServletRequest request) {
        ContentReport r = new ContentReport();
        r.setTargetType(type);
        r.setReporterUser(reporter);
        r.setReporterIp(request.getRemoteAddr());
        r.setStatus(ReportStatus.OPEN);
        if (type == ReportTargetType.EVENT) {
            Event e = eventRepository.findById(targetId).orElseThrow(() -> notFound());
            r.setTargetEvent(e);
        } else {
            GalleryPhoto p = galleryPhotoRepository.findById(targetId).orElseThrow(() -> notFound());
            r.setTargetPhoto(p);
            r.setTargetEvent(p.getEvent());
        }
        return contentReportRepository.save(r);
    }

    @Transactional(readOnly = true)
    public List<ReportQueueResponse> queue(Long hostId, User user) {
        if (!accessControlService.canManageHost(hostId, user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        return contentReportRepository.findOpenForHost(hostId, ReportStatus.OPEN).stream()
                .map(
                        r -> {
                            Long eventRef =
                                    r.getTargetEvent() != null
                                            ? r.getTargetEvent().getId()
                                            : (r.getTargetPhoto() != null
                                                    ? r.getTargetPhoto().getEvent().getId()
                                                    : null);
                            return new ReportQueueResponse(
                                    r.getId(),
                                    r.getTargetType().name(),
                                    eventRef,
                                    r.getTargetPhoto() != null ? r.getTargetPhoto().getId() : null,
                                    r.getStatus().name(),
                                    r.getCreatedAt());
                        })
                .toList();
    }

    @Transactional
    public void resolve(Long reportId, User user, boolean hide) {
        ContentReport r = contentReportRepository.findById(reportId).orElseThrow(() -> notFound());
        Long hostId = resolveHostId(r);
        if (!accessControlService.canManageHost(hostId, user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        r.setResolvedBy(user);
        r.setResolvedAt(Instant.now());
        if (hide) {
            r.setStatus(ReportStatus.CONTENT_HIDDEN);
            if (r.getTargetType() == ReportTargetType.EVENT && r.getTargetEvent() != null) {
                Event e = r.getTargetEvent();
                e.setModerationHidden(true);
                eventRepository.save(e);
            } else if (r.getTargetPhoto() != null) {
                GalleryPhoto p = r.getTargetPhoto();
                p.setModerationHidden(true);
                galleryPhotoRepository.save(p);
            }
        } else {
            r.setStatus(ReportStatus.DISMISSED);
        }
        contentReportRepository.save(r);
    }

    private Long resolveHostId(ContentReport r) {
        if (r.getTargetPhoto() != null) {
            return r.getTargetPhoto().getEvent().getHost().getId();
        }
        if (r.getTargetEvent() != null) {
            return r.getTargetEvent().getHost().getId();
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid report");
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "Not found");
    }
}
