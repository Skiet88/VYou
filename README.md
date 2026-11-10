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

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/interviews` | `{"jobRole","level":"INTERN\|JUNIOR\|MID\|SENIOR","mode"}` → session with first question |
| POST | `/api/interviews/{id}/answers` | `{"answer"}` → evaluation (scores 0–10, strengths, improvements) and next question |
| POST | `/api/interviews/{id}/complete` | end the session |
| GET  | `/api/interviews/{id}` | session and all turns |

A rejected answer (too short, or a repeat of the question) returns `422` with `reasons`, and no LLM
quota is used. When the daily rate limit is reached the API returns `503`.

## Tests

`./mvnw test` runs the factory, screening and evaluator unit tests, plus an end-to-end API test on
H2 with a fake LLM. No database or API key is needed.
