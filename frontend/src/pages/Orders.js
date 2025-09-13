import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import axios from 'axios';

function Orders() {
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchOrders = async () => {
            try {
                const response = await axios.get('/api/orders');
                setOrders(response.data);
                setLoading(false);
            } catch (err) {
                setError('Failed to fetch orders');
                setLoading(false);

                // For development, set dummy data
                setOrders([
                    {
                        id: '1001',
                        symbol: 'AAPL',
                        side: 'BUY',
                        orderType: 'LIMIT',
                        price: null,
                        limitPrice: 180.50,
                        quantity: 100,
                        status: 'PENDING',
                        submittedAt: '2025-09-11T09:45:30'
                    },
                    {
                        id: '1000',
                        symbol: 'TSLA',
                        side: 'SELL',
                        orderType: 'MARKET',
                        price: 242.15,
                        limitPrice: null,
                        quantity: 50,
                        status: 'EXECUTED',
                        submittedAt: '2025-09-10T14:20:15'
                    }
                ]);
            }
        };

        fetchOrders();
    }, []);

    if (loading) return <div className="text-center">Loading orders...</div>;
    if (error) return <div className="alert alert-danger">{error}</div>;

    return (
        <div>
            <div className="d-flex justify-content-between align-items-center mb-4">
                <h2>Your Orders</h2>
                <Link to="/orders/new" className="btn btn-success">
                    Place New Order
                </Link>
            </div>

            <div className="table-responsive">
                <table className="table table-striped">
                    <thead>
                    <tr>
                        <th>Order ID</th>
                        <th>Symbol</th>
                        <th>Type</th>
                        <th>Side</th>
                        <th>Quantity</th>
                        <th>Price</th>
                        <th>Status</th>
                        <th>Submitted</th>
                        <th>Details</th>
                    </tr>
                    </thead>
                    <tbody>
                    {orders.map(order => (
                        <tr key={order.id}>
                            <td>{order.id}</td>
                            <td>{order.symbol}</td>
                            <td>{order.orderType}</td>
                            <td className={order.side === 'BUY' ? 'text-success' : 'text-danger'}>
                                {order.side}
                            </td>
                            <td>{order.quantity}</td>
                            <td>
                                {order.orderType === 'MARKET'
                                    ? (order.price ? `$${order.price.toFixed(2)}` : '-')
                                    : (order.limitPrice ? `$${order.limitPrice.toFixed(2)}` : '-')}
                            </td>
                            <td>
                  <span className={`badge ${order.status === 'EXECUTED' ? 'bg-success' :
                      (order.status === 'PENDING' ? 'bg-warning' : 'bg-secondary')}`}>
                    {order.status}
                  </span>
                            </td>
                            <td>{new Date(order.submittedAt).toLocaleString()}</td>
                            <td>
                                <Link to={`/orders/${order.id}`} className="btn btn-sm btn-outline-primary">
                                    View
                                </Link>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
}

export default Orders;