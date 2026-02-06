import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiService } from '../services/api';
import { Order, Trade } from '../types';

interface UserProfile {
    user: {
        userId: string;
        username: string;
        email: string;
        ledgerBalance?: number;
        availableBalance?: number;
    };
    account?: {
        ledgerBalance: number;
        availableBalance: number;
        holdings: Record<string, number>;
    };
    statistics: {
        totalOrders: number;
        pendingOrders: number;
        filledOrders: number;
        totalTrades: number;
        totalTradeValue: number;
    };
    recentOrders: Order[];
    recentTrades: Trade[];
}

const ProfilePage: React.FC = () => {
    const [profile, setProfile] = useState<UserProfile | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [activeTab, setActiveTab] = useState<'overview' | 'orders' | 'trades'>('overview');

    const navigate = useNavigate();

    useEffect(() => {
        loadUserProfile();
    }, []);

    const loadUserProfile = async () => {
        try {
            setLoading(true);
            setError('');
            const profileData = await apiService.getUserProfile();
            setProfile(profileData);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to load profile');
        } finally {
            setLoading(false);
        }
    };

    const handleBackClick = () => {
        navigate('/');
    };

    const formatCurrency = (amount: number) => {
        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD'
        }).format(amount);
    };

    const formatDate = (dateString: string) => {
        return new Date(dateString).toLocaleString();
    };

    const getOrderStatusColor = (status: string) => {
        switch (status) {
            case 'FILLED': return '#10b981';
            case 'PENDING': return '#f59e0b';
            case 'CANCELLED': return '#ef4444';
            case 'PARTIALLY_FILLED': return '#3b82f6';
            default: return '#6b7280';
        }
    };

    if (loading) {
        return (
            <div style={styles.container}>
                <div style={styles.loading}>
                    <div style={styles.spinner}></div>
                    <p>Loading profile...</p>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div style={styles.container}>
                <div style={styles.error}>
                    <h3>Error Loading Profile</h3>
                    <p>{error}</p>
                    <button onClick={loadUserProfile} style={styles.retryButton}>
                        Try Again
                    </button>
                </div>
            </div>
        );
    }

    if (!profile) {
        return (
            <div style={styles.container}>
                <div style={styles.error}>Profile not found</div>
            </div>
        );
    }

    return (
        <div style={styles.container}>
            <div style={styles.header}>
                <button onClick={handleBackClick} style={styles.backButton}>
                    ← Back to Home
                </button>
                <div style={styles.userInfo}>
                    <div style={styles.avatar}>
                        {profile.user.username.charAt(0).toUpperCase()}
                    </div>
                    <div>
                        <h1 style={styles.username}>{profile.user.username}</h1>
                        <p style={styles.email}>{profile.user.email}</p>
                    </div>
                </div>
            </div>

            {/* Account Balance Cards - Always Visible */}
            <div style={styles.accountSection}>
                <h2 style={styles.accountSectionTitle}>Account Balance</h2>
                <div style={styles.statsGrid}>
                    <div style={styles.accountCard}>
                        <h3 style={styles.accountValue}>
                            {formatCurrency(
                                profile.account?.ledgerBalance ?? 
                                profile.user.ledgerBalance ?? 
                                0
                            )}
                        </h3>
                        <p style={styles.statLabel}>Ledger Balance</p>
                    </div>
                    <div style={styles.accountCard}>
                        <h3 style={styles.accountValue}>
                            {formatCurrency(
                                profile.account?.availableBalance ?? 
                                profile.user.availableBalance ?? 
                                0
                            )}
                        </h3>
                        <p style={styles.statLabel}>Available Balance</p>
                    </div>
                </div>
            </div>

            {/* Statistics Cards */}
            <div style={styles.statsGrid}>
                <div style={styles.statCard}>
                    <h3 style={styles.statValue}>{profile.statistics.totalOrders}</h3>
                    <p style={styles.statLabel}>Total Orders</p>
                </div>
                <div style={styles.statCard}>
                    <h3 style={styles.statValue}>{profile.statistics.pendingOrders}</h3>
                    <p style={styles.statLabel}>Pending Orders</p>
                </div>
                <div style={styles.statCard}>
                    <h3 style={styles.statValue}>{profile.statistics.filledOrders}</h3>
                    <p style={styles.statLabel}>Filled Orders</p>
                </div>
                <div style={styles.statCard}>
                    <h3 style={styles.statValue}>{profile.statistics.totalTrades}</h3>
                    <p style={styles.statLabel}>Total Trades</p>
                </div>
                <div style={styles.statCard}>
                    <h3 style={styles.statValue}>{formatCurrency(profile.statistics.totalTradeValue)}</h3>
                    <p style={styles.statLabel}>Trade Volume</p>
                </div>
            </div>

            {/* Tabs */}
            <div style={styles.tabContainer}>
                <div style={styles.tabs}>
                    <button
                        style={{
                            ...styles.tab,
                            ...(activeTab === 'overview' ? styles.activeTab : {})
                        }}
                        onClick={() => setActiveTab('overview')}
                    >
                        Overview
                    </button>
                    <button
                        style={{
                            ...styles.tab,
                            ...(activeTab === 'orders' ? styles.activeTab : {})
                        }}
                        onClick={() => setActiveTab('orders')}
                    >
                        Recent Orders ({profile.recentOrders.length})
                    </button>
                    <button
                        style={{
                            ...styles.tab,
                            ...(activeTab === 'trades' ? styles.activeTab : {})
                        }}
                        onClick={() => setActiveTab('trades')}
                    >
                        Recent Trades ({profile.recentTrades.length})
                    </button>
                </div>

                <div style={styles.tabContent}>
                    {activeTab === 'overview' && (
                        <div style={styles.overview}>
                            {/* Stock Holdings - Always Show */}
                            <div style={styles.section}>
                                <h3 style={styles.sectionTitle}>Stock Holdings</h3>
                                {profile.account?.holdings && Object.keys(profile.account.holdings).length > 0 ? (
                                    <div style={styles.tableContainer}>
                                        <table style={styles.table}>
                                            <thead>
                                            <tr style={styles.tableHeader}>
                                                <th style={styles.th}>Symbol</th>
                                                <th style={styles.th}>Quantity</th>
                                            </tr>
                                            </thead>
                                            <tbody>
                                            {Object.entries(profile.account.holdings).map(([symbol, quantity]) => (
                                                <tr key={symbol} style={styles.tableRow}>
                                                    <td style={styles.td}>{symbol}</td>
                                                    <td style={styles.td}>{quantity}</td>
                                                </tr>
                                            ))}
                                            </tbody>
                                        </table>
                                    </div>
                                ) : (
                                    <div style={styles.emptyState}>No stock holdings</div>
                                )}
                            </div>

                            {/* Trading Statistics */}
                            <div style={styles.section}>
                                <h3 style={styles.sectionTitle}>Trading Statistics</h3>
                                <div style={styles.summaryGrid}>
                                    <div style={styles.summaryItem}>
                                        <span>Success Rate:</span>
                                        <span style={styles.successRate}>
                      {profile.statistics.totalOrders > 0
                          ? ((profile.statistics.filledOrders / profile.statistics.totalOrders) * 100).toFixed(1)
                          : 0}%
                    </span>
                                    </div>
                                    <div style={styles.summaryItem}>
                                        <span>Average Trade Value:</span>
                                        <span>
                      {profile.statistics.totalTrades > 0
                          ? formatCurrency(profile.statistics.totalTradeValue / profile.statistics.totalTrades)
                          : '$0.00'}
                    </span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}

                    {activeTab === 'orders' && (
                        <div style={styles.section}>
                            <h3 style={styles.sectionTitle}>Recent Orders</h3>
                            {profile.recentOrders.length > 0 ? (
                                <div style={styles.tableContainer}>
                                    <table style={styles.table}>
                                        <thead>
                                        <tr style={styles.tableHeader}>
                                            <th style={styles.th}>Symbol</th>
                                            <th style={styles.th}>Side</th>
                                            <th style={styles.th}>Type</th>
                                            <th style={styles.th}>Price</th>
                                            <th style={styles.th}>Quantity</th>
                                            <th style={styles.th}>Status</th>
                                            <th style={styles.th}>Date</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {profile.recentOrders.map((order, index) => (
                                            <tr key={index} style={styles.tableRow}>
                                                <td style={styles.td}>{order.symbol}</td>
                                                <td style={{
                                                    ...styles.td,
                                                    color: order.side === 'BUY' ? '#10b981' : '#ef4444',
                                                    fontWeight: 'bold'
                                                }}>
                                                    {order.side}
                                                </td>
                                                <td style={styles.td}>{order.type}</td>
                                                <td style={styles.td}>${order.price.toFixed(2)}</td>
                                                <td style={styles.td}>{order.quantity || order.currentQuantity || 0}</td>
                                                <td style={styles.td}>
                            <span style={{
                                ...styles.statusBadge,
                                backgroundColor: getOrderStatusColor(order.status?.toString() || 'PENDING')
                            }}>
                              {order.status || 'PENDING'}
                            </span>
                                                </td>
                                                <td style={styles.td}>
                                                    {order.orderTimestamp ? formatDate(order.orderTimestamp) : 'N/A'}
                                                </td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                </div>
                            ) : (
                                <div style={styles.emptyState}>No orders yet</div>
                            )}
                        </div>
                    )}

                    {activeTab === 'trades' && (
                        <div style={styles.section}>
                            <h3 style={styles.sectionTitle}>Recent Trades</h3>
                            {profile.recentTrades.length > 0 ? (
                                <div style={styles.tableContainer}>
                                    <table style={styles.table}>
                                        <thead>
                                        <tr style={styles.tableHeader}>
                                            <th style={styles.th}>Symbol</th>
                                            <th style={styles.th}>Price</th>
                                            <th style={styles.th}>Quantity</th>
                                            <th style={styles.th}>Total Value</th>
                                            <th style={styles.th}>Date</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {profile.recentTrades.map((trade, index) => (
                                            <tr key={index} style={styles.tableRow}>
                                                <td style={styles.td}>{trade.symbol}</td>
                                                <td style={styles.td}>${trade.price.toFixed(2)}</td>
                                                <td style={styles.td}>{trade.quantity}</td>
                                                <td style={styles.td}>
                                                    {formatCurrency(trade.price * trade.quantity)}
                                                </td>
                                                <td style={styles.td}>
                                                    {formatDate(trade.tradeTimestamp)}
                                                </td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                </div>
                            ) : (
                                <div style={styles.emptyState}>No trades yet</div>
                            )}
                        </div>
                    )}
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
    userInfo: {
        display: 'flex',
        alignItems: 'center',
        gap: '1rem',
    },
    avatar: {
        width: '3rem',
        height: '3rem',
        borderRadius: '50%',
        backgroundColor: '#3b82f6',
        color: 'white',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontSize: '1.5rem',
        fontWeight: 'bold',
    },
    username: {
        fontSize: '1.5rem',
        fontWeight: 'bold',
        color: '#1f2937',
        margin: 0,
    },
    email: {
        color: '#6b7280',
        fontSize: '0.875rem',
        margin: 0,
    },
    accountSection: {
        backgroundColor: '#f0f9ff',
        padding: '1.5rem',
        borderRadius: '0.5rem',
        border: '2px solid #3b82f6',
        marginBottom: '2rem',
    },
    accountSectionTitle: {
        fontSize: '1.25rem',
        fontWeight: 'bold',
        color: '#1f2937',
        margin: '0 0 1rem 0',
    },
    accountCard: {
        backgroundColor: 'white',
        padding: '1.5rem',
        borderRadius: '0.5rem',
        border: '1px solid #3b82f6',
        textAlign: 'center' as const,
        boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)',
    },
    accountValue: {
        fontSize: '2.5rem',
        fontWeight: 'bold',
        color: '#3b82f6',
        margin: '0 0 0.5rem 0',
    },
    statsGrid: {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
        gap: '1rem',
        marginBottom: '2rem',
    },
    statCard: {
        backgroundColor: 'white',
        padding: '1.5rem',
        borderRadius: '0.5rem',
        border: '1px solid #e5e7eb',
        textAlign: 'center' as const,
    },
    statValue: {
        fontSize: '2rem',
        fontWeight: 'bold',
        color: '#1f2937',
        margin: '0 0 0.5rem 0',
    },
    statLabel: {
        color: '#6b7280',
        fontSize: '0.875rem',
        margin: 0,
    },
    tabContainer: {
        backgroundColor: 'white',
        borderRadius: '0.5rem',
        border: '1px solid #e5e7eb',
        overflow: 'hidden',
    },
    tabs: {
        display: 'flex',
        borderBottom: '1px solid #e5e7eb',
    },
    tab: {
        flex: 1,
        padding: '1rem',
        border: 'none',
        backgroundColor: 'transparent',
        cursor: 'pointer',
        fontSize: '0.875rem',
        fontWeight: '500',
        color: '#6b7280',
        transition: 'background-color 0.2s',
    },
    activeTab: {
        backgroundColor: '#f9fafb',
        color: '#3b82f6',
        borderBottom: '2px solid #3b82f6',
    },
    tabContent: {
        padding: '1.5rem',
    },
    overview: {
        display: 'flex',
        flexDirection: 'column' as const,
        gap: '1.5rem',
    },
    section: {
        display: 'flex',
        flexDirection: 'column' as const,
        gap: '1rem',
    },
    sectionTitle: {
        fontSize: '1.125rem',
        fontWeight: 'bold',
        color: '#1f2937',
        margin: 0,
    },
    summaryGrid: {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
        gap: '1rem',
    },
    summaryItem: {
        display: 'flex',
        justifyContent: 'space-between',
        padding: '0.75rem',
        backgroundColor: '#f9fafb',
        borderRadius: '0.25rem',
        fontSize: '0.875rem',
    },
    successRate: {
        fontWeight: 'bold',
        color: '#10b981',
    },
    balanceValue: {
        fontWeight: 'bold',
        color: '#3b82f6',
    },
    tableContainer: {
        overflowX: 'auto' as const,
    },
    table: {
        width: '100%',
        borderCollapse: 'collapse' as const,
    },
    tableHeader: {
        backgroundColor: '#f9fafb',
    },
    th: {
        padding: '0.75rem',
        textAlign: 'left' as const,
        fontSize: '0.875rem',
        fontWeight: '500',
        color: '#374151',
        borderBottom: '1px solid #e5e7eb',
    },
    tableRow: {
        borderBottom: '1px solid #f3f4f6',
    },
    td: {
        padding: '0.75rem',
        fontSize: '0.875rem',
        color: '#1f2937',
    },
    statusBadge: {
        color: 'white',
        padding: '0.25rem 0.5rem',
        borderRadius: '0.25rem',
        fontSize: '0.75rem',
        fontWeight: 'bold',
    },
    emptyState: {
        textAlign: 'center' as const,
        color: '#9ca3af',
        fontSize: '1rem',
        padding: '3rem',
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
    },
};

export default ProfilePage;