package com.project.matchingengine.service.authentication;

/**
 * Lua scripts for atomic Redis operations
 * These scripts ensure atomicity when updating user balances and holdings
 */
public class RedisLuaScripts {
    
    /**
     * Lua script for applying a BUY trade atomically
     * Updates: available balance, ledger balance, and holdings
     * 
     * KEYS[1]: user balance hash key (me:user:{userId}:balance)
     * KEYS[2]: user holding key (me:user:{userId}:h:{symbol})
     * ARGV[1]: quantityDelta (as string)
     * ARGV[2]: tradePrice (as string)
     * ARGV[3]: initialPrice (as string)
     * ARGV[4]: updatedAt timestamp (as string)
     * 
     * Returns: "OK" on success
     */
    public static final String APPLY_BUY_TRADE = 
        "local qty = tonumber(ARGV[1])\n" +
        "local tradePrice = tonumber(ARGV[2])\n" +
        "local initialPrice = tonumber(ARGV[3])\n" +
        "\n" +
        "local available = tonumber(redis.call('HGET', KEYS[1], 'available') or '0')\n" +
        "local ledger = tonumber(redis.call('HGET', KEYS[1], 'ledger') or '0')\n" +
        "local hold = tonumber(redis.call('GET', KEYS[2]) or '0')\n" +
        "\n" +
        "local cashDelta = qty * initialPrice - qty * tradePrice\n" +
        "available = available + cashDelta\n" +
        "ledger = ledger - qty * initialPrice\n" +
        "hold = hold + qty\n" +
        "\n" +
        "redis.call('HSET', KEYS[1], 'available', tostring(available), 'ledger', tostring(ledger), 'updatedAt', ARGV[4])\n" +
        "redis.call('SET', KEYS[2], tostring(hold))\n" +
        "return 'OK'";
    
    /**
     * Lua script for applying a SELL trade atomically
     * Updates: available balance, ledger balance, and holdings
     * 
     * KEYS[1]: user balance hash key (me:user:{userId}:balance)
     * KEYS[2]: user holding key (me:user:{userId}:h:{symbol})
     * ARGV[1]: quantityDelta (as string)
     * ARGV[2]: tradePrice (as string)
     * ARGV[3]: updatedAt timestamp (as string)
     * 
     * Returns: "OK" on success
     */
    public static final String APPLY_SELL_TRADE = 
        "local qty = tonumber(ARGV[1])\n" +
        "local tradePrice = tonumber(ARGV[2])\n" +
        "\n" +
        "local available = tonumber(redis.call('HGET', KEYS[1], 'available') or '0')\n" +
        "local ledger = tonumber(redis.call('HGET', KEYS[1], 'ledger') or '0')\n" +
        "local hold = tonumber(redis.call('GET', KEYS[2]) or '0')\n" +
        "\n" +
        "local cashDelta = qty * tradePrice\n" +
        "available = available + cashDelta\n" +
        "ledger = ledger + cashDelta\n" +
        "hold = hold - qty\n" +
        "\n" +
        "redis.call('HSET', KEYS[1], 'available', tostring(available), 'ledger', tostring(ledger), 'updatedAt', ARGV[3])\n" +
        "if hold > 0 then\n" +
        "  redis.call('SET', KEYS[2], tostring(hold))\n" +
        "else\n" +
        "  redis.call('DEL', KEYS[2])\n" +
        "end\n" +
        "return 'OK'";
    
    /**
     * Lua script for placing a BUY order (check and deduct funds)
     * 
     * KEYS[1]: user balance hash key (me:user:{userId}:balance)
     * ARGV[1]: required amount (price * quantity, as string)
     * 
     * Returns: "OK" if sufficient funds, "INSUFFICIENT" otherwise
     */
    public static final String PLACE_BUY_ORDER = 
        "local required = tonumber(ARGV[1])\n" +
        "local available = tonumber(redis.call('HGET', KEYS[1], 'available') or '0')\n" +
        "\n" +
        "if available >= required then\n" +
        "  available = available - required\n" +
        "  redis.call('HSET', KEYS[1], 'available', tostring(available), 'updatedAt', ARGV[2])\n" +
        "  return 'OK'\n" +
        "else\n" +
        "  return 'INSUFFICIENT'\n" +
        "end";
    
    /**
     * Lua script for placing a SELL order (check and deduct holdings)
     * 
     * KEYS[1]: user holding key (me:user:{userId}:h:{symbol})
     * ARGV[1]: required quantity (as string)
     * 
     * Returns: "OK" if sufficient holdings, "INSUFFICIENT" otherwise
     */
    public static final String PLACE_SELL_ORDER = 
        "local required = tonumber(ARGV[1])\n" +
        "local hold = tonumber(redis.call('GET', KEYS[1]) or '0')\n" +
        "\n" +
        "if hold >= required then\n" +
        "  hold = hold - required\n" +
        "  if hold > 0 then\n" +
        "    redis.call('SET', KEYS[1], tostring(hold))\n" +
        "  else\n" +
        "    redis.call('DEL', KEYS[1])\n" +
        "  end\n" +
        "  return 'OK'\n" +
        "else\n" +
        "  return 'INSUFFICIENT'\n" +
        "end";
}
