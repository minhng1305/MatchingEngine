package com.project.matchingengine.controllers.order;

import com.project.matchingengine.config.OrderBookConfig;
import com.project.matchingengine.models.order.OrderBook;
import com.project.matchingengine.models.order.Stock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("api/prices")
public class PriceController {

    private final OrderBookConfig orderBookConfig;

    @Autowired
    public PriceController(OrderBookConfig orderBookConfig) {
        this.orderBookConfig = orderBookConfig;
    }

    @GetMapping("/current/{symbol}")
    public ResponseEntity<?> getCurrentPrice(@PathVariable String symbol) {
        try {
            OrderBook orderBook = orderBookConfig.getOrderBook(symbol);
            double currentPrice = orderBook.getCurrentPrice();

            return ResponseEntity.ok(Map.of(
                    "symbol", symbol,
                    "currentPrice", currentPrice,
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllPrices() {
        try {
            Map<String, Double> prices = new HashMap<>();

            for (Stock stock : Stock.values()) {
                OrderBook orderBook = orderBookConfig.getOrderBook(stock.name());
                prices.put(stock.name(), orderBook.getCurrentPrice());
            }

            return ResponseEntity.ok(Map.of(
                    "prices", prices,
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}
