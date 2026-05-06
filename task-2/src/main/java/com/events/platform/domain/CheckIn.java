package com.events.platform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "check_ins")
@Getter
@Setter
@NoArgsConstructor
public class CheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(name = "checked_in_at", nullable = false)
    private Instant checkedInAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "checked_in_by_user_id")
    private User checkedInBy;

    @Column(name = "undone_at")
    private Instant undoneAt;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    public boolean isActive() {
        return undoneAt == null;
    }
}
