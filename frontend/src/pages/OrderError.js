import React from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';

function OrderError() {
    const location = useLocation();
    const navigate = useNavigate();

    const errorData = location.state?.error || 'Unknown error occurred';
    const errorCode = location.state?.error?.code || 'ERR-5102';
    const attemptedOrder = location.state?.attemptedOrder || {
        symbol: 'AAPL',
        side: 'BUY',
        orderType: 'LIMIT',
        quantity: '100'
    };

    const handleTryAgain = () => {
        navigate('/orders/new', { state: { prefillData: attemptedOrder } });
    };

    return (
        <div className="card">
            <div className="card-header bg-danger text-white">
                <h2>Order Submission Failed</h2>
            </div>
            <div className="card-body">
                <p>We couldn't process your order due to an error.</p>

                <div className="alert alert-danger">
                    <h5>Error Details:</h5>
                    <p>{typeof errorData === 'string' ? errorData : errorData.message || 'Insufficient funds available in your account to place this order.'}</p>
                    <p><strong>Error Code:</strong> {errorCode}</p>
                </div>

                <h4>Attempted Order:</h4>
                <div className="table-responsive">
                    <table className="table">
                        <tbody>
                        <tr>
                            <th>Symbol:</th>
                            <td>{attemptedOrder.symbol}</td>
                        </tr>
                        <tr>
                            <th>Side:</th>
                            <td>{attemptedOrder.side}</td>
                        </tr>
                        <tr>
                            <th>Type:</th>
                            <td>{attemptedOrder.orderType}</td>
                        </tr>
                        <tr>
                            <th>Quantity:</th>
                            <td>{attemptedOrder.quantity}</td>
                        </tr>
                        </tbody>
                    </table>
                </div>

                <h4 className="mt-4">Possible Solutions:</h4>
                <ul>
                    <li>Check your account balance and ensure sufficient funds are available.</li>
                    <li>Verify that you have the proper permissions to trade this security.</li>
                    <li>Confirm that the market is open for the selected symbol.</li>
                    <li>Try reducing the order quantity.</li>
                </ul>

                <div className="mt-4 d-flex gap-3">
                    <button onClick={handleTryAgain} className="btn btn-primary">Try Again</button>
                    <a href="mailto:support@tradingplatform.com" className="btn btn-secondary">Contact Support</a>
                </div>

                <p className="mt-3">
                    If you need assistance, please contact our support team at <a href="mailto:support@tradingplatform.com">support@tradingplatform.com</a>
                </p>
            </div>
        </div>
    );
}

export default OrderError;