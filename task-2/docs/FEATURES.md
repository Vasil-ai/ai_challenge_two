# Feature specification cards (F01–F20)

Cross-reference: [requirements.txt](../requirements.txt), master [SDD.md](SDD.md), таски реализации [SDD-TASKS.md](SDD-TASKS.md).

---

## F01 — Auth and return after sign-in

**Source:** L18, L51  

**Goal:** RSVP and other protected actions require authentication; after sign-in the user returns to the intended page (e.g. event page).

**Actors:** Guest, User.

**Scenarios:**
- Guest opens event page → clicks RSVP → redirect to sign-in with `returnUrl` (or state) → after success → redirect back to same event page.
- Direct visit to sign-in without return URL → land on safe default (e.g. Explore or home).

**Data / contracts:** Session or JWT; store `returnUrl` validated (same origin / allowlist) to prevent open redirects.

**API / UI:** `GET /sign-in?returnUrl=...`; post-login middleware reads param.

**Acceptance criteria:**
- [ ] Unauthenticated RSVP click always ends on event page after successful auth.
- [ ] `returnUrl` cannot redirect to external domains.

**Dependencies:** None (foundation).  

**Risks:** Open redirect if return URL not validated.

---

## F02 — Host profile and public Host page

**Source:** L6–7  

**Goal:** Any signed-in user can register as Host; profile has name, logo, short bio, contact email; public page is shareable.

**Actors:** User, Guest (read-only public page).

**Scenarios:**
- User completes “Become a Host” flow → `Host` + membership with role `Host` created.
- Guest opens `/hosts/:slug` → sees name, logo, bio, contact (email policy: public per requirements).

**Data:** `Host`, `HostMembership` (role HOST).

**API / UI:** `POST /api/hosts`, `GET /api/hosts/{slug}`, edit form for owners.

**Acceptance criteria:**
- [ ] Self-serve registration creates host + owning membership.
- [ ] Public page lists upcoming/published events for that host (when F03 exists).

**Dependencies:** F01.  

**Risks:** Email scraping — consider obfuscation optional (out of scope unless required).

---

## F03 — Event CRUD and lifecycle

**Source:** L8–9  

**Goal:** Create/edit events with title, description, start/end + timezone, venue or online link, capacity, cover image; Draft vs Published; Public vs Unlisted; Publish, Unpublish, Duplicate.

**Actors:** Host (role Host).

**Scenarios:**
- Create draft → edit fields → Publish → event visible per visibility rules.
- Unpublish → not shown as active listing per product rules (still link for Unlisted per policy).
- Duplicate → new draft copy with cleared RSVPs.

**Data:** `Event` with lifecycle and visibility enums.

**API:** CRUD under `/api/hosts/{hostId}/events`, publish/unpublish/duplicate actions.

**Acceptance criteria:**
- [ ] All editorial fields persisted and validated (end after start, capacity ≥ 1).
- [ ] Public events appear in Explore (F05); Unlisted only via direct link.
- [ ] Duplicate creates independent event in Draft.

**Dependencies:** F02.  

**Risks:** Timezone bugs — store UTC + IANA zone id.

---

## F04 — Free / Paid toggle (Paid disabled)

**Source:** L10, L59  

**Goal:** Editor shows Free/Paid toggle; Paid disabled with “Coming soon” tooltip.

**Actors:** Host.

**Scenarios:** Toggle visible; attempting Paid shows tooltip; persisted pricing model fixed to “free” for MVP.

**Acceptance criteria:**
- [ ] Paid cannot be selected as active state.
- [ ] Tooltip copy matches spec intent (“Coming soon”).

**Dependencies:** F03.  

**Risks:** None.

---

## F05 — Explore page

**Source:** L13–15, L50, L60  

**Goal:** Browse events: text search, date range (default upcoming), location filter, Include Past toggle; past events show Ended; RSVP hidden when ended.

**Actors:** Guest, User.

**Scenarios:** Default list = upcoming Public events; Include Past adds ended; Unlisted never listed publicly.

**Data:** Query `Event` where `Published` and `Public` and time predicates.

**API:** `GET /api/events` with query params.

**Acceptance criteria:**
- [ ] Guests can browse all Public events including past (L50).
- [ ] Ended badge/label on past events; no RSVP control on event page (with F07/F08).

**Dependencies:** F03.  

**Risks:** Location filter fuzzy — define “contains text match” on venue field for MVP.

---

## F06 — Social preview metadata

**Source:** L15  

**Goal:** Event and Host public pages expose Open Graph / Twitter card meta for shareable links.

**Actors:** Guest (crawler), users sharing links.

**Implementation:** SSR meta injection or prerender for public URLs.

