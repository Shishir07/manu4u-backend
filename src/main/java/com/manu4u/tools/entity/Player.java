package com.manu4u.tools.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "players",
       indexes = {
           @Index(name = "idx_players_name",        columnList = "name"),
           @Index(name = "idx_players_external_id", columnList = "external_id"),
           @Index(name = "idx_players_team_id",     columnList = "team_id")
       },
       uniqueConstraints = @UniqueConstraint(name = "uq_players_external_id", columnNames = "external_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** API-Football player id — globally unique per player */
    @Column(name = "external_id", nullable = false)
    private Integer externalId;

    /** Display name as returned by API, e.g. "M. Rashford" */
    @Column(nullable = false)
    private String name;

    private String firstName;
    private String lastName;
    private String nationality;

    /** "Attacker", "Midfielder", "Defender", "Goalkeeper" */
    private String position;

    private Integer jerseyNumber;
    private Integer age;

    /** FK → teams.id (our surrogate id) */
    @Column(name = "team_id")
    private Long teamId;

    private Instant syncedAt;
}
