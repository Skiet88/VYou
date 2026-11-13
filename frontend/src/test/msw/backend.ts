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
  status: 'ACTIVE' | 'SUSPENDED' | 'PENDING_DELETION';
  deletionScheduledAt?: string;
  createdAt: string;
  emailVerified: boolean;
  verificationToken?: string;
  passwordResetToken?: string;
}

interface StoredTurn {
  sequenceNumber: number;
  question: string;
  answer: string;
  evaluation: { clarity: number; technical: number; confidence: number; overall: number; strengths: string[]; improvements: string[] };
  answeredAt: string;
}

interface StoredInterview {
  id: string;
  ownerId: string;
  jobRole: string;
  level: string;
  feedbackMode: 'INSTANT' | 'END_OF_INTERVIEW';
  status: 'IN_PROGRESS' | 'COMPLETED';
  totalQuestions: number;
  currentQuestion: string | null;
  turns: StoredTurn[];
  summary: Record<string, unknown> | null;
  startedAt: string;
  endedAt: string | null;
}

const state = {
  users: new Map<string, StoredUser>(),
  sessionUserId: null as string | null,
  lastRequestHeaders: null as Headers | null,
  interviews: new Map<string, StoredInterview>(),
  /** Number of upcoming AI calls that should fail with 503 (simulates the provider being down). */
  aiFailures: 0,
  summaryRequests: 0,
};

export const TOTAL_QUESTIONS = 5;
export const MIN_WORDS = 5;
export const question = (n: number) => `Interview question number ${n}?`;

/** A completed, summarised turn for seeding finished interviews in tests. */
export const scoredTurn = (n: number, answer = `My answer to question ${n}, with a real example.`) => ({
  sequenceNumber: n,
  question: question(n),
  answer,
  evaluation: { clarity: 8, technical: 7, confidence: 6, overall: 7, strengths: ['Concrete example'], improvements: ['Quantify the impact'] },
  answeredAt: new Date().toISOString(),
});

