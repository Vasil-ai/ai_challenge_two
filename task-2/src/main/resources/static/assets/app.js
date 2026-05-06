const app = document.getElementById('app');
const nav = document.getElementById('nav');

async function api(path, opts = {}) {
  const headers = { ...(opts.headers || {}) };
  if (opts.body && typeof opts.body === 'object' && !(opts.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }
  const r = await fetch(path, {
    credentials: 'include',
    ...opts,
    headers,
    body:
      opts.body && typeof opts.body === 'object' && !(opts.body instanceof FormData)
        ? JSON.stringify(opts.body)
        : opts.body,
  });
  if (path === '/api/me' && r.status === 401) return null;
  if (!r.ok) {
    let msg = r.statusText;
    try {
      const j = await r.json();
      if (j.error) msg = j.error;
    } catch (_) {}
    throw new Error(msg);
  }
  if (r.status === 204) return null;
  const ct = r.headers.get('content-type');
  if (ct && ct.includes('application/json')) return r.json();
  return r.text();
}

let user = undefined;

async function refreshUser() {
  try {
    user = await api('/api/me');
  } catch {
    user = null;
  }
}

function renderNav() {
  nav.innerHTML = '';
  const add = (html) => {
    const d = document.createElement('span');
    d.innerHTML = html;
    nav.appendChild(d.firstElementChild || d);
  };
  add('<a href="#/explore"><strong>Events</strong></a>');
  add('<a href="#/explore">Explore</a>');
  if (user) {
    add('<a href="#/tickets">My Tickets</a>');
    add('<a href="#/mine">My Events</a>');
    add('<a href="#/become-host">Become Host</a>');
    add('<span style="margin-left:auto;opacity:.8">' + escapeHtml(user.name) + '</span>');
    const l = document.createElement('button');
    l.textContent = 'Log out';
    l.onclick = async () => {
      await api('/api/auth/logout', { method: 'POST' });
      user = null;
      route();
    };
    nav.appendChild(l);
  } else {
    add('<a href="#/login">Sign in</a>');
    add('<a href="#/register">Register</a>');
  }
}

function escapeHtml(s) {
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/"/g, '&quot;');
}

function route() {
  renderNav();
  const h = (location.hash.replace(/^#\/?/, '/') || '/').split('?')[0];
  const parts = h.split('/').filter(Boolean);
  if (h === '/' || h === '') return pageHome();
  if (h.startsWith('/explore')) return pageExplore();
  if (parts[0] === 'events' && parts[1] && parts[2] === 'check-in') return pageCheckIn(parts[1]);
  if (parts[0] === 'events' && parts[1]) return pageEvent(parts[1]);
  if (parts[0] === 'hosts' && parts[1] === 'by-slug' && parts[2]) return pageHost(parts[2]);
  if (parts[0] === 'host' && parts[2] === 'events' && parts[3] === 'new') return pageNewEvent(parts[1]);
  if (parts[0] === 'host' && parts[2] === 'reports') return pageHostReports(parts[1]);
  if (parts[0] === 'host' && parts[2] === 'gallery-mod') return pageHostGallery(parts[1]);
  if (h.startsWith('/login')) return pageLogin();
  if (h.startsWith('/register')) return pageRegister();
  if (h.startsWith('/invite')) return pageInviteAccept();
  if (h.startsWith('/become-host')) return pageBecomeHost();
  if (h.startsWith('/host') && parts[1] && parts[2] === 'dashboard') return pageDashboard(parts[1]);
  if (h.startsWith('/tickets')) return pageTickets();
  if (h.startsWith('/mine')) return pageMyEvents();
  app.innerHTML = '<p>Not found</p>';
}

window.addEventListener('hashchange', route);

(async () => {
  await refreshUser();
  route();
})();

function pageHome() {
  app.innerHTML =
    '<div class="card"><h1>Community events</h1><p><a href="#/explore">Browse upcoming events</a></p></div>';
}

async function pageExplore() {
  app.innerHTML = '<p>Loading…</p>';
  try {
    const includePast = sessionStorage.getItem('explorePast') === '1';
    const evs = await api('/api/events?includePast=' + includePast);
    app.innerHTML =
      '<div class="card"><h1>Explore</h1><label><input type="checkbox" id="past"/> Include past</label> <button class="primary" id="apply">Apply</button></div>';
    document.getElementById('past').checked = includePast;
    document.getElementById('apply').onclick = () => {
      sessionStorage.setItem('explorePast', document.getElementById('past').checked ? '1' : '0');
      route();
    };
    const ul = document.createElement('div');
    evs.forEach((ev) => {
      const d = document.createElement('div');
      d.className = 'card';
      d.innerHTML =
        '<a href="#/events/' +
        ev.id +
        '"><strong>' +
        escapeHtml(ev.title) +
        '</strong></a>' +
        (ev.ended ? '<span class="badge-ended">Ended</span>' : '') +
        '<div style="opacity:.85;font-size:.9rem">' +
        new Date(ev.startAt).toLocaleString() +
        '</div>';
      ul.appendChild(d);
    });
    app.appendChild(ul);
  } catch (e) {
    app.innerHTML = '<p class="err">' + escapeHtml(e.message) + '</p>';
  }
}

async function pageEvent(id) {
  app.innerHTML = '<p>Loading…</p>';
  try {
    const ev = await api('/api/events/' + id);
    const gallery = await api('/api/events/' + id + '/gallery').catch(() => []);
    let ticketCode = '';
    let rsvpInfo = '';
    const share = location.origin + '/share/event/' + id;

    const body = document.createElement('div');
    body.innerHTML =
      '<div class="card"><h1>' +
      escapeHtml(ev.title) +
      (ev.ended ? '<span class="badge-ended">Ended</span>' : '') +
      '</h1><p>' +
      escapeHtml(ev.description || '') +
      '</p>' +
      '<p><strong>When:</strong> ' +
      new Date(ev.startAt).toLocaleString() +
      ' – ' +
      new Date(ev.endAt).toLocaleString() +
      '</p>' +
      '<p><strong>Where:</strong> ' +
      escapeHtml(ev.venueText || ev.onlineUrl || '—') +
      '</p>' +
      '<p><small>Share preview: <a href="' +
      share +
      '" target="_blank" rel="noreferrer">' +
      share +
      '</a></small></p>' +
      '<div id="rsvp-actions"></div>' +
      '<div id="ticket-box"></div>' +
      '<p><button type="button" id="report">Report event</button></p></div>';

    app.innerHTML = '';
    app.appendChild(body);

    const ga = document.createElement('div');
    ga.className = 'card';
    ga.innerHTML = '<h3>Gallery</h3><div id="gal"></div>';
    gallery.forEach((p) => {
      const img = document.createElement('img');
      img.src = p.imageUrl;
      img.style.maxWidth = '140px';
      img.style.margin = '4px';
      ga.querySelector('#gal').appendChild(img);
    });
    app.appendChild(ga);

    if (user && !ev.ended) {
      const up = document.createElement('div');
      up.className = 'card';
      up.innerHTML = '<h3>Gallery upload</h3><input type="file" id="galup" accept="image/*"/>';
      app.appendChild(up);
      document.getElementById('galup').onchange = async (e) => {
        const f = e.target.files[0];
        if (!f) return;
        const fd = new FormData();
        fd.append('file', f);
        await fetch('/api/events/' + id + '/gallery', { method: 'POST', credentials: 'include', body: fd });
        route();
      };
    }
    if (ev.ended && user) {
      const fb = document.createElement('div');
      fb.className = 'card';
      fb.innerHTML =
        '<h3>Feedback</h3><label>Stars (1–5) <input id="fbst" type="number" min="1" max="5" value="5"/></label><br/><textarea id="fbc" placeholder="Comment"></textarea><br/><button class="primary" id="fbs">Submit</button>';
      app.appendChild(fb);
      document.getElementById('fbs').onclick = async () => {
        try {
          await api('/api/events/' + id + '/feedback', {
            method: 'POST',
            body: {
              stars: Number(document.getElementById('fbst').value),
              comment: document.getElementById('fbc').value || null,
            },
          });
          alert('Thanks!');
        } catch (e) {
          alert(e.message);
        }
      };
    }

    const actions = document.getElementById('rsvp-actions');
    if (!ev.ended) {
      actions.innerHTML =
        '<button class="primary" id="rsvp">RSVP</button> <button id="cancel">Cancel RSVP</button>' +
        '<p class="hint" style="margin-top:.5rem;font-size:.9rem;opacity:.85">Guests: ' +
        '<a href="#/login?return=' +
        encodeURIComponent('#/events/' + id) +
        '">Sign in</a> or ' +
        '<a href="#/register?return=' +
        encodeURIComponent('#/events/' + id) +
        '">Register</a> to RSVP.</p>';
      document.getElementById('rsvp').onclick = async () => {
        if (!user) {
          location.hash = '#/login?return=' + encodeURIComponent('#/events/' + id);
          return;
        }
        try {
          const res = await api('/api/events/' + id + '/rsvp', { method: 'POST' });
          ticketCode = res.ticketCode || '';
          rsvpInfo =
            'Status: <strong>' +
            res.status +
            '</strong>' +
            (res.waitlistPosition != null ? ' (waitlist #' + res.waitlistPosition + ')' : '');
          if (res.promotionPending) rsvpInfo += ' — Promoted from waitlist!';
          renderTicket();
        } catch (e) {
          alert(e.message);
        }
      };
      document.getElementById('cancel').onclick = async () => {
        try {
          await api('/api/events/' + id + '/rsvp', { method: 'DELETE' });
          ticketCode = '';
          document.getElementById('ticket-box').innerHTML = '';
          alert('Cancelled');
        } catch (e) {
          alert(e.message);
        }
      };
    }

    document.getElementById('report').onclick = async () => {
      try {
        await api('/api/reports', { method: 'POST', body: { targetType: 'EVENT', targetId: Number(id) } });
        alert('Report submitted');
      } catch (e) {
        alert(e.message);
      }
    };

    function renderTicket() {
      const box = document.getElementById('ticket-box');
      box.innerHTML = '<p>' + rsvpInfo + '</p>';
      if (!ticketCode) return;
      box.innerHTML += '<p>Ticket code:</p><div id="qr"></div><pre>' + escapeHtml(ticketCode) + '</pre>';
      box.innerHTML +=
        '<p><button type="button" id="ics">Add to Calendar</button></p>';
      const qrEl = document.getElementById('qr');
      qrEl.innerHTML = '';
      if (window.QRCode) new QRCode(qrEl, { text: ticketCode, width: 160, height: 160 });
      document.getElementById('ics').onclick = () => downloadIcs(ev);
    }

    function downloadIcs(ev) {
      const dt = (x) =>
        new Date(x).toISOString().replace(/[-:]/g, '').replace(/\.\d{3}Z/, 'Z');
      const lines = [
        'BEGIN:VCALENDAR',
        'VERSION:2.0',
        'BEGIN:VEVENT',
        'UID:' + ev.id + '@event-platform',
        'DTSTAMP:' + dt(new Date()),
        'DTSTART:' + dt(ev.startAt),
        'DTEND:' + dt(ev.endAt),
        'SUMMARY:' + ev.title.replace(/\n/g, ' '),
        'END:VEVENT',
        'END:VCALENDAR',
      ];
      const blob = new Blob([lines.join('\r\n')], { type: 'text/calendar;charset=utf-8' });
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = 'event.ics';
      a.click();
    }
  } catch (e) {
    app.innerHTML = '<p class="err">' + escapeHtml(e.message) + '</p>';
  }
}

async function pageHost(slug) {
  app.innerHTML = '<p>Loading…</p>';
  try {
    const data = await api('/api/hosts/by-slug/' + slug);
    let html =
      '<div class="card"><h1>' +
      escapeHtml(data.host.displayName) +
      '</h1><p>' +
      escapeHtml(data.host.bio || '') +
      '</p><p>Contact: ' +
      escapeHtml(data.host.contactEmail) +
      '</p></div><h2>Events</h2>';
    data.publishedEvents.forEach((ev) => {
      html +=
        '<div class="card"><a href="#/events/' + ev.id + '">' + escapeHtml(ev.title) + '</a></div>';
    });
    app.innerHTML = html;
  } catch (e) {
    app.innerHTML = '<p class="err">' + escapeHtml(e.message) + '</p>';
  }
}

function readReturnHash() {
  const raw = new URLSearchParams(location.hash.split('?')[1] || '').get('return');
  let ret = '#/explore';
  if (raw) {
    try {
      ret = decodeURIComponent(raw);
      if (ret.startsWith('/')) {
        ret = '#' + ret;
      }
      if (!ret.startsWith('#/')) {
        ret = '#/explore';
      }
    } catch (_) {
      ret = '#/explore';
    }
  }
  return ret;
}

function pageLogin() {
  const ret = readReturnHash();
  app.innerHTML =
    '<div class="card"><h2>Sign in</h2>' +
    '<label>Email<br/><input id="em"/></label><br/>' +
    '<label>Password<br/><input id="pw" type="password"/></label><br/>' +
    '<p id="er" class="err"></p>' +
    '<button class="primary" id="go">Sign in</button></div>';
  document.getElementById('go').onclick = async () => {
    document.getElementById('er').textContent = '';
    try {
      await api('/api/auth/login', {
        method: 'POST',
        body: {
          email: document.getElementById('em').value,
          password: document.getElementById('pw').value,
        },
      });
      await refreshUser();
      location.hash = ret;
    } catch (e) {
      document.getElementById('er').textContent = e.message;
    }
  };
}

function pageRegister() {
  const ret = readReturnHash();
  app.innerHTML =
    '<div class="card"><h2>Register</h2>' +
    '<label>Name<br/><input id="nm"/></label><br/>' +
    '<label>Email<br/><input id="em"/></label><br/>' +
    '<label>Password (8+)<br/><input id="pw" type="password"/></label><br/>' +
    '<p id="er" class="err"></p>' +
    '<button class="primary" id="go">Create</button></div>';
  document.getElementById('go').onclick = async () => {
    document.getElementById('er').textContent = '';
    try {
      await api('/api/auth/register', {
        method: 'POST',
        body: {
          name: document.getElementById('nm').value,
          email: document.getElementById('em').value,
          password: document.getElementById('pw').value,
        },
      });
      await refreshUser();
      location.hash = ret;
    } catch (e) {
      document.getElementById('er').textContent = e.message;
    }
  };
}

function pageNewEvent(hostId) {
  if (!user) {
    location.hash = '#/login';
    return;
  }
  const start = new Date(Date.now() + 86400000).toISOString().slice(0, 16);
  const end = new Date(Date.now() + 90000000).toISOString().slice(0, 16);
  app.innerHTML =
    '<div class="card"><h2>New event</h2>' +
    '<label>Title<br/><input id="ti"/></label><br/>' +
    '<label>Description<br/><textarea id="de"></textarea></label><br/>' +
    '<label>Start<br/><input id="st" type="datetime-local" value="' +
    start +
    '"/></label><br/>' +
    '<label>End<br/><input id="en" type="datetime-local" value="' +
    end +
    '"/></label><br/>' +
    '<label>Capacity<br/><input id="ca" type="number" value="50"/></label><br/>' +
    '<label>Venue<br/><input id="ve"/></label><br/>' +
    '<label>Visibility <select id="vi"><option>PUBLIC</option><option>UNLISTED</option></select></label><br/>' +
    '<p id="er" class="err"></p>' +
    '<button class="primary" id="go">Create</button></div>';
  document.getElementById('go').onclick = async () => {
    document.getElementById('er').textContent = '';
    try {
      const ev = await api('/api/hosts/' + hostId + '/events', {
        method: 'POST',
        body: {
          title: document.getElementById('ti').value,
          description: document.getElementById('de').value,
          startAt: new Date(document.getElementById('st').value).toISOString(),
          endAt: new Date(document.getElementById('en').value).toISOString(),
          timezone: 'UTC',
          venueText: document.getElementById('ve').value || null,
          onlineUrl: null,
          capacity: Number(document.getElementById('ca').value),
          coverImageUrl: null,
          visibility: document.getElementById('vi').value,
        },
      });
      location.hash = '#/events/' + ev.id;
    } catch (e) {
      document.getElementById('er').textContent = e.message;
    }
  };
}

function pageInviteAccept() {
  const q = new URLSearchParams(location.hash.split('?')[1] || '');
  const token = q.get('token');
  const hostId = q.get('hostId');
  if (!user) {
    location.hash = '#/login?return=' + encodeURIComponent(location.hash);
    return;
  }
  app.innerHTML =
    '<div class="card"><h2>Accept invite</h2><p>Host #' +
    escapeHtml(hostId) +
    '</p><button class="primary" id="go">Join</button><p id="er" class="err"></p></div>';
  document.getElementById('go').onclick = async () => {
    document.getElementById('er').textContent = '';
    try {
      await api('/api/invites/accept', {
        method: 'POST',
        body: { token, hostId: Number(hostId) },
      });
      location.hash = '#/mine';
    } catch (e) {
      document.getElementById('er').textContent = e.message;
    }
  };
}

function pageBecomeHost() {
  if (!user) {
    location.hash = '#/login';
    return;
  }
  app.innerHTML =
    '<div class="card"><h2>Become Host</h2>' +
    '<label>Display name<br/><input id="dn"/></label><br/>' +
    '<label>Slug<br/><input id="sl"/></label><br/>' +
    '<label>Bio<br/><textarea id="bio"></textarea></label><br/>' +
    '<label>Contact email<br/><input id="ce" type="email"/></label><br/>' +
    '<p id="er" class="err"></p>' +
    '<button class="primary" id="go">Create</button></div>';
  document.getElementById('go').onclick = async () => {
    document.getElementById('er').textContent = '';
    try {
      const h = await api('/api/hosts', {
        method: 'POST',
        body: {
          displayName: document.getElementById('dn').value,
          slug: document.getElementById('sl').value,
          bio: document.getElementById('bio').value,
          contactEmail: document.getElementById('ce').value,
          logoUrl: null,
        },
      });
      location.hash = '#/host/' + h.id + '/dashboard';
    } catch (e) {
      document.getElementById('er').textContent = e.message;
    }
  };
}

async function pageDashboard(hostId) {
  if (!user) {
    location.hash = '#/login';
    return;
  }
  app.innerHTML = '<p>Loading…</p>';
  try {
    const rows = await api('/api/hosts/' + hostId + '/dashboard');
    const slug = rows[0] ? rows[0].event.hostSlug : 'demo-community';
    let html =
      '<h1>Dashboard</h1><p><a href="#/host/' +
      hostId +
      '/events/new">New event</a> · <a href="#/hosts/by-slug/' +
      slug +
      '">Public host</a> · <a href="#/host/' +
      hostId +
      '/reports">Reports</a> · <a href="#/host/' +
      hostId +
      '/gallery-mod">Gallery moderation</a></p>';
    rows.forEach(({ event, stats }) => {
      html +=
        '<div class="card"><a href="#/events/' +
        event.id +
        '"><strong>' +
        escapeHtml(event.title) +
        '</strong></a> — Going ' +
        stats.goingCount +
        ', Waitlist ' +
        stats.waitlistCount +
        ', In ' +
        stats.checkedInCount +
        '<div style="margin-top:.5rem">' +
        '<button data-a="pub" data-id="' +
        event.id +
        '">Publish</button> ' +
        '<button data-a="unpub" data-id="' +
        event.id +
        '">Unpublish</button> ' +
        '<button data-a="dup" data-id="' +
        event.id +
        '">Duplicate</button> ' +
        '<a href="#/events/' +
        event.id +
        '/check-in">Check-in</a> ' +
        '<button data-a="csv" data-id="' +
        event.id +
        '">CSV</button>' +
        '</div></div>';
    });
    app.innerHTML = html;
    app.querySelectorAll('button[data-a]').forEach((b) => {
      b.onclick = async () => {
        const id = b.getAttribute('data-id');
        const a = b.getAttribute('data-a');
        try {
          if (a === 'pub') await api('/api/events/' + id + '/publish', { method: 'POST' });
          if (a === 'unpub') await api('/api/events/' + id + '/unpublish', { method: 'POST' });
          if (a === 'dup') await api('/api/events/' + id + '/duplicate', { method: 'POST' });
          if (a === 'csv') {
            const r = await fetch('/api/events/' + id + '/export/rsvps.csv', { credentials: 'include' });
            const blob = await r.blob();
            const x = document.createElement('a');
            x.href = URL.createObjectURL(blob);
            x.download = 'rsvps.csv';
            x.click();
          }
          route();
        } catch (e) {
          alert(e.message);
        }
      };
    });
  } catch (e) {
    app.innerHTML = '<p class="err">' + escapeHtml(e.message) + '</p>';
  }
}

async function pageTickets() {
  if (!user) {
    location.hash = '#/login';
    return;
  }
  app.innerHTML = '<p>Loading…</p>';
  try {
    const rows = await api('/api/me/tickets');
    let html = '<h1>My Tickets</h1>';
    rows.forEach((row) => {
      html +=
        '<div class="card"><a href="#/events/' +
        row.event.id +
        '">' +
        escapeHtml(row.event.title) +
        '</a><pre>' +
        escapeHtml(row.ticketCode) +
        '</pre><div class="qr" id="q' +
        row.event.id +
        '"></div></div>';
    });
    app.innerHTML = html;
    rows.forEach((row) => {
      const el = document.getElementById('q' + row.event.id);
      if (el && window.QRCode) new QRCode(el, { text: row.ticketCode, width: 128, height: 128 });
    });
  } catch (e) {
    app.innerHTML = '<p class="err">' + escapeHtml(e.message) + '</p>';
  }
}

function isHostOnHostId(hostId) {
  if (!user || !user.hostMemberships) {
    return false;
  }
  return user.hostMemberships.some((m) => m.hostId === hostId && m.role === 'HOST');
}

async function pageMyEvents() {
  if (!user) {
    location.hash = '#/login';
    return;
  }
  const evs = await api('/api/me/events');
  let html = '<h1>My Events</h1>';
  evs.forEach((ev) => {
    const dash =
      isHostOnHostId(ev.hostId) ? ' · <a href="#/host/' + ev.hostId + '/dashboard">Dashboard</a>' : '';
    html +=
      '<div class="card"><a href="#/events/' +
      ev.id +
      '">' +
      escapeHtml(ev.title) +
      '</a> · <a href="#/events/' +
      ev.id +
      '/check-in">Check-in</a>' +
      dash +
      '</div>';
  });
  app.innerHTML = html;
}

async function pageCheckIn(id) {
  if (!user) {
    location.hash = '#/login';
    return;
  }
  let sid = sessionStorage.getItem('cin_' + id);
  if (!sid) {
    sid = crypto.randomUUID();
    sessionStorage.setItem('cin_' + id, sid);
  }
  app.innerHTML = '<div class="card"><h1>Check-in</h1><p id="st"></p>' +
    '<input id="code" placeholder="Ticket code"/> ' +
    '<button class="primary" id="go">Check in</button> ' +
    '<button id="undo">Undo last</button><p id="er" class="err"></p></div>';

  async function poll() {
    try {
      const s = await api('/api/events/' + id + '/check-in/stats');
      document.getElementById('st').textContent =
        'Going ' + s.goingCount + ', Waitlist ' + s.waitlistCount + ', Checked-in ' + s.checkedInCount;
    } catch (_) {}
  }
  poll();
  const iv = setInterval(poll, 3000);
  window.addEventListener(
    'hashchange',
    () => clearInterval(iv),
    { once: true },
  );

  document.getElementById('go').onclick = async () => {
    document.getElementById('er').textContent = '';
    try {
      const r = await fetch('/api/events/' + id + '/check-in', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          'X-CheckIn-Session': sid,
        },
        body: JSON.stringify({ code: document.getElementById('code').value }),
      });
      const j = await r.json().catch(() => ({}));
      if (!r.ok) throw new Error(j.error || r.statusText);
      document.getElementById('st').textContent =
        'Going ' + j.goingCount + ', Waitlist ' + j.waitlistCount + ', Checked-in ' + j.checkedInCount;
    } catch (e) {
      document.getElementById('er').textContent = e.message;
    }
  };
  document.getElementById('undo').onclick = async () => {
    document.getElementById('er').textContent = '';
    try {
      const r = await fetch('/api/events/' + id + '/check-in/undo-last', {
        method: 'POST',
        credentials: 'include',
        headers: { 'X-CheckIn-Session': sid },
      });
      const j = await r.json().catch(() => ({}));
      if (!r.ok) throw new Error(j.error || r.statusText);
      document.getElementById('st').textContent =
        'Going ' + j.goingCount + ', Waitlist ' + j.waitlistCount + ', Checked-in ' + j.checkedInCount;
    } catch (e) {
      document.getElementById('er').textContent = e.message;
    }
  };
}

async function pageHostReports(hostId) {
  if (!user) {
    location.hash = '#/login';
    return;
  }
  app.innerHTML = '<p>Loading…</p>';
  try {
    const rows = await api('/api/hosts/' + hostId + '/reports');
    let html = '<h1>Reports</h1>';
    rows.forEach((r) => {
      html +=
        '<div class="card">#' +
        r.id +
        ' ' +
        r.targetType +
        ' — event ' +
        r.targetEventId +
        ' photo ' +
        r.targetPhotoId +
        '<br/><button data-id="' +
        r.id +
        '" data-h="0">Dismiss</button> <button data-id="' +
        r.id +
        '" data-h="1">Hide</button></div>';
    });
    app.innerHTML = html;
    app.querySelectorAll('button[data-id]').forEach((b) => {
      b.onclick = async () => {
        await api('/api/reports/' + b.getAttribute('data-id') + '/resolve', {
          method: 'POST',
          body: { hide: b.getAttribute('data-h') === '1' },
        });
        pageHostReports(hostId);
      };
    });
  } catch (e) {
    app.innerHTML = '<p class="err">' + escapeHtml(e.message) + '</p>';
  }
}

async function pageHostGallery(hostId) {
  if (!user) {
    location.hash = '#/login';
    return;
  }
  app.innerHTML = '<p>Loading…</p>';
  try {
    const rows = await api('/api/hosts/' + hostId + '/gallery/pending');
    let html = '<h1>Gallery moderation</h1>';
    rows.forEach((p) => {
      html +=
        '<div class="card"><img src="' +
        escapeHtml(p.imageUrl) +
        '" style="max-width:200px"/><br/><button data-id="' +
        p.id +
        '" data-a="1">Approve</button> <button data-id="' +
        p.id +
        '" data-a="0">Reject</button></div>';
    });
    app.innerHTML = html || '<p>No pending photos</p>';
    app.querySelectorAll('button[data-id]').forEach((b) => {
      b.onclick = async () => {
        await fetch('/api/gallery/' + b.getAttribute('data-id') + '/approve?approve=' + b.getAttribute('data-a'), {
          method: 'POST',
          credentials: 'include',
        });
        pageHostGallery(hostId);
      };
    });
  } catch (e) {
    app.innerHTML = '<p class="err">' + escapeHtml(e.message) + '</p>';
  }
}
