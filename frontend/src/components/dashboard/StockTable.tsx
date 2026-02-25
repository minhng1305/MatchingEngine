import { useNavigate } from 'react-router-dom';
import { useMarketStore } from '../../stores/marketStore';
import { Star, TrendingUp, TrendingDown } from 'lucide-react';
import clsx from 'clsx';
import { useState, useMemo } from 'react';

type SortKey = 'symbol' | 'price' | 'change' | 'esg';
type SortDir = 'asc' | 'desc';

export default function StockTable() {
  const { stocks, prices, previousPrices, watchlist, toggleWatchlist } = useMarketStore();
  const navigate = useNavigate();
  const [search, setSearch] = useState('');
  const [sortKey, setSortKey] = useState<SortKey>('symbol');
  const [sortDir, setSortDir] = useState<SortDir>('asc');
  const [filter, setFilter] = useState<'all' | 'watchlist'>('all');

  const enriched = useMemo(() => {
    return stocks.map((s) => {
      const price = prices[s.symbol] ?? s.currentPrice;
      const prev = previousPrices[s.symbol] ?? s.currentPrice;
      const change = prev ? ((price - prev) / prev) * 100 : 0;
      return { ...s, price, change, isWatched: watchlist.includes(s.symbol) };
    });
  }, [stocks, prices, previousPrices, watchlist]);

  const filtered = useMemo(() => {
    let list = enriched;
    if (filter === 'watchlist') list = list.filter((s) => s.isWatched);
    if (search) {
      const q = search.toLowerCase();
      list = list.filter((s) => s.symbol.toLowerCase().includes(q) || s.companyName.toLowerCase().includes(q));
    }
    list.sort((a, b) => {
      let cmp = 0;
      switch (sortKey) {
        case 'symbol': cmp = a.symbol.localeCompare(b.symbol); break;
        case 'price': cmp = a.price - b.price; break;
        case 'change': cmp = a.change - b.change; break;
        case 'esg': cmp = a.esgScore - b.esgScore; break;
      }
      return sortDir === 'asc' ? cmp : -cmp;
    });
    return list;
  }, [enriched, filter, search, sortKey, sortDir]);

  const toggleSort = (key: SortKey) => {
    if (sortKey === key) setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    else { setSortKey(key); setSortDir('asc'); }
  };

  const SortHeader = ({ label, field }: { label: string; field: SortKey }) => (
    <th
      onClick={() => toggleSort(field)}
      className="px-4 py-2.5 text-left text-2xs font-semibold uppercase tracking-wider text-gray-500 cursor-pointer hover:text-gray-300 select-none"
    >
      {label}
      {sortKey === field && <span className="ml-1">{sortDir === 'asc' ? '\u25B2' : '\u25BC'}</span>}
    </th>
  );

  return (
    <div className="panel flex flex-col h-full">
      <div className="px-4 py-3 border-b border-panel-border flex items-center gap-3">
        <input
          type="text"
          placeholder="Search stocks..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="input-field max-w-xs text-xs"
        />
        <div className="flex gap-1 ml-auto">
          <button
            onClick={() => setFilter('all')}
            className={clsx('tab', filter === 'all' && 'tab-active')}
          >
            All ({enriched.length})
          </button>
          <button
            onClick={() => setFilter('watchlist')}
            className={clsx('tab', filter === 'watchlist' && 'tab-active')}
          >
            Watchlist ({enriched.filter((s) => s.isWatched).length})
          </button>
        </div>
      </div>

      <div className="flex-1 overflow-auto">
        <table className="w-full">
          <thead className="sticky top-0 bg-panel z-10">
            <tr>
              <th className="w-10 px-3 py-2.5" />
              <SortHeader label="Symbol" field="symbol" />
              <th className="px-4 py-2.5 text-left text-2xs font-semibold uppercase tracking-wider text-gray-500">
                Company
              </th>
              <SortHeader label="Price" field="price" />
              <SortHeader label="Change" field="change" />
              <SortHeader label="ESG" field="esg" />
              <th className="px-4 py-2.5" />
            </tr>
          </thead>
          <tbody>
            {filtered.map((stock) => (
              <tr
                key={stock.symbol}
                onClick={() => navigate(`/trade/${stock.symbol}`)}
                className="border-b border-panel-border/50 hover:bg-panel-lighter cursor-pointer transition-colors group"
              >
                <td className="px-3 py-2.5">
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      toggleWatchlist(stock.symbol);
                    }}
                    className="text-gray-600 hover:text-yellow-400 transition-colors"
                  >
                    <Star
                      className={clsx(
                        'w-3.5 h-3.5',
                        stock.isWatched && 'fill-yellow-400 text-yellow-400'
                      )}
                    />
                  </button>
                </td>
                <td className="px-4 py-2.5">
                  <span className="text-sm font-semibold text-white">{stock.symbol}</span>
                </td>
                <td className="px-4 py-2.5">
                  <span className="text-xs text-gray-400">{stock.companyName}</span>
                </td>
                <td className="px-4 py-2.5">
                  <span className="text-sm font-mono tabular-nums text-gray-100">
                    ${stock.price.toFixed(2)}
                  </span>
                </td>
                <td className="px-4 py-2.5">
                  <div className="flex items-center gap-1">
                    {stock.change > 0 ? (
                      <TrendingUp className="w-3 h-3 text-bull-text" />
                    ) : stock.change < 0 ? (
                      <TrendingDown className="w-3 h-3 text-bear-text" />
                    ) : null}
                    <span
                      className={clsx(
                        'text-xs font-mono tabular-nums',
                        stock.change > 0 ? 'text-bull-text' : stock.change < 0 ? 'text-bear-text' : 'text-gray-500'
                      )}
                    >
                      {stock.change > 0 ? '+' : ''}{stock.change.toFixed(2)}%
                    </span>
                  </div>
                </td>
                <td className="px-4 py-2.5">
                  <div className="flex items-center gap-2">
                    <div className="w-16 h-1.5 bg-panel-lighter rounded-full overflow-hidden">
                      <div
                        className="h-full bg-gradient-to-r from-bear to-bull rounded-full"
                        style={{ width: `${stock.esgScore}%` }}
                      />
                    </div>
                    <span className="text-2xs text-gray-500 font-mono">{stock.esgScore}</span>
                  </div>
                </td>
                <td className="px-4 py-2.5 text-right">
                  <span className="text-xs text-accent opacity-0 group-hover:opacity-100 transition-opacity font-medium">
                    Trade →
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {filtered.length === 0 && (
          <div className="flex items-center justify-center py-12 text-sm text-gray-500">
            {search ? 'No stocks match your search' : 'No stocks in watchlist'}
          </div>
        )}
      </div>
    </div>
  );
}
