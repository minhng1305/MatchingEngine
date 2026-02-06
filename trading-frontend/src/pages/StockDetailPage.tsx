import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { OrderBookSummary, OrderBookSummaryApiResponse, Order } from '../types';
import { apiService } from '../services/api';
import { useWebSocket } from '../hooks/useWebSocket';
import OrderForm from '../components/OrderForm';
import OrderBook from '../components/OrderBook';
import TradeHistory from '../components/TradeHistory';
import PriceChart from '../components/PriceChart';
import UserOpenOrders from '../components/UserOpenOrders';

const StockDetailPage: React.FC = () => {
    const { symbol } = useParams<{ symbol: string }>();
    const navigate = useNavigate();

    const [stockData, setStockData] = useState<OrderBookSummary | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [orderSuccess, setOrderSuccess] = useState('');
    const [activeTab, setActiveTab] = useState<'trades' | 'orders'>('trades');

    const { subscribeToOrderBook, subscribeToTrades } = useWebSocket();

    const loadStockDetail = useCallback(async () => {
        if (!symbol) return;

        try {
            setError('');
            const data: OrderBookSummaryApiResponse = await apiService.getStockDetail(symbol);

            // Map API response to internal OrderBookSummary format
            const safeData: OrderBookSummary = {
                symbol: data.symbol || symbol,
                topBuys: data.topBuyOrders || data.topBuys || [],
                lowestSells: data.topSellOrders || data.lowestSells || [],
                currentPrice: data.currentPrice || 0,
                bestBidPrice: data.bestBidPrice || 0,
                bestBidQuantity: data.bestBidQuantity || 0,
                bestAskPrice: data.bestAskPrice || 0,
                bestAskQuantity: data.bestAskQuantity || 0,
                recentTrades: data.recentTrades || [],
            };

            setStockData(safeData);
            setLoading(false);
        } catch (err) {
            console.error('Error loading stock detail:', err);
            setError(err instanceof Error ? err.message : 'Failed to load stock details');
            setLoading(false);
        }
    }, [symbol]);

    // Load initial stock data
    useEffect(() => {
        if (!symbol) {
            navigate('/');
            return;
        }

        loadStockDetail();
    }, [symbol, loadStockDetail, navigate]);

    // Setup WebSocket subscriptions
    useEffect(() => {
        if (!symbol) return;

        const setupSubscriptions = async () => {
            try {
                // Subscribe to order book updates
                await subscribeToOrderBook(symbol, (orderBookData: OrderBookSummary) => {
                    console.log('Received order book update:', orderBookData);
                    setStockData(orderBookData);
                });

                // Subscribe to trade updates
                await subscribeToTrades(symbol, (tradeData) => {
                    console.log('Received trade update:', tradeData);
                    setStockData(prev => prev ? {
                        ...prev,
                        recentTrades: [tradeData, ...(prev.recentTrades || [])].slice(0, 50)
                    } : null);
                });
            } catch (err) {
                console.error('Error setting up WebSocket subscriptions:', err);
            }
        };

        setupSubscriptions();

        // Cleanup is handled by useWebSocket hook
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [symbol]);

    // Poll for updates every 3 seconds as fallback
    useEffect(() => {
        if (!symbol) return;

        const interval = setInterval(() => {
            loadStockDetail();
        }, 3000);

        return () => clearInterval(interval);
    }, [symbol, loadStockDetail]);

    const handleOrderSubmit = async (order: Order) => {
        try {
            console.log('Submitting order:', order);
            const response = await apiService.submitOrder(order);
            setOrderSuccess(`Order submitted successfully! Order ID: ${response.orderId}`);
            setTimeout(() => setOrderSuccess(''), 5000);
            await loadStockDetail();
        } catch (err) {
            console.error('Error submitting order:', err);
            throw err;
        }
    };

    const handleBackClick = () => {
        navigate('/');
    };

    if (loading && !stockData) {
        return (
            <div style={styles.loading}>
                <div style={styles.spinner}></div>
                <p>Loading stock details...</p>
            </div>
        );
    }

    if (error && !stockData) {
        return (
            <div style={styles.error}>
                <h2>Error Loading Stock</h2>
                <p>{error}</p>
                <button style={styles.retryButton} onClick={loadStockDetail}>
                    Try Again
                </button>
                <button style={styles.backButton} onClick={handleBackClick}>
                    ← Back to Home
                </button>
            </div>
        );
    }

    if (!stockData) {
        return (
            <div style={styles.error}>
                <h2>Stock Not Found</h2>
                <p>The requested stock symbol could not be found.</p>
                <button style={styles.backButton} onClick={handleBackClick}>
                    ← Back to Home
                </button>
            </div>
        );
    }

    return (
        <div style={styles.container}>
            {/* Header */}
            <div style={styles.header}>
                <button style={styles.backButton} onClick={handleBackClick}>
                    ← Back to Stocks
                </button>
                <div style={styles.stockInfo}>
                    <h1 style={styles.symbol}>{stockData.symbol}</h1>
                    <div style={styles.priceContainer}>
                        <span style={styles.currentPrice}>
                            ${stockData.currentPrice.toFixed(2)}
                        </span>
                        {stockData.bestBidPrice > 0 && stockData.bestAskPrice > 0 && (
                            <span style={styles.spread}>
                                Spread: ${(stockData.bestAskPrice - stockData.bestBidPrice).toFixed(2)}
                            </span>
                        )}
                    </div>
                </div>
            </div>

            {orderSuccess && (
                <div style={styles.success}>
                    <span style={styles.successIcon}>✓</span>
                    {orderSuccess}
                </div>
            )}

            {/* Main Content Grid */}
            <div style={styles.content}>
                {/* Left Column: Order Form and Order Book */}
                <div style={styles.leftColumn}>
                    <OrderForm
                        symbol={stockData.symbol}
                        currentPrice={stockData.currentPrice}
                        onSubmitOrder={handleOrderSubmit}
                    />
                    <OrderBook
                        topBuys={stockData.topBuys || []}
                        lowestSells={stockData.lowestSells || []}
                        bestBidPrice={stockData.bestBidPrice || 0}
                        bestAskPrice={stockData.bestAskPrice || 0}
                    />
                </div>

                {/* Right Column: Chart and Trades/Orders */}
                <div style={styles.rightColumn}>
                    <PriceChart
                        symbol={stockData.symbol}
                        currentPrice={stockData.currentPrice}
                    />
                    
                    {/* Tab Selector */}
                    <div style={styles.tabContainer}>
                        <button
                            type="button"
                            onClick={() => setActiveTab('trades')}
                            style={{
                                ...styles.tab,
                                ...(activeTab === 'trades' ? styles.tabActive : {}),
                            }}
                        >
                            Trade History
                        </button>
                        <button
                            type="button"
                            onClick={() => setActiveTab('orders')}
                            style={{
                                ...styles.tab,
                                ...(activeTab === 'orders' ? styles.tabActive : {}),
                            }}
                        >
                            My Orders
                        </button>
                    </div>

                    {/* Tab Content */}
                    {activeTab === 'trades' ? (
                        <TradeHistory trades={stockData.recentTrades || []} symbol={stockData.symbol} />
                    ) : (
                        <UserOpenOrders />
                    )}
                </div>
            </div>
        </div>
    );
};

const styles: { [key: string]: React.CSSProperties } = {
    container: {
        maxWidth: '1600px',
        margin: '0 auto',
        padding: '2rem 1rem',
        minHeight: 'calc(100vh - 80px)',
    },
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: '2rem',
        paddingBottom: '1.5rem',
        borderBottom: '2px solid #334155',
        flexWrap: 'wrap',
        gap: '1rem',
    },
    backButton: {
        backgroundColor: '#475569',
        color: 'white',
        border: 'none',
        padding: '0.5rem 1rem',
        borderRadius: '0.5rem',
        cursor: 'pointer',
        fontSize: '0.875rem',
        fontWeight: '500',
        transition: 'all 0.2s',
    },
    stockInfo: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'flex-end',
        gap: '0.5rem',
    },
    symbol: {
        fontSize: '2.5rem',
        fontWeight: '700',
        color: '#e2e8f0',
        margin: 0,
    },
    priceContainer: {
        display: 'flex',
        alignItems: 'center',
        gap: '1rem',
    },
    currentPrice: {
        fontSize: '2rem',
        fontWeight: '700',
        color: '#10b981',
    },
    spread: {
        fontSize: '0.875rem',
        color: '#cbd5e1',
        backgroundColor: '#0f172a',
        padding: '0.25rem 0.75rem',
        borderRadius: '0.375rem',
    },
    success: {
        backgroundColor: '#14532d',
        border: '1px solid #166534',
        color: '#86efac',
        padding: '1rem',
        borderRadius: '0.5rem',
        marginBottom: '1.5rem',
        fontSize: '0.875rem',
        display: 'flex',
        alignItems: 'center',
        gap: '0.5rem',
    },
    successIcon: {
        fontSize: '1.25rem',
    },
    loading: {
        textAlign: 'center',
        fontSize: '1.125rem',
        color: '#94a3b8',
        padding: '4rem',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: '1rem',
    },
    spinner: {
        width: '3rem',
        height: '3rem',
        border: '4px solid #1e293b',
        borderTop: '4px solid #10b981',
        borderRadius: '50%',
        animation: 'spin 1s linear infinite',
    },
    error: {
        textAlign: 'center',
        padding: '4rem',
        color: '#f87171',
    },
    retryButton: {
        backgroundColor: '#10b981',
        color: 'white',
        border: 'none',
        padding: '0.75rem 1.5rem',
        borderRadius: '0.5rem',
        cursor: 'pointer',
        marginTop: '1rem',
        marginRight: '1rem',
        fontWeight: '500',
    },
    content: {
        display: 'grid',
        gridTemplateColumns: '1fr 1fr',
        gap: '2rem',
        alignItems: 'start',
    },
    leftColumn: {
        display: 'flex',
        flexDirection: 'column',
        gap: '1.5rem',
    },
    rightColumn: {
        display: 'flex',
        flexDirection: 'column',
        gap: '1.5rem',
    },
    tabContainer: {
        display: 'flex',
        gap: '0.5rem',
        backgroundColor: '#0f172a',
        padding: '0.25rem',
        borderRadius: '0.5rem',
        border: '1px solid #334155',
    },
    tab: {
        flex: 1,
        padding: '0.75rem',
        border: 'none',
        borderRadius: '0.375rem',
        fontSize: '0.875rem',
        fontWeight: '600',
        cursor: 'pointer',
        backgroundColor: 'transparent',
        color: '#94a3b8',
        transition: 'all 0.2s',
    },
    tabActive: {
        backgroundColor: '#0f172a',
        color: '#e2e8f0',
        boxShadow: '0 1px 3px rgba(0, 0, 0, 0.3)',
    },
};

export default StockDetailPage;
