package com.project.matchingengine.controllers.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.ui.Model;

import java.util.UUID;
import java.sql.Timestamp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.service.orderbook.OrderService;

// TODO: Convert this to the PRG (Post/Redirect/Get) pattern with RESTful URLs
@Controller
@RequestMapping("orders")
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    private final ObjectMapper objectMapper;
    private OrderService orderService;

    @Autowired
    public OrderController(ObjectMapper objectMapper, OrderService orderService) {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
    }

    @GetMapping("orderForm")
    public String getOrderForm(Model model) {
        model.addAttribute("order", new Order());
        return "orderForm.html";
    }


    @PostMapping("submitOrder")
    public String submitOrder(@ModelAttribute @Payload Order order, Model model) {
        try {
            order.setOrderId(UUID.randomUUID());
            order.setOrderTimestamp(new Timestamp(System.currentTimeMillis()));

            System.out.println(order);
            logger.info("Received Order: {}", order.getOrderId());
            orderService.submitOrder(order);

            model.addAttribute("order", order);
            return "orderSubmitSuccess.html";
        } catch (Exception e) {
            logger.error("Failed to submit order from WebSocket to Kafka: {}", e.getMessage(), e);
            return "orderSubmitError.html";
        }
    }


}
