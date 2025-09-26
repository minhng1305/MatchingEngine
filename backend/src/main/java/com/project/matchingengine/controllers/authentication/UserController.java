package com.project.matchingengine.controllers.authentication;


import com.project.matchingengine.repository.order.OrderRepo;
import com.project.matchingengine.repository.order.TradeRepo;
import com.project.matchingengine.repository.authentication.UserRepo;
import com.project.matchingengine.models.authentication.User;
import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.models.order.Trade;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.ResponseEntity;


@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("api/user")
public class UserController
{
    private final UserRepo userRepo;
    private final OrderRepo orderRepo;
    private final TradeRepo tradeRepo;

    @Autowired
    public UserController(UserRepo userRepo, OrderRepo orderRepo, TradeRepo tradeRepo)
    {
        this.userRepo = userRepo;
        this.orderRepo = orderRepo;
        this.tradeRepo = tradeRepo;
    }

    @GetMapping("/info")
    public ResponseEntity<?> getUserDetails() {
        try {
            String userName = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Optional<User> userOpt = userRepo.findByUsername(userName);

            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            User user = userOpt.get();

            // Return user info without password
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", user.getUserId().toString());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());

            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to get user details"));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile() {
        try {
            String userName = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Optional<User> userOpt = userRepo.findByUsername(userName);

            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            User user = userOpt.get();

            // Get user's orders and trades
            List<Order> userOrders = orderRepo.findByUserIdOrderByOrderTimestampDesc(user.getUserId());
            List<Trade> userTrades = tradeRepo.findByBuyOrderUserIdOrSellOrderUserIdOrderByTradeTimestampDesc(
                    user.getUserId(), user.getUserId());

            // Calculate statistics
            long totalOrders = userOrders.size();
            long pendingOrders = userOrders.stream()
                    .filter(order -> "PENDING".equals(order.getStatus().toString()))
                    .count();
            long filledOrders = userOrders.stream()
                    .filter(order -> "FILLED".equals(order.getStatus().toString()))
                    .count();

            double totalTradeValue = userTrades.stream()
                    .mapToDouble(trade -> trade.getPrice() * trade.getQuantity())
                    .sum();

            // Build response
            Map<String, Object> profile = new HashMap<>();
            profile.put("user", Map.of(
                    "userId", user.getUserId().toString(),
                    "username", user.getUsername(),
                    "email", user.getEmail()
            ));
            profile.put("statistics", Map.of(
                    "totalOrders", totalOrders,
                    "pendingOrders", pendingOrders,
                    "filledOrders", filledOrders,
                    "totalTrades", userTrades.size(),
                    "totalTradeValue", totalTradeValue
            ));
            profile.put("recentOrders", userOrders.subList(0, Math.min(10, userOrders.size())));
            profile.put("recentTrades", userTrades.subList(0, Math.min(10, userTrades.size())));

            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to get user profile"));
        }
    }

    @GetMapping("/orders")
    public ResponseEntity<?> getUserOrders() {
        try {
            String userName = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Optional<User> userOpt = userRepo.findByUsername(userName);

            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            User user = userOpt.get();
            List<Order> orders = orderRepo.findByUserIdOrderByOrderTimestampDesc(user.getUserId());

            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to get user orders"));
        }
    }

    @GetMapping("/trades")
    public ResponseEntity<?> getUserTrades() {
        try {
            String userName = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Optional<User> userOpt = userRepo.findByUsername(userName);

            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            User user = userOpt.get();
            List<Trade> trades = tradeRepo.findByBuyOrderUserIdOrSellOrderUserIdOrderByTradeTimestampDesc(
                    user.getUserId(), user.getUserId());

            return ResponseEntity.ok(trades);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to get user trades"));
        }
    }
}

