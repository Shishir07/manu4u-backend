package com.manu4u.tools.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "player_statistics",
       indexes = {
           @Index(name = "idx_ps_player_id",  columnList = "player_id"),
           @Index(name = "idx_ps_season_id",  columnList = "season_id"),
           @Index(name = "idx_ps_league_id",  columnList = "league_id")
       },
       uniqueConstraints = @UniqueConstraint(
           name = "uq_player_statistics",
           columnNames = {"player_id", "season_id", "league_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → players.id (our surrogate id) */
    @Column(name = "player_id", nullable = false)
    private Long playerId;

    /** FK → seasons.id (our surrogate id) */
    @Column(name = "season_id", nullable = false)
    private Long seasonId;

    /** FK → leagues.id (our surrogate id) */
    @Column(name = "league_id", nullable = false)
    private Long leagueId;

    /** FK → teams.id — player may be on loan at a different club in this league */
    @Column(name = "team_id")
    private Long teamId;

    private Integer appearances;
    private Integer minutesPlayed;
    private Integer goals;
    private Integer assists;
    private Integer yellowCards;
    private Integer redCards;

    /** API returns rating as string, e.g. "6.58" */
    private String rating;

    private Integer shotsTotal;
    private Integer shotsOnTarget;
    private Integer passesKey;

    private Instant syncedAt;
}
