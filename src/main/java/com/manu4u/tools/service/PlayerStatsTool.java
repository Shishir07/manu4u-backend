package com.manu4u.tools.service;

import com.manu4u.tools.client.ApiFootballClient;
import com.manu4u.tools.client.dto.PlayerDto;
import com.manu4u.tools.config.Manu4uProperties;
import com.manu4u.tools.entity.Player;
import com.manu4u.tools.model.PlayerSeasonStats;
import com.manu4u.tools.repository.PlayerRepository;
import com.manu4u.tools.service.sync.MasterDataSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerStatsTool {

    private final ApiFootballClient apiFootballClient;
    private final PlayerRepository playerRepo;
    private final Manu4uProperties properties;
    private final MasterDataSyncService syncService;

    /**
     * Looks up player(s) by name in the local DB, then fetches current season stats
     * filtered to the default league (Premier League). Used by the find_player @Tool.
     */
    public List<PlayerSeasonStats> findAndFetchStats(String playerName, Integer teamId) {
        int season = currentSeason();
        Integer leagueId = properties.getDefaultLeagueId();
        List<Player> matches = resolvePlayerName(playerName);
        if (matches.isEmpty()) {
            log.info("No players found for name '{}' in DB — squad sync may be needed", playerName);
            return Collections.emptyList();
        }
        return matches.stream()
                .map(p -> fetchStats(p.getExternalId(), season, leagueId))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Multi-token name resolution using the new players table.
     * 1. Try the full input string (e.g. "Harry Maguire")
     * 2. Try each individual word longest-first (e.g. "Maguire", then "Harry")
     * 3. Deduplicate by externalId
     */
    private List<Player> resolvePlayerName(String playerName) {
        List<Player> full = playerRepo.findByNameContainingIgnoreCase(playerName.trim());
        if (!full.isEmpty()) {
            log.info("Player '{}' matched by full name", playerName);
            return full;
        }

        String[] tokens = playerName.trim().split("\\s+");
        Map<Integer, Player> seen = new LinkedHashMap<>();
        Arrays.stream(tokens)
                .filter(t -> t.length() > 1)
                .sorted((a, b) -> b.length() - a.length())
                .forEach(token -> {
                    playerRepo.findByNameContainingIgnoreCase(token)
                            .forEach(p -> seen.putIfAbsent(p.getExternalId(), p));
                });

        if (!seen.isEmpty()) {
            log.info("Player '{}' matched via token search: {}", playerName,
                    seen.values().stream().map(Player::getName).toList());
            return new ArrayList<>(seen.values());
        }

        return Collections.emptyList();
    }

    public PlayerSeasonStats fetchStats(Integer playerExternalId, Integer season, Integer leagueId) {
        try {
            var response = apiFootballClient.getPlayerStats(playerExternalId, season, leagueId);
            if (response == null || response.getResponse() == null || response.getResponse().isEmpty()) {
                return null;
            }
            return toStats(response.getResponse().get(0));
        } catch (Exception e) {
            log.error("Failed to fetch stats for player {} league {}: {}", playerExternalId, leagueId, e.getMessage());
            return null;
        }
    }

    public List<PlayerSeasonStats> getTopScorers(Integer leagueId) {
        int season = currentSeason();
        try {
            var response = apiFootballClient.getTopScorers(leagueId, season);
            if (response == null || response.getResponse() == null) return Collections.emptyList();
            return response.getResponse().stream().map(this::toStats).toList();
        } catch (Exception e) {
            log.error("Failed to fetch top scorers for league {}: {}", leagueId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<PlayerSeasonStats> getTopAssists(Integer leagueId) {
        int season = currentSeason();
        try {
            var response = apiFootballClient.getTopAssists(leagueId, season);
            if (response == null || response.getResponse() == null) return Collections.emptyList();
            return response.getResponse().stream().map(this::toStats).toList();
        } catch (Exception e) {
            log.error("Failed to fetch top assists for league {}: {}", leagueId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Maps PlayerDto to PlayerSeasonStats.
     * The statistics array is pre-filtered server-side by leagueId (passed to API),
     * so statistics[0] is the correct league entry.
     */
    private PlayerSeasonStats toStats(PlayerDto dto) {
        PlayerDto.Statistics stats = (dto.getStatistics() != null && !dto.getStatistics().isEmpty())
                ? dto.getStatistics().get(0) : null;
        return PlayerSeasonStats.builder()
                .playerId(dto.getPlayer() != null ? dto.getPlayer().getId() : null)
                .playerName(dto.getPlayer() != null ? dto.getPlayer().getName() : null)
                .teamName(stats != null && stats.getTeam() != null ? stats.getTeam().getName() : null)
                .leagueName(stats != null && stats.getLeague() != null ? stats.getLeague().getName() : null)
                .season(stats != null && stats.getLeague() != null ? stats.getLeague().getSeason() : null)
                .position(stats != null && stats.getGames() != null ? stats.getGames().getPosition() : null)
                .appearances(stats != null && stats.getGames() != null ? stats.getGames().getAppearences() : null)
                .minutesPlayed(stats != null && stats.getGames() != null ? stats.getGames().getMinutes() : null)
                .rating(stats != null && stats.getGames() != null ? stats.getGames().getRating() : null)
                .goals(stats != null && stats.getGoals() != null ? stats.getGoals().getTotal() : null)
                .assists(stats != null && stats.getGoals() != null ? stats.getGoals().getAssists() : null)
                .yellowCards(stats != null && stats.getCards() != null ? stats.getCards().getYellow() : null)
                .redCards(stats != null && stats.getCards() != null ? stats.getCards().getRed() : null)
                .shotsTotal(stats != null && stats.getShots() != null ? stats.getShots().getTotal() : null)
                .shotsOnTarget(stats != null && stats.getShots() != null ? stats.getShots().getOn() : null)
                .passesKey(stats != null && stats.getPasses() != null ? stats.getPasses().getKey() : null)
                .build();
    }

    /**
     * Delegates to MasterDataSyncService — kept for the sync_squad @Tool backward compatibility.
     */
    public int syncSquad(Integer season) {
        return syncService.syncPlayers(season);
    }

    private int currentSeason() {
        return TimeTool.footballSeasonYear(LocalDate.now());
    }
}
