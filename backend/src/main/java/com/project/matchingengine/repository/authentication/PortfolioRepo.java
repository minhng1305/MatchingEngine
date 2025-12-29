package com.project.matchingengine.repository.authentication;

import com.project.matchingengine.models.authentication.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;


public interface PortfolioRepo extends JpaRepository<Portfolio, UUID>
{
    @Query("SELECT p FROM Portfolio p WHERE p.userId = :userId AND p.symbol = :symbol")
    Optional<Portfolio> findByUserIdAndSymbol(@Param("userId") UUID userId, @Param("symbol") String symbol);

    @Query("SELECT p FROM Portfolio p WHERE p.userId = :userId")
    Optional<Portfolio> findByUserId(UUID userId);
}