**Acceptance criteria:**
- [ ] Sharing event URL shows title, description, image in major messengers.
- [ ] Host page same.

**Dependencies:** F02, F03.  

**Risks:** SPA-only may need prerender — document chosen approach.

---

## F07 — RSVP, capacity, waitlist FIFO, auto-promotion

**Source:** L19–20, L24–25, L54  

**Goal:** Enforce capacity; overflow to FIFO waitlist; auto-promote on seat opening or capacity increase; promotion visible to attendee.

**Actors:** User.

**Scenarios:**
- RSVP when seats free → confirmed + ticket.
- RSVP when full → waitlisted; position implicit in FIFO.
- Cancel confirmed → head of waitlist promoted + ticket + notification.
- Increase capacity → if waitlist exists, promote up to new free seats (define: promote min(waitlist, new free seats)).

**Data:** Transactional updates on `RsvpRegistration`, `WaitlistEntry` / queue, `Ticket`.

**API:** `POST/DELETE /api/events/{id}/rsvp`.

**Acceptance criteria:**
- [ ] Never exceed capacity for confirmed count.
- [ ] FIFO order preserved across concurrent cancels (DB locking or serializable transaction).
- [ ] Promoted user sees updated state without manual admin action (L25).

**Dependencies:** F01, F03.  

**Risks:** Race conditions — must use DB transactions and row-level locking on event or counter.

---

## F08 — Ticket QR, calendar, cancel, My Tickets

**Source:** L20–22, L53  

**Goal:** Confirmed attendees get ticket with unique QR payload; Add to Calendar; cancel RSVP; My Tickets lists upcoming tickets.

**Actors:** User.

**Scenarios:** After RSVP confirmed, show QR and download option; `.ics` link or download; cancel removes ticket and may trigger F07 promotion.

**Data:** `Ticket` with stable public code; QR encodes code or URL.

**API:** `GET /api/me/tickets`.

**Acceptance criteria:**
- [ ] Each ticket code unique per registration.
- [ ] My Tickets shows only upcoming (or not ended) events.
- [ ] Calendar file includes correct times in user-local or event zone (document).

**Dependencies:** F07.  

**Risks:** QR size — keep payload compact.

---

## F09 — Roles and invite links

**Source:** L28–29  

**Goal:** Two roles per Host org: Host, Checker; invite by copyable link with embedded role.

**Actors:** Host (inviter), invitee User.

**Scenarios:** Host generates invite for Checker → link opens accept flow → membership created.

**Data:** `InviteLink` token hash, role, optional expiry.

**API:** `POST /api/hosts/{hostId}/invites`, `POST /api/invites/accept`.

**Acceptance criteria:**
- [ ] Link encodes role; accepting user gains correct membership.
- [ ] Revocation optional MVP: document if not implemented.

**Dependencies:** F02, F01.  

**Risks:** Token leak — short TTL recommended.

---

## F10 — Permission matrix

**Source:** L30–31, L55  

**Goal:** Host role: full event/gallery/dashboard/CSV/report queue for org; Checker: only check-in for org’s events.

**Actors:** Host, Checker.

**Acceptance criteria:**
- [ ] Checker receives 403 on dashboard, CSV, gallery approve, report queue, event editor.
- [ ] Host can perform all management actions listed in requirements.

**Dependencies:** F09, F03.  

**Risks:** Consistent enforcement in API (not UI-only).

---

## F11 — Host dashboard

**Source:** L33  

**Goal:** Lists Upcoming and Past events with per-event Going, Waitlist, Checked-in counts.

**Actors:** Host.

**Data:** Aggregated counts per event.

**API:** `GET /api/hosts/{hostId}/dashboard/summary`.

**Acceptance criteria:**
- [ ] Counts match ground truth from registrations and check-ins.
- [ ] Split tabs or sections: Upcoming vs Past.

**Dependencies:** F03, F07, F14.  

**Risks:** N+1 queries — use aggregation query or cache.

---

## F12 — CSV export

**Source:** L34, L66  

**Goal:** Export RSVPs/attendance with columns: name, email, RSVP status, check-in time; opens in Excel and Google Sheets.

**Actors:** Host.

**API:** `GET .../export/rsvps.csv`.

**Acceptance criteria:**
- [ ] Header row exact column set per L34.
- [ ] UTF-8 (+ BOM if required) — verify in Excel.
- [ ] Repository includes at least one example export file for submission (L66).

**Dependencies:** F07, F14, F10.  

**Risks:** Comma in fields — proper CSV escaping.

---

## F13 — My Events

**Source:** L35, L58  

**Goal:** Page visible only if user has any role on any host; aggregates events across hosts; filters: Host, date range, text search; quick actions depend on role (edit vs check-in).

