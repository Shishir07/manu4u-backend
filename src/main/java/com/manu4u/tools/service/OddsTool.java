package com.manu4u.tools.service;

import com.manu4u.tools.client.ApiFootballClient;
import com.manu4u.tools.client.dto.OddsDto;
import com.manu4u.tools.model.MatchOdds;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OddsTool {

    private final ApiFootballClient apiFootballClient;

    public List<MatchOdds> getPreMatchOdds(Integer fixtureId) {
        try {
            var response = apiFootballClient.getPreMatchOdds(fixtureId);
            return parseOdds(response);
        } catch (Exception e) {
            log.warn("Pre-match odds unavailable for fixture {}: {}", fixtureId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<MatchOdds> getLiveOdds(Integer fixtureId) {
        try {
            var response = apiFootballClient.getLiveOdds(fixtureId);
            return parseOdds(response);
        } catch (Exception e) {
            log.warn("Live odds unavailable for fixture {}: {}", fixtureId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<MatchOdds> parseOdds(com.manu4u.tools.client.dto.ApiFootballResponse<OddsDto> response) {
        if (response == null || response.getResponse() == null || response.getResponse().isEmpty()) {
            return Collections.emptyList();
        }
        return response.getResponse().stream()
                .filter(o -> o.getBookmakers() != null)
                .flatMap(o -> o.getBookmakers().stream())
                .map(this::extractMatchWinner)
                .filter(odds -> odds != null)
                .toList();
    }

    private MatchOdds extractMatchWinner(OddsDto.Bookmaker bookmaker) {
        if (bookmaker.getBets() == null) return null;
        return bookmaker.getBets().stream()
                .filter(b -> "Match Winner".equalsIgnoreCase(b.getName()))
                .findFirst()
                .map(bet -> {
                    String home = null, draw = null, away = null;
                    if (bet.getValues() != null) {
                        for (var v : bet.getValues()) {
                            if ("Home".equalsIgnoreCase(v.getValue())) home = v.getOdd();
                            else if ("Draw".equalsIgnoreCase(v.getValue())) draw = v.getOdd();
                            else if ("Away".equalsIgnoreCase(v.getValue())) away = v.getOdd();
                        }
                    }
                    return MatchOdds.builder()
                            .bookmaker(bookmaker.getName())
                            .homeWin(home)
                            .draw(draw)
                            .awayWin(away)
                            .build();
                })
                .orElse(null);
    }
}
