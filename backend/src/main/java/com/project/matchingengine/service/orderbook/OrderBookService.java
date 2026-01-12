package com.project.matchingengine.service.orderbook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.project.matchingengine.models.order.OrderBook;
import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.repository.order.TradeRepo;
import com.project.matchingengine.repository.order.OrderRepo;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;


@Service
public class OrderBookService
{
    private final Logger logger = LoggerFactory.getLogger(OrderBookService.class);
    private final Map<String, OrderBook> orderBooks;
    private final TradeRepo tradeRepo;
    private final OrderRepo orderRepo;

    @Autowired
    public OrderBookService(
            Map<String, OrderBook> orderBooks,
            TradeRepo tradeRepo,
            OrderRepo orderRepo)
    {
        this.orderBooks = orderBooks;
        this.tradeRepo = tradeRepo;
        this.orderRepo = orderRepo;
    }

    @Scheduled(fixedRate = 5000)
    public void updateTradeTable() {
        try {
            List<Order> allOrdersToUpdate = new ArrayList<>();
            
            for (OrderBook orderBook : orderBooks.values()) {
                // Save trades first (this ensures orders already exist in DB due to fix in KafkaConsumer)
                if (!orderBook.getTrades().isEmpty()) {
                    tradeRepo.saveAll(orderBook.getTrades());
                    logger.debug("Saved {} trades for symbol: {}", orderBook.getTrades().size(), orderBook.getSymbol());
                }
                
                // Collect orders that need updating (active and filled orders)
                allOrdersToUpdate.addAll(orderBook.getAllOrdersToUpdate());
                
                orderBook.updateOrderBookSummary();
                orderBook.clearTradeRecords();
                logger.debug("Processed order book for symbol: {}", orderBook.getSymbol());
            }
            
            // Batch update all orders (remove duplicates by orderId)
            if (!allOrdersToUpdate.isEmpty()) {
                List<Order> uniqueOrders = allOrdersToUpdate.stream()
                    .collect(Collectors.toMap(
                        Order::getOrderId,
                        order -> order,
                        (existing, replacement) -> existing  // Keep first occurrence
                    ))
                    .values()
                    .stream()
                    .collect(Collectors.toList());
                    
                orderRepo.saveAll(uniqueOrders);
                logger.info("Updated {} order records", uniqueOrders.size());
            }
            
            logger.info("Trade and order records updated successfully");
        } catch (Exception e) {
            logger.error("Error scheduled updating trade and order records: {}", e.getMessage(), e);
        }
    }

}
