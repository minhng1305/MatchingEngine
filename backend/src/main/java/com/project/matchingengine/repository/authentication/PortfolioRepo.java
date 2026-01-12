package com.project.matchingengine.repository.authentication;

import com.project.matchingengine.models.authentication.Portfolio;
import com.project.matchingengine.models.authentication.PortfolioId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface PortfolioRepo extends JpaRepository<Portfolio, PortfolioId>
{
    @Query("SELECT p FROM Portfolio p WHERE p.id.userId = :userId AND p.id.symbol = :symbol")
    Optional<Portfolio> findByUserIdAndSymbol(@Param("userId") UUID userId, @Param("symbol") String symbol);

    @Query("SELECT p FROM Portfolio p WHERE p.id.userId = :userId")
    List<Portfolio> findByUserId(@Param("userId") UUID userId);
}
