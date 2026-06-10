# ManU4U — Project Summary
> Last updated: 2026-04-29

---

## 1. What It Is

ManU4U is a conversational AI agent for Manchester United fans. A user asks a natural-language question (`POST /ask`) and the agent — powered by Spring AI + OpenAI gpt-4o — selects and calls the right tools to answer it from live data (API-Football), a PostgreSQL master-data store, and a Qdrant RAG knowledge base.

---

## 2. Technology Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 21 |
| Framework | Spring Boot | 3.4.5 |
| AI / Agent | Spring AI | 1.0.0 |
| LLM | OpenAI gpt-4o | via Spring AI |
| Vector Store | Qdrant | via Spring AI |
| Database | PostgreSQL | via Spring Data JPA |
| API Client | Spring WebFlux (WebClient) | — |
| API Docs | SpringDoc OpenAPI | 2.8.3 |
| Boilerplate | Lombok | — |
| Local infra | Docker Compose | Qdrant + PostgreSQL |

---

## 3. Directory Structure

```
manu4u/
├── PRODUCTION_PLAN.md          # Hosting + prod readiness roadmap
├── Future.MD                   # Deferred work items
├── src/main/java/com/manu4u/tools/
│   ├── Manu4uApplication.java
│   ├── agent/
│   │   ├── AgentOrchestrator.java          # Spring AI ChatClient loop + toolTrace
│   │   ├── ConversationHistoryService.java  # In-memory session store (Map<sessionId, …>)
│   │   └── ToolCallTracker.java            # Captures @Tool call sequences per request
│   ├── client/
│   │   ├── ApiFootballClient.java          # All API-Football HTTP calls (WebClient)
│   │   └── dto/
│   │       ├── ApiFootballResponse.java    # Generic response wrapper + error check
│   │       ├── EventDto.java
│   │       ├── FixtureDto.java
│   │       ├── LeagueDto.java
│   │       ├── LineupDto.java
│   │       ├── OddsDto.java
│   │       ├── PlayerDto.java
│   │       ├── SquadDto.java
│   │       ├── StandingDto.java
│   │       ├── StatisticsDto.java
│   │       └── claude/                     # Unused legacy Claude direct API DTOs
│   ├── config/
│   │   ├── ApiFootballProperties.java      # baseUrl, apiKey
│   │   ├── ChatClientConfig.java           # Spring AI ChatClient bean
│   │   ├── Manu4uProperties.java           # timezone, defaultTeamId, defaultLeagueId, providerName
│   │   ├── OpenApiConfig.java
│   │   ├── RagConfig.java                  # VectorStore + TextSplitter beans
│   │   └── WebClientConfig.java
│   ├── controller/
│   │   ├── AgentController.java            # POST /ask
│   │   ├── AdminController.java            # POST /admin/players/sync, GET /admin/players
│   │   ├── GlobalExceptionHandler.java
│   │   ├── IngestionController.java        # POST /ingest/text, POST /ingest/files
│   │   ├── SyncController.java             # POST /admin/sync/{all,countries,leagues,seasons,teams,players}
│   │   └── ToolsController.java            # GET /tools/* — debug/test endpoints
│   ├── entity/                             # JPA entities (PostgreSQL)
│   │   ├── Country.java                    # id (Long PK), externalCode, name
│   │   ├── FixtureRecord.java              # Fixture cache (TTL-aware, JSON columns)
│   │   ├── League.java                     # id, externalId, name, type, countryId, currentSeason
│   │   ├── Player.java                     # id, externalId, name, position, jerseyNumber, teamId
│   │   ├── PlayerStatistics.java           # id, playerId, seasonId, leagueId, goals, assists, …
│   │   ├── Season.java                     # id, startYear, endYear, label, current
│   │   └── Team.java                       # id, externalId=33, name="Manchester United"
│   ├── model/                              # Domain models returned by tools
│   │   ├── FixtureSummary.java
│   │   ├── Lineup.java                     # teamId, teamName, formation, starters[], substitutes[]
│   │   ├── MatchEvent.java
│   │   ├── MatchOdds.java
│   │   ├── PlayerSeasonStats.java
│   │   ├── ResolvedDate.java               # Legacy single-date model
│   │   ├── ResolvedTimeRange.java          # from, to, isRange, description
│   │   ├── StandingEntry.java              # rank, points, group, description, form, …
│   │   └── agent/
│   │       ├── AgentRequest.java           # { question, sessionId }
│   │       └── AgentResponse.java          # { answer, confidence, sessionId, toolTrace[] }
│   ├── repository/                         # Spring Data JPA repositories
│   │   ├── CountryRepository.java
│   │   ├── FixtureRecordRepository.java
│   │   ├── LeagueRepository.java
│   │   ├── PlayerRepository.java           # findByExternalIdIn, findByNameContainingIgnoreCase
│   │   ├── PlayerStatisticsRepository.java
│   │   ├── SeasonRepository.java
│   │   └── TeamRepository.java
│   └── service/
│       ├── EventsTool.java                 # /fixtures/events
│       ├── FixtureCacheService.java        # TTL-aware PostgreSQL fixture cache
│       ├── FixturesTool.java               # /fixtures by date + range
│       ├── KnowledgeBaseTool.java          # RAG search (Qdrant)
│       ├── LineupsTool.java                # /fixtures/lineups
│       ├── OddsTool.java                   # /odds + /odds/live (graceful 402/403)
│       ├── PlayerStatsTool.java            # DB name lookup → /players stats, top scorers/assists
│       ├── StandingsTool.java              # /standings
│       ├── StatisticsTool.java             # /fixtures/statistics
│       ├── TimeTool.java                   # Natural language time expression resolver
│       ├── ingestion/
│       │   ├── DocumentIngestionService.java   # Chunks + embeds text into Qdrant
│       │   └── StaticFileLoaderService.java    # Loads knowledge/ files at startup
│       ├── sync/
│       │   └── MasterDataSyncService.java  # countries→leagues→seasons→teams→players (idempotent)
│       └── tools/
│           ├── KnowledgeBaseToolService.java   # @Tool: search_knowledge_base
│           └── LiveMatchToolsService.java      # @Tool: all 15 agent-callable tools
└── src/main/resources/
    ├── application.yml             # Base config (no secrets, no env-specific defaults)
    ├── application-dev.yml         # Local defaults, DEBUG logging
    ├── application-staging.yml     # ddl-auto: validate, INFO logging
    ├── application-prod.yml        # ddl-auto: validate, WARN logging, larger Hikari pool
    └── knowledge/                  # Static RAG documents (loaded at startup)
```

