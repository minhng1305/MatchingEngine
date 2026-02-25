import { AuthResponse, Order, OrderBookSummary, Stock, Trade, UserProfile } from '../types';
import { DEFAULT_API, getServerForSymbol, INGRESS_URL, SERVERS } from '../config/servers';

function getToken(): string | null {
  return sessionStorage.getItem('token');
}

async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...((options.headers as Record<string, string>) || {}),
  };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(url, { ...options, headers });
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(text || `Request failed: ${res.status}`);
  }
  return res.json();
}

// ── Auth ──────────────────────────────────────────────
export async function login(username: string, password: string): Promise<AuthResponse> {
  return request<AuthResponse>(`${DEFAULT_API}/auth/login`, {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
}

export async function register(username: string, email: string, password: string): Promise<AuthResponse> {
  return request<AuthResponse>(`${DEFAULT_API}/auth/register`, {
    method: 'POST',
    body: JSON.stringify({ username, email, password }),
  });
}

// ── Stocks ────────────────────────────────────────────
export async function fetchAllStocks(): Promise<Stock[]> {
  const results = await Promise.allSettled(
    SERVERS.map((s) => request<Stock[]>(`${s.baseUrl}/stocks/all`))
  );

  // First pass: collect stocks from their assigned (authoritative) server
  const stockMap = new Map<string, Stock>();
  results.forEach((r, idx) => {
    if (r.status !== 'fulfilled') return;
    const owned = new Set(SERVERS[idx].symbols);
    for (const stock of r.value) {
      if (owned.has(stock.symbol)) stockMap.set(stock.symbol, stock);
    }
  });

  // Second pass: fill in any missing symbols from whatever server has them
  results.forEach((r) => {
    if (r.status !== 'fulfilled') return;
    for (const stock of r.value) {
      if (!stockMap.has(stock.symbol)) stockMap.set(stock.symbol, stock);
    }
  });

  return Array.from(stockMap.values());
}

export async function fetchStockDetail(symbol: string): Promise<OrderBookSummary> {
  const server = getServerForSymbol(symbol);
  const data = await request<any>(`${server.baseUrl}/stocks/${symbol}`);
  return {
    symbol: data.symbol,
    topBuys: data.topBuyOrders ?? data.topBuys ?? [],
    lowestSells: data.topSellOrders ?? data.lowestSells ?? [],
    currentPrice: data.currentPrice,
    bestBidPrice: data.bestBidPrice,
    bestBidQuantity: data.bestBidQuantity,
    bestAskPrice: data.bestAskPrice,
    bestAskQuantity: data.bestAskQuantity,
    recentTrades: data.recentTrades ?? [],
  };
}

// ── Orders ────────────────────────────────────────────
export async function submitOrder(order: {
  symbol: string;
  side: string;
  type: string;
  price: number;
  quantity: number;
  userId: string;
}): Promise<{ success: boolean; orderId: string; message: string }> {
  return request(`${INGRESS_URL}/orders/submit`, {
    method: 'POST',
    body: JSON.stringify(order),
  });
}

// ── Prices ────────────────────────────────────────────
export async function fetchAllPrices(): Promise<Record<string, number>> {
  const results = await Promise.allSettled(
    SERVERS.map((s) => request<{ prices: Record<string, number> }>(`${s.baseUrl}/prices/all`))
  );

  // First pass: take prices from each symbol's authoritative server
  const prices: Record<string, number> = {};
  results.forEach((r, idx) => {
    if (r.status !== 'fulfilled') return;
    const owned = new Set(SERVERS[idx].symbols);
    for (const [symbol, price] of Object.entries(r.value.prices)) {
      if (owned.has(symbol)) prices[symbol] = price;
    }
  });

  // Second pass: fill in missing symbols from any available server
  results.forEach((r) => {
    if (r.status !== 'fulfilled') return;
    for (const [symbol, price] of Object.entries(r.value.prices)) {
      if (!(symbol in prices)) prices[symbol] = price;
    }
  });

  return prices;
}

export async function fetchCurrentPrice(symbol: string): Promise<number> {
  const server = getServerForSymbol(symbol);
  const data = await request<{ currentPrice: number }>(`${server.baseUrl}/prices/current/${symbol}`);
  return data.currentPrice;
}

/**
 * Fetch correct prices via each stock's OrderBookSummary (which caches the
 * last trade price correctly, unlike getCurrentPrice() which resets to 0
 * after clearTradeRecords).
 */
export async function fetchSummaryPrices(): Promise<Record<string, number>> {
  const results = await Promise.allSettled(
    SERVERS.map((s) =>
      request<{ symbol: string; currentPrice: number }[]>(`${s.baseUrl}/stocks/all`).then((stocks) => ({
        server: s,
        stocks,
      }))
    )
  );

  const prices: Record<string, number> = {};
  const fetched = new Set<string>();

  // Collect all available servers & their stock lists
  const available: { server: typeof SERVERS[number]; stocks: { symbol: string; currentPrice: number }[] }[] = [];
  for (const r of results) {
    if (r.status === 'fulfilled') available.push(r.value);
  }

  // For each available server, fetch order book summaries for its owned symbols
  const detailPromises: Promise<void>[] = [];
  for (const { server } of available) {
    const symbolsToFetch = server.symbols.filter((s) => !fetched.has(s));
    for (const sym of symbolsToFetch) {
      fetched.add(sym);
      detailPromises.push(
        request<any>(`${server.baseUrl}/stocks/${sym}`)
          .then((data) => { if (data.currentPrice) prices[sym] = data.currentPrice; })
          .catch(() => {})
      );
    }
  }

  // Also fetch remaining symbols from any available server
  const allSymbols = SERVERS.flatMap((s) => s.symbols);
  if (available.length > 0) {
    const fallbackServer = available[0].server;
    for (const sym of allSymbols) {
      if (!fetched.has(sym)) {
        fetched.add(sym);
        detailPromises.push(
          request<any>(`${fallbackServer.baseUrl}/stocks/${sym}`)
            .then((data) => { if (data.currentPrice) prices[sym] = data.currentPrice; })
            .catch(() => {})
        );
      }
    }
  }

  await Promise.allSettled(detailPromises);
  return prices;
}

// ── Trades ────────────────────────────────────────────
export async function fetchStockTrades(symbol: string): Promise<Trade[]> {
  const server = getServerForSymbol(symbol);
  return request<Trade[]>(`${server.baseUrl}/stocks/${symbol}/trades`);
}

// ── User ──────────────────────────────────────────────
export async function fetchUserProfile(): Promise<UserProfile> {
  return request<UserProfile>(`${DEFAULT_API}/user/profile`);
}

export async function fetchUserOrders(): Promise<Order[]> {
  return request<Order[]>(`${DEFAULT_API}/user/orders`);
}

export async function fetchUserTrades(): Promise<Trade[]> {
  return request<Trade[]>(`${DEFAULT_API}/user/trades`);
}
