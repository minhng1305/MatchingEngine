import { useState, useEffect, useCallback, useRef } from 'react';
import { webSocketService, WebSocketCallback } from '../services/websocket';

interface UseWebSocketOptions {
    onConnect?: () => void;
    onDisconnect?: () => void;
    onError?: (error: Error) => void;
}

export const useWebSocket = (options: UseWebSocketOptions = {}) => {
    const [isConnected, setIsConnected] = useState(false);
    const [error, setError] = useState<Error | null>(null);
    const subscriptionsRef = useRef<string[]>([]);

    useEffect(() => {
        const connect = async () => {
            try {
                await webSocketService.connect();
                setIsConnected(true);
                setError(null);
                options.onConnect?.();
            } catch (err) {
                const error = err instanceof Error ? err : new Error('WebSocket connection failed');
                setError(error);
                setIsConnected(false);
                options.onError?.(error);
            }
        };

        connect();

        return () => {
            subscriptionsRef.current.forEach((subscriptionId) => {
                webSocketService.unsubscribe(subscriptionId);
            });
            subscriptionsRef.current = [];
            webSocketService.disconnect();
            setIsConnected(false);
            options.onDisconnect?.();
        };
    }, []);

    const subscribeToOrderBook = useCallback((symbol: string, callback: WebSocketCallback) => {
        if (!isConnected) return null;

        try {
            const subscriptionId = webSocketService.subscribeToOrderBookUpdates(symbol, callback);
            subscriptionsRef.current.push(subscriptionId);
            return subscriptionId;
        } catch (err) {
            console.error('Error subscribing to order book:', err);
            return null;
        }
    }, [isConnected]);

    const subscribeToTrades = useCallback((symbol: string, callback: WebSocketCallback) => {
        if (!isConnected) return null;

        try {
            const subscriptionId = webSocketService.subscribeToTradeUpdates(symbol, callback);
            subscriptionsRef.current.push(subscriptionId);
            return subscriptionId;
        } catch (err) {
            console.error('Error subscribing to trades:', err);
            return null;
        }
    }, [isConnected]);

    const subscribeToAllPrices = useCallback((callback: WebSocketCallback) => {
        if (!isConnected) return null;

        try {
            const subscriptionId = webSocketService.subscribeToAllPriceUpdates(callback);
            subscriptionsRef.current.push(subscriptionId);
            return subscriptionId;
        } catch (err) {
            console.error('Error subscribing to all prices:', err);
            return null;
        }
    }, [isConnected]);

    const unsubscribe = useCallback((subscriptionId: string) => {
        webSocketService.unsubscribe(subscriptionId);
        subscriptionsRef.current = subscriptionsRef.current.filter(id => id !== subscriptionId);
    }, []);

    return {
        isConnected,
        error,
        subscribeToOrderBook,
        subscribeToTrades,
        subscribeToAllPrices,
        unsubscribe,
    };
};