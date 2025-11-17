package com.project.matchingengine.service.authentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import com.project.matchingengine.repository.authentication.UserRepo;
import com.project.matchingengine.models.authentication.User;

@Service
public class CustomedUserDetailsService implements UserDetailsService
{
    private UserRepo userRepo;

    @Autowired
    public CustomedUserDetailsService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> userRes = userRepo.findByUsername(username);

        if(userRes.isEmpty())
            throw new UsernameNotFoundException("No user found with this username " + username);
        User user = userRes.get();
        return new
                org.springframework.security.core.userdetails.User(
                username,
                user.getPassword(),
                Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_USER")
                )
        );
    }

    public User getUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> userRes = userRepo.findByUsername(username);

        if(userRes.isEmpty())
            throw new UsernameNotFoundException("No user found with this username " + username);
        return userRes.get();
    }

    public User findUserById(UUID userId) {
        return userRepo.findById(userId.toString()).orElseThrow(() -> new RuntimeException("User not found: " + userId.toString()));
    }

    // TODO: Verify this method
    @Transactional
    public User addFunds(UUID userId, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }
        User user = findUserById(userId);
        user.setLedgerBalance(user.getLedgerBalance() + amount);
        user.setAvailableBalance(user.getAvailableBalance() + amount);
        return userRepo.save(user);
    }

    // TODO: Verify this method
    @Transactional
    public boolean placeHoldOnFunds(UUID userId, double amount) {
        User user = findUserById(userId);
        if (user.getAvailableBalance() >= amount) {
            user.setAvailableBalance(user.getAvailableBalance() - amount);
            userRepo.save(user);
            return true;
        }
        return false;
    }

    // TODO: Verify this method
    @Transactional
    public void releaseHoldOnFunds(UUID userId, double amount) {
        User user = findUserById(userId);
        user.setAvailableBalance(user.getAvailableBalance() + amount);
        userRepo.save(user);
    }

    // TODO: Verify this method
    @Transactional
    public void settleTrade(UUID buyerId, UUID sellerId, double tradeValue, double buyerOrderPrice, int tradeQuantity) {
        // --- Handle Buyer ---
        User buyer = findUserById(buyerId);
        double heldAmountForTrade = buyerOrderPrice * tradeQuantity;
        buyer.setLedgerBalance(buyer.getLedgerBalance() - tradeValue);

        // If the trade executed at a better (lower) price than the limit, refund the difference to available balance.
        double priceDifference = heldAmountForTrade - tradeValue;
        if (priceDifference > 0) {
            buyer.setAvailableBalance(buyer.getAvailableBalance() + priceDifference);
        }
        userRepo.save(buyer);

        // --- Handle Seller ---
        User seller = findUserById(sellerId);
        seller.setLedgerBalance(seller.getLedgerBalance() + tradeValue);
        seller.setAvailableBalance(seller.getAvailableBalance() + tradeValue);
        userRepo.save(seller);
    }
}
