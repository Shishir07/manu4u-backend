# ManU4U — Roadmap

**Goal:** a complete ingestion pipeline and a RAG layer whose answers are *correct* — above
all for topical news (where the freshest fact wins) and history (where the right document wins
and stale data must never leak in).

Scope is deliberately limited to ingestion, retrieval correctness, and the data-access layer that
feeds the agent (§10). Parked items outside that
scope are listed in [HISTORY.md §7](HISTORY.md#7-current-status-known-bugs-and-improvements).

## Where things stand

| Area | State |
|---|---|
| Curated corpus (18 files), header → metadata parser, idempotent re-ingest, 800-token chunking | **Done** (merged, PR #1) |
| Freshness-aware re-ranking (over-fetch, volatility-based bonus, archive demotion, prompt rules) | **In review** (PR #2, unit-tested; live check pending) |
| Raw news ingestion | Not started |
| Archival of aging news | Not started |
| Retrieval evaluation | Eval harness exists (105 cases) but covers live-API tools only; nothing tests the knowledge base |

## Order of work

Each item is its own branch and PR. Nothing merges to `main` except through a reviewed PR.

### 0. Fix the known ingestion bugs (small, do first)
From [HISTORY.md §7.2](HISTORY.md#72-verified-bugs):

- `POST /admin/ingest/text` must use `ingestReplacing` and parse the same header as files,
  so ad-hoc text is idempotent and carries `status`/`asOfEpoch` (bug 3).
- Ingest the corpus automatically on first start when the collection is empty, behind a flag,
  so a fresh environment is not silently blank (bug 4).
- Re-point the 8 obsolete `injuries` eval cases (bug 7) — see §5.

### 1. Land and verify freshness-aware retrieval
Merge PR #2 after a live run: ingest the corpus, confirm the point count is stable across two
runs, confirm `status`/`asOfEpoch` are present on stored points, and confirm a "this summer's
signings" question cites the `AS OF` date while a 1999 question does not hedge.

- **Also:** `manu4u.knowledge-base-enabled` defaults to `false`, so the agent never sees the
  knowledge-base tool out of the box. Decide the default per profile and make it explicit in
  the README.

### 2. News ingester (two decoupled steps)
**2a. Fetch — RSS → local folder.** Configurable feed list. For each entry, derive a stable id
from the GUID/link, skip if already on disk, and write `{id}.md` plus metadata atomically
(write `.tmp`, then rename) so a half-written file is never visible. Output uses the same
header format as the curated corpus (`CATEGORY: news`, `STATUS: current`, publication date as
`AS OF`). Cap each item near 400 tokens at fetch time.

**2b. Sync — folder → Qdrant.** A Postgres ledger (`filename`, `content_hash`, `source`,
`published_at`, `chunk_count`, `status`, `ingested_at`) decides what to do per file:
unchanged → skip; changed → delete old points then re-ingest; new → ingest. News items use
`SplitStrategy.NONE` (one item, one embedding — never split, never batched).

Acceptance: running fetch twice creates no new files; running sync twice creates no new
points; killing the process mid-write leaves no partial file ingested.

### 3. Archival instead of expiry
Football facts are superseded, not falsified, so aging news is **archived, not deleted.**
A scheduled job flips `status: current → archived` past a configurable age, in the ledger and
in the stored point metadata, while each item **stays an individual embedding** (collating
into one vector would blur unrelated items together). Archived items remain retrievable for
explicitly historical queries and rank below current ones (the demotion already exists in PR #2).
Collated archive files are written to storage for reference only.

Acceptance: an archived transfer rumour never outranks a current item for a "latest" question
but is still returned for "who did we try to sign last summer?".

### 4. Retrieval correctness
Targeted improvements, each justified by a failing eval case before it is built:

- **Date/status filters on the tool** — let a "latest/this week" question restrict to a recent
  publication window using the numeric `asOfEpoch`, rather than relying on ranking alone.
- **Supersession handling** — when two items disagree (a rumour vs a completed deal), prefer
  the newer, and surface the conflict rather than silently picking one.
- **Minimum-similarity cutoff** — return nothing instead of a weak match, so the agent can
  say "the knowledge base doesn't cover that" rather than grounding an answer on noise.
- **Citation fields** — keep `source`, `topic`, `asOf` and `status` on every returned snippet
  and require the answer to state the date for volatile facts.
- **Separate news and history paths** if evaluation shows a single tool confuses selection.

### 5. Retrieval evaluation (the correctness gate)
- **Fix the obsolete cases.** 8 of the 105 existing cases (`injuries`) require a
  `get_injuries` tool that was removed; re-point them at news retrieval.
- **Add knowledge-base categories:** history lookups, topical/news lookups, freshness
  (a superseded rumour must not win), and *unanswerable* questions (the agent must decline).
- **Add LLM-free retrieval metrics:** for a labelled query set, measure hit@k and MRR directly
  against the vector store, so retrieval regressions are visible without paying for or
  trusting model output. Proposed starting target: hit@3 ≥ 0.9 on history queries.
- **Gate changes on it:** retrieval or corpus changes must not lower the score.

### 6. Corpus completeness and quality
- Add the remaining planned history documents: further legends (Law, Giggs, Scholes, Beckham,
  Schmeichel, Rooney, Ronaldo, van Nistelrooy, Ferdinand, Vidic, Busby, Ferguson), season
  reviews for 2020-21 through 2025-26, rivalries, club records, academy, crest and kit history.
- **Corpus lint** (build-time check): header present and well-formed, `AS OF` parses, file is
  under the token budget, dates are absolute, no duplicate `source` ids.
- Flag documents marked `needs-update` whose subject has moved on.

### 7. Durable storage
Put raw news files and archives behind a small `KnowledgeSource` interface — local folder
now, S3 or Azure Blob later — so the same ingestion logic runs against either. Keeps a
replayable record of everything ever ingested.

### 8. Scheduling and ingestion observability
Run fetch/sync/archive on a schedule (manual triggers first, scheduling once output is
trusted). Log and expose per-run counts: fetched, skipped, ingested, replaced, archived,
failed. Alert if a run fails or a feed returns nothing for an unusual period.

### 9. Gated refresh of topical documents (last)
Periodically fold new news into the curated `topical-` documents using a fixed curation
prompt and the corpus writing rules, replacing the document under the same `source` id.
Publishing is **gated on the retrieval evaluation in §5**: if the score drops, keep the
previous document and log the diff for review. Only worth building once §2–§5 are in place.

### 10. MCP integration for API-Football (retire the hand-written tools)
Today every API-Football endpoint is wrapped by hand: a client method, DTOs, a normalizing
tool class, and an `@Tool` wrapper (`ApiFootballClient`, `*Tool.java`, `LiveMatchToolsService`).
Goal: let the agent reach API-Football through the **Model Context Protocol** as an MCP
client, so new endpoints need no new Java and most of that code can be deleted.

- **Verify first:** confirm an API-Football MCP server exists and is maintained, or decide to
  run a thin one ourselves over the REST API. Do not assume; this decides the whole approach.
- **Spring AI MCP client:** register the server and expose its tools to the `ChatClient`
  alongside the remaining local tools.
- **Keep what MCP does not give us**, and decide each explicitly before deleting code:
  - the PostgreSQL fixture cache and API quota protection (MCP calls must still go through
    the cache layer or an equivalent, or every question spends quota);
  - normalized, size-bounded results (raw API payloads are large and hurt answer quality
    and token cost);
  - the fixes from the API audit (`league=` filtering on player stats, inner lineup fields,
    standings groups) — the MCP path must reproduce them or the bugs return;
  - `toolTrace`, so the eval harness keeps working (traces must record MCP tool names and
    arguments).
- **Migrate incrementally, behind a flag:** one endpoint family at a time (standings first,
  lowest risk), run the eval suite on both paths, and delete the hand-written tool only when
  scores match.
- **Local tools that stay:** `resolve_time`, `find_player` (our DB lookup), `sync_squad` and
  `search_knowledge_base` are not API-Football wrappers.

Acceptance: the eval suite scores the same or better on the MCP path, per-question API calls
do not increase, and the list of hand-written API-Football wrappers shrinks.

## Definition of done for this roadmap

- Fetch, sync and archive are idempotent and safe to re-run.
- A "latest" question returns the newest relevant fact with its date; a history question
  returns the right document with no spurious date caveat.
- The retrieval evaluation runs in CI and blocks regressions.
- Every ingested item can be traced to its source, date and status.
- API-Football data reaches the agent through MCP with no loss of eval score, cache protection or traceability.
