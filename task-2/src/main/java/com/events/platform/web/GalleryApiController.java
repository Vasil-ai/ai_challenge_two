package com.events.platform.web;

import com.events.platform.domain.User;
import com.events.platform.service.GalleryService;
import com.events.platform.web.dto.GalleryPhotoResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class GalleryApiController {

    private final GalleryService galleryService;

    @GetMapping("/api/events/{eventId}/gallery")
    public List<GalleryPhotoResponse> publicGallery(@PathVariable Long eventId) {
        return galleryService.publicGallery(eventId).stream()
                .map(p -> new GalleryPhotoResponse(p.getId(), p.getImageUrl(), p.getStatus().name()))
                .toList();
    }

    @PostMapping("/api/events/{eventId}/gallery")
    public GalleryPhotoResponse upload(@PathVariable Long eventId, @RequestParam("file") MultipartFile file)
            throws IOException {
        User user = SecurityUtils.requireUser();
        var saved = galleryService.upload(eventId, user, file);
        return new GalleryPhotoResponse(saved.getId(), saved.getImageUrl(), saved.getStatus().name());
    }

    @GetMapping("/api/hosts/{hostId}/gallery/pending")
    public List<GalleryPhotoResponse> pending(@PathVariable Long hostId) {
        User user = SecurityUtils.requireUser();
        return galleryService.pendingForHost(hostId, user).stream()
                .map(p -> new GalleryPhotoResponse(p.getId(), p.getImageUrl(), p.getStatus().name()))
                .toList();
    }

    @PostMapping("/api/gallery/{photoId}/approve")
    public GalleryPhotoResponse approve(@PathVariable Long photoId, @RequestParam boolean approve) {
        User user = SecurityUtils.requireUser();
        var p = galleryService.moderate(photoId, user, approve);
        return new GalleryPhotoResponse(p.getId(), p.getImageUrl(), p.getStatus().name());
    }
}
