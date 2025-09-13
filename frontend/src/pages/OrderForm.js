import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

function OrderForm() {
    const navigate = useNavigate();
    const [symbols, setSymbols] = useState([]);
    const [formData, setFormData] = useState({
        userId: '',
        symbol: '',
        orderType: '',
        side: '',
        price: '',
        limitPrice: '',
        quantity: ''
    });
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        // Fetch available symbols for dropdown
        const fetchSymbols = async () => {
            try {
                const response = await axios.get('/api/stocks');
                setSymbols(response.data.map(stock => stock.symbol));
                setLoading(false);
            } catch (err) {
                console.error('Failed to fetch symbols', err);
                setLoading(false);
                // For development
                setSymbols(['AAPL', 'TSLA', 'MS']);
            }
        };

        fetchSymbols();
    }, []);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prevState => ({
            ...prevState,
            [name]: value
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        try {
            const response = await axios.post('/api/orders', formData);
            // Redirect to success page with order details
            navigate('/orders/success', { state: { orderDetails: response.data } });
        } catch (err) {
            // Redirect to error page with error details
            navigate('/orders/error', {
                state: {
                    error: err.response?.data || 'Failed to submit order',
                    attemptedOrder: formData
                }
            });
        }
    };

    if (loading) return <div className="text-center">Loading form...</div>;

    return (
        <div className="card">
            <div className="card-header">
                <h2>Place New Order</h2>
            </div>
            <div className="card-body">
                <form onSubmit={handleSubmit}>
                    <div className="mb-3">
                        <label htmlFor="userId" className="form-label">User ID</label>
                        <input
                            type="text"
                            className="form-control"
                            id="userId"
                            name="userId"
                            value={formData.userId}
                            onChange={handleChange}
                            required
                        />
                    </div>

                    <div className="mb-3">
                        <label htmlFor="symbol" className="form-label">Symbol</label>
                        <select
                            className="form-select"
                            id="symbol"
                            name="symbol"
                            value={formData.symbol}
                            onChange={handleChange}
                            required
                        >
                            <option value="">Select a symbol</option>
                            {symbols.map(symbol => (
                                <option key={symbol} value={symbol}>{symbol}</option>
                            ))}
                        </select>
                    </div>

                    <div className="mb-3">
                        <label htmlFor="orderType" className="form-label">Order Type</label>
                        <select
                            className="form-select"
                            id="orderType"
                            name="orderType"
                            value={formData.orderType}
                            onChange={handleChange}
                            required
                        >
                            <option value="">Select order type</option>
                            <option value="MARKET">MARKET</option>
                            <option value="LIMIT">LIMIT</option>
                        </select>
                    </div>

                    <div className="mb-3">
                        <label htmlFor="side" className="form-label">Side</label>
                        <select
                            className="form-select"
                            id="side"
                            name="side"
                            value={formData.side}
                            onChange={handleChange}
                            required
                        >
                            <option value="">Select order side</option>
                            <option value="BUY">BUY</option>
                            <option value="SELL">SELL</option>
                        </select>
                    </div>

                    {formData.orderType === 'MARKET' && (
                        <div className="mb-3">
                            <label htmlFor="price" className="form-label">Price</label>
                            <input
                                type="number"
                                step="0.01"
                                className="form-control"
                                id="price"
                                name="price"
                                value={formData.price}
                                onChange={handleChange}
                                required
                            />
                        </div>
                    )}

                    {formData.orderType === 'LIMIT' && (
                        <div className="mb-3">
                            <label htmlFor="limitPrice" className="form-label">Limit Price</label>
                            <input
                                type="number"
                                step="0.01"
                                className="form-control"
                                id="limitPrice"
                                name="limitPrice"
                                value={formData.limitPrice}
                                onChange={handleChange}
                                required
                            />
                        </div>
                    )}

                    <div className="mb-3">
                        <label htmlFor="quantity" className="form-label">Quantity</label>
                        <input
                            type="number"
                            className="form-control"
                            id="quantity"
                            name="quantity"
                            value={formData.quantity}
                            onChange={handleChange}
                            required
                        />
                    </div>

                    <button type="submit" className="btn btn-primary">Submit Order</button>
                </form>
            </div>
        </div>
    );
}

export default OrderForm;