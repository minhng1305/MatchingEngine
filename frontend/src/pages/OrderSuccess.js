import React from 'react';
import { Link, useLocation } from 'react-router-dom';

function OrderSuccess() {
    const location = useLocation();
    const orderDetails = location.state?.orderDetails || {
        orderId: '-',
        userId: '-',
        symbol: '-',
        orderType: '-',
        side: '-',
        price: '-',
        limitPrice: '-',
        quantity: '-',
        status: '-',
        submittedAt: '-',
        confirmationNumber: '-'
    };

    return (
        <div className="card">
            <div className="card-header bg-success text-white">
                <h2>Order Submitted Successfully</h2>
            </div>
            <div className="card-body">
                <p>Your order has been submitted and is now being processed.</p>

                <h4 className="mt-4">Order Details</h4>
                <div className="table-responsive">
                    <table className="table table-striped">
                        <tbody>
                        <tr>
                            <th>Order ID:</th>
                            <td>{orderDetails.orderId}</td>
                        </tr>
                        <tr>
                            <th>User ID:</th>
                            <td>{orderDetails.userId}</td>
                        </tr>
                        <tr>
                            <th>Symbol:</th>
                            <td>{orderDetails.symbol}</td>
                        </tr>
                        <tr>
                            <th>Order Type:</th>
                            <td>{orderDetails.orderType}</td>
                        </tr>
                        <tr>
                            <th>Side:</th>
                            <td>{orderDetails.side}</td>
                        </tr>
                        <tr>
                            <th>Price:</th>
                            <td>{orderDetails.price}</td>
                        </tr>
                        <tr>
                            <th>Limit Price:</th>
                            <td>{orderDetails.limitPrice}</td>
                        </tr>
                        <tr>
                            <th>Quantity:</th>
                            <td>{orderDetails.quantity}</td>
                        </tr>
                        <tr>
                            <th>Status:</th>
                            <td>{orderDetails.status}</td>
                        </tr>
                        <tr>
                            <th>Submitted at:</th>
                            <td>{orderDetails.submittedAt}</td>
                        </tr>
                        </tbody>
                    </table>
                </div>

                <p className="mt-3">You will receive a notification when your order is executed.</p>

                <div className="mt-4">
                    <h5>Order confirmation #{orderDetails.confirmationNumber}</h5>
                    <p>Please save this confirmation for your records.</p>
                </div>

                <div className="mt-4 d-flex gap-3">
                    <Link to="/orders/new" className="btn btn-primary">Place Another Order</Link>
                    <Link to="/orders" className="btn btn-secondary">View All Orders</Link>
                </div>
            </div>
        </div>
    );
}

export default OrderSuccess;