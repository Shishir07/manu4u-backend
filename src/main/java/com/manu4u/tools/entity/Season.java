package com.manu4u.tools.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seasons",
       uniqueConstraints = @UniqueConstraint(name = "uq_seasons_start_year", columnNames = "start_year"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Season {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Football season start year, e.g. 2024 for the 2024/25 season */
    @Column(name = "start_year", nullable = false)
    private Integer startYear;

    /** Football season end year, e.g. 2025 */
    @Column(name = "end_year", nullable = false)
    private Integer endYear;

    /** Human-readable label, e.g. "2024-25" */
    @Column(nullable = false)
    private String label;

    /** True for the active/current season */
    private boolean current;
}
