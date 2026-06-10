package com.manu4u.tools.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "teams",
       indexes = @Index(name = "idx_teams_external_id", columnList = "external_id"),
       uniqueConstraints = @UniqueConstraint(name = "uq_teams_external_id", columnNames = "external_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** API-Football team id, e.g. 33 (Manchester United) */
    @Column(name = "external_id", nullable = false)
    private Integer externalId;

    @Column(nullable = false)
    private String name;

    /** Short code, e.g. "MUN" */
    private String code;

    /** FK → countries.id (our surrogate id) */
    private Long countryId;

    private Integer founded;

    private String venueName;

    private String logoUrl;
}
