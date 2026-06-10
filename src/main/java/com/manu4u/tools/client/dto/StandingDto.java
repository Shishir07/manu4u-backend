package com.manu4u.tools.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/** Wraps the response from GET /standings. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StandingDto {

    private League league;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class League {
        private Integer id;
        private String name;
        /** 2D array: standings[group][rank] */
        private List<List<Standing>> standings;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Standing {
        private Integer rank;
        private Team team;
        private Integer points;
        private Integer goalsDiff;
        /** Group name — "Premier League" for single-group leagues; "Group A" etc for tournaments. */
        private String group;
        private String form;
        private String status;
        private String description;
        private All all;
        private Integer season;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Team {
        private Integer id;
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class All {
        private Integer played;
        private Integer win;
        private Integer draw;
        private Integer lose;
        private Goals goals;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Goals {
        @com.fasterxml.jackson.annotation.JsonProperty("for")
        private Integer goalsFor;
        private Integer against;
    }
}
