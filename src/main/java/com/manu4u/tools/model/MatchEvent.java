package com.manu4u.tools.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchEvent {
    private Integer minute;
    private String type;
    private Integer teamId;
    private String playerName;
    private String assistName;
    private String detail;
}
