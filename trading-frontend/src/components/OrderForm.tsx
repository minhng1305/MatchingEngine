import React, { useState, useEffect } from 'react';
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
    const [price, setPrice] = useState('');
    const [quantity, setQuantity] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    useEffect(() => {
        if (currentPrice > 0 && type === 'LIMIT') {
            setPrice(currentPrice.toFixed(2));
        }
    }, [currentPrice, type]);

    const validateForm = (): string | null => {
        if (!quantity || parseFloat(quantity) <= 0) {
            return 'Quantity must be greater than 0';
        }
        if (type === 'LIMIT') {
            if (!price || parseFloat(price) <= 0) {
                return 'Price must be greater than 0 for limit orders';
            }
        }
        return null;
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!user) {
            setError('You must be logged in to submit orders');
            return;
        }

        const validationError = validateForm();
        if (validationError) {
            setError(validationError);
            return;
        }

        setLoading(true);
        setError('');
        setSuccess('');

        try {
            const order: Order = {
                userId: user.userId,
                symbol,
                side,
                type,
                price: type === 'MARKET' ? 0 : parseFloat(price),
                quantity: parseInt(quantity),
            };

            await onSubmitOrder(order);
            setSuccess(`Order submitted successfully!`);
            
            // Reset form
            setQuantity('');
            if (type === 'LIMIT') {
                setPrice(currentPrice.toFixed(2));
            }
            
            setTimeout(() => setSuccess(''), 3000);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to submit order');
        } finally {
            setLoading(false);
        }
    };

    const handleSideToggle = (newSide: 'BUY' | 'SELL') => {
        setSide(newSide);
        setError('');
    };

    const calculateTotal = (): number => {
        if (!price || !quantity) return 0;
        return parseFloat(price) * parseInt(quantity);
    };

    const quickPrice = (multiplier: number) => {
        const newPrice = (currentPrice * multiplier).toFixed(2);
        setPrice(newPrice);
    };

    return (
        <div style={styles.container}>
            <div style={styles.header}>
                <h3 style={styles.title}>Place Order</h3>
                <div style={styles.symbolBadge}>{symbol}</div>
            </div>

            {error && (
                <div style={styles.error}>
                    <span style={styles.errorIcon}>⚠️</span>
                    {error}
                </div>
            )}

            {success && (
                <div style={styles.success}>
                    <span style={styles.successIcon}>✓</span>
                    {success}
                </div>
            )}

            <form onSubmit={handleSubmit} style={styles.form}>
                {/* Side Toggle */}
                <div style={styles.sideToggle}>
                    <button
                        type="button"
                        onClick={() => handleSideToggle('BUY')}
                        style={{
                            ...styles.toggleButton,
                            ...(side === 'BUY' ? styles.toggleButtonActiveBuy : {}),
                        }}
                    >
                        Buy
                    </button>
                    <button
                        type="button"
                        onClick={() => handleSideToggle('SELL')}
                        style={{
                            ...styles.toggleButton,
                            ...(side === 'SELL' ? styles.toggleButtonActiveSell : {}),
                        }}
                    >
                        Sell
                    </button>
                </div>

                {/* Order Type */}
                <div style={styles.group}>
                    <label style={styles.label}>Order Type</label>
                    <div style={styles.typeToggle}>
                        <button
                            type="button"
                            onClick={() => setType('LIMIT')}
                            style={{
                                ...styles.typeButton,
                                ...(type === 'LIMIT' ? styles.typeButtonActive : {}),
                            }}
                        >
                            Limit
                        </button>
                        <button
                            type="button"
                            onClick={() => setType('MARKET')}
                            style={{
                                ...styles.typeButton,
                                ...(type === 'MARKET' ? styles.typeButtonActive : {}),
                            }}
                        >
                            Market
                        </button>
                    </div>
                </div>

                {/* Price Input */}
                {type === 'LIMIT' && (
                    <div style={styles.group}>
                        <label style={styles.label}>
                            Limit Price
                            <span style={styles.currentPriceHint}>
                                (Current: ${currentPrice.toFixed(2)})
                            </span>
                        </label>
                        <input
                            type="number"
                            step="0.01"
                            min="0.01"
                            value={price}
                            onChange={(e) => setPrice(e.target.value)}
                            style={styles.input}
                            placeholder="Enter price"
                            required
                        />
                        <div style={styles.quickPrices}>
                            <button type="button" onClick={() => quickPrice(0.95)} style={styles.quickBtn}>
                                -5%
                            </button>
                            <button type="button" onClick={() => quickPrice(0.98)} style={styles.quickBtn}>
                                -2%
                            </button>
                            <button type="button" onClick={() => quickPrice(1.0)} style={styles.quickBtn}>
                                Market
                            </button>
                            <button type="button" onClick={() => quickPrice(1.02)} style={styles.quickBtn}>
                                +2%
                            </button>
                            <button type="button" onClick={() => quickPrice(1.05)} style={styles.quickBtn}>
                                +5%
                            </button>
                        </div>
                    </div>
                )}

                {type === 'MARKET' && (
                    <div style={styles.marketInfo}>
                        <span style={styles.marketIcon}>⚡</span>
                        Market orders execute at the best available price
                    </div>
                )}

                {/* Quantity Input */}
                <div style={styles.group}>
                    <label style={styles.label}>Quantity</label>
                    <input
                        type="number"
                        min="1"
                        step="1"
                        value={quantity}
                        onChange={(e) => setQuantity(e.target.value)}
                        style={styles.input}
                        placeholder="Enter quantity"
                        required
                    />
                    <div style={styles.quickQuantities}>
                        <button type="button" onClick={() => setQuantity('10')} style={styles.quickBtn}>
                            10
                        </button>
                        <button type="button" onClick={() => setQuantity('50')} style={styles.quickBtn}>
                            50
                        </button>
                        <button type="button" onClick={() => setQuantity('100')} style={styles.quickBtn}>
                            100
                        </button>
                        <button type="button" onClick={() => setQuantity('500')} style={styles.quickBtn}>
                            500
                        </button>
                    </div>
                </div>

                {/* Order Summary */}
                {type === 'LIMIT' && price && quantity && (
                    <div style={styles.summary}>
                        <div style={styles.summaryRow}>
                            <span>Total Value:</span>
                            <span style={styles.summaryValue}>
                                ${calculateTotal().toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                            </span>
                        </div>
                    </div>
                )}

                {/* Submit Button */}
                <button
                    type="submit"
                    disabled={loading || !quantity || (type === 'LIMIT' && !price)}
                    style={{
                        ...styles.submitButton,
                        ...(side === 'BUY' ? styles.submitButtonBuy : styles.submitButtonSell),
                        ...(loading || !quantity || (type === 'LIMIT' && !price) ? styles.submitButtonDisabled : {}),
                    }}
                >
                    {loading ? (
                        <>
                            <span style={styles.spinner}></span>
                            Submitting...
                        </>
                    ) : (
                        `${side} ${quantity || '0'} ${symbol} ${type === 'MARKET' ? '(Market)' : `@ $${price || '0.00'}`}`
                    )}
                </button>
            </form>
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
    },
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: '1.5rem',
        paddingBottom: '1rem',
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
    success: {
        backgroundColor: '#14532d',
        border: '1px solid #166534',
        color: '#86efac',
        padding: '0.75rem',
        borderRadius: '0.5rem',
        marginBottom: '1rem',
        fontSize: '0.875rem',
        display: 'flex',
        alignItems: 'center',
        gap: '0.5rem',
    },
    successIcon: {
        fontSize: '1rem',
    },
    form: {
        display: 'flex',
        flexDirection: 'column',
        gap: '1.25rem',
    },
    sideToggle: {
        display: 'grid',
        gridTemplateColumns: '1fr 1fr',
        gap: '0.5rem',
        marginBottom: '0.5rem',
    },
    toggleButton: {
        padding: '0.75rem',
        border: '2px solid #475569',
        borderRadius: '0.5rem',
        fontSize: '1rem',
        fontWeight: '600',
        cursor: 'pointer',
        backgroundColor: '#0f172a',
        color: '#94a3b8',
        transition: 'all 0.2s',
    },
    toggleButtonActiveBuy: {
        backgroundColor: '#10b981',
        color: '#ffffff',
        borderColor: '#10b981',
    },
    toggleButtonActiveSell: {
        backgroundColor: '#ef4444',
        color: '#ffffff',
        borderColor: '#ef4444',
    },
    group: {
        display: 'flex',
        flexDirection: 'column',
        gap: '0.5rem',
    },
    label: {
        fontSize: '0.875rem',
        fontWeight: '600',
        color: '#e2e8f0',
    },
    currentPriceHint: {
        fontSize: '0.75rem',
        fontWeight: '400',
        color: '#94a3b8',
        marginLeft: '0.5rem',
    },
    typeToggle: {
        display: 'grid',
        gridTemplateColumns: '1fr 1fr',
        gap: '0.5rem',
    },
    typeButton: {
        padding: '0.5rem',
        border: '1px solid #475569',
        borderRadius: '0.375rem',
        fontSize: '0.875rem',
        fontWeight: '500',
        cursor: 'pointer',
        backgroundColor: '#0f172a',
        color: '#cbd5e1',
        transition: 'all 0.2s',
    },
    typeButtonActive: {
        backgroundColor: '#3b82f6',
        color: '#ffffff',
        borderColor: '#3b82f6',
    },
    input: {
        padding: '0.75rem',
        border: '1px solid #475569',
        borderRadius: '0.5rem',
        fontSize: '1rem',
        outline: 'none',
        transition: 'border-color 0.2s',
        backgroundColor: '#0f172a',
        color: '#e2e8f0',
    },
    quickPrices: {
        display: 'flex',
        gap: '0.5rem',
        flexWrap: 'wrap',
    },
    quickQuantities: {
        display: 'flex',
        gap: '0.5rem',
        flexWrap: 'wrap',
    },
    quickBtn: {
        padding: '0.25rem 0.75rem',
        border: '1px solid #475569',
        borderRadius: '0.375rem',
        fontSize: '0.75rem',
        backgroundColor: '#0f172a',
        color: '#cbd5e1',
        cursor: 'pointer',
        transition: 'all 0.2s',
    },
    marketInfo: {
        backgroundColor: '#1e3a8a',
        border: '1px solid #3b82f6',
        color: '#93c5fd',
        padding: '0.75rem',
        borderRadius: '0.5rem',
        fontSize: '0.875rem',
        display: 'flex',
        alignItems: 'center',
        gap: '0.5rem',
    },
    marketIcon: {
        fontSize: '1.25rem',
    },
    summary: {
        backgroundColor: '#0f172a',
        border: '1px solid #334155',
        borderRadius: '0.5rem',
        padding: '1rem',
    },
    summaryRow: {
        display: 'flex',
        justifyContent: 'space-between',
        fontSize: '0.875rem',
        color: '#cbd5e1',
    },
    summaryValue: {
        fontWeight: '700',
        color: '#e2e8f0',
        fontSize: '1rem',
    },
    submitButton: {
        padding: '1rem',
        borderRadius: '0.5rem',
        fontSize: '1rem',
        fontWeight: '700',
        cursor: 'pointer',
        border: 'none',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '0.5rem',
        transition: 'all 0.2s',
    },
    submitButtonBuy: {
        backgroundColor: '#10b981',
        color: '#ffffff',
    },
    submitButtonSell: {
        backgroundColor: '#ef4444',
        color: '#ffffff',
    },
    submitButtonDisabled: {
        opacity: 0.5,
        cursor: 'not-allowed',
    },
    spinner: {
        width: '1rem',
        height: '1rem',
        border: '2px solid rgba(255, 255, 255, 0.3)',
        borderTop: '2px solid #ffffff',
        borderRadius: '50%',
        animation: 'spin 0.8s linear infinite',
    },
};

export default OrderForm;
