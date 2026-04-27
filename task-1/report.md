# Report — Company Leader Board recreation

A short write-up of how the static recreation of the SharePoint Modern page `CompanyLeaderBoard.aspx` was built, what techniques were used, and how every piece of real corporate data was replaced with fictional content.

---

## 1. Goal & constraints

- **Goal**: reproduce the visual design (chrome, layout, type scale, colors) and the functional behaviour (top-3 podium, KPI strip, sortable / filterable / paginated ranking, threaded comments) of the original SharePoint Modern page.
- **Hard constraint**: **no real corporate data**. Every name, role, department, office, score, badge, avatar, comment author and breadcrumb label must be invented and clearly fictional.
- **Output**: a static, self-contained `index.html` + `styles.css` + `app.js` (no build step, no framework, no server) that opens with a double-click and can be embedded into a SharePoint Communication site via Embed / Script Editor.

## 2. Tools & techniques

### Source-of-truth investigation
The live page (`https://ventionteamsinc.sharepoint.com/sites/edu/SitePages/CompanyLeaderBoard.aspx`) is gated by Microsoft 365 authentication and `robots.txt`, so direct fetching with `WebFetch` / MCP fetch / `curl` was not possible. The user provided the rendered HTML (`original.html`, ~787 KB minified). All structural information had to be mined out of that single dump.

Techniques used to extract design tokens from `original.html`:

- **PowerShell `Substring` + `IndexOf`** to slice out the embedded `spClientSidePageContext = JSON.parse('…')` blob (≈760 KB of escaped JSON describing the whole page).
- **`[Regex]::Unescape`** to decode the doubly-escaped string into a working JSON snapshot.
- Multiple targeted text searches (`themePrimary`, `headerLayout`, `headerEmphasis`, `layoutType`, `officeFabricIconFontName`, `webPartId`, `loaderConfig`) to pull out:
  - the site theme (standard Office: `themePrimary #0078d4`, `headerEmphasis: 0` white, `headerLayout: 3` Extended);
  - the Title Region configuration (`layoutType: FullWidthImage`, `enableGradientEffect: true`, `textAlignment: Left`);
  - the Page Hierarchy properties (`pagesToDisplay: ancestors`, `isFullWidth: true`, `isHeaderColored: true`);
  - the Advanced Comments configuration (`datetimeFormat: DD/MM/YYYY`, `roundProfilePictures: true`, all toggles enabled);
  - the manifest of the custom **Vention EDU – Leaderboard** SPFx web part (alias `LeaderboardWebPart`, React 17, bundle URL on the tenant App Catalog);
  - the Fluent UI icon names referenced in the manifests (`CompassNW`, `Page`, `FileComment`).

The custom SPFx bundle itself (`appcatalog/ClientSideAssets/479c037c-…/leaderboard-web-part_2882dd4a…js`) was attempted via raw `curl` and via the MCP fetch tool — both rejected with `403 Forbidden`. So the leaderboard web part had to be reconstructed from the SharePoint Modern Communication-site design language, not copied 1:1.

### Stack
- **HTML5** — semantic landmarks (`header`, `nav`, `main`, `section`, `footer`), inline SVGs for Fluent-style icons (no external icon font), accessible labels (`aria-*`, `visually-hidden`, `tabindex` on sortable headers).
- **CSS3** — CSS custom properties for the full Fluent UI v8 token set (`--themePrimary/DarkAlt/Dark/Light/Lighter…`, `--neutralPrimary/Secondary/…`, real `--depth-4/8/16/64` shadows, radii 2/4/8 px, Segoe UI Variable type stack), CSS Grid for the canvas / podium / KPI strip, Flexbox for command bars, sticky positioning for both the suite bar and the white site header, three responsive breakpoints (1024 / 768 / 480 px).
- **Vanilla JavaScript (ES2018, IIFE)** — no framework, no bundler. The script is also CommonJS-compatible (`module.exports`) so it can be `require()`-d from Node for snapshot generation and tests.
- **DiceBear `initials` API** — only external runtime asset (avatar SVGs); the page degrades gracefully if it is unreachable.

### Architecture inside `app.js`
- A deterministic `mulberry32` PRNG (seed `20251225`) feeds all generators so reloads always produce the same ranking.
- A small in-memory store (`STATE`) holds the employees, the current filters, the sort key/direction, and the pagination cursor.
- Render functions are pure: `renderPodium`, `renderStats`, `renderTable`, `renderPagination`, `renderComments`. They rebuild their slice of the DOM from `STATE` on each refresh — simple, debuggable, and avoids hand-managed diffing.
- Event handlers are attached once (`attachLeaderboardEvents`, `attachCommentsEvents`) using delegation on the table header and the comments list, so they survive every re-render.
- Comments are persisted in `localStorage` under `acme.leaderboard.comments.v1`; on first visit a curated set of seeded fake comments is shown.

