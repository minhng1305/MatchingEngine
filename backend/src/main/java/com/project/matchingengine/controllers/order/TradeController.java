package com.project.matchingengine.controllers.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.matchingengine.service.orderbook.TradeService;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("api/trades")
@Validated
public class TradeController {
    private static final Logger logger = LoggerFactory.getLogger(TradeController.class);
    private final ObjectMapper objectMapper;
    private TradeService tradeService;

    @Autowired
    public TradeController(ObjectMapper objectMapper, TradeService tradeService)
    {
        this.objectMapper = objectMapper;
        this.tradeService = tradeService;
    }


}
