import React from 'react';
import { Order } from '../types';

interface OrderBookProps {
    topBuys: Order[];
    lowestSells: Order[];
    bestBidPrice: number;
    bestAskPrice: number;
}

const OrderBook: React.FC<OrderBookProps> = ({
    topBuys,
    lowestSells,
    bestBidPrice,
    bestAskPrice
}) => {
    const safeBuyOrders = topBuys || [];
    const safeSellOrders = lowestSells || [];
    const safeBidPrice = bestBidPrice || 0;
    const safeAskPrice = bestAskPrice || 0;
    const spread = safeAskPrice > 0 && safeBidPrice > 0 ? safeAskPrice - safeBidPrice : 0;
    const spreadPercent = safeBidPrice > 0 ? (spread / safeBidPrice) * 100 : 0;

    // Reverse sells to show highest asks first (top of list)
    const displaySells = [...safeSellOrders].reverse();
    const displayBuys = safeBuyOrders;

    // Calculate max quantity for visual depth bars
    const allQuantities = [
        ...displaySells.map(o => o.quantity || o.currentQuantity || 0),
        ...displayBuys.map(o => o.quantity || o.currentQuantity || 0)
    ];
    const maxQuantity = Math.max(...allQuantities, 1);

    return (
        <div style={styles.container}>
            <div style={styles.header}>
                <h3 style={styles.title}>Order Book</h3>
                <div style={styles.spreadContainer}>
                    <span style={styles.spreadLabel}>Spread:</span>
                    <span style={styles.spreadValue}>${spread.toFixed(2)}</span>
                    <span style={styles.spreadPercent}>({spreadPercent.toFixed(2)}%)</span>
                </div>
            </div>

            <div style={styles.tableHeader}>
                <div style={styles.headerCell}>Price</div>
                <div style={styles.headerCell}>Quantity</div>
                <div style={styles.headerCell}>Total</div>
            </div>

            <div style={styles.bookContainer}>
                {/* ASKS (Sell Orders) - Top, Red */}
                <div style={styles.asksSection}>
                    {displaySells.length > 0 ? (
                        displaySells.map((order, index) => {
                            const qty = order.quantity || order.currentQuantity || 0;
                            const total = order.price * qty;
                            const depthPercent = (qty / maxQuantity) * 100;
                            
                            return (
                                <div
                                    key={`ask-${index}`}
                                    style={styles.orderRow}
                                    className="order-row-ask"
                                >
                                    <div 
                                        style={{
                                            ...styles.depthBar,
                                            backgroundColor: '#ef4444',
                                            width: `${depthPercent}%`,
                                        }} 
                                        className="depth-bar-ask" 
                                    />
                                    <div style={{...styles.priceCell, color: '#ef4444'}}>
                                        ${order.price.toFixed(2)}
                                    </div>
                                    <div style={styles.quantityCell}>{qty.toLocaleString()}</div>
                                    <div style={styles.totalCell}>${total.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</div>
                                </div>
                            );
                        })
                    ) : (
                        <div style={styles.emptyState}>No sell orders</div>
                    )}
                </div>

                {/* SPREAD INDICATOR */}
                <div style={styles.spreadRow}>
                    <div style={styles.spreadContent}>
                        <div style={styles.spreadPrice}>
                            <span style={styles.bidPrice}>${safeBidPrice.toFixed(2)}</span>
                            <span style={styles.spreadSeparator}> ↔ </span>
                            <span style={styles.askPrice}>${safeAskPrice.toFixed(2)}</span>
                        </div>
                        <div style={styles.spreadInfo}>
                            Spread: ${spread.toFixed(2)} ({spreadPercent.toFixed(2)}%)
                        </div>
                    </div>
                </div>

                {/* BIDS (Buy Orders) - Bottom, Green */}
                <div style={styles.bidsSection}>
                    {displayBuys.length > 0 ? (
                        displayBuys.map((order, index) => {
                            const qty = order.quantity || order.currentQuantity || 0;
                            const total = order.price * qty;
                            const depthPercent = (qty / maxQuantity) * 100;
                            
                            return (
                                <div
                                    key={`bid-${index}`}
                                    style={styles.orderRow}
                                    className="order-row-bid"
                                >
                                    <div 
                                        style={{
                                            ...styles.depthBar,
                                            backgroundColor: '#10b981',
                                            width: `${depthPercent}%`,
                                        }} 
                                        className="depth-bar-bid" 
                                    />
                                    <div style={{...styles.priceCell, color: '#10b981'}}>
                                        ${order.price.toFixed(2)}
                                    </div>
                                    <div style={styles.quantityCell}>{qty.toLocaleString()}</div>
                                    <div style={styles.totalCell}>${total.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</div>
                                </div>
                            );
                        })
                    ) : (
                        <div style={styles.emptyState}>No buy orders</div>
                    )}
                </div>
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
    spreadContainer: {
        display: 'flex',
        alignItems: 'center',
        gap: '0.5rem',
        fontSize: '0.875rem',
    },
    spreadLabel: {
        color: '#94a3b8',
        fontWeight: '500',
    },
    spreadValue: {
        color: '#e2e8f0',
        fontWeight: '600',
    },
    spreadPercent: {
        color: '#94a3b8',
    },
    tableHeader: {
        display: 'grid',
        gridTemplateColumns: '1fr 1fr 1fr',
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
    bookContainer: {
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
    },
    asksSection: {
        display: 'flex',
        flexDirection: 'column',
        maxHeight: '45%',
        overflowY: 'auto',
    },
    bidsSection: {
        display: 'flex',
        flexDirection: 'column',
        maxHeight: '45%',
        overflowY: 'auto',
    },
    orderRow: {
        display: 'grid',
        gridTemplateColumns: '1fr 1fr 1fr',
        gap: '0.5rem',
        padding: '0.5rem 0',
        position: 'relative',
        fontSize: '0.875rem',
        transition: 'background-color 0.15s ease',
        cursor: 'pointer',
    },
    depthBar: {
        position: 'absolute',
        top: 0,
        right: 0,
        bottom: 0,
        height: '100%',
        opacity: 0.15,
        zIndex: 0,
    },
    priceCell: {
        fontWeight: '600',
        textAlign: 'right',
        position: 'relative',
        zIndex: 1,
    },
    quantityCell: {
        textAlign: 'right',
        color: '#cbd5e1',
        position: 'relative',
        zIndex: 1,
    },
    totalCell: {
        textAlign: 'right',
        color: '#94a3b8',
        position: 'relative',
        zIndex: 1,
    },
    spreadRow: {
        padding: '0.75rem 0',
        borderTop: '2px solid #334155',
        borderBottom: '2px solid #334155',
        backgroundColor: '#0f172a',
        margin: '0.5rem 0',
    },
    spreadContent: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: '0.25rem',
    },
    spreadPrice: {
        fontSize: '1rem',
        fontWeight: '700',
        color: '#e2e8f0',
    },
    bidPrice: {
        color: '#10b981',
    },
    askPrice: {
        color: '#ef4444',
    },
    spreadSeparator: {
        color: '#94a3b8',
        margin: '0 0.5rem',
    },
    spreadInfo: {
        fontSize: '0.75rem',
        color: '#94a3b8',
    },
    emptyState: {
        textAlign: 'center',
        color: '#64748b',
        fontSize: '0.875rem',
        padding: '2rem',
        fontStyle: 'italic',
    },
};

// Add CSS for depth bars and hover effects
const styleSheet = document.createElement('style');
styleSheet.textContent = `
    .order-row-ask:hover {
        background-color: rgba(239, 68, 68, 0.1);
    }
    .order-row-bid:hover {
        background-color: rgba(16, 185, 129, 0.1);
    }
`;
if (!document.getElementById('orderbook-styles')) {
    styleSheet.id = 'orderbook-styles';
    document.head.appendChild(styleSheet);
}

export default OrderBook;
