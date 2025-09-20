package com.project.matchingengine.repository.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.matchingengine.models.order.Trade;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;


@Repository
public interface TradeRepo extends JpaRepository<Trade, UUID> {

    @Query("SELECT t FROM Trade t WHERE t.symbol = :symbol")
    List<Trade> findTradesBySymbol(@Param("symbol") String symbol);
}