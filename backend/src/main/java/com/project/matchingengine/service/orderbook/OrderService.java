package com.project.matchingengine.service.orderbook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.service.kafka.KafkaProducer;
import com.project.matchingengine.repository.order.OrderRepo;
import org.springframework.messaging.handler.annotation.Payload;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private final KafkaProducer kafkaProducer;
    private OrderRepo orderRepo;

    @Autowired
    public OrderService(KafkaProducer kafkaProducer, OrderRepo orderRepo)
    {
        this.kafkaProducer = kafkaProducer;
        this.orderRepo = orderRepo;
    }

    public void submitOrder(@Payload Order order)
    {
        kafkaProducer.sendOrder(order);
        logger.info("Order: {} - Submitted to Kafka", order.getOrderId());
        saveOrder(order);
    }

    public void saveOrder(Order order)
    {
        orderRepo.save(order);
        logger.info("Order: {} - Saved", order.getOrderId());
    }

    public Order getOrderById(UUID orderId)
    {
        Optional<Order> optionalOrder = orderRepo.findById(orderId);
        if(optionalOrder.isPresent()){
            return optionalOrder.get();
        }
        logger.info("Order: {} - Does NOT exist", orderId);
        return null;
    }

    public List<Order> getAllOrders()
    {
        return orderRepo.findAll();
    }

    // TODO: Fetch orders by symbol logic
    public List<Order> getOrdersBySymbol(String symbol)
    {
        return null;
    }

    public void updateOrder(Order order)
    {
        orderRepo.save(order);
        logger.info("Order: {} - Updated successfully", order.getOrderId());
    }

    public void removeOrder(UUID orderId)
    {
        orderRepo.deleteById(orderId);
        logger.info("Order: {} - Removed", orderId);
    }
}
