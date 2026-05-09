# Task specification: Align waitlist data model in docs with implementation (canonical `RsvpRegistration`)

**Status:** Draft  
**Last updated:** 2026-05-09  
**Owner / requester:** N/A (invoked via `/dor` without explicit subject; scope derived from `docs/SDD-TASKS.md` review row *Waitlist в ER* and related DOC-02)

## 1. Summary

The runtime codebase already implements a **single** waitlist representation: `RsvpRegistration` with `RsvpStatus` (`CONFIRMED`, `WAITLISTED`, `CANCELLED`), FIFO ordering via `waitlistPosition`, and promotion logic in `RsvpService` / `RsvpRegistrationRepository`. The SDD and feature docs still describe or imply a separate **`WaitlistEntry`** entity alongside `RsvpRegistration`, which contradicts the schema in `V1__schema.sql` and creates onboarding risk. This task is to **declare that model canonical in documentation**, update diagrams and feature text to match, and close the related backlog note—**without changing application behavior** unless a gap is discovered during doc review.

## 2. Goals and non-goals

### Goals

- Record **one** canonical waitlist model consistent with Flyway + JPA.
- Update **SDD §5** (ER / narrative) so it no longer implies a separate `WaitlistEntry` table unless the team explicitly reintroduces it in code.
- Update **FEATURES.md F07** “Data” line to match the canonical model.
- Resolve or update the **SDD-TASKS.md** review row for waitlist ER and the **DOC-02** follow-up so future readers see “done” or a crisp residual.

### Non-goals

- Refactoring RSVP runtime logic, new endpoints, or schema migrations (unless review finds a **documented** bug; then file a separate task).
- Implementing `WaitlistEntry` as a second persistence path.
- Changing FIFO semantics or promotion rules (already specified in SDD §3 / `RsvpService`).

### Assumptions

- **`report.md`** already states the canonical model; treat it as aligned with code and reuse wording where helpful.
- Hibernate `ddl-auto: validate` and Flyway `V1__schema.sql` are source of truth for **physical** schema.

## 3. Background and context

| Source | Current signal |
|--------|----------------|
| `docs/SDD-TASKS.md` | Review table: *Waitlist в ER* — choose one canonical scheme; DOC-02: describe chosen model in SDD §5 after code decision |
| `docs/SDD.md` §5 | Mermaid ER includes `WaitlistEntry`; text lists both `RsvpRegistration` and `WaitlistEntry` |
| `docs/FEATURES.md` F07 | Mentions `RsvpRegistration`, `WaitlistEntry` / queue, `Ticket` |
| Code | `domain/RsvpRegistration.java`, `service/RsvpService.java`, `repo/RsvpRegistrationRepository.java`, `db/migration/V1__schema.sql` — **no** `WaitlistEntry` entity or table |
| `report.md` | Documents single canonical model via `RsvpRegistration` + `waitlistPosition` |

Related implementation touchpoints (read-only for this task unless correcting doc references):

- `src/main/java/com/events/platform/domain/RsvpRegistration.java`
- `src/main/java/com/events/platform/service/RsvpService.java`
- `src/main/java/com/events/platform/repo/RsvpRegistrationRepository.java`
- `src/main/resources/db/migration/V1__schema.sql`

## 4. Functional requirements

1. **Canonical declaration:** In `docs/SDD.md` §5, state explicitly that the **waitlist queue is represented only inside `RsvpRegistration`** rows with `status = WAITLISTED` and monotonic `waitlistPosition` per event (FIFO), plus nullable `promotedAt` / `promotionPending` as already modeled.
2. **ER diagram:** Update the Mermaid (or equivalent) ER in SDD §5 to **remove** `WaitlistEntry` entity and relationships **or** replace with a note box explaining “logical queue = filtered registrations,” without inventing a new table.
3. **Sequence / narrative consistency:** Scan SDD §6 RSVP sequences; replace phrasing that assumes a separate waitlist table with operations on **waitlisted registrations** (SELECT … ORDER BY `waitlist_position` ASC).
4. **FEATURES F07:** Adjust the **Data** subsection so it lists only persistence artifacts that exist (`rsvp_registrations`, `tickets`, etc.) and describes FIFO in terms of `waitlist_position`.
5. **SDD-TASKS:** Update the review-row status for *Waitlist в ER* from “На уточнение при коде” to **resolved** with a one-line pointer to SDD §5; mark or annotate DOC-02 as satisfied when §5 is updated.
6. **Cross-links:** Ensure `docs/CODEBASE_MAP.md` “Data and persistence” bullet list remains accurate (no mention of `WaitlistEntry`).

