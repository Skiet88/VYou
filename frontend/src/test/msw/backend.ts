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

export const backend = {
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
      startedAt: new Date().toISOString(),
      endedAt: null,
      ...overrides,
    };
    state.interviews.set(interview.id, interview);
    return interview;
  },
  addUser(email: string, password: string, displayName = 'Test User', role: 'USER' | 'ADMIN' = 'USER') {
    const user: StoredUser = {
      id: crypto.randomUUID(),
      email: email.toLowerCase(),
      displayName,
      password,
      role,
      createdAt: new Date().toISOString(),
    };
    state.users.set(user.email, user);
    return user;
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

const requireSession = () => (state.sessionUserId ? null : problem(401, 'Unauthorized', 'Authentication is required'));

handlers.push(
  http.post('*/api/interviews', async ({ request }) => {
    const rejected = csrfRejected(request) ?? requireSession() ?? aiUnavailable();
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

  http.get('*/api/interviews/mine', () => {
    const rejected = requireSession();
    if (rejected) return rejected;
    const mine = [...state.interviews.values()].filter((i) => i.ownerId === state.sessionUserId).reverse();
    return HttpResponse.json(mine.map(sessionJson));
  }),

  http.get('*/api/interviews/:id', ({ params }) => {
    const rejected = requireSession();
    if (rejected) return rejected;
    const i = owned(String(params.id));
    if (!i) return notFound();
    return HttpResponse.json({ session: sessionJson(i), turns: i.turns.map((t) => turnJson(t, reveals(i))), summary: i.summary });
  }),

  http.post('*/api/interviews/:id/answers', async ({ request, params }) => {
    const rejected = csrfRejected(request) ?? requireSession();
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
    const rejected = csrfRejected(request) ?? requireSession();
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
    const rejected = csrfRejected(request) ?? requireSession();
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
