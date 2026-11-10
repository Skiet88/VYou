import { http, HttpResponse } from 'msw';

/**
 * In-memory stand-in for the real backend's /api/auth endpoints, following the same contract:
 * problem+json errors with the same generic messages, a session, and CSRF enforced on every
 * POST (a missing/wrong X-XSRF-TOKEN gets 403, exactly like Spring Security).
 */
export const TEST_CSRF_TOKEN = 'test-csrf-token';
export const RATE_LIMITED_EMAIL = 'ratelimited@example.com';

interface StoredUser {
  id: string;
  email: string;
  displayName: string;
  password: string;
  role: 'USER' | 'ADMIN';
  createdAt: string;
}

const state = {
  users: new Map<string, StoredUser>(),
  sessionUserId: null as string | null,
  lastRequestHeaders: null as Headers | null,
};

export const backend = {
  reset() {
    state.users.clear();
    state.sessionUserId = null;
    state.lastRequestHeaders = null;
  },
  addUser(email: string, password: string, displayName = 'Test User') {
    const user: StoredUser = {
      id: crypto.randomUUID(),
      email: email.toLowerCase(),
      displayName,
      password,
      role: 'USER',
      createdAt: new Date().toISOString(),
    };
    state.users.set(user.email, user);
    return user;
  },
  signIn(email: string) {
    state.sessionUserId = state.users.get(email.toLowerCase())?.id ?? null;
  },
  get signedInUserId() {
    return state.sessionUserId;
  },
  get lastRequestHeaders() {
    return state.lastRequestHeaders;
  },
};

const problem = (status: number, title: string, detail: string) =>
  HttpResponse.json({ type: 'about:blank', title, status, detail }, { status, headers: { 'Content-Type': 'application/problem+json' } });

const publicUser = ({ id, email, displayName, role, createdAt }: StoredUser) => ({ id, email, displayName, role, createdAt });

function csrfRejected(request: Request) {
  state.lastRequestHeaders = request.headers;
  return request.headers.get('X-XSRF-TOKEN') !== TEST_CSRF_TOKEN ? problem(403, 'Forbidden', 'Forbidden') : null;
}

export const handlers = [
  http.get('*/api/auth/me', () => {
    const user = [...state.users.values()].find((u) => u.id === state.sessionUserId);
    return user ? HttpResponse.json(publicUser(user)) : problem(401, 'Unauthorized', 'Authentication is required');
  }),

  http.post('*/api/auth/login', async ({ request }) => {
    const rejected = csrfRejected(request);
    if (rejected) return rejected;
    const body = (await request.json()) as { email: string; password: string };
    if (body.email === RATE_LIMITED_EMAIL) return problem(429, 'Too many requests', 'Too many requests. Please wait and try again.');
    const user = state.users.get(body.email.toLowerCase());
    if (!user || user.password !== body.password) return problem(401, 'Unauthorized', 'Invalid email or password');
    state.sessionUserId = user.id;
    return HttpResponse.json(publicUser(user));
  }),

  http.post('*/api/auth/signup', async ({ request }) => {
    const rejected = csrfRejected(request);
    if (rejected) return rejected;
    const body = (await request.json()) as { email: string; displayName: string; password: string };
    if (state.users.has(body.email.toLowerCase())) {
      return problem(409, 'Conflict', 'Could not create an account with the details provided');
    }
    const user = backend.addUser(body.email, body.password, body.displayName);
    state.sessionUserId = user.id;
    return HttpResponse.json(publicUser(user), { status: 201 });
  }),

  http.post('*/api/auth/logout', ({ request }) => {
    const rejected = csrfRejected(request);
    if (rejected) return rejected;
    state.sessionUserId = null;
    return new HttpResponse(null, { status: 200 });
  }),
];
