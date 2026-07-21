package com.manu4u.tools.service;

import com.manu4u.tools.config.Manu4uProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Re-ranks vector-search candidates so that freshness matters for content where
 * freshness actually matters — and nowhere else.
 *
 * <p><b>Why not simply boost by newest {@code asOfEpoch}?</b> Because {@code AS OF}
 * records when a document was last <i>verified</i>, not when its events happened.
 * Every file in the seed corpus carries {@code AS OF: 2026-07} — the Munich 1958
 * document and the 2026 summer transfer window document have the same date. A naive
 * age-based boost would therefore do nothing today, and would actively misfire later:
 * re-verify the transfer document next month and the Treble document gets penalised
 * for being "old" when it is in fact timeless.</p>
 *
 * <p>So recency keys off <b>volatility</b>, not age:</p>
 * <ul>
 *   <li><b>Timeless</b> ({@code status: stable-history}, or category era/legend/moment) —
 *       no recency adjustment. A 1999 question competes on semantic similarity alone.</li>
 *   <li><b>Time-sensitive</b> (topical, news, anything volatile/needs-update) —
 *       exponential freshness bonus, so a newer transfer report outranks a superseded one.</li>
 *   <li><b>Archived</b> — additive penalty. Still retrievable (it may be exactly what a
 *       historical query wants) but ranked below current material.</li>
 * </ul>
 *
 * <p>Because the bonus only ever applies to time-sensitive documents, this needs no
 * query classification: a question about 1999 retrieves era/moment documents, which
 * receive a zero boost, leaving their ordering untouched.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecencyRanker {

    private final Manu4uProperties properties;

    /** Categories whose content does not go stale (see CORPUS_GUIDE.md). */
    private static final Set<String> TIMELESS_CATEGORIES = Set.of("era", "legend", "moment");

    private static final String STATUS_STABLE   = "stable-history";
    private static final String STATUS_ARCHIVED = "archived";

    private static final double SECONDS_PER_DAY = 86_400.0;

    /**
     * Re-score candidates and return the best {@code topK}.
     *
     * @param candidates over-fetched results from the vector store (scores populated)
     * @param topK       how many to return after re-ranking
     */
    public List<Document> rerank(List<Document> candidates, int topK) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        Instant now = Instant.now();
        Manu4uProperties.Retrieval cfg = properties.getRetrieval();

        List<Document> ranked = candidates.stream()
                .sorted(Comparator.comparingDouble((Document d) -> adjustedScore(d, now, cfg)).reversed())
                .limit(topK)
                .toList();

        if (log.isDebugEnabled()) {
            ranked.forEach(d -> log.debug("ranked: source={} status={} base={} adjusted={}",
                    d.getMetadata().get("source"), d.getMetadata().get("status"),
                    d.getScore(), String.format("%.4f", adjustedScore(d, now, cfg))));
        }
        return ranked;
    }

    /** similarity + freshness bonus (time-sensitive only) − archive penalty. */
    double adjustedScore(Document doc, Instant now, Manu4uProperties.Retrieval cfg) {
        Map<String, Object> md = doc.getMetadata();
        double score = doc.getScore() != null ? doc.getScore() : 0.0;

        if (STATUS_ARCHIVED.equalsIgnoreCase(asString(md.get("status")))) {
            score -= cfg.getArchivePenalty();
        }
        if (isTimeSensitive(md)) {
            score += recencyBoost(md, now, cfg);
        }
        return score;
    }

    /** Explicit stable-history status wins; otherwise fall back to category. */
    private boolean isTimeSensitive(Map<String, Object> md) {
        String status = asString(md.get("status"));
        if (STATUS_STABLE.equalsIgnoreCase(status)) {
            return false;
        }
        String category = asString(md.get("category"));
        return category == null || !TIMELESS_CATEGORIES.contains(category.toLowerCase());
    }

    /** weight * e^(-ageDays / halfLife) — full weight when brand new, decaying smoothly. */
    private double recencyBoost(Map<String, Object> md, Instant now, Manu4uProperties.Retrieval cfg) {
        Long asOfEpoch = asLong(md.get("asOfEpoch"));
        if (asOfEpoch == null || cfg.getHalfLifeDays() <= 0) {
            return 0.0;
        }
        double ageDays = Math.max(0.0, (now.getEpochSecond() - asOfEpoch) / SECONDS_PER_DAY);
        return cfg.getRecencyWeight() * Math.exp(-ageDays / cfg.getHalfLifeDays());
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    /** Vector-store payloads may round-trip numbers as Integer, Long, Double or String. */
    private Long asLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
