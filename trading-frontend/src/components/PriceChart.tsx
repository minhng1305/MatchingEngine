import React, { useState, useEffect } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, ReferenceLine } from 'recharts';
import { apiService } from '../services/api';

interface PriceChartProps {
    symbol: string;
    currentPrice: number;
}

interface PriceDataPoint {
    time: string;
    price: number;
    timestamp: number;
}

const PriceChart: React.FC<PriceChartProps> = ({ symbol, currentPrice }) => {
    const [priceHistory, setPriceHistory] = useState<PriceDataPoint[]>([]);
    const [loading, setLoading] = useState(true);
    const [timeRange, setTimeRange] = useState<'1m' | '5m' | '15m' | '1h'>('5m');

    useEffect(() => {
        const fetchPrice = async () => {
            try {
                const response = await apiService.getCurrentPrice(symbol);
                const newPoint: PriceDataPoint = {
                    time: new Date(response.timestamp).toLocaleTimeString('en-US', { hour12: false }),
                    price: response.currentPrice,
                    timestamp: response.timestamp,
                };

                setPriceHistory(prev => {
                    const updated = [...prev, newPoint];
                    // Keep only last 50 points for performance
                    return updated.slice(-50);
                });
                setLoading(false);
            } catch (error) {
                console.error('Error fetching price:', error);
            }
        };

        // Initial fetch
        fetchPrice();

        // Poll every 2 seconds
        const interval = setInterval(fetchPrice, 2000);

        return () => clearInterval(interval);
    }, [symbol]);

    // Calculate price change
    const priceChange = priceHistory.length > 1
        ? priceHistory[priceHistory.length - 1].price - priceHistory[0].price
        : 0;
    const priceChangePercent = priceHistory.length > 1 && priceHistory[0].price > 0
        ? ((priceChange / priceHistory[0].price) * 100)
        : 0;

    const chartData = priceHistory.map((point, index) => ({
        time: point.time.split(':').slice(0, 2).join(':'), // HH:MM format
        price: point.price,
        index,
    }));

    const CustomTooltip = ({ active, payload }: any) => {
        if (active && payload && payload.length) {
            return (
                <div style={{
                    backgroundColor: '#1e293b',
                    border: '1px solid #334155',
                    borderRadius: '0.5rem',
                    padding: '0.75rem',
                    boxShadow: '0 4px 6px rgba(0, 0, 0, 0.5)',
                }}>
                    <p style={{ margin: 0, fontSize: '0.875rem', fontWeight: '600', color: '#e2e8f0' }}>
                        ${payload[0].value.toFixed(2)}
                    </p>
                    <p style={{ margin: '0.25rem 0 0 0', fontSize: '0.75rem', color: '#94a3b8' }}>
                        {payload[0].payload.time}
                    </p>
                </div>
            );
        }
        return null;
    };

    return (
        <div style={styles.container}>
            <div style={styles.header}>
                <div>
                    <h3 style={styles.title}>Price Chart</h3>
                    <div style={styles.priceInfo}>
                        <span style={styles.currentPrice}>${currentPrice.toFixed(2)}</span>
                        {priceChange !== 0 && (
                            <span style={{
                                ...styles.priceChange,
                                color: priceChange >= 0 ? '#10b981' : '#ef4444',
                            }}>
                                {priceChange >= 0 ? '+' : ''}{priceChange.toFixed(2)} ({priceChangePercent >= 0 ? '+' : ''}{priceChangePercent.toFixed(2)}%)
                            </span>
                        )}
                    </div>
                </div>
                <div style={styles.timeRangeSelector}>
                    {(['1m', '5m', '15m', '1h'] as const).map((range) => (
                        <button
                            key={range}
                            type="button"
                            onClick={() => setTimeRange(range)}
                            style={{
                                ...styles.timeRangeButton,
                                ...(timeRange === range ? styles.timeRangeButtonActive : {}),
                            }}
                        >
                            {range}
                        </button>
                    ))}
                </div>
            </div>

            <div style={styles.chartContainer}>
                {loading && priceHistory.length === 0 ? (
                    <div style={styles.loading}>
                        <div style={styles.spinner}></div>
                        <p>Loading chart data...</p>
                    </div>
                ) : chartData.length === 0 ? (
                    <div style={styles.emptyState}>
                        <div style={styles.emptyIcon}>📈</div>
                        <div style={styles.emptyText}>No price data available</div>
                    </div>
                ) : (
                    <ResponsiveContainer width="100%" height="100%">
                        <LineChart data={chartData} margin={{ top: 5, right: 10, left: 10, bottom: 5 }}>
                            <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                            <XAxis
                                dataKey="time"
                                stroke="#94a3b8"
                                fontSize={12}
                                tick={{ fill: '#94a3b8' }}
                            />
                            <YAxis
                                stroke="#94a3b8"
                                fontSize={12}
                                tick={{ fill: '#94a3b8' }}
                                domain={['auto', 'auto']}
                            />
                            <Tooltip content={<CustomTooltip />} />
                            <ReferenceLine
                                y={currentPrice}
                                stroke="#10b981"
                                strokeDasharray="2 2"
                                label={{ value: 'Current', position: 'right', fill: '#10b981', fontSize: 10 }}
                            />
                            <Line
                                type="monotone"
                                dataKey="price"
                                stroke="#10b981"
                                strokeWidth={2}
                                dot={false}
                                activeDot={{ r: 4, fill: '#10b981' }}
                            />
                        </LineChart>
                    </ResponsiveContainer>
                )}
            </div>
        </div>
    );
};

