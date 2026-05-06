package com.events.platform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "events",
        uniqueConstraints = @UniqueConstraint(columnNames = {"host_id", "slug"}))
@Getter
@Setter
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "host_id")
    private Host host;

    @Column(nullable = false, length = 160)
    private String slug;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(nullable = false, length = 64)
    private String timezone = "UTC";

    @Column(name = "venue_text")
    private String venueText;

    @Column(name = "online_url")
    private String onlineUrl;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EventVisibility visibility = EventVisibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EventLifecycle lifecycle = EventLifecycle.DRAFT;

    @Column(name = "moderation_hidden", nullable = false)
    private boolean moderationHidden = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public boolean isEnded(Instant now) {
        return !now.isBefore(endAt);
    }

    public boolean isUpcoming(Instant now) {
        return now.isBefore(startAt);
    }
}
