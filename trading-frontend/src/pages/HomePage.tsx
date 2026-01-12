import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Stock } from '../types';
import { apiService } from '../services/api';
import { useWebSocket } from '../hooks/useWebSocket';

const HomePage: React.FC = () => {
    const [stocks, setStocks] = useState<Stock[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [connectionStatus, setConnectionStatus] = useState('Connecting...');
    const [priceChanges, setPriceChanges] = useState<Record<string, { prev: number; current: number }>>({});
    const [searchQuery, setSearchQuery] = useState('');
    const navigate = useNavigate();

    const { subscribeToMultiServerPrices } = useWebSocket();

    useEffect(() => {
        loadStocks();
    }, []);

    useEffect(() => {
        console.log('Setting up multi-server price subscriptions...');

        subscribeToMultiServerPrices((priceData) => {
            console.log('📊 Received price update:', priceData);

            setConnectionStatus('Connected');

            if (priceData.prices) {
                setStocks(prevStocks => {
                    const updatedStocks = prevStocks.map(stock => {
                        const newPrice = priceData.prices[stock.symbol];
                        if (newPrice !== undefined && newPrice !== stock.currentPrice) {
                            console.log(`💲 ${stock.symbol}: ${stock.currentPrice} → ${newPrice}`);

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
    }, [subscribeToMultiServerPrices]);

    const loadStocks = async () => {
        try {
            setLoading(true);
            setError('');
            console.log('📡 Loading stocks from API...');
            const stocksData = await apiService.getAllStocks();
            console.log('✅ Loaded stocks:', stocksData);
            setStocks(stocksData);

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
        if (change.current > change.prev) return '#10b981';
        if (change.current < change.prev) return '#ef4444';
        return '#1f2937';
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
        return '#f59e0b';
    };

    const getStatusEmoji = () => {
        if (connectionStatus === 'Connected') return '🟢';
        if (connectionStatus === 'Error') return '🔴';
        return '🟡';
    };

    const filteredStocks = stocks.filter(stock =>
        stock.symbol.toLowerCase().includes(searchQuery.toLowerCase()) ||
        stock.companyName.toLowerCase().includes(searchQuery.toLowerCase())
    );

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
                    <h1 style={styles.title}>Live Stock Market</h1>
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
                        ↻ Refresh
                    </button>
                </div>
            </div>

            <div style={styles.searchContainer}>
                <input
                    type="text"
                    placeholder="Search by symbol or company name..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    style={styles.searchInput}
                />
            </div>

            <div style={styles.stocksGrid}>
                {filteredStocks.length > 0 ? (
                    filteredStocks.map((stock) => (
                        <div
                            key={stock.symbol}
                            style={styles.stockCard}
                            onClick={() => handleStockClick(stock.symbol)}
                        >
                            <div style={styles.stockHeader}>
                                <div>
                                    <h3 style={styles.symbol}>{stock.symbol}</h3>
                                    <p style={styles.companyName}>{stock.companyName}</p>
                                </div>
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
                            <div style={styles.footer}>
                                <span style={styles.esg}>
                                    ESG: {stock.esgScore}
                                </span>
                                <span style={styles.clickHint}>View Details →</span>
                            </div>
                        </div>
                    ))
                ) : (
                    <div style={styles.noStocks}>
                        <h3>No Stocks Found</h3>
                        <p>{searchQuery ? 'Try a different search term' : 'Please check your backend connection'}</p>
                    </div>
                )}
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
        alignItems: 'flex-start',
        marginBottom: '2rem',
        flexWrap: 'wrap',
        gap: '1rem',
    },
    title: {
        fontSize: '2.5rem',
        fontWeight: '700',
        color: '#e2e8f0',
        margin: '0 0 0.5rem 0',
        background: 'linear-gradient(135deg, #10b981 0%, #34d399 100%)',
        WebkitBackgroundClip: 'text',
        WebkitTextFillColor: 'transparent',
        backgroundClip: 'text',
    },
    subtitle: {
        color: '#94a3b8',
        fontSize: '1rem',
        margin: 0,
    },
    statusContainer: {
        display: 'flex',
        alignItems: 'center',
        gap: '1rem',
    },
    status: {
        fontSize: '0.875rem',
        fontWeight: '600',
        display: 'flex',
        alignItems: 'center',
        gap: '0.5rem',
    },
    statusDot: {
        fontSize: '0.75rem',
    },
    refreshButton: {
        backgroundColor: '#10b981',
        color: 'white',
        border: 'none',
        padding: '0.5rem 1rem',
        borderRadius: '0.5rem',
        cursor: 'pointer',
        fontSize: '0.875rem',
        fontWeight: '500',
        transition: 'all 0.2s',
    },
    searchContainer: {
        marginBottom: '2rem',
    },
    searchInput: {
        width: '100%',
        maxWidth: '500px',
        padding: '0.75rem 1rem',
        border: '2px solid #475569',
        borderRadius: '0.5rem',
        fontSize: '1rem',
        outline: 'none',
        transition: 'border-color 0.2s',
        backgroundColor: '#1e293b',
        color: '#e2e8f0',
    },
    loading: {
        textAlign: 'center',
        padding: '4rem',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: '1rem',
        color: '#94a3b8',
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
        fontWeight: '500',
    },
    stocksGrid: {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
        gap: '1.5rem',
    },
    stockCard: {
        backgroundColor: '#1e293b',
        border: '1px solid #334155',
        borderRadius: '0.75rem',
        padding: '1.5rem',
        cursor: 'pointer',
        transition: 'all 0.2s ease',
        boxShadow: '0 4px 6px rgba(0, 0, 0, 0.3)',
    },
    stockHeader: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        marginBottom: '1rem',
    },
    symbol: {
        fontSize: '1.5rem',
        fontWeight: '700',
        color: '#e2e8f0',
        margin: '0 0 0.25rem 0',
    },
    companyName: {
        color: '#94a3b8',
        fontSize: '0.875rem',
        margin: 0,
        lineHeight: '1.25',
    },
    priceContainer: {
        display: 'flex',
        alignItems: 'center',
        gap: '0.5rem',
    },
    price: {
        fontSize: '1.5rem',
        fontWeight: '700',
    },
    changeIndicator: {
        fontSize: '1.25rem',
        fontWeight: '700',
    },
    footer: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        paddingTop: '1rem',
        borderTop: '1px solid #334155',
    },
    esg: {
        fontSize: '0.75rem',
        color: '#cbd5e1',
        backgroundColor: '#0f172a',
        padding: '0.25rem 0.75rem',
        borderRadius: '0.375rem',
        fontWeight: '500',
    },
    clickHint: {
        fontSize: '0.75rem',
        color: '#10b981',
        fontWeight: '600',
    },
    noStocks: {
        textAlign: 'center',
        color: '#64748b',
        padding: '4rem',
        gridColumn: '1 / -1',
    },
};

export default HomePage;