const styles: { [key: string]: React.CSSProperties } = {
    container: {
        backgroundColor: '#1e293b',
        border: '1px solid #334155',
        borderRadius: '0.75rem',
        padding: '1.5rem',
        boxShadow: '0 4px 6px rgba(0, 0, 0, 0.3)',
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
    },
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        marginBottom: '1rem',
        paddingBottom: '1rem',
        borderBottom: '2px solid #334155',
        flexWrap: 'wrap',
        gap: '1rem',
    },
    title: {
        fontSize: '1.25rem',
        fontWeight: '700',
        color: '#e2e8f0',
        margin: '0 0 0.5rem 0',
    },
    priceInfo: {
        display: 'flex',
        alignItems: 'center',
        gap: '1rem',
    },
    currentPrice: {
        fontSize: '1.5rem',
        fontWeight: '700',
        color: '#e2e8f0',
    },
    priceChange: {
        fontSize: '0.875rem',
        fontWeight: '600',
    },
    timeRangeSelector: {
        display: 'flex',
        gap: '0.5rem',
    },
    timeRangeButton: {
        padding: '0.375rem 0.75rem',
        border: '1px solid #475569',
        borderRadius: '0.375rem',
        fontSize: '0.75rem',
        fontWeight: '500',
        backgroundColor: '#0f172a',
        color: '#cbd5e1',
        cursor: 'pointer',
        transition: 'all 0.2s',
    },
    timeRangeButtonActive: {
        backgroundColor: '#3b82f6',
        color: '#ffffff',
        borderColor: '#3b82f6',
    },
    chartContainer: {
        flex: 1,
        minHeight: '300px',
        position: 'relative',
    },
    loading: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100%',
        color: '#94a3b8',
        gap: '1rem',
    },
    spinner: {
        width: '2rem',
        height: '2rem',
        border: '3px solid #1e293b',
        borderTop: '3px solid #10b981',
        borderRadius: '50%',
        animation: 'spin 1s linear infinite',
    },
    emptyState: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100%',
        color: '#64748b',
    },
    emptyIcon: {
        fontSize: '3rem',
        marginBottom: '1rem',
    },
    emptyText: {
        fontSize: '1rem',
        fontWeight: '600',
    },
};

export default PriceChart;
