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
├── components   UI only. ui/ (shared pieces), auth/, app/, pages/. Talk to hooks, never services.
├── hooks        useCurrentUser/useLogin/useSignup/useLogout, useZodForm, useTheme. Talk to services.
├── services     HttpClient (CSRF header, problem+json -> ApiError, schema-checked responses), AuthService.
├── schemas      Zod schemas: form validation and response validation from one definition.
├── state        Query client, theme provider, service injection.
├── util         Cookie reading, safe in-app redirects.
├── config       Environment-based config (API path; no hosts, no secrets).
└── styles       Design tokens (one type scale, light/dark) and base styles.
```

The layer rules are enforced by ESLint (`eslint.config.js`), as are `react/no-danger`, no
`innerHTML`, and no inline `style` (the backend's `default-src 'self'` CSP would block it).
`AppProviders.tsx` and `App.tsx` at the root are the composition root that wires it all together.

Tests sit next to the code they cover and run against an MSW fake of the backend contract (same
generic error messages, CSRF enforced). They include automated axe accessibility checks, and
`styles/contrast.test.ts` checks every text/background and border pair in both themes against WCAG
AA using the real token values.

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
| GET  | `/api/interviews/mine` | the caller's sessions, newest first |
| POST | `/api/interviews/{id}/answers` | `{"answer"}` → evaluation and next question (evaluation withheld until completion in `END_OF_INTERVIEW` mode) |
| POST | `/api/interviews/{id}/complete` | end the session; includes the GPT-4o summary when available |
| POST | `/api/interviews/{id}/summary` | get the summary, generating it if completion couldn't |
| GET  | `/api/interviews/{id}` | session, all turns, and summary |

A fixed number of questions per interview (default 5, `vyou.interview.question-count`) auto-completes
the session on the last answer. A rejected answer (too short, or a repeat of the question) returns
`422` with `reasons`, and no LLM quota is used. When the daily rate limit is reached the API returns
`503`.

## Tests

`./mvnw test` needs no database or API key. It covers factories, screening, evaluator parsing, the
LLM client's retry rules (against a mock server), encryption and PII redaction, and end-to-end API
flows (auth, ownership/IDOR, CSRF, rate limiting, feedback modes) on H2 with a fake LLM.
