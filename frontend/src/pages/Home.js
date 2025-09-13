import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import axios from 'axios';

function Home() {
    const [stocks, setStocks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchStocks = async () => {
            try {
                // Replace with your actual API endpoint
                const response = await axios.get('/api/stocks');
                setStocks(response.data);
                setLoading(false);
            } catch (err) {
                setError('Failed to fetch stocks');
                setLoading(false);

                // For development, set dummy data
                setStocks([
                    { symbol: 'AAPL', name: 'Apple Inc.', price: 178.92, change: 1.27 },
                    { symbol: 'TSLA', name: 'Tesla, Inc.', price: 242.15, change: -3.46 },
                    { symbol: 'MS', name: 'Morgan Stanley', price: 89.73, change: 0.52 },
                ]);
            }
        };

        fetchStocks();
    }, []);

    if (loading) return <div className="text-center">Loading stocks...</div>;
    if (error) return <div className="alert alert-danger">{error}</div>;

    return (
        <div>
            <h2>Available Stocks</h2>
            <div className="table-responsive">
                <table className="table table-hover">
                    <thead className="table-light">
                    <tr>
                        <th>Symbol</th>
                        <th>Name</th>
                        <th>Price</th>
                        <th>Change</th>
                        <th>Action</th>
                    </tr>
                    </thead>
                    <tbody>
                    {stocks.map(stock => (
                        <tr key={stock.symbol}>
                            <td>{stock.symbol}</td>
                            <td>{stock.name}</td>
                            <td>${stock.price.toFixed(2)}</td>
                            <td className={stock.change >= 0 ? 'text-success' : 'text-danger'}>
                                {stock.change >= 0 ? '+' : ''}{stock.change.toFixed(2)}%
                            </td>
                            <td>
                                <Link to={`/stocks/${stock.symbol}`} className="btn btn-sm btn-outline-primary">View Details</Link>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
}

export default Home;