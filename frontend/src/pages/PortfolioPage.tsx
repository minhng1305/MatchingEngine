import { useEffect, useState } from 'react';
import { useAuthStore } from '../stores/authStore';
import { useMarketStore } from '../stores/marketStore';
import { fetchUserOrders, fetchUserTrades } from '../services/api';
import { Order, Trade } from '../types';
import {
  Wallet,
  TrendingUp,
  BarChart3,
  Clock,
  ArrowUpRight,
  ArrowDownRight,
  LogIn,
  AlertTriangle,
} from 'lucide-react';
import clsx from 'clsx';
import Spinner from '../components/common/Spinner';
import { useNavigate, Link } from 'react-router-dom';

type Tab = 'holdings' | 'orders' | 'trades';

export default function PortfolioPage() {
  const { user, profile, loadProfile } = useAuthStore();
  const { prices } = useMarketStore();
  const [tab, setTab] = useState<Tab>('holdings');
  const [orders, setOrders] = useState<Order[]>([]);
  const [trades, setTrades] = useState<Trade[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    if (!user) return;
    setLoading(true);
    setError(null);
    Promise.all([loadProfile(), fetchUserOrders(), fetchUserTrades()])
      .then(([_, o, t]) => {
        setOrders(o ?? []);
        setTrades(t ?? []);
      })
      .catch(() => {
        setError('Unable to load portfolio data. Please check that the backend is running.');
      })
      .finally(() => setLoading(false));
  }, [user, loadProfile]);

  if (!user) {
    return (
      <div className="h-full flex items-center justify-center">
        <div className="text-center space-y-4">
          <LogIn className="w-12 h-12 text-gray-600 mx-auto" />
          <div>
            <h2 className="text-lg font-semibold text-gray-200">Sign in to view your portfolio</h2>
            <p className="text-sm text-gray-500 mt-1">Track your holdings, orders, and trading history</p>
          </div>
          <Link to="/login" className="btn-primary inline-block">
            Sign In
          </Link>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="h-full flex items-center justify-center">
        <div className="text-center space-y-3">
          <div className="w-8 h-8 border-2 border-accent/30 border-t-accent rounded-full animate-spin mx-auto" />
          <p className="text-sm text-gray-500">Loading portfolio...</p>
        </div>
      </div>
    );
  }

  if (error || !profile) {
    return (
      <div className="h-full flex items-center justify-center">
        <div className="text-center space-y-4 max-w-md">
          <AlertTriangle className="w-12 h-12 text-yellow-500 mx-auto" />
          <div>
            <h2 className="text-lg font-semibold text-gray-200">Could not load portfolio</h2>
            <p className="text-sm text-gray-500 mt-1">
              {error || 'The server may be unavailable. Please try again later.'}
            </p>
          </div>
          <button onClick={() => window.location.reload()} className="btn-primary">
            Retry
          </button>
        </div>
      </div>
    );
  }

  const { account, statistics } = profile;
  const holdings = Array.isArray(account.holdings)
    ? account.holdings
    : account.holdings
      ? Object.entries(account.holdings).map(([symbol, quantity]) => ({ symbol, quantity: Number(quantity) }))
      : [];

  const holdingsValue = holdings.reduce((sum, h) => {
    const price = prices[h.symbol] ?? 0;
    return sum + price * h.quantity;
  }, 0);

  const totalValue = (account.availableBalance ?? 0) + holdingsValue;

  const statCards = [
    {
      label: 'Total Value',
      value: `$${totalValue.toFixed(2)}`,
      icon: Wallet,
      color: 'text-accent',
    },
    {
      label: 'Available Cash',
      value: `$${account.availableBalance.toFixed(2)}`,
      icon: BarChart3,
      color: 'text-bull-text',
    },
    {
      label: 'Holdings Value',
      value: `$${holdingsValue.toFixed(2)}`,
      icon: TrendingUp,
      color: 'text-yellow-400',
    },
    {
      label: 'Total Trades',
      value: statistics.totalTrades.toString(),
      sub: `$${statistics.totalTradeValue.toFixed(0)} volume`,
      icon: Clock,
      color: 'text-purple-400',
    },
  ];

  const sortedOrders = [...orders].sort(
    (a, b) => new Date(b.orderTimestamp || 0).getTime() - new Date(a.orderTimestamp || 0).getTime()
  );

  const sortedTrades = [...trades].sort(
    (a, b) => new Date(b.tradeTimestamp).getTime() - new Date(a.tradeTimestamp).getTime()
  );

  return (
    <div className="h-full overflow-auto p-4 space-y-4">
      {/* Stats */}
      <div className="grid grid-cols-4 gap-3">
        {statCards.map((card) => (
          <div key={card.label} className="panel px-4 py-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-2xs uppercase tracking-wider text-gray-500">{card.label}</span>
              <card.icon className={`w-4 h-4 ${card.color}`} />
            </div>
            <div className="text-xl font-bold font-mono">{card.value}</div>
            {card.sub && <div className="text-xs text-gray-500 mt-0.5">{card.sub}</div>}
          </div>
        ))}
      </div>

      {/* Tabs */}
      <div className="panel">
        <div className="flex border-b border-panel-border px-4 pt-2 gap-1">
          {([
            { key: 'holdings' as Tab, label: 'Holdings', count: holdings.length },
            { key: 'orders' as Tab, label: 'Orders', count: orders.length },
            { key: 'trades' as Tab, label: 'Trade History', count: trades.length },
          ]).map((t) => (
            <button
              key={t.key}
              onClick={() => setTab(t.key)}
              className={clsx(
                'px-4 py-2.5 text-xs font-medium border-b-2 transition-colors -mb-px',
                tab === t.key
                  ? 'border-accent text-white'
                  : 'border-transparent text-gray-500 hover:text-gray-300'
              )}
            >
              {t.label}
              <span className="ml-1.5 text-2xs bg-panel-lighter px-1.5 py-0.5 rounded-full">
                {t.count}
              </span>
            </button>
          ))}
        </div>

        <div className="p-0">
          {/* Holdings */}
          {tab === 'holdings' && (
            <table className="w-full">
              <thead>
                <tr className="border-b border-panel-border">
                  <th className="px-4 py-3 text-left text-2xs uppercase tracking-wider text-gray-500">Symbol</th>
                  <th className="px-4 py-3 text-right text-2xs uppercase tracking-wider text-gray-500">Quantity</th>
                  <th className="px-4 py-3 text-right text-2xs uppercase tracking-wider text-gray-500">Price</th>
                  <th className="px-4 py-3 text-right text-2xs uppercase tracking-wider text-gray-500">Value</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody>
                {holdings.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="px-4 py-8 text-center text-sm text-gray-600">
                      No holdings yet. Start trading to build your portfolio.
                    </td>
                  </tr>
                ) : (
                  holdings.map((h) => {
                    const price = prices[h.symbol] ?? 0;
                    const value = price * h.quantity;
                    return (
                      <tr
                        key={h.symbol}
                        className="border-b border-panel-border/50 hover:bg-panel-lighter cursor-pointer transition-colors"
                        onClick={() => navigate(`/trade/${h.symbol}`)}
                      >
                        <td className="px-4 py-3">
                          <span className="text-sm font-semibold text-white">{h.symbol}</span>
                        </td>
                        <td className="px-4 py-3 text-right font-mono text-sm text-gray-300">
                          {h.quantity}
                        </td>
                        <td className="px-4 py-3 text-right font-mono text-sm text-gray-300">
                          ${price.toFixed(2)}
                        </td>
                        <td className="px-4 py-3 text-right font-mono text-sm font-medium text-white">
                          ${value.toFixed(2)}
                        </td>
                        <td className="px-4 py-3 text-right">
                          <span className="text-xs text-accent">Trade →</span>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          )}

          {/* Orders */}
          {tab === 'orders' && (
            <table className="w-full">
              <thead>
                <tr className="border-b border-panel-border">
                  <th className="px-4 py-3 text-left text-2xs uppercase tracking-wider text-gray-500">Time</th>
                  <th className="px-4 py-3 text-left text-2xs uppercase tracking-wider text-gray-500">Symbol</th>
                  <th className="px-4 py-3 text-left text-2xs uppercase tracking-wider text-gray-500">Side</th>
                  <th className="px-4 py-3 text-left text-2xs uppercase tracking-wider text-gray-500">Type</th>
                  <th className="px-4 py-3 text-right text-2xs uppercase tracking-wider text-gray-500">Price</th>
                  <th className="px-4 py-3 text-right text-2xs uppercase tracking-wider text-gray-500">Qty</th>
                  <th className="px-4 py-3 text-right text-2xs uppercase tracking-wider text-gray-500">Status</th>
                </tr>
              </thead>
              <tbody>
                {sortedOrders.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="px-4 py-8 text-center text-sm text-gray-600">
                      No orders yet
                    </td>
                  </tr>
                ) : (
                  sortedOrders.slice(0, 50).map((o) => (
                    <tr key={o.orderId} className="border-b border-panel-border/50 hover:bg-panel-lighter transition-colors">
                      <td className="px-4 py-2.5 text-xs text-gray-500 font-mono">
                        {o.orderTimestamp
                          ? new Date(o.orderTimestamp).toLocaleString('en-US', {
                              month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
                            })
                          : '-'}
                      </td>
                      <td className="px-4 py-2.5 text-sm font-semibold text-white">{o.symbol}</td>
                      <td className="px-4 py-2.5">
                        <span className="flex items-center gap-1 text-xs">
                          {o.side === 'BUY' ? (
                            <ArrowUpRight className="w-3 h-3 text-bull-text" />
                          ) : (
                            <ArrowDownRight className="w-3 h-3 text-bear-text" />
                          )}
                          <span className={o.side === 'BUY' ? 'text-bull-text' : 'text-bear-text'}>
                            {o.side}
                          </span>
                        </span>
                      </td>
                      <td className="px-4 py-2.5 text-xs text-gray-400">{o.type}</td>
                      <td className="px-4 py-2.5 text-right font-mono text-xs text-gray-300">
                        ${o.price.toFixed(2)}
                      </td>
                      <td className="px-4 py-2.5 text-right font-mono text-xs text-gray-300">
                        {o.currentQuantity ?? o.quantity}/{o.originalQuantity ?? o.quantity}
                      </td>
                      <td className="px-4 py-2.5 text-right">
                        <span
                          className={clsx(
                            'text-2xs px-2 py-0.5 rounded-full font-medium',
                            o.status === 'FILLED' && 'bg-bull-dim text-bull-text',
                            o.status === 'PENDING' && 'bg-accent/10 text-accent',
                            o.status === 'PARTIALLY_FILLED' && 'bg-yellow-500/10 text-yellow-400',
                            o.status === 'CANCELED' && 'bg-gray-500/10 text-gray-500'
                          )}
                        >
                          {o.status}
                        </span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          )}

          {/* Trades */}
          {tab === 'trades' && (
            <table className="w-full">
              <thead>
                <tr className="border-b border-panel-border">
                  <th className="px-4 py-3 text-left text-2xs uppercase tracking-wider text-gray-500">Time</th>
                  <th className="px-4 py-3 text-left text-2xs uppercase tracking-wider text-gray-500">Symbol</th>
                  <th className="px-4 py-3 text-right text-2xs uppercase tracking-wider text-gray-500">Price</th>
                  <th className="px-4 py-3 text-right text-2xs uppercase tracking-wider text-gray-500">Quantity</th>
                  <th className="px-4 py-3 text-right text-2xs uppercase tracking-wider text-gray-500">Total</th>
                </tr>
              </thead>
              <tbody>
                {sortedTrades.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="px-4 py-8 text-center text-sm text-gray-600">
                      No trades yet
                    </td>
                  </tr>
                ) : (
                  sortedTrades.slice(0, 50).map((t) => (
                    <tr key={t.tradeId} className="border-b border-panel-border/50 hover:bg-panel-lighter transition-colors">
                      <td className="px-4 py-2.5 text-xs text-gray-500 font-mono">
                        {new Date(t.tradeTimestamp).toLocaleString('en-US', {
                          month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
                        })}
                      </td>
                      <td className="px-4 py-2.5 text-sm font-semibold text-white">{t.symbol}</td>
                      <td className="px-4 py-2.5 text-right font-mono text-xs text-gray-300">
                        ${t.price.toFixed(2)}
                      </td>
                      <td className="px-4 py-2.5 text-right font-mono text-xs text-gray-300">
                        {t.quantity}
                      </td>
                      <td className="px-4 py-2.5 text-right font-mono text-xs font-medium text-white">
                        ${(t.price * t.quantity).toFixed(2)}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}
