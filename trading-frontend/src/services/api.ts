import { Stock, Order, Trade, OrderBookSummary, LoginCredentials, AuthResponse } from '../types';

const API_BASE_URL = 'http://localhost:8080/api';

class ApiService {
    private token: string | null = null;

    setToken(token: string) {
        this.token = token;
    }

    private getHeaders(): HeadersInit {
        const headers: HeadersInit = {
            'Content-Type': 'application/json',
        };

        if (this.token) {
            headers['Authorization'] = `Bearer ${this.token}`;
        }

        return headers;
    }

    private async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
        const url = `${API_BASE_URL}${endpoint}`;
        const response = await fetch(url, {
            ...options,
            headers: {
                ...this.getHeaders(),
                ...options.headers,
            },
        });

        if (!response.ok) {
            const error = await response.json().catch(() => ({ message: 'Network error' }));
            throw new Error(error.message || `HTTP error! status: ${response.status}`);
        }

        return response.json();
    }

    // Authentication
    async login(credentials: LoginCredentials): Promise<AuthResponse> {
        return this.request<AuthResponse>('/auth/login', {
            method: 'POST',
            body: JSON.stringify(credentials),
        });
    }

    async register(userData: { username: string; email: string; password: string }): Promise<AuthResponse> {
        return this.request<AuthResponse>('/auth/register', {
            method: 'POST',
            body: JSON.stringify(userData),
        });
    }

    // Stocks
    async getAllStocks(): Promise<Stock[]> {
        return this.request<Stock[]>('/stocks/all');
    }

    async getStockDetail(symbol: string): Promise<OrderBookSummary> {
        return this.request<OrderBookSummary>(`/stocks/${symbol}`);
    }

    async getStockTrades(symbol: string): Promise<Trade[]> {
        return this.request<Trade[]>(`/stocks/${symbol}/trades`);
    }

    // Orders
    async submitOrder(order: Order): Promise<{ success: boolean; orderId: string; message: string }> {
        return this.request('/orders/submit', {
            method: 'POST',
            body: JSON.stringify(order),
        });
    }

    async getAllOrders(): Promise<Order[]> {
        return this.request<Order[]>('/orders/all');
    }

    async cancelOrder(orderId: string): Promise<{ success: boolean; message: string }> {
        return this.request(`/orders/${orderId}`, {
            method: 'DELETE',
        });
    }

    // Prices
    async getCurrentPrice(symbol: string): Promise<{ symbol: string; currentPrice: number; timestamp: number }> {
        return this.request(`/prices/current/${symbol}`);
    }

    async getAllPrices(): Promise<{ prices: Record<string, number>; timestamp: number }> {
        return this.request('/prices/all');
    }

    // User Profile
    async getUserProfile(): Promise<{
        user: { userId: string; username: string; email: string };
        statistics: {
            totalOrders: number;
            pendingOrders: number;
            filledOrders: number;
            totalTrades: number;
            totalTradeValue: number;
        };
        recentOrders: Order[];
        recentTrades: Trade[];
    }> {
        return this.request('/user/profile');
    }

    async getUserOrders(): Promise<Order[]> {
        return this.request('/user/orders');
    }

    async getUserTrades(): Promise<Trade[]> {
        return this.request('/user/trades');
    }

    async getUserInfo(): Promise<{ userId: string; username: string; email: string }> {
        return this.request('/user/info');
    }
}

export const apiService = new ApiService();