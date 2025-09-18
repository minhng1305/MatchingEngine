package com.project.matchingengine.repository.order;

import com.project.matchingengine.models.order.Order;

import com.project.matchingengine.models.order.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrderRepo extends JpaRepository<Order, UUID>  {
    @Query("SELECT t FROM Trade t WHERE t.symbol = :symbol")
    List<Trade> findTradesBySymbol(@Param("symbol") String symbol);
}