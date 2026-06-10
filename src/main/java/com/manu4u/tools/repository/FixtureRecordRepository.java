package com.manu4u.tools.repository;

import com.manu4u.tools.entity.FixtureRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FixtureRecordRepository extends JpaRepository<FixtureRecord, Integer> {
    List<FixtureRecord> findByMatchDate(LocalDate matchDate);
}
