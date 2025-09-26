import React, { useState } from 'react';
import { Order } from '../types';
import { useAuth } from '../context/AuthContext';

interface OrderFormProps {
    symbol: string;
    currentPrice: number;
    onSubmitOrder: (order: Order) => Promise<void>;
}

const OrderForm: React.FC<OrderFormProps> = ({ symbol, currentPrice, onSubmitOrder }) => {
    const { user } = useAuth();
    const [side, setSide] = useState<'BUY' | 'SELL'>('BUY');
    const [type, setType] = useState<'LIMIT' | 'MARKET'>('LIMIT');
    const [price, setPrice] = useState(currentPrice.toString());
    const [quantity, setQuantity] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    // 🆕 NEW: Testing mode toggle
    const [testingMode, setTestingMode] = useState(true);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!user) return;

        setLoading(true);
        setError('');

        try {
            const order: Order = {
                userId: user.userId,
                symbol,
                side,
                type,
                // ✅ MODIFIED: Allow custom price for market orders in testing mode
                price: testingMode ? parseFloat(price) : (type === 'MARKET' ? 0 : parseFloat(price)),
                quantity: parseInt(quantity),
            };

            await onSubmitOrder(order);

            // Reset form
            setQuantity('');
            setPrice(currentPrice.toString());
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to submit order');
        } finally {
            setLoading(false);
        }
    };

    const handleTypeChange = (newType: 'LIMIT' | 'MARKET') => {
        setType(newType);
        // Auto-fill price suggestions for testing
        if (newType === 'MARKET' && testingMode) {
            if (side === 'BUY') {
                // Suggest a higher price for market buy orders
                setPrice((currentPrice * 1.05).toFixed(2));
            } else {
                // Suggest a lower price for market sell orders
                setPrice((currentPrice * 0.95).toFixed(2));
            }
        } else if (newType === 'LIMIT') {
            setPrice(currentPrice.toString());
        }
    };

    const handleSideChange = (newSide: 'BUY' | 'SELL') => {
        setSide(newSide);
        // Auto-adjust suggested prices when side changes in market mode
        if (type === 'MARKET' && testingMode) {
            if (newSide === 'BUY') {
                setPrice((currentPrice * 1.05).toFixed(2));
            } else {
                setPrice((currentPrice * 0.95).toFixed(2));
            }
        }
    };

    const getPriceSuggestions = () => {
        const current = currentPrice;
        return [
            { label: 'Current Price', value: current.toFixed(2) },
            { label: '+5%', value: (current * 1.05).toFixed(2) },
            { label: '+10%', value: (current * 1.10).toFixed(2) },
            { label: '-5%', value: (current * 0.95).toFixed(2) },
            { label: '-10%', value: (current * 0.90).toFixed(2) },
        ];
    };

    return (
        <div style={styles.container}>
            <div style={styles.header}>
                <h3 style={styles.title}>Submit Order</h3>

                {/* 🆕 NEW: Testing Mode Toggle */}
                <div style={styles.toggleContainer}>
                    <label style={styles.toggleLabel}>
                        <input
                            type="checkbox"
                            checked={testingMode}
                            onChange={(e) => setTestingMode(e.target.checked)}
                            style={styles.checkbox}
                        />
                        <span style={styles.toggleText}>Testing Mode</span>
                    </label>
                </div>
            </div>

            {error && <div style={styles.error}>{error}</div>}

            <form onSubmit={handleSubmit} style={styles.form}>
                <div style={styles.row}>
                    <div style={styles.group}>
                        <label style={styles.label}>Side</label>
                        <select
                            value={side}
                            onChange={(e) => handleSideChange(e.target.value as 'BUY' | 'SELL')}
                            style={{...styles.input, color: side === 'BUY' ? '#10b981' : '#ef4444'}}
                        >
                            <option value="BUY">BUY</option>
                            <option value="SELL">SELL</option>
                        </select>
                    </div>

                    <div style={styles.group}>
                        <label style={styles.label}>Type</label>
                        <select
                            value={type}
                            onChange={(e) => handleTypeChange(e.target.value as 'LIMIT' | 'MARKET')}
                            style={styles.input}
                        >
                            <option value="LIMIT">LIMIT</option>
                            <option value="MARKET">MARKET</option>
                        </select>
                    </div>
                </div>

                <div style={styles.group}>
                    <label style={styles.label}>
                        Price
                        {type === 'MARKET' && !testingMode && ' (Market Order)'}
                        {type === 'MARKET' && testingMode && ' (Testing: Custom Price)'}
                    </label>

                    {/* ✅ MODIFIED: Price input always enabled in testing mode */}
                    <input
                        type="number"
                        step="0.01"
                        value={price}
                        onChange={(e) => setPrice(e.target.value)}
                        disabled={type === 'MARKET' && !testingMode}
                        style={{
                            ...styles.input,
                            backgroundColor: (type === 'MARKET' && !testingMode) ? '#f3f4f6' : 'white',
                            border: testingMode && type === 'MARKET' ? '2px solid #3b82f6' : '1px solid #d1d5db'
                        }}
                        placeholder={type === 'MARKET' && testingMode ? 'Enter test price' : 'Enter limit price'}
                        required
                    />

                    {/* 🆕 NEW: Price suggestions for testing */}
                    {testingMode && (
                        <div style={styles.priceSuggestions}>
                            <span style={styles.suggestionsLabel}>Quick prices:</span>
                            {getPriceSuggestions().map((suggestion) => (
                                <button
                                    key={suggestion.label}
                                    type="button"
                                    onClick={() => setPrice(suggestion.value)}
                                    style={styles.suggestionBtn}
                                >
                                    {suggestion.label}: ${suggestion.value}
                                </button>
                            ))}
                        </div>
                    )}
                </div>

                <div style={styles.group}>
                    <label style={styles.label}>Quantity</label>
                    <input
                        type="number"
                        min="1"
                        value={quantity}
                        onChange={(e) => setQuantity(e.target.value)}
                        style={styles.input}
                        placeholder="Enter quantity"
                        required
                    />
                </div>

                {/* 🆕 NEW: Order summary */}
                {price && quantity && (
                    <div style={styles.orderSummary}>
                        <h4 style={styles.summaryTitle}>Order Summary:</h4>
                        <div style={styles.summaryRow}>
                            <span>Action:</span>
                            <span style={{color: side === 'BUY' ? '#10b981' : '#ef4444', fontWeight: 'bold'}}>
                {side} {quantity} shares of {symbol}
              </span>
                        </div>
                        <div style={styles.summaryRow}>
                            <span>Price:</span>
                            <span>${parseFloat(price).toFixed(2)} per share</span>
                        </div>
                        <div style={styles.summaryRow}>
                            <span>Total Value:</span>
                            <span style={{fontWeight: 'bold'}}>
                ${(parseFloat(price) * parseInt(quantity || '0')).toFixed(2)}
              </span>
                        </div>
                        <div style={styles.summaryRow}>
                            <span>Order Type:</span>
                            <span>
                {type}
                                {type === 'MARKET' && testingMode && ' (Testing Mode)'}
              </span>
                        </div>
                    </div>
                )}

                <button
                    type="submit"
                    disabled={loading || !quantity}
                    style={{
                        ...styles.submitBtn,
                        backgroundColor: side === 'BUY' ? '#10b981' : '#ef4444',
                        opacity: loading || !quantity ? 0.5 : 1
                    }}
                >
                    {loading ? 'Submitting...' : `${side} ${symbol}`}
                </button>
            </form>

            {/* 🆕 NEW: Testing mode info */}
            {testingMode && (
                <div style={styles.testingInfo}>
                    <h4 style={styles.infoTitle}>🧪 Testing Mode Active</h4>
                    <ul style={styles.infoList}>
                        <li>✅ Market orders can specify custom prices</li>
                        <li>✅ Test different price ranges for order matching</li>
                        <li>✅ Quick price suggestions available</li>
                        <li>✅ Perfect for testing your matching engine</li>
                    </ul>
                </div>
            )}
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
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: '1rem',
    },
    title: {
        fontSize: '1.125rem',
        fontWeight: 'bold',
        color: '#1f2937',
    },
    toggleContainer: {
        display: 'flex',
        alignItems: 'center',
    },
    toggleLabel: {
        display: 'flex',
        alignItems: 'center',
        cursor: 'pointer',
        fontSize: '0.875rem',
    },
    checkbox: {
        marginRight: '0.5rem',
    },
    toggleText: {
        color: '#3b82f6',
        fontWeight: '500',
    },
    error: {
        backgroundColor: '#fef2f2',
        border: '1px solid #fecaca',
        color: '#dc2626',
        padding: '0.75rem',
        borderRadius: '0.25rem',
        marginBottom: '1rem',
        fontSize: '0.875rem',
    },
    form: {
        display: 'flex',
        flexDirection: 'column' as const,
        gap: '1rem',
    },
    row: {
        display: 'grid',
        gridTemplateColumns: '1fr 1fr',
        gap: '1rem',
    },
    group: {
        display: 'flex',
        flexDirection: 'column' as const,
    },
    label: {
        fontSize: '0.875rem',
        fontWeight: '500',
        color: '#374151',
        marginBottom: '0.25rem',
    },
    input: {
        padding: '0.5rem',
        border: '1px solid #d1d5db',
        borderRadius: '0.25rem',
        fontSize: '0.875rem',
        outline: 'none',
        transition: 'border-color 0.2s',
    },
    priceSuggestions: {
        display: 'flex',
        flexWrap: 'wrap' as const,
        gap: '0.5rem',
        marginTop: '0.5rem',
        padding: '0.75rem',
        backgroundColor: '#f8fafc',
        borderRadius: '0.25rem',
        border: '1px solid #e2e8f0',
    },
    suggestionsLabel: {
        fontSize: '0.75rem',
        color: '#64748b',
        fontWeight: '500',
        width: '100%',
        marginBottom: '0.25rem',
    },
    suggestionBtn: {
        backgroundColor: '#e2e8f0',
        border: '1px solid #cbd5e1',
        borderRadius: '0.25rem',
        padding: '0.25rem 0.5rem',
        fontSize: '0.75rem',
        cursor: 'pointer',
        transition: 'background-color 0.2s',
        color: '#475569',
    },
    orderSummary: {
        backgroundColor: '#f8fafc',
        border: '1px solid #e2e8f0',
        borderRadius: '0.25rem',
        padding: '1rem',
    },
    summaryTitle: {
        fontSize: '0.875rem',
        fontWeight: '600',
        color: '#374151',
        marginBottom: '0.5rem',
    },
    summaryRow: {
        display: 'flex',
        justifyContent: 'space-between',
        fontSize: '0.875rem',
        marginBottom: '0.25rem',
        color: '#4b5563',
    },
    submitBtn: {
        color: 'white',
        border: 'none',
        padding: '0.75rem 1.5rem',
        borderRadius: '0.25rem',
        fontSize: '0.875rem',
        fontWeight: '500',
        cursor: 'pointer',
        transition: 'opacity 0.2s',
    },
    testingInfo: {
        backgroundColor: '#fef3c7',
        border: '1px solid #f59e0b',
        borderRadius: '0.25rem',
        padding: '1rem',
        marginTop: '1rem',
    },
    infoTitle: {
        fontSize: '0.875rem',
        fontWeight: '600',
        color: '#92400e',
        marginBottom: '0.5rem',
    },
    infoList: {
        fontSize: '0.75rem',
        color: '#b45309',
        margin: 0,
        paddingLeft: '1rem',
    },
};

export default OrderForm;
