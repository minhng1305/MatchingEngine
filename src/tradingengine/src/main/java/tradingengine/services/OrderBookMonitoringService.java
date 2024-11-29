package tradingengine.services;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import tradingengine.models.OrderBookSummary;

public class OrderBookMonitoringService {
    private final RedisTemplate<String, Object> redisTemplate;

    public OrderBookMonitoringService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(fixedRate = 1000) // Run every second
    public void printOrderBookStatus() {
        // Print order book status for monitoring
        redisTemplate.keys("orderbook:*").forEach(key -> {
            OrderBookSummary summary = (OrderBookSummary) redisTemplate.opsForValue().get(key);
            System.out.println("Order Book Status for " + summary.getSymbol());
            System.out.println("Top Buys: " + summary.highestBuys);
            System.out.println("Top Sells: " + summary.lowestSells);
        });
    }
}
