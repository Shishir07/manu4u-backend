package com.manu4u.tools.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/** Wraps one entry from GET /odds or GET /odds/live. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OddsDto {

    private Fixture fixture;
    private League league;
    private List<Bookmaker> bookmakers;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Fixture {
        private Integer id;
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
    public static class Bookmaker {
        private Integer id;
        private String name;
        private List<Bet> bets;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Bet {
        private Integer id;
        private String name;
        private List<Value> values;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Value {
        private String value;
        private String odd;
    }
}
