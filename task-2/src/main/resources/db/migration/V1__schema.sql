CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE hosts (
    id BIGSERIAL PRIMARY KEY,
    slug VARCHAR(120) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    logo_url VARCHAR(2048),
    bio TEXT,
    contact_email VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE host_memberships (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    host_id BIGINT NOT NULL REFERENCES hosts(id) ON DELETE CASCADE,
    role VARCHAR(16) NOT NULL CHECK (role IN ('HOST', 'CHECKER')),
    PRIMARY KEY (user_id, host_id)
);

CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    host_id BIGINT NOT NULL REFERENCES hosts(id) ON DELETE CASCADE,
    slug VARCHAR(160) NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    timezone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    venue_text VARCHAR(500),
    online_url VARCHAR(2048),
    capacity INT NOT NULL CHECK (capacity >= 1),
    cover_image_url VARCHAR(2048),
    visibility VARCHAR(16) NOT NULL CHECK (visibility IN ('PUBLIC', 'UNLISTED')),
    lifecycle VARCHAR(16) NOT NULL CHECK (lifecycle IN ('DRAFT', 'PUBLISHED')),
    moderation_hidden BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (host_id, slug)
);

CREATE TABLE rsvp_registrations (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(16) NOT NULL CHECK (status IN ('CONFIRMED', 'WAITLISTED', 'CANCELLED')),
    waitlist_position INT,
    promoted_at TIMESTAMPTZ,
    promotion_pending BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_rsvp_active_per_user_event
    ON rsvp_registrations (event_id, user_id)
    WHERE status <> 'CANCELLED';

CREATE TABLE tickets (
    id BIGSERIAL PRIMARY KEY,
    registration_id BIGINT NOT NULL UNIQUE REFERENCES rsvp_registrations(id) ON DELETE CASCADE,
    public_code VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE check_ins (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    checked_in_at TIMESTAMPTZ NOT NULL,
    checked_in_by_user_id BIGINT NOT NULL REFERENCES users(id),
    undone_at TIMESTAMPTZ,
    session_id VARCHAR(64) NOT NULL
);

CREATE INDEX idx_check_ins_event_session ON check_ins(event_id, session_id, checked_in_at DESC);

CREATE TABLE gallery_photos (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    submitted_by_user_id BIGINT NOT NULL REFERENCES users(id),
    image_url VARCHAR(2048) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    moderation_hidden BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE event_feedback (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    stars SMALLINT NOT NULL CHECK (stars >= 1 AND stars <= 5),
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (event_id, user_id)
);

CREATE TABLE content_reports (
    id BIGSERIAL PRIMARY KEY,
    target_type VARCHAR(16) NOT NULL CHECK (target_type IN ('EVENT', 'PHOTO')),
    target_event_id BIGINT REFERENCES events(id) ON DELETE CASCADE,
    target_photo_id BIGINT REFERENCES gallery_photos(id) ON DELETE CASCADE,
    reporter_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    reporter_ip VARCHAR(45),
    status VARCHAR(24) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'DISMISSED', 'CONTENT_HIDDEN')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_by_user_id BIGINT REFERENCES users(id),
    resolved_at TIMESTAMPTZ,
    CONSTRAINT chk_report_target CHECK (
        (target_type = 'EVENT' AND target_event_id IS NOT NULL AND target_photo_id IS NULL)
        OR (target_type = 'PHOTO' AND target_photo_id IS NOT NULL)
    )
);

CREATE TABLE invite_links (
    id BIGSERIAL PRIMARY KEY,
    host_id BIGINT NOT NULL REFERENCES hosts(id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    role VARCHAR(16) NOT NULL CHECK (role IN ('HOST', 'CHECKER')),
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
