package com.manu4u.tools.service;

import com.manu4u.tools.client.ApiFootballClient;
import com.manu4u.tools.client.dto.StandingDto;
import com.manu4u.tools.model.StandingEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StandingsTool {

    private final ApiFootballClient apiFootballClient;

    /** Returns Man United's entry in the given league's table. */
    public StandingEntry getTeamStanding(Integer leagueId, Integer teamId) {
        int season = currentSeason();
        return getFullTable(leagueId).stream()
                .filter(e -> e.getTeamId() != null && e.getTeamId().equals(teamId))
                .findFirst()
                .orElse(null);
    }

    /** Returns the full league table, all teams, ordered by rank. */
    public List<StandingEntry> getFullTable(Integer leagueId) {
        int season = currentSeason();
        try {
            var response = apiFootballClient.getStandings(leagueId, season);
            if (response == null || response.getResponse() == null || response.getResponse().isEmpty()) {
                return Collections.emptyList();
            }
            StandingDto dto = response.getResponse().get(0);
            if (dto.getLeague() == null || dto.getLeague().getStandings() == null) {
                return Collections.emptyList();
            }
            String leagueName = dto.getLeague().getName();
            return dto.getLeague().getStandings().stream()
                    .flatMap(List::stream)
                    .map(s -> toEntry(s, leagueName, leagueId, season))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to fetch standings for league {}: {}", leagueId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private StandingEntry toEntry(StandingDto.Standing s, String leagueName, Integer leagueId, int season) {
        StandingDto.All all = s.getAll();
        StandingDto.Goals goals = all != null ? all.getGoals() : null;
        Integer gf = goals != null ? goals.getGoalsFor() : null;
        Integer ga = goals != null ? goals.getAgainst() : null;
        return StandingEntry.builder()
                .rank(s.getRank())
                .teamId(s.getTeam() != null ? s.getTeam().getId() : null)
                .teamName(s.getTeam() != null ? s.getTeam().getName() : null)
                .points(s.getPoints())
                .played(all != null ? all.getPlayed() : null)
                .won(all != null ? all.getWin() : null)
                .drawn(all != null ? all.getDraw() : null)
                .lost(all != null ? all.getLose() : null)
                .goalsFor(gf)
                .goalsAgainst(ga)
                .goalDifference(s.getGoalsDiff())
                .form(s.getForm())
                .description(s.getDescription())
                .group(s.getGroup())
                .leagueName(leagueName)
                .leagueId(leagueId)
                .season(season)
                .build();
    }

    private int currentSeason() {
        return TimeTool.footballSeasonYear(LocalDate.now());
    }
}
