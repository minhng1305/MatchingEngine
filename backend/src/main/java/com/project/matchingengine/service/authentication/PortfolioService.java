package com.project.matchingengine.service.authentication;

import com.project.matchingengine.models.authentication.Portfolio;
import com.project.matchingengine.repository.authentication.PortfolioRepo;
import com.project.matchingengine.service.orderbook.OrderService;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.Optional;


@Service
public class PortfolioService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private final PortfolioRepo portfolioRepo;

    public PortfolioService(PortfolioRepo portfolioRepo) {
        this.portfolioRepo = portfolioRepo;
    }

    public boolean isPortfolioExists(UUID userId, String symbol) {
        return portfolioRepo.findByUserIdAndSymbol(userId, symbol).isPresent();
    }


    public int getHoldingQuantity(UUID userId, String symbol) {
        return portfolioRepo.findByUserIdAndSymbol(userId, symbol)
                .map(Portfolio::getQuantity)
                .orElse(0);
    }

    @Transactional
    public void addHolding(UUID userId, String symbol, int quantity) {
        Optional<Portfolio> existingPortfolio = portfolioRepo.findByUserIdAndSymbol(userId, symbol);
        if (existingPortfolio.isEmpty()) {
            logger.info("Creating new portfolio entry for User: {} Symbol: {}", userId, symbol);
            portfolioRepo.save(new Portfolio(userId, symbol, quantity));
        } else {
            Portfolio portfolio = existingPortfolio.get();
            int newQuantity = portfolio.getQuantity() + quantity;
            portfolio.setQuantity(newQuantity);
            portfolioRepo.save(portfolio);
            logger.info("Updated portfolio for User: {} Symbol: {}. New Quantity: {}", userId, symbol, newQuantity);
        }
    }

    @Transactional
    public boolean removeHolding(UUID userId, String symbol, int quantity) {
        Optional<Portfolio> existingPortfolio = portfolioRepo.findByUserIdAndSymbol(userId, symbol);

        if (existingPortfolio.isEmpty()) {
            logger.error("Attempted to remove holding for non-existent portfolio. User: {} Symbol: {}", userId, symbol);
            return false;
        }
        Portfolio portfolio = existingPortfolio.get();

        if (quantity > portfolio.getQuantity()) {
            logger.error("Insufficient funds. User: {} Symbol: {} Current: {} Requested: {}",
                    userId, symbol, portfolio.getQuantity(), quantity);
            return false;
        }
        int newQuantity = portfolio.getQuantity() - quantity;
        portfolio.setQuantity(newQuantity);
        portfolioRepo.save(portfolio);
        logger.info("Removed holding. User: {} Symbol: {}. Remaining Quantity: {}", userId, symbol, newQuantity);
        return true;
    }
}
