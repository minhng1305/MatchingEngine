import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { OrderBookSummary, Order } from '../types';
import { apiService } from '../services/api';
import { useWebSocket } from '../hooks/useWebSocket';
import OrderForm from '../components/OrderForm';
import OrderBook from '../components/OrderBook';
import TradesList from '../components/TradesList';

const StockDetailPage: React.FC = () => {
    const { symbol } = useParams<{ symbol: string }>();
    const navigate = useNavigate();

    // FIXED: Initialize with better default structure
    const [stockData, setStockData] = useState<OrderBookSummary | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [orderSuccess, setOrderSuccess] = useState('');

    const { isConnected, subscribeToOrderBook, subscribeToTrades } = useWebSocket();

    useEffect(() => {
        if (!symbol) {
            navigate('/');
            return;
        }

        loadStockDetail();
    }, [symbol, navigate]);

    useEffect(() => {
        if (isConnected && symbol) {
            // Subscribe to real-time order book updates
            const orderBookSub = subscribeToOrderBook(symbol, (orderBookData: OrderBookSummary) => {
                console.log('Received order book update:', orderBookData);
                setStockData(orderBookData);
            });

            // Subscribe to real-time trade updates
            const tradesSub = subscribeToTrades(symbol, (tradeData) => {
                console.log('Received trade update:', tradeData);
                setStockData(prev => prev ? {
                    ...prev,
                    recentTrades: [tradeData, ...(prev.recentTrades || [])].slice(0, 10)
                } : null);
            });

            // Cleanup subscriptions
            return () => {
                if (orderBookSub) {
                    // Note: Add unsubscribe logic if your useWebSocket hook supports it
                }
            };
        }
    }, [isConnected, symbol, subscribeToOrderBook, subscribeToTrades]);

    const loadStockDetail = async () => {
        if (!symbol) return;

        try {
            setLoading(true);
            setError('');
            console.log('Loading stock detail for:', symbol);

            const data = await apiService.getStockDetail(symbol);
            console.log('Loaded stock data:', data);

            // FIXED: Ensure data has proper structure with defaults
            const safeData: OrderBookSummary = {
                symbol: data.symbol || symbol,
                topBuys: data.topBuys || [],
                lowestSells: data.lowestSells || [],
                currentPrice: data.currentPrice || 0,
                bestBidPrice: data.bestBidPrice || 0,
                bestBidQuantity: data.bestBidQuantity || 0,
                bestAskPrice: data.bestAskPrice || 0,
                bestAskQuantity: data.bestAskQuantity || 0,
                recentTrades: data.recentTrades || [],
            };

            setStockData(safeData);
        } catch (err) {
            console.error('Error loading stock detail:', err);
            setError(err instanceof Error ? err.message : 'Failed to load stock details');
        } finally {
            setLoading(false);
        }
    };

    const handleOrderSubmit = async (order: Order) => {
        try {
            console.log('Submitting order:', order);
            const response = await apiService.submitOrder(order);
            setOrderSuccess(`Order submitted successfully! Order ID: ${response.orderId}`);

            // Clear success message after 5 seconds
            setTimeout(() => setOrderSuccess(''), 5000);

            // Reload stock data to show updated order book
            await loadStockDetail();
        } catch (err) {
            console.error('Error submitting order:', err);
            throw err; // Let OrderForm handle the error
        }
    };

    const handleBackClick = () => {
        navigate('/');
    };

    if (loading) {
        return (
            <div style={styles.container}>
                <div style={styles.loading}>
                    <div style={styles.spinner}></div>
                    <p>Loading stock details...</p>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div style={styles.container}>
                <div style={styles.error}>
                    <h3>Error Loading Stock</h3>
                    <p>{error}</p>
                    <button onClick={loadStockDetail} style={styles.retryButton}>
                        Try Again
                    </button>
                    <button onClick={handleBackClick} style={styles.backButton}>
                        ← Back to Home
                    </button>
                </div>
            </div>
        );
    }

    if (!stockData) {
        return (
            <div style={styles.container}>
                <div style={styles.error}>
                    <h3>Stock Not Found</h3>
                    <p>The requested stock symbol could not be found.</p>
                    <button onClick={handleBackClick} style={styles.backButton}>
                        ← Back to Home
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div style={styles.container}>
            <div style={styles.header}>
                <button onClick={handleBackClick} style={styles.backButton}>
                    ← Back to Stocks
                </button>
                <div style={styles.stockInfo}>
                    <h1 style={styles.symbol}>{stockData.symbol}</h1>
                    <div style={styles.priceContainer}>
            <span style={styles.currentPrice}>
              ${stockData.currentPrice.toFixed(2)}
            </span>
                    </div>
                </div>
            </div>

            {orderSuccess && (
                <div style={styles.success}>
                    {orderSuccess}
                </div>
            )}

            <div style={styles.content}>
                <div style={styles.leftColumn}>
                    <OrderForm
                        symbol={stockData.symbol}
                        currentPrice={stockData.currentPrice}
                        onSubmitOrder={handleOrderSubmit}
                    />

                    {/* ✅ FIXED: Now passes safe data to OrderBook */}
                    <OrderBook
                        topBuys={stockData.topBuys || []}
                        lowestSells={stockData.lowestSells || []}
                        bestBidPrice={stockData.bestBidPrice || 0}
                        bestAskPrice={stockData.bestAskPrice || 0}
                    />
                </div>

                <div style={styles.rightColumn}>
                    <TradesList trades={stockData.recentTrades || []} />
                </div>
            </div>
        </div>
    );
};

const styles = {
    container: {
        maxWidth: '1200px',
        margin: '0 auto',
        padding: '2rem 1rem',
        minHeight: '80vh',
    },
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: '2rem',
    },
    backButton: {
        backgroundColor: '#6b7280',
        color: 'white',
        border: 'none',
        padding: '0.5rem 1rem',
        borderRadius: '0.25rem',
        cursor: 'pointer',
        fontSize: '0.875rem',
    },
    stockInfo: {
        display: 'flex',
        alignItems: 'center',
        gap: '1rem',
    },
    symbol: {
        fontSize: '2rem',
        fontWeight: 'bold',
        color: '#1f2937',
    },
    priceContainer: {
        display: 'flex',
        alignItems: 'center',
        gap: '0.5rem',
    },
    currentPrice: {
        fontSize: '1.5rem',
        fontWeight: 'bold',
        color: '#10b981',
    },
    loading: {
        textAlign: 'center' as const,
        fontSize: '1.125rem',
        color: '#6b7280',
        padding: '4rem',
        display: 'flex',
        flexDirection: 'column' as const,
        alignItems: 'center',
        gap: '1rem',
    },
    spinner: {
        width: '2rem',
        height: '2rem',
        border: '3px solid #f3f4f6',
        borderTop: '3px solid #3b82f6',
        borderRadius: '50%',
        animation: 'spin 1s linear infinite',
    },
    error: {
        textAlign: 'center' as const,
        padding: '4rem',
        color: '#ef4444',
    },
    retryButton: {
        backgroundColor: '#3b82f6',
        color: 'white',
        border: 'none',
        padding: '0.75rem 1.5rem',
        borderRadius: '0.25rem',
        cursor: 'pointer',
        marginTop: '1rem',
        marginRight: '1rem',
    },
    success: {
        backgroundColor: '#dcfce7',
        border: '1px solid #bbf7d0',
        color: '#166534',
        padding: '0.75rem',
        borderRadius: '0.25rem',
        marginBottom: '1rem',
        fontSize: '0.875rem',
    },
    content: {
        display: 'grid',
        gridTemplateColumns: '2fr 1fr',
        gap: '2rem',
    },
    leftColumn: {
        display: 'flex',
        flexDirection: 'column' as const,
    },
    rightColumn: {
        display: 'flex',
        flexDirection: 'column' as const,
    },
};

export default StockDetailPage;