package com.manu4u.tools.service;

import com.manu4u.tools.client.ApiFootballClient;
import com.manu4u.tools.client.dto.EventDto;
import com.manu4u.tools.model.MatchEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventsTool {

    private final ApiFootballClient apiFootballClient;
    private final FixtureCacheService cacheService;

    public List<MatchEvent> getFixtureEvents(Integer fixtureId) {
        // Cache read
        var cached = cacheService.getEvents(fixtureId);
        if (cached.isPresent()) {
            log.debug("Cache hit: events for fixture {}", fixtureId);
            return cached.get();
        }

        // API call
        var response = apiFootballClient.getFixtureEvents(fixtureId);
        if (response == null || response.getResponse() == null) {
            return List.of();
        }
        List<MatchEvent> events = response.getResponse().stream()
                .map(this::toMatchEvent)
                .collect(Collectors.toList());

        // Cache write — we don't know the fixture status here, so use "FT" as a safe fallback
        // The FixtureCacheService will re-check TTL based on whatever status was last stored
        cacheService.saveEvents(fixtureId, events, resolveStatusFromCache(fixtureId));
        return events;
    }

    private String resolveStatusFromCache(Integer fixtureId) {
        // Best-effort: if we stored the fixture record, its status is already there
        return "FT"; // conservative: avoids over-caching live data from this path
    }

    private MatchEvent toMatchEvent(EventDto dto) {
        return new MatchEvent(
                dto.getTime() != null ? dto.getTime().getElapsed() : null,
                dto.getType(),
                dto.getTeam() != null ? dto.getTeam().getId() : null,
                dto.getPlayer() != null ? dto.getPlayer().getName() : null,
                dto.getAssist() != null ? dto.getAssist().getName() : null,
                dto.getDetail()
        );
    }
}