## 5. Technical notes

- **No Flyway version** required if schema unchanged.
- **Filenames:** `docs/SDD.md`, `docs/FEATURES.md`, `docs/SDD-TASKS.md`; optionally a single sentence in `README.md` only if it currently contradicts the model (grep first).
- Preserve Russian/English mix already used in each doc; do not wholesale-translate.

## 6. User flows / behavior

N/A for end users. **Author / maintainer flow:**

1. Reader opens SDD §5 → sees one ER story matching DB.
2. Reader opens F07 → sees data contracts matching `V1__schema.sql`.
3. Reader opens SDD-TASKS review → sees waitlist ER closed.

## 7. Acceptance criteria

- [ ] `docs/SDD.md` §5 contains no standalone `WaitlistEntry` entity in the ER diagram unless explicitly labeled as *deprecated / not implemented* with zero ambiguity (preferred: **removed**).
- [ ] `docs/FEATURES.md` F07 “Data” section describes waitlist FIFO using **`RsvpRegistration`** + `waitlist_position` only.
- [ ] `docs/SDD-TASKS.md` review table reflects resolution of the waitlist ER item; DOC-02 status updated or removed as appropriate.
- [ ] Grep across `docs/` for `WaitlistEntry` returns **no** stale references implying a real table (allowed: historical changelog sentence only if clearly marked obsolete—prefer eliminate).
- [ ] No regression in meaning vs `report.md` / `RsvpService` behavior (promotion on cancel and `promoteWaitlistWhileHasSeats`).

## 8. Edge cases and risks

| Scenario | Handling |
|----------|----------|
| Stakeholder wanted `WaitlistEntry` for analytics | Document as future optional projection/read-model in ADR-style note, **not** as current schema |
| SDD §6 diagrams too large to edit | Update narrative first; diagram second in same PR |
| Translation drift | Keep terminology: `RsvpRegistration`, `waitlist_position`, `WAITLISTED` |

## 9. Open questions

1. Should `docs/project-map/PROJECT_MAP.md` or other maps mention waitlist? If yes, align in the same change set.

## 10. Implementation sequence

1. Grep `docs/` for `WaitlistEntry`, `waitlist`, `RsvpRegistration` and list hits.
2. Edit `docs/SDD.md` §5 (text + Mermaid); align §6 wording if needed.
3. Edit `docs/FEATURES.md` F07 Data subsection.
4. Edit `docs/SDD-TASKS.md` review + DOC-02 lines.
5. Optional: sweep `README.md` / `CODEBASE_MAP.md` for consistency.
6. Final grep + quick read-through for contradictions.

---

## Appendix: Implementation prompt (copy below)

```markdown
## Task: Documentation — canonical waitlist model

**Objective:** Remove documentation drift: SDD and FEATURES still reference a `WaitlistEntry` entity, but the product implements FIFO waitlist **only** via `RsvpRegistration` (`status`, `waitlist_position`, promotion fields) and Flyway `V1__schema.sql`. Align docs with code; **do not** change runtime behavior or DB schema.

**Scope:**
- Update `docs/SDD.md` §5 ER and narrative; fix §6 if it implies a separate waitlist table.
- Update `docs/FEATURES.md` F07 Data line.
- Update `docs/SDD-TASKS.md` review row (*Waitlist в ER*) and DOC-02 status.
- Grep `docs/` for `WaitlistEntry` and eliminate stale implications.

**References:** `report.md` (already correct), `RsvpService.java`, `RsvpRegistration.java`, `V1__schema.sql`.

**Acceptance:** No ER/table ambiguity; SDD-TASKS shows resolved; grep clean for phantom `WaitlistEntry`.
```
