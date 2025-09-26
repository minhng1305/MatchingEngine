import React from 'react';
import { Stock } from '../types';

interface StockCardProps {
    stock: Stock;
    onClick: (symbol: string) => void;
}

const StockCard: React.FC<StockCardProps> = ({ stock, onClick }) => {
    const priceColor = stock.currentPrice > 0 ? '#10b981' : '#ef4444';

    return (
        <div style={styles.card} onClick={() => onClick(stock.symbol)}>
            <div style={styles.header}>
                <h3 style={styles.symbol}>{stock.symbol}</h3>
                <span style={{...styles.price, color: priceColor}}>
          ${stock.currentPrice.toFixed(2)}
        </span>
            </div>
            <p style={styles.companyName}>{stock.companyName}</p>
            <div style={styles.footer}>
                <span style={styles.esg}>ESG Score: {stock.esgScore}</span>
            </div>
        </div>
    );
};

const styles = {
    card: {
        backgroundColor: 'white',
        border: '1px solid #e5e7eb',
        borderRadius: '0.5rem',
        padding: '1.5rem',
        cursor: 'pointer',
        transition: 'all 0.2s',
        boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)',
    },
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        marginBottom: '0.5rem',
    },
    symbol: {
        fontSize: '1.25rem',
        fontWeight: 'bold',
        color: '#1f2937',
        margin: 0,
    },
    price: {
        fontSize: '1.125rem',
        fontWeight: 'bold',
    },
    companyName: {
        color: '#6b7280',
        fontSize: '0.875rem',
        margin: '0.5rem 0 1rem 0',
        lineHeight: '1.25',
    },
    footer: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
    },
    esg: {
        fontSize: '0.75rem',
        color: '#9ca3af',
        backgroundColor: '#f3f4f6',
        padding: '0.25rem 0.5rem',
        borderRadius: '0.25rem',
    },
};

export default StockCard;