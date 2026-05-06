# Development report

## Tools and techniques

- **Backend:** Java 21, Spring Boot 3.2, Spring Web, Spring Security (session + BCrypt), Spring Data JPA, Flyway, PostgreSQL.
- **Frontend:** React 18, Vite 5, React Router, `qrcode.react`, fetch with `credentials: 'include'` for session cookies.
- **Persistence:** Single canonical waitlist model via `RsvpRegistration` status (`CONFIRMED` / `WAITLISTED` / `CANCELLED`) and `waitlistPosition` for FIFO ordering; pessimistic lock on `Event` during RSVP.
- **Deployment:** Docker Compose for PostgreSQL; static UI built into `classpath:/static` with SPA fallback for client-side routes.

## What worked well

- Vertical slice alignment with the SDD (explore → RSVP → ticket → check-in) mapped cleanly to REST boundaries.
- FIFO promotions on cancel and on capacity increase reuse one transactional path (`promoteWaitlistWhileHasSeats`).
- Session-scoped undo for check-in matches the SDD choice (per browser session id header).

## What did not / limitations

- **JAVA_HOME** was not available in the automated build environment here; verify locally with `mvn -DskipTests verify` after setting JDK 21.
- **OAuth2** was not integrated; email/password auth keeps the challenge self-contained.
- **Guest reporting** is supported by API (`reporter` nullable) but the minimal UI focuses on signed-in flows; optional CAPTCHA/rate limits are noted in SDD only.
- **Social crawlers:** rich previews use `/share/event/{id}` HTML with OG tags; canonical event URLs remain SPA routes.

## Notable decisions

- **Moderation:** Report queue and hide/dismiss are limited to the Host role of the owning organization (see `docs/SDD.md` §0).
- **Static resources:** `spring.web.resources.add-mappings=false` and a custom `WebConfig` handler serve `/uploads/**` from disk and `/**` from `static/` with `index.html` fallback for React Router.
- **CSV:** UTF-8 with BOM for Excel compatibility (`CsvExportService`).
