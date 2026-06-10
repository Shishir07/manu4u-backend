package com.manu4u.tools.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatisticsDto {
    private Team team;
    private List<Statistic> statistics;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Team {
        private Integer id;
        private String name;
        private String logo;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Statistic {
        private String type;
        private Object value; // Can be String, Integer, or null
    }
}