### Quality gates
- `node --check app.js` for syntax.
- `ReadLints` across `index.html`, `styles.css`, `app.js` — clean.
- A throw-away **jsdom** smoke test loaded `index.html`, evaluated `app.js`, dispatched a manual `DOMContentLoaded`, and asserted 17 invariants (3 podium cards, 4 KPI cards, populated table, rank-badge / member-cell / trend-cell present, pagination buttons rendered, active period pivot is `Year`, ≥ 4 seeded comments, comments-count > 0, like icon present, comment dates match `\d{2}/\d{2}/\d{4}`, suite header / breadcrumb / title region present). All 17 passed; the test script and `node_modules` were removed afterwards to keep the deliverable clean.

## 3. Data-replacement strategy

The single most important constraint was **no real names, no real corporate signals**. The strategy:

1. **Branding** — every reference to the original tenant was replaced with the fictional brand `AcmeCorp` and the fictional site `AcmeCorp EDU`. The site header logo shows the made-up token `EDU`. The page title is `Company Leader Board 2025` (a generic phrase common to many leaderboards, not specific to any company).
2. **People** — the leaderboard does not contain a single real-person reference. Names are produced by a deterministic combinatorial generator from two pools defined at the top of `app.js`:
   - `FIRST_NAMES` — 60 short, internationally varied first names (`Alex`, `Maya`, `Liam`, `Sofia`, `Diego`, `Niko`, `Tess`, `Mei`, …).
   - `LAST_NAMES` — 50 short surnames (`Stone`, `Park`, `Nakamura`, `Reyes`, `Khoury`, `Voss`, `Pavlov`, …).
   - The generator deduplicates collisions and stops when the requested count is reached, so the same `(first, last)` pair never appears twice.
3. **Roles, levels, departments** — 26 generic, framework-agnostic role titles (`Frontend Engineer`, `Tech Lead`, `Product Designer`, `HR Specialist`, `Marketing Specialist`, `Sales Executive`, `Financial Analyst`, …) combined with 5 seniority levels (`Junior` … `Principal`), mapped via a fixed lookup table (`ROLE_TO_DEPT`) into 6 departments (`Engineering`, `Design`, `People`, `Marketing`, `Sales`, `Finance`).
4. **Offices** — generic city names with no organizational meaning (`Remote`, `Warsaw`, `Lisbon`, `Berlin`, `Yerevan`, `Tbilisi`).
5. **Points & trend** — random integers in a sane range (`points` 50–500, `trend` −25…+35) sampled from the same seeded RNG, then rebased per period (Week / Month / Quarter / Year) by a multiplicative factor so users observe the pivot reacting.
6. **Badges** — 7 invented program-style badges with their own color palette (`Mentor`, `Innovator`, `Top Contributor`, `Quick Learner`, `Helping Hand`, `Streak Master`, `Community Hero`). Each employee gets 0–3 of them, picked without repetition.
7. **Avatars** — DiceBear `initials` SVGs derived from the generated name + a Fluent-palette background color. They are visibly initials, never photos, so they cannot be confused for real people.
8. **Seeded comments** — 4 starter comments authored by characters drawn from the same fictional name pool (`Alex Stone`, `Maya Carter`, `Liam Park`, `Sofia Reyes`, `Theo Iyer`); their text is generic platform feedback that contains no real product, project, person or process.
9. **Breadcrumbs** — replaced with `AcmeCorp EDU › Site Pages › Company Leader Board 2025`.
10. **Web-part description** — explicitly carries the line *“All members, scores and badges on this page are **fictional** and generated for illustration only.”* directly under the leaderboard title.
11. **Page footer** — closes with the same disclaimer so the fictional nature of the data is reinforced even after the user scrolls past the leaderboard.

The deterministic generator means anyone can re-run the page (or the snapshot script in the README) and get the exact same 120-employee dataset, which is convenient for code review but never produces anything beyond the controlled fictional pools above. To regenerate or extend the dataset it is enough to edit those pools and / or change `SEED` at the top of `app.js`; nothing in the codebase reaches out to the original SharePoint site.

## 4. Outcome

- Visual chrome reproduces SharePoint Modern Communication site (themed M365 suite bar + white sticky site header with logo, title and horizontal navigation + full-width Title Region with gradient + colored Page Hierarchy strip + 1188 px-wide canvas).
- Leaderboard web part reproduces the Fluent UI **DetailsList** look (sticky sortable headers with up/down indicators, hover `neutralLighterAlt`, rank-badge pills in gold / silver / bronze, inline trend arrows, badge pills) plus a top-3 podium and a 4-card KPI strip with inline Fluent icons.
- Advanced Comments behaves like the SharePoint comments web part (32 px round avatars, like / reply / edit / delete, `DD/MM/YYYY` date format, threaded replies under a left rule, `localStorage` persistence).
- Zero real corporate data; the disclaimer is rendered both inside the leaderboard description and in the page footer.
- Static, dependency-free, ~80 KB total (excluding the reference `original.html`); opens with a double-click.
