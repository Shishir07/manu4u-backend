package com.manu4u.tools.repository;

import com.manu4u.tools.entity.PlayerStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlayerStatisticsRepository extends JpaRepository<PlayerStatistics, Long> {

    Optional<PlayerStatistics> findByPlayerIdAndSeasonIdAndLeagueId(
            Long playerId, Long seasonId, Long leagueId);

    List<PlayerStatistics> findByPlayerIdAndSeasonId(Long playerId, Long seasonId);

    List<PlayerStatistics> findByPlayerId(Long playerId);

    /** Aggregate totals across all leagues for a player in a season */
    @Query("""
           SELECT COALESCE(SUM(ps.goals), 0),
                  COALESCE(SUM(ps.assists), 0),
                  COALESCE(SUM(ps.appearances), 0)
           FROM PlayerStatistics ps
           WHERE ps.playerId = :playerId AND ps.seasonId = :seasonId
           """)
    List<Object[]> aggregateByPlayerAndSeason(
            @Param("playerId") Long playerId,
            @Param("seasonId") Long seasonId);
}
