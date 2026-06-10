package com.manu4u.tools.repository;

import com.manu4u.tools.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByExternalId(Integer externalId);
}
