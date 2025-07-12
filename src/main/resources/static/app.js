let stompClient = new StompJs.Client({
    brokerURL: 'ws://localhost:8080/ws',
    debug: function (str) {
        console.log(str); // Optional: for debugging STOMP messages
    },
    reconnectDelay: 5000, // Optional: delay before attempting reconnect
    heartbeatIncoming: 4000, // Optional: expected incoming heartbeat interval
    heartbeatOutgoing: 4000, // Optional: desired outgoing heartbeat interval
});
let connected = false;

function connect() {
    // Clear previous logs
    document.getElementById('logs').innerHTML = '';
    log('🔄 Attempting to connect to WebSocket server...');
    
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    
    // Enable debug logging
    stompClient.debug = function(str) {
        log('DEBUG: ' + str);
    };
    
    const connectHeaders = {};
    
    stompClient.connect(connectHeaders, function (frame) {
        connected = true;
        updateConnectionStatus();
        log('✅ Successfully connected to WebSocket server');
        log('📡 Session: ' + frame.headers['session']);
        
        // Subscribe to user-specific order updates
        const userId = document.getElementById('userId').value;
        try {
            stompClient.subscribe('/user/queue/orders', function (message) {
                try {
                    log('📦 Raw order message: ' + message.body);
                    const order = JSON.parse(message.body);
                    displayOrder(order);
                    log('📦 Processed order update: ' + order.orderId + ' - ' + order.status);
                } catch (e) {
                    log('❌ Error parsing order message: ' + e.message);
                }
            });
            log('✅ Subscribed to user order updates: /user/queue/orders');
        } catch (e) {
            log('❌ Error subscribing to user orders: ' + e.message);
        }
        
        // Subscribe to market data updates (order book)
        try {
            stompClient.subscribe('/topic/market-data', function (message) {
                try {
                    log('📊 Raw market data: ' + message.body);
                    const orderBook = JSON.parse(message.body);
                    displayOrderBook(orderBook);
                    log('📊 Processed order book update for ' + orderBook.symbol);
                } catch (e) {
                    log('❌ Error parsing order book message: ' + e.message);
                }
            });
            log('✅ Subscribed to market data: /topic/market-data');
        } catch (e) {
            log('❌ Error subscribing to market data: ' + e.message);
        }
        
        // Test connection by sending a simple message
        setTimeout(() => {
            log('🔔 Connection established successfully!');
        }, 1000);
        
    }, function (error) {
        connected = false;
        updateConnectionStatus();
        log('❌ Connection failed: ' + error);
        console.error('STOMP error:', error);
        
        // Try to reconnect after 5 seconds
        setTimeout(() => {
            log('🔄 Attempting to reconnect...');
            connect();
        }, 5000);
    });
}

function disconnect() {
    if (stompClient !== null && connected) {
        stompClient.disconnect(function() {
            connected = false;
            updateConnectionStatus();
            log('🔌 Disconnected from WebSocket server');
        });
    } else {
        log('⚠️ No active connection to disconnect');
    }
}

function submitOrder() {
    if (!connected) {
        log('❌ Not connected to server');
        alert('Please connect to the server first!');
        return;
    }
    
    const userId = document.getElementById('userId').value;
    const symbol = document.getElementById('symbol').value;
    const side = document.getElementById('side').value;
    const type = document.getElementById('type').value;
    const quantity = parseInt(document.getElementById('quantity').value);
    const price = parseFloat(document.getElementById('price').value);
    
    if (!userId || !symbol || !quantity || (type === 'LIMIT' && !price)) {
        log('❌ Please fill in all required fields');
        alert('Please fill in all required fields!');
        return;
    }
    
    const order = {
        userId: userId,
        symbol: symbol,
        side: side,
        type: type,
        originalQuantity: quantity,
        currentQuantity: quantity,
        price: price,
        limitPrice: type === 'LIMIT' ? price : 0
    };
    
    try {
        log('📤 Sending order: ' + JSON.stringify(order));
        stompClient.send('/app/submit-order', {}, JSON.stringify(order));
        log('✅ Order sent successfully: ' + side + ' ' + quantity + ' ' + symbol + ' @ ' + price);
    } catch (error) {
        log('❌ Error submitting order: ' + error.message);
        console.error('Order submission error:', error);
    }
}

