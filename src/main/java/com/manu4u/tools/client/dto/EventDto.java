package com.manu4u.tools.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventDto {
    private Time time;
    private Team team;
    private Player player;
    private Player assist;
    private String type;
    private String detail;
    private String comments;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Time {
        private Integer elapsed;
        private Integer extra;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Team {
        private Integer id;
        private String name;
        private String logo;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Player {
        private Integer id;
        private String name;
    }
}
