package com.manu4u.tools.controller;

import com.manu4u.tools.config.Manu4uProperties;
import com.manu4u.tools.entity.Player;
import com.manu4u.tools.repository.PlayerRepository;
import com.manu4u.tools.repository.TeamRepository;
import com.manu4u.tools.service.PlayerStatsTool;
import com.manu4u.tools.service.TimeTool;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Administrative endpoints for data sync and maintenance")
public class AdminController {

    private final PlayerStatsTool playerStatsTool;
    private final PlayerRepository playerRepo;
    private final TeamRepository teamRepo;
    private final Manu4uProperties properties;

    @Operation(summary = "Sync Man United squad",
               description = "Fetches Man United's squad from API-Football and upserts into local players table. " +
                             "Run once per season to enable player name lookup.")
    @PostMapping("/players/sync")
    public ResponseEntity<Map<String, Object>> syncPlayers(
            @RequestParam(required = false) Integer season) {

        int resolvedSeason = season != null ? season
                : TimeTool.footballSeasonYear(LocalDate.now());

        log.info("Syncing Man United squad for season {}", resolvedSeason);

        try {
            int count = playerStatsTool.syncSquad(resolvedSeason);

            if (count == 0) {
                return ResponseEntity.ok(Map.of(
                        "synced", 0,
                        "season", resolvedSeason,
                        "message", "No squad data returned from API-Football"));
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("synced", count);
            result.put("season", resolvedSeason);
            result.put("message", "Player sync complete. Use find_player tool to look up stats.");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Player sync failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Sync failed: " + e.getMessage()));
        }
    }

    @Operation(summary = "List synced players",
               description = "Returns all Man United players currently in the local database. " +
                             "Run POST /admin/sync/players first to populate.")
    @GetMapping("/players")
    public ResponseEntity<List<Player>> listPlayers() {
        return teamRepo.findByExternalId(properties.getDefaultTeamId())
                .map(team -> ResponseEntity.ok(playerRepo.findByTeamId(team.getId())))
                .orElseGet(() -> {
                    log.warn("Team externalId={} not found — run POST /admin/sync/teams first",
                            properties.getDefaultTeamId());
                    return ResponseEntity.ok(Collections.emptyList());
                });
    }
}
