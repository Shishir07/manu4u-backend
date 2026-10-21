# ManU4U Backend

A conversational AI backend for **Manchester United** fans. Ask a question in plain English —
"Who scored today?", "Where are we in the table?", "Why did the 1999 Treble matter?" — and an
LLM agent answers by calling real tools: live data from API-Football, a PostgreSQL store of
reference data, and a vector-search knowledge base of club history and news. Answers come back
with the sources the agent actually used, so they can be checked.

> **Status:** work in progress, not production-hardened. Admin and debug endpoints are
> unauthenticated — run it locally, do not expose it publicly as-is. See
> [HISTORY.md §7](HISTORY.md#7-current-status-known-bugs-and-improvements).

## Who it's for

- **Fans, via a frontend.** The backend serves a chat UI through `POST /ask/stream`.
- **Developers** who want a worked example of a grounded, tool-calling RAG agent in Spring Boot,
  including deterministic (no LLM-as-judge) evaluation.

## What it does

- Answers live-match questions: fixtures, results, scorers, lineups, match stats, standings,
  player stats, top scorers/assists and odds.
- Understands time ("last week", "next Saturday", "this season").
- Answers history and topical questions from a curated knowledge base (Qdrant), with
  freshness-aware ranking so recent news outranks superseded reports.
- Returns `confidence`, `sources` and a server-side `toolTrace` listing every tool that
  actually ran.
- Keeps per-session conversation memory and can stream answers token by token (SSE).

## Built with

| | |
|---|---|
| Language / framework | Java 21, Spring Boot 3.4.5 |
| AI | Spring AI 1.0.0, OpenAI `gpt-4o` (chat), `text-embedding-3-small` (embeddings) |
| Data | PostgreSQL 16 (Spring Data JPA), Qdrant (vector store) |
| External API | [API-Football v3](https://www.api-football.com/documentation-v3) |
| HTTP | Spring MVC + WebClient, Spring Security, SpringDoc OpenAPI |
| Tooling | Maven (wrapper included), Docker Compose, Lombok |

## How it works

```
Client ──► /ask or /ask/stream  (X-Api-Key)
              │
        AgentOrchestrator ──► LLM with tool calling
              │                    │
              │      ┌─────────────┼───────────────┐
              ▼      ▼             ▼               ▼
        session   API-Football  PostgreSQL     Qdrant
        memory    (live data)   (reference     (knowledge
                                 data, cache)   corpus)
```

The model decides which of 15 tools to call; backend code executes them and records the
`toolTrace`. Facts must come from tools, never from model memory. Details and the reasoning
behind the design are in [HISTORY.md](HISTORY.md).

## Getting started

### Prerequisites

- JDK 21
- Docker (for PostgreSQL and Qdrant)
- An [OpenAI API key](https://platform.openai.com/) and an
  [API-Football key](https://www.api-football.com/)

### 1. Configure

```bash
cp .env.example .env
# edit .env: set API_FOOTBALL_KEY, OPENAI_API_KEY, a local DB password
# (POSTGRES_PASSWORD and DB_PASSWORD must match) and MANU4U_GUEST_API_KEY
```

`.env` is git-ignored. No secrets are committed anywhere in this repository.

### 2. Start the databases

```bash
docker compose up -d        # PostgreSQL :5432, Qdrant :6333 (REST) / :6334 (gRPC)
```

### 3. Run the app

```bash
set -a; source .env; set +a
./mvnw spring-boot:run      # activates the `dev` profile automatically
```

The API is now at <http://localhost:8080>; Swagger UI at
<http://localhost:8080/swagger-ui.html>.

### 4. Load data (first run only)

```bash
# Reference data + Manchester United squad
curl -X POST localhost:8080/admin/sync/all

# Knowledge corpus -> Qdrant (idempotent: safe to re-run)
curl -X POST localhost:8080/admin/ingest/files
```

The knowledge-base tool is off by default. To let the agent use it:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--manu4u.knowledge-base-enabled=true
```

### 5. Ask something

```bash
curl -X POST localhost:8080/ask \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: $MANU4U_GUEST_API_KEY" \
  -d '{"question": "Why was 26 May 1999 significant for Manchester United?", "sessionId": "demo-1"}'
```

Streaming (what a chat UI uses):

```bash
curl -N -X POST localhost:8080/ask/stream \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: $MANU4U_GUEST_API_KEY" \
  -d '{"question": "Where are United in the table?"}'
```

## API overview

| Route | Purpose | Auth |
|---|---|---|
| `POST /ask` | Structured answer: `answer`, `evidence`, `sources`, `confidence`, `toolTrace` | `X-Api-Key` |
| `POST /ask/stream` | Same agent, plain-text tokens over SSE | `X-Api-Key` |
| `GET /tools/*` | Debug access to individual tools | none (dev only) |
| `POST /admin/sync/*`, `/admin/players/*` | Reference-data sync | none (dev only) |
| `POST /admin/ingest/files`, `/admin/ingest/text` | Knowledge ingestion | none (dev only) |

Full list: [HISTORY.md §2](HISTORY.md#2-architecture-as-built) or the Swagger UI.

## Configuration

Profiles: `dev` (default via Maven), `staging`, `prod`. Secrets are always read from the
environment:

| Variable | Used for |
|---|---|
| `OPENAI_API_KEY`, `API_FOOTBALL_KEY` | External services |
| `MANU4U_GUEST_API_KEY` | Shared key the frontend sends as `X-Api-Key` |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL (dev defaults for URL/username only) |
| `QDRANT_HOST`, `QDRANT_PORT` | Qdrant (dev defaults: `localhost`, `6334`) |

`staging` and `prod` use `ddl-auto: validate` and require every variable above.

## Project structure

```
src/main/java/com/manu4u/tools/
├── agent/        AgentOrchestrator, session memory, tool-call tracker
├── client/       ApiFootballClient + response DTOs
├── config/       Properties, ChatClient, RAG (splitter), WebClient, OpenAPI
├── controller/   /ask, /tools, /admin, /admin/sync, /admin/ingest, error handling
├── entity/       JPA entities (countries, leagues, seasons, teams, players, stats, fixture cache)
├── model/        Domain models returned by tools and the agent
├── repository/   Spring Data repositories
├── security/     API-key filter and Spring Security config
└── service/
    ├── *Tool.java        fixtures, events, lineups, standings, players, odds, time, knowledge base
    ├── tools/            @Tool wrappers exposed to the LLM
    ├── ingestion/        corpus loader, header parser, chunk + embed + store
    └── sync/             master-data sync
src/main/resources/
├── application*.yml      base, dev, staging, prod
└── knowledge/            curated markdown corpus (era-, moment-, legend-, club-, season-, topical-)
src/test/                 unit tests + deterministic eval harness (eval/)
```

## Testing

```bash
./mvnw test                         # unit tests (eval suite excluded)
./mvnw test -Peval                  # eval suite against a running instance
./mvnw test -Peval -Deval.base-url=http://localhost:8080
```

The eval suite sends questions to a live instance and grades tool selection, ordering,
parameters, answer content, confidence and latency from the `toolTrace` — no LLM judge.

## Docs

- [HISTORY.md](HISTORY.md) — how it was built, architecture, design decisions, corpus format,
  current status and known bugs
- [ROADMAP.md](ROADMAP.md) — what's next: the ingestion pipeline and retrieval correctness

## Contributing

`main` is changed **only through pull requests** — no direct pushes or merges. Work on a
feature branch, keep commits focused, and open a PR. Never commit secrets; use `.env` and
update `.env.example` with placeholders when adding a variable.

## License

[MIT](LICENSE)
