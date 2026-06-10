package com.manu4u.tools.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/** Wraps one entry from GET /leagues. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeagueDto {

    private LeagueInfo league;
    private CountryInfo country;
    private List<SeasonInfo> seasons;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LeagueInfo {
        private Integer id;
        private String name;
        /** "League" or "Cup" */
        private String type;
        private String logo;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CountryInfo {
        private String name;
        private String code;
        private String flag;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SeasonInfo {
        private Integer year;
        private String start;
        private String end;
        private boolean current;
    }
}
