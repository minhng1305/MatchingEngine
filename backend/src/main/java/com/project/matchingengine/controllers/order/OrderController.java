package com.project.matchingengine.controllers.order;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.matchingengine.config.OrderBookConfig;
import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.service.orderbook.OrderService;


@RestController
@CrossOrigin(origins = "${spring.web.cors.allowed-origins}")
@RequestMapping("api/orders")
@Validated
@Profile("!ingress")
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
}
