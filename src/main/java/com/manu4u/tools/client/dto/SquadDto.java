package com.manu4u.tools.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/** Wraps one team entry from GET /players/squads. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SquadDto {

    private Team team;
    private List<Player> players;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Team {
        private Integer id;
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Player {
        private Integer id;
        private String name;
        private Integer age;
        private Integer number;
        /** e.g. "Attacker", "Midfielder", "Defender", "Goalkeeper" */
        private String position;
        private String photo;
    }
}
