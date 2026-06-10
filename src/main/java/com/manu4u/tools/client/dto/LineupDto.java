package com.manu4u.tools.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LineupDto {
    private Team team;
    private Coach coach;
    private String formation;
    private List<Player> startXI;
    private List<Player> substitutes;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Team {
        private Integer id;
        private String name;
        private String logo;
        private Colors colors;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Colors {
            private ColorInfo player;
            private ColorInfo goalkeeper;

            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class ColorInfo {
                private String primary;
                private String number;
                private String border;
            }
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Coach {
        private Integer id;
        private String name;
        private String photo;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Player {
        private PlayerInfo player;
        private Integer number;
        private String position;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class PlayerInfo {
            private Integer id;
            private String name;
            private Integer number;
            private String pos;
            private String grid;
        }
    }
}
