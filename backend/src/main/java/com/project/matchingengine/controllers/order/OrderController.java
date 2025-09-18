package com.project.matchingengine.controllers.order;

import com.project.matchingengine.models.order.Stock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.service.orderbook.OrderService;



/* TODO: Implement the following functions
*  - Implement service layer for order processing, trade execution, and stock management
* */
@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("api/orders")
@Validated
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    private final ObjectMapper objectMapper;
    private OrderService orderService;
    private final String[] symbols = Arrays.stream(Stock.values())
            .map(Enum::name)
            .toArray(String[]::new);

    @Autowired
    public OrderController(ObjectMapper objectMapper, OrderService orderService)
    {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
    }

    @PostMapping("")
    public ResponseEntity<String> submitOrder(@RequestBody Order order)
    {
        try {
            // order.setOrderId(UUID.randomUUID());
            // order.setOrderTimestamp(new Timestamp(System.currentTimeMillis()));
            if (!Arrays.asList(symbols).contains(order.getSymbol())) {
                logger.info("Symbol: {} - Does NOT exist in OrderBooks map", order.getSymbol());
                return ResponseEntity.ok().body("Order symbol does not exist!");
            }
            orderService.submitOrder(order);
            ResponseEntity.ok().body("Order submitted successfully!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to submit order: " + e.getMessage());
        }
        return null;
    }

    @GetMapping("")
    public ResponseEntity<List<Order>> getAllOrders()
    {
        return ResponseEntity.ok().body(orderService.getAllOrders());
    }

    @GetMapping("{orderId}")
    public ResponseEntity<Order> getOrderById(@PathVariable UUID orderId)
    {
        return ResponseEntity.ok().body(orderService.getOrderById(orderId));
    }

    @DeleteMapping("{orderId}")
    public ResponseEntity<String> removeOrder(@PathVariable UUID orderId)
    {
        orderService.removeOrder(orderId);
        return ResponseEntity.ok().body("Deleted order successfully");
    }

}
