import React, { useEffect, useRef } from 'react';
import { Trade } from '../types';

interface TradeHistoryProps {
    trades: Trade[];
    symbol?: string;
}

const TradeHistory: React.FC<TradeHistoryProps> = ({ trades, symbol }) => {
    const safeTrades = trades || [];
    const scrollRef = useRef<HTMLDivElement>(null);
    const prevTradesLengthRef = useRef(0);

    // Auto-scroll to bottom when new trades arrive
    useEffect(() => {
        if (safeTrades.length > prevTradesLengthRef.current && scrollRef.current) {
            scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
        }
        prevTradesLengthRef.current = safeTrades.length;
    }, [safeTrades.length]);

    const formatTime = (timestamp: string) => {
        try {
            const date = new Date(timestamp);
            return date.toLocaleTimeString('en-US', { 
                hour12: false, 
                hour: '2-digit', 
                minute: '2-digit', 
                second: '2-digit' 
            });
        } catch {
            return timestamp;
        }
    };

    return (
        <div style={styles.container}>
            <div style={styles.header}>
                <h3 style={styles.title}>Trade History</h3>
                {symbol && <div style={styles.symbolBadge}>{symbol}</div>}
            </div>
            
            <div style={styles.tableHeader}>
                <div style={styles.headerCell}>Time</div>
                <div style={styles.headerCell}>Price</div>
                <div style={styles.headerCell}>Quantity</div>
                <div style={styles.headerCell}>Value</div>
            </div>

            <div style={styles.tradesList} ref={scrollRef}>
                {safeTrades.length > 0 ? (
                    safeTrades.map((trade, index) => {
                        const value = trade.price * trade.quantity;
                        const isRecent = index < 3; // Highlight most recent trades
                        
                        return (
                            <div
                                key={`${trade.tradeId}-${index}`}
                                style={{
                                    ...styles.tradeRow,
                                    ...(isRecent ? styles.tradeRowRecent : {}),
                                }}
                            >
                                <div style={styles.timeCell}>{formatTime(trade.tradeTimestamp)}</div>
                                <div style={styles.priceCell}>
                                    ${trade.price.toFixed(2)}
                                </div>
                                <div style={styles.quantityCell}>
                                    {trade.quantity.toLocaleString()}
                                </div>
                                <div style={styles.valueCell}>
                                    ${value.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                                </div>
                            </div>
                        );
                    })
                ) : (
                    <div style={styles.emptyState}>
                        <div style={styles.emptyIcon}>📊</div>
                        <div style={styles.emptyText}>No trades yet</div>
                        <div style={styles.emptySubtext}>Trades will appear here as they execute</div>
                    </div>
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
        alignItems: 'center',
        marginBottom: '1rem',
        paddingBottom: '0.75rem',
        borderBottom: '2px solid #334155',
    },
    title: {
        fontSize: '1.25rem',
        fontWeight: '700',
        color: '#e2e8f0',
        margin: 0,
    },
    symbolBadge: {
        backgroundColor: '#0f172a',
        color: '#cbd5e1',
        padding: '0.25rem 0.75rem',
        borderRadius: '0.5rem',
        fontSize: '0.875rem',
        fontWeight: '600',
    },
    tableHeader: {
        display: 'grid',
        gridTemplateColumns: '1fr 1.5fr 1fr 1.5fr',
        gap: '0.5rem',
        padding: '0.5rem 0',
        borderBottom: '1px solid #334155',
        fontSize: '0.75rem',
        fontWeight: '600',
        color: '#94a3b8',
        textTransform: 'uppercase',
        letterSpacing: '0.05em',
    },
    headerCell: {
        textAlign: 'right',
    },
    tradesList: {
        flex: 1,
        overflowY: 'auto',
        maxHeight: '500px',
    },
    tradeRow: {
        display: 'grid',
        gridTemplateColumns: '1fr 1.5fr 1fr 1.5fr',
        gap: '0.5rem',
        padding: '0.75rem 0',
        borderBottom: '1px solid #334155',
        fontSize: '0.875rem',
        transition: 'background-color 0.15s ease',
    },
    tradeRowRecent: {
        backgroundColor: 'rgba(16, 185, 129, 0.1)',
        borderLeft: '3px solid #10b981',
        paddingLeft: '0.5rem',
    },
    timeCell: {
        textAlign: 'right',
        color: '#94a3b8',
        fontSize: '0.75rem',
    },
    priceCell: {
        textAlign: 'right',
        fontWeight: '600',
        color: '#e2e8f0',
    },
    quantityCell: {
        textAlign: 'right',
        color: '#cbd5e1',
    },
    valueCell: {
        textAlign: 'right',
        color: '#94a3b8',
        fontWeight: '500',
    },
    emptyState: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '3rem 1rem',
        color: '#64748b',
    },
    emptyIcon: {
        fontSize: '3rem',
        marginBottom: '1rem',
    },
    emptyText: {
        fontSize: '1rem',
        fontWeight: '600',
        marginBottom: '0.5rem',
    },
    emptySubtext: {
        fontSize: '0.875rem',
        textAlign: 'center',
    },
};

export default TradeHistory;
