import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMarketStore } from '../../stores/marketStore';
import { Search, ChevronDown } from 'lucide-react';
import clsx from 'clsx';

interface Props {
  currentSymbol: string;
}

export default function SymbolSelector({ currentSymbol }: Props) {
  const { stocks, prices } = useMarketStore();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState('');
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const currentStock = stocks.find((s) => s.symbol === currentSymbol);
  const filtered = stocks.filter((s) => {
    if (!search) return true;
    const q = search.toLowerCase();
    return s.symbol.toLowerCase().includes(q) || s.companyName.toLowerCase().includes(q);
  });

  return (
    <div ref={ref} className="relative">
      <button
        onClick={() => setOpen(!open)}
        className="flex items-center gap-2 px-3 py-2 bg-panel-lighter border border-panel-border rounded-lg hover:border-accent/50 transition-colors"
      >
        <span className="text-lg font-bold">{currentSymbol}</span>
        {currentStock && (
          <span className="text-xs text-gray-400 hidden sm:inline">{currentStock.companyName}</span>
        )}
        <ChevronDown className={clsx('w-4 h-4 text-gray-500 transition-transform', open && 'rotate-180')} />
      </button>

      {open && (
        <div className="absolute top-full left-0 mt-1 w-80 bg-panel border border-panel-border rounded-lg shadow-2xl z-50 animate-fade-in">
          <div className="p-2 border-b border-panel-border">
            <div className="relative">
              <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-gray-500" />
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="input-field pl-8 text-xs"
                placeholder="Search symbol or name..."
                autoFocus
              />
            </div>
          </div>
          <div className="max-h-64 overflow-auto py-1">
            {filtered.map((stock) => (
              <button
                key={stock.symbol}
                onClick={() => {
                  navigate(`/trade/${stock.symbol}`);
                  setOpen(false);
                  setSearch('');
                }}
                className={clsx(
                  'w-full flex items-center justify-between px-3 py-2 hover:bg-panel-lighter text-left transition-colors',
                  stock.symbol === currentSymbol && 'bg-accent/10'
                )}
              >
                <div>
                  <span className="text-sm font-semibold text-white">{stock.symbol}</span>
                  <span className="text-xs text-gray-500 ml-2">{stock.companyName}</span>
                </div>
                <span className="text-xs font-mono text-gray-300">
                  ${(prices[stock.symbol] ?? stock.currentPrice).toFixed(2)}
                </span>
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