function displayOrderBook(orderBook) {
    log('📈 Updating order book display for ' + orderBook.symbol);
    
    // Update bids table
    const bidsTable = document.getElementById('bidsTable').getElementsByTagName('tbody')[0];
    bidsTable.innerHTML = '';
    
    if (orderBook.bids && orderBook.bids.length > 0) {
        orderBook.bids.forEach(bid => {
            const row = bidsTable.insertRow();
            row.innerHTML = `<td class="buy">$${parseFloat(bid.price).toFixed(2)}</td><td>${bid.quantity}</td>`;
        });
    } else {
        const row = bidsTable.insertRow();
        row.innerHTML = '<td colspan="2" style="text-align: center; color: #666;">No bids</td>';
    }
    
    // Update asks table
    const asksTable = document.getElementById('asksTable').getElementsByTagName('tbody')[0];
    asksTable.innerHTML = '';
    
    if (orderBook.asks && orderBook.asks.length > 0) {
        orderBook.asks.forEach(ask => {
            const row = asksTable.insertRow();
            row.innerHTML = `<td class="sell">$${parseFloat(ask.price).toFixed(2)}</td><td>${ask.quantity}</td>`;
        });
    } else {
        const row = asksTable.insertRow();
        row.innerHTML = '<td colspan="2" style="text-align: center; color: #666;">No asks</td>';
    }
    
    // Update symbol display
    document.getElementById('orderBookSymbol').textContent = orderBook.symbol || 'Unknown';
}

function displayOrder(order) {
    const ordersDiv = document.getElementById('orders');
    
    // Remove "No orders" message if it exists
    if (ordersDiv.querySelector('div[style*="text-align: center"]')) {
        ordersDiv.innerHTML = '';
    }
    
    const orderElement = document.createElement('div');
    orderElement.className = 'order-item';
    orderElement.innerHTML = `
        <div><strong>Order ID:</strong> ${order.orderId}</div>
        <div><strong>Status:</strong> <span style="color: ${getStatusColor(order.status)}">${order.status}</span></div>
        <div><strong>Side:</strong> <span class="${order.side.toLowerCase()}">${order.side}</span></div>
        <div><strong>Symbol:</strong> ${order.symbol}</div>
        <div><strong>Quantity:</strong> ${order.originalQuantity} (Remaining: ${order.currentQuantity})</div>
        <div><strong>Price:</strong> $${parseFloat(order.price || 0).toFixed(2)}</div>
        <div><strong>Time:</strong> ${new Date(order.orderTimestamp).toLocaleString()}</div>
    `;
    ordersDiv.insertBefore(orderElement, ordersDiv.firstChild);
    
    // Keep only last 10 orders
    while (ordersDiv.children.length > 10) {
        ordersDiv.removeChild(ordersDiv.lastChild);
    }
}

function getStatusColor(status) {
    switch (status) {
        case 'FILLED': return '#28a745';
        case 'PARTIALLY_FILLED': return '#ffc107';
        case 'PENDING': return '#17a2b8';
        case 'CANCELLED': return '#dc3545';
        default: return '#6c757d';
    }
}

function updateConnectionStatus() {
    const statusElement = document.getElementById('status');
    if (connected) {
        statusElement.textContent = 'Connected ✅';
        statusElement.className = 'connected';
    } else {
        statusElement.textContent = 'Disconnected ❌';
        statusElement.className = 'disconnected';
    }
}

function log(message) {
    const logsDiv = document.getElementById('logs');
    const logElement = document.createElement('div');
    logElement.className = 'log-item';
    logElement.textContent = new Date().toLocaleTimeString() + ': ' + message;
    logsDiv.appendChild(logElement);
    logsDiv.scrollTop = logsDiv.scrollHeight;
    
    // Also log to console for debugging
    console.log(message);
}

// Event listeners
document.getElementById('connect').addEventListener('click', function() {
    log('🔘 Connect button clicked');
    connect();
});

document.getElementById('disconnect').addEventListener('click', function() {
    log('🔘 Disconnect button clicked');
    disconnect();
});

// Auto-connect on page load
window.addEventListener('load', function() {
    log('🌐 Page loaded, waiting 2 seconds before auto-connecting...');
    setTimeout(() => {
        log('🚀 Starting auto-connection...');
        connect();
    }, 2000);
});

// Handle form submission with Enter key
document.addEventListener('keypress', function(e) {
    if (e.key === 'Enter' && e.target.tagName !== 'BUTTON') {
        submitOrder();
    }
});