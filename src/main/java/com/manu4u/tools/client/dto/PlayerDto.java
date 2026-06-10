package com.manu4u.tools.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/** Wraps one player entry from GET /players, /players/topscorers, /players/topassists. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerDto {

    private Player player;
    private List<Statistics> statistics;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Player {
        private Integer id;
        private String name;
        private String firstname;
        private String lastname;
        private Integer age;
        private String nationality;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Statistics {
        private Team team;
        private League league;
        private Games games;
        private Goals goals;
        private Cards cards;
        private Shots shots;
        private Passes passes;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Team {
        private Integer id;
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class League {
        private Integer id;
        private String name;
        private Integer season;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Games {
        private Integer appearences;
        private Integer lineups;
        private Integer minutes;
        private String rating;
        private String position;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Goals {
        private Integer total;
        private Integer assists;
        private Integer conceded;
        private Integer saves;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Cards {
        private Integer yellow;
        private Integer yellowred;
        private Integer red;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Shots {
        private Integer total;
        private Integer on;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Passes {
        private Integer total;
        private Integer key;
        private Integer accuracy;
    }
}
