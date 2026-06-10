package com.manu4u.tools.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FixtureDto {
    private Fixture fixture;
    private League league;
    private Teams teams;
    private Goals goals;
    private Score score;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Fixture {
        private Integer id;
        private String referee;
        private String timezone;
        private String date;
        private Long timestamp;
        private Periods periods;
        private Venue venue;
        private Status status;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Status {
            @com.fasterxml.jackson.annotation.JsonProperty("long")
            private String longStatus;
            @com.fasterxml.jackson.annotation.JsonProperty("short")
            private String shortStatus;
            private Integer elapsed;
            private Integer extra;

            @com.fasterxml.jackson.annotation.JsonGetter("long")
            public String getLongStatus() {
                return longStatus;
            }

            @com.fasterxml.jackson.annotation.JsonGetter("short")
            public String getShortStatus() {
                return shortStatus;
            }
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Periods {
            private Long first;
            private Long second;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Venue {
            private Integer id;
            private String name;
            private String city;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class League {
        private Integer id;
        private String name;
        private String country;
        private String logo;
        private String flag;
        private Integer season;
        private String round;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Teams {
        private Team home;
        private Team away;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Team {
            private Integer id;
            private String name;
            private String logo;
            private Boolean winner;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Goals {
        private Integer home;
        private Integer away;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Score {
        private ScoreDetail halftime;
        private ScoreDetail fulltime;
        private ScoreDetail extratime;
        private ScoreDetail penalty;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ScoreDetail {
            private Integer home;
            private Integer away;
        }
    }
}
