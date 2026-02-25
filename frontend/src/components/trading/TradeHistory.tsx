import { Trade } from '../../types';
import { useRef, useEffect } from 'react';
import clsx from 'clsx';

interface Props {
  trades: Trade[];
}

export default function TradeHistory({ trades }: Props) {
  const listRef = useRef<HTMLDivElement>(null);
  const prevCountRef = useRef(trades.length);

  useEffect(() => {
    if (trades.length > prevCountRef.current && listRef.current) {
      listRef.current.scrollTop = 0;
    }
    prevCountRef.current = trades.length;
  }, [trades.length]);

  const sorted = [...trades].sort(
    (a, b) => new Date(b.tradeTimestamp).getTime() - new Date(a.tradeTimestamp).getTime()
  );

  return (
    <div className="panel flex flex-col h-full">
      <div className="panel-header">Recent Trades</div>

      <div className="flex items-center px-3 py-1.5 text-2xs text-gray-600 border-b border-panel-border">
        <span className="w-1/3">PRICE</span>
        <span className="w-1/3 text-right">QTY</span>
        <span className="w-1/3 text-right">TIME</span>
      </div>

      <div ref={listRef} className="flex-1 overflow-auto">
        {sorted.length === 0 ? (
          <div className="flex items-center justify-center py-8 text-xs text-gray-600">
            No trades yet
          </div>
        ) : (
          sorted.slice(0, 50).map((trade, i) => {
            const prevTrade = sorted[i + 1];
            const direction = prevTrade
              ? trade.price > prevTrade.price ? 'up' : trade.price < prevTrade.price ? 'down' : 'neutral'
              : 'neutral';

            const time = new Date(trade.tradeTimestamp);
            const timeStr = time.toLocaleTimeString('en-US', {
              hour12: false,
              hour: '2-digit',
              minute: '2-digit',
              second: '2-digit',
            });

            return (
              <div
                key={trade.tradeId}
                className={clsx(
                  'flex items-center px-3 py-1 text-xs font-mono tabular-nums',
                  i === 0 && 'animate-flash-green'
                )}
              >
                <span
                  className={clsx(
                    'w-1/3',
                    direction === 'up' ? 'text-bull-text' : direction === 'down' ? 'text-bear-text' : 'text-gray-300'
                  )}
                >
                  ${trade.price.toFixed(2)}
                </span>
                <span className="w-1/3 text-right text-gray-300">{trade.quantity}</span>
                <span className="w-1/3 text-right text-gray-500">{timeStr}</span>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
