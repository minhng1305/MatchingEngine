package com.project.matchingengine.repository.order;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.matchingengine.models.order.Trade;

import java.util.UUID;


public interface TradeRepo extends JpaRepository<Trade, UUID> {

}