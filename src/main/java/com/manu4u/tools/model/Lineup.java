package com.manu4u.tools.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Lineup {
    private Integer teamId;
    private String teamName;
    private String formation;
    private List<Player> starters;
    private List<Player> substitutes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Player {
        private Integer id;
        private String name;
        private Integer number;
        /** Short position code from API: GK, D, M, F */
        private String position;
        /** Grid coordinate, e.g. "1:1" for goalkeeper position */
        private String grid;
    }
}
