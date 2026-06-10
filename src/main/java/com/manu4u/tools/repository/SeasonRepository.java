package com.manu4u.tools.repository;

import com.manu4u.tools.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SeasonRepository extends JpaRepository<Season, Long> {
    Optional<Season> findByStartYear(Integer startYear);
    Optional<Season> findByCurrentTrue();
}
