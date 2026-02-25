import { useEffect, useRef } from 'react';
import { subscribeOrderBook, subscribePriceUpdates } from '../services/websocket';
import { useMarketStore } from '../stores/marketStore';
import { OrderBookSummary } from '../types';

export function useOrderBookSocket(symbol: string | null) {
  const updateOrderBook = useMarketStore((s) => s.updateOrderBook);
  const unsubRef = useRef<(() => void) | null>(null);

  useEffect(() => {
    if (!symbol) return;
    let cancelled = false;

    subscribeOrderBook(symbol, (data: OrderBookSummary) => {
      if (!cancelled) updateOrderBook(data);
    }).then((unsub) => {
      if (cancelled) unsub();
      else unsubRef.current = unsub;
    }).catch(() => {});

    return () => {
      cancelled = true;
      unsubRef.current?.();
      unsubRef.current = null;
    };
  }, [symbol, updateOrderBook]);
}

export function usePriceSocket() {
  const updatePrices = useMarketStore((s) => s.updatePrices);
  const unsubRef = useRef<(() => void) | null>(null);

  useEffect(() => {
    let cancelled = false;

    subscribePriceUpdates((update) => {
      if (!cancelled) updatePrices(update.prices);
    }).then((unsub) => {
      if (cancelled) unsub();
      else unsubRef.current = unsub;
    }).catch(() => {});

    return () => {
      cancelled = true;
      unsubRef.current?.();
      unsubRef.current = null;
    };
  }, [updatePrices]);
}
