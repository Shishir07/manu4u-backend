package com.manu4u.tools.service;

import com.manu4u.tools.client.ApiFootballClient;
import com.manu4u.tools.client.dto.StatisticsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsTool {

    private final ApiFootballClient apiFootballClient;

    public List<StatisticsDto> getFixtureStatistics(Integer fixtureId, Integer teamId, String type, Boolean half) {
        var response = apiFootballClient.getFixtureStatistics(fixtureId, teamId, type, half);
        
        if (response == null || response.getResponse() == null) {
            return List.of();
        }

        return response.getResponse();
    }
}
