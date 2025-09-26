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
    // ✅ FIXED: Safe array handling with defaults
    const safeBuyOrders = topBuys || [];
    const safeSellOrders = lowestSells || [];
    const safeBidPrice = bestBidPrice || 0;
    const safeAskPrice = bestAskPrice || 0;

    return (
        <div style={styles.container}>
            <h3 style={styles.title}>Order Book</h3>

            <div style={styles.spread}>
                <div style={styles.spreadItem}>
                    <span style={styles.spreadLabel}>Best Bid:</span>
                    <span style={{...styles.spreadPrice, color: '#10b981'}}>
            ${safeBidPrice.toFixed(2)}
          </span>
                </div>
                <div style={styles.spreadItem}>
                    <span style={styles.spreadLabel}>Best Ask:</span>
                    <span style={{...styles.spreadPrice, color: '#ef4444'}}>
            ${safeAskPrice.toFixed(2)}
          </span>
                </div>
            </div>

            <div style={styles.bookContainer}>
                {/* Sell Orders (Asks) */}
                <div style={styles.bookSide}>
                    <h4 style={{...styles.sideTitle, color: '#ef4444'}}>Sell Orders</h4>
                    <div style={styles.orderList}>
                        {/* FIXED: Now uses safeSellOrders.slice() */}
                        {safeSellOrders.slice(0, 5).map((order, index) => (
                            <div key={index} style={{...styles.orderRow, borderLeft: '3px solid #ef4444'}}>
                                <span style={styles.price}>${order.price.toFixed(2)}</span>
                                <span style={styles.quantity}>{order.quantity || order.currentQuantity || 0}</span>
                            </div>
                        ))}
                        {safeSellOrders.length === 0 && (
                            <div style={styles.noOrders}>No sell orders</div>
                        )}
                    </div>
                </div>

                {/* Buy Orders (Bids) */}
                <div style={styles.bookSide}>
                    <h4 style={{...styles.sideTitle, color: '#10b981'}}>Buy Orders</h4>
                    <div style={styles.orderList}>
                        {/* ✅ FIXED: Now uses safeBuyOrders.slice() */}
                        {safeBuyOrders.slice(0, 5).map((order, index) => (
                            <div key={index} style={{...styles.orderRow, borderLeft: '3px solid #10b981'}}>
                                <span style={styles.price}>${order.price.toFixed(2)}</span>
                                <span style={styles.quantity}>{order.quantity || order.currentQuantity || 0}</span>
                            </div>
                        ))}
                        {safeBuyOrders.length === 0 && (
                            <div style={styles.noOrders}>No buy orders</div>
                        )}
                    </div>
                </div>
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
    spread: {
        display: 'flex',
        justifyContent: 'space-between',
        padding: '1rem',
        backgroundColor: '#f9fafb',
        borderRadius: '0.25rem',
        marginBottom: '1rem',
    },
    spreadItem: {
        display: 'flex',
        flexDirection: 'column' as const,
        alignItems: 'center',
    },
    spreadLabel: {
        fontSize: '0.75rem',
        color: '#6b7280',
        marginBottom: '0.25rem',
    },
    spreadPrice: {
        fontSize: '1rem',
        fontWeight: 'bold',
    },
    bookContainer: {
        display: 'grid',
        gridTemplateColumns: '1fr 1fr',
        gap: '1rem',
    },
    bookSide: {
        minHeight: '200px',
    },
    sideTitle: {
        fontSize: '0.875rem',
        fontWeight: '600',
        marginBottom: '0.5rem',
        textAlign: 'center' as const,
    },
    orderList: {
        display: 'flex',
        flexDirection: 'column' as const,
        gap: '0.25rem',
    },
    orderRow: {
        display: 'flex',
        justifyContent: 'space-between',
        padding: '0.5rem',
        backgroundColor: '#f9fafb',
        borderRadius: '0.25rem',
        fontSize: '0.875rem',
    },
    price: {
        fontWeight: '500',
    },
    quantity: {
        color: '#6b7280',
    },
    noOrders: {
        textAlign: 'center' as const,
        color: '#9ca3af',
        fontSize: '0.875rem',
        padding: '1rem',
        fontStyle: 'italic',
    },
};

export default OrderBook;
