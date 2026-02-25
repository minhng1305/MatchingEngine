import { useMarketStore } from '../../stores/marketStore';
import clsx from 'clsx';

export default function MarketTicker() {
  const { stocks, prices, previousPrices } = useMarketStore();

  if (stocks.length === 0) return null;

  const items = stocks.map((s) => {
    const price = prices[s.symbol] ?? s.currentPrice;
    const prev = previousPrices[s.symbol] ?? s.currentPrice;
    const change = prev ? ((price - prev) / prev) * 100 : 0;
    return { symbol: s.symbol, price, change };
  });

  const doubled = [...items, ...items];

  return (
    <div className="w-full overflow-hidden bg-panel-light border-b border-panel-border">
      <div className="flex animate-[scroll_60s_linear_infinite] whitespace-nowrap py-2">
        {doubled.map((item, i) => (
          <div key={`${item.symbol}-${i}`} className="flex items-center gap-2 mx-4 flex-shrink-0">
            <span className="text-xs font-semibold text-gray-300">{item.symbol}</span>
            <span className="text-xs font-mono tabular-nums text-gray-200">
              ${item.price.toFixed(2)}
            </span>
            <span
              className={clsx(
                'text-2xs font-mono tabular-nums',
                item.change > 0 ? 'text-bull-text' : item.change < 0 ? 'text-bear-text' : 'text-gray-500'
              )}
            >
              {item.change > 0 ? '+' : ''}{item.change.toFixed(2)}%
            </span>
          </div>
        ))}
      </div>

      <style>{`
        @keyframes scroll {
          0% { transform: translateX(0); }
          100% { transform: translateX(-50%); }
        }
      `}</style>
    </div>
  );
}
