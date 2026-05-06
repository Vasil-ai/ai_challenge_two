package com.events.platform.service;

import com.events.platform.domain.Event;
import com.events.platform.domain.GalleryPhoto;
import com.events.platform.domain.GalleryPhotoStatus;
import com.events.platform.domain.RsvpStatus;
import com.events.platform.domain.User;
import com.events.platform.repo.EventRepository;
import com.events.platform.repo.GalleryPhotoRepository;
import com.events.platform.repo.RsvpRegistrationRepository;
import com.events.platform.web.error.ApiException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class GalleryService {

    private final EventRepository eventRepository;
    private final GalleryPhotoRepository galleryPhotoRepository;
    private final RsvpRegistrationRepository rsvpRegistrationRepository;
    private final AccessControlService accessControlService;

    @Value("${app.upload-dir:./data/uploads}")
    private String uploadDir;

    @PostConstruct
    void mkdir() throws IOException {
        Files.createDirectories(Path.of(uploadDir));
    }

    public List<GalleryPhoto> publicGallery(Long eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> notFound());
        if (event.getLifecycle() != com.events.platform.domain.EventLifecycle.PUBLISHED
                || event.isModerationHidden()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Not found");
        }
        return galleryPhotoRepository.findByEventIdAndStatusAndModerationHiddenFalseOrderByCreatedAtDesc(
                eventId, GalleryPhotoStatus.APPROVED);
    }

    @Transactional
    public GalleryPhoto upload(Long eventId, User user, MultipartFile file) throws IOException {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> notFound());
        Instant now = Instant.now();
        if (event.isEnded(now)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot upload after event ended");
        }
        var reg =
                rsvpRegistrationRepository
                        .findActiveForUserAndEvent(user.getId(), eventId)
                        .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "RSVP required"));
        if (reg.getStatus() != RsvpStatus.CONFIRMED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Confirmed RSVP required");
        }
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Empty file");
        }
        String ext = guessExt(file.getOriginalFilename());
        String name = UUID.randomUUID() + ext;
        Path dest = Path.of(uploadDir).resolve(name);
        Files.copy(file.getInputStream(), dest);

        GalleryPhoto p = new GalleryPhoto();
        p.setEvent(event);
        p.setSubmittedBy(user);
        p.setImageUrl("/uploads/" + name);
        p.setStatus(GalleryPhotoStatus.PENDING);
        return galleryPhotoRepository.save(p);
    }

    public List<GalleryPhoto> pendingForHost(Long hostId, User user) {
        if (!accessControlService.canManageHost(hostId, user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        return galleryPhotoRepository.findByEvent_Host_IdAndStatusOrderByCreatedAtDesc(
                hostId, GalleryPhotoStatus.PENDING);
    }

    @Transactional
    public GalleryPhoto moderate(Long photoId, User user, boolean approve) {
        GalleryPhoto p =
                galleryPhotoRepository.findById(photoId).orElseThrow(() -> notFound());
        Event event = p.getEvent();
        if (!accessControlService.canManageHost(event.getHost().getId(), user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        p.setStatus(approve ? GalleryPhotoStatus.APPROVED : GalleryPhotoStatus.REJECTED);
        return galleryPhotoRepository.save(p);
    }

    private static String guessExt(String original) {
        if (original == null || !original.contains(".")) {
            return ".bin";
        }
        return original.substring(original.lastIndexOf('.')).toLowerCase();
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "Not found");
    }
}
