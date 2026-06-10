package com.manu4u.tools.service.sync;

import com.manu4u.tools.client.ApiFootballClient;
import com.manu4u.tools.client.dto.LeagueDto;
import com.manu4u.tools.client.dto.SquadDto;
import com.manu4u.tools.config.Manu4uProperties;
import com.manu4u.tools.entity.*;
import com.manu4u.tools.repository.*;
import com.manu4u.tools.service.TimeTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MasterDataSyncService {

    private final ApiFootballClient apiFootballClient;
    private final Manu4uProperties properties;

    private final CountryRepository countryRepo;
    private final LeagueRepository leagueRepo;
    private final SeasonRepository seasonRepo;
    private final TeamRepository teamRepo;
    private final PlayerRepository playerRepo;

    // ── Countries ─────────────────────────────────────────────────────────────

    /**
     * Seeds England and World — the two country scopes we care about.
     * Idempotent: skips if already present.
     */
    @Transactional
    public SyncResult syncCountries() {
        List<String[]> seeds = List.of(
                new String[]{"ENG",   "England"},
                new String[]{"World", "World"}
        );
        int created = 0;
        for (String[] s : seeds) {
            if (countryRepo.findByExternalCode(s[0]).isEmpty()) {
                countryRepo.save(Country.builder()
                        .externalCode(s[0])
                        .name(s[1])
                        .build());
                created++;
                log.info("Seeded country: {}", s[1]);
            }
        }
        return new SyncResult("countries", created, seeds.size() - created);
    }

    // ── Leagues ───────────────────────────────────────────────────────────────

    /**
     * Syncs all current leagues for England + World from API-Football.
     * Upserts by externalId.
     */
    @Transactional
    public SyncResult syncLeagues() {
        Country england = countryRepo.findByExternalCode("ENG")
                .orElseThrow(() -> new IllegalStateException("Country 'England' not seeded — run syncCountries first"));
        Country world = countryRepo.findByExternalCode("World")
                .orElseThrow(() -> new IllegalStateException("Country 'World' not seeded — run syncCountries first"));

        List<LeagueDto> allLeagues = new ArrayList<>();
        allLeagues.addAll(fetchLeagues("England"));
        allLeagues.addAll(fetchLeagues("World"));

        if (allLeagues.isEmpty()) {
            log.warn("No leagues returned from API");
            return new SyncResult("leagues", 0, 0);
        }

        // Build existing map for bulk upsert
        List<Integer> externalIds = allLeagues.stream()
                .filter(l -> l.getLeague() != null)
                .map(l -> l.getLeague().getId())
                .toList();
        Map<Integer, League> existing = leagueRepo.findAll().stream()
                .filter(l -> externalIds.contains(l.getExternalId()))
                .collect(Collectors.toMap(League::getExternalId, Function.identity()));

        List<League> toSave = new ArrayList<>();
        for (LeagueDto dto : allLeagues) {
            if (dto.getLeague() == null) continue;
            LeagueDto.LeagueInfo info = dto.getLeague();
            LeagueDto.CountryInfo countryInfo = dto.getCountry();

            Long countryId = (countryInfo != null && "World".equalsIgnoreCase(countryInfo.getName()))
                    ? world.getId() : england.getId();

            // Find current season year
            Integer currentSeason = null;
            if (dto.getSeasons() != null) {
                currentSeason = dto.getSeasons().stream()
                        .filter(LeagueDto.SeasonInfo::isCurrent)
                        .map(LeagueDto.SeasonInfo::getYear)
                        .findFirst()
                        .orElse(null);
            }

            League league = existing.getOrDefault(info.getId(),
                    League.builder().externalId(info.getId()).build());
            league.setName(info.getName());
            league.setType(info.getType());
            league.setCountryId(countryId);
            league.setCurrentSeason(currentSeason);
            league.setLogoUrl(info.getLogo());
            toSave.add(league);
        }

        leagueRepo.saveAll(toSave);
        log.info("Synced {} leagues", toSave.size());
        return new SyncResult("leagues", toSave.size(), 0);
    }

    private List<LeagueDto> fetchLeagues(String country) {
        try {
            var response = apiFootballClient.getLeagues(country, true);
            if (response == null || response.getResponse() == null) return Collections.emptyList();
            return response.getResponse();
        } catch (Exception e) {
            log.error("Failed to fetch leagues for country {}: {}", country, e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── Seasons ───────────────────────────────────────────────────────────────

    /**
     * Seeds the last 5 football seasons (computed, no API call).
     * Idempotent: skips years already present.
     */
    @Transactional
    public SyncResult syncSeasons() {
        int currentStartYear = TimeTool.footballSeasonYear(LocalDate.now());
        int created = 0;

        for (int i = 0; i < 5; i++) {
            int startYear = currentStartYear - i;
            if (seasonRepo.findByStartYear(startYear).isEmpty()) {
                boolean isCurrent = (i == 0);
                seasonRepo.save(Season.builder()
                        .startYear(startYear)
                        .endYear(startYear + 1)
                        .label(startYear + "-" + String.valueOf(startYear + 1).substring(2))
                        .current(isCurrent)
                        .build());
                created++;
                log.info("Seeded season: {}-{}", startYear, startYear + 1);
            } else if (i == 0) {
                // Ensure current flag is set correctly
                seasonRepo.findByStartYear(startYear).ifPresent(s -> {
                    if (!s.isCurrent()) {
                        s.setCurrent(true);
                        seasonRepo.save(s);
                    }
                });
            }
        }
        return new SyncResult("seasons", created, 5 - created);
    }

    // ── Teams ─────────────────────────────────────────────────────────────────

    /**
     * Seeds Man United (externalId=33). Idempotent.
     */
    @Transactional
    public SyncResult syncTeams() {
        Country england = countryRepo.findByExternalCode("ENG").orElse(null);
        Long countryId = england != null ? england.getId() : null;

        if (teamRepo.findByExternalId(properties.getDefaultTeamId()).isEmpty()) {
            teamRepo.save(Team.builder()
                    .externalId(properties.getDefaultTeamId())
                    .name("Manchester United")
                    .code("MUN")
                    .countryId(countryId)
                    .founded(1878)
                    .venueName("Old Trafford")
                    .build());
            log.info("Seeded team: Manchester United (externalId={})", properties.getDefaultTeamId());
            return new SyncResult("teams", 1, 0);
        }
        return new SyncResult("teams", 0, 1);
    }

    // ── Players ───────────────────────────────────────────────────────────────

    /**
     * Syncs Man United's squad from API-Football into the players table.
     * Uses bulk findByExternalIdIn + saveAll (no N+1).
     *
     * @param season football season start year; null = current
     * @return number of players synced
     */
    @Transactional
    public int syncPlayers(Integer season) {
        int resolvedSeason = season != null ? season : TimeTool.footballSeasonYear(LocalDate.now());
        log.info("Syncing Man United squad (season {})", resolvedSeason);

        Team team = teamRepo.findByExternalId(properties.getDefaultTeamId())
                .orElseThrow(() -> new IllegalStateException("Team not seeded — run syncTeams first"));

        try {
            var response = apiFootballClient.getSquad(properties.getDefaultTeamId());
            if (response == null || response.getResponse() == null || response.getResponse().isEmpty()) {
                log.warn("No squad data from API-Football");
                return 0;
            }

            SquadDto squadDto = response.getResponse().get(0);
            if (squadDto.getPlayers() == null || squadDto.getPlayers().isEmpty()) return 0;

            List<SquadDto.Player> apiPlayers = squadDto.getPlayers().stream()
                    .filter(p -> p.getId() != null)
                    .toList();

            // Bulk fetch existing records — one SELECT
            List<Integer> extIds = apiPlayers.stream().map(SquadDto.Player::getId).toList();
            Map<Integer, Player> existing = playerRepo.findByExternalIdIn(extIds).stream()
                    .collect(Collectors.toMap(Player::getExternalId, Function.identity()));

            Instant now = Instant.now();
            List<Player> records = new ArrayList<>();
            for (SquadDto.Player p : apiPlayers) {
                Player record = existing.getOrDefault(p.getId(),
                        Player.builder().externalId(p.getId()).build());
                record.setName(p.getName());
                record.setPosition(p.getPosition());
                record.setJerseyNumber(p.getNumber());
                record.setAge(p.getAge());
                record.setTeamId(team.getId());
                record.setSyncedAt(now);
                records.add(record);
            }

            playerRepo.saveAll(records);
            log.info("Synced {} players for season {}", records.size(), resolvedSeason);
            return records.size();

        } catch (Exception e) {
            log.error("Squad sync failed: {}", e.getMessage(), e);
            return -1;
        }
    }

    // ── Sync All ──────────────────────────────────────────────────────────────

    /**
     * Runs all sync operations in dependency order.
     * Safe to call repeatedly — all operations are idempotent.
     */
    @Transactional
    public Map<String, Object> syncAll() {
        Map<String, Object> results = new LinkedHashMap<>();
        results.put("countries", syncCountries());
        results.put("leagues",   syncLeagues());
        results.put("seasons",   syncSeasons());
        results.put("teams",     syncTeams());
        int players = syncPlayers(null);
        results.put("players",   new SyncResult("players", players, 0));
        return results;
    }

    // ── Result DTO ────────────────────────────────────────────────────────────

    public record SyncResult(String entity, int created, int skipped) {}
}
