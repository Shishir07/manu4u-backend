package com.manu4u.tools.client;

import com.manu4u.tools.client.dto.ApiFootballResponse;
import com.manu4u.tools.client.dto.EventDto;
import com.manu4u.tools.client.dto.FixtureDto;
import com.manu4u.tools.client.dto.LeagueDto;
import com.manu4u.tools.client.dto.LineupDto;
import com.manu4u.tools.client.dto.OddsDto;
import com.manu4u.tools.client.dto.PlayerDto;
import com.manu4u.tools.client.dto.SquadDto;
import com.manu4u.tools.client.dto.StandingDto;
import com.manu4u.tools.client.dto.StatisticsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

@Slf4j
@Component
public class ApiFootballClient {

    private final WebClient webClient;

    public ApiFootballClient(@Qualifier("apiFootballWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public ApiFootballResponse<FixtureDto> getFixtures(Integer teamId, String date, Integer season) {
        try {
            var uriBuilder = webClient.get()
                    .uri(uriBuilder1 -> {
                        var builder = uriBuilder1.path("/fixtures")
                                .queryParam("team", teamId)
                                .queryParam("date", date);
                        if (season != null) {
                            builder.queryParam("season", season);
                        }
                        return builder.build();
                    });
            
            ApiFootballResponse<FixtureDto> response = uriBuilder
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiFootballResponse<FixtureDto>>() {})
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            checkForErrors(response, "fixtures");
            return response;
        } catch (WebClientResponseException e) {
            String errorMessage = String.format("API request failed with status %d: %s", 
                    e.getStatusCode().value(), e.getResponseBodyAsString());
            log.error("Error calling API-Football /fixtures: {}", errorMessage);
            throw new ApiFootballException(errorMessage, e);
        } catch (ApiFootballException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error calling API-Football /fixtures", e);
            throw new ApiFootballException("Unexpected error fetching fixtures: " + e.getMessage(), e);
        }
    }

