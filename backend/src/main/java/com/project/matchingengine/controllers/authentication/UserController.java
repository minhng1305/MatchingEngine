package com.project.matchingengine.controllers.authentication;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.matchingengine.models.authentication.User;
import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.models.order.Trade;
import com.project.matchingengine.repository.authentication.UserRepo;
import com.project.matchingengine.repository.order.OrderRepo;
import com.project.matchingengine.repository.order.TradeRepo;
import com.project.matchingengine.service.authentication.UserDetailsCacheService;


@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("api/user")
public class UserController
{
    private final UserRepo userRepo;
    private final OrderRepo orderRepo;
    private final TradeRepo tradeRepo;
    private final UserDetailsCacheService userDetailsCacheService;

    @Autowired
    public UserController(UserRepo userRepo, OrderRepo orderRepo, TradeRepo tradeRepo, UserDetailsCacheService userDetailsCacheService)
    {
        this.userRepo = userRepo;
        this.orderRepo = orderRepo;
        this.tradeRepo = tradeRepo;
        this.userDetailsCacheService = userDetailsCacheService;
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

            // Get account balance and holdings from cache (real-time data)
            UserDetailsCacheService.CachedUserDetails cachedDetails = userDetailsCacheService.getBalance(user.getUserId());

            // Return user info without password, with balance from cache
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", user.getUserId().toString());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("ledgerBalance", cachedDetails.ledgerBalance);
            userInfo.put("availableBalance", cachedDetails.availableBalance);
            userInfo.put("holdings", cachedDetails.holdings);

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

            // Get account balance and holdings from cache (real-time data)
            UserDetailsCacheService.CachedUserDetails cachedDetails = userDetailsCacheService.getBalance(user.getUserId());

            // Get user's orders and trades
            List<Order> userOrders = orderRepo.findByUserIdOrderByOrderTimestampDesc(user.getUserId());
            List<Trade> userTrades = tradeRepo.findByBuyerUserIdOrSellerUserIdOrderByTradeTimestampDesc(user.getUserId(), user.getUserId());

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

            // Build response with account info from cache
            Map<String, Object> profile = new HashMap<>();
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", user.getUserId().toString());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("ledgerBalance", cachedDetails.ledgerBalance);
            userInfo.put("availableBalance", cachedDetails.availableBalance);
            profile.put("user", userInfo);
            
            // Add account and holdings
            Map<String, Object> account = new HashMap<>();
            account.put("ledgerBalance", cachedDetails.ledgerBalance);
            account.put("availableBalance", cachedDetails.availableBalance);
            account.put("holdings", cachedDetails.holdings);
            profile.put("account", account);
            
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
            List<Trade> trades = tradeRepo.findByBuyerUserIdOrSellerUserIdOrderByTradeTimestampDesc(user.getUserId(), user.getUserId());

            return ResponseEntity.ok(trades);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to get user trades"));
        }
    }

//    // TODO: Verify this endpoint for updating user balance
//    @PostMapping("/add-balance") // More descriptive endpoint name
//    public ResponseEntity<?> addBalance(@RequestBody Map<String, Double> request) {
//        try {
//            String userName = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//            User user = userRepo.findByUsername(userName)
//                    .orElseThrow(() -> new RuntimeException("User not found"));
//
//            Double amount = request.get("amount");
//            if (amount == null || amount <= 0) {
//                return ResponseEntity.badRequest().body(Map.of("error", "Invalid amount specified."));
//            }
//
//            User updatedUser = customedUserDetailsService.addFunds(user.getUserId(), amount);
//
//            return ResponseEntity.ok(Map.of(
//                    "success", true,
//                    "message", "Balance updated successfully.",
//                    "newLedgerBalance", updatedUser.getLedgerBalance(),
//                    "newAvailableBalance", updatedUser.getAvailableBalance()
//            ));
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(Map.of("error", "Failed to add balance: " + e.getMessage()));
//        }
//    }

    // TODO: Verify this endpoint for updating user balance
//    @PostMapping("/add-balance")
//    public ResponseEntity<Map<String, Object>> addBalance(@RequestBody Map<String, Object> request) {
//        try {
//            // Check if userId is provided in payload (for Admin/Test use)
//            // If not, fall back to currently logged-in user
//            String userIdStr = (String) request.get("userId");
//            User user;
//
//            if (userIdStr != null) {
//                user = customedUserDetailsService.findUserById(UUID.fromString(userIdStr));
//            } else {
//                String userName = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//                user = userRepo.findByUsername(userName).orElseThrow(() -> new RuntimeException("User not found"));
//            }
//
//            // Handle both Integer and Double from JSON
//            Double amount;
//            Object amountObj = request.get("amount");
//            if (amountObj instanceof Integer) {
//                amount = ((Integer) amountObj).doubleValue();
//            } else {
//                amount = (Double) amountObj;
//            }
//
//            if (amount == null || amount <= 0) {
//                return ResponseEntity.badRequest().body(Map.of("error", "Invalid amount specified."));
//            }
//
//            User updatedUser = customedUserDetailsService.addFunds(user.getUserId(), amount);
//
//            return ResponseEntity.ok(Map.of(
//                    "success", true,
//                    "message", "Balance updated successfully.",
//                    "newLedgerBalance", updatedUser.getLedgerBalance(),
//                    "newAvailableBalance", updatedUser.getAvailableBalance()
//            ));
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(Map.of("error", "Failed to add balance: " + e.getMessage()));
//        }
//    }
}

