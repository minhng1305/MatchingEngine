package com.project.matchingengine.controllers.ingress;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.models.order.OrderSide;
import com.project.matchingengine.models.order.OrderType;
import com.project.matchingengine.service.orderbook.OrderService;


@RestController
@CrossOrigin(origins = "${spring.web.cors.allowed-origins}")
@RequestMapping("/api/orders")
@Validated
@Profile({"ingress", "production"})
public class IngressController {

    private static final Logger logger = LoggerFactory.getLogger(IngressController.class);
    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    @Autowired
    public IngressController(ObjectMapper objectMapper, OrderService orderService) {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
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

            if (symbol == null || symbol.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Symbol is required"
                ));
            }

            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "UserId is required"
                ));
            }

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
        } catch (IllegalArgumentException e) {
            logger.error("Invalid order data: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Invalid order data: " + e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error submitting order: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to submit order: " + e.getMessage()
            ));
        }
    }
}
