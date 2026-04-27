# AcmeCorp EDU — Company Leader Board (static recreation)

A self-contained HTML / CSS / vanilla-JS recreation of a SharePoint Modern Communication Site page «Company Leader Board». The chrome, layout and visual language match the original SharePoint page (suite bar, white site header, full-width Title Region with gradient, colored Page Hierarchy strip, centered canvas, Advanced Comments web part), but **every piece of content is fictional** — names, departments, offices, scores, badges and avatars are all generated.

> All visible content on this page is fake and exists for UI demonstration only. Not affiliated with any real SharePoint site.

## Open

Double-click `index.html` in any modern browser (Edge, Chrome, Firefox, Safari). No build step, no server, no install required.

If you want to embed the page in a SharePoint site, copy the markup of `index.html` plus `styles.css` and `app.js` into a Modern Page through an Embed / Script Editor web part on a Communication site.

## Files

| Path | Purpose |
| --- | --- |
| `index.html` | Page markup: M365 suite bar, white site header, Title Region, Page Hierarchy breadcrumbs, Leaderboard web part, Advanced Comments. |
| `styles.css` | Fluent UI / SharePoint Modern tokens (themePrimary `#0078d4`, headerLayout = 3 Extended, headerEmphasis = 0 White), Fluent depth elevations and responsive grid. |
| `app.js` | Deterministic data generator (mulberry32 seeded RNG), ranking logic, filters, sorting, pagination, comments with localStorage persistence. |
| `data/leaderboard.json` | Static snapshot of the generated 120-employee dataset for reference / external editing. |
| `original.html` | Original SharePoint page source (View Source). Reference only — not used at runtime. |

## Page anatomy (1:1 with the original SharePoint Modern page)

1. **M365 suite bar** — themed top navigation (waffle launcher, SharePoint brand, search, notifications / settings / help icons, profile avatar).
2. **Site header** (Communication site, `headerLayout: 3` Extended, `headerEmphasis: 0` White) — white sticky band with site logo «EDU», site title «AcmeCorp EDU», horizontal navigation (Home / Documents / Pages / Programs / Members) and Edit / Share / Follow actions.
3. **Title Region** (`layoutType: FullWidthImage`, `enableGradientEffect: true`) — full-width 324 px hero with bottom-left title «Company Leader Board 2025» and a Fluent gradient overlay.
4. **Page Hierarchy** web part (`isFullWidth: true`, `isHeaderColored: true`) — colored full-width strip rendered with `themeDark`, items `AcmeCorp EDU › Site Pages › Company Leader Board 2025`.
5. **AcmeCorp EDU — Leaderboard** web part (custom SPFx, `sectionFactor: 12`, max-width 1188 px) — recreates the SPFx-rendered visual:
   - top-3 podium with rank-tinted top borders (gold / silver / bronze) and points;
   - KPI strip of 4 Fluent stat cards with inline icons (Total participants, Average score, Top score, Leader of the week);
   - command bar with search, Department select, Office select and period pivots (Week / Month / Quarter / Year);
   - **Fluent DetailsList** with sortable columns (#, Member, Position, Department, Office, Points, Δ trend, Badges), 25 rows per page;
   - pagination with previous / next + first-/-last-/window page buttons.
6. **Advanced Comments** web part (`datetimeFormat: DD/MM/YYYY`, `roundProfilePictures: true`) — supports add, reply, edit, delete and upvote. Persists in `localStorage` under `acme.leaderboard.comments.v1`.

## Fake data

- 120 employees generated with a seeded RNG (`mulberry32`, seed `20251225`) so reloads are reproducible.
- Pools (first names, last names, roles, levels, departments, offices, badges) live at the top of `app.js` and can be edited directly.
- A static snapshot of the same 120 entries is also available at `data/leaderboard.json`.

To regenerate the JSON snapshot after editing the pools or the seed:

```powershell
node -e "const { generateEmployees } = require('./app.js'); require('fs').writeFileSync('data/leaderboard.json', JSON.stringify({ generatedAt: new Date().toISOString(), seed: 20251225, count: 120, employees: generateEmployees(120) }, null, 2))"
```

## Notes about fidelity to the original page

- The original Vention EDU Leaderboard SPFx bundle is hosted on the tenant App Catalog (`appcatalog/ClientSideAssets/479c037c-…/leaderboard-web-part_2882dd4a…js`) and is gated by SharePoint authentication (anonymous access returns `403 Forbidden`). We could not download it, so the visual recreation of the leaderboard web part is built from the SharePoint Modern Communication-site design language and the page-context metadata (theme, layout, web-part properties) extracted from `original.html`.
- Theme colors, type scale, depth elevations, hover / focus states, breadcrumb chevrons, Pivot underline, DetailsList column header sort indicators and rounded comment avatars all match Fluent UI v8 / SharePoint Modern defaults.
- Web-part fabric icons referenced in the manifest (`Page Hierarchy: CompassNW`, `Vention EDU - Leaderboard: Page`, `Advanced Comments: FileComment`) are reproduced inline as small SVGs.

## External resources

- Fonts: Segoe UI Variable / Segoe UI (system on Windows; falls back to Apple system fonts on macOS / iOS and Roboto on Android / Linux).
- Avatars: [DiceBear](https://www.dicebear.com/) initials API (`api.dicebear.com`). The page works offline but avatars will appear blank; replace with your own asset URLs in `app.js` (`avatarUrl`) if needed.

## Reset comments

To clear locally-stored comments, open DevTools → **Application → Local Storage** → remove the `acme.leaderboard.comments.v1` key (or run `localStorage.removeItem('acme.leaderboard.comments.v1')` in the console).
