package com.manu4u.tools.service;

import com.manu4u.tools.config.Manu4uProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ranking logic is pure — no Qdrant, no Spring context, no API keys required.
 */
class RecencyRankerTest {

    private final Manu4uProperties properties = new Manu4uProperties();
    private final RecencyRanker ranker = new RecencyRanker(properties);
    private final Instant now = Instant.now();

    private Document doc(String source, String category, String status, Instant asOf, double score) {
        Map<String, Object> md = new HashMap<>();
        md.put("source", source);
        md.put("category", category);
        md.put("status", status);
        if (asOf != null) md.put("asOfEpoch", asOf.getEpochSecond());
        return Document.builder().text("content of " + source).metadata(md).score(score).build();
    }

    private double score(Document d) {
        return ranker.adjustedScore(d, now, properties.getRetrieval());
    }

    @Test
    @DisplayName("stable-history is never recency-adjusted, however old its asOf date")
    void stableHistoryIsTimeless() {
        Document ancient = doc("moment-treble-1999", "moment", "stable-history",
                now.minus(3650, ChronoUnit.DAYS), 0.80);
        assertEquals(0.80, score(ancient), 1e-9,
                "a timeless document must score exactly its similarity — no decay, no bonus");
    }

    @Test
    @DisplayName("era/legend/moment are timeless even without a stable-history status")
    void timelessCategoriesAreNotDecayed() {
        Document era = doc("era-1986-1999", "era", "needs-update",
                now.minus(500, ChronoUnit.DAYS), 0.70);
        assertEquals(0.70, score(era), 1e-9);
    }

    @Test
    @DisplayName("a fresher volatile document outranks a stale one that matched slightly better")
    void freshVolatileBeatsMarginallyBetterStale() {
        Document stale = doc("topical-window-old", "topical", "volatile",
                now.minus(365, ChronoUnit.DAYS), 0.82);
        Document fresh = doc("topical-window-new", "topical", "volatile", now, 0.78);

        List<Document> ranked = ranker.rerank(List.of(stale, fresh), 2);
        assertEquals("topical-window-new", ranked.get(0).getMetadata().get("source"),
                "recency bonus should let the fresher document overtake");
    }

    @Test
    @DisplayName("recency never overturns a decisively better semantic match")
    void recencyDoesNotHijackStrongMatches() {
        Document strongHistory = doc("moment-munich-1958", "moment", "stable-history", null, 0.90);
        Document freshTopical  = doc("topical-window", "topical", "volatile", now, 0.70);

        List<Document> ranked = ranker.rerank(List.of(freshTopical, strongHistory), 2);
        assertEquals("moment-munich-1958", ranked.get(0).getMetadata().get("source"),
                "a 0.20 similarity gap must survive a max 0.15 recency bonus");
    }

    @Test
    @DisplayName("archived content is demoted but still retrievable")
    void archivedIsDemotedNotRemoved() {
        Document archived = doc("topical-window-2025", "topical", "archived", now, 0.85);
        Document current  = doc("topical-window-2026", "topical", "volatile", now, 0.80);

        List<Document> ranked = ranker.rerank(List.of(archived, current), 2);
        assertEquals("topical-window-2026", ranked.get(0).getMetadata().get("source"));
        assertEquals(2, ranked.size(), "archived content must remain available, just ranked lower");
    }

    @Test
    @DisplayName("missing score or asOfEpoch degrades gracefully")
    void handlesMissingFields() {
        Map<String, Object> md = new HashMap<>();
        md.put("source", "bare");
        Document bare = Document.builder().text("no metadata").metadata(md).build();
        assertDoesNotThrow(() -> ranker.rerank(List.of(bare), 1));
        assertEquals(1, ranker.rerank(List.of(bare), 1).size());
    }

    @Test
    @DisplayName("rerank truncates to topK")
    void truncatesToTopK() {
        List<Document> many = List.of(
                doc("a", "era", "stable-history", null, 0.9),
                doc("b", "era", "stable-history", null, 0.8),
                doc("c", "era", "stable-history", null, 0.7));
        assertEquals(2, ranker.rerank(many, 2).size());
    }

    @Test
    @DisplayName("empty and null candidate lists are safe")
    void handlesEmptyInput() {
        assertTrue(ranker.rerank(List.of(), 5).isEmpty());
        assertTrue(ranker.rerank(null, 5).isEmpty());
    }
}
