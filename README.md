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
./mvnw spring-boot:run
```

The app starts without a token. AI endpoints then return `503` with a message explaining what to set.

To use a different OpenAI-compatible provider, set `VYOU_LLM_BASE_URL`, `VYOU_LLM_MODEL` and
`VYOU_LLM_API_KEY`. For Groq: `https://api.groq.com/openai/v1` with `openai/gpt-oss-120b`.

## API

Full contract for the frontend: `/swagger-ui.html` (spec at `/v3/api-docs`).
CORS allows `http://localhost:5173` and `:3000` by default (`VYOU_CORS_ALLOWED_ORIGINS`).

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/users` | `{"email","displayName"}` → user (`409` if the email is taken) |
| GET  | `/api/users/{id}` | user |
| GET  | `/api/users/{id}/interviews` | the user's sessions, newest first |
| POST | `/api/interviews` | `{"userId","jobRole","level":"INTERN\|JUNIOR\|MID\|SENIOR","mode"}` → session with first question |
| POST | `/api/interviews/{id}/answers` | `{"answer"}` → evaluation (scores 0–10, strengths, improvements) and next question |
| POST | `/api/interviews/{id}/complete` | end the session; includes the GPT-4o summary when available |
| POST | `/api/interviews/{id}/summary` | get the summary, generating it if completion couldn't |
| GET  | `/api/interviews/{id}` | session, all turns, and summary |

A rejected answer (too short, or a repeat of the question) returns `422` with `reasons`, and no LLM
quota is used. When the daily rate limit is reached the API returns `503`.

## Tests

`./mvnw test` needs no database or API key. It covers factories, screening, evaluator parsing, the
LLM client's retry rules (against a mock server), and end-to-end API flows on H2 with a fake LLM,
including an AI outage partway through an interview.
