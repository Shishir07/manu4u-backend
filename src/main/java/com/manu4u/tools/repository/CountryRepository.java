package com.manu4u.tools.repository;

import com.manu4u.tools.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CountryRepository extends JpaRepository<Country, Long> {
    Optional<Country> findByExternalCode(String externalCode);
    Optional<Country> findByName(String name);
}
