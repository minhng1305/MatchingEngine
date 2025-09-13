package com.project.matchingengine.service.orderbook;

import com.project.matchingengine.models.order.Trade;
import com.project.matchingengine.repository.order.TradeRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
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
        logger.info("Trade: {} - Saved", trade.getTradeId());
    }

//    public Trade getTradeById(UUID tradeId)
//    {
//        Optional<Trade> optionalOrder = TradeRepo.findById(tradeId);
//        if(optionalOrder.isPresent()){
//            return optionalOrder.get();
//        }
//        logger.info("Trade: {} - Does NOT exist", tradeId);
//        return null;
//    }

    public List<Trade> getAllTrades()
    {
        return tradeRepo.findAll();
    }

    // TODO: Fetch trades by symbol logic
    public List<Trade> getTradesBySymbol(String symbol)
    {
        return null;
    }


    public void removeTrade(UUID tradeId)
    {
        tradeRepo.deleteById(tradeId);
        logger.info("Trade: {} - Removed", tradeId);
    }
}

