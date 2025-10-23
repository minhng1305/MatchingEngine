import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Stock } from '../types';
import { apiService } from '../services/api';
import { useWebSocket } from '../hooks/useWebSocket';
import StockCard from '../components/StockCard';

const HomePage: React.FC = () => {
    const [stocks, setStocks] = useState<Stock[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [connectionStatus, setConnectionStatus] = useState('Connecting...');
    const [priceChanges, setPriceChanges] = useState<Record<string, { prev: number; current: number }>>({});
    const navigate = useNavigate();

    // ✅ FIX 1: Remove useWebSocket options (we'll handle connection manually)
    const { subscribeToMultiServerPrices } = useWebSocket();

    useEffect(() => {
        loadStocks();
    }, []);

    // ✅ FIX 2: Connect immediately without waiting for isConnected
    useEffect(() => {
        console.log('Setting up multi-server price subscriptions...');

        subscribeToMultiServerPrices((priceData) => {
            console.log('📊 Received price update:', priceData);

            // ✅ FIX 3: Update connection status on first message
            setConnectionStatus('Connected');

            if (priceData.prices) {
                setStocks(prevStocks => {
                    const updatedStocks = prevStocks.map(stock => {
                        const newPrice = priceData.prices[stock.symbol];
                        if (newPrice !== undefined && newPrice !== stock.currentPrice) {
                            console.log(`💲 ${stock.symbol}: ${stock.currentPrice} → ${newPrice}`);

                            // Track price changes for visual indicators
                            setPriceChanges(prev => ({
                                ...prev,
                                [stock.symbol]: {
                                    prev: stock.currentPrice,
                                    current: newPrice
                                }
                            }));

                            return {
                                ...stock,
                                currentPrice: newPrice
                            };
                        }
                        return stock;
                    });
                    return updatedStocks;
                });
            }
        }).then(subscriptionIds => {
            console.log(`✅ Successfully subscribed to ${subscriptionIds.length} servers:`, subscriptionIds);
            setConnectionStatus('Connected');
        }).catch(err => {
            console.error('❌ Failed to subscribe to multi-server prices:', err);
            setConnectionStatus('Error');
        });

        // Cleanup is handled by useWebSocket hook
    }, [subscribeToMultiServerPrices]);

    const loadStocks = async () => {
        try {
            setLoading(true);
            setError('');
            console.log('📡 Loading stocks from API...');
            const stocksData = await apiService.getAllStocks();
            console.log('✅ Loaded stocks:', stocksData);
            setStocks(stocksData);

            // Initialize price changes tracking
            const initialChanges: Record<string, { prev: number; current: number }> = {};
            stocksData.forEach(stock => {
                initialChanges[stock.symbol] = {
                    prev: stock.currentPrice,
                    current: stock.currentPrice
                };
            });
            setPriceChanges(initialChanges);
        } catch (err) {
            console.error('❌ Error loading stocks:', err);
            setError(err instanceof Error ? err.message : 'Failed to load stocks');
        } finally {
            setLoading(false);
        }
    };

    const handleStockClick = (symbol: string) => {
        console.log('🔍 Navigating to stock:', symbol);
        navigate(`/stock/${symbol}`);
    };

    const getPriceChangeColor = (symbol: string) => {
        const change = priceChanges[symbol];
        if (!change) return '#1f2937';
        if (change.current > change.prev) return '#10b981'; // Green for up
        if (change.current < change.prev) return '#ef4444'; // Red for down
        return '#1f2937'; // Default for no change
    };

    const getPriceChangeIndicator = (symbol: string) => {
        const change = priceChanges[symbol];
        if (!change) return '';
        if (change.current > change.prev) return '↑';
        if (change.current < change.prev) return '↓';
        return '';
    };

    const getStatusColor = () => {
        if (connectionStatus === 'Connected') return '#10b981';
        if (connectionStatus === 'Error') return '#ef4444';
        return '#f59e0b'; // Orange for connecting
    };

    const getStatusEmoji = () => {
        if (connectionStatus === 'Connected') return '🟢';
        if (connectionStatus === 'Error') return '🔴';
        return '🟡';
    };

    if (loading) {
        return (
            <div style={styles.loading}>
                <div style={styles.spinner}></div>
                <p>Loading stocks...</p>
            </div>
        );
    }

    if (error) {
        return (
            <div style={styles.error}>
                <h2>Error Loading Stocks</h2>
                <p>{error}</p>
                <button style={styles.retryButton} onClick={loadStocks}>
                    Try Again
                </button>
            </div>
        );
    }

    return (
        <div style={styles.container}>
            <div style={styles.header}>
                <div>
                    <h1 style={styles.title}>Live Stock Prices</h1>
                    <p style={styles.subtitle}>Real-time updates • {stocks.length} stocks available</p>
                </div>
                <div style={styles.statusContainer}>
          <span style={{...styles.status, color: getStatusColor()}}>
            <span style={styles.statusDot}>
              {getStatusEmoji()}
            </span>
              {connectionStatus}
          </span>
                    <button style={styles.refreshButton} onClick={loadStocks}>
                        Refresh
                    </button>
                </div>
            </div>

            <div style={styles.stocksGrid}>
                {stocks.map((stock) => (
                    <div
                        key={stock.symbol}
                        style={styles.stockCard}
                        onClick={() => handleStockClick(stock.symbol)}
                    >
                        <div style={styles.stockHeader}>
                            <h3 style={styles.symbol}>{stock.symbol}</h3>
                            <div style={styles.priceContainer}>
                <span
                    style={{
                        ...styles.price,
                        color: getPriceChangeColor(stock.symbol)
                    }}
                >
                  ${stock.currentPrice.toFixed(2)}
                </span>
                                <span
                                    style={{
                                        ...styles.changeIndicator,
                                        color: getPriceChangeColor(stock.symbol)
                                    }}
                                >
                  {getPriceChangeIndicator(stock.symbol)}
                </span>
                            </div>
                        </div>
                        <p style={styles.companyName}>{stock.companyName}</p>
                        <div style={styles.footer}>
                            <span style={styles.esg}>ESG Score: {stock.esgScore}</span>
                            <span style={styles.clickHint}>Click to trade →</span>
                        </div>
                    </div>
                ))}
                {stocks.length === 0 && !loading && (
                    <div style={styles.noStocks}>
                        <h3>No Stocks Available</h3>
                        <p>Please check your backend connection</p>
                    </div>
                )}
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
        alignItems: 'flex-start',
        marginBottom: '2rem',
        flexWrap: 'wrap' as const,
        gap: '1rem',
    },
    title: {
        fontSize: '2rem',
        fontWeight: 'bold',
        color: '#1f2937',
        margin: 0,
    },
    subtitle: {
        color: '#6b7280',
        fontSize: '0.875rem',
        margin: '0.25rem 0 0 0',
    },
    statusContainer: {
        display: 'flex',
        alignItems: 'center',
        gap: '1rem',
    },
    status: {
        fontSize: '0.875rem',
        fontWeight: '600' as const,
        display: 'flex',
        alignItems: 'center',
        gap: '0.5rem',
    },
    statusDot: {
        fontSize: '0.75rem',
    },
    refreshButton: {
        backgroundColor: '#6b7280',
        color: 'white',
        border: 'none',
        padding: '0.5rem 1rem',
        borderRadius: '0.25rem',
        cursor: 'pointer',
        fontSize: '0.875rem',
        fontWeight: '500' as const,
    },
    loading: {
        textAlign: 'center' as const,
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
        fontWeight: '500' as const,
    },
    stocksGrid: {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))',
        gap: '1.5rem',
    },
    stockCard: {
        backgroundColor: 'white',
        border: '1px solid #e5e7eb',
        borderRadius: '0.5rem',
        padding: '1.5rem',
        cursor: 'pointer',
        transition: 'all 0.2s ease',
        boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)',
    },
    stockHeader: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        marginBottom: '0.5rem',
    },
    symbol: {
        fontSize: '1.25rem',
        fontWeight: 'bold' as const,
        color: '#1f2937',
        margin: 0,
    },
    priceContainer: {
        display: 'flex',
        alignItems: 'center',
        gap: '0.25rem',
    },
    price: {
        fontSize: '1.125rem',
        fontWeight: 'bold' as const,
    },
    changeIndicator: {
        fontSize: '1rem',
        fontWeight: 'bold' as const,
    },
    companyName: {
        color: '#6b7280',
        fontSize: '0.875rem',
        margin: '0.5rem 0 1rem 0',
        lineHeight: '1.25',
    },
    footer: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
    },
    esg: {
        fontSize: '0.75rem',
        color: '#9ca3af',
        backgroundColor: '#f3f4f6',
        padding: '0.25rem 0.5rem',
        borderRadius: '0.25rem',
    },
    clickHint: {
        fontSize: '0.75rem',
        color: '#3b82f6',
        fontWeight: '500' as const,
    },
    noStocks: {
        textAlign: 'center' as const,
        color: '#9ca3af',
        padding: '4rem',
    },
};

export default HomePage;