export const backend = {
  /** Puts an existing user straight into PENDING_DELETION, as if they had already requested it. */
  scheduleDeletion(email: string, daysRemaining = 30) {
    const user = state.users.get(email.toLowerCase());
    if (!user) throw new Error(`No user ${email}`);
    user.status = 'PENDING_DELETION';
    user.deletionScheduledAt = new Date(Date.now() + daysRemaining * 86_400_000).toISOString();
  },
  reset() {
    state.users.clear();
    state.sessionUserId = null;
    state.lastRequestHeaders = null;
    state.interviews.clear();
    state.aiFailures = 0;
    state.summaryRequests = 0;
  },
  /** The next `count` AI-backed calls fail with 503. */
  failAi(count = 1) {
    state.aiFailures = count;
  },
  get summaryRequests() {
    return state.summaryRequests;
  },
  interview(id: string) {
    return state.interviews.get(id);
  },
  /** Seeds an interview owned by `email` directly, e.g. to test resuming or other users' interviews. */
  addInterview(email: string, overrides: Partial<StoredInterview> = {}) {
    const owner = state.users.get(email.toLowerCase());
    if (!owner) throw new Error(`No user ${email}`);
    const interview: StoredInterview = {
      id: crypto.randomUUID(),
      ownerId: owner.id,
      jobRole: 'Java Developer',
      level: 'JUNIOR',
      feedbackMode: 'INSTANT',
      status: 'IN_PROGRESS',
      totalQuestions: TOTAL_QUESTIONS,
      currentQuestion: question(1),
      turns: [],
      summary: null,
      startedAt: new Date(Date.UTC(2026, 0, 1, 9, 0, state.interviews.size)).toISOString(),
      endedAt: null,
      ...overrides,
    };
    state.interviews.set(interview.id, interview);
    return interview;
  },
  /**
   * Verified by default: most tests seed a user to use the app right away, the same way the real
   * backend's test suite uses `signupVerified`. Pass `emailVerified: false` to test the block.
   */
  addUser(
    email: string,
    password: string,
    displayName = 'Test User',
    role: 'USER' | 'ADMIN' = 'USER',
    emailVerified = true,
  ) {
    const user: StoredUser = {
      id: crypto.randomUUID(),
      email: email.toLowerCase(),
      displayName,
      password,
      role,
      status: 'ACTIVE',
      createdAt: new Date(Date.UTC(2026, 0, 1, 8, 0, state.users.size)).toISOString(),
      emailVerified,
      verificationToken: emailVerified ? undefined : crypto.randomUUID(),
    };
    state.users.set(user.email, user);
    return user;
  },
  /** The current verification token for an unverified user, as a real email link would carry it. */
  verificationToken(email: string) {
    return state.users.get(email.toLowerCase())?.verificationToken ?? null;
  },
  /** The current password-reset token for a user, as a real email link would carry it. */
  passwordResetToken(email: string) {
    return state.users.get(email.toLowerCase())?.passwordResetToken ?? null;
  },
  passwordOf(email: string) {
    return state.users.get(email.toLowerCase())?.password ?? null;
  },
  /** Creates a user and gives them a session in one step. */
  signedInAs(email = 'thabo@example.com', displayName = 'Thabo', role: 'USER' | 'ADMIN' = 'USER') {
    const user = backend.addUser(email, 'Secret123', displayName, role);
    state.sessionUserId = user.id;
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

const problem = (status: number, title: string, detail: string, properties?: Record<string, unknown>) =>
  HttpResponse.json(
    { type: 'about:blank', title, status, detail, ...(properties ? { properties } : {}) },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  );

const publicUser = ({ id, email, displayName, role, emailVerified, createdAt }: StoredUser) => ({
  id,
  email,
  displayName,
  role,
  emailVerified,
  createdAt,
});

function csrfRejected(request: Request) {
  state.lastRequestHeaders = request.headers;
  return request.headers.get('X-XSRF-TOKEN') !== TEST_CSRF_TOKEN ? problem(403, 'Forbidden', 'Forbidden') : null;
}

const currentUser = () => [...state.users.values()].find((u) => u.id === state.sessionUserId && u.status === 'ACTIVE');

/** Like the backend's ActiveAccountFilter: a suspended account's session no longer works. */
const requireSession = () => (currentUser() ? null : problem(401, 'Unauthorized', 'Authentication is required'));

/** Like the backend's EmailVerificationFilter: blocked everywhere except auth/account/verification. */
const requireVerified = () =>
  currentUser()?.emailVerified === false
    ? problem(
        403,
        'Forbidden',
        'Verify your email before continuing. Check your inbox, or request a new verification email.',
        { code: 'EMAIL_NOT_VERIFIED' },
      )
    : null;

export const handlers = [
  http.get('*/api/auth/me', () => {
    const user = [...state.users.values()].find((u) => u.id === state.sessionUserId && u.status === 'ACTIVE');
    return user ? HttpResponse.json(publicUser(user)) : problem(401, 'Unauthorized', 'Authentication is required');
  }),

  http.post('*/api/auth/login', async ({ request }) => {
    const rejected = csrfRejected(request);
    if (rejected) return rejected;
    const body = (await request.json()) as { email: string; password: string };
    if (body.email === RATE_LIMITED_EMAIL) return problem(429, 'Too many requests', 'Too many requests. Please wait and try again.');
    const user = state.users.get(body.email.toLowerCase());
    if (!user || user.password !== body.password || user.status === 'SUSPENDED') {
      return problem(401, 'Unauthorized', 'Invalid email or password');
    }
    const accountReactivated = user.status === 'PENDING_DELETION';
    if (accountReactivated) {
      user.status = 'ACTIVE';
      user.deletionScheduledAt = undefined;
    }
    state.sessionUserId = user.id;
    return HttpResponse.json({ user: publicUser(user), accountReactivated });
  }),

  http.post('*/api/auth/signup', async ({ request }) => {
    const rejected = csrfRejected(request);
    if (rejected) return rejected;
    const body = (await request.json()) as { email: string; displayName: string; password: string };
    if (state.users.has(body.email.toLowerCase())) {
      return problem(409, 'Conflict', 'Could not create an account with the details provided');
    }
    const user = backend.addUser(body.email, body.password, body.displayName, 'USER', false);
    state.sessionUserId = user.id;
    return HttpResponse.json(publicUser(user), { status: 201 });
  }),

  http.post('*/api/auth/logout', ({ request }) => {
    const rejected = csrfRejected(request);
    if (rejected) return rejected;
    state.sessionUserId = null;
    return new HttpResponse(null, { status: 200 });
  }),

  http.post('*/api/email-verification/verify', async ({ request }) => {
    const { token } = (await request.json()) as { token: string };
    const user = [...state.users.values()].find((u) => u.verificationToken === token);
    if (!user) return problem(400, 'Invalid request', 'That verification link is invalid or has expired.');
    user.emailVerified = true;
    user.verificationToken = undefined;
    return new HttpResponse(null, { status: 200 });
  }),

  http.post('*/api/email-verification/resend', ({ request }) => {
    const rejected = csrfRejected(request) ?? requireSession();
    if (rejected) return rejected;
    const me = currentUser()!;
    if (!me.emailVerified) me.verificationToken = crypto.randomUUID();
    return new HttpResponse(null, { status: 200 });
  }),

  http.post('*/api/password-reset/request', async ({ request }) => {
    const rejected = csrfRejected(request);
    if (rejected) return rejected;
    const { email } = (await request.json()) as { email: string };
    // Same response either way, or none at all: never reveals whether an email is registered.
    const user = state.users.get(email.toLowerCase());
    if (user) user.passwordResetToken = crypto.randomUUID();
    return new HttpResponse(null, { status: 200 });
  }),

  http.post('*/api/password-reset/reset', async ({ request }) => {
    const rejected = csrfRejected(request);
    if (rejected) return rejected;
    const { token, newPassword } = (await request.json()) as { token: string; newPassword: string };
    const user = [...state.users.values()].find((u) => u.passwordResetToken === token);
    if (!user) return problem(400, 'Invalid request', 'That password reset link is invalid or has expired.');
    user.password = newPassword;
    user.passwordResetToken = undefined;
    return new HttpResponse(null, { status: 200 });
  }),
];

// ---- Interviews: same rules as the real backend (ownership -> 404, screening -> 422 with reasons,
// feedback withheld in END_OF_INTERVIEW mode until completion, auto-complete at the limit). ----

const notFound = () => problem(404, 'Not found', 'Interview session not found');

function aiUnavailable() {
  if (state.aiFailures > 0) {
    state.aiFailures -= 1;
    return problem(503, 'AI provider unavailable', 'Rate limit reached');
  }
  return null;
}

function owned(id: string) {
  const interview = state.interviews.get(id);
  return interview && interview.ownerId === state.sessionUserId ? interview : null;
}

const reveals = (i: StoredInterview) => i.feedbackMode === 'INSTANT' || i.status !== 'IN_PROGRESS';

function sessionJson(i: StoredInterview) {
  return {
    id: i.id,
    jobRole: i.jobRole,
    level: i.level,
    mode: 'TEXT',
    status: i.status,
    feedbackMode: i.feedbackMode,
    totalQuestions: i.totalQuestions,
    questionsAnswered: i.turns.length,
    currentQuestion: i.currentQuestion,
    startedAt: i.startedAt,
    endedAt: i.endedAt,
  };
}

const turnJson = (t: StoredTurn, reveal: boolean) => ({ ...t, evaluation: reveal ? t.evaluation : null });

function makeSummary(i: StoredInterview) {
  const avg = (key: 'clarity' | 'technical' | 'confidence' | 'overall') =>
    Math.round((i.turns.reduce((sum, t) => sum + t.evaluation[key], 0) / i.turns.length) * 10) / 10;
  return {
    questionsAnswered: i.turns.length,
    averageClarity: avg('clarity'),
    averageTechnical: avg('technical'),
    averageConfidence: avg('confidence'),
    averageOverall: avg('overall'),
    feedback: 'You gave concrete examples. Work on structuring longer answers.',
    topStrengths: ['Concrete examples'],
    focusAreas: ['Structure longer answers'],
    createdAt: new Date().toISOString(),
  };
}

handlers.push(
  http.post('*/api/interviews', async ({ request }) => {
    const rejected = csrfRejected(request) ?? requireSession() ?? requireVerified() ?? aiUnavailable();
    if (rejected) return rejected;
    const body = (await request.json()) as { jobRole: string; level: string; feedbackMode?: 'INSTANT' | 'END_OF_INTERVIEW' };
    const owner = [...state.users.values()].find((u) => u.id === state.sessionUserId)!;
    const interview = backend.addInterview(owner.email, {
      jobRole: body.jobRole.trim(),
      level: body.level,
      feedbackMode: body.feedbackMode ?? 'INSTANT',
    });
    return HttpResponse.json(sessionJson(interview), { status: 201 });
  }),

  http.get('*/api/interviews/mine', ({ request }) => {
    const rejected = requireSession() ?? requireVerified();
    if (rejected) return rejected;
    const url = new URL(request.url);
    const page = Number(url.searchParams.get('page') ?? '0');
    const size = Number(url.searchParams.get('size') ?? '10');
    const status = url.searchParams.get('status');
    if (!Number.isInteger(page) || page < 0 || !Number.isInteger(size) || size < 1 || size > 50) {
      return problem(400, 'Invalid request', 'Invalid paging parameters');
    }
    const mine = [...state.interviews.values()]
      .filter((i) => i.ownerId === state.sessionUserId && (!status || i.status === status))
      .sort((a, b) => b.startedAt.localeCompare(a.startedAt));
    const items = mine.slice(page * size, page * size + size).map((i) => ({
      id: i.id,
      jobRole: i.jobRole,
      level: i.level,
      status: i.status,
      feedbackMode: i.feedbackMode,
      totalQuestions: i.totalQuestions,
      questionsAnswered: i.turns.length,
      averageOverall: (i.summary?.averageOverall as number | undefined) ?? null,
      startedAt: i.startedAt,
      endedAt: i.endedAt,
    }));
    return HttpResponse.json({ items, page, size, totalItems: mine.length, totalPages: Math.ceil(mine.length / size) });
  }),

  http.get('*/api/interviews/:id', ({ params }) => {
    const rejected = requireSession() ?? requireVerified();
    if (rejected) return rejected;
    const i = owned(String(params.id));
    if (!i) return notFound();
    return HttpResponse.json({ session: sessionJson(i), turns: i.turns.map((t) => turnJson(t, reveals(i))), summary: i.summary });
  }),

  http.post('*/api/interviews/:id/answers', async ({ request, params }) => {
    const rejected = csrfRejected(request) ?? requireSession() ?? requireVerified();
    if (rejected) return rejected;
    const i = owned(String(params.id));
    if (!i) return notFound();
    if (i.status !== 'IN_PROGRESS') return problem(409, 'Conflict', 'Interview session is already completed');
    const { answer } = (await request.json()) as { answer: string };
    const words = answer.trim().split(/\s+/).filter(Boolean).length;
    if (words < MIN_WORDS) {
      return HttpResponse.json(
        { title: 'Answer rejected by screening', status: 422, detail: 'Answer rejected', reasons: [`Answer is too short (${words} words, minimum ${MIN_WORDS})`] },
        { status: 422, headers: { 'Content-Type': 'application/problem+json' } },
      );
    }
    const unavailable = aiUnavailable();
    if (unavailable) return unavailable;
    const turn: StoredTurn = {
      sequenceNumber: i.turns.length + 1,
      question: i.currentQuestion!,
      answer,
      evaluation: { clarity: 8, technical: 7, confidence: 6, overall: 7, strengths: ['Concrete example'], improvements: ['Quantify the impact'] },
      answeredAt: new Date().toISOString(),
    };
    i.turns.push(turn);
    if (turn.sequenceNumber >= i.totalQuestions) {
      i.status = 'COMPLETED';
      i.currentQuestion = null;
      i.endedAt = new Date().toISOString();
    } else {
      i.currentQuestion = question(turn.sequenceNumber + 1);
    }
    return HttpResponse.json({ turn: turnJson(turn, reveals(i)), nextQuestion: i.currentQuestion, sessionStatus: i.status });
  }),

  http.post('*/api/interviews/:id/complete', ({ request, params }) => {
    const rejected = csrfRejected(request) ?? requireSession() ?? requireVerified();
    if (rejected) return rejected;
    const i = owned(String(params.id));
    if (!i) return notFound();
    if (i.status !== 'IN_PROGRESS') return problem(409, 'Conflict', 'Interview session is already completed');
    i.status = 'COMPLETED';
    i.currentQuestion = null;
    i.endedAt = new Date().toISOString();
    if (i.turns.length > 0 && state.aiFailures === 0) i.summary = makeSummary(i);
    else if (state.aiFailures > 0) state.aiFailures -= 1;
    return HttpResponse.json({ session: sessionJson(i), summary: i.summary });
  }),

  http.post('*/api/interviews/:id/summary', ({ request, params }) => {
    const rejected = csrfRejected(request) ?? requireSession() ?? requireVerified();
    if (rejected) return rejected;
    const i = owned(String(params.id));
    if (!i) return notFound();
    state.summaryRequests += 1;
    if (i.summary) return HttpResponse.json(i.summary);
    if (i.status === 'IN_PROGRESS') return problem(409, 'Conflict', 'Complete the interview before requesting its summary');
    const unavailable = aiUnavailable();
    if (unavailable) return unavailable;
    i.summary = makeSummary(i);
    return HttpResponse.json(i.summary);
  }),
);

// ---- Admin: same rules as the backend (admin role required; can't suspend yourself or an admin). ----

function adminUserJson(u: StoredUser) {
  const theirs = [...state.interviews.values()].filter((i) => i.ownerId === u.id);
  const averages = theirs.map((i) => i.summary?.averageOverall as number | undefined).filter((v): v is number => v !== undefined);
  return {
    id: u.id,
    email: u.email,
    displayName: u.displayName,
    role: u.role,
    status: u.status,
    createdAt: u.createdAt,
    interviewsTotal: theirs.length,
    interviewsCompleted: theirs.filter((i) => i.status === 'COMPLETED').length,
    averageOverall: averages.length ? Math.round((averages.reduce((a, b) => a + b, 0) / averages.length) * 10) / 10 : null,
  };
}

function isLastAdmin(user: StoredUser) {
  return user.role === 'ADMIN' && [...state.users.values()].filter((u) => u.role === 'ADMIN').length <= 1;
}

function requireAdmin() {
  const me = currentUser();
  if (!me) return problem(401, 'Unauthorized', 'Authentication is required');
  if (me.role !== 'ADMIN') return problem(403, 'Forbidden', 'You do not have permission to do that');
  return null;
}

handlers.push(
  http.get('*/api/admin/users', ({ request }) => {
    const rejected = requireAdmin() ?? requireVerified();
    if (rejected) return rejected;
    const url = new URL(request.url);
    const search = (url.searchParams.get('search') ?? '').toLowerCase();
    const status = url.searchParams.get('status');
    const page = Number(url.searchParams.get('page') ?? '0');
    const size = Number(url.searchParams.get('size') ?? '20');
    const matching = [...state.users.values()]
      .filter((u) => (!status || u.status === status) && (!search || u.email.includes(search) || u.displayName.toLowerCase().includes(search)))
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt));
    return HttpResponse.json({
      items: matching.slice(page * size, page * size + size).map(adminUserJson),
      page,
      size,
      totalItems: matching.length,
      totalPages: Math.ceil(matching.length / size),
    });
  }),

  http.post('*/api/admin/users/:id/:action', ({ request, params }) => {
    const rejected = csrfRejected(request) ?? requireAdmin() ?? requireVerified();
    if (rejected) return rejected;
    const target = [...state.users.values()].find((u) => u.id === params.id);
    if (!target) return problem(404, 'Not found', 'User not found');
    if (params.action === 'suspend') {
      if (target.id === state.sessionUserId) return problem(409, 'Conflict', "You can't suspend your own account");
      if (target.role === 'ADMIN') return problem(409, 'Conflict', "Admin accounts can't be suspended here");
      target.status = 'SUSPENDED';
    } else if (params.action === 'reinstate') {
      target.status = 'ACTIVE';
    } else {
      return problem(404, 'Not found', 'Not found');
    }
    return HttpResponse.json(adminUserJson(target));
  }),
);

export const adminBackend = {
  status(email: string) {
    return state.users.get(email.toLowerCase())?.status;
  },
  suspend(email: string) {
    const u = state.users.get(email.toLowerCase());
    if (u) u.status = 'SUSPENDED';
  },
};

handlers.push(
  http.post('*/api/account/delete', async ({ request }) => {
    const rejected = csrfRejected(request) ?? requireSession();
    if (rejected) return rejected;
    const me = currentUser()!;
    const body = (await request.json()) as { password: string };
    if (body.password !== me.password) return problem(401, 'Unauthorized', 'Incorrect password');
    if (isLastAdmin(me)) {
      return problem(409, 'Conflict', "You're the only admin. Promote another admin before deleting your account.");
    }
    me.status = 'PENDING_DELETION';
    me.deletionScheduledAt = new Date(Date.now() + 30 * 86_400_000).toISOString();
    state.sessionUserId = null;
    return new HttpResponse(null, { status: 200 });
  }),
);