    public ApiFootballResponse<EventDto> getFixtureEvents(Integer fixtureId) {
        try {
            ApiFootballResponse<EventDto> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/fixtures/events")
                            .queryParam("fixture", fixtureId)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiFootballResponse<EventDto>>() {})
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            checkForErrors(response, "fixtures/events");
            return response;
        } catch (WebClientResponseException e) {
            String errorMessage = String.format("API request failed with status %d: %s", 
                    e.getStatusCode().value(), e.getResponseBodyAsString());
            log.error("Error calling API-Football /fixtures/events: {}", errorMessage);
            throw new ApiFootballException(errorMessage, e);
        } catch (ApiFootballException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error calling API-Football /fixtures/events", e);
            throw new ApiFootballException("Unexpected error fetching fixture events: " + e.getMessage(), e);
        }
    }

    public ApiFootballResponse<LineupDto> getFixtureLineups(Integer fixtureId) {
        try {
            ApiFootballResponse<LineupDto> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/fixtures/lineups")
                            .queryParam("fixture", fixtureId)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiFootballResponse<LineupDto>>() {})
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            checkForErrors(response, "fixtures/lineups");
            return response;
        } catch (WebClientResponseException e) {
            String errorMessage = String.format("API request failed with status %d: %s", 
                    e.getStatusCode().value(), e.getResponseBodyAsString());
            log.error("Error calling API-Football /fixtures/lineups: {}", errorMessage);
            throw new ApiFootballException(errorMessage, e);
        } catch (ApiFootballException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error calling API-Football /fixtures/lineups", e);
            throw new ApiFootballException("Unexpected error fetching fixture lineups: " + e.getMessage(), e);
        }
    }

    public ApiFootballResponse<StatisticsDto> getFixtureStatistics(Integer fixtureId, Integer teamId, String type, Boolean half) {
        try {
            ApiFootballResponse<StatisticsDto> response = webClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/fixtures/statistics")
                                .queryParam("fixture", fixtureId);
                        if (teamId != null) {
                            builder.queryParam("team", teamId);
                        }
                        if (type != null) {
                            builder.queryParam("type", type);
                        }
                        if (half != null) {
                            builder.queryParam("half", half);
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiFootballResponse<StatisticsDto>>() {})
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            checkForErrors(response, "fixtures/statistics");
            return response;
        } catch (WebClientResponseException e) {
            String errorMessage = String.format("API request failed with status %d: %s", 
                    e.getStatusCode().value(), e.getResponseBodyAsString());
            log.error("Error calling API-Football /fixtures/statistics: {}", errorMessage);
            throw new ApiFootballException(errorMessage, e);
        } catch (ApiFootballException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error calling API-Football /fixtures/statistics", e);
            throw new ApiFootballException("Unexpected error fetching fixture statistics: " + e.getMessage(), e);
        }
    }

    // ── New endpoints ────────────────────────────────────────────────────────

    public ApiFootballResponse<FixtureDto> getFixturesByRange(Integer teamId, String fromDate, String toDate, Integer season) {
        try {
            ApiFootballResponse<FixtureDto> response = webClient.get()
                    .uri(uri -> {
                        var b = uri.path("/fixtures").queryParam("team", teamId)
                                .queryParam("from", fromDate).queryParam("to", toDate);
                        if (season != null) b.queryParam("season", season);
                        return b.build();
                    })
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiFootballResponse<FixtureDto>>() {})
                    .timeout(Duration.ofSeconds(30))
                    .block();
            checkForErrors(response, "fixtures/range");
            return response;
        } catch (WebClientResponseException e) {
            throw new ApiFootballException("fixtures/range " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString(), e);
        } catch (ApiFootballException e) { throw e; }
        catch (Exception e) { throw new ApiFootballException("Unexpected error: " + e.getMessage(), e); }
    }

    public ApiFootballResponse<StandingDto> getStandings(Integer leagueId, Integer season) {
        try {
            ApiFootballResponse<StandingDto> response = webClient.get()
                    .uri(uri -> uri.path("/standings")
                            .queryParam("league", leagueId)
                            .queryParam("season", season)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiFootballResponse<StandingDto>>() {})
                    .timeout(Duration.ofSeconds(30))
                    .block();
            checkForErrors(response, "standings");
            return response;
        } catch (WebClientResponseException e) {
            throw new ApiFootballException("standings " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString(), e);
        } catch (ApiFootballException e) { throw e; }
        catch (Exception e) { throw new ApiFootballException("Unexpected error: " + e.getMessage(), e); }
    }

    /**
     * @param leagueId optional — when provided the API returns statistics[] with a single entry
     *                 for that league, eliminating the need for client-side filtering.
     */
    public ApiFootballResponse<PlayerDto> getPlayerStats(Integer playerId, Integer season, Integer leagueId) {
        try {
            ApiFootballResponse<PlayerDto> response = webClient.get()
                    .uri(uri -> {
                        var b = uri.path("/players")
                                .queryParam("id", playerId)
                                .queryParam("season", season);
                        if (leagueId != null) b.queryParam("league", leagueId);
                        return b.build();
                    })
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiFootballResponse<PlayerDto>>() {})
                    .timeout(Duration.ofSeconds(30))
                    .block();
            checkForErrors(response, "players");
            return response;
        } catch (WebClientResponseException e) {
            throw new ApiFootballException("players " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString(), e);
        } catch (ApiFootballException e) { throw e; }
        catch (Exception e) { throw new ApiFootballException("Unexpected error: " + e.getMessage(), e); }
    }

    public ApiFootballResponse<PlayerDto> getTopScorers(Integer leagueId, Integer season) {
        try {
            ApiFootballResponse<PlayerDto> response = webClient.get()
                    .uri(uri -> uri.path("/players/topscorers")
                            .queryParam("league", leagueId)
                            .queryParam("season", season)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiFootballResponse<PlayerDto>>() {})
                    .timeout(Duration.ofSeconds(30))
                    .block();
            checkForErrors(response, "players/topscorers");
            return response;
        } catch (WebClientResponseException e) {
            throw new ApiFootballException("topscorers " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString(), e);
        } catch (ApiFootballException e) { throw e; }
        catch (Exception e) { throw new ApiFootballException("Unexpected error: " + e.getMessage(), e); }
    }

    public ApiFootballResponse<PlayerDto> getTopAssists(Integer leagueId, Integer season) {
        try {
            ApiFootballResponse<PlayerDto> response = webClient.get()
                    .uri(uri -> uri.path("/players/topassists")
                            .queryParam("league", leagueId)
                            .queryParam("season", season)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiFootballResponse<PlayerDto>>() {})
                    .timeout(Duration.ofSeconds(30))
                    .block();
            checkForErrors(response, "players/topassists");
            return response;
        } catch (WebClientResponseException e) {
            throw new ApiFootballException("topassists " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString(), e);
        } catch (ApiFootballException e) { throw e; }
        catch (Exception e) { throw new ApiFootballException("Unexpected error: " + e.getMessage(), e); }
    }

    public ApiFootballResponse<OddsDto> getPreMatchOdds(Integer fixtureId) {
        try {
            ApiFootballResponse<OddsDto> response = webClient.get()
                    .uri(uri -> uri.path("/odds")
                            .queryParam("fixture", fixtureId)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiFootballResponse<OddsDto>>() {})
                    .timeout(Duration.ofSeconds(30))
                    .block();
            checkForErrors(response, "odds");
            return response;
        } catch (WebClientResponseException e) {
            log.warn("Odds unavailable (status {}), subscription may not include odds", e.getStatusCode().value());
            return null; // graceful degradation
        } catch (ApiFootballException e) { throw e; }
        catch (Exception e) {
            log.warn("Could not fetch pre-match odds: {}", e.getMessage());
            return null;
        }
    }

    public ApiFootballResponse<OddsDto> getLiveOdds(Integer fixtureId) {
        try {
            ApiFootballResponse<OddsDto> response = webClient.get()
                    .uri(uri -> uri.path("/odds/live")
                            .queryParam("fixture", fixtureId)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiFootballResponse<OddsDto>>() {})
                    .timeout(Duration.ofSeconds(30))
                    .block();
            checkForErrors(response, "odds/live");
            return response;
        } catch (WebClientResponseException e) {
            log.warn("Live odds unavailable (status {}), subscription may not include live odds", e.getStatusCode().value());
            return null;
        } catch (ApiFootballException e) { throw e; }
        catch (Exception e) {
            log.warn("Could not fetch live odds: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Fetch leagues filtered by country name and/or current-season flag.
     *
     * @param country e.g. "England" or "World" — null to skip filter
     * @param current true = only leagues with an active current season
     */
    public ApiFootballResponse<LeagueDto> getLeagues(String country, Boolean current) {
        try {
            ApiFootballResponse<LeagueDto> response = webClient.get()
                    .uri(uri -> {
                        var b = uri.path("/leagues");
                        if (country != null) b.queryParam("country", country);
                        if (current != null)  b.queryParam("current", current);
                        return b.build();
                    })
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiFootballResponse<LeagueDto>>() {})
                    .timeout(Duration.ofSeconds(30))
                    .block();
            checkForErrors(response, "leagues");
            return response;
        } catch (WebClientResponseException e) {
            throw new ApiFootballException("leagues " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString(), e);
        } catch (ApiFootballException e) { throw e; }
        catch (Exception e) { throw new ApiFootballException("Unexpected error: " + e.getMessage(), e); }
    }

    public ApiFootballResponse<SquadDto> getSquad(Integer teamId) {
        try {
            ApiFootballResponse<SquadDto> response = webClient.get()
                    .uri(uri -> uri.path("/players/squads")
                            .queryParam("team", teamId)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiFootballResponse<SquadDto>>() {})
                    .timeout(Duration.ofSeconds(30))
                    .block();
            checkForErrors(response, "players/squads");
            return response;
        } catch (WebClientResponseException e) {
            throw new ApiFootballException("squads " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString(), e);
        } catch (ApiFootballException e) { throw e; }
        catch (Exception e) { throw new ApiFootballException("Unexpected error: " + e.getMessage(), e); }
    }

    private <T> void checkForErrors(ApiFootballResponse<T> response, String endpoint) {
        if (response == null) {
            throw new ApiFootballException("Empty response from API-Football " + endpoint);
        }
        
        if (response.hasErrors()) {
            String errorMessage = response.getErrorMessage();
            log.error("API-Football {} returned errors: {}", endpoint, errorMessage);
            throw new ApiFootballException("API returned errors: " + errorMessage);
        }
    }

    public static class ApiFootballException extends RuntimeException {
        public ApiFootballException(String message) {
            super(message);
        }

        public ApiFootballException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