---

## 4. Agent Architecture

```
POST /ask  { question, sessionId }
     │
     ▼
AgentController
     │
     ▼
AgentOrchestrator
  ├── ConversationHistoryService  ← session memory (in-memory Map)
  ├── ToolCallTracker             ← captures tool name + args per request
  └── Spring AI ChatClient
        ├── System prompt  (Man Utd domain + tool usage rules)
        ├── Conversation history (multi-turn context)
        └── @Tool methods (via LiveMatchToolsService + KnowledgeBaseToolService)
              │
              ▼ (LLM selects tools, Spring AI invokes them)
         [resolve_time] → TimeTool
         [get_fixtures] → FixturesTool → FixtureCacheService → ApiFootballClient
         [get_fixtures_range] → FixturesTool
         [get_events] → EventsTool → ApiFootballClient
         [get_lineups] → LineupsTool → ApiFootballClient
         [get_match_stats] → StatisticsTool → ApiFootballClient
         [get_standings] → StandingsTool → ApiFootballClient
         [find_player] → PlayerStatsTool (DB lookup) → ApiFootballClient
         [get_top_scorers] → PlayerStatsTool → ApiFootballClient
         [get_top_assists] → PlayerStatsTool → ApiFootballClient
         [get_pre_match_odds] → OddsTool → ApiFootballClient
         [get_live_odds] → OddsTool → ApiFootballClient
         [sync_squad] → PlayerStatsTool → MasterDataSyncService
         [search_knowledge_base] → KnowledgeBaseTool → Qdrant VectorStore
              │
              ▼
AgentResponse { answer, confidence, sessionId, toolTrace[] }
```

**toolTrace** — every response includes an array of `ToolCallRecord { name, arguments, sequence }` showing exactly which tools the agent called and in what order. Used by the eval engine.

---

## 5. All API Endpoints

### Agent
| Method | Path | Description |
|---|---|---|
| POST | `/ask` | Main conversational entry point |

### Debug / Test
| Method | Path | Description |
|---|---|---|
| GET | `/tools/time?expr=&timezone=` | Test time expression resolution |
| GET | `/tools/fixtures?teamId=&date=` | Test fixture lookup |
| GET | `/tools/fixtures/today` | Today's fixtures for default team |
| GET | `/tools/events/{fixtureId}` | Raw events for a fixture |
| GET | `/tools/lineups/{fixtureId}` | Raw lineups for a fixture |
| GET | `/tools/statistics/{fixtureId}` | Raw statistics for a fixture |

### Admin — Squad
| Method | Path | Description |
|---|---|---|
| POST | `/admin/players/sync?season=` | Sync Man United squad from API-Football |
| GET | `/admin/players` | List all synced players |

### Admin — Master Data Sync (all idempotent)
| Method | Path | Description |
|---|---|---|
| POST | `/admin/sync/all` | Run full sync: countries→leagues→seasons→teams→players |
| POST | `/admin/sync/countries` | Seed England + World |
| POST | `/admin/sync/leagues` | Sync all England + World leagues |
| POST | `/admin/sync/seasons` | Seed last 5 seasons |
| POST | `/admin/sync/teams` | Seed Manchester United |
| POST | `/admin/sync/players?season=` | Sync Man United squad |

### RAG Ingestion
| Method | Path | Description |
|---|---|---|
| POST | `/ingest/text` | Ingest text content into Qdrant |
| POST | `/ingest/files` | Ingest files from knowledge/ directory |

### Docs
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

---

## 6. Database Schema (PostgreSQL)

```
countries      id(PK), external_code(UQ), name, flag_url
leagues        id(PK), external_id(UQ), name, type, country_id(FK), current_season, logo_url
seasons        id(PK), start_year(UQ), end_year, label, current
teams          id(PK), external_id(UQ)=33, name, code, country_id(FK), founded, venue_name
players        id(PK), external_id(UQ), name, position, jersey_number, age, team_id(FK), synced_at
               indexes: name, external_id, team_id
player_statistics  id(PK), player_id(FK), season_id(FK), league_id(FK), team_id(FK),
                   appearances, goals, assists, yellow_cards, red_cards, rating(VARCHAR),
                   shots_total, shots_on_target, passes_key, synced_at
                   UQ: (player_id, season_id, league_id)
fixture_cache  fixture_id(PK), match_date, season, home_team, away_team, status, utc_kickoff,
               events_json, lineups_json, statistics_json,
               cached_at, events_cached_at, lineups_cached_at
```

**Design principle:** All entities use our own surrogate `Long id` as PK. API-Football IDs are stored as a separate `externalId` field with a unique constraint. This keeps the schema provider-agnostic.

---

## 7. Configuration Profiles

| Profile | `ddl-auto` | Logging | Notes |
|---|---|---|---|
| (base) | — | INFO | No secrets, no env fallbacks |
| dev | update | DEBUG (Spring AI) | Local DB/Qdrant defaults |
| staging | validate | INFO | Env vars required for all secrets |
| prod | validate | WARN | Larger Hikari pool (20/50) |

**All secrets come from environment variables — never hardcoded:**
`SPRING_AI_OPENAI_API_KEY`, `API_FOOTBALL_KEY`, `SPRING_DATASOURCE_*`, `QDRANT_HOST`, `QDRANT_API_KEY`

---

## 8. RAG Pipeline

**Ingestion (built):**
```
Text / Files → DocumentIngestionService
                  ├── TokenTextSplitter (chunk_size=800, overlap=150)
                  ├── OpenAI text-embedding-ada-002 (embeddings)
                  └── Qdrant VectorStore (collection: manutd_knowledge)
```

