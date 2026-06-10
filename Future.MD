# Future Work

Deferred items that should be addressed before or during production scaling.
Add new items here as they surface during development.

---

## 1. Thread Safety — ConversationHistoryService

**File:** `agent/ConversationHistoryService.java`

**Issue:** The `ArrayList` values inside the `ConcurrentHashMap` are mutated by multiple threads (concurrent requests to the same session) with no synchronization. `ConcurrentHashMap` only protects the map itself, not the lists inside it.

**Fix options:**
- Wrap each list in `Collections.synchronizedList()`
- Replace `ArrayList` with `CopyOnWriteArrayList`
- Use `ReadWriteLock` per session

**Also:** No session TTL — the map grows unbounded. Consider adding:
- Eviction after N minutes of inactivity (e.g. `Caffeine` cache with TTL)
- Maximum session count cap (e.g. 1000 concurrent sessions)
- Redis-backed store for multi-instance deployments

---

## 2. Error Handler Gaps — GlobalExceptionHandler

**File:** `controller/GlobalExceptionHandler.java`

**Missing `@ExceptionHandler` methods for:**
- `MethodArgumentTypeMismatchException` — wrong type for query params (e.g. `?season=abc`)
- `HttpMessageNotReadableException` — malformed JSON request body
- `MissingServletRequestParameterException` — required param not provided
- `MethodArgumentNotValidException` — `@Valid` constraint violations (currently falls through to generic 500)

---

## 3. Section C — Master Data / Reference Tables

Entity resolution tables for disambiguating user queries when no league/season/team is specified.

### Tables to create:
- **`countries`** — seed: England + World
- **`leagues`** — sync all leagues where `country=England OR country=World AND current=true` from `/leagues`
- **`seasons`** — past 5 seasons (start year as PK, e.g. 2020–2024)
- **`teams`** — seed: Man United (id=33), extensible later
- **`player_statistics`** — per-player per-season per-league grain, unique on `(player_id, season, league_id)`.
  Enables:
  - `SELECT SUM(goals) WHERE player_id=? AND season=?` (all competitions)
  - `SELECT goals WHERE player_id=? AND season=? AND league_id=39` (PL only)

### Sync endpoints:
```
POST /admin/sync/countries
POST /admin/sync/leagues
POST /admin/sync/seasons
POST /admin/sync/teams
POST /admin/sync/player-stats?playerId=X&season=Y
POST /admin/sync/all          (runs all above in order)
```

### Agent enhancement:
Add `resolve_entity(type, query)` tool — lets agent ask "which league is the FA Cup?" → returns `{id: 45, name: "FA Cup"}` from local DB. Reduces hallucination and disambiguates multi-competition queries.

---

## 4. Multi-League Aggregation in PlayerStatsTool

**File:** `service/PlayerStatsTool.java`

Currently `toStats()` uses the server-side `league` filter to get a single-entry statistics array.
For future "total goals across all competitions" queries, we need client-side aggregation:

```java
private PlayerSeasonStats aggregateAcrossLeagues(List<PlayerDto.Statistics> stats) {
    // SUM goals, assists, appearances, minutes across all league entries
    // Use first entry for non-summable fields (position, team, rating)
}
```

This enables questions like: "How many goals has Rashford scored in total this season across all comps?"

---

## 5. Fixture Cache — 6-Month Retention Job

**File:** `service/FixtureCacheService.java`, `entity/FixtureRecord.java`

Add a scheduled daily purge job to remove completed fixtures older than 6 months:

```java
@Scheduled(cron = "0 0 3 * * *")   // 3am daily
@Transactional
public void purgeOldFixtures() {
    LocalDate cutoff = LocalDate.now().minusMonths(6);
    int deleted = repo.deleteByMatchDateBeforeAndStatusIn(cutoff, COMPLETED);
    log.info("Purged {} fixture records older than {}", deleted, cutoff);
}
```

Also add composite index on `(match_date, status)` for efficient purge queries.

---

## 6. Fixture Cache — N+1 in saveFixtures

**File:** `service/FixtureCacheService.java` (lines 65–80)

`saveFixtures()` loops over fixtures and does `findById + save` per fixture.
Replace with `findAllById(ids)` + map + `saveAll()` — same pattern as squad sync fix.

---

## 7. Admin Endpoint Security

**Files:** `controller/AdminController.java`, `controller/IngestionController.java`

All `/admin/*` endpoints have no authentication or rate limiting.
Before production exposure, add:
- API key header validation (`X-Admin-Key`) or Spring Security basic auth
- Rate limiting on sync endpoints (they call the external API)
- IP allowlist option

---
