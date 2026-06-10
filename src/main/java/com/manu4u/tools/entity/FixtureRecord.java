package com.manu4u.tools.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "fixture_cache")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FixtureRecord {

    @Id
    private Integer fixtureId;

    private LocalDate matchDate;
    private Integer season;
    private String homeTeam;
    private String awayTeam;
    /** Short status code: FT, NS, 1H, HT, 2H, ET, BT, P, PEN, AET, CANC, ABD, AWD, WO, INT, PST */
    private String status;
    private Instant utcKickoff;

    /** JSON-serialised List<MatchEvent> — null until first fetch */
    @Column(columnDefinition = "TEXT")
    private String eventsJson;

    /** JSON-serialised List<Lineup> — null until first fetch */
    @Column(columnDefinition = "TEXT")
    private String lineupsJson;

    /** JSON-serialised List<StatisticsDto> — null until first fetch */
    @Column(columnDefinition = "TEXT")
    private String statisticsJson;

    /** When fixture metadata row was last written */
    private Instant cachedAt;
    /** When eventsJson was last written */
    private Instant eventsCachedAt;
    /** When lineupsJson was last written */
    private Instant lineupsCachedAt;
    /** When statisticsJson was last written */
    private Instant statisticsCachedAt;
}
