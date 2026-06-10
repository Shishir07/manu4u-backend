package com.manu4u.tools.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manu4u.tools.entity.FixtureRecord;
import com.manu4u.tools.model.FixtureSummary;
import com.manu4u.tools.model.Lineup;
import com.manu4u.tools.model.MatchEvent;
import com.manu4u.tools.repository.FixtureRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Read-through / write-through cache for API-Football fixture data stored in PostgreSQL.
 *
 * <p>TTL rules:
 * <ul>
 *   <li>Completed matches (FT, AET, PEN, CANC, …): cached indefinitely</li>
 *   <li>Not-started (NS): 60-minute TTL</li>
 *   <li>Live (1H, HT, 2H, ET, BT, P): 60-second TTL</li>
 *   <li>Live events / lineups: 30-second TTL</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FixtureCacheService {

    private static final Set<String> COMPLETED = Set.of(
            "FT", "AET", "PEN", "AWD", "WO", "CANC", "ABD", "INT", "PST");
    private static final Set<String> LIVE = Set.of(
            "1H", "HT", "2H", "ET", "BT", "P");

    private static final long NS_TTL_SECONDS   = 3600;  // 60 min
    private static final long LIVE_TTL_SECONDS =   60;  // 1 min
    private static final long LIVE_DETAIL_TTL  =   30;  // 30 s for events/lineups during live match

    private final FixtureRecordRepository repo;
    private final ObjectMapper objectMapper;

    // ── Fixtures by date ──────────────────────────────────────────────────────

    public Optional<List<FixtureSummary>> getFixturesByDate(LocalDate date) {
        List<FixtureRecord> records = repo.findByMatchDate(date);
        if (records.isEmpty()) {
            return Optional.empty();
        }
        // If any fixture on this date is stale, force a full refresh
        boolean anyStale = records.stream()
                .anyMatch(r -> !isFreshMetadata(r.getCachedAt(), r.getStatus()));
        if (anyStale) {
            log.debug("Cache stale for date {}, forcing refresh", date);
            return Optional.empty();
        }
        log.debug("Cache hit for fixtures on {}", date);
        return Optional.of(records.stream().map(this::toFixtureSummary).toList());
    }

    public void saveFixtures(List<FixtureSummary> fixtures, LocalDate date) {
        if (fixtures == null || fixtures.isEmpty()) return;
        Instant now = Instant.now();
        for (FixtureSummary f : fixtures) {
            if (f.getId() == null) continue;
            FixtureRecord record = repo.findById(f.getId())
                    .orElse(FixtureRecord.builder().fixtureId(f.getId()).build());
            record.setMatchDate(date);
            record.setHomeTeam(f.getHomeTeam());
            record.setAwayTeam(f.getAwayTeam());
            record.setStatus(f.getStatus());
            record.setUtcKickoff(f.getUtcKickoff());
            record.setCachedAt(now);
            repo.save(record);
        }
        log.debug("Saved {} fixtures for {}", fixtures.size(), date);
    }

    // ── Events ────────────────────────────────────────────────────────────────

    public Optional<List<MatchEvent>> getEvents(Integer fixtureId) {
        return repo.findById(fixtureId).flatMap(r -> {
            if (r.getEventsJson() == null) return Optional.empty();
            if (!isFreshDetail(r.getEventsCachedAt(), r.getStatus())) {
                log.debug("Events cache stale for fixture {}", fixtureId);
                return Optional.empty();
            }
            log.debug("Events cache hit for fixture {}", fixtureId);
            return deserialize(r.getEventsJson(), new TypeReference<List<MatchEvent>>() {});
        });
    }

    public void saveEvents(Integer fixtureId, List<MatchEvent> events, String status) {
        repo.findById(fixtureId).ifPresent(r -> {
            try {
                r.setEventsJson(objectMapper.writeValueAsString(events));
                r.setEventsCachedAt(Instant.now());
                r.setStatus(status);
                repo.save(r);
                log.debug("Saved {} events for fixture {}", events.size(), fixtureId);
            } catch (Exception e) {
                log.warn("Failed to serialize events for fixture {}: {}", fixtureId, e.getMessage());
            }
        });
    }

    // ── Lineups ───────────────────────────────────────────────────────────────

    public Optional<List<Lineup>> getLineups(Integer fixtureId) {
        return repo.findById(fixtureId).flatMap(r -> {
            if (r.getLineupsJson() == null) return Optional.empty();
            if (!isFreshDetail(r.getLineupsCachedAt(), r.getStatus())) {
                log.debug("Lineups cache stale for fixture {}", fixtureId);
                return Optional.empty();
            }
            log.debug("Lineups cache hit for fixture {}", fixtureId);
            return deserialize(r.getLineupsJson(), new TypeReference<List<Lineup>>() {});
        });
    }

    public void saveLineups(Integer fixtureId, List<Lineup> lineups, String status) {
        repo.findById(fixtureId).ifPresent(r -> {
            try {
                r.setLineupsJson(objectMapper.writeValueAsString(lineups));
                r.setLineupsCachedAt(Instant.now());
                r.setStatus(status);
                repo.save(r);
                log.debug("Saved lineups for fixture {}", fixtureId);
            } catch (Exception e) {
                log.warn("Failed to serialize lineups for fixture {}: {}", fixtureId, e.getMessage());
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** TTL check for fixture metadata (date, teams, status). */
    private boolean isFreshMetadata(Instant cachedAt, String status) {
        if (cachedAt == null) return false;
        if (COMPLETED.contains(status)) return true;
        long age = Duration.between(cachedAt, Instant.now()).getSeconds();
        if (LIVE.contains(status)) return age < LIVE_TTL_SECONDS;
        return age < NS_TTL_SECONDS; // NS or unknown
    }

    /** TTL check for events / lineups detail data. */
    private boolean isFreshDetail(Instant cachedAt, String status) {
        if (cachedAt == null) return false;
        if (COMPLETED.contains(status)) return true;
        long age = Duration.between(cachedAt, Instant.now()).getSeconds();
        if (LIVE.contains(status)) return age < LIVE_DETAIL_TTL;
        return age < NS_TTL_SECONDS;
    }

    private FixtureSummary toFixtureSummary(FixtureRecord r) {
        return new FixtureSummary(r.getFixtureId(), r.getUtcKickoff(), r.getStatus(),
                r.getHomeTeam(), r.getAwayTeam());
    }

    private <T> Optional<T> deserialize(String json, TypeReference<T> type) {
        try {
            return Optional.of(objectMapper.readValue(json, type));
        } catch (Exception e) {
            log.warn("Failed to deserialize cached JSON: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
