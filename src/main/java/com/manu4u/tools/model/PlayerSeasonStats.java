package com.manu4u.tools.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerSeasonStats {
    private Integer playerId;
    private String playerName;
    private String teamName;
    private String leagueName;
    private Integer season;
    private String position;
    // Games
    private Integer appearances;
    private Integer minutesPlayed;
    private String rating;
    // Goals & assists
    private Integer goals;
    private Integer assists;
    // Cards
    private Integer yellowCards;
    private Integer redCards;
    // Shots
    private Integer shotsTotal;
    private Integer shotsOnTarget;
    // Passes
    private Integer passesKey;
}
