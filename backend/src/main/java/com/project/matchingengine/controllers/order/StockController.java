package com.project.matchingengine.controllers.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.annotation.Profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.matchingengine.service.orderbook.TradeService;
import com.project.matchingengine.models.order.Trade;
import com.project.matchingengine.models.order.Stock;
import com.project.matchingengine.config.OrderBookConfig;
import com.project.matchingengine.models.order.OrderBook;
import com.project.matchingengine.models.order.OrderBookSummary;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


@RestController
@CrossOrigin(origins = "${spring.web.cors.allowed-origins}")
@RequestMapping("api/stocks")
@Validated
@Profile("!ingress")
public class StockController {
    private static final Logger logger = LoggerFactory.getLogger(StockController.class);
    private final ObjectMapper objectMapper;
    private final TradeService tradeService;
    private final OrderBookConfig orderBookConfig;

    private final String[] symbols = Arrays.stream(Stock.values())
            .map(Enum::name)
            .toArray(String[]::new);

    @Autowired
    public StockController(ObjectMapper objectMapper, TradeService tradeService, OrderBookConfig orderBookConfig)
    {
        this.objectMapper = objectMapper;
        this.tradeService = tradeService;
        this.orderBookConfig = orderBookConfig;
    }

    // TODO: Return list of all stocks with their latest trade price and volume (linked to OrderBookSummary)
    @GetMapping("/all")
    public ResponseEntity<?> getAllStocks() {
        try {
            List<Map<String, Object>> stockData = Arrays.stream(Stock.values())
                    .map(stock -> {
                        OrderBook orderBook = orderBookConfig.getOrderBook(stock.name());
                        Map<String, Object> stockInfo = new HashMap<>();
                        stockInfo.put("symbol", stock.name());
                        stockInfo.put("companyName", stock.getCompanyName());
                        stockInfo.put("currentPrice", orderBook.getCurrentPrice());
                        stockInfo.put("esgScore", stock.getEsgScore());
                        return stockInfo;
                    })
                    .toList();

            return ResponseEntity.ok(stockData);
        } catch (Exception e) {
            logger.error("Error fetching all stocks: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<?> getStockDetail(@PathVariable String symbol) {
        try {
            Stock stock;
            try {
                stock = Stock.valueOf(symbol.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Invalid stock symbol: " + symbol
                ));
            }

            OrderBook orderBook = orderBookConfig.getOrderBook(symbol);
            OrderBookSummary summary = orderBook.getOrderBookSummary();

            Map<String, Object> stockDetail = new HashMap<>();
            stockDetail.put("symbol", symbol);
            stockDetail.put("companyName", stock.getCompanyName());
            stockDetail.put("esgScore", stock.getEsgScore());
            stockDetail.put("currentPrice", summary.currentPrice);
            stockDetail.put("bestBidPrice", summary.bestBidPrice);
            stockDetail.put("bestBidQuantity", summary.bestBidQuantity);
            stockDetail.put("bestAskPrice", summary.bestAskPrice);
            stockDetail.put("bestAskQuantity", summary.bestAskQuantity);
            stockDetail.put("topBuyOrders", summary.topBuys);
            stockDetail.put("topSellOrders", summary.lowestSells);
            stockDetail.put("recentTrades", summary.recentTrades);

            return ResponseEntity.ok(stockDetail);
        } catch (Exception e) {
            logger.error("Error fetching stock detail for {}: {}", symbol, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/{symbol}/trades")
    public ResponseEntity<?> getStockTrades(@PathVariable String symbol) {
        try {
            List<Trade> trades = tradeService.getTradesBySymbol(symbol);
            return ResponseEntity.ok(trades);
        } catch (Exception e) {
            logger.error("Error fetching trades for {}: {}", symbol, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/{symbol}/orderbook")
    public ResponseEntity<?> getOrderBookSummary(@PathVariable String symbol) {
        try {
            OrderBook orderBook = orderBookConfig.getOrderBook(symbol);
            OrderBookSummary summary = orderBook.getOrderBookSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Error fetching order book for {}: {}", symbol, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}
