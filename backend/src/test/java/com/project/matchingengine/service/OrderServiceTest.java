package com.project.matchingengine.service;

import com.project.matchingengine.models.order.OrderSide;
import com.project.matchingengine.models.order.OrderType;
import com.project.matchingengine.repository.order.OrderRepo;
import com.project.matchingengine.service.orderbook.OrderService;
import com.project.matchingengine.models.order.Order;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;
import java.sql.Timestamp;



class OrderServiceTest {
    @Mock
    private OrderRepo orderRepo;

    @InjectMocks
    private OrderService orderService;

//    @Test
//    void shouldSubmitOrderSuccessfully() {
//        Order newOrder = new Order(UUID.randomUUID(),
//                                UUID.randomUUID(),
//                                "AAPL",
//                                150.00,
//                                10,
//                                OrderSide.BUY,
//                                OrderType.LIMIT,
//                                150.00,
//                                new Timestamp(System.currentTimeMillis()));
//        when(orderRepo.save(any(Order.class))).thenReturn(newOrder);
//
//        Order order = orderService.saveOrder();
//    }

};




