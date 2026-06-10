package com.manu4u.tools.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FixtureSummary {
    private Integer id;
    private Instant utcKickoff;
    private String status;
    private String homeTeam;
    private String awayTeam;
}
