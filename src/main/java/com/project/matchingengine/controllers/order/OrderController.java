//package com.project.matchingengine.controllers.order;
//
//import java.sql.Timestamp;
//import java.util.UUID;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.messaging.handler.annotation.Payload;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.ModelAttribute;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//import com.project.matchingengine.models.order.Order;
//import com.project.matchingengine.models.order.OrderStatus;
//import com.project.matchingengine.service.kafka.KafkaProducer;
//import com.project.matchingengine.service.orderbook.OrderService;
//
//
//@Controller
//@RequestMapping("/orders")
//public class OrderController {
//    private static final Logger logger = LoggerFactory.getLogger(OrderSubmit.class);
//    private final ObjectMapper objectMapper;
//    private final OrderService orderService;
//
//    @Autowired
//    public OrderController(ObjectMapper objectMapper, OrderService orderService) {
//        this.objectMapper = objectMapper;
//        this.orderService = orderService;
//    }
//
//    @GetMapping("orderForm")
//    public String submitOrder(Model model) {
//        model.addAttribute("order", new Order());
//        return "orderForm.html";
//    }
//
//    @PostMapping("submitOrder")
//    public String handleSubmitedOrder(@ModelAttribute @Payload Order order, Model model) {
//        try {
//            order.setOrderId(UUID.randomUUID());
//            order.setOrderTimestamp(new Timestamp(System.currentTimeMillis()));
//
//            System.out.println(order);
//            logger.info("Received Order: {}. Sending to Kafka...", order.getOrderId());
//            kafkaProducer.sendOrder(order);
//            logger.info("=> Sent Order to Kafka: {}", order.getOrderId());
//
//            model.addAttribute("order", order);
//            return "orderSubmitSuccess.html";
//        } catch (Exception e) {
//            logger.error("Failed to submit order from WebSocket to Kafka: {}", e.getMessage(), e);
//            return "orderSubmitErrror.html";
//        }
//    }
//}