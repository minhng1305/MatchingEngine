import React, { useState, useEffect, useCallback } from 'react';
import { Order, OrderStatus } from '../types';
import { apiService } from '../services/api';
import { useAuth } from '../context/AuthContext';

const UserOpenOrders: React.FC = () => {
    const { user } = useAuth();
    const [orders, setOrders] = useState<Order[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [cancellingId, setCancellingId] = useState<string | null>(null);

    const loadOrders = useCallback(async () => {
        if (!user) return;

        try {
            setLoading(true);
            setError('');
            const userOrders = await apiService.getUserOrders();
            // Filter to show only pending/partially filled orders
            const openOrders = userOrders.filter(
                order => order.status === OrderStatus.PENDING || order.status === OrderStatus.PARTIALLY_FILLED
            );
            setOrders(openOrders);
        } catch (err) {
            console.error('Error loading orders:', err);
            setError(err instanceof Error ? err.message : 'Failed to load orders');
        } finally {
            setLoading(false);
        }
    }, [user]);

    useEffect(() => {
        if (user) {
            loadOrders();
            // Refresh every 5 seconds
            const interval = setInterval(loadOrders, 5000);
            return () => clearInterval(interval);
        }
    }, [user, loadOrders]);

    const handleCancel = async (orderId: string) => {
        if (!orderId) return;

        try {
            setCancellingId(orderId);
            // Note: The backend has this endpoint commented out, so this will fail
            // but we'll handle it gracefully
            await apiService.cancelOrder(orderId);
            await loadOrders();
        } catch (err) {
            console.error('Error cancelling order:', err);
            alert('Failed to cancel order. The cancel endpoint may not be implemented yet.');
        } finally {
            setCancellingId(null);
        }
    };

    const formatTime = (timestamp?: string) => {
        if (!timestamp) return 'N/A';
        try {
            const date = new Date(timestamp);
            return date.toLocaleString('en-US', {
                month: 'short',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
            });
        } catch {
            return timestamp;
        }
    };

    const getStatusColor = (status?: OrderStatus) => {
        switch (status) {
            case OrderStatus.PENDING:
                return '#f59e0b';
            case OrderStatus.PARTIALLY_FILLED:
                return '#3b82f6';
            case OrderStatus.FILLED:
                return '#10b981';
            case OrderStatus.CANCELLED:
                return '#6b7280';
            default:
                return '#6b7280';
        }
    };

    if (loading && orders.length === 0) {
        return (
            <div style={styles.container}>
                <div style={styles.loading}>
                    <div style={styles.spinner}></div>
                    <p>Loading orders...</p>
                </div>
            </div>
        );
    }

    return (
        <div style={styles.container}>
            <div style={styles.header}>
                <h3 style={styles.title}>My Open Orders</h3>
                <button onClick={loadOrders} style={styles.refreshButton}>
                    ↻ Refresh
                </button>
            </div>

            {error && (
                <div style={styles.error}>
                    <span style={styles.errorIcon}>⚠️</span>
                    {error}
                </div>
            )}

            {orders.length === 0 ? (
                <div style={styles.emptyState}>
                    <div style={styles.emptyIcon}>📋</div>
                    <div style={styles.emptyText}>No open orders</div>
                    <div style={styles.emptySubtext}>Your pending orders will appear here</div>
                </div>
            ) : (
                <>
                    <div style={styles.tableHeader}>
                        <div style={styles.headerCell}>Symbol</div>
                        <div style={styles.headerCell}>Side</div>
                        <div style={styles.headerCell}>Type</div>
                        <div style={styles.headerCell}>Price</div>
                        <div style={styles.headerCell}>Quantity</div>
                        <div style={styles.headerCell}>Filled</div>
                        <div style={styles.headerCell}>Status</div>
                        <div style={styles.headerCell}>Time</div>
                        <div style={styles.headerCell}>Action</div>
                    </div>

                    <div style={styles.ordersList}>
                        {orders.map((order) => {
                            const filledQty = (order.originalQuantity || order.quantity || 0) - (order.currentQuantity || order.quantity || 0);
                            const filledPercent = order.originalQuantity
                                ? ((filledQty / order.originalQuantity) * 100).toFixed(1)
                                : '0';

                            return (
                                <div key={order.orderId} style={styles.orderRow}>
                                    <div style={styles.symbolCell}>
                                        <span style={styles.symbolBadge}>{order.symbol}</span>
                                    </div>
                                    <div style={{
                                        ...styles.sideCell,
                                        color: order.side === 'BUY' ? '#10b981' : '#ef4444',
                                        fontWeight: '600',
                                    }}>
                                        {order.side}
                                    </div>
                                    <div style={styles.typeCell}>{order.type}</div>
                                    <div style={styles.priceCell}>
                                        ${order.price.toFixed(2)}
                                    </div>
                                    <div style={styles.quantityCell}>
                                        {order.quantity || order.currentQuantity || 0}
                                    </div>
                                    <div style={styles.filledCell}>
                                        {filledQty} ({filledPercent}%)
                                    </div>
                                    <div style={styles.statusCell}>
                                        <span style={{
                                            ...styles.statusBadge,
                                            backgroundColor: getStatusColor(order.status),
                                        }}>
                                            {order.status || 'PENDING'}
                                        </span>
                                    </div>
                                    <div style={styles.timeCell}>
                                        {formatTime(order.orderTimestamp)}
                                    </div>
                                    <div style={styles.actionCell}>
                                        {(order.status === OrderStatus.PENDING || order.status === OrderStatus.PARTIALLY_FILLED) && (
                                            <button
                                                onClick={() => order.orderId && handleCancel(order.orderId)}
                                                disabled={cancellingId === order.orderId}
                                                style={{
                                                    ...styles.cancelButton,
                                                    ...(cancellingId === order.orderId ? styles.cancelButtonDisabled : {}),
                                                }}
                                            >
                                                {cancellingId === order.orderId ? '...' : 'Cancel'}
                                            </button>
                                        )}
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </>
            )}
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
    refreshButton: {
        padding: '0.5rem 1rem',
        border: '1px solid #475569',
        borderRadius: '0.375rem',
        fontSize: '0.875rem',
        fontWeight: '500',
        backgroundColor: '#0f172a',
        color: '#cbd5e1',
        cursor: 'pointer',
        transition: 'all 0.2s',
    },
    error: {
        backgroundColor: '#7f1d1d',
        border: '1px solid #991b1b',
        color: '#fca5a5',
        padding: '0.75rem',
        borderRadius: '0.5rem',
        marginBottom: '1rem',
        fontSize: '0.875rem',
        display: 'flex',
        alignItems: 'center',
        gap: '0.5rem',
    },
    errorIcon: {
        fontSize: '1rem',
    },
    loading: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '3rem',
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
    tableHeader: {
        display: 'grid',
        gridTemplateColumns: '0.8fr 0.6fr 0.6fr 1fr 0.8fr 1fr 1fr 1.2fr 1fr',
        gap: '0.5rem',
        padding: '0.75rem 0',
        borderBottom: '2px solid #334155',
        fontSize: '0.75rem',
        fontWeight: '600',
        color: '#94a3b8',
        textTransform: 'uppercase',
        letterSpacing: '0.05em',
    },
    headerCell: {
        textAlign: 'left',
        fontSize: '0.7rem',
    },
    ordersList: {
        flex: 1,
        overflowY: 'auto',
        maxHeight: '600px',
    },
    orderRow: {
        display: 'grid',
        gridTemplateColumns: '0.8fr 0.6fr 0.6fr 1fr 0.8fr 1fr 1fr 1.2fr 1fr',
        gap: '0.5rem',
        padding: '0.75rem 0',
        borderBottom: '1px solid #334155',
        fontSize: '0.875rem',
        alignItems: 'center',
        transition: 'background-color 0.15s ease',
    },
    symbolCell: {
        display: 'flex',
        alignItems: 'center',
    },
    symbolBadge: {
        backgroundColor: '#0f172a',
        color: '#cbd5e1',
        padding: '0.25rem 0.5rem',
        borderRadius: '0.375rem',
        fontSize: '0.75rem',
        fontWeight: '600',
    },
    sideCell: {
        fontWeight: '600',
    },
    typeCell: {
        color: '#94a3b8',
        fontSize: '0.75rem',
    },
    priceCell: {
        fontWeight: '600',
        color: '#e2e8f0',
    },
    quantityCell: {
        color: '#cbd5e1',
    },
    filledCell: {
        color: '#94a3b8',
        fontSize: '0.75rem',
    },
    statusCell: {
        display: 'flex',
        alignItems: 'center',
    },
    statusBadge: {
        padding: '0.25rem 0.5rem',
        borderRadius: '0.375rem',
        fontSize: '0.75rem',
        fontWeight: '600',
        color: '#ffffff',
    },
    timeCell: {
        color: '#94a3b8',
        fontSize: '0.75rem',
    },
    actionCell: {
        display: 'flex',
        alignItems: 'center',
    },
    cancelButton: {
        padding: '0.375rem 0.75rem',
        border: '1px solid #ef4444',
        borderRadius: '0.375rem',
        fontSize: '0.75rem',
        fontWeight: '500',
        backgroundColor: '#0f172a',
        color: '#f87171',
        cursor: 'pointer',
        transition: 'all 0.2s',
    },
    cancelButtonDisabled: {
        opacity: 0.5,
        cursor: 'not-allowed',
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

export default UserOpenOrders;
