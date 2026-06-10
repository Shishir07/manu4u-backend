package com.manu4u.tools.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "leagues",
       indexes = @Index(name = "idx_leagues_external_id", columnList = "external_id"),
       uniqueConstraints = @UniqueConstraint(name = "uq_leagues_external_id", columnNames = "external_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class League {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** API-Football league id, e.g. 39 (Premier League), 3 (Europa), 45 (FA Cup) */
    @Column(name = "external_id", nullable = false)
    private Integer externalId;

    @Column(nullable = false)
    private String name;

    /** "League" or "Cup" */
    private String type;

    /** FK → countries.id (our surrogate id) */
    private Long countryId;

    /** Season start year of the current/most recent season, e.g. 2024 */
    private Integer currentSeason;

    private String logoUrl;
}
