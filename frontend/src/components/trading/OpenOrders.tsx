import { useState, useEffect } from 'react';
import { useAuthStore } from '../../stores/authStore';
import { fetchUserOrders } from '../../services/api';
import { Order } from '../../types';
import clsx from 'clsx';

interface Props {
  symbol: string;
}

export default function OpenOrders({ symbol }: Props) {
  const user = useAuthStore((s) => s.user);
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!user) return;
    setLoading(true);
    fetchUserOrders()
      .then((all) => {
        const filtered = all.filter(
          (o) => o.symbol === symbol && (o.status === 'PENDING' || o.status === 'PARTIALLY_FILLED')
        );
        setOrders(filtered);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [user, symbol]);

  if (!user) {
    return (
      <div className="panel flex flex-col h-full">
        <div className="panel-header">My Orders</div>
        <div className="flex-1 flex items-center justify-center text-xs text-gray-500">
          Sign in to view your orders
        </div>
      </div>
    );
  }

  return (
    <div className="panel flex flex-col h-full">
      <div className="panel-header flex items-center justify-between">
        <span>My Orders</span>
        {orders.length > 0 && (
          <span className="text-2xs bg-accent/20 text-accent px-1.5 py-0.5 rounded font-mono">
            {orders.length}
          </span>
        )}
      </div>

      <div className="flex-1 overflow-auto">
        {loading ? (
          <div className="flex items-center justify-center py-8">
            <div className="w-4 h-4 border-2 border-accent/30 border-t-accent rounded-full animate-spin" />
          </div>
        ) : orders.length === 0 ? (
          <div className="flex items-center justify-center py-8 text-xs text-gray-600">
            No open orders for {symbol}
          </div>
        ) : (
          <div className="divide-y divide-panel-border/50">
            {orders.map((order) => (
              <div key={order.orderId} className="px-3 py-2 hover:bg-panel-lighter/50 transition-colors">
                <div className="flex items-center justify-between mb-1">
                  <div className="flex items-center gap-2">
                    <span
                      className={clsx(
                        'text-2xs font-bold px-1.5 py-0.5 rounded',
                        order.side === 'BUY'
                          ? 'bg-bull-dim text-bull-text'
                          : 'bg-bear-dim text-bear-text'
                      )}
                    >
                      {order.side}
                    </span>
                    <span className="text-xs text-gray-300">{order.type}</span>
                  </div>
                  <span
                    className={clsx(
                      'text-2xs px-1.5 py-0.5 rounded',
                      order.status === 'PARTIALLY_FILLED'
                        ? 'bg-yellow-500/10 text-yellow-400'
                        : 'bg-accent/10 text-accent'
                    )}
                  >
                    {order.status}
                  </span>
                </div>
                <div className="flex items-center justify-between text-xs font-mono tabular-nums">
                  <span className="text-gray-400">
                    {order.currentQuantity ?? order.quantity} / {order.originalQuantity ?? order.quantity}
                  </span>
                  <span className="text-gray-300">${order.price.toFixed(2)}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
