package com.manu4u.tools.service;

import com.manu4u.tools.client.ApiFootballClient;
import com.manu4u.tools.client.dto.FixtureDto;
import com.manu4u.tools.model.FixtureSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FixturesTool {

    private final ApiFootballClient apiFootballClient;
    private final FixtureCacheService cacheService;

    public List<FixtureSummary> getFixturesByDate(Integer teamId, String date) {
        LocalDate localDate = parseDate(date);

        // Cache read
        if (localDate != null) {
            var cached = cacheService.getFixturesByDate(localDate);
            if (cached.isPresent()) {
                log.debug("Cache hit: fixtures for {}", date);
                return cached.get();
            }
        }

        // API call
        Integer season = extractSeasonFromDate(date);
        var response = apiFootballClient.getFixtures(teamId, date, season);
        if (response == null || response.getResponse() == null) {
            return List.of();
        }
        List<FixtureSummary> fixtures = response.getResponse().stream()
                .map(this::toFixtureSummary)
                .collect(Collectors.toList());

        // Cache write
        if (localDate != null) {
            cacheService.saveFixtures(fixtures, localDate);
        }
        return fixtures;
    }

    public List<FixtureSummary> getFixturesByRange(Integer teamId, String fromDate, String toDate) {
        Integer season = extractSeasonFromDate(fromDate);
        var response = apiFootballClient.getFixturesByRange(teamId, fromDate, toDate, season);
        if (response == null || response.getResponse() == null) {
            return List.of();
        }
        List<FixtureSummary> fixtures = response.getResponse().stream()
                .map(this::toFixtureSummary)
                .collect(Collectors.toList());

        // Cache each individual fixture for future single-date lookups
        fixtures.forEach(f -> {
            if (f.getUtcKickoff() != null) {
                LocalDate d = f.getUtcKickoff().atZone(java.time.ZoneOffset.UTC).toLocalDate();
                cacheService.saveFixtures(List.of(f), d);
            }
        });
        return fixtures;
    }

    public FixtureSummary selectFixture(List<FixtureSummary> fixtures, String preference) {
        if (fixtures == null || fixtures.isEmpty()) return null;

        var live = fixtures.stream()
                .filter(f -> {
                    String s = f.getStatus();
                    return s != null && (
                            "LIVE".equalsIgnoreCase(s) ||
                            "1H".equals(s) || "HT".equals(s) ||
                            "2H".equals(s) || "ET".equals(s) || "PEN".equals(s));
                })
                .findFirst();
        if (live.isPresent()) return live.get();

        return fixtures.stream()
                .min(Comparator.comparing(FixtureSummary::getUtcKickoff))
                .orElse(fixtures.get(0));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private LocalDate parseDate(String date) {
        try { return LocalDate.parse(date); } catch (Exception e) { return null; }
    }

    static Integer extractSeasonFromDate(String date) {
        try {
            LocalDate d = LocalDate.parse(date);
            return d.getMonthValue() >= 8 ? d.getYear() : d.getYear() - 1;
        } catch (DateTimeParseException e) {
            if (date != null && date.length() >= 4) {
                try { return Integer.parseInt(date.substring(0, 4)); }
                catch (NumberFormatException ignored) {}
            }
            return null;
        }
    }

    private FixtureSummary toFixtureSummary(FixtureDto dto) {
        Instant utcKickoff = null;
        if (dto.getFixture() != null && dto.getFixture().getDate() != null) {
            try { utcKickoff = Instant.parse(dto.getFixture().getDate()); }
            catch (Exception e) { log.warn("Bad fixture date: {}", dto.getFixture().getDate()); }
        }
        String status = dto.getFixture() != null && dto.getFixture().getStatus() != null
                ? dto.getFixture().getStatus().getShortStatus() : null;
        String home = dto.getTeams() != null && dto.getTeams().getHome() != null
                ? dto.getTeams().getHome().getName() : null;
        String away = dto.getTeams() != null && dto.getTeams().getAway() != null
                ? dto.getTeams().getAway().getName() : null;
        return new FixtureSummary(
                dto.getFixture() != null ? dto.getFixture().getId() : null,
                utcKickoff, status, home, away);
    }
}
