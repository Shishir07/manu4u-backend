package com.manu4u.tools.service;

import com.manu4u.tools.client.ApiFootballClient;
import com.manu4u.tools.client.dto.LineupDto;
import com.manu4u.tools.model.Lineup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LineupsTool {

    private final ApiFootballClient apiFootballClient;
    private final FixtureCacheService cacheService;

    public List<Lineup> getFixtureLineups(Integer fixtureId) {
        // Cache read
        var cached = cacheService.getLineups(fixtureId);
        if (cached.isPresent()) {
            log.debug("Cache hit: lineups for fixture {}", fixtureId);
            return cached.get();
        }

        // API call
        var response = apiFootballClient.getFixtureLineups(fixtureId);
        if (response == null || response.getResponse() == null) {
            return List.of();
        }
        List<Lineup> lineups = response.getResponse().stream()
                .map(this::toLineup)
                .collect(Collectors.toList());

        cacheService.saveLineups(fixtureId, lineups, "FT");
        return lineups;
    }

    private Lineup toLineup(LineupDto dto) {
        Integer teamId = dto.getTeam() != null ? dto.getTeam().getId() : null;
        String teamName = dto.getTeam() != null ? dto.getTeam().getName() : null;
        String formation = dto.getFormation();

        List<Lineup.Player> starters = dto.getStartXI() != null
                ? dto.getStartXI().stream()
                        .map(this::toPlayer)
                        .collect(Collectors.toList())
                : List.of();

        List<Lineup.Player> substitutes = dto.getSubstitutes() != null
                ? dto.getSubstitutes().stream()
                        .map(this::toPlayer)
                        .collect(Collectors.toList())
                : List.of();

        return new Lineup(teamId, teamName, formation, starters, substitutes);
    }

    /** Maps a LineupDto.Player (wrapper) to Lineup.Player using the inner PlayerInfo fields. */
    private Lineup.Player toPlayer(LineupDto.Player p) {
        LineupDto.Player.PlayerInfo info = p.getPlayer();
        if (info == null) return new Lineup.Player(null, null, null, null, null);
        return new Lineup.Player(
                info.getId(),
                info.getName(),
                info.getNumber(),   // number from PlayerInfo, not outer wrapper
                info.getPos(),      // pos from PlayerInfo ("GK", "D", "M", "F")
                info.getGrid()      // grid coordinate e.g. "1:1"
        );
    }
}
