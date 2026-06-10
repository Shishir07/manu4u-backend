package com.manu4u.tools.service.tools;

import com.manu4u.tools.agent.ToolCallTracker;
import com.manu4u.tools.config.Manu4uProperties;
import com.manu4u.tools.model.*;
import com.manu4u.tools.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveMatchToolsService {

    private final TimeTool timeTool;
    private final FixturesTool fixturesTool;
    private final EventsTool eventsTool;
    private final LineupsTool lineupsTool;
    private final StatisticsTool statisticsTool;
    private final StandingsTool standingsTool;
    private final PlayerStatsTool playerStatsTool;
    private final OddsTool oddsTool;
    private final Manu4uProperties properties;
    private final ToolCallTracker tracker;

    // ── Time ─────────────────────────────────────────────────────────────────

    @Tool(name = "resolve_time",
          description = "Resolve a time expression to one or more dates. " +
                        "Supports: today, tomorrow, yesterday, this/last/next week, this/last/next month, " +
                        "this/last/next season, day names (monday, last friday, next saturday), " +
                        "month names (january, last october), last/next N days/weeks/months, " +
                        "season YYYY, and absolute dates like 2024-01-15. " +
                        "Returns isRange=false for single dates (use get_fixtures), " +
                        "isRange=true for ranges (use get_fixtures_range).")
    public ResolvedTimeRange resolveTime(String expression) {
        ResolvedTimeRange result = timeTool.resolveDate(expression, properties.getTimezone());
        tracker.record("resolve_time", expression,
                result.isRange()
                        ? "range: " + result.getFrom() + " to " + result.getTo()
                        : "date: " + result.getFrom());
        return result;
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    @Tool(name = "get_fixtures",
          description = "Get Manchester United fixtures for a single date (YYYY-MM-DD). " +
                        "Use when resolve_time returns isRange=false. " +
                        "Returns fixtureId values needed by get_events, get_lineups, get_match_stats.")
    public List<FixtureSummary> getFixtures(String date) {
        List<FixtureSummary> result = fixturesTool.getFixturesByDate(properties.getDefaultTeamId(), date);
        tracker.record("get_fixtures", date,
                result.isEmpty() ? "no fixtures" : result.size() + " fixture(s)");
        return result;
    }

    @Tool(name = "get_fixtures_range",
          description = "Get Manchester United fixtures across a date range. " +
                        "Use when resolve_time returns isRange=true. " +
                        "Pass the 'from' and 'to' dates from the resolve_time result.")
    public List<FixtureSummary> getFixturesRange(String fromDate, String toDate) {
        List<FixtureSummary> result = fixturesTool.getFixturesByRange(properties.getDefaultTeamId(), fromDate, toDate);
        tracker.record("get_fixtures_range", fromDate + " → " + toDate,
                result.isEmpty() ? "no fixtures" : result.size() + " fixture(s)");
        return result;
    }

    // ── Match detail ──────────────────────────────────────────────────────────

    @Tool(name = "get_events",
          description = "Get match events (goals, assists, cards, substitutions, VAR) for a specific fixture. " +
                        "Use the fixtureId from get_fixtures or get_fixtures_range.")
    public List<MatchEvent> getEvents(Integer fixtureId) {
        List<MatchEvent> result = eventsTool.getFixtureEvents(fixtureId);
        tracker.record("get_events", "fixtureId=" + fixtureId,
                result.isEmpty() ? "no events" : result.size() + " event(s)");
        return result;
    }

    @Tool(name = "get_lineups",
          description = "Get starting XI, formation, and substitutes for a specific fixture. " +
                        "Use the fixtureId from get_fixtures or get_fixtures_range.")
    public List<Lineup> getLineups(Integer fixtureId) {
        List<Lineup> result = lineupsTool.getFixtureLineups(fixtureId);
        tracker.record("get_lineups", "fixtureId=" + fixtureId,
                result.isEmpty() ? "no lineups" : result.size() + " team(s)");
        return result;
    }

    @Tool(name = "get_match_stats",
          description = "Get match statistics (possession, shots, passes, corners, etc.) for a specific fixture. " +
                        "Use the fixtureId from get_fixtures.")
    public List<com.manu4u.tools.client.dto.StatisticsDto> getMatchStats(Integer fixtureId) {
        var result = statisticsTool.getFixtureStatistics(fixtureId, null, null, null);
        tracker.record("get_match_stats", "fixtureId=" + fixtureId,
                result.isEmpty() ? "no stats" : result.size() + " team stat block(s)");
        return result;
    }

    // ── Standings ─────────────────────────────────────────────────────────────

    @Tool(name = "get_standings",
          description = "Get current league table standings. " +
                        "leagueId: 39=Premier League, 3=UEFA Europa League, 45=FA Cup, 48=Carabao Cup. " +
                        "Use 39 for 'where are United in the table?' questions. " +
                        "Returns Man United's entry and their rank among all teams.")
    public StandingEntry getStandings(Integer leagueId) {
        StandingEntry result = standingsTool.getTeamStanding(leagueId, properties.getDefaultTeamId());
        tracker.record("get_standings", "leagueId=" + leagueId,
                result != null ? "rank=" + result.getRank() + " pts=" + result.getPoints() : "not found");
        return result;
    }

    @Tool(name = "get_full_table",
          description = "Get the full league table for a given league. " +
                        "leagueId: 39=Premier League, 3=UEFA Europa League. " +
                        "Use when asked about top teams, relegation zone, or Champions League spots.")
    public List<StandingEntry> getFullTable(Integer leagueId) {
        List<StandingEntry> result = standingsTool.getFullTable(leagueId);
        tracker.record("get_full_table", "leagueId=" + leagueId,
                result.isEmpty() ? "empty table" : result.size() + " teams");
        return result;
    }

    // ── Players ───────────────────────────────────────────────────────────────

    @Tool(name = "sync_squad",
          description = "Sync Man United's current squad from API-Football into the local database. " +
                        "Call this ONLY if find_player returns empty results, then retry find_player once. " +
                        "Do NOT call if find_player already returned data. " +
                        "Returns the number of players synced. Only needs to run once per season.")
    public String syncSquad() {
        int count = playerStatsTool.syncSquad(null);  // null = current season
        String resultMsg;
        if (count < 0) resultMsg = "Squad sync failed. Please try again later.";
        else if (count == 0) resultMsg = "No players returned from API. Squad may already be up to date.";
        else resultMsg = "Synced " + count + " players. Retry find_player now.";
        tracker.record("sync_squad", "teamId=" + properties.getDefaultTeamId(), resultMsg);
        return resultMsg;
    }

    @Tool(name = "find_player",
          description = "Find a Man United player by name and get their current season stats. " +
                        "Use for questions like 'How many goals has Rashford scored?'. " +
                        "Handles partial and fuzzy name matching — pass the full common name (e.g. 'Harry Maguire', 'Rashford'). " +
                        "If this returns empty results, call sync_squad first to populate the database, then retry find_player once. " +
                        "Returns goals, assists, cards, appearances for the current season.")
    public List<PlayerSeasonStats> findPlayer(String playerName) {
        List<PlayerSeasonStats> result = playerStatsTool.findAndFetchStats(playerName, properties.getDefaultTeamId());
        tracker.record("find_player", playerName,
                result.isEmpty() ? "no match" : result.size() + " player(s): " +
                        result.stream().map(PlayerSeasonStats::getPlayerName).toList());
        return result;
    }

    @Tool(name = "get_top_scorers",
          description = "Get top goalscorers in a league for the current season. " +
                        "leagueId: 39=Premier League, 3=Europa League, 45=FA Cup. " +
                        "Use for 'who is the Premier League top scorer?' type questions.")
    public List<PlayerSeasonStats> getTopScorers(Integer leagueId) {
        List<PlayerSeasonStats> result = playerStatsTool.getTopScorers(leagueId);
        tracker.record("get_top_scorers", "leagueId=" + leagueId,
                result.isEmpty() ? "no data" : result.size() + " players, top: " +
                        (result.get(0).getPlayerName() + "=" + result.get(0).getGoals()));
        return result;
    }

    @Tool(name = "get_top_assists",
          description = "Get top assist providers in a league for the current season. " +
                        "leagueId: 39=Premier League, 3=Europa League. " +
                        "Use for 'who has the most assists in the Premier League?' questions.")
    public List<PlayerSeasonStats> getTopAssists(Integer leagueId) {
        List<PlayerSeasonStats> result = playerStatsTool.getTopAssists(leagueId);
        tracker.record("get_top_assists", "leagueId=" + leagueId,
                result.isEmpty() ? "no data" : result.size() + " players, top: " +
                        (result.get(0).getPlayerName() + "=" + result.get(0).getAssists()));
        return result;
    }

    // ── Odds ─────────────────────────────────────────────────────────────────

    @Tool(name = "get_pre_match_odds",
          description = "Get pre-match betting odds for a specific fixture. " +
                        "Returns home-win, draw, away-win odds from bookmakers. " +
                        "May return empty list if subscription does not include odds.")
    public List<MatchOdds> getPreMatchOdds(Integer fixtureId) {
        List<MatchOdds> result = oddsTool.getPreMatchOdds(fixtureId);
        tracker.record("get_pre_match_odds", "fixtureId=" + fixtureId,
                result.isEmpty() ? "no odds (subscription?)" : result.size() + " bookmaker(s)");
        return result;
    }

    @Tool(name = "get_live_odds",
          description = "Get live in-play odds for a currently active fixture. " +
                        "Use during live matches when user asks about current odds. " +
                        "May return empty list if subscription does not include live odds.")
    public List<MatchOdds> getLiveOdds(Integer fixtureId) {
        List<MatchOdds> result = oddsTool.getLiveOdds(fixtureId);
        tracker.record("get_live_odds", "fixtureId=" + fixtureId,
                result.isEmpty() ? "no live odds" : result.size() + " bookmaker(s)");
        return result;
    }
}
