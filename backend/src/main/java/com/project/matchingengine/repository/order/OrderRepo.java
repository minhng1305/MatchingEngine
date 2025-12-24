package com.project.matchingengine.repository.order;

import com.project.matchingengine.models.order.Order;

import com.project.matchingengine.models.order.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;


@Repository
public interface OrderRepo extends JpaRepository<Order, UUID>  {
    @Query("SELECT t FROM Trade t WHERE t.symbol = :symbol")
    List<Trade> findTradesBySymbol(@Param("symbol") String symbol);

    List<Order> findByUserIdOrderByOrderTimestampDesc(UUID userId);

    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.symbol = :symbol")
    List<Order> findOrdersByUserIdAndSymbol(UUID userId, String symbol);
}