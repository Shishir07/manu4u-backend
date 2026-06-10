package com.manu4u.tools.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "countries",
       uniqueConstraints = @UniqueConstraint(name = "uq_countries_external_code", columnNames = "external_code"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** API-Football country code / name key, e.g. "ENG", "World" */
    @Column(name = "external_code", nullable = false)
    private String externalCode;

    /** Human-readable name, e.g. "England", "World" */
    @Column(nullable = false)
    private String name;

    private String flagUrl;
}
