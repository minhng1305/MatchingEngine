import { Client, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { OrderBookSummary } from '../types';
import { getWebSocketUrl, SERVER_CONFIGS } from './serverRouter';

export type WebSocketCallback = (data: any) => void;

class WebSocketService {
    // Map of server URL → WebSocket client
    private clients: Map<string, Client> = new Map();
    private subscriptions: Map<string, StompSubscription> = new Map();
    private reconnectAttempts: Map<string, number> = new Map();
    private maxReconnectAttempts = 5;
    private reconnectDelay = 5000;

    /**
     * ✅ Connect to a specific WebSocket server
     */
    async connectToServer(serverUrl: string): Promise<void> {
        // Check if already connected
        if (this.clients.has(serverUrl) && this.clients.get(serverUrl)?.connected) {
            console.log(`[WS] ✅ Already connected to ${serverUrl}`);
            return;
        }

        return new Promise((resolve, reject) => {
            console.log(`[WS] 🔌 Connecting to ${serverUrl}`);

            const client = new Client({
                webSocketFactory: () => new SockJS(`${serverUrl}/ws`),
                connectHeaders: {},
                debug: (str) => {
                    // console.log(`[WS ${serverUrl}]`, str); // Uncomment for detailed debugging
                },
                reconnectDelay: this.reconnectDelay,
                heartbeatIncoming: 4000,
                heartbeatOutgoing: 4000,
            });

            client.onConnect = () => {
                console.log(`[WS] ✅ Connected to ${serverUrl}`);
                this.reconnectAttempts.set(serverUrl, 0);
                this.clients.set(serverUrl, client);
                resolve();
            };

            client.onStompError = (frame) => {
                console.error(`[WS] ❌ Error connecting to ${serverUrl}:`, frame);
                reject(new Error(frame.body));
            };

            client.onDisconnect = () => {
                console.log(`[WS] ⚠️ Disconnected from ${serverUrl}`);
                this.handleReconnect(serverUrl);
            };

            client.activate();
        });
    }

    /**
     * ✅ Connect to the appropriate server for a symbol
     */
    async connectForSymbol(symbol: string): Promise<void> {
        const serverUrl = getWebSocketUrl(symbol);
        return this.connectToServer(serverUrl);
    }

    /**
     * ✅ NEW: Connect to ALL servers for multi-server price updates
     */
    async connectToAllServers(): Promise<void> {
        const connectionPromises = SERVER_CONFIGS.map(config => {
            const serverUrl = `http://localhost:${config.wsPort}`;
            return this.connectToServer(serverUrl).catch(err => {
                console.error(`[WS] ❌ Failed to connect to ${serverUrl}:`, err);
                // Don't reject - allow other servers to connect
            });
        });

        await Promise.all(connectionPromises);
        console.log('[WS] ✅ Connected to all available servers');
    }

    private handleReconnect(serverUrl: string) {
        const attempts = this.reconnectAttempts.get(serverUrl) || 0;

        if (attempts < this.maxReconnectAttempts) {
            this.reconnectAttempts.set(serverUrl, attempts + 1);
            console.log(`[WS] 🔄 Reconnecting to ${serverUrl}... (${attempts + 1}/${this.maxReconnectAttempts})`);

            setTimeout(() => {
                this.connectToServer(serverUrl).catch(console.error);
            }, this.reconnectDelay * (attempts + 1));
        } else {
            console.error(`[WS] ❌ Max reconnection attempts reached for ${serverUrl}`);
        }
    }

    /**
     * ✅ Subscribe to order book updates for a specific symbol
     */
    async subscribeToOrderBookUpdates(symbol: string, callback: WebSocketCallback): Promise<string> {
        await this.connectForSymbol(symbol);

        const serverUrl = getWebSocketUrl(symbol);
        const client = this.clients.get(serverUrl);

        if (!client || !client.connected) {
            throw new Error(`WebSocket not connected for ${symbol}`);
        }

        const topic = `/topic/orderbook-updates/${symbol}`;
        const subscription = client.subscribe(topic, (message) => {
            try {
                const data: OrderBookSummary = JSON.parse(message.body);
                callback(data);
            } catch (error) {
                console.error(`[WS] ❌ Error parsing message for ${symbol}:`, error);
            }
        });

        const subscriptionId = `orderbook-${symbol}`;
        this.subscriptions.set(subscriptionId, subscription);
        console.log(`[WS] 📡 Subscribed to ${topic} on ${serverUrl}`);

        return subscriptionId;
    }

    /**
     * ✅ Subscribe to trade updates for a specific symbol
     */
    async subscribeToTradeUpdates(symbol: string, callback: WebSocketCallback): Promise<string> {
        await this.connectForSymbol(symbol);

        const serverUrl = getWebSocketUrl(symbol);
        const client = this.clients.get(serverUrl);

        if (!client || !client.connected) {
            throw new Error(`WebSocket not connected for ${symbol}`);
        }

        const topic = `/topic/trades/${symbol}`;
        const subscription = client.subscribe(topic, (message) => {
            try {
                const data = JSON.parse(message.body);
                callback(data);
            } catch (error) {
                console.error(`[WS] ❌ Error parsing trade message for ${symbol}:`, error);
            }
        });

        const subscriptionId = `trades-${symbol}`;
        this.subscriptions.set(subscriptionId, subscription);

        return subscriptionId;
    }

    /**
     * ✅ NEW: Subscribe to price updates from ALL servers
     * This fixes the HomePage price inconsistency issue!
     */
    async subscribeToMultiServerPriceUpdates(callback: WebSocketCallback): Promise<string[]> {
        await this.connectToAllServers();

        const subscriptionIds: string[] = [];

        // Subscribe to price updates from EACH server
        for (const config of SERVER_CONFIGS) {
            const serverUrl = `http://localhost:${config.wsPort}`;
            const client = this.clients.get(serverUrl);

            if (!client || !client.connected) {
                console.warn(`[WS] ⚠️ Skipping ${serverUrl} - not connected`);
                continue;
            }

            const topic = '/topic/price-updates';

            try {
                const subscription = client.subscribe(topic, (message: { body: string }) => {
                    try {
                        const data = JSON.parse(message.body);
                        // Add server info for debugging
                        data._sourceServer = serverUrl;
                        callback(data);
                    } catch (error) {
                        console.error(`[WS] ❌ Error parsing price update from ${serverUrl}:`, error);
                    }
                });

                const subscriptionId = `price-updates-${config.port}`;
                this.subscriptions.set(subscriptionId, subscription);
                subscriptionIds.push(subscriptionId);

                console.log(`[WS] 📡 Subscribed to ${topic} on ${serverUrl}`);
            } catch (error) {
                console.error(`[WS] ❌ Failed to subscribe to ${serverUrl}:`, error);
            }
        }

        if (subscriptionIds.length === 0) {
            throw new Error('Failed to subscribe to any price update feeds');
        }

        console.log(`[WS] ✅ Subscribed to price updates from ${subscriptionIds.length} servers`);
        return subscriptionIds;
    }

    /**
     * ✅ DEPRECATED: Old method - still works for backwards compatibility
     */
    async subscribeToAllPriceUpdates(callback: WebSocketCallback): Promise<string> {
        console.warn('[WS] ⚠️ subscribeToAllPriceUpdates is deprecated. Use subscribeToMultiServerPriceUpdates instead.');

        // Connect to first server only (old behavior)
        await this.connectForSymbol('AAPL');
        const client = this.clients.values().next().value;

        if (!client || !client.connected) {
            throw new Error('WebSocket not connected');
        }

        const topic = '/topic/price-updates';
        const subscription = client.subscribe(topic, (message: { body: string }) => {
            try {
                const data = JSON.parse(message.body);
                callback(data);
            } catch (error) {
                console.error('[WS] ❌ Error parsing price update:', error);
            }
        });

        const subscriptionId = 'all-prices';
        this.subscriptions.set(subscriptionId, subscription);

        return subscriptionId;
    }

    unsubscribe(subscriptionId: string) {
        const subscription = this.subscriptions.get(subscriptionId);
        if (subscription) {
            subscription.unsubscribe();
            this.subscriptions.delete(subscriptionId);
        }
    }

    unsubscribeAll() {
        this.subscriptions.forEach((subscription) => {
            subscription.unsubscribe();
        });
        this.subscriptions.clear();
    }

    disconnect() {
        this.clients.forEach((client) => {
            if (client.connected) {
                client.deactivate();
            }
        });
        this.clients.clear();
        this.unsubscribeAll();
    }

    isConnected(symbol?: string): boolean {
        if (symbol) {
            const serverUrl = getWebSocketUrl(symbol);
            const client = this.clients.get(serverUrl);
            return client ? client.connected : false;
        }

        // Check if any client is connected
        return Array.from(this.clients.values()).some(client => client.connected);
    }
}

export const webSocketService = new WebSocketService();