**Retrieval (wired via @Tool):**
```
Agent → search_knowledge_base(query) → KnowledgeBaseTool
          → VectorStore.similaritySearch(query, topK=5, threshold=0.7)
          → Returns List<Document> as context to LLM
```

Static documents in `src/main/resources/knowledge/` are auto-loaded at startup by `StaticFileLoaderService`.

---

## 9. Development Status

### ✅ Completed

| Phase | What was built |
|---|---|
| Tool wrappers | ApiFootballClient, 5 core tools, REST test endpoints, error handling, Swagger |
| Agent loop | Spring AI ChatClient, 14 @Tool methods, `/ask` endpoint, multi-turn session memory, toolTrace |
| Expanded toolset | Standings, PlayerStats (with DB name lookup), Odds, fixture range queries |
| RAG ingestion | Qdrant VectorStore, DocumentIngestionService, KnowledgeBaseTool, static file loader |
| Prod readiness | Config profiles (dev/staging/prod), @Transactional, N+1 fixes, @Valid + @NotBlank, typed ResponseEntity |
| API correctness | Lineups fix (inner PlayerInfo), standings group/description fields, leagueId server-side filter for player stats |
| Master data | 7 entities + 7 repositories, MasterDataSyncService (idempotent), SyncController, player name resolution |
| Code hygiene | Deleted InjuriesTool/InjuryDto/InjuryReport, replaced PlayerRecord with Player entity |

### 🔧 In Progress

| Item | Status |
|---|---|
| Fixture cache | `FixtureCacheService` + `FixtureRecord` entity exist; TTL integration into FixturesTool/EventsTool/LineupsTool pending |
| RAG retrieval | `search_knowledge_base` tool wired; knowledge base content population and quality testing pending |

### 📋 Planned (see `Future.MD` + `PRODUCTION_PLAN.md`)

| Item | Where tracked |
|---|---|
| Observability (Actuator + Prometheus + Grafana + Tempo) | PRODUCTION_PLAN.md §4 |
| 6 custom Micrometer metrics | PRODUCTION_PLAN.md §4.3 |
| Eval engine (`mvn test -Peval`, 25 test cases, 7 graders) | Eval plan in plan file |
| Flyway migrations | PRODUCTION_PLAN.md §3.1 |
| Admin endpoint security (Spring Security) | PRODUCTION_PLAN.md §3.3 |
| Rate limiting on `/ask` | PRODUCTION_PLAN.md §3.4 |
| Session store externalisation (Redis/PG) for multi-instance | Future.MD |
| player_statistics sync into DB | Future.MD |
| Fixture cache TTL purge job | Future.MD |
| MCP integration | separate window |

---

## 10. Key Design Decisions

- **LLM-first tool selection** — the agent decides which tools to call; no hardcoded routing logic
- **toolTrace in every response** — every `/ask` response carries a full record of which tools fired and in what order, enabling deterministic eval without LLM-as-judge
- **Provider-agnostic schema** — all entities use our own surrogate `Long id` PK; API-Football IDs are `externalId` fields. Switching data providers requires no schema changes
- **Server-side statistics filtering** — `?league=X` passed to API-Football so the returned `statistics[]` array is always a single entry for the requested league; no client-side filtering needed
- **Fixture cache only for completed matches** (FT/AET/PEN) by default; live/upcoming always fetch fresh. TTL is status-aware
- **Injuries removed** — topical injury info to come from RAG (news articles), not the API-Football injuries endpoint
- **Micrometer for custom metrics** (not raw OTel SDK) — Spring AI already bridges Micrometer → OTel; adding raw OTel SDK would be dual-SDK overhead for no benefit

---

## 11. Running Locally

```bash
# Start infrastructure
docker-compose up -d          # PostgreSQL + Qdrant

# Run the app (dev profile)
SPRING_PROFILES_ACTIVE=dev \
API_FOOTBALL_KEY=<key> \
SPRING_AI_OPENAI_API_KEY=<key> \
mvn spring-boot:run

# First-time data setup
curl -X POST localhost:8080/admin/sync/all   # seed all reference tables + squad

# Ask a question
curl -X POST localhost:8080/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "When is United's next match?", "sessionId": "dev-1"}'
```
