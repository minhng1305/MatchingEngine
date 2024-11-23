package tradingengine.services;

import java.time.Duration;
import java.util.Random;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import redis.clients.jedis.*;
import tradingengine.jni.MatchingEngineJNI;
import tradingengine.models.Order;

@Service
public class OrderService {
    private final RedisTemplate<String, String> redisTemplate;
    private final MatchingEngineJNI matchingEngineJNI;
    private static final String REDIS_KEY_PREFIX = "order:";
    private static final Duration ORDER_EXPIRATION = Duration.ofHours(24);
    private static final String[] SYMBOLS = { "BTCUSD", "ETHUSD", "LTCUSD" };
    private static final Random random = new Random();

    public OrderService(RedisTemplate<String, String> redisTemplate, MatchingEngineJNI matchingEngineJNI) {
        this.redisTemplate = redisTemplate;
        this.matchingEngineJNI = matchingEngineJNI;
    }

    public void processOrder(Order order, long pointer) {
        String redisKey = REDIS_KEY_PREFIX + order.getOrderId().toString();
        Boolean isNewOrder = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", ORDER_EXPIRATION);
        if (Boolean.TRUE.equals(isNewOrder)) {
            try {
                System.out.println("Processing order for symbol: " + order.getSymbol());
                if (pointer != -1)
                    matchingEngineJNI.processOrder(pointer, order.getOrderId().toString(), order.side, order.getPrice(), order.getCurrentQuantity());
                else
                    System.out.println("Error: No order book found for symbol " + order.getSymbol());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Duplicated order: " + order.getOrderId());
        }
    }
}

