package com.manu4u.tools.controller;

import com.manu4u.tools.service.sync.MasterDataSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin/sync")
@RequiredArgsConstructor
@Tag(name = "Sync", description = "Master data sync endpoints — seed reference tables from API-Football")
public class SyncController {

    private final MasterDataSyncService syncService;

    @Operation(summary = "Sync all master data",
               description = "Runs all sync steps in order: countries → leagues → seasons → teams → players. " +
                             "All operations are idempotent.")
    @PostMapping("/all")
    public ResponseEntity<Map<String, Object>> syncAll() {
        log.info("Starting full master data sync");
        return ResponseEntity.ok(syncService.syncAll());
    }

    @Operation(summary = "Seed countries", description = "Seeds England and World. Idempotent.")
    @PostMapping("/countries")
    public ResponseEntity<MasterDataSyncService.SyncResult> syncCountries() {
        return ResponseEntity.ok(syncService.syncCountries());
    }

    @Operation(summary = "Sync leagues",
               description = "Fetches all current leagues for England + World from API-Football. Upserts by externalId.")
    @PostMapping("/leagues")
    public ResponseEntity<MasterDataSyncService.SyncResult> syncLeagues() {
        return ResponseEntity.ok(syncService.syncLeagues());
    }

    @Operation(summary = "Seed seasons", description = "Seeds the last 5 football seasons (computed). Idempotent.")
    @PostMapping("/seasons")
    public ResponseEntity<MasterDataSyncService.SyncResult> syncSeasons() {
        return ResponseEntity.ok(syncService.syncSeasons());
    }

    @Operation(summary = "Seed teams", description = "Seeds Manchester United (externalId=33). Idempotent.")
    @PostMapping("/teams")
    public ResponseEntity<MasterDataSyncService.SyncResult> syncTeams() {
        return ResponseEntity.ok(syncService.syncTeams());
    }

    @Operation(summary = "Sync players",
               description = "Fetches Man United's squad from API-Football and upserts into players table.")
    @PostMapping("/players")
    public ResponseEntity<Map<String, Object>> syncPlayers(
            @RequestParam(required = false) Integer season) {
        log.info("Syncing players for season {}", season != null ? season : "current");
        int count = syncService.syncPlayers(season);
        if (count < 0) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Sync failed — check logs"));
        }
        return ResponseEntity.ok(Map.of(
                "synced", count,
                "season", season != null ? season : "current"
        ));
    }
}
