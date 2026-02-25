import { create } from 'zustand';
import { OrderBookSummary, Stock, Trade } from '../types';
import * as api from '../services/api';

/**
 * Merge incoming prices into existing ones. Never overwrite a known
 * non-zero price with 0 — works around the backend bug where
 * getCurrentPrice() returns 0 after clearTradeRecords().
 */
function mergePrices(
  existing: Record<string, number>,
  incoming: Record<string, number>
): Record<string, number> {
  const merged = { ...existing };
  for (const [sym, price] of Object.entries(incoming)) {
    if (price !== 0 || !(sym in merged) || merged[sym] === 0) {
      merged[sym] = price;
    }
  }
  return merged;
}

interface MarketState {
  stocks: Stock[];
  prices: Record<string, number>;
  previousPrices: Record<string, number>;
  selectedSymbol: string | null;
  orderBook: OrderBookSummary | null;
  recentTrades: Trade[];
  watchlist: string[];
  loading: boolean;

  loadStocks: () => Promise<void>;
  loadPrices: () => Promise<void>;
  loadSummaryPrices: () => Promise<void>;
  updatePrices: (newPrices: Record<string, number>) => void;
  selectSymbol: (symbol: string) => void;
  loadStockDetail: (symbol: string) => Promise<void>;
  loadTrades: (symbol: string) => Promise<void>;
  updateOrderBook: (data: OrderBookSummary) => void;
  toggleWatchlist: (symbol: string) => void;
}

const savedWatchlist = (): string[] => {
  try {
    return JSON.parse(localStorage.getItem('watchlist') || '[]');
  } catch {
    return [];
  }
};

export const useMarketStore = create<MarketState>((set, get) => ({
  stocks: [],
  prices: {},
  previousPrices: {},
  selectedSymbol: null,
  orderBook: null,
  recentTrades: [],
  watchlist: savedWatchlist(),
  loading: false,

  loadStocks: async () => {
    set({ loading: true });
    try {
      const stocks = await api.fetchAllStocks();
      const incoming: Record<string, number> = {};
      stocks.forEach((s) => (incoming[s.symbol] = s.currentPrice));
      set((state) => ({
        stocks,
        prices: mergePrices(state.prices, incoming),
        loading: false,
      }));
    } catch {
      set({ loading: false });
    }
  },

  loadPrices: async () => {
    try {
      const newPrices = await api.fetchAllPrices();
      set((state) => ({
        previousPrices: { ...state.prices },
        prices: mergePrices(state.prices, newPrices),
      }));
    } catch {
      // ignore
    }
  },

  loadSummaryPrices: async () => {
    try {
      const summaryPrices = await api.fetchSummaryPrices();
      set((state) => ({
        previousPrices: { ...state.prices },
        prices: mergePrices(state.prices, summaryPrices),
      }));
    } catch {
      // ignore
    }
  },

  updatePrices: (newPrices) => {
    set((state) => ({
      previousPrices: { ...state.prices },
      prices: mergePrices(state.prices, newPrices),
    }));
  },

  selectSymbol: (symbol) => set({ selectedSymbol: symbol }),

  loadStockDetail: async (symbol) => {
    try {
      const data = await api.fetchStockDetail(symbol);
      set((state) => {
        const trades = data.recentTrades.length > 0 ? data.recentTrades : state.recentTrades;
        let price = data.currentPrice;
        if (price === 0 && trades.length > 0) {
          const sorted = [...trades].sort(
            (a, b) => new Date(b.tradeTimestamp).getTime() - new Date(a.tradeTimestamp).getTime()
          );
          price = sorted[0].price;
        }
        if (price === 0 && state.orderBook?.currentPrice) {
          price = state.orderBook.currentPrice;
        }
        return {
          orderBook: { ...data, currentPrice: price },
          recentTrades: trades,
          prices: mergePrices(state.prices, { [symbol]: price }),
        };
      });
    } catch {
      // ignore
    }
  },

  loadTrades: async (symbol) => {
    try {
      const trades = await api.fetchStockTrades(symbol);
      if (trades.length > 0) {
        set({ recentTrades: trades });
      }
    } catch {
      // ignore
    }
  },

  updateOrderBook: (data) => {
    set((state) => {
      const trades = data.recentTrades.length > 0 ? data.recentTrades : state.recentTrades;
      let price = data.currentPrice;
      if (price === 0 && trades.length > 0) {
        const sorted = [...trades].sort(
          (a, b) => new Date(b.tradeTimestamp).getTime() - new Date(a.tradeTimestamp).getTime()
        );
        price = sorted[0].price;
      }
      if (price === 0 && state.orderBook?.currentPrice) {
        price = state.orderBook.currentPrice;
      }
      return {
        orderBook: { ...data, currentPrice: price },
        recentTrades: trades,
        prices: mergePrices(state.prices, { [data.symbol]: price }),
      };
    });
  },

  toggleWatchlist: (symbol) => {
    set((state) => {
      const next = state.watchlist.includes(symbol)
        ? state.watchlist.filter((s) => s !== symbol)
        : [...state.watchlist, symbol];
      localStorage.setItem('watchlist', JSON.stringify(next));
      return { watchlist: next };
    });
  },
}));
