//package com.project.matchingengine.service.orderbook;
//
//import com.project.matchingengine.controllers.order.OrderSubmit;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Service;
//import org.springframework.beans.factory.annotation.Autowired;
//import com.project.matchingengine.models.order.Order;
//import com.project.matchingengine.service.kafka.KafkaProducer;
//
//@Service
//public class OrderService {
//    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
//    private final OrderBook orderBook;
//    private final KafkaProducer kafkaProducer;
//
//    @Autowired
//    public OrderService(OrderBook orderBook, KafkaProducer kafkaProducer) {
//        this.orderBook = orderBook;
//        this.kafkaProducer = kafkaProducer;
//    }
//
//    public void submitOrder(Order order) {
//        kafkaProducer.sendOrder(order);
//        logger.info("Order submitted: {}", order.getOrderId());
//    }
//
////    public Order saveOrder(Order order) {
////        return orderRepository.save(order);
////    }
//
//    public Order getOrderById(UUID orderId) {
//        return orderRepository.findById(orderId)
//                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));
//    }
//}
