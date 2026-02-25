import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { OrderBookSummary, PriceUpdate } from '../types';
import { getServerForSymbol, SERVERS } from '../config/servers';

type Unsubscribe = () => void;

const clients = new Map<string, Client>();

function getOrCreateClient(wsUrl: string): Client {
  const existing = clients.get(wsUrl);
  if (existing?.connected) return existing;

  const client = new Client({
    webSocketFactory: () => new SockJS(wsUrl) as any,
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
  });

  clients.set(wsUrl, client);
  return client;
}

function ensureConnected(client: Client): Promise<void> {
  if (client.connected) return Promise.resolve();
  return new Promise((resolve, reject) => {
    const prevOnConnect = client.onConnect;
    client.onConnect = (frame) => {
      prevOnConnect?.(frame);
      resolve();
    };
    client.onStompError = (frame) => {
      reject(new Error(frame.headers['message'] || 'STOMP error'));
    };
    if (!client.active) client.activate();
  });
}

export async function subscribeOrderBook(
  symbol: string,
  callback: (data: OrderBookSummary) => void
): Promise<Unsubscribe> {
  const server = getServerForSymbol(symbol);
  const client = getOrCreateClient(server.wsUrl);
  await ensureConnected(client);

  const sub = client.subscribe(`/topic/orderbook-updates/${symbol}`, (msg: IMessage) => {
    try {
      const data = JSON.parse(msg.body);
      callback({
        symbol: data.symbol,
        topBuys: data.topBuyOrders ?? data.topBuys ?? [],
        lowestSells: data.topSellOrders ?? data.lowestSells ?? [],
        currentPrice: data.currentPrice,
        bestBidPrice: data.bestBidPrice,
        bestBidQuantity: data.bestBidQuantity,
        bestAskPrice: data.bestAskPrice,
        bestAskQuantity: data.bestAskQuantity,
        recentTrades: data.recentTrades ?? [],
      });
    } catch {
      // ignore malformed messages
    }
  });

  return () => sub.unsubscribe();
}

export async function subscribePriceUpdates(
  callback: (update: PriceUpdate) => void
): Promise<Unsubscribe> {
  const unsubs: Unsubscribe[] = [];
  const connectedServers = new Set<string>();

  for (const server of SERVERS) {
    try {
      const client = getOrCreateClient(server.wsUrl);
      await ensureConnected(client);
      connectedServers.add(server.id);
      const sub = client.subscribe('/topic/price-updates', (msg: IMessage) => {
        try {
          callback(JSON.parse(msg.body));
        } catch {
          // ignore
        }
      });
      unsubs.push(() => sub.unsubscribe());
    } catch {
      // server unavailable, continue
    }
  }

  return () => unsubs.forEach((u) => u());
}

export function disconnectAll(): void {
  clients.forEach((client) => {
    if (client.active) client.deactivate();
  });
  clients.clear();
}
