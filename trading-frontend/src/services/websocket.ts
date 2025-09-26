import { Client, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { OrderBookSummary } from '../types';

export type WebSocketCallback = (data: any) => void;

class WebSocketService {
    private client: Client | null = null;
    private subscriptions: Map<string, StompSubscription> = new Map();
    private reconnectAttempts = 0;
    private maxReconnectAttempts = 5;
    private reconnectDelay = 5000;

    connect(): Promise<void> {
        return new Promise((resolve, reject) => {
            if (this.client && this.client.connected) {
                resolve();
                return;
            }

            this.client = new Client({
                webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
                connectHeaders: {},
                debug: (str) => {
                    console.log('WebSocket Debug:', str);
                },
                reconnectDelay: this.reconnectDelay,
                heartbeatIncoming: 4000,
                heartbeatOutgoing: 4000,
            });

            this.client.onConnect = () => {
                console.log('WebSocket Connected');
                this.reconnectAttempts = 0;
                resolve();
            };

            this.client.onStompError = (frame) => {
                console.error('WebSocket Error:', frame);
                reject(new Error(frame.body));
            };

            this.client.onDisconnect = () => {
                console.log('WebSocket Disconnected');
                this.handleReconnect();
            };

            this.client.activate();
        });
    }

    private handleReconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            console.log(`Attempting to reconnect... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
            setTimeout(() => {
                this.connect().catch(console.error);
            }, this.reconnectDelay * this.reconnectAttempts);
        }
    }

    subscribeToOrderBookUpdates(symbol: string, callback: WebSocketCallback): string {
        if (!this.client || !this.client.connected) {
            throw new Error('WebSocket not connected');
        }

        const topic = `/topic/orderbook-updates/${symbol}`;
        const subscription = this.client.subscribe(topic, (message) => {
            try {
                const data: OrderBookSummary = JSON.parse(message.body);
                callback(data);
            } catch (error) {
                console.error('Error parsing WebSocket message:', error);
            }
        });

        const subscriptionId = `orderbook-${symbol}`;
        this.subscriptions.set(subscriptionId, subscription);
        return subscriptionId;
    }

    subscribeToTradeUpdates(symbol: string, callback: WebSocketCallback): string {
        if (!this.client || !this.client.connected) {
            throw new Error('WebSocket not connected');
        }

        const topic = `/topic/trades/${symbol}`;
        const subscription = this.client.subscribe(topic, (message) => {
            try {
                const data = JSON.parse(message.body);
                callback(data);
            } catch (error) {
                console.error('Error parsing WebSocket message:', error);
            }
        });

        const subscriptionId = `trades-${symbol}`;
        this.subscriptions.set(subscriptionId, subscription);
        return subscriptionId;
    }

    subscribeToAllPriceUpdates(callback: WebSocketCallback): string {
        if (!this.client || !this.client.connected) {
            throw new Error('WebSocket not connected');
        }

        const topic = '/topic/price-updates';
        const subscription = this.client.subscribe(topic, (message) => {
            try {
                const data = JSON.parse(message.body);
                callback(data);
            } catch (error) {
                console.error('Error parsing WebSocket message:', error);
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
        if (this.client) {
            this.unsubscribeAll();
            this.client.deactivate();
            this.client = null;
        }
    }

    isConnected(): boolean {
        return this.client ? this.client.connected : false;
    }
}

export const webSocketService = new WebSocketService();