import { useMemo } from 'react';
import { Order } from '../../types';
import clsx from 'clsx';

interface Props {
  bids: Order[];
  asks: Order[];
  currentPrice: number;
  spread?: number;
  onPriceClick?: (price: number) => void;
}

export default function OrderBook({ bids, asks, currentPrice, onPriceClick }: Props) {
  const maxQty = useMemo(() => {
    const allQty = [...bids, ...asks].map((o) => o.currentQuantity ?? o.quantity);
    return Math.max(...allQty, 1);
  }, [bids, asks]);

  const sortedAsks = useMemo(
    () => [...asks].sort((a, b) => b.price - a.price).slice(0, 12),
    [asks]
  );

  const sortedBids = useMemo(
    () => [...bids].sort((a, b) => b.price - a.price).slice(0, 12),
    [bids]
  );

  const spreadValue = sortedAsks.length > 0 && sortedBids.length > 0
    ? (sortedAsks[sortedAsks.length - 1].price - sortedBids[0].price)
    : 0;

  const Row = ({ order, side }: { order: Order; side: 'buy' | 'sell' }) => {
    const qty = order.currentQuantity ?? order.quantity;
    const pct = (qty / maxQty) * 100;
    const isBuy = side === 'buy';

    return (
      <div
        onClick={() => onPriceClick?.(order.price)}
        className="relative flex items-center px-3 py-0.5 cursor-pointer hover:bg-panel-lighter/50 text-xs font-mono tabular-nums group"
      >
        <div
          className={clsx(
            'absolute inset-y-0 h-full opacity-15',
            isBuy ? 'right-0 bg-bull' : 'left-0 bg-bear'
          )}
          style={{ width: `${pct}%` }}
        />
        <span className="w-1/3 text-right pr-3 text-gray-400 relative z-10">{qty}</span>
        <span
          className={clsx(
            'w-1/3 text-center relative z-10 font-medium',
            isBuy ? 'text-bull-text' : 'text-bear-text'
          )}
        >
          {order.price.toFixed(2)}
        </span>
        <span className="w-1/3 text-left pl-3 text-gray-500 relative z-10">
          {(order.price * qty).toFixed(0)}
        </span>
      </div>
    );
  };

  return (
    <div className="panel flex flex-col h-full">
      <div className="panel-header flex items-center justify-between">
        <span>Order Book</span>
      </div>

      <div className="flex items-center px-3 py-1.5 text-2xs text-gray-600 border-b border-panel-border">
        <span className="w-1/3 text-right pr-3">QTY</span>
        <span className="w-1/3 text-center">PRICE</span>
        <span className="w-1/3 text-left pl-3">TOTAL</span>
      </div>

      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Asks (sells) - lowest at bottom */}
        <div className="flex-1 overflow-auto flex flex-col justify-end">
          {sortedAsks.map((order, i) => (
            <Row key={`ask-${order.orderId ?? i}`} order={order} side="sell" />
          ))}
        </div>

        {/* Spread / current price */}
        <div className="px-3 py-2 border-y border-panel-border bg-panel-light flex items-center justify-between">
          <span className="text-sm font-bold font-mono tabular-nums text-white">
            ${currentPrice.toFixed(2)}
          </span>
          {spreadValue > 0 && (
            <span className="text-2xs text-gray-500">
              Spread: ${spreadValue.toFixed(2)}
            </span>
          )}
        </div>

        {/* Bids (buys) - highest at top */}
        <div className="flex-1 overflow-auto">
          {sortedBids.map((order, i) => (
            <Row key={`bid-${order.orderId ?? i}`} order={order} side="buy" />
          ))}
        </div>
      </div>
    </div>
  );
}
