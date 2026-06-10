package com.manu4u.tools.controller;

import com.manu4u.tools.config.Manu4uProperties;
import com.manu4u.tools.model.*;
import com.manu4u.tools.client.dto.StatisticsDto;
import com.manu4u.tools.service.*;
import com.manu4u.tools.service.tools.LiveMatchToolsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/tools")
@RequiredArgsConstructor
@Tag(name = "Tools", description = "API endpoints for time resolution, fixtures, events, and lineups")
public class ToolsController {

    private final TimeTool timeTool;
    private final FixturesTool fixturesTool;
    private final EventsTool eventsTool;
    private final LineupsTool lineupsTool;
    private final StatisticsTool statisticsTool;
    private final StandingsTool standingsTool;
    private final PlayerStatsTool playerStatsTool;
    private final Manu4uProperties manu4uProperties;

    @Operation(summary = "Resolve date expression", description = "Resolves natural-language time expressions to dates or date ranges")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Date resolved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid date expression")
    })
    @GetMapping("/time")
    public ResponseEntity<ResolvedTimeRange> resolveTime(
            @Parameter(description = "Expression: today, last week, next saturday, this season, last 7 days, …", example = "last week")
            @RequestParam(defaultValue = "today") String expr,
            @Parameter(description = "Timezone (defaults to configured timezone)", example = "America/Vancouver")
            @RequestParam(required = false) String timezone) {
        String tz = timezone != null ? timezone : manu4uProperties.getTimezone();
        ResolvedTimeRange resolved = timeTool.resolveDate(expr, tz);
        return ResponseEntity.ok(resolved);
    }

    @Operation(summary = "Get today's fixtures", description = "Returns fixtures for the default team (Manchester United) for today")
    @ApiResponse(responseCode = "200", description = "Fixtures retrieved successfully")
    @GetMapping("/fixtures/today")
    public ResponseEntity<List<FixtureSummary>> getFixturesToday() {
        ResolvedTimeRange today = timeTool.resolveDate("today", manu4uProperties.getTimezone());
        String dateStr = today.getFrom().toString();
        List<FixtureSummary> fixtures = fixturesTool.getFixturesByDate(
                manu4uProperties.getDefaultTeamId(), 
                dateStr
        );
        return ResponseEntity.ok(fixtures);
    }

    @Operation(summary = "Get fixtures by date", description = "Returns fixtures for a specific team and date")
    @ApiResponse(responseCode = "200", description = "Fixtures retrieved successfully")
    @GetMapping("/fixtures")
    public ResponseEntity<List<FixtureSummary>> getFixtures(
            @Parameter(description = "Team ID (defaults to Manchester United: 33)", example = "33")
            @RequestParam(required = false) Integer teamId,
            @Parameter(description = "Date in YYYY-MM-DD format", example = "2024-01-25")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Integer team = teamId != null ? teamId : manu4uProperties.getDefaultTeamId();
        String dateStr = date.toString();
        List<FixtureSummary> fixtures = fixturesTool.getFixturesByDate(team, dateStr);
        return ResponseEntity.ok(fixtures);
    }

    @Operation(summary = "Get fixture events", description = "Returns all events (goals, cards, substitutions, etc.) for a specific fixture")
    @ApiResponse(responseCode = "200", description = "Events retrieved successfully")
    @GetMapping("/events/{fixtureId}")
    public ResponseEntity<List<MatchEvent>> getFixtureEvents(
            @Parameter(description = "Fixture ID", example = "123456")
            @PathVariable Integer fixtureId) {
        List<MatchEvent> events = eventsTool.getFixtureEvents(fixtureId);
        return ResponseEntity.ok(events);
    }

    @Operation(summary = "Get fixture lineups", description = "Returns starting lineups and substitutes for both teams in a fixture")
    @ApiResponse(responseCode = "200", description = "Lineups retrieved successfully")
    @GetMapping("/lineups/{fixtureId}")
    public ResponseEntity<List<Lineup>> getFixtureLineups(
            @Parameter(description = "Fixture ID", example = "123456")
            @PathVariable Integer fixtureId) {
        List<Lineup> lineups = lineupsTool.getFixtureLineups(fixtureId);
        return ResponseEntity.ok(lineups);
    }

    @Operation(summary = "Get fixture statistics", description = "Returns statistics for a fixture, optionally filtered by team, type, and half")
    @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    @GetMapping("/statistics/{fixtureId}")
    public ResponseEntity<List<StatisticsDto>> getFixtureStatistics(
            @Parameter(description = "Fixture ID", example = "123456")
            @PathVariable Integer fixtureId,
            @Parameter(description = "Team ID (optional)", example = "33")
            @RequestParam(required = false) Integer teamId,
            @Parameter(description = "Statistics type (optional)")
            @RequestParam(required = false) String type,
            @Parameter(description = "Include halftime statistics (optional, default: false)", example = "false")
            @RequestParam(required = false) Boolean half) {
        List<StatisticsDto> statistics = statisticsTool.getFixtureStatistics(fixtureId, teamId, type, half);
        return ResponseEntity.ok(statistics);
    }

    @Operation(summary = "Get league standings", description = "Returns Man United's position in the league table")
    @ApiResponse(responseCode = "200", description = "Standings retrieved successfully")
    @GetMapping("/standings")
    public ResponseEntity<StandingEntry> getStandings(
            @Parameter(description = "League ID (39=PL, 3=Europa, 45=FA Cup)", example = "39")
            @RequestParam(defaultValue = "39") Integer leagueId) {
        return ResponseEntity.ok(standingsTool.getTeamStanding(leagueId, manu4uProperties.getDefaultTeamId()));
    }

    @Operation(summary = "Get full league table", description = "Returns all teams in the league table")
    @ApiResponse(responseCode = "200", description = "Table retrieved successfully")
    @GetMapping("/table")
    public ResponseEntity<List<StandingEntry>> getFullTable(
            @Parameter(description = "League ID (39=PL, 3=Europa)", example = "39")
            @RequestParam(defaultValue = "39") Integer leagueId) {
        return ResponseEntity.ok(standingsTool.getFullTable(leagueId));
    }

    @Operation(summary = "Find player stats", description = "Search for a Man United player by name and get season stats")
    @ApiResponse(responseCode = "200", description = "Player stats retrieved")
    @GetMapping("/players/search")
    public ResponseEntity<List<PlayerSeasonStats>> findPlayer(
            @Parameter(description = "Player name (partial match)", example = "Rashford")
            @RequestParam String name) {
        return ResponseEntity.ok(playerStatsTool.findAndFetchStats(name, manu4uProperties.getDefaultTeamId()));
    }

    @Operation(summary = "Get top scorers", description = "Returns top scorers in a league for the current season")
    @ApiResponse(responseCode = "200", description = "Top scorers retrieved")
    @GetMapping("/players/topscorers")
    public ResponseEntity<List<PlayerSeasonStats>> getTopScorers(
            @Parameter(description = "League ID (39=PL, 3=Europa)", example = "39")
            @RequestParam(defaultValue = "39") Integer leagueId) {
        return ResponseEntity.ok(playerStatsTool.getTopScorers(leagueId));
    }

}
