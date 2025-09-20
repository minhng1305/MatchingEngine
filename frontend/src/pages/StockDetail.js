import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import axios from 'axios';

function StockDetail() {
    const { symbol } = useParams();
    const [stockData, setStockData] = useState(null);
    const [trades, setTrades] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchStockData = async () => {
            try {
                // Replace with your actual API endpoints
                const stockResponse = await axios.get(`/api/trades/${symbol}`);
                const tradesResponse = await axios.get(`/api/stocks/${symbol}/trades`);

                setStockData(stockResponse.data);
                setTrades(tradesResponse.data);
                setLoading(false);
            } catch (err) {
                setError(`Failed to fetch data for ${symbol}`);
                setLoading(false);

                // For development, set dummy data
                setStockData({
                    symbol: symbol,
                    name: symbol === 'AAPL' ? 'Apple Inc.' : (symbol === 'TSLA' ? 'Tesla, Inc.' : 'Morgan Stanley'),
                    price: 178.92,
                    change: 1.27,
                    volume: 28945612,
                    high: 180.32,
                    low: 177.58
                });

                setTrades([
                    { id: 1, price: 178.92, quantity: 100, timestamp: '2025-09-11T10:23:45', side: 'BUY' },
                    { id: 2, price: 178.90, quantity: 50, timestamp: '2025-09-11T10:22:30', side: 'SELL' },
                    { id: 3, price: 179.05, quantity: 200, timestamp: '2025-09-11T10:20:15', side: 'BUY' }
                ]);
            }
        };

        fetchStockData();
    }, [symbol]);

    if (loading) return <div className="text-center">Loading stock data...</div>;
    if (error) return <div className="alert alert-danger">{error}</div>;

    return (
        <div>
            <div className="d-flex justify-content-between align-items-center mb-4">
                <h2>{stockData.name} ({stockData.symbol})</h2>
                <Link to="/orders/new" className="btn btn-success">
                    Submit New Order
                </Link>
            </div>

            <div className="row mb-4">
                <div className="col-md-6">
                    <div className="card">
                        <div className="card-header">
                            Stock Information
                        </div>
                        <div className="card-body">
                            <div className="row">
                                <div className="col-6">
                                    <h3>${stockData.price.toFixed(2)}</h3>
                                    <p className={stockData.change >= 0 ? 'text-success' : 'text-danger'}>
                                        {stockData.change >= 0 ? '+' : ''}{stockData.change.toFixed(2)}%
                                    </p>
                                </div>
                                <div className="col-6">
                                    <p><strong>Volume:</strong> {stockData.volume.toLocaleString()}</p>
                                    <p><strong>High:</strong> ${stockData.high.toFixed(2)}</p>
                                    <p><strong>Low:</strong> ${stockData.low.toFixed(2)}</p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <h3>Recent Trades</h3>
            <div className="table-responsive">
                <table className="table table-striped">
                    <thead>
                    <tr>
                        <th>Trade ID</th>
                        <th>Price</th>
                        <th>Quantity</th>
                        <th>Side</th>
                        <th>Timestamp</th>
                    </tr>
                    </thead>
                    <tbody>
                    {trades.map(trade => (
                        <tr key={trade.id}>
                            <td>{trade.id}</td>
                            <td>${trade.price.toFixed(2)}</td>
                            <td>{trade.quantity}</td>
                            <td className={trade.side === 'BUY' ? 'text-success' : 'text-danger'}>
                                {trade.side}
                            </td>
                            <td>{new Date(trade.timestamp).toLocaleString()}</td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
}

export default StockDetail;