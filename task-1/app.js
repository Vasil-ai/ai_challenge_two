/* eslint-disable no-prototype-builtins */
/**
 * AcmeCorp Leader Board - static recreation.
 *
 * Self-contained: no fetch, no build step. All data is generated deterministically
 * from a fixed seed so reloading the page always yields the same ranking.
 *
 * The script is also Node-compatible: when loaded as a CommonJS module it exports
 * `generateEmployees` so a snapshot can be dumped to data/leaderboard.json.
 */
(function () {
  'use strict';

  // ==========================================================================
  // Deterministic RNG (mulberry32) - keeps the dataset stable across reloads.
  // ==========================================================================
  function mulberry32(seed) {
    let s = seed >>> 0;
    return function () {
      s = (s + 0x6D2B79F5) >>> 0;
      let t = Math.imul(s ^ (s >>> 15), 1 | s);
      t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  }

  const SEED = 20251225;
  let rand = mulberry32(SEED);
  const pick = (arr) => arr[Math.floor(rand() * arr.length)];
  const randInt = (min, max) => Math.floor(rand() * (max - min + 1)) + min;

  // ==========================================================================
  // Fake data pools (NO real names, brands, or photos).
  // ==========================================================================
  const FIRST_NAMES = [
    'Alex', 'Maya', 'Liam', 'Nora', 'Sofia', 'Diego', 'Aria', 'Theo', 'Luna', 'Mateo',
    'Ivy', 'Hugo', 'Zara', 'Ezra', 'Mila', 'Felix', 'Rhea', 'Arlo', 'Cleo', 'Sage',
    'Niko', 'Tess', 'Jasper', 'Iris', 'Bruno', 'Amara', 'Otis', 'Vera', 'Caleb', 'Stella',
    'Rocco', 'Maeve', 'Kian', 'Linnea', 'Asher', 'Elara', 'Quinn', 'Wren', 'Roman', 'Ines',
    'Nasir', 'Rhona', 'Marek', 'Aiyla', 'Otto', 'Lyra', 'Soren', 'Nadia', 'Reza', 'Esme',
    'Idris', 'Pia', 'Cyril', 'Amaya', 'Joran', 'Suri', 'Ade', 'Noor', 'Bo', 'Mei',
  ];

  const LAST_NAMES = [
    'Stone', 'Carter', 'Park', 'Hale', 'Nakamura', 'Becker', 'Quinn', 'Vega', 'Reyes', 'Kim',
    'Voss', 'Holt', 'Finch', 'Greer', 'Iyer', 'Khoury', 'Marsh', 'Ortiz', 'Pham', 'Rao',
    'Saito', 'Tate', 'Ueno', 'Vance', 'Wirth', 'Yates', 'Zima', 'Brand', 'Cole', 'Doyle',
    'Eklund', 'Fontana', 'Gallo', 'Hartley', 'Ibarra', 'Jansen', 'Kovac', 'Lange', 'Mahler', 'Norris',
    'Okafor', 'Pavlov', 'Ramos', 'Salim', 'Tanaka', 'Uribe', 'Volkov', 'Wexler', 'Xu', 'Young',
  ];

  const ROLES = [
    'Frontend Engineer', 'Backend Engineer', 'Full-stack Engineer',
    'QA Engineer', 'DevOps Engineer', 'Mobile Engineer', 'Data Engineer',
    'Tech Lead', 'Engineering Manager',
    'UX Designer', 'UI Designer', 'Product Designer',
    'Product Manager', 'Project Manager',
    'HR Specialist', 'Recruiter', 'People Partner',
    'Marketing Specialist', 'Content Strategist', 'Brand Manager',
    'Sales Executive', 'Account Manager', 'Customer Success Manager',
    'Financial Analyst', 'Business Analyst', 'Data Analyst',
  ];

  const LEVELS = ['Junior', 'Middle', 'Senior', 'Lead', 'Principal'];

  const DEPARTMENTS = ['Engineering', 'Design', 'People', 'Marketing', 'Sales', 'Finance'];

  const ROLE_TO_DEPT = {
    'Frontend Engineer':    'Engineering',
    'Backend Engineer':     'Engineering',
    'Full-stack Engineer':  'Engineering',
    'QA Engineer':          'Engineering',
    'DevOps Engineer':      'Engineering',
    'Mobile Engineer':      'Engineering',
    'Data Engineer':        'Engineering',
    'Tech Lead':            'Engineering',
    'Engineering Manager':  'Engineering',
    'UX Designer':          'Design',
    'UI Designer':          'Design',
    'Product Designer':     'Design',
    'Product Manager':      'Engineering',
    'Project Manager':      'Engineering',
    'HR Specialist':        'People',
    'Recruiter':            'People',
    'People Partner':       'People',
    'Marketing Specialist': 'Marketing',
    'Content Strategist':   'Marketing',
    'Brand Manager':        'Marketing',
    'Sales Executive':      'Sales',
    'Account Manager':      'Sales',
    'Customer Success Manager': 'Sales',
    'Financial Analyst':    'Finance',
    'Business Analyst':     'Finance',
    'Data Analyst':         'Finance',
  };

  const OFFICES = ['Remote', 'Warsaw', 'Lisbon', 'Berlin', 'Yerevan', 'Tbilisi'];

  const BADGES = [
    { id: 'mentor',          label: 'Mentor',          color: '#5c2d91' },
    { id: 'innovator',       label: 'Innovator',       color: '#0078d4' },
    { id: 'top-contributor', label: 'Top Contributor', color: '#d83b01' },
    { id: 'quick-learner',   label: 'Quick Learner',   color: '#107c10' },
    { id: 'helping-hand',    label: 'Helping Hand',    color: '#008272' },
    { id: 'streak-master',   label: 'Streak Master',   color: '#b4009e' },
    { id: 'community-hero',  label: 'Community Hero',  color: '#e3008c' },
  ];

  const AVATAR_COLORS = [
    '0078d4', '5c2d91', 'd83b01', '107c10', '008272', 'b4009e', 'ca5010', '038387', '8764b8',
  ];

  // ==========================================================================
  // Employee generator
  // ==========================================================================
  function generateEmployees(count) {
    rand = mulberry32(SEED); // reset RNG so the dataset is reproducible
    const total = count || 120;
    const used = new Set();
    const employees = [];

    while (employees.length < total) {
      const first = pick(FIRST_NAMES);
      const last  = pick(LAST_NAMES);
      const fullName = first + ' ' + last;
      if (used.has(fullName)) continue;
      used.add(fullName);

      const role = pick(ROLES);
      const level = pick(LEVELS);
      const department = ROLE_TO_DEPT[role] || pick(DEPARTMENTS);
      const office = pick(OFFICES);
      const points = randInt(50, 500);
      const trend  = randInt(-25, 35);

      const badgeCount = randInt(0, 3);
      const badgePool  = BADGES.slice();
      const badges = [];
      for (let i = 0; i < badgeCount; i++) {
        const idx = Math.floor(rand() * badgePool.length);
        badges.push(badgePool.splice(idx, 1)[0]);
      }

      employees.push({
        id: 'emp-' + (employees.length + 1),
        name: fullName,
        position: level + ' ' + role,
        department,
        office,
        points,
        trend,
        badges,
        avatarColor: pick(AVATAR_COLORS),
      });
    }

    employees.sort(function (a, b) { return b.points - a.points; });
    employees.forEach(function (e, i) { e.rank = i + 1; });
    return employees;
  }

  // ==========================================================================
  // Node export hook (used by tools/dump-data.js to materialize the JSON file).
  // The DOM-bound init() below is skipped when there's no document.
  // ==========================================================================
  if (typeof module !== 'undefined' && module.exports) {
    module.exports = { generateEmployees, DEPARTMENTS, OFFICES };
    return;
  }

  // ==========================================================================
  // Browser-only code from this point on.
  // ==========================================================================
  const $  = (sel, ctx) => (ctx || document).querySelector(sel);
  const $$ = (sel, ctx) => Array.prototype.slice.call((ctx || document).querySelectorAll(sel));

  function escape(s) {
    return String(s == null ? '' : s)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function avatarUrl(person, size) {
    const seed = encodeURIComponent(person.name);
    const bg = person.avatarColor || '0078d4';
    const px = size || 64;
    return 'https://api.dicebear.com/7.x/initials/svg' +
      '?seed=' + seed +
      '&backgroundColor=' + bg +
      '&fontSize=42&radius=50&size=' + px;
  }

  function formatDate(iso) {
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return '';
    const dd = String(d.getDate()).padStart(2, '0');
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const yyyy = d.getFullYear();
    return dd + '/' + mm + '/' + yyyy;
  }

  function rankBadgeClass(rank) {
    if (rank === 1) return 'rank-badge rank-badge--top1';
    if (rank === 2) return 'rank-badge rank-badge--top2';
    if (rank === 3) return 'rank-badge rank-badge--top3';
    return 'rank-badge';
  }

  // Inline Fluent-style icons used in the leaderboard cells / KPI strip.
  const ICONS = {
    arrowUp:   '<svg viewBox="0 0 16 16" width="12" height="12" aria-hidden="true"><path fill="currentColor" d="M8 3.2 12.8 8H10v4.8H6V8H3.2L8 3.2Z"/></svg>',
    arrowDown: '<svg viewBox="0 0 16 16" width="12" height="12" aria-hidden="true"><path fill="currentColor" d="M8 12.8 3.2 8H6V3.2h4V8h2.8L8 12.8Z"/></svg>',
    flat:      '<svg viewBox="0 0 16 16" width="12" height="12" aria-hidden="true"><path fill="currentColor" d="M3 7.25h10v1.5H3z"/></svg>',
    people:    '<svg viewBox="0 0 20 20" width="20" height="20" aria-hidden="true"><path fill="currentColor" d="M7 9a3 3 0 1 1 0-6 3 3 0 0 1 0 6Zm6 1a2.5 2.5 0 1 1 0-5 2.5 2.5 0 0 1 0 5Zm-6 1c-2.8 0-5 1.5-5 4v2h10v-2c0-2.5-2.2-4-5-4Zm6 0c-.7 0-1.4.1-2 .3 1.3 1 2 2.4 2 3.7v2h6v-2c0-2.2-1.7-4-6-4Z"/></svg>',
    chart:     '<svg viewBox="0 0 20 20" width="20" height="20" aria-hidden="true"><path fill="currentColor" d="M3 3h2v14H3zm5 6h2v8H8zm5-3h2v11h-2zm5-2h2v13h-2z"/></svg>',
    trophy:    '<svg viewBox="0 0 20 20" width="20" height="20" aria-hidden="true"><path fill="currentColor" d="M5 3h10v2h2a2 2 0 0 1-2 4 5 5 0 0 1-4 3.9V15h2v2H7v-2h2v-2.1A5 5 0 0 1 5 9a2 2 0 0 1-2-4h2V3Zm0 3H4a1 1 0 0 0 1 2V6Zm10 0v2a1 1 0 0 0 1-2h-1Z"/></svg>',
    star:      '<svg viewBox="0 0 20 20" width="20" height="20" aria-hidden="true"><path fill="currentColor" d="M10 2.5l2.5 5 5.5.8-4 3.9.9 5.5L10 15l-4.9 2.7.9-5.5-4-3.9 5.5-.8L10 2.5Z"/></svg>',
    likeUp:    '<svg viewBox="0 0 16 16" width="14" height="14" aria-hidden="true"><path fill="none" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round" d="M5.2 13.5h-1A1.2 1.2 0 0 1 3 12.3V8.5a1.2 1.2 0 0 1 1.2-1.2h1m0 6.2V7.3m0 6.2h6a1.5 1.5 0 0 0 1.4-1l1.3-3.6a1.5 1.5 0 0 0-1.4-2H9.8L10 4.5a1.4 1.4 0 0 0-2.4-1l-2.4 3.7"/></svg>',
  };

  // ==========================================================================
  // Leaderboard state + actions
  // ==========================================================================
  const STATE = {
    employees: [],
    filtered:  [],
    filters:   { search: '', department: 'all', office: 'all', period: 'year' },
    sort:      { key: 'rank', dir: 'asc' },
    page:      1,
    pageSize:  25,
  };

  /**
   * The "period" tabs are illustrative only - the demo dataset has no time
   * series. We re-tint points slightly per period so users see filters react,
   * but the ranking stays consistent.
   */
  const PERIOD_FACTORS = { week: 0.18, month: 0.55, quarter: 0.85, year: 1 };

  function applyFilters() {
    const f = STATE.filters;
    const factor = PERIOD_FACTORS[f.period] || 1;

    let list = STATE.employees.map(function (e) {
      return Object.assign({}, e, {
        points: Math.max(1, Math.round(e.points * factor)),
        trend:  Math.round(e.trend * Math.max(0.4, factor)),
      });
    });

    if (f.search) {
      const q = f.search.toLowerCase();
      list = list.filter(function (e) {
        return (
          e.name.toLowerCase().indexOf(q) !== -1 ||
          e.position.toLowerCase().indexOf(q) !== -1 ||
          e.department.toLowerCase().indexOf(q) !== -1 ||
          e.office.toLowerCase().indexOf(q) !== -1
        );
      });
    }
    if (f.department !== 'all') list = list.filter(function (e) { return e.department === f.department; });
    if (f.office !== 'all')     list = list.filter(function (e) { return e.office === f.office; });

    list.sort(function (a, b) { return b.points - a.points; });
    list.forEach(function (e, i) { e.rank = i + 1; });

    const k = STATE.sort.key;
    const dir = STATE.sort.dir === 'asc' ? 1 : -1;
    if (k !== 'rank' || dir !== 1) {
      list.sort(function (a, b) {
        const av = a[k];
        const bv = b[k];
        if (typeof av === 'string' && typeof bv === 'string') return av.localeCompare(bv) * dir;
        return ((av || 0) - (bv || 0)) * dir;
      });
    }

    STATE.filtered = list;
  }

  // ==========================================================================
  // Rendering
  // ==========================================================================
  function renderPodium() {
    const node = $('#podium');
    const top3 = STATE.employees.slice(0, 3);

    node.innerHTML = top3.map(function (e) {
      if (!e) return '';
      return [
        '<article class="podium-card" data-rank="', e.rank, '">',
        '  <span class="podium-medal" aria-hidden="true">#', e.rank, '</span>',
        '  <img class="podium-avatar" src="', avatarUrl(e, 96), '" alt="" loading="lazy">',
        '  <h3 class="podium-name">', escape(e.name), '</h3>',
        '  <p class="podium-position">', escape(e.position), '</p>',
        '  <span class="podium-points">', e.points.toLocaleString(), '</span>',
        '  <span class="podium-points-label">points</span>',
        '</article>',
      ].join('');
    }).join('');
  }

  function renderStats() {
    const list = STATE.employees;
    if (!list.length) return;
    const total = list.length;
    const avg = Math.round(list.reduce(function (s, e) { return s + e.points; }, 0) / total);
    const top = list[0];
    const weekLeader = list.slice().sort(function (a, b) { return b.trend - a.trend; })[0];

    const cards = [
      { icon: ICONS.people, label: 'Total participants', value: total.toLocaleString() },
      { icon: ICONS.chart,  label: 'Average score',      value: avg.toLocaleString() + ' pts' },
      { icon: ICONS.trophy, label: 'Top score',          value: top.points.toLocaleString() + ' pts' },
      { icon: ICONS.star,   label: 'Leader of the week', value: '+' + weekLeader.trend + ' pts' },
    ];

    $('#stats').innerHTML = cards.map(function (c) {
      return [
        '<div class="stat-card">',
        '  <span class="stat-card__icon" aria-hidden="true">', c.icon, '</span>',
        '  <div>',
        '    <span class="stat-card__value">', escape(c.value), '</span>',
        '    <span class="stat-card__label">', escape(c.label), '</span>',
        '  </div>',
        '</div>',
      ].join('');
    }).join('');
  }

  function renderTable() {
    const tbody = $('#ranking-tbody');
    const empty = $('#ranking-empty');
    const list = STATE.filtered;
    const pageNode = $('#pagination');

    if (!list.length) {
      tbody.innerHTML = '';
      empty.hidden = false;
      pageNode.innerHTML = '';
      return;
    }
    empty.hidden = true;

    const pages = Math.max(1, Math.ceil(list.length / STATE.pageSize));
    if (STATE.page > pages) STATE.page = pages;
    const start = (STATE.page - 1) * STATE.pageSize;
    const slice = list.slice(start, start + STATE.pageSize);

    tbody.innerHTML = slice.map(function (e) {
      const trendCell = e.trend > 0
        ? '<span class="trend-cell trend-cell--up">'   + ICONS.arrowUp   + e.trend + '</span>'
        : e.trend < 0
          ? '<span class="trend-cell trend-cell--down">' + ICONS.arrowDown + Math.abs(e.trend) + '</span>'
          : '<span class="trend-cell trend-cell--flat">' + ICONS.flat     + '0</span>';

      const visibleBadges = (e.badges || []).slice(0, 2).map(function (b) {
        return '<span class="badge" title="' + escape(b.label) + '">' + escape(b.label) + '</span>';
      }).join('');
      const extraBadge = (e.badges || []).length > 2
        ? '<span class="badge" title="' + (e.badges.length - 2) + ' more">+' + (e.badges.length - 2) + '</span>'
        : '';

      return [
        '<tr>',
        '  <td class="details-list__col-rank"><span class="', rankBadgeClass(e.rank), '">', e.rank, '</span></td>',
        '  <td class="details-list__col-name">',
        '    <div class="member-cell">',
        '      <img class="member-cell__avatar" src="', avatarUrl(e, 48), '" alt="" loading="lazy">',
        '      <div>',
        '        <div class="member-cell__name">', escape(e.name), '</div>',
        '        <div class="member-cell__sub">', escape(e.office), '</div>',
        '      </div>',
        '    </div>',
        '  </td>',
        '  <td class="details-list__col-position">',   escape(e.position),   '</td>',
        '  <td class="details-list__col-department">', escape(e.department), '</td>',
        '  <td class="details-list__col-office">',     escape(e.office),     '</td>',
        '  <td class="points-cell">', e.points.toLocaleString(), '</td>',
        '  <td class="trend-cell">',  trendCell, '</td>',
        '  <td class="details-list__col-badges"><span class="badges">', visibleBadges, extraBadge, '</span></td>',
        '</tr>',
      ].join('');
    }).join('');

    renderPagination(list.length, pages);
  }

  function renderPagination(total, pages) {
    const node = $('#pagination');
    if (pages <= 1) {
      node.innerHTML = '<span class="pagination__info">Showing ' + total + ' of ' + total + '</span>';
      return;
    }
    const cur = STATE.page;
    const buttons = [];
    buttons.push('<button type="button" class="page-btn" data-page="prev"' + (cur === 1 ? ' disabled' : '') + ' aria-label="Previous page">\u2039</button>');
    for (let p = 1; p <= pages; p++) {
      if (p === 1 || p === pages || (p >= cur - 2 && p <= cur + 2)) {
        buttons.push('<button type="button" class="page-btn ' + (p === cur ? 'is-active' : '') + '" data-page="' + p + '"' + (p === cur ? ' aria-current="page"' : '') + '>' + p + '</button>');
      } else if (p === cur - 3 || p === cur + 3) {
        buttons.push('<span class="page-btn" aria-hidden="true" disabled>\u2026</span>');
      }
    }
    buttons.push('<button type="button" class="page-btn" data-page="next"' + (cur === pages ? ' disabled' : '') + ' aria-label="Next page">\u203A</button>');

    const start = (cur - 1) * STATE.pageSize + 1;
    const end = Math.min(cur * STATE.pageSize, total);
    node.innerHTML =
      '<span class="pagination__info">Showing ' + start + '\u2013' + end + ' of ' + total + '</span>' +
      '<div class="pagination__pages">' + buttons.join('') + '</div>';
  }

  function refresh(resetPage) {
    if (resetPage) STATE.page = 1;
    applyFilters();
    renderTable();
    renderStats();
  }

  // ==========================================================================
  // Filters / sort wiring
  // ==========================================================================
  function populateFilterOptions() {
    const dep = $('#filter-department');
    DEPARTMENTS.forEach(function (d) {
      const opt = document.createElement('option');
      opt.value = d; opt.textContent = d;
      dep.appendChild(opt);
    });
    const off = $('#filter-office');
    OFFICES.forEach(function (o) {
      const opt = document.createElement('option');
      opt.value = o; opt.textContent = o;
      off.appendChild(opt);
    });
  }

  function attachLeaderboardEvents() {
    $('#filter-search').addEventListener('input', function (e) {
      STATE.filters.search = e.target.value.trim();
      refresh(true);
    });
    $('#filter-department').addEventListener('change', function (e) {
      STATE.filters.department = e.target.value;
      refresh(true);
    });
    $('#filter-office').addEventListener('change', function (e) {
      STATE.filters.office = e.target.value;
      refresh(true);
    });

    $$('.pivot').forEach(function (btn) {
      btn.addEventListener('click', function () {
        $$('.pivot').forEach(function (b) {
          b.classList.remove('is-active');
          b.setAttribute('aria-selected', 'false');
        });
        btn.classList.add('is-active');
        btn.setAttribute('aria-selected', 'true');
        STATE.filters.period = btn.dataset.period;
        refresh(true);
      });
    });

    const headers = $$('#ranking-table thead th[data-sort]');
    headers.forEach(function (th) {
      const handler = function () {
        const key = th.dataset.sort;
        if (STATE.sort.key === key) {
          STATE.sort.dir = STATE.sort.dir === 'asc' ? 'desc' : 'asc';
        } else {
          STATE.sort.key = key;
          STATE.sort.dir = (key === 'rank' || key === 'name' || key === 'position' || key === 'department' || key === 'office')
            ? 'asc'
            : 'desc';
        }
        headers.forEach(function (t) { t.removeAttribute('aria-sort'); });
        th.setAttribute('aria-sort', STATE.sort.dir === 'asc' ? 'ascending' : 'descending');
        refresh();
      };
      th.addEventListener('click', handler);
      th.addEventListener('keydown', function (e) {
        if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); handler(); }
      });
    });

    $('#pagination').addEventListener('click', function (e) {
      const btn = e.target.closest('.page-btn');
      if (!btn || btn.disabled) return;
      const p = btn.dataset.page;
      const pages = Math.ceil(STATE.filtered.length / STATE.pageSize);
      if (p === 'prev')      STATE.page = Math.max(1, STATE.page - 1);
      else if (p === 'next') STATE.page = Math.min(pages, STATE.page + 1);
      else                   STATE.page = parseInt(p, 10);
      renderTable();
    });
  }

  // ==========================================================================
  // Comments (Advanced Comments web part)
  // ==========================================================================
  const COMMENTS_KEY = 'acme.leaderboard.comments.v1';
  const CURRENT_USER = { name: 'Demo User', avatarColor: '038387' };

  function cid(prefix) { return (prefix || 'c') + '-' + Math.random().toString(36).slice(2, 9) + Date.now().toString(36).slice(-3); }
  function nowMinusDays(n) {
    const d = new Date();
    d.setDate(d.getDate() - n);
    return d.toISOString();
  }

  function seedComments() {
    return [
      {
        id: cid('c'), author: 'Alex Stone', avatarColor: '0078d4',
        text: 'Big shout-out to everyone keeping the streak this month \u2014 you make this fun!',
        date: nowMinusDays(2), likes: 14, liked: false,
        replies: [
          {
            id: cid('r'), author: 'Maya Carter', avatarColor: '5c2d91',
            text: 'Couldn\u2019t agree more. Looking forward to the Q3 challenge!',
            date: nowMinusDays(1), likes: 4, liked: false,
          },
        ],
      },
      {
        id: cid('c'), author: 'Liam Park', avatarColor: 'd83b01',
        text: 'Tip: pair the weekly knowledge sessions with the bonus quiz, it doubles the points.',
        date: nowMinusDays(5), likes: 9, liked: false, replies: [],
      },
      {
        id: cid('c'), author: 'Sofia Reyes', avatarColor: '107c10',
        text: 'Could we get a filter by office in the next iteration? Would be amazing.',
        date: nowMinusDays(8), likes: 22, liked: false, replies: [],
      },
      {
        id: cid('c'), author: 'Theo Iyer', avatarColor: 'b4009e',
        text: 'Mentor program participants - remember to log your sessions, they count double this month.',
        date: nowMinusDays(12), likes: 6, liked: false, replies: [],
      },
    ];
  }

  function loadComments() {
    try {
      const raw = localStorage.getItem(COMMENTS_KEY);
      if (raw) {
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed)) return parsed;
      }
    } catch (_) { /* ignore */ }
    return seedComments();
  }

  function saveComments(list) {
    try { localStorage.setItem(COMMENTS_KEY, JSON.stringify(list)); } catch (_) { /* ignore */ }
  }

  const COMMENTS_STATE = { list: [] };

  function commentTpl(c, isReply) {
    const avatar = 'https://api.dicebear.com/7.x/initials/svg?seed=' +
      encodeURIComponent(c.author) + '&backgroundColor=' + (c.avatarColor || '0078d4') +
      '&fontSize=42&radius=50';
    const replyButton = !isReply ? '<button type="button" class="comment__action" data-action="reply">Reply</button>' : '';
    const repliesHtml = (c.replies && c.replies.length)
      ? '<ul class="comment__replies">' +
          c.replies.map(function (r) { return commentTpl(r, true); }).join('') +
        '</ul>'
      : '';

    return [
      '<li class="comment" data-id="', c.id, '">',
      '  <img class="comment__avatar" src="', avatar, '" alt="" loading="lazy">',
      '  <div class="comment__body">',
      '    <div class="comment__head">',
      '      <span class="comment__author">', escape(c.author), '</span>',
      '      <span class="comment__date">', formatDate(c.date), '</span>',
      '    </div>',
      '    <p class="comment__text">', escape(c.text), '</p>',
      '    <div class="comment__actions">',
      '      <button type="button" class="comment__action ', c.liked ? 'is-active' : '', '" data-action="like" aria-pressed="', c.liked ? 'true' : 'false', '" aria-label="Like">',
              ICONS.likeUp,
      '        <span class="comment__like-count">', c.likes, '</span>',
      '      </button>',
             replyButton,
      '      <button type="button" class="comment__action" data-action="edit">Edit</button>',
      '      <button type="button" class="comment__action" data-action="delete">Delete</button>',
      '    </div>',
           repliesHtml,
      '  </div>',
      '</li>',
    ].join('');
  }

  function totalCommentCount() {
    return COMMENTS_STATE.list.reduce(function (s, c) {
      return s + 1 + ((c.replies && c.replies.length) || 0);
    }, 0);
  }

  function renderComments() {
    $('#comments-count').textContent = totalCommentCount();
    $('#comments-list').innerHTML = COMMENTS_STATE.list.map(function (c) { return commentTpl(c, false); }).join('');
  }

  function findComment(id) {
    for (let i = 0; i < COMMENTS_STATE.list.length; i++) {
      const c = COMMENTS_STATE.list[i];
      if (c.id === id) return { comment: c, parent: null };
      if (c.replies) {
        const r = c.replies.find(function (x) { return x.id === id; });
        if (r) return { comment: r, parent: c };
      }
    }
    return null;
  }

  function startEdit(li, comment) {
    const body = $('.comment__body', li);
    const textNode = $('.comment__text', body);
    if (!textNode || $('.comment__edit-textarea', body)) return;

    const editor = document.createElement('textarea');
    editor.className = 'comment__edit-textarea';
    editor.value = comment.text;
    editor.maxLength = 800;

    const actions = document.createElement('div');
    actions.className = 'reply-form__actions';
    actions.innerHTML =
      '<button type="button" class="btn btn--ghost" data-edit-action="cancel">Cancel</button>' +
      '<button type="button" class="btn btn--primary" data-edit-action="save">Save</button>';

    textNode.replaceWith(editor);
    editor.after(actions);
    editor.focus();
    editor.setSelectionRange(editor.value.length, editor.value.length);

    actions.addEventListener('click', function (e) {
      const btn = e.target.closest('[data-edit-action]');
      if (!btn) return;
      if (btn.dataset.editAction === 'save') {
        const next = editor.value.trim();
        if (next) {
          comment.text = next;
          saveComments(COMMENTS_STATE.list);
        }
      }
      renderComments();
    });
  }

  function startReply(li, comment) {
    const body = $('.comment__body', li);
    if ($('.reply-form', body)) return;

    const form = document.createElement('form');
    form.className = 'reply-form';
    form.innerHTML =
      '<img class="reply-form__avatar" src="https://api.dicebear.com/7.x/initials/svg?seed=' +
        encodeURIComponent(CURRENT_USER.name) + '&backgroundColor=' + CURRENT_USER.avatarColor +
        '&fontSize=42&radius=50" alt="" aria-hidden="true">' +
      '<div class="reply-form__field">' +
      '  <textarea placeholder="Write a reply\u2026" maxlength="800" required></textarea>' +
      '  <div class="reply-form__actions">' +
      '    <button type="button" class="btn btn--ghost" data-reply-action="cancel">Cancel</button>' +
      '    <button type="submit" class="btn btn--primary">Reply</button>' +
      '  </div>' +
      '</div>';

    body.appendChild(form);
    const ta = $('textarea', form);
    ta.focus();

    form.addEventListener('submit', function (e) {
      e.preventDefault();
      const text = ta.value.trim();
      if (!text) return;
      comment.replies = comment.replies || [];
      comment.replies.push({
        id: cid('r'),
        author: CURRENT_USER.name,
        avatarColor: CURRENT_USER.avatarColor,
        text,
        date: new Date().toISOString(),
        likes: 0, liked: false,
      });
      saveComments(COMMENTS_STATE.list);
      renderComments();
    });
    form.addEventListener('click', function (e) {
      const btn = e.target.closest('[data-reply-action="cancel"]');
      if (btn) { e.preventDefault(); renderComments(); }
    });
  }

  function attachCommentsEvents() {
    const form = $('#comment-form');
    const input = $('#comment-input');
    const cancel = $('#comment-cancel');

    form.addEventListener('submit', function (e) {
      e.preventDefault();
      const text = input.value.trim();
      if (!text) return;
      COMMENTS_STATE.list.unshift({
        id: cid('c'),
        author: CURRENT_USER.name,
        avatarColor: CURRENT_USER.avatarColor,
        text,
        date: new Date().toISOString(),
        likes: 0, liked: false,
        replies: [],
      });
      input.value = '';
      saveComments(COMMENTS_STATE.list);
      renderComments();
    });
    cancel.addEventListener('click', function () { input.value = ''; });

    $('#comments-list').addEventListener('click', function (e) {
      const btn = e.target.closest('button[data-action]');
      if (!btn) return;
      const li = btn.closest('.comment');
      if (!li) return;
      const id = li.dataset.id;
      const found = findComment(id);
      if (!found) return;
      const c = found.comment;
      const action = btn.dataset.action;

      if (action === 'like') {
        if (c.liked) { c.likes = Math.max(0, c.likes - 1); c.liked = false; }
        else         { c.likes += 1;                       c.liked = true; }
        saveComments(COMMENTS_STATE.list);
        renderComments();
      } else if (action === 'delete') {
        if (!window.confirm('Delete this comment?')) return;
        if (found.parent) {
          found.parent.replies = found.parent.replies.filter(function (r) { return r.id !== id; });
        } else {
          COMMENTS_STATE.list = COMMENTS_STATE.list.filter(function (x) { return x.id !== id; });
        }
        saveComments(COMMENTS_STATE.list);
        renderComments();
      } else if (action === 'edit') {
        startEdit(li, c);
      } else if (action === 'reply') {
        startReply(li, c);
      }
    });
  }

  // ==========================================================================
  // Init
  // ==========================================================================
  function init() {
    STATE.employees = generateEmployees(120);
    populateFilterOptions();
    renderPodium();
    refresh();
    attachLeaderboardEvents();

    COMMENTS_STATE.list = loadComments();
    renderComments();
    attachCommentsEvents();
  }

  if (typeof document !== 'undefined') {
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', init);
    } else {
      init();
    }
  }
})();
