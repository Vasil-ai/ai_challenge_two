package com.events.platform.repo;

import com.events.platform.domain.GalleryPhoto;
import com.events.platform.domain.GalleryPhotoStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GalleryPhotoRepository extends JpaRepository<GalleryPhoto, Long> {

    List<GalleryPhoto> findByEventIdAndStatusAndModerationHiddenFalseOrderByCreatedAtDesc(
            Long eventId, GalleryPhotoStatus status);

    List<GalleryPhoto> findByEventIdAndStatusOrderByCreatedAtDesc(Long eventId, GalleryPhotoStatus status);

    List<GalleryPhoto> findByEvent_Host_IdAndStatusOrderByCreatedAtDesc(
            Long hostId, GalleryPhotoStatus status);
}
