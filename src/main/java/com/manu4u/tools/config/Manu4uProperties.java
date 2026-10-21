package com.manu4u.tools.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "manu4u")
public class Manu4uProperties {
    private String timezone = "America/Vancouver";
    private Integer defaultTeamId = 33;      // Manchester United
    private Integer defaultLeagueId = 39;    // Premier League
    private boolean knowledgeBaseEnabled = false;

    /** Data provider name — used as a label on synced data. */
    private String providerName = "API-Football";

    /**
     * Shared API key for guest (unauthenticated) UI sessions.
     * Set via MANU4U_GUEST_API_KEY env var. Frontend includes this as X-Api-Key header.
     * Not a secret in the traditional sense — its purpose is rate-limiting, not identity.
     */
    private String guestApiKey;

    /** Knowledge-base retrieval tuning — see RecencyRanker. */
    private Retrieval retrieval = new Retrieval();

    @Data
    public static class Retrieval {
        /**
         * Over-fetch multiplier: similaritySearch pulls topK * this, then the
         * ranker re-scores and truncates back to topK. Larger = more candidates
         * for recency to reorder, at slightly higher Qdrant cost.
         */
        private int overFetchFactor = 3;

        /**
         * Maximum additive recency bonus for a brand-new time-sensitive document.
         * Cosine similarity is ~0..1, so 0.15 is enough to let a fresh document
         * overtake a marginally better-matching stale one, without letting recency
         * hijack a clearly historical query.
         */
        private double recencyWeight = 0.15;

        /** Exponential decay half-life (days) applied to time-sensitive content. */
        private double halfLifeDays = 45.0;

        /** Additive penalty for documents whose status is 'archived'. */
        private double archivePenalty = 0.20;
    }
}
