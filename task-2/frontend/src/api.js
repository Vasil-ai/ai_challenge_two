export async function api(path, opts = {}) {
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
  if (r.status === 401 && path.startsWith('/api/me')) {
    return null;
  }
  if (!r.ok) {
    let msg = r.statusText;
    try {
      const j = await r.json();
      if (j.error) msg = j.error;
    } catch {
      /* ignore */
    }
    throw new Error(msg);
  }
  if (r.status === 204) return null;
  const ct = r.headers.get('content-type');
  if (ct && ct.includes('application/json')) return r.json();
  return r.text();
}
