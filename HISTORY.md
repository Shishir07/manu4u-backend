# ManU4U — Project History

How the backend got to its current state, and why it looks the way it does. This file replaces
the earlier planning notes (`CurrentState.MD`, `Starter.MD`, `Future.MD`, `PRODUCTION_PLAN.md`,
`PROJECT_SUMMARY.md`, `CORPUS_GUIDE.md`, `APIFootball.MD`); their full text remains in git
history. For what comes next, see [ROADMAP.md](ROADMAP.md). For setup, see [README.md](README.md).

---

## 1. Build timeline

| Stage | What was built |
|---|---|
| **1. Tool wrappers** | `ApiFootballClient` (WebClient), normalized domain models, five tools (time, fixtures, events, lineups, statistics), `/tools/*` debug endpoints, global error handler, Swagger. |
| **2. Agent loop** | Spring AI `ChatClient` with OpenAI `gpt-4o` (temperature 0) and `@Tool`-annotated methods. `POST /ask` returns `{answer, evidence, sources, confidence, toolTrace}`. In-memory multi-turn session history. |
| **3. Wider toolset** | Standings, player stats (DB name lookup, then API), top scorers/assists, pre-match and live odds, date-range fixtures, a natural-language time resolver. PostgreSQL fixture cache. |
| **4. RAG foundation** | Qdrant vector store, text splitter, `/admin/ingest/*` endpoints, `search_knowledge_base` tool. |
| **5. Production readiness** | Config split into base/dev/staging/prod profiles with no secrets in files, `ddl-auto: validate` outside dev, `@Transactional` on multi-row writes, N+1 fix in squad sync, request validation, Hikari pool tuning. |
| **6. API correctness** | Audit against the API-Football docs (details in §4). |
| **7. Master data** | Reference tables (`countries`, `leagues`, `seasons`, `teams`, `players`, `player_statistics`) with our own surrogate keys, and an idempotent `MasterDataSyncService` behind `/admin/sync/*`. |
| **8. Access control** | Spring Security with a shared guest API key (`X-Api-Key`) on `/ask` and `/ask/stream`; stateless, JSON 401s, CORS. |
| **9. Streaming** | `POST /ask/stream` — Server-Sent Events, plain-text tokens, history saved when the stream completes. |
| **10. Knowledge corpus + ingestion** | 18-file curated markdown corpus, header-to-metadata parser, idempotent re-ingest, 800-token chunking (PR #1). |
| **11. Freshness-aware retrieval** | Over-fetch and re-rank keyed on volatility (PR #2, in review at the time of writing). |

---

## 2. Architecture as built

```
POST /ask | /ask/stream   (X-Api-Key)
        │
        ▼
AgentOrchestrator ── ConversationHistoryService (in-memory, per sessionId)
        │            └─ ToolCallTracker (records every tool actually executed → toolTrace)
        ▼
Spring AI ChatClient (gpt-4o) ── tool calling ──►  LiveMatchToolsService
                                                   KnowledgeBaseToolService
        ┌───────────────────────────────┬───────────────────────────┐
        ▼                               ▼                           ▼
 API-Football (WebClient)       PostgreSQL (JPA)             Qdrant (vector store)
 fixtures, events, lineups,     fixture cache, reference     curated knowledge corpus
 stats, standings, players,     tables, player records       (collection manutd_knowledge)
 odds, leagues, squads
```

**Agent tools (15):** `resolve_time`, `get_fixtures`, `get_fixtures_range`, `get_events`,
`get_lineups`, `get_match_stats`, `get_standings`, `get_full_table`, `find_player`,
`get_top_scorers`, `get_top_assists`, `get_pre_match_odds`, `get_live_odds`, `sync_squad`,
`search_knowledge_base`.

**Endpoints**

| Group | Routes |
|---|---|
| Agent (needs `X-Api-Key`) | `POST /ask`, `POST /ask/stream` |
| Debug | `GET /tools/time`, `/tools/fixtures`, `/tools/fixtures/today`, `/tools/events/{id}`, `/tools/lineups/{id}`, `/tools/statistics/{id}`, `/tools/standings`, `/tools/table`, `/tools/players/search`, `/tools/players/topscorers` |
| Admin | `POST /admin/players/sync`, `GET /admin/players`, `POST /admin/sync/{all,countries,leagues,seasons,teams,players}` |
| Ingestion | `POST /admin/ingest/files` (re-ingest the corpus), `POST /admin/ingest/text` |

**Database.** All entities use our own surrogate `Long id`; the provider's id is stored as a
separate `externalId` with a unique constraint, so swapping data providers needs no schema
change. Tables: `countries`, `leagues`, `seasons`, `teams`, `players`, `player_statistics`
(unique on player + season + league), `fixture_cache` (JSON columns, per-field cache
timestamps). `rating` stays `VARCHAR` because the API returns strings like `"6.58"`.

**Config profiles.** Base has no secrets and no environment defaults; `dev` adds local
host/port defaults and debug logging; `staging`/`prod` use `ddl-auto: validate` and require
every secret from the environment.

---

## 3. Design decisions and the reasoning behind them

- **LLM-first routing.** The model chooses tools; backend code does not pattern-match intent.
  Backend code executes tools deterministically and validates outputs.
- **Facts come from tools, never from model memory.** The system prompt forbids guessing; the
  response schema carries `sources` and `confidence` so ungrounded answers are visible.
- **`toolTrace` is server-side ground truth.** It records what actually executed, not what the
  model claims. This is what lets evaluation be deterministic (no LLM-as-judge).
- **Normalize at the boundary.** API-Football DTOs are converted to domain models immediately.
- **SSE, not WebSocket, for chat.** The conversation is turn-based; only server-to-client flow
  is needed. SSE is stateless, proxy-friendly and horizontally scalable. `/ask` (structured
  JSON) stays for programmatic and eval use; `/ask/stream` uses a plain-text prompt because
  streaming a JSON object token by token is useless to a UI. Tool calls finish before the
  first token streams.
- **Injuries tool removed.** Topical injury information is better served by news in the
  knowledge base than by the API's injuries endpoint.
- **Metrics via Micrometer, not the raw OpenTelemetry SDK**, because Spring AI already
  instruments through Micrometer. (Designed, not yet built.)
- **No dev secrets in the repo.** Local passwords and the guest key come from `.env`
  (see `.env.example`).

---

## 4. API-Football notes

Endpoints used: `/fixtures` (date and range), `/fixtures/events`, `/fixtures/lineups`,
`/fixtures/statistics`, `/standings`, `/players`, `/players/squads`, `/players/topscorers`,
`/players/topassists`, `/odds`, `/odds/live`, `/leagues`. Reference: the official
API-Football v3 documentation (the raw dump that used to live in `APIFootball.MD` was removed).

Gotchas found during the audit, all fixed:

- `/players` returns a `statistics[]` array **per league**. Reading `[0]` returned the wrong
  competition. The client now passes `league=` so the server returns one entry.
- Lineup `number`, `pos` and `grid` live on the **inner** `player` object, not the wrapper.
- Standings carry a `group` (e.g. "Group A") and a `description` (e.g. promotion/relegation
  zone); both are now exposed.
- Odds may be outside the subscription; the client degrades gracefully instead of failing.

---

## 5. Knowledge corpus: format and rules

The curated corpus is flat markdown in `src/main/resources/knowledge/`, categorized by filename
prefix:

| Prefix | Content | Volatility |
|---|---|---|
| `era-` | Club history by period | stable |
| `moment-` | Iconic matches and events | stable |
| `legend-` | Player and manager biographies | stable |
| `club-` | Stadium, ownership, records, academy | mostly stable |
| `season-` | Single-season reviews | stable once the season ends |
| `topical-` | Rolling news layer: transfers, squad, manager | **volatile** |

Every file begins with a header that is parsed into vector-store metadata:

```
TOPIC: <specific, entity-rich title>
CATEGORY: era | moment | legend | club | season | topical
AS OF: YYYY-MM[-DD]   STATUS: stable-history | needs-update | volatile
```

Writing rules (chunking is 800 tokens, **no overlap** — the splitter has no overlap setting,
so standalone paragraphs are what protect chunk boundaries):

1. Every paragraph stands alone; a chunk may be retrieved without its neighbours.
2. Full names every time — never "he" or "the club" across a paragraph boundary.
3. Absolute dates ("26 May 1999"), never relative ones.
4. One topic per file, under about 750 tokens, so a file is one chunk.
5. Facts only, densely. Overlap between files is fine; **contradiction is the only sin.**
6. Mark uncertainty with `STATUS: needs-update` and an inline `[VERIFY]`.

Chunking is **per layer, not global**: curated documents are split at 800 tokens; raw news
items (planned) are never split — one item is one embedding, capped near 400 tokens, and items
are never batched into one document.

**Why `AS OF` cannot drive recency by itself.** It records when a document was last
*verified*, not when its events happened. All seed files are dated 2026-07, including the 1958
Munich document. Freshness ranking therefore keys off *volatility* (`status`/`category`),
never raw age.

---

## 6. Ingestion as it works today

1. `StaticFileLoaderService` reads `classpath:knowledge/*.md` (and legacy `*.txt`).
2. `KnowledgeHeaderParser` turns the header into metadata: `source`, `topic`, `category`,
   `contentType`, `status`, `asOf`, `asOfEpoch` (numeric, for range filters).
3. `DocumentIngestionService.ingestReplacing` deletes existing points whose `source` matches,
   then chunks (800 tokens) and embeds (`text-embedding-3-small`) into Qdrant. Re-running
   replaces rather than duplicates.

History worth remembering: the original loader only matched `*.txt` while the corpus is `.md`,
so ingestion silently processed zero files (the Qdrant collection was never even created); re-running ingestion also used to
duplicate every point. Both were fixed in PR #1.

---

## 7. Current status, known bugs and improvements

### 7.1 What works today

- **Live match Q&A** through `/ask` (structured JSON with `toolTrace`) and `/ask/stream` (SSE):
  fixtures by date or range, events, lineups, match statistics, standings and full table,
  player stats by name, top scorers/assists, pre-match and live odds, natural-language time
  resolution ("last week", "next Saturday", "this season").
- **Multi-turn memory** per `sessionId` (in-memory).
- **Fixture cache** in PostgreSQL, used by the fixtures, events and lineups tools.
- **Master-data sync** (`/admin/sync/*`) — idempotent and ordered.
- **Knowledge corpus ingestion** via `POST /admin/ingest/files` — header metadata, 800-token
  chunks, replace-not-duplicate. Retrieval is available to the agent only when
  `manu4u.knowledge-base-enabled=true` (default `false`).
- **API key gate** on `/ask` and `/ask/stream`.
- **Eval harness** (`mvn test -Peval`, 105 cases, deterministic graders) against a running instance.

### 7.2 Verified bugs

Each of these was confirmed against the code, not assumed.

| # | Bug | Where | Impact |
|---|---|---|---|
| 1 | `/ask/stream` generates a `sessionId` when none is sent but never returns it, so the client cannot continue the conversation. | `AgentController.askStream`, `AgentOrchestrator.streamQuestion` | Streaming follow-ups lose context unless the client invents its own id. |
| 2 | The streaming system prompt's "Tool Usage Logic" section is just "Same as always…". The detailed rules (player-stats flow, `sync_squad` auto-retry, standings ids) exist only in the JSON prompt. | `AgentOrchestrator` (`STREAMING_SYSTEM_PROMPT`) | `/ask/stream` can behave differently from, and worse than, `/ask`. |
| 3 | `POST /admin/ingest/text` uses additive `ingest`, not `ingestReplacing`, and does not parse a header. | `IngestionController.ingestText` | Calling it twice duplicates points; ad-hoc text lacks `status`/`asOfEpoch`, so it is invisible to freshness logic. |
| 4 | The corpus is **not** ingested at startup (no startup hook exists), although earlier notes implied it. | — | A fresh environment answers history questions with nothing until `POST /admin/ingest/files` is run by hand. |
| 5 | Squad sync returns `-1` on failure and `AdminController` still replies 200 with `"synced": -1`. | `MasterDataSyncService.syncPlayers`, `AdminController` | Failures look like successes to callers. |
| 6 | `test-agent.http` sends no `X-Api-Key`, so every `/ask` request in it returns 401. | `src/test/resources/test-agent.http` | Misleading sample requests. |
| 7 | 8 eval cases require a removed `get_injuries` tool. | `src/test/resources/eval/eval-dataset.json` | Those cases fail by construction and drag the score down. |
| 8 | The guest key is compared with `String.equals`, which is not constant-time. | `GuestApiKeyFilter` | Minor timing side channel; use `MessageDigest.isEqual`. |
| 9 | `ConversationHistoryService` mutates plain `ArrayList`s from concurrent requests and never expires sessions. | `ConversationHistoryService` | Races under concurrent use of one session; unbounded memory growth; unusable beyond one instance. |
| 10 | `GlobalExceptionHandler` has no handlers for malformed JSON, type-mismatched or missing params, or `@Valid` failures. | `GlobalExceptionHandler` | Client errors surface as generic 500s. |
| 11 | `/admin/**`, `/tools/**`, `/admin/ingest/**` are open (`anyRequest().permitAll()`), and CORS allows every origin with credentials. | `SecurityConfig` | **Do not expose these publicly.** Fine for local development only. |

### 7.3 Improvements worth making

- Return the `sessionId` on the stream (response header or a first SSE event) and share one
  prompt body between `/ask` and `/ask/stream`, differing only in output format.
- Make admin and debug routes require a separate admin credential; restrict CORS to the real
  frontend origin.
- Add a fixture-cache retention job (old completed fixtures) and fix the N+1 in
  `FixtureCacheService.saveFixtures`.
- Player stats are single-league; add "all competitions" aggregation.
- Delete the unused `client/dto/claude/` classes (nothing references them).
- Add unit tests beyond `Manu4uApplicationTests` and `AgentOrchestratorTest` — tools, `TimeTool`
  expression parsing and the sync service have none.
- Observability (Spring AI's Micrometer instrumentation, a few custom metrics such as API
  quota usage and cache hit rate) was designed but not built.

Ingestion and retrieval items (3, 4, 7 and the retrieval work) are tracked in
[ROADMAP.md](ROADMAP.md).
