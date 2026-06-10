package com.manu4u.tools.repository;

import com.manu4u.tools.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByExternalId(Integer externalId);
    List<Player> findByExternalIdIn(List<Integer> externalIds);
    List<Player> findByNameContainingIgnoreCase(String name);
    List<Player> findByTeamId(Long teamId);
}
