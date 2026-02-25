import { useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useMarketStore } from '../stores/marketStore';
import { useOrderBookSocket } from '../hooks/useWebSocket';
import SymbolSelector from '../components/trading/SymbolSelector';
import OrderBook from '../components/trading/OrderBook';
import OrderForm from '../components/trading/OrderForm';
import PriceChart from '../components/trading/PriceChart';
import TradeHistory from '../components/trading/TradeHistory';
import OpenOrders from '../components/trading/OpenOrders';
import Spinner from '../components/common/Spinner';

export default function TradingPage() {
  const { symbol } = useParams<{ symbol: string }>();
  const navigate = useNavigate();
  const { orderBook, recentTrades, loadStockDetail, loadTrades, loadStocks, stocks } = useMarketStore();

  const activeSymbol = symbol?.toUpperCase() || 'AAPL';

  useOrderBookSocket(activeSymbol);

  useEffect(() => {
    if (stocks.length === 0) loadStocks();
  }, [stocks.length, loadStocks]);

  useEffect(() => {
    loadStockDetail(activeSymbol);
    loadTrades(activeSymbol);
    const obInterval = setInterval(() => loadStockDetail(activeSymbol), 3000);
    const tradeInterval = setInterval(() => loadTrades(activeSymbol), 3000);
    return () => {
      clearInterval(obInterval);
      clearInterval(tradeInterval);
    };
  }, [activeSymbol, loadStockDetail, loadTrades]);

  const handlePriceClick = useCallback((price: number) => {
    const priceInput = document.querySelector<HTMLInputElement>('input[step="0.01"]');
    if (priceInput) {
      const nativeInputValueSetter = Object.getOwnPropertyDescriptor(
        window.HTMLInputElement.prototype, 'value'
      )?.set;
      nativeInputValueSetter?.call(priceInput, price.toFixed(2));
      priceInput.dispatchEvent(new Event('input', { bubbles: true }));
    }
  }, []);

  if (!orderBook) {
    return <Spinner className="h-full" />;
  }

  return (
    <div className="h-full flex flex-col overflow-hidden">
      {/* Symbol header */}
      <div className="flex-shrink-0 px-4 py-2 border-b border-panel-border bg-panel-light flex items-center gap-4">
        <SymbolSelector currentSymbol={activeSymbol} />

        <div className="flex items-center gap-6 ml-4">
          <div>
            <div className="text-lg font-bold font-mono tabular-nums">${orderBook.currentPrice.toFixed(2)}</div>
          </div>

          <div className="h-8 w-px bg-panel-border" />

          <div className="flex gap-6">
            <div>
              <div className="text-2xs text-gray-500 uppercase">Bid</div>
              <div className="text-sm font-mono text-bull-text">
                ${orderBook.bestBidPrice.toFixed(2)}
                <span className="text-2xs text-gray-500 ml-1">x{orderBook.bestBidQuantity}</span>
              </div>
            </div>
            <div>
              <div className="text-2xs text-gray-500 uppercase">Ask</div>
              <div className="text-sm font-mono text-bear-text">
                ${orderBook.bestAskPrice.toFixed(2)}
                <span className="text-2xs text-gray-500 ml-1">x{orderBook.bestAskQuantity}</span>
              </div>
            </div>
            <div>
              <div className="text-2xs text-gray-500 uppercase">Spread</div>
              <div className="text-sm font-mono text-gray-300">
                ${(orderBook.bestAskPrice - orderBook.bestBidPrice).toFixed(2)}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Trading grid */}
      <div className="flex-1 grid grid-cols-12 grid-rows-2 gap-1 p-1 min-h-0">
        {/* Order Book - left column */}
        <div className="col-span-3 row-span-2">
          <OrderBook
            bids={orderBook.topBuys}
            asks={orderBook.lowestSells}
            currentPrice={orderBook.currentPrice}
            onPriceClick={handlePriceClick}
          />
        </div>

        {/* Price Chart - center top */}
        <div className="col-span-6 row-span-1">
          <PriceChart
            symbol={activeSymbol}
            trades={recentTrades}
            currentPrice={orderBook.currentPrice}
          />
        </div>

        {/* Order Form - right top */}
        <div className="col-span-3 row-span-1">
          <OrderForm
            symbol={activeSymbol}
            currentPrice={orderBook.currentPrice}
            bestBid={orderBook.bestBidPrice}
            bestAsk={orderBook.bestAskPrice}
            onOrderSubmitted={() => { loadStockDetail(activeSymbol); loadTrades(activeSymbol); }}
          />
        </div>

        {/* Trade History - center bottom */}
        <div className="col-span-3 row-span-1">
          <TradeHistory trades={recentTrades} />
        </div>

        {/* Open Orders - right bottom */}
        <div className="col-span-3 row-span-1">
          <OpenOrders symbol={activeSymbol} />
        </div>
      </div>
    </div>
  );
}
