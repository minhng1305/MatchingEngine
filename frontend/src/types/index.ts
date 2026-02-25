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
  side: OrderSide;
  type: OrderType;
  price: number;
  quantity: number;
  originalQuantity?: number;
  currentQuantity?: number;
  limitPrice?: number;
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
  buyUserId?: string;
  sellUserId?: string;
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

export interface User {
  userId: string;
  username: string;
  email: string;
  ledgerBalance?: number;
  availableBalance?: number;
}

export interface Holding {
  symbol: string;
  quantity: number;
}

export interface UserProfile {
  user: User;
  account: {
    ledgerBalance: number;
    availableBalance: number;
    holdings: Holding[];
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
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface PriceUpdate {
  prices: Record<string, number>;
  timestamp: number;
}

export type OrderSide = 'BUY' | 'SELL';
export type OrderType = 'LIMIT' | 'MARKET';
export type OrderStatus = 'PENDING' | 'FILLED' | 'PARTIALLY_FILLED' | 'CANCELED';

export interface TickerItem {
  symbol: string;
  price: number;
  change: number;
}
