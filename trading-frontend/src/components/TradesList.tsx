import React from 'react';
import { Trade } from '../types';

interface TradesListProps {
    trades: Trade[];
}

const TradesList: React.FC<TradesListProps> = ({ trades }) => {
    const formatTime = (timestamp: string) => {
        return new Date(timestamp).toLocaleTimeString();
    };

    return (
        <div style={styles.container}>
            <h3 style={styles.title}>Recent Trades</h3>

            <div style={styles.tradesContainer}>
                {trades.length > 0 ? (
                    <div style={styles.tradesList}>
                        {trades.slice(0, 10).map((trade) => (
                            <div key={trade.tradeId} style={styles.tradeRow}>
                                <div style={styles.tradeInfo}>
                  <span style={styles.price}>
                    ${trade.price.toFixed(2)}
                  </span>
                                    <span style={styles.quantity}>
                    {trade.quantity} shares
                  </span>
                                </div>
                                <span style={styles.time}>
                  {formatTime(trade.tradeTimestamp)}
                </span>
                            </div>
                        ))}
                    </div>
                ) : (
                    <div style={styles.noTrades}>
                        No trades yet
                    </div>
                )}
            </div>
        </div>
    );
};

const styles = {
    container: {
        backgroundColor: 'white',
        border: '1px solid #e5e7eb',
        borderRadius: '0.5rem',
        padding: '1.5rem',
        marginBottom: '1.5rem',
    },
    title: {
        fontSize: '1.125rem',
        fontWeight: 'bold',
        marginBottom: '1rem',
        color: '#1f2937',
    },
    tradesContainer: {
        maxHeight: '300px',
        overflowY: 'auto' as const,
    },
    tradesList: {
        display: 'flex',
        flexDirection: 'column' as const,
        gap: '0.5rem',
    },
    tradeRow: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: '0.75rem',
        backgroundColor: '#f9fafb',
        borderRadius: '0.25rem',
        borderLeft: '3px solid #3b82f6',
    },
    tradeInfo: {
        display: 'flex',
        flexDirection: 'column' as const,
        gap: '0.25rem',
    },
    price: {
        fontSize: '0.875rem',
        fontWeight: '600',
        color: '#1f2937',
    },
    quantity: {
        fontSize: '0.75rem',
        color: '#6b7280',
    },
    time: {
        fontSize: '0.75rem',
        color: '#9ca3af',
    },
    noTrades: {
        textAlign: 'center' as const,
        color: '#9ca3af',
        fontSize: '0.875rem',
        padding: '2rem',
        fontStyle: 'italic',
    },
};

export default TradesList;