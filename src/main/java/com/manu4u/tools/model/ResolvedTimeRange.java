package com.manu4u.tools.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Result of resolving a natural-language time expression.
 * For single-date expressions (today, next Saturday) from == to and isRange == false.
 * For range expressions (last week, this month) from != to and isRange == true.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResolvedTimeRange {
    /** Start of the resolved period (inclusive). */
    private LocalDate from;
    /** End of the resolved period (inclusive). For single dates, equals from. */
    private LocalDate to;
    private String timezone;
    /** Human-readable description, e.g. "last week (2026-03-16 to 2026-03-22)". */
    private String description;
    /** True when the result is a date range — use get_fixtures_range instead of get_fixtures. */
    private boolean isRange;
}
