import { useState, useEffect, useCallback, useRef } from 'react';
import { webSocketService, WebSocketCallback } from '../services/websocket';

interface UseWebSocketOptions {
    autoConnect?: boolean;
    onConnect?: () => void;
    onDisconnect?: () => void;
    onError?: (error: Error) => void;
}

export const useWebSocket = (options: UseWebSocketOptions = {}) => {
    const [isConnected, setIsConnected] = useState(false);
    const [error, setError] = useState<Error | null>(null);
    const subscriptionsRef = useRef<string[]>([]);

    useEffect(() => {
        // Cleanup on unmount
        return () => {
            subscriptionsRef.current.forEach(id => {
                webSocketService.unsubscribe(id);
            });
            subscriptionsRef.current = [];
        };
    }, []);

    /**
     * ✅ Subscribe to order book for a specific symbol
     */
    const subscribeToOrderBook = useCallback(async (symbol: string, callback: WebSocketCallback) => {
        try {
            const subscriptionId = await webSocketService.subscribeToOrderBookUpdates(symbol, callback);
            subscriptionsRef.current.push(subscriptionId);
            setIsConnected(true);
            return subscriptionId;
        } catch (err) {
            console.error('Error subscribing to order book:', err);
            const error = err instanceof Error ? err : new Error('Failed to subscribe to order book');
            setError(error);
            options.onError?.(error);
            throw err;
        }
    }, [options]);

    /**
     * ✅ Subscribe to trades for a specific symbol
     */
    const subscribeToTrades = useCallback(async (symbol: string, callback: WebSocketCallback) => {
        try {
            const subscriptionId = await webSocketService.subscribeToTradeUpdates(symbol, callback);
            subscriptionsRef.current.push(subscriptionId);
            setIsConnected(true);
            return subscriptionId;
        } catch (err) {
            console.error('Error subscribing to trades:', err);
            const error = err instanceof Error ? err : new Error('Failed to subscribe to trades');
            setError(error);
            options.onError?.(error);
            throw err;
        }
    }, [options]);

    /**
     * ✅ DEPRECATED: Old single-server subscription
     */
    const subscribeToAllPrices = useCallback(async (callback: WebSocketCallback) => {
        try {
            const subscriptionId = await webSocketService.subscribeToAllPriceUpdates(callback);
            subscriptionsRef.current.push(subscriptionId);
            setIsConnected(true);
            return subscriptionId;
        } catch (err) {
            console.error('Error subscribing to all prices:', err);
            const error = err instanceof Error ? err : new Error('Failed to subscribe to all prices');
            setError(error);
            options.onError?.(error);
            throw err;
        }
    }, [options]);

    /**
     * ✅ NEW: Subscribe to prices from ALL servers
     * This is what HomePage should use!
     */
    const subscribeToMultiServerPrices = useCallback(async (callback: WebSocketCallback) => {
        try {
            const subscriptionIds = await webSocketService.subscribeToMultiServerPriceUpdates(callback);
            subscriptionsRef.current.push(...subscriptionIds);
            setIsConnected(true);
            options.onConnect?.();
            return subscriptionIds;
        } catch (err) {
            console.error('Error subscribing to multi-server prices:', err);
            const error = err instanceof Error ? err : new Error('Failed to subscribe to multi-server prices');
            setError(error);
            options.onError?.(error);
            throw err;
        }
    }, [options]);

    const unsubscribe = useCallback((subscriptionId: string) => {
        webSocketService.unsubscribe(subscriptionId);
        subscriptionsRef.current = subscriptionsRef.current.filter(id => id !== subscriptionId);
    }, []);

    const disconnect = useCallback(() => {
        subscriptionsRef.current.forEach(id => {
            webSocketService.unsubscribe(id);
        });
        subscriptionsRef.current = [];
        webSocketService.disconnect();
        setIsConnected(false);
        options.onDisconnect?.();
    }, [options]);

    return {
        isConnected,
        error,
        subscribeToOrderBook,
        subscribeToTrades,
        subscribeToAllPrices,         // ⚠️ DEPRECATED - for backwards compatibility
        subscribeToMultiServerPrices, // ✅ NEW - use this for HomePage!
        unsubscribe,
        disconnect,
    };
};
