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

    @Query("SELECT t FROM Trade t WHERE t.buyOrderId IN (SELECT o.orderId FROM Order o WHERE o.userId = ?1) OR t.sellOrderId IN (SELECT o.orderId FROM Order o WHERE o.userId = ?2) ORDER BY t.tradeTimestamp DESC")
    List<Trade> findByBuyOrderUserIdOrSellOrderUserIdOrderByTradeTimestampDesc(UUID buyUserId, UUID sellUserId);
}