import { useEffect, useMemo, useState } from 'react';
import { Link, Navigate, Route, Routes, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { QRCodeSVG } from 'qrcode.react';
import { api } from './api.js';

function Nav({ user, onLogout }) {
  return (
    <nav>
      <Link to="/">
        <strong>Events</strong>
      </Link>
      <Link to="/explore">Explore</Link>
      {user && (
        <>
          <Link to="/me/tickets">My Tickets</Link>
          <Link to="/me/events">My Events</Link>
          <Link to="/become-host">Become Host</Link>
        </>
      )}
      {!user && (
        <>
          <Link to="/login">Sign in</Link>
          <Link to="/register">Register</Link>
        </>
      )}
      {user && (
        <>
          <span style={{ marginLeft: 'auto', opacity: 0.8 }}>{user.name}</span>
          <button type="button" onClick={onLogout}>
            Log out
          </button>
        </>
      )}
    </nav>
  );
}

function Home() {
  return (
    <div className="layout">
      <h1>Community events</h1>
      <p>
        <Link to="/explore">Browse upcoming events</Link>
      </p>
    </div>
  );
}

function Explore() {
  const [includePast, setIncludePast] = useState(false);
  const [query, setQuery] = useState('');
  const [location, setLocation] = useState('');
  const [events, setEvents] = useState([]);
  const [err, setErr] = useState('');
  const load = async () => {
    setErr('');
    try {
      const qs = new URLSearchParams({ includePast: String(includePast) });
      if (query) qs.set('query', query);
      if (location) qs.set('location', location);
      const data = await api('/api/events?' + qs.toString());
      setEvents(data);
    } catch (e) {
      setErr(e.message);
    }
  };
  useEffect(() => {
    load();
  }, [includePast]);
  return (
    <div className="layout">
      <h1>Explore</h1>
      <div className="card grid">
        <label>
          Search{' '}
          <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Title or description" />
        </label>
        <label>
          Location{' '}
          <input value={location} onChange={(e) => setLocation(e.target.value)} placeholder="Venue" />
        </label>
        <label>
          <input type="checkbox" checked={includePast} onChange={(e) => setIncludePast(e.target.checked)} /> Include
          past
        </label>
        <button type="button" className="primary" onClick={load}>
          Apply
        </button>
      </div>
      {err && <p className="err">{err}</p>}
      <ul style={{ listStyle: 'none', padding: 0 }}>
        {events.map((ev) => (
          <li key={ev.id} className="card">
            <Link to={'/events/' + ev.id}>
              <strong>{ev.title}</strong>
            </Link>
            {ev.ended && (
              <span className="badge badge-ended" style={{ marginLeft: 8 }}>
                Ended
              </span>
            )}
            <div style={{ fontSize: '0.9rem', opacity: 0.85 }}>
              {new Date(ev.startAt).toLocaleString()} · {ev.venueText || ev.onlineUrl || '—'}
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}

function downloadIcs(ev) {
  const dt = (d) =>
    new Date(d)
      .toISOString()
      .replace(/[-:]/g, '')
      .replace(/\.\d{3}Z/, 'Z');
  const lines = [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'BEGIN:VEVENT',
    'UID:' + ev.id + '@event-platform',
    'DTSTAMP:' + dt(new Date()),
    'DTSTART:' + dt(ev.startAt),
    'DTEND:' + dt(ev.endAt),
    'SUMMARY:' + ev.title.replace(/\n/g, ' '),
    'DESCRIPTION:' + (ev.description || '').replace(/\n/g, ' '),
    'LOCATION:' + (ev.venueText || '').replace(/\n/g, ' '),
    'END:VEVENT',
    'END:VCALENDAR',
  ];
  const blob = new Blob([lines.join('\r\n')], { type: 'text/calendar;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'event.ics';
  a.click();
  URL.revokeObjectURL(url);
}

function EventDetail({ user }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const [ev, setEv] = useState(null);
  const [gallery, setGallery] = useState([]);
  const [rsvp, setRsvp] = useState(null);
  const [ticketCode, setTicketCode] = useState('');
  const [err, setErr] = useState('');
  const [fbStars, setFbStars] = useState(5);
  const [fbComment, setFbComment] = useState('');
  const load = async () => {
    setErr('');
    try {
      const e = await api('/api/events/' + id);
      setEv(e);
      const g = await api('/api/events/' + id + '/gallery');
      setGallery(g);
    } catch (e) {
      setErr(e.message);
    }
  };
  useEffect(() => {
    load();
  }, [id]);
  const doRsvp = async () => {
    if (!user) {
      navigate('/login?return=' + encodeURIComponent('/events/' + id));
      return;
    }
    try {
      const res = await api('/api/events/' + id + '/rsvp', { method: 'POST' });
      setRsvp(res);
      if (res.ticketCode) setTicketCode(res.ticketCode);
    } catch (e) {
      setErr(e.message);
    }
  };
  const cancelRsvp = async () => {
    try {
      await api('/api/events/' + id + '/rsvp', { method: 'DELETE' });
      setRsvp(null);
      setTicketCode('');
      load();
    } catch (e) {
      setErr(e.message);
    }
  };
  const uploadGallery = async (file) => {
    const fd = new FormData();
    fd.append('file', file);
    await fetch('/api/events/' + id + '/gallery', { method: 'POST', credentials: 'include', body: fd });
    load();
  };
  const submitFeedback = async () => {
    try {
      await api('/api/events/' + id + '/feedback', {
        method: 'POST',
        body: { stars: fbStars, comment: fbComment || null },
      });
      alert('Thanks for your feedback!');
    } catch (e) {
      setErr(e.message);
    }
  };
  const submitReport = async () => {
    try {
      await api('/api/reports', { method: 'POST', body: { targetType: 'EVENT', targetId: Number(id) } });
      alert('Report submitted.');
    } catch (e) {
      setErr(e.message);
    }
  };
  if (!ev) return err ? <div className="layout err">{err}</div> : <div className="layout">Loading…</div>;

  const shareUrl = window.location.origin + '/share/event/' + id;

  return (
    <div className="layout">
      <div className="card">
        <h1>
          {ev.title}{' '}
          {ev.ended && (
            <span className="badge badge-ended" style={{ marginLeft: 8 }}>
              Ended
            </span>
          )}
        </h1>
        <p>{ev.description}</p>
        <p>
          <strong>When:</strong> {new Date(ev.startAt).toLocaleString()} – {new Date(ev.endAt).toLocaleString()} (
          {ev.timezone})
        </p>
        <p>
          <strong>Where:</strong> {ev.venueText || ev.onlineUrl || '—'}
        </p>
        <p>
          <small>
            Share (OG preview):{' '}
            <a href={shareUrl} target="_blank" rel="noreferrer">
              {shareUrl}
            </a>
          </small>
        </p>
        {!ev.ended && (
          <>
            <button type="button" className="primary" onClick={doRsvp}>
              RSVP
            </button>{' '}
            <button type="button" onClick={cancelRsvp}>
              Cancel RSVP
            </button>
          </>
        )}
        {rsvp && (
          <p style={{ marginTop: 8 }}>
            Status: <strong>{rsvp.status}</strong>
            {rsvp.waitlistPosition != null && <> (waitlist #{rsvp.waitlistPosition})</>}
            {rsvp.promotionPending && <> — you were promoted from the waitlist!</>}
          </p>
        )}
        {ticketCode && (
          <div style={{ marginTop: 12 }}>
            <p>Your ticket code (show at door):</p>
            <QRCodeSVG value={ticketCode} size={160} />
            <pre>{ticketCode}</pre>
            <button type="button" onClick={() => downloadIcs(ev)}>
              Add to Calendar
            </button>
          </div>
        )}
        <p style={{ marginTop: 12 }}>
          <button type="button" onClick={submitReport}>
            Report event
          </button>
        </p>
      </div>
      {user && !ev.ended && (
        <div className="card">
          <h3>Gallery upload</h3>
          <input type="file" accept="image/*" onChange={(e) => e.target.files[0] && uploadGallery(e.target.files[0])} />
        </div>
      )}
      <div className="card">
        <h3>Gallery</h3>
        <div className="grid" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(120px,1fr))' }}>
          {gallery.map((p) => (
            <a key={p.id} href={p.imageUrl} target="_blank" rel="noreferrer">
              <img src={p.imageUrl} alt="" style={{ width: '100%', borderRadius: 6 }} />
            </a>
          ))}
        </div>
      </div>
      {ev.ended && user && (
        <div className="card">
          <h3>Feedback</h3>
          <label>
            Stars (1–5){' '}
            <input
              type="number"
              min={1}
              max={5}
              value={fbStars}
              onChange={(e) => setFbStars(Number(e.target.value))}
            />
          </label>
          <textarea value={fbComment} onChange={(e) => setFbComment(e.target.value)} placeholder="Comment (optional)" />
          <button type="button" className="primary" onClick={submitFeedback}>
            Submit
          </button>
        </div>
      )}
      {err && <p className="err">{err}</p>}
    </div>
  );
}

function HostPage() {
  const { slug } = useParams();
  const [data, setData] = useState(null);
  const [err, setErr] = useState('');
  useEffect(() => {
    api('/api/hosts/by-slug/' + slug)
      .then(setData)
      .catch((e) => setErr(e.message));
  }, [slug]);
  if (err) return <div className="layout err">{err}</div>;
  if (!data) return <div className="layout">Loading…</div>;
  return (
    <div className="layout">
      <div className="card">
        <h1>{data.host.displayName}</h1>
        <p>{data.host.bio}</p>
        <p>Contact: {data.host.contactEmail}</p>
        <p>
          <small>Host page URL for sharing: {window.location.href}</small>
        </p>
      </div>
      <h2>Published events</h2>
      {data.publishedEvents.map((ev) => (
        <div key={ev.id} className="card">
          <Link to={'/events/' + ev.id}>{ev.title}</Link>
        </div>
      ))}
    </div>
  );
}

function Login() {
  const [params] = useSearchParams();
  const nav = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [err, setErr] = useState('');
  const ret = params.get('return') || '/explore';
  const safeReturn = ret.startsWith('/') && !ret.startsWith('//') ? ret : '/explore';
  const submit = async (e) => {
    e.preventDefault();
    setErr('');
    try {
      await api('/api/auth/login', { method: 'POST', body: { email, password } });
      nav(safeReturn);
    } catch (ex) {
      setErr(ex.message);
    }
  };
  return (
    <div className="layout">
      <form className="card grid" onSubmit={submit}>
        <h2>Sign in</h2>
        <label>
          Email <input value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="username" />
        </label>
        <label>
          Password{' '}
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
          />
        </label>
        {err && <p className="err">{err}</p>}
        <button type="submit" className="primary">
          Sign in
        </button>
      </form>
    </div>
  );
}

function Register() {
  const nav = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [err, setErr] = useState('');
  const submit = async (e) => {
    e.preventDefault();
    setErr('');
    try {
      await api('/api/auth/register', { method: 'POST', body: { email, password, name } });
      nav('/explore');
    } catch (ex) {
      setErr(ex.message);
    }
  };
  return (
    <div className="layout">
      <form className="card grid" onSubmit={submit}>
        <h2>Register</h2>
        <label>
          Name <input value={name} onChange={(e) => setName(e.target.value)} />
        </label>
        <label>
          Email <input value={email} onChange={(e) => setEmail(e.target.value)} />
        </label>
        <label>
          Password (min 8 chars){' '}
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
        </label>
        {err && <p className="err">{err}</p>}
        <button type="submit" className="primary">
          Create account
        </button>
      </form>
    </div>
  );
}

function BecomeHost() {
  const nav = useNavigate();
  const [displayName, setDisplayName] = useState('');
  const [slug, setSlug] = useState('');
  const [bio, setBio] = useState('');
  const [contactEmail, setContactEmail] = useState('');
  const [err, setErr] = useState('');
  const submit = async (e) => {
    e.preventDefault();
    setErr('');
    try {
      const h = await api('/api/hosts', {
        method: 'POST',
        body: { displayName, slug, bio, contactEmail, logoUrl: null },
      });
      nav('/host/' + h.id + '/dashboard');
    } catch (ex) {
      setErr(ex.message);
    }
  };
  return (
    <div className="layout">
      <form className="card grid" onSubmit={submit}>
        <h2>Become a Host</h2>
        <label>
          Display name <input value={displayName} onChange={(e) => setDisplayName(e.target.value)} required />
        </label>
        <label>
          URL slug <input value={slug} onChange={(e) => setSlug(e.target.value)} required />
        </label>
        <label>
          Bio <textarea value={bio} onChange={(e) => setBio(e.target.value)} />
        </label>
        <label>
          Contact email <input type="email" value={contactEmail} onChange={(e) => setContactEmail(e.target.value)} required />
        </label>
        {err && <p className="err">{err}</p>}
        <button type="submit" className="primary">
          Create host profile
        </button>
      </form>
    </div>
  );
}

function EventEditor({ mode }) {
  const params = useParams();
  const hostId = params.hostId;
  const id = params.id;
  const nav = useNavigate();
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [startAt, setStartAt] = useState('');
  const [endAt, setEndAt] = useState('');
  const [timezone, setTimezone] = useState('UTC');
  const [venueText, setVenueText] = useState('');
  const [onlineUrl, setOnlineUrl] = useState('');
  const [capacity, setCapacity] = useState(50);
  const [visibility, setVisibility] = useState('PUBLIC');
  const [pricing, setPricing] = useState('FREE');
  const [err, setErr] = useState('');
  useEffect(() => {
    if (mode === 'edit') {
      api('/api/events/' + id).then((ev) => {
        setTitle(ev.title);
        setDescription(ev.description || '');
        setStartAt(ev.startAt.slice(0, 16));
        setEndAt(ev.endAt.slice(0, 16));
        setTimezone(ev.timezone);
        setVenueText(ev.venueText || '');
        setOnlineUrl(ev.onlineUrl || '');
        setCapacity(ev.capacity);
        setVisibility(ev.visibility);
      });
    }
  }, [mode, id]);
  const body = () => ({
    title,
    description,
    startAt: new Date(startAt).toISOString(),
    endAt: new Date(endAt).toISOString(),
    timezone,
    venueText: venueText || null,
    onlineUrl: onlineUrl || null,
    capacity: Number(capacity),
    coverImageUrl: null,
    visibility,
  });
  const save = async (e) => {
    e.preventDefault();
    setErr('');
    try {
      if (mode === 'create') {
        const ev = await api('/api/hosts/' + hostId + '/events', { method: 'POST', body: body() });
        nav('/events/' + ev.id);
      } else {
        await api('/api/events/' + id, { method: 'PUT', body: body() });
        nav('/events/' + id);
      }
    } catch (ex) {
      setErr(ex.message);
    }
  };
  return (
    <div className="layout">
      <form className="card grid" onSubmit={save}>
        <h2>{mode === 'create' ? 'New event' : 'Edit event'}</h2>
        <label>
          Title <input value={title} onChange={(e) => setTitle(e.target.value)} required />
        </label>
        <label>
          Description <textarea value={description} onChange={(e) => setDescription(e.target.value)} />
        </label>
        <label>
          Start <input type="datetime-local" value={startAt} onChange={(e) => setStartAt(e.target.value)} required />
        </label>
        <label>
          End <input type="datetime-local" value={endAt} onChange={(e) => setEndAt(e.target.value)} required />
        </label>
        <label>
          Timezone <input value={timezone} onChange={(e) => setTimezone(e.target.value)} />
        </label>
        <label>
          Venue <input value={venueText} onChange={(e) => setVenueText(e.target.value)} />
        </label>
        <label>
          Online URL <input value={onlineUrl} onChange={(e) => setOnlineUrl(e.target.value)} />
        </label>
        <label>
          Capacity <input type="number" min={1} value={capacity} onChange={(e) => setCapacity(e.target.value)} />
        </label>
        <label>
          Visibility{' '}
          <select value={visibility} onChange={(e) => setVisibility(e.target.value)}>
            <option value="PUBLIC">Public</option>
            <option value="UNLISTED">Unlisted</option>
          </select>
        </label>
        <div>
          <span className="tooltip-wrap" data-title="Coming soon">
            <label>
              <input type="radio" checked={pricing === 'FREE'} onChange={() => setPricing('FREE')} /> Free
            </label>{' '}
            <label style={{ opacity: 0.5 }}>
              <input type="radio" disabled checked={pricing === 'PAID'} onChange={() => {}} /> Paid
            </label>
          </span>
        </div>
        {err && <p className="err">{err}</p>}
        <button type="submit" className="primary">
          Save
        </button>
      </form>
    </div>
  );
}

function Dashboard({ user }) {
  const { hostId } = useParams();
  const [rows, setRows] = useState([]);
  const [err, setErr] = useState('');
  const load = () =>
    api('/api/hosts/' + hostId + '/dashboard')
      .then(setRows)
      .catch((e) => setErr(e.message));
  useEffect(() => {
    load();
  }, [hostId]);
  const publish = async (eventId) => {
    await api('/api/events/' + eventId + '/publish', { method: 'POST' });
    load();
  };
  const unpublish = async (eventId) => {
    await api('/api/events/' + eventId + '/unpublish', { method: 'POST' });
    load();
  };
  const dup = async (eventId) => {
    const ev = await api('/api/events/' + eventId + '/duplicate', { method: 'POST' });
    alert('Duplicated as draft id ' + ev.id);
    load();
  };
  const csv = async (eventId) => {
    const r = await fetch('/api/events/' + eventId + '/export/rsvps.csv', { credentials: 'include' });
    const blob = await r.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'rsvps.csv';
    a.click();
    URL.revokeObjectURL(url);
  };
  if (!user) return <Navigate to="/login" />;
  return (
    <div className="layout">
      <h1>Host dashboard</h1>
      <p>
        <Link to={'/host/' + hostId + '/events/new'}>New event</Link>
        {rows[0] && (
          <>
            {' '}
            · <Link to={'/hosts/by-slug/' + rows[0].event.hostSlug}>Public host page</Link>
          </>
        )}{' '}
        · <Link to={'/host/' + hostId + '/reports'}>Reports queue</Link> ·{' '}
        <Link to={'/host/' + hostId + '/gallery'}>Gallery moderation</Link>
      </p>
      {err && <p className="err">{err}</p>}
      {rows.map(({ event, stats }) => (
        <div key={event.id} className="card">
          <Link to={'/events/' + event.id}>
            <strong>{event.title}</strong>
          </Link>{' '}
          {event.ended && <span className="badge badge-ended">Ended</span>}
          <div>
            Going: {stats.goingCount} · Waitlist: {stats.waitlistCount} · Checked-in: {stats.checkedInCount}
          </div>
          <div style={{ marginTop: 8, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            <button type="button" onClick={() => publish(event.id)}>
              Publish
            </button>
            <button type="button" onClick={() => unpublish(event.id)}>
              Unpublish
            </button>
            <button type="button" onClick={() => dup(event.id)}>
              Duplicate
            </button>
            <Link to={'/events/' + event.id + '/edit'}>Edit</Link>
            <button type="button" onClick={() => csv(event.id)}>
              CSV export
            </button>
            <Link to={'/events/' + event.id + '/check-in'}>Check-in</Link>
          </div>
        </div>
      ))}
    </div>
  );
}

function MyTickets() {
  const [rows, setRows] = useState([]);
  useEffect(() => {
    api('/api/me/tickets').then(setRows);
  }, []);
  return (
    <div className="layout">
      <h1>My Tickets</h1>
      {rows.map((row) => (
        <div key={row.event.id} className="card">
          <Link to={'/events/' + row.event.id}>{row.event.title}</Link>
          {row.promotionPending && <p>You were promoted from the waitlist.</p>}
          <QRCodeSVG value={row.ticketCode} size={128} />
          <pre>{row.ticketCode}</pre>
        </div>
      ))}
    </div>
  );
}

function MyEventsView() {
  const [events, setEvents] = useState([]);
  useEffect(() => {
    api('/api/me/events').then(setEvents);
  }, []);
  return (
    <div className="layout">
      <h1>My Events</h1>
      {events.map((ev) => (
        <div key={ev.id} className="card">
          <Link to={'/events/' + ev.id}>{ev.title}</Link>
          <div style={{ marginTop: 8 }}>
            <Link to={'/events/' + ev.id + '/edit'}>Edit</Link> ·{' '}
            <Link to={'/events/' + ev.id + '/check-in'}>Check-in</Link> ·{' '}
            <Link to={'/host/' + ev.hostId + '/dashboard'}>Dashboard</Link>
          </div>
        </div>
      ))}
    </div>
  );
}

function CheckInPage() {
  const { id } = useParams();
  const [stats, setStats] = useState(null);
  const [code, setCode] = useState('');
  const [err, setErr] = useState('');
  const sessionId = useMemo(() => {
    let s = sessionStorage.getItem('checkinSession_' + id);
    if (!s) {
      s = crypto.randomUUID();
      sessionStorage.setItem('checkinSession_' + id, s);
    }
    return s;
  }, [id]);
  const poll = async () => {
    try {
      const s = await api('/api/events/' + id + '/check-in/stats');
      setStats(s);
    } catch {
      /* ignore */
    }
  };
  useEffect(() => {
    poll();
    const t = setInterval(poll, 3000);
    return () => clearInterval(t);
  }, [id]);
  const submit = async () => {
    setErr('');
    try {
      const s = await fetch('/api/events/' + id + '/check-in', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          'X-CheckIn-Session': sessionId,
        },
        body: JSON.stringify({ code }),
      });
      if (!s.ok) throw new Error((await s.json()).error || s.statusText);
      setStats(await s.json());
      setCode('');
    } catch (e) {
      setErr(e.message);
    }
  };
  const undo = async () => {
    setErr('');
    try {
      const s = await fetch('/api/events/' + id + '/check-in/undo-last', {
        method: 'POST',
        credentials: 'include',
        headers: { 'X-CheckIn-Session': sessionId },
      });
      if (!s.ok) throw new Error((await s.json()).error || s.statusText);
      setStats(await s.json());
    } catch (e) {
      setErr(e.message);
    }
  };
  return (
    <div className="layout">
      <h1>Check-in</h1>
      {stats && (
        <p>
          Going: {stats.goingCount} · Waitlist: {stats.waitlistCount} · Checked-in: {stats.checkedInCount}
        </p>
      )}
      <div className="card grid">
        <input value={code} onChange={(e) => setCode(e.target.value)} placeholder="Ticket code" />
        <button type="button" className="primary" onClick={submit}>
          Check in
        </button>
        <button type="button" onClick={undo}>
          Undo last scan
        </button>
      </div>
      {err && <p className="err">{err}</p>}
    </div>
  );
}

function ReportsPage() {
  const { hostId } = useParams();
  const [rows, setRows] = useState([]);
  const load = () => api('/api/hosts/' + hostId + '/reports').then(setRows);
  useEffect(() => {
    load();
  }, [hostId]);
  const resolve = async (reportId, hide) => {
    await api('/api/reports/' + reportId + '/resolve', { method: 'POST', body: { hide } });
    load();
  };
  return (
    <div className="layout">
      <h1>Reports</h1>
      {rows.map((r) => (
        <div key={r.id} className="card">
          <div>
            #{r.id} · {r.targetType} · event #{r.targetEventId} · photo #{r.targetPhotoId}
          </div>
          <button type="button" onClick={() => resolve(r.id, false)}>
            Dismiss
          </button>{' '}
          <button type="button" onClick={() => resolve(r.id, true)}>
            Hide content
          </button>
        </div>
      ))}
    </div>
  );
}

function GalleryModPage() {
  const { hostId } = useParams();
  const [rows, setRows] = useState([]);
  const load = () => api('/api/hosts/' + hostId + '/gallery/pending').then(setRows);
  useEffect(() => {
    load();
  }, [hostId]);
  const mod = async (photoId, approve) => {
    await fetch('/api/gallery/' + photoId + '/approve?approve=' + approve, {
      method: 'POST',
      credentials: 'include',
    });
    load();
  };
  return (
    <div className="layout">
      <h1>Gallery moderation</h1>
      {rows.map((p) => (
        <div key={p.id} className="card">
          <img src={p.imageUrl} alt="" style={{ maxWidth: 200 }} />
          <div>
            <button type="button" onClick={() => mod(p.id, true)}>
              Approve
            </button>{' '}
            <button type="button" onClick={() => mod(p.id, false)}>
              Reject
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}

function InvitePage() {
  const { token } = useParams();
  const [params] = useSearchParams();
  const nav = useNavigate();
  const hostId = Number(params.get('hostId'));
  const submit = async () => {
    await api('/api/invites/accept', { method: 'POST', body: { token, hostId } });
    nav('/me/events');
  };
  return (
    <div className="layout">
      <div className="card">
        <h2>Accept invite</h2>
        <p>Host #{hostId}</p>
        <button type="button" className="primary" onClick={submit}>
          Join host team
        </button>
      </div>
    </div>
  );
}

export default function App() {
  const [user, setUser] = useState(undefined);
  const refreshUser = () =>
    api('/api/me')
      .then((u) => setUser(u))
      .catch(() => setUser(null));
  useEffect(() => {
    refreshUser();
  }, []);
  const logout = async () => {
    await api('/api/auth/logout', { method: 'POST' });
    setUser(null);
  };
  if (user === undefined) return <div className="layout">Loading…</div>;
  return (
    <>
      <Nav user={user} onLogout={logout} />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/explore" element={<Explore />} />
        <Route path="/events/:id" element={<EventDetail user={user} />} />
        <Route path="/hosts/by-slug/:slug" element={<HostPage />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/become-host" element={user ? <BecomeHost /> : <Navigate to="/login" />} />
        <Route path="/host/:hostId/dashboard" element={<Dashboard user={user} />} />
        <Route path="/host/:hostId/events/new" element={user ? <EventEditor mode="create" /> : <Navigate to="/login" />} />
        <Route path="/events/:id/edit" element={user ? <EventEditor mode="edit" /> : <Navigate to="/login" />} />
        <Route path="/me/tickets" element={user ? <MyTickets /> : <Navigate to="/login" />} />
        <Route path="/me/events" element={user ? <MyEventsView /> : <Navigate to="/login" />} />
        <Route path="/events/:id/check-in" element={user ? <CheckInPage /> : <Navigate to="/login" />} />
        <Route path="/host/:hostId/reports" element={user ? <ReportsPage /> : <Navigate to="/login" />} />
        <Route path="/host/:hostId/gallery" element={user ? <GalleryModPage /> : <Navigate to="/login" />} />
        <Route path="/invite/:token" element={user ? <InvitePage /> : <Navigate to="/login" />} />
        <Route path="*" element={<Navigate to="/" />} />
      </Routes>
    </>
  );
}
