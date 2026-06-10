package com.manu4u.tools.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchOdds {
    private String bookmaker;
    /** Decimal odds for home team win, e.g. "1.85" */
    private String homeWin;
    /** Decimal odds for a draw, e.g. "3.50" */
    private String draw;
    /** Decimal odds for away team win, e.g. "4.20" */
    private String awayWin;
}
