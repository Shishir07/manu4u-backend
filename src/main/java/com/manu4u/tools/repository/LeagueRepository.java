package com.manu4u.tools.repository;

import com.manu4u.tools.entity.League;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeagueRepository extends JpaRepository<League, Long> {
    Optional<League> findByExternalId(Integer externalId);
    List<League> findByCountryId(Long countryId);
}
