package com.manu4u.tools.service;

import com.manu4u.tools.model.ResolvedTimeRange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves natural-language time expressions to concrete date ranges.
 *
 * <p>Supported expressions:
 * <ul>
 *   <li>Single days: today, tomorrow, yesterday</li>
 *   <li>Day-of-week: monday…sunday, last/next/this monday…sunday</li>
 *   <li>Weeks: this week, last week, next week</li>
 *   <li>Months: this month, last month, next month, january…december, last/next january</li>
 *   <li>Football seasons: this season, last season, next season, season 2024, 2024-25 season</li>
 *   <li>Relative ranges: last/next N days, last/next N weeks, last/next N months</li>
 *   <li>Absolute dates: 2024-01-15, 15 January 2024, Jan 15 2024, etc.</li>
 * </ul>
 */
@Slf4j
@Service
public class TimeTool {

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,               // 2024-01-15
            DateTimeFormatter.ofPattern("d MMMM yyyy"),     // 15 January 2024
            DateTimeFormatter.ofPattern("d MMM yyyy"),      // 15 Jan 2024
            DateTimeFormatter.ofPattern("MMMM d, yyyy"),    // January 15, 2024
            DateTimeFormatter.ofPattern("MMM d, yyyy"),     // Jan 15, 2024
            DateTimeFormatter.ofPattern("d/M/yyyy"),        // 15/1/2024
            DateTimeFormatter.ofPattern("d-MMM-yyyy")       // 15-Jan-2024
    );

    private static final Pattern LAST_N_UNIT =
            Pattern.compile("last (\\d+) (days?|weeks?|months?)");
    private static final Pattern NEXT_N_UNIT =
            Pattern.compile("next (\\d+) (days?|weeks?|months?)");

    public ResolvedTimeRange resolveDate(String expression, String timezone) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Date expression cannot be blank");
        }
        ZoneId zoneId = ZoneId.of(timezone);
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        String expr = expression.trim().toLowerCase();

        // ── 1. Exact keywords ────────────────────────────────────────────────────
        switch (expr) {
            case "today":
                return single(now.toLocalDate(), timezone, "today");
            case "tomorrow":
                return single(now.toLocalDate().plusDays(1), timezone, "tomorrow");
            case "yesterday":
                return single(now.toLocalDate().minusDays(1), timezone, "yesterday");
            case "this week": {
                LocalDate m = startOfWeek(now);
                return range(m, m.plusDays(6), timezone, "this week");
            }
            case "last week": {
                LocalDate m = startOfWeek(now).minusWeeks(1);
                return range(m, m.plusDays(6), timezone, "last week");
            }
            case "next week": {
                LocalDate m = startOfWeek(now).plusWeeks(1);
                return range(m, m.plusDays(6), timezone, "next week");
            }
            case "this month": {
                LocalDate f = now.toLocalDate().withDayOfMonth(1);
                return range(f, f.withDayOfMonth(f.lengthOfMonth()), timezone, "this month");
            }
            case "last month": {
                LocalDate f = now.toLocalDate().withDayOfMonth(1).minusMonths(1);
                return range(f, f.withDayOfMonth(f.lengthOfMonth()), timezone, "last month");
            }
            case "next month": {
                LocalDate f = now.toLocalDate().withDayOfMonth(1).plusMonths(1);
                return range(f, f.withDayOfMonth(f.lengthOfMonth()), timezone, "next month");
            }
            case "this season":
                return footballSeason(footballSeasonYear(now.toLocalDate()), timezone);
            case "last season":
                return footballSeason(footballSeasonYear(now.toLocalDate()) - 1, timezone);
            case "next season":
                return footballSeason(footballSeasonYear(now.toLocalDate()) + 1, timezone);
        }

        // ── 2. Extract optional prefix (last/next/this) ─────────────────────────
        String prefix = null;
        String rest = expr;
        if (expr.startsWith("last ")) { prefix = "last"; rest = expr.substring(5); }
        else if (expr.startsWith("next ")) { prefix = "next"; rest = expr.substring(5); }
        else if (expr.startsWith("this ")) { prefix = "this"; rest = expr.substring(5); }

        // ── 3. Day-of-week patterns ──────────────────────────────────────────────
        DayOfWeek dow = parseDayOfWeek(rest);
        if (dow != null) {
            LocalDate today = now.toLocalDate();
            LocalDate date;
            if ("last".equals(prefix)) {
                // strictly before today
                date = today.with(TemporalAdjusters.previous(dow));
            } else if ("next".equals(prefix)) {
                // strictly after today
                date = today.with(TemporalAdjusters.next(dow));
            } else if ("this".equals(prefix)) {
                // this calendar week's occurrence (Mon-based)
                date = startOfWeek(now).with(TemporalAdjusters.nextOrSame(dow));
            } else {
                // bare day name → most recent occurrence including today
                date = today.with(TemporalAdjusters.previousOrSame(dow));
            }
            return single(date, timezone, expr);
        }

        // ── 4. Month name patterns ───────────────────────────────────────────────
        Month month = parseMonthName(rest);
        if (month != null) {
            int year = now.getYear();
            LocalDate firstOfMonth;
            if ("last".equals(prefix)) {
                // most recently passed occurrence of that month
                LocalDate candidate = LocalDate.of(year, month, 1);
                if (!candidate.isBefore(now.toLocalDate().withDayOfMonth(1))) {
                    candidate = candidate.minusYears(1);
                }
                firstOfMonth = candidate;
            } else if ("next".equals(prefix)) {
                // next future occurrence of that month
                LocalDate candidate = LocalDate.of(year, month, 1);
                if (!candidate.isAfter(now.toLocalDate())) {
                    candidate = candidate.plusYears(1);
                }
                firstOfMonth = candidate;
            } else {
                // bare month name → that month in current year
                firstOfMonth = LocalDate.of(year, month, 1);
            }
            LocalDate lastOfMonth = firstOfMonth.withDayOfMonth(firstOfMonth.lengthOfMonth());
            String label = month.name().charAt(0) + month.name().substring(1).toLowerCase()
                    + " " + firstOfMonth.getYear();
            return range(firstOfMonth, lastOfMonth, timezone, label);
        }

        // ── 5. "last/next N unit" patterns ───────────────────────────────────────
        Matcher mLast = LAST_N_UNIT.matcher(expr);
        if (mLast.matches()) {
            int n = Integer.parseInt(mLast.group(1));
            String unit = mLast.group(2);
            LocalDate today = now.toLocalDate();
            LocalDate from = unit.startsWith("day")   ? today.minusDays(n) :
                             unit.startsWith("week")  ? today.minusWeeks(n) :
                                                        today.minusMonths(n);
            return range(from, today, timezone, "last " + n + " " + unit);
        }

        Matcher mNext = NEXT_N_UNIT.matcher(expr);
        if (mNext.matches()) {
            int n = Integer.parseInt(mNext.group(1));
            String unit = mNext.group(2);
            LocalDate today = now.toLocalDate();
            LocalDate to = unit.startsWith("day")   ? today.plusDays(n) :
                           unit.startsWith("week")  ? today.plusWeeks(n) :
                                                      today.plusMonths(n);
            return range(today, to, timezone, "next " + n + " " + unit);
        }

        // ── 6. Football season patterns ──────────────────────────────────────────
        // "season 2024", "2024 season", "2024-25 season", "2024/25"
        if (expr.startsWith("season ") && expr.length() >= 11) {
            try {
                int seasonYear = Integer.parseInt(expr.substring(7, 11));
                return footballSeason(seasonYear, timezone);
            } catch (NumberFormatException ignored) {}
        }
        if (expr.endsWith(" season") && expr.length() >= 11) {
            try {
                int seasonYear = Integer.parseInt(expr.substring(0, 4));
                return footballSeason(seasonYear, timezone);
            } catch (NumberFormatException ignored) {}
        }
        if (expr.matches("\\d{4}[/-]\\d{2}")) {
            int seasonYear = Integer.parseInt(expr.substring(0, 4));
            return footballSeason(seasonYear, timezone);
        }

        // ── 7. Absolute date fallback ─────────────────────────────────────────────
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                LocalDate date = LocalDate.parse(expression.trim(), fmt);
                return single(date, timezone, expression.trim());
            } catch (DateTimeParseException ignored) {
            }
        }

        throw new IllegalArgumentException(
                "Unsupported date expression: '" + expression + "'. " +
                "Supported: today, tomorrow, yesterday, this/last/next week/month/season, " +
                "day names (monday, last friday, next saturday), " +
                "month names (january, last october), " +
                "last/next N days/weeks/months, season YYYY, " +
                "or absolute date like 2024-01-15");
    }

    // ─────────────────── Helpers ───────────────────────────────────────────────

    private static LocalDate startOfWeek(ZonedDateTime now) {
        return now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** Returns the football season start year for a given date. */
    public static int footballSeasonYear(LocalDate date) {
        return date.getMonthValue() >= 8 ? date.getYear() : date.getYear() - 1;
    }

    private static ResolvedTimeRange footballSeason(int seasonYear, String timezone) {
        LocalDate from = LocalDate.of(seasonYear, 8, 1);
        LocalDate to   = LocalDate.of(seasonYear + 1, 7, 31);
        return range(from, to, timezone, "season " + seasonYear + "/" + (seasonYear + 1));
    }

    private static ResolvedTimeRange single(LocalDate date, String timezone, String desc) {
        return new ResolvedTimeRange(date, date, timezone, desc, false);
    }

    private static ResolvedTimeRange range(LocalDate from, LocalDate to, String timezone, String desc) {
        return new ResolvedTimeRange(from, to, timezone, desc + " (" + from + " to " + to + ")", true);
    }

    private static DayOfWeek parseDayOfWeek(String s) {
        return switch (s) {
            case "monday",    "mon" -> DayOfWeek.MONDAY;
            case "tuesday",   "tue" -> DayOfWeek.TUESDAY;
            case "wednesday", "wed" -> DayOfWeek.WEDNESDAY;
            case "thursday",  "thu" -> DayOfWeek.THURSDAY;
            case "friday",    "fri" -> DayOfWeek.FRIDAY;
            case "saturday",  "sat" -> DayOfWeek.SATURDAY;
            case "sunday",    "sun" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }

    private static Month parseMonthName(String s) {
        return switch (s) {
            case "january",   "jan" -> Month.JANUARY;
            case "february",  "feb" -> Month.FEBRUARY;
            case "march",     "mar" -> Month.MARCH;
            case "april",     "apr" -> Month.APRIL;
            case "may"              -> Month.MAY;
            case "june",      "jun" -> Month.JUNE;
            case "july",      "jul" -> Month.JULY;
            case "august",    "aug" -> Month.AUGUST;
            case "september", "sep" -> Month.SEPTEMBER;
            case "october",   "oct" -> Month.OCTOBER;
            case "november",  "nov" -> Month.NOVEMBER;
            case "december",  "dec" -> Month.DECEMBER;
            default -> null;
        };
    }
}
