import { Stock, Order, Trade, OrderBookSummaryApiResponse, LoginCredentials, AuthResponse } from '../types';
import { getServerForSymbol, getAllServerUrls } from './serverRouter';

class ApiService {
    private token: string | null = null;

    // Use environment variables with fallback to localhost for development
    private defaultBaseUrl = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api';
    private ingressBaseUrl = process.env.REACT_APP_INGRESS_BASE_URL || 'http://localhost:8085/api';

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

    private async requestWithSymbol<T>(
        symbol: string,
        endpoint: string,
        options: RequestInit = {}
    ): Promise<T> {
        const server = getServerForSymbol(symbol);
        const url = `${server.baseUrl}/api${endpoint}`;

        console.log(`[API] 🎯 Routing ${symbol} to ${server.baseUrl}${endpoint}`);

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

    // ✅ EXISTING METHOD: For non-symbol-specific requests
    private async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
        const url = `${this.defaultBaseUrl}${endpoint}`;

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

    // ✅ NEW METHOD: For requests to ingress server (order submission)
    private async requestToIngress<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
        const url = `${this.ingressBaseUrl}${endpoint}`;

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

    // ========================================
    // Authentication (always to default server)
    // ========================================

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

    // ========================================
    // Stocks (aggregated from all servers)
    // ========================================

    async getAllStocks(): Promise<Stock[]> {
        try {
            const serverUrls = getAllServerUrls();
            console.log(`[API] 📊 Fetching stocks from ${serverUrls.length} servers`);

            // ✅ CHANGED: Fetch from ALL servers in parallel
            const stockPromises = serverUrls.map(async (baseUrl) => {
                try {
                    const response = await fetch(`${baseUrl}/api/stocks/all`, {
                        headers: this.getHeaders(),
                    });
                    if (!response.ok) {
                        console.warn(`[API] ⚠️ Server ${baseUrl} failed`);
                        return [] as Stock[];
                    }
                    const stocks = await response.json() as Stock[];
                    console.log(`[API] ✅ Got ${stocks.length} stocks from ${baseUrl}`);
                    return stocks;
                } catch (error) {
                    console.error(`[API] ❌ Failed to fetch from ${baseUrl}:`, error);
                    return [] as Stock[];
                }
            });

            const results = await Promise.all(stockPromises);

            // Combine and deduplicate stocks from all servers
            const allStocks = results.flat();
            const uniqueStocks = Array.from(
                new Map(allStocks.map(stock => [stock.symbol, stock])).values()
            );

            console.log(`[API] 📈 Total: ${uniqueStocks.length} unique stocks from ${serverUrls.length} servers`);
            return uniqueStocks;
        } catch (error) {
            console.error('[API] ❌ Error fetching all stocks:', error);
            throw error;
        }
    }

    // ✅ CHANGED: Now routes to correct server based on symbol
    async getStockDetail(symbol: string): Promise<OrderBookSummaryApiResponse> {
        return this.requestWithSymbol<OrderBookSummaryApiResponse>(symbol, `/stocks/${symbol}`);
    }

    // ✅ CHANGED: Now routes to correct server based on symbol
    async getStockTrades(symbol: string): Promise<Trade[]> {
        return this.requestWithSymbol<Trade[]>(symbol, `/stocks/${symbol}/trades`);
    }

    // ========================================
    // Orders (symbol-specific)
    // ========================================

    // ✅ CHANGED: Routes ALL orders to ingress server (8085) which sends to Kafka
    async submitOrder(order: Order): Promise<{ success: boolean; orderId: string; message: string }> {
        console.log(`[API] 📤 Submitting ${order.side} order for ${order.symbol} (qty: ${order.quantity}, price: ${order.price}) via ingress server`);
        return this.requestToIngress('/orders/submit', {
            method: 'POST',
            body: JSON.stringify(order),
        });
    }

    // User orders - go to default server
    async getAllOrders(): Promise<Order[]> {
        return this.request<Order[]>('/orders/all');
    }

    async cancelOrder(orderId: string): Promise<{ success: boolean; message: string }> {
        return this.request(`/orders/${orderId}`, {
            method: 'DELETE',
        });
    }

    // ========================================
    // Prices (symbol-specific)
    // ========================================

    // ✅ CHANGED: Now routes to correct server based on symbol
    async getCurrentPrice(symbol: string): Promise<{ symbol: string; currentPrice: number; timestamp: number }> {
        return this.requestWithSymbol(symbol, `/prices/current/${symbol}`);
    }

    async getAllPrices(): Promise<{ prices: Record<string, number>; timestamp: number }> {
        return this.request('/prices/all');
    }

    // ========================================
    // User Profile (default server)
    // ========================================

    async getUserProfile(): Promise<{
        user: { userId: string; username: string; email: string; ledgerBalance?: number; availableBalance?: number };
        account?: {
            ledgerBalance: number;
            availableBalance: number;
            holdings: Record<string, number>;
        };
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
        return this.request<Order[]>('/user/orders');
    }

    async getUserTrades(): Promise<Trade[]> {
        return this.request<Trade[]>('/user/trades');
    }

    async getUserInfo(): Promise<{ 
        userId: string; 
        username: string; 
        email: string; 
        ledgerBalance: number; 
        availableBalance: number; 
        holdings: Record<string, number> 
    }> {
        return this.request('/user/info');
    }
}

export const apiService = new ApiService();
