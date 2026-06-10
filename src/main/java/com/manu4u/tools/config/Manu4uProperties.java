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
}
