package com.project.matchingengine.service.orderbook;

import com.project.matchingengine.models.order.OrderSide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;

import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.service.kafka.KafkaProducer;
import com.project.matchingengine.repository.order.OrderRepo;
import com.project.matchingengine.repository.authentication.UserRepo;
import com.project.matchingengine.service.user.UserDetailsCacheService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private final KafkaProducer kafkaProducer;
    private final UserDetailsCacheService userDetailsCacheService;
    private final OrderRepo orderRepo;
    private final UserRepo userRepo;

    @Autowired
    public OrderService(KafkaProducer kafkaProducer,
                        OrderRepo orderRepo,
                        UserRepo userRepo,
                        UserDetailsCacheService userDetailsCacheService)
    {
        this.kafkaProducer = kafkaProducer;
        this.orderRepo = orderRepo;
        this.userRepo = userRepo;
        this.userDetailsCacheService = userDetailsCacheService;
    }

    // TODO: Verify this endpoint logic
    public void submitOrder(@Payload Order order)
    {
        if (order.getSide() == OrderSide.BUY) {
            userDetailsCacheService.placeOrder(order.getUserId(), order.getSymbol(), order.getCurrentQuantity(), order.getPrice(), true);
        }
        else if (order.getSide() == OrderSide.SELL) {
            userDetailsCacheService.placeOrder(order.getUserId(), order.getSymbol(), order.getCurrentQuantity(), order.getPrice(), false);
        }
        kafkaProducer.sendOrder(order);
        logger.info("Order {} submitted for user {}. Funds held if BUY.", order.getOrderId(), order.getUserId());
    }


    // TODO: Update such that it pulled data from cache rather than DB
    public Order getOrderById(UUID orderId)
    {
        Optional<Order> optionalOrder = orderRepo.findById(orderId);
        if(optionalOrder.isPresent()){
            return optionalOrder.get();
        }
        logger.info("Order: {} - Does NOT exist", orderId);
        return null;
    }


    // TODO: Update such that it pulled data from cache rather than DB
    public List<Order> getAllOrders()
    {
        return orderRepo.findAll();
    }


    // TODO: Update such that it pulled data from cache rather than DB
    public List<Order> getOrdersBySymbol(String symbol)
    {
        return null;
    }


    // TODO: Update such that it pulled data from cache rather than DB
    public void updateOrder(Order order)
    {
        orderRepo.save(order);
        logger.info("Order: {} - Updated successfully", order.getOrderId());
    }
}
