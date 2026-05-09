---
name: dor
description: >-
  Produces a comprehensive task specification markdown document usable as a prompt
  for later implementation or review. Use when the user runs /dor, mentions DOR,
  definition of requirements, task specification, or asks for a spec before coding.
disable-model-invocation: true
---

# DOR — task specification

Turn the user’s intent (and repository context when relevant) into **one clear, self-contained specification** that another person or agent can execute without re-negotiating scope.

## Deliverable

Write or refresh:

`docs/TASK_SPEC.md`

Create `docs/` if missing. If the user gives an explicit path or filename, use that instead and still follow the same structure.

The document must read like **instructions for the implementer**, not a brainstorm.

## Principles

- **Complete**: goals, scope boundaries, acceptance criteria, and edge cases the reader would otherwise guess.
- **Traceable**: reference real paths, endpoints, or docs in this repo when the task touches code (discovered via search/read, not invented).
- **Prompt-ready**: closing section is a copy-paste **implementation prompt** summarizing the task in one block.

## Workflow

1. **Capture the ask** — Restate the task in one sentence; note constraints (deadline, tech, “must not change X”).
2. **Scope** — **In scope** / **Out of scope** / **Assumptions** (explicit).
3. **Context** — If implementation is in-repo: skim `README.md`, `docs/CODEBASE_MAP.md` if present, and relevant packages; list files or modules likely to change.
4. **Requirements** — Functional bullets (numbered). Non-functional only if applicable (performance, security, compatibility).
5. **Behavior** — User-visible flows, API shape, data rules, error handling expectations.
6. **Acceptance criteria** — Checklist; each item objectively verifiable.
7. **Edge cases & risks** — Table or bullets.
8. **Open questions** — Numbered; empty section only if none remain after reasonable defaults.
9. **Suggested sequencing** — Ordered steps for implementation/testing.
10. **Implementation prompt** — Final fenced markdown block the user can paste into a new chat.

## Output template

Fill every applicable section; use “N/A” only after confirming irrelevance.

```markdown
# Task specification: [short title]

**Status:** Draft  
**Last updated:** YYYY-MM-DD  
**Owner / requester:** [if known, else N/A]

## 1. Summary

[2–4 sentences: what to build or change and why]

## 2. Goals and non-goals

### Goals

- …

### Non-goals

- …

### Assumptions

- …

## 3. Background and context

[Links to ADR/SDD/issue IDs if any; relevant repo areas]

## 4. Functional requirements

1. …
2. …

## 5. Technical notes

[APIs, schema, security, config, migrations — as needed]

## 6. User flows / behavior

[Step-by-step or bullet scenarios]

## 7. Acceptance criteria

- [ ] …
- [ ] …

## 8. Edge cases and risks

| Scenario | Handling |
|----------|----------|
| … | … |

## 9. Open questions

1. …

## 10. Implementation sequence

1. …
2. …

---

## Appendix: Implementation prompt (copy below)

Use one fenced code block with language tag `markdown` containing the paste-ready prompt (objective, scope, key requirements, acceptance criteria, files to touch).
```

## After writing

- Re-read for internal consistency (no contradictory requirements).
- Ensure acceptance criteria match functional requirements.
- If the task is trivial, keep sections short but do not omit acceptance criteria or the implementation prompt.
