package com.manu4u.tools.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandingEntry {
    private Integer rank;
    private Integer teamId;
    private String teamName;
    private Integer points;
    private Integer played;
    private Integer won;
    private Integer drawn;
    private Integer lost;
    private Integer goalsFor;
    private Integer goalsAgainst;
    private Integer goalDifference;
    /** Last 5 results, e.g. "WDLWW" */
    private String form;
    /** Promotion/relegation description, e.g. "Promotion - Champions League (League phase)" */
    private String description;
    /** Group name — same as leagueName for single-group leagues; "Group A" etc for tournaments. */
    private String group;
    private String leagueName;
    private Integer leagueId;
    private Integer season;
}
