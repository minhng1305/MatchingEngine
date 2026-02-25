import { useMarketStore } from '../../stores/marketStore';
import { useMemo } from 'react';
import { TrendingUp, TrendingDown, BarChart3, Zap } from 'lucide-react';

export default function MarketStats() {
  const { stocks, prices, previousPrices } = useMarketStore();

  const stats = useMemo(() => {
    let gainers = 0;
    let losers = 0;
    let topGainer = { symbol: '-', change: 0 };
    let topLoser = { symbol: '-', change: 0 };

    stocks.forEach((s) => {
      const price = prices[s.symbol] ?? s.currentPrice;
      const prev = previousPrices[s.symbol] ?? s.currentPrice;
      const change = prev ? ((price - prev) / prev) * 100 : 0;
      if (change > 0) gainers++;
      if (change < 0) losers++;
      if (change > topGainer.change) topGainer = { symbol: s.symbol, change };
      if (change < topLoser.change) topLoser = { symbol: s.symbol, change };
    });

    return { gainers, losers, unchanged: stocks.length - gainers - losers, topGainer, topLoser };
  }, [stocks, prices, previousPrices]);

  const cards = [
    {
      label: 'Total Instruments',
      value: stocks.length.toString(),
      icon: BarChart3,
      color: 'text-accent',
    },
    {
      label: 'Gainers',
      value: stats.gainers.toString(),
      icon: TrendingUp,
      color: 'text-bull-text',
    },
    {
      label: 'Losers',
      value: stats.losers.toString(),
      icon: TrendingDown,
      color: 'text-bear-text',
    },
    {
      label: 'Top Mover',
      value: stats.topGainer.symbol,
      sub: `${stats.topGainer.change > 0 ? '+' : ''}${stats.topGainer.change.toFixed(2)}%`,
      icon: Zap,
      color: 'text-yellow-400',
    },
  ];

  return (
    <div className="grid grid-cols-4 gap-3">
      {cards.map((card) => (
        <div key={card.label} className="panel px-4 py-3">
          <div className="flex items-center justify-between mb-1">
            <span className="text-2xs uppercase tracking-wider text-gray-500">{card.label}</span>
            <card.icon className={`w-3.5 h-3.5 ${card.color}`} />
          </div>
          <div className="text-xl font-bold font-mono">{card.value}</div>
          {card.sub && <div className={`text-xs font-mono ${card.color}`}>{card.sub}</div>}
        </div>
      ))}
    </div>
  );
}
