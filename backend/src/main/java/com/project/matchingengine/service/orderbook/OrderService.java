package com.project.matchingengine.service.orderbook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.transaction.annotation.Transactional;

import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.service.kafka.KafkaProducer;
import com.project.matchingengine.repository.order.OrderRepo;
import com.project.matchingengine.repository.authentication.UserRepo;
import com.project.matchingengine.service.authentication.CustomedUserDetailsService;
import com.project.matchingengine.models.order.OrderSide;
import com.project.matchingengine.models.order.OrderStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private final KafkaProducer kafkaProducer;
    private OrderRepo orderRepo;
    private final UserRepo userRepo;
    private final CustomedUserDetailsService userService;

    @Autowired
    public OrderService(KafkaProducer kafkaProducer, OrderRepo orderRepo, UserRepo userRepo, CustomedUserDetailsService userService)
    {
        this.kafkaProducer = kafkaProducer;
        this.orderRepo = orderRepo;
        this.userRepo = userRepo;
        this.userService = userService;
    }

    // TODO: Verify this endpoint logic
    @Transactional
    public void submitOrder(@Payload Order order)
    {
        if (order.getSide() == OrderSide.BUY) {
            double requiredFunds = order.getPrice() * order.getOriginalQuantity();
            boolean fundsHeld = userService.placeHoldOnFunds(order.getUserId(), requiredFunds);
            if (!fundsHeld) {
                throw new RuntimeException("Insufficient available balance to place buy order.");
            }
        }

        // TODO: Enable holdings check for SELL orders
        else if (order.getSide() == OrderSide.SELL) {
            boolean holdingsHeld = userService.placeHoldOnHoldings(order.getUserId(), order.getSymbol(), order.getOriginalQuantity());
            if (!holdingsHeld) { // Not enough available holdings, reject the order
                throw new RuntimeException("Insufficient available holdings to place sell order.");
            }
        }

        saveOrder(order);
        kafkaProducer.sendOrder(order);
        logger.info("Order {} submitted for user {}. Funds held if BUY.", order.getOrderId(), order.getUserId());
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

//    /* TODO: Remove order logic
//        1. If order is in PENDING status, remove it from the order book and set its status to CANCELLED.
//        2. If order is in PARTIALLY_FILLED status, remove it from the order book and set its status to CANCELLED.
//        3. If order is in FILLED or CANCELLED status, do nothing.
//     */
//    @Transactional
//    public void cancelOrder(UUID orderId, UUID userId) {
//        Order order = orderRepo.findById(orderId)
//                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
//
//        if (!order.getUserId().equals(userId)) {
//            throw new SecurityException("User not authorized to cancel this order.");
//        }
//
//        if (order.getStatus() == OrderStatus.FILLED || order.getStatus() == OrderStatus.CANCELED) {
//            throw new IllegalStateException("Cannot cancel an order that is already filled or cancelled.");
//        }
//
//        // --- BALANCE LOGIC for CANCELLATION ---
//        if (order.getSide() == OrderSide.BUY && order.getCurrentQuantity() > 0) {
//            // Release the hold on funds for the remaining quantity of the buy order.
//            double amountToRelease = order.getPrice() * order.getCurrentQuantity();
//            userService.releaseHoldOnFunds(order.getUserId(), amountToRelease);
//            logger.info("Released hold of {} for cancelled order {}", amountToRelease, orderId);
//        }
//
//        order.setStatus(OrderStatus.CANCELED);
//        updateOrder(order);
//
//        // Note: You also need a mechanism to remove the order from the in-memory OrderBook.
//        // This could be another Kafka message (e.g., to a 'cancellations' topic) or a direct call
//        // if the architecture allows. For now, we've handled the DB and balance part.
//    }
}
