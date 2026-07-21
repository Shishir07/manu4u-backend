# Knowledge Corpus — Structure & Writing Conventions
> This guide lives OUTSIDE `src/main/resources/knowledge/` on purpose: everything inside
> that folder is ingested into Qdrant at startup by `StaticFileLoaderService`.

## File layout (flat, prefix-categorized)
Files are FLAT (no subfolders — avoids any loader-recursion assumptions) and categorized
by filename prefix:

| Prefix | Content | Volatility |
|---|---|---|
| `era-`    | Club history by period (chronological spine) | stable |
| `moment-` | Deep dives on iconic matches/events | stable |
| `legend-` | Player & manager biographies | stable |
| `club-`   | Institution topics: stadium, ownership, crest, records, academy | mostly stable |
| `season-` | Single-season reviews (recent seasons in detail) | stable once season ends |
| `topical-`| Rolling news layer: transfers, manager news, current squad | VOLATILE — has `AS OF` date, replaced on re-ingest |

The `topical-` layer is fed by the external story ingester, NOT hand-written here.

## Document format
Every file starts with a plain-text header block (ingested with the text — it aids retrieval):

```
TOPIC: <specific, entity-rich title>
CATEGORY: era | moment | legend | club | season | topical
AS OF: YYYY-MM   STATUS: stable-history | needs-update | volatile
```

Then a 2–3 sentence summary paragraph, then sections.

## Chunking (per-layer, not global)
- **Curated files (this folder): split at 800 tokens, NO overlap.** Spring AI's
  `TokenTextSplitter` has no overlap parameter — rule 1 below (standalone paragraphs)
  is what protects chunk boundaries, not overlap.
- **Target one topic under ~750 tokens (~550 words)** so a file fits in ONE chunk —
  chunk == document is the ideal retrieval unit for a one-topic-per-file corpus.
  13 of the 18 seed files already hit this.
- **Raw news items are NEVER split** (`SplitStrategy.NONE`): one item = one embedding,
  capped at ~400 tokens at fetch time. Never batch multiple items into one document —
  a grab-bag chunk of unrelated items embeds to semantic mush.

## Writing rules (write for the chunking above)
1. **Every paragraph must stand alone.** A chunk may be retrieved without its neighbours,
   so no paragraph should depend on the previous one to be understood.
2. **Full names, always.** Never "he/the club/the manager" across paragraph boundaries —
   write "George Best", "Manchester United", "Sir Alex Ferguson". Repetition is a feature.
3. **Absolute dates.** "26 May 1999", never "that year" or "two seasons later".
4. **One topic per file**, ~300–600 words. Overlapping topics across files is fine
   (the treble appears in both `era-1986-1999` and `moment-treble-1999`) — retrieval
   benefits from redundancy; contradiction is the only sin.
5. **Facts only, densely.** No flowery filler; scores, dates, fees, names. The agent adds
   the storytelling voice — the corpus supplies verifiable substance.
6. **Mark uncertainty.** Anything that may have changed carries `STATUS: needs-update`
   and a `[VERIFY]` tag inline. Grounding is the product's brand — a wrong corpus fact
   is worse than a missing one.

## Known gaps in this first batch (2026-07-14)
- Claude's knowledge is reliable through **January 2026**. The end of the **2025-26 season**
  (Feb–May 2026), **WC2026**, and the **2026 summer window** must come from the story
  ingester or Shishir's review — files touching them are marked `needs-update`.
- Missing (next batches): more legends (Law, Giggs, Scholes, Beckham, Schmeichel, Rooney,
  Ronaldo, van Nistelrooy, Ferdinand/Vidic, Busby & Ferguson as standalone manager bios),
  `season-` files for 2020-21 → 2025-26, rivalries (Liverpool, City, Leeds, Arsenal),
  `club-records`, `club-academy` (Class of '92, Busby Babes youth system), crest/kit history.

## Re-ingestion & dedupe (DONE — feature/knowledge-ingest)
Ingestion is idempotent: each file's header (`TOPIC / CATEGORY / AS OF / STATUS`) is
parsed into Qdrant metadata (`source`, `topic`, `category`, `contentType`, `status`,
`asOf`, `asOfEpoch`), and existing points matching `source` are deleted before re-adding.
Re-running `POST /admin/ingest/files` replaces rather than duplicates.
