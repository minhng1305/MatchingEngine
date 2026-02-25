import { useState } from 'react';
import { useAuthStore } from '../../stores/authStore';
import { submitOrder } from '../../services/api';
import { OrderSide, OrderType } from '../../types';
import clsx from 'clsx';
import { useNavigate } from 'react-router-dom';

interface Props {
  symbol: string;
  currentPrice: number;
  bestBid: number;
  bestAsk: number;
  onOrderSubmitted?: () => void;
}

export default function OrderForm({ symbol, currentPrice, bestBid, bestAsk, onOrderSubmitted }: Props) {
  const user = useAuthStore((s) => s.user);
  const navigate = useNavigate();
  const [side, setSide] = useState<OrderSide>('BUY');
  const [type, setType] = useState<OrderType>('LIMIT');
  const [price, setPrice] = useState(currentPrice.toFixed(2));
  const [quantity, setQuantity] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; msg: string } | null>(null);

  const isBuy = side === 'BUY';
  const total = (parseFloat(price || '0') * parseFloat(quantity || '0'));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) {
      navigate('/login');
      return;
    }

    setSubmitting(true);
    setFeedback(null);

    try {
      const res = await submitOrder({
        symbol,
        side,
        type,
        price: type === 'MARKET' ? currentPrice : parseFloat(price),
        quantity: parseInt(quantity, 10),
        userId: user.userId,
      });

      if (res.success) {
        setFeedback({ type: 'success', msg: `Order ${res.orderId?.slice(0, 8)}... submitted` });
        setQuantity('');
        onOrderSubmitted?.();
      } else {
        setFeedback({ type: 'error', msg: res.message || 'Order rejected' });
      }
    } catch (err: any) {
      setFeedback({ type: 'error', msg: err.message || 'Failed to submit order' });
    } finally {
      setSubmitting(false);
      setTimeout(() => setFeedback(null), 4000);
    }
  };

  const setMarketPrice = (p: number) => {
    if (p > 0) setPrice(p.toFixed(2));
  };

  return (
    <div className="panel flex flex-col">
      <div className="panel-header">Place Order</div>

      <div className="p-4 space-y-4">
        {/* Side toggle */}
        <div className="grid grid-cols-2 gap-1 bg-panel-lighter rounded-lg p-1">
          <button
            onClick={() => setSide('BUY')}
            className={clsx(
              'py-2 text-sm font-bold rounded-md transition-all',
              isBuy ? 'bg-bull text-white glow-green' : 'text-gray-400 hover:text-gray-200'
            )}
          >
            BUY
          </button>
          <button
            onClick={() => setSide('SELL')}
            className={clsx(
              'py-2 text-sm font-bold rounded-md transition-all',
              !isBuy ? 'bg-bear text-white glow-red' : 'text-gray-400 hover:text-gray-200'
            )}
          >
            SELL
          </button>
        </div>

        {/* Type toggle */}
        <div className="flex gap-2">
          {(['LIMIT', 'MARKET'] as OrderType[]).map((t) => (
            <button
              key={t}
              onClick={() => setType(t)}
              className={clsx('tab flex-1 text-center', type === t && 'tab-active')}
            >
              {t}
            </button>
          ))}
        </div>

        <form onSubmit={handleSubmit} className="space-y-3">
          {/* Price */}
          {type === 'LIMIT' && (
            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="text-2xs text-gray-500 uppercase">Price</label>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => setMarketPrice(bestBid)}
                    className="text-2xs text-bull-text/70 hover:text-bull-text"
                  >
                    Bid
                  </button>
                  <button
                    type="button"
                    onClick={() => setMarketPrice(bestAsk)}
                    className="text-2xs text-bear-text/70 hover:text-bear-text"
                  >
                    Ask
                  </button>
                </div>
              </div>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500 text-sm">$</span>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  value={price}
                  onChange={(e) => setPrice(e.target.value)}
                  className="input-field pl-7 font-mono"
                  required
                />
              </div>
            </div>
          )}

          {/* Quantity */}
          <div>
            <label className="text-2xs text-gray-500 uppercase mb-1 block">Quantity</label>
            <input
              type="number"
              min="1"
              step="1"
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              className="input-field font-mono"
              placeholder="0"
              required
            />
            <div className="flex gap-1 mt-1.5">
              {[10, 25, 50, 100].map((q) => (
                <button
                  key={q}
                  type="button"
                  onClick={() => setQuantity(q.toString())}
                  className="flex-1 text-2xs py-1 bg-panel-lighter hover:bg-panel-border rounded text-gray-400 transition-colors"
                >
                  {q}
                </button>
              ))}
            </div>
          </div>

          {/* Total estimate */}
          <div className="flex items-center justify-between py-2 border-t border-panel-border">
            <span className="text-xs text-gray-500">Estimated Total</span>
            <span className="text-sm font-mono font-medium text-gray-200">
              ${total.toFixed(2)}
            </span>
          </div>

          {/* Submit */}
          <button
            type="submit"
            disabled={submitting || !quantity}
            className={clsx('w-full py-3 text-sm font-bold rounded-md transition-all', isBuy ? 'btn-buy' : 'btn-sell')}
          >
            {submitting ? (
              <span className="flex items-center justify-center gap-2">
                <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                Submitting...
              </span>
            ) : (
              `${side} ${symbol}`
            )}
          </button>
        </form>

        {/* Feedback */}
        {feedback && (
          <div
            className={clsx(
              'p-2.5 rounded-md text-xs animate-fade-in',
              feedback.type === 'success'
                ? 'bg-bull-dim border border-bull/30 text-bull-text'
                : 'bg-bear-dim border border-bear/30 text-bear-text'
            )}
          >
            {feedback.msg}
          </div>
        )}
      </div>
    </div>
  );
}
