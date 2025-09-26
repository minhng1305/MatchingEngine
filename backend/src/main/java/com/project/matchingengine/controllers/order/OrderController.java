package com.project.matchingengine.controllers.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.service.orderbook.OrderService;
import com.project.matchingengine.models.order.OrderSide;
import com.project.matchingengine.models.order.OrderType;
import com.project.matchingengine.config.OrderBookConfig;


@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("api/orders")
@Validated
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    private final ObjectMapper objectMapper;
    private OrderService orderService;
    private final OrderBookConfig orderBookConfig;

    @Autowired
    public OrderController(ObjectMapper objectMapper, OrderService orderService, OrderBookConfig orderBookConfig)
    {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
        this.orderBookConfig = orderBookConfig;
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitOrder(@RequestBody Map<String, Object> orderData) {
        try {
            String symbol = (String) orderData.get("symbol");
            String side = (String) orderData.get("side");
            String type = (String) orderData.get("type");
            double price = Double.parseDouble(orderData.get("price").toString());
            int quantity = Integer.parseInt(orderData.get("quantity").toString());
            String userId = (String) orderData.get("userId");

            Order order = new Order(
                    UUID.randomUUID(),
                    UUID.fromString(userId),
                    symbol,
                    price,
                    quantity,
                    OrderSide.fromString(side),
                    OrderType.fromString(type),
                    price, // limitPrice same as price for simplicity
                    new Timestamp(System.currentTimeMillis())
            );
            orderService.submitOrder(order);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderId", order.getOrderId().toString(),
                    "message", "Order submitted successfully"
            ));
        } catch (Exception e) {
            logger.error("Error submitting order: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable String orderId) {
        try {
            Order order = orderService.getOrderById(UUID.fromString(orderId));
            if (order != null) {
                return ResponseEntity.ok(order);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllOrders() {
        try {
            List<Order> orders = orderService.getAllOrders();
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<?> deleteOrder(@PathVariable String orderId) {
        try {
            orderService.removeOrder(UUID.fromString(orderId));
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Order cancelled successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
