package com.project.matchingengine.service.orderbook;

import com.project.matchingengine.models.order.Trade;
import com.project.matchingengine.repository.order.TradeRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TradeService {
    private static final Logger logger = LoggerFactory.getLogger(TradeService.class);
    private TradeRepo tradeRepo;

    @Autowired
    public TradeService(TradeRepo tradeRepo)
    {
        this.tradeRepo = tradeRepo;
    }

    public void saveTrade(Trade trade)
    {
        tradeRepo.save(trade);
    }

    public List<Trade> getAllTrades()
    {
        return tradeRepo.findAll();
    }

    // TODO: Fetch trades by symbol logic
    public List<Trade> getTradesBySymbol(String symbol)
    {
        try {
            List<Trade> trades = tradeRepo.findTradesBySymbol(symbol);
            logger.info("Found {} trades for symbol: {}", trades.size(), symbol);
            return trades;
        } catch (Exception e) {
            logger.error("Error fetching trades for symbol {}: {}", symbol, e.getMessage());
            return List.of();
        }
    }


    public void removeTrade(UUID tradeId)
    {
        tradeRepo.deleteById(tradeId);
        logger.info("Trade: {} - Removed", tradeId);
    }
}

