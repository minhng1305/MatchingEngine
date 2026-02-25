import { useEffect } from 'react';
import { useMarketStore } from '../stores/marketStore';
import { usePriceSocket } from '../hooks/useWebSocket';
import MarketTicker from '../components/dashboard/MarketTicker';
import MarketStats from '../components/dashboard/MarketStats';
import StockTable from '../components/dashboard/StockTable';
import Spinner from '../components/common/Spinner';

export default function DashboardPage() {
  const { loadStocks, loadSummaryPrices, loading, stocks } = useMarketStore();

  usePriceSocket();

  useEffect(() => {
    loadStocks().then(() => loadSummaryPrices());
    const interval = setInterval(loadSummaryPrices, 5000);
    return () => clearInterval(interval);
  }, [loadStocks, loadSummaryPrices]);

  if (loading && stocks.length === 0) {
    return <Spinner className="h-full" />;
  }

  return (
    <div className="h-full flex flex-col">
      <MarketTicker />
      <div className="flex-1 overflow-auto p-4 space-y-4">
        <MarketStats />
        <StockTable />
      </div>
    </div>
  );
}
