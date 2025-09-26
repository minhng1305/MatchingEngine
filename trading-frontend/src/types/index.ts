export interface Stock {
    symbol: string;
    companyName: string;
    currentPrice: number;
    esgScore: number;
}

export interface Order {
    orderId?: string;
    userId?: string;
    symbol: string;
    side: 'BUY' | 'SELL';
    type: 'LIMIT' | 'MARKET';
    price: number;
    quantity: number;
    originalQuantity?: number;
    currentQuantity?: number;
    status?: OrderStatus;
    orderTimestamp?: string;
}

export interface Trade {
    tradeId: string;
    symbol: string;
    price: number;
    quantity: number;
    buyOrderId: string;
    sellOrderId: string;
    tradeTimestamp: string;
}

export interface OrderBookSummary {
    symbol: string;
    topBuys: Order[];
    lowestSells: Order[];
    currentPrice: number;
    bestBidPrice: number;
    bestBidQuantity: number;
    bestAskPrice: number;
    bestAskQuantity: number;
    recentTrades: Trade[];
}

export enum OrderStatus {
    PENDING = 'PENDING',
    FILLED = 'FILLED',
    PARTIALLY_FILLED = 'PARTIALLY_FILLED',
    CANCELLED = 'CANCELLED'
}

export interface User {
    userId: string;
    username: string;
    email: string;
}

export interface LoginCredentials {
    username: string;
    password: string;
}

export interface AuthResponse {
    token: string;
    user: User;
}

export interface ApiError {
    message: string;
    status?: number;
}