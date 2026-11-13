# VYou2 – AI Interview Simulator

A rebuild of VYou on a strict layered architecture. It uses Spring Boot 3.5, Java 17 and PostgreSQL,
and evaluates answers with GPT-4o on GitHub Models (free tier, daily rate limit).

## Layers

```
za.ac.skiet.vyou2
├── api          REST controllers. Depend only on InterviewFacade.
├── schemas      Request/response records (the API contract).
├── service      InterviewFacade (Facade) plus session/turn services (interface + impl).
├── screening    Local checks that reject unusable answers before any LLM call (Strategy).
├── llm          LlmClient (Strategy), answer evaluator, question generator, prompts.
├── repository   Spring Data repository interfaces.
├── factory      Validating factories: the only way to create or transition entities.
├── domain       Immutable JPA entities (no setters) and enums.
├── util         Validation guards and text helpers.
├── config       LLM properties/client wiring, Clock.
└── exception    Domain exceptions and the RFC 7807 ProblemDetail handler.
```

Each layer calls the next one only through interfaces. Entities have no setters. A state change
such as `InterviewSessionFactory.complete(session, now)` returns a new instance with the same id.

## Stability

- **Schema migrations:** Flyway owns the schema (`src/main/resources/db/migration`). Hibernate only
  validates it (`ddl-auto=validate`), so a mismatch fails at startup instead of silently changing tables.
- **Concurrency:** sessions use optimistic locking (`@Version`), and turns have a unique
  `(session_id, sequence_number)` constraint. A double-submitted answer gets `409`, never a duplicate turn.
- **AI failures never lose work:** answers are screened locally first. LLM calls run outside DB
  transactions. Network errors and 5xx get one retry; `429` (daily quota) is not retried. If generating
  the next question fails after an answer is saved, a fallback question is used. Completing an
  interview always succeeds, and a missing summary can be generated later with `POST /{id}/summary`.
- **Errors:** every error is an RFC 7807 `ProblemDetail` JSON body with a consistent status code.

## Running

```bash
export GITHUB_TOKEN=...   # fine-grained token with the "Models: read" permission
export DB_URL=jdbc:postgresql://localhost:5432/vyou2 DB_USERNAME=postgres DB_PASSWORD=...
export VYOU_ENCRYPTION_KEY=$(openssl rand -base64 32)   # AES-256 key for answers/feedback at rest
./mvnw spring-boot:run
```

The app refuses to start without `VYOU_ENCRYPTION_KEY`. It starts fine without `GITHUB_TOKEN`; AI
endpoints then return `503` with a message explaining what to set.

It also starts fine with no mail configured: signup still works, but the verification email is
just logged and dropped (`VYOU_MAIL_USERNAME` blank). To send real email, set a Gmail App Password
(Google Account → Security → 2-Step Verification → App passwords) as `VYOU_MAIL_USERNAME`/
`VYOU_MAIL_PASSWORD`:

```bash
export VYOU_MAIL_USERNAME=you@gmail.com VYOU_MAIL_PASSWORD=<16-char app password>
```

`VYOU_MAIL_HOST`/`VYOU_MAIL_PORT` default to Gmail's SMTP (`smtp.gmail.com:587`); set them to use a
different provider. `VYOU_FRONTEND_BASE_URL` overrides the origin put into verification links -
leave it unset for the normal single-origin deployment described in "Production build" below,
where it's derived from the incoming request.

To use a different OpenAI-compatible provider, set `VYOU_LLM_BASE_URL`, `VYOU_LLM_MODEL` and
`VYOU_LLM_API_KEY`. For Groq: `https://api.groq.com/openai/v1` with `openai/gpt-oss-120b`.

## Frontend

```bash
cd frontend
npm install
npm run dev     # http://localhost:5173 (start the backend first)
npm run check   # typecheck + lint + tests + npm audit
```

Vite proxies `/api/*` to `http://localhost:8080` (`vite.config.ts`), so the browser only ever talks
to `localhost:5173`. That keeps the session cookie's `SameSite=Lax` setting working in dev, matching
production behind a single reverse-proxied origin. Never point the frontend at the backend's origin
directly; that would force a weaker `SameSite=None` cookie. Browsers treat `localhost` as a secure
context, so the backend's `Secure` session cookie works over plain HTTP locally without any flag.
See `frontend/.env.example` for the two optional overrides.

```
frontend/src
├── components   UI only. ui/ (shared pieces), app/ (shell, nav), auth/, interview/ (wizard, interview flow),
│                pages/. Talk to hooks, never services.
├── hooks        Auth and interview queries/mutations, useZodForm, useTheme, error wording. Talk to services.
├── services     HttpClient (CSRF header, problem+json -> ApiError, schema-checked responses), AuthService,
│                InterviewService.
├── schemas      Zod schemas: form validation and response validation from one definition.
├── state        Query client, theme provider, service injection.
├── util         Cookie reading, safe in-app redirects.
├── config       Environment-based config (API path; no hosts, no secrets).
└── styles       Design tokens (one type scale, light/dark) and base styles.
```

The layer rules are enforced by ESLint (`eslint.config.js`), as are `react/no-danger`, no
`innerHTML`, and no inline `style` (the backend's `default-src 'self'` CSP would block it).
`AppProviders.tsx` and `App.tsx` at the root are the composition root that wires it all together.

Screens so far: sign in / create account; the dashboard (resume an unfinished interview or start
one, completed count and recent average, last three results); the signed-in shell (sidebar with Dashboard, Interviews and
admin-only Users, an account menu with theme and sign out, breadcrumbs, skip link, focus moved to each
new page's heading); the three-step new-interview wizard (`/interviews/new`, step in the URL so Back
works); and the interview itself (`/interviews/:id`): question, answer, practice-mode feedback or a
saved confirmation, end-early confirmation, and a summary-first completion view with a collapsed
question-by-question breakdown; the Interviews list (`/interviews`: All / In progress / Completed
filter, 10 per page, filter and page in the URL); for admins only, Users (`/users`: search,
Active / Suspended filter, suspend with confirmation, reinstate); and email verification
(`/verify-email/pending`: shown instead of the app to a signed-in-but-unverified account, with a
resend button and sign out; `/verify-email?token=...`: the public page a verification email's link
opens, signed in or out). A 401 from any request is treated
as "signed out" everywhere: the user is sent to sign in (and back afterwards) and cached data from
the old session is dropped. Typed answers are
kept in memory only and survive every failure (rejection, AI unavailable, rate limit).

Tests sit next to the code they cover and run against an MSW fake of the backend contract (same
generic error messages, CSRF enforced). They include automated axe accessibility checks, and
`styles/contrast.test.ts` checks every text/background and border pair in both themes against WCAG
AA using the real token values.

## Production build (one deployable, one origin)

```bash
(cd frontend && npm ci && npm run build)   # writes frontend/dist
./mvnw package                              # the jar now includes frontend/dist as static files
java -jar target/vyou2-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

The backend serves the built app from the same origin as the API, so the session cookie stays
`SameSite=Lax` and the backend's CSP and security headers cover the app too. Client-side routes
(`/interviews/123`) get `index.html`, so deep links and reloads work; `/api/**` and missing files
never fall back to it. Hashed `/assets/**` are cached for a year; `index.html` is always revalidated.
Run it behind HTTPS (a reverse proxy terminating TLS): the `prod` profile keeps the session cookie
`Secure` and turns the API docs off. Without `npm run build` first, `./mvnw package` still works and
simply produces an API-only jar.

## Authentication & security

- Sessions are server-side (httpOnly, `SameSite=Lax` cookie); no token is ever readable from
  JavaScript. `POST /api/auth/signup`, `/login`, `/logout`, `GET /api/auth/me`.
- CSRF protection is on for every state-changing request: read the `XSRF-TOKEN` cookie and echo it
  back in an `X-XSRF-TOKEN` header (the standard Spring Security SPA pattern).
- Every interview endpoint acts for the authenticated caller only; a session ID that exists but
  belongs to someone else returns `404`, identical to a missing one.
- Rate limits apply to `/api/auth/*` (by IP) and the AI-calling endpoints (by user); both return
  `429` when exceeded. CORS is restricted to `VYOU_CORS_ALLOWED_ORIGINS` (default:
  `http://localhost:5173,http://localhost:3000`).
- Role and status are re-read from the database on every authenticated request
  (`ActiveAccountFilter`): suspending a user signs them out on their very next request, and a role
  change applies immediately, without a new login.
- New accounts must verify their email before using any API endpoint except auth, account
  deletion and verification itself (`EmailVerificationFilter`, `EMAIL_NOT_VERIFIED` error code);
  existing accounts from before this feature are considered already verified. The verification
  link's token is a random 32-byte value; only its SHA-256 hash is stored, and it expires after 24
  hours. `POST /api/email-verification/verify` (public) and `/resend` (signed in; a no-op once
  verified). This only guards the API: a signed-in but unverified visitor can still load the SPA
  shell directly (e.g. on refresh), and the frontend's own route guard sends them to the
  pending-verification page once it reads `emailVerified: false` from `GET /api/auth/me`.
- Self-service account deletion has a 30-day grace period (`UserStatus.PENDING_DELETION`,
  `AccountDeletionService`): requesting it re-checks the password, signs the user out everywhere
  immediately, and is blocked for the last remaining admin. Signing in again before the 30 days are
  up cancels it (`LoginResponse.accountReactivated`). A daily job (`AccountPurgeScheduler`,
  `vyou.account.purge-cron`) permanently deletes accounts once their grace period elapses: their
  interviews, turns and summaries first, then the account itself.
- Admins (`/api/admin/**`, `ROLE_ADMIN`) can list users with aggregate numbers and suspend or
  reinstate them, but never see interview content, and can't suspend themselves, other admins, or
  an account already scheduled for deletion.
  Admin actions are written to the `vyou.audit` logger with ids only. There is deliberately no
  promote-to-admin endpoint; create the first admin directly in the database:
  `UPDATE users SET role = 'ADMIN' WHERE email = 'you@example.com';`
- `/swagger-ui.html` / `/v3/api-docs` are available by default, off in the `prod` profile
  (`application-prod.properties`).

## API

Full contract: `/swagger-ui.html` (spec at `/v3/api-docs`). Every endpoint below except signup/login
requires an active session.

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/auth/signup` | `{"email","displayName","password"}` → user, and logs in |
| POST | `/api/auth/login` | `{"email","password"}` → user |
| POST | `/api/auth/logout` | ends the session |
| GET  | `/api/auth/me` | the current user |
| POST | `/api/interviews` | `{"jobRole","level":"INTERN\|JUNIOR\|MID\|SENIOR","mode","feedbackMode":"INSTANT\|END_OF_INTERVIEW"}` → session with first question |
| GET  | `/api/interviews/mine` | `?page=0&size=10&status=IN_PROGRESS\|COMPLETED` → a page of the caller's interviews, newest first (size 1–50) |
| POST | `/api/interviews/{id}/answers` | `{"answer"}` → evaluation and next question (evaluation withheld until completion in `END_OF_INTERVIEW` mode) |
| POST | `/api/interviews/{id}/complete` | end the session; includes the GPT-4o summary when available |
| POST | `/api/interviews/{id}/summary` | get the summary, generating it if completion couldn't |
| GET  | `/api/interviews/{id}` | session, all turns, and summary |
| GET  | `/api/admin/users` | admin: `?search=&status=ACTIVE\|SUSPENDED&page=0&size=20` → users with interview counts and average score |
| POST | `/api/admin/users/{id}/suspend` | admin: suspend (not yourself, not another admin) |
| POST | `/api/admin/users/{id}/reinstate` | admin: reinstate |
| POST | `/api/account/delete` | `{"password"}` → schedules the caller's own account for deletion in 30 days and signs them out everywhere |
| POST | `/api/email-verification/verify` | `{"token"}` → verifies the account the token belongs to (public: works signed in or out) |
| POST | `/api/email-verification/resend` | sends the caller a fresh verification email; a no-op if already verified |

A fixed number of questions per interview (default 5, `vyou.interview.question-count`) auto-completes
the session on the last answer. A rejected answer (too short, or a repeat of the question) returns
`422` with `reasons`, and no LLM quota is used. When the daily rate limit is reached the API returns
`503`.

## Tests

`./mvnw test` needs no database or API key. It covers factories, screening, evaluator parsing, the
LLM client's retry rules (against a mock server), encryption and PII redaction, and end-to-end API
flows (auth, ownership/IDOR, CSRF, rate limiting, feedback modes) on H2 with a fake LLM.
