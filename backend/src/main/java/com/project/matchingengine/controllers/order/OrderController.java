package com.project.matchingengine.controllers.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.service.orderbook.OrderService;



/* TODO: Implement the following functions
*  - Create repositories for orders, trades, and stocks
*  - Implement service layer for order processing, trade execution, and stock management
* */
@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("api/orders")
@Validated
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    private final ObjectMapper objectMapper;
    private OrderService orderService;

    @Autowired
    public OrderController(ObjectMapper objectMapper, OrderService orderService)
    {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
    }

    @PostMapping("submitOrder")
    public ResponseEntity<String> submitOrder(@RequestBody Order order)
    {
        try {
            // order.setOrderId(UUID.randomUUID());
            // order.setOrderTimestamp(new Timestamp(System.currentTimeMillis()));
            orderService.submitOrder(order);
            ResponseEntity.ok().body("Order submitted successfully!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to submit order: " + e.getMessage());
        }
        return null;
    }

    @PostMapping("")
    public ResponseEntity<String> saveOrder(@RequestBody Order order)
    {
        orderService.saveOrder(order);
        return ResponseEntity.ok().body("Saving done!");
    }

    @GetMapping("")
    public ResponseEntity<List<Order>> getAllOrders()
    {
        return ResponseEntity.ok().body(orderService.getAllOrders());
    }

    @GetMapping("{orderId}")
    public ResponseEntity<Order> getOrderById(@PathVariable UUID orderId)
    {
        return ResponseEntity.ok().body(orderService.getOrderById(orderId));
    }

    @DeleteMapping("{orderId}")
    public ResponseEntity<String> removeOrder(@PathVariable UUID orderId)
    {
        logger.info("Remove API called");
        orderService.removeOrder(orderId);
        return ResponseEntity.ok().body("Deleted order successfully");
    }





//    @GetMapping("/stocks")
//    public ResponseEntity<List<StockDTO>> getAvailableStocks() {
//        try {
//            // Replace this with actual logic to fetch available stocks
//            List<StockDTO> stocks = orderService.getAvailableStocks();
//            return ResponseEntity.ok(stocks);
//        } catch (Exception e) {
//            logger.error("Failed to fetch available stocks: {}", e.getMessage(), e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//
//    /**
//     * Get details for a specific stock
//     */
//    @GetMapping("/stocks/{symbol}")
//    public ResponseEntity<StockDTO> getStockDetails(@PathVariable String symbol) {
//        try {
//            StockDTO stock = orderService.getStockDetails(symbol);
//            if (stock != null) {
//                return ResponseEntity.ok(stock);
//            } else {
//                return ResponseEntity.notFound().build();
//            }
//        } catch (Exception e) {
//            logger.error("Failed to fetch stock details for {}: {}", symbol, e.getMessage(), e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//
//    /**
//     * Get recent trades for a specific stock
//     */
//    @GetMapping("/stocks/{symbol}/trades")
//    public ResponseEntity<List<TradeDTO>> getStockTrades(@PathVariable String symbol) {
//        try {
//            List<TradeDTO> trades = orderService.getRecentTrades(symbol);
//            return ResponseEntity.ok(trades);
//        } catch (Exception e) {
//            logger.error("Failed to fetch trades for {}: {}", symbol, e.getMessage(), e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//
//    /**
//     * Submit a new order
//     */
//    @PostMapping("/orders")
//    public ResponseEntity<?> submitOrder(@RequestBody OrderDTO orderRequest) {
//        try {
//            // Convert DTO to entity and set generated values
//            Order order = new Order();
//            order.setOrderId(UUID.randomUUID());
//            order.setOrderTimestamp(new Timestamp(System.currentTimeMillis()));
//            order.setUserId(orderRequest.getUserId());
//            order.setSymbol(orderRequest.getSymbol());
//            order.setOrderType(orderRequest.getOrderType());
//            order.setSide(orderRequest.getSide());
//            order.setPrice(orderRequest.getPrice());
//            order.setLimitPrice(orderRequest.getLimitPrice());
//            order.setQuantity(orderRequest.getQuantity());
//            order.setStatus("PENDING");
//
//            logger.info("Received Order: {}", order.getOrderId());
//
//            // Process the order
//            Order submittedOrder = orderService.submitOrder(order);
//
//            // Convert back to DTO for response
//            OrderResponseDTO response = new OrderResponseDTO(
//                    submittedOrder.getOrderId().toString(),
//                    submittedOrder.getUserId(),
//                    submittedOrder.getSymbol(),
//                    submittedOrder.getOrderType(),
//                    submittedOrder.getSide(),
//                    submittedOrder.getPrice(),
//                    submittedOrder.getLimitPrice(),
//                    submittedOrder.getQuantity(),
//                    submittedOrder.getStatus(),
//                    submittedOrder.getOrderTimestamp().toString(),
//                    generateConfirmationNumber()
//            );
//
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            logger.error("Failed to submit order: {}", e.getMessage(), e);
//
//            ErrorResponseDTO errorResponse = new ErrorResponseDTO(
//                    "ERR-5102",
//                    e.getMessage(),
//                    "Failed to process your order"
//            );
//
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
//        }
//    }
//
//    /**
//     * Get all orders for the user
//     */
//    @GetMapping("/orders")
//    public ResponseEntity<List<OrderResponseDTO>> getUserOrders() {
//        try {
//            // In a real application, you would get the user ID from authentication
//            // For now, we'll just fetch all orders
//            List<Order> orders = orderService.getAllOrders();
//
//            List<OrderResponseDTO> orderDTOs = orders.stream()
//                    .map(order -> new OrderResponseDTO(
//                            order.getOrderId().toString(),
//                            order.getUserId(),
//                            order.getSymbol(),
//                            order.getOrderType(),
//                            order.getSide(),
//                            order.getPrice(),
//                            order.getLimitPrice(),
//                            order.getQuantity(),
//                            order.getStatus(),
//                            order.getOrderTimestamp().toString(),
//                            generateConfirmationNumber()
//                    ))
//                    .collect(Collectors.toList());
//
//            return ResponseEntity.ok(orderDTOs);
//        } catch (Exception e) {
//            logger.error("Failed to fetch orders: {}", e.getMessage(), e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//
//    /**
//     * Get details for a specific order
//     */
//    @GetMapping("/orders/{orderId}")
//    public ResponseEntity<OrderResponseDTO> getOrderDetails(@PathVariable String orderId) {
//        try {
//            Order order = orderService.getOrderById(UUID.fromString(orderId));
//
//            if (order == null) {
//                return ResponseEntity.notFound().build();
//            }
//
//            OrderResponseDTO orderDTO = new OrderResponseDTO(
//                    order.getOrderId().toString(),
//                    order.getUserId(),
//                    order.getSymbol(),
//                    order.getOrderType(),
//                    order.getSide(),
//                    order.getPrice(),
//                    order.getLimitPrice(),
//                    order.getQuantity(),
//                    order.getStatus(),
//                    order.getOrderTimestamp().toString(),
//                    generateConfirmationNumber()
//            );
//
//            return ResponseEntity.ok(orderDTO);
//        } catch (Exception e) {
//            logger.error("Failed to fetch order details for {}: {}", orderId, e.getMessage(), e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//
//    // Helper method to generate a confirmation number
//    private String generateConfirmationNumber() {
//        return "CONF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
//    }
//
//    @GetMapping("orderForm")
//    public String getOrderForm(Model model) {
//        model.addAttribute("order", new Order());
//        return "orderForm.html";
//    }
}
