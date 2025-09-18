package com.project.matchingengine.controllers.order;

import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.models.order.Stock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.matchingengine.service.orderbook.TradeService;
import com.project.matchingengine.models.order.Trade;

import java.util.Arrays;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("api/stocks")
@Validated
public class StockController {
    private static final Logger logger = LoggerFactory.getLogger(StockController.class);
    private final ObjectMapper objectMapper;
    private TradeService tradeService;
    private final String[] symbols = Arrays.stream(Stock.values())
            .map(Enum::name)
            .toArray(String[]::new);

    @Autowired
    public StockController(ObjectMapper objectMapper, TradeService tradeService)
    {
        this.objectMapper = objectMapper;
        this.tradeService = tradeService;
    }

    // TODO: Return list of all stocks with their latest trade price and volume (linked to OrderBookSummary)
    @GetMapping("")
    public ResponseEntity<List<Trade>> getAllStocks()
    {
        return ResponseEntity.ok().body(tradeService.getAllTrades());
    }

    // TODO: Return stock details by symbol with its latest trade price and volume (linked to OrderBookSummary)
    @GetMapping("{symbol}")
    public ResponseEntity<List<Trade>> getStockDetails()
    {
        return ResponseEntity.ok().body(tradeService.getAllTrades());
    }

    @GetMapping("{symbol}/trades")
    public ResponseEntity<List<Trade>> getTradesBySymbol(@PathVariable String symbol)
    {
        if (!Arrays.asList(symbols).contains(symbol)) {
            logger.info("Symbol: {} - Does NOT exist in OrderBooks map", symbol);
            return ResponseEntity.badRequest().body(List.of());
        }
        return ResponseEntity.ok().body(tradeService.getTradesBySymbol(symbol));
    }




}