**Actors:** Host, Checker.

**Acceptance criteria:**
- [ ] Users without roles do not see nav entry / get 404 or redirect.
- [ ] Filters work in combination.
- [ ] Checker sees check-in quick action; Host sees edit/dashboard links.

**Dependencies:** F03, F09, F10.  

**Risks:** Large lists — pagination.

---

## F14 — Check-in page

**Source:** L38–39, L55  

**Goal:** Checker opens check-in for event; manual code entry (no camera required); live counters; block duplicate check-ins; undo last scan (see SDD session policy).

**Actors:** Checker, Host.

**Acceptance criteria:**
- [ ] Valid code increments checked-in count once.
- [ ] Second scan same code returns error without double count.
- [ ] Undo reverses last successful check-in per SDD policy.
- [ ] Counters refresh (polling or SSE) within acceptable delay.

**Dependencies:** F08, F10.  

**Risks:** Concurrent checkers — use transactions; undo policy conflicts — document clearly.

---

## F15 — Post-event feedback

**Source:** L42, L56  

**Goal:** After event end, attendee may submit 1–5 stars + optional comment; before end — disabled or 400.

**Actors:** User (attendee).

**API:** `POST /api/events/{id}/feedback` with server-side time check.

**Acceptance criteria:**
- [ ] Submission rejected before `endAt`.
- [ ] One feedback per user per event (or allow edit — pick one; default one submission).

**Dependencies:** F03, F07 (attendance implied: optional “only if RSVP’d” — recommend yes).  

**Risks:** Spam — rate limit.

---

## F16 — Gallery uploads and Host approval

**Source:** L43, L56  

**Goal:** Attendees upload photos; pending until Host approves; public gallery shows approved only.

**Actors:** User, Host.

**API:** upload, list pending for host, approve/reject.

**Acceptance criteria:**
- [ ] Unapproved never on public event page.
- [ ] Host can approve/reject from queue.

**Dependencies:** F03, F10, F07.  

**Risks:** File size/type validation; malware scan out of scope.

---

## F17 — Report event or photo, review queue, hide

**Source:** L44, L57; moderation model [SDD.md](SDD.md) §0  

**Goal:** Any user (including guests per L44) may report an event or photo; items appear in review queue for **Host** role of owning org; can hide from public or dismiss. Guests require anti-abuse controls (see [SDD.md](SDD.md) §0).

**Actors:** Guest, User, Host.

**API:** create report, list queue, resolve.

**Acceptance criteria:**
- [ ] Reported content can be hidden from public views.
- [ ] Checker cannot access queue.
- [ ] Dismissed reports do not permanently delete content unless product says so — default: dismiss = no visibility change.

**Dependencies:** F03, F16, F10.  

**Risks:** Report bombing — rate limits.

---

## F18 — Seed data

**Source:** L64  

**Goal:** Deployed app (and local dev) has at least one Host, one upcoming Published event, one past Published event.

**Acceptance criteria:**
- [ ] Data present after migrate/seed command or Flyway callback.
- [ ] Past event shows Ended on public page.

**Dependencies:** F02, F03.  

**Risks:** None.

---

## F19 — Deployment

**Source:** L63  

**Goal:** Public HTTPS URL to working app; env-specific config for DB and OAuth secrets.

**Acceptance criteria:**
- [ ] Health check passes; core flows runnable on production URL.

**Dependencies:** Full stack build.  

**Risks:** CORS if SPA on different origin.

---

## F20 — Submission documentation

**Source:** L65–67  

**Goal:** `report.md` (tools, what worked/not, decisions); README step-by-step user guide Publish → RSVP → Ticket → Check-in (not a copy of requirements).

**Acceptance criteria:**
- [ ] Files exist at repository paths expected by challenge.
- [ ] README is procedural for a human operator/tester.

**Dependencies:** Feature-complete enough to describe truthfully.  

**Risks:** Drift between README and app — update before submission.

---

## Summary index

| ID | Name |
|----|------|
| F01 | Auth + return URL |
| F02 | Host profile |
| F03 | Event lifecycle |
| F04 | Paid toggle UI |
| F05 | Explore |
| F06 | Social preview |
| F07 | RSVP / waitlist / promotion |
| F08 | Tickets / calendar / My Tickets |
| F09 | Invites / roles |
| F10 | Permissions |
| F11 | Host dashboard |
| F12 | CSV export |
| F13 | My Events |
| F14 | Check-in |
| F15 | Feedback |
| F16 | Gallery moderation |
| F17 | Reports queue |
| F18 | Seeds |
| F19 | Deploy |
| F20 | report.md + README |
