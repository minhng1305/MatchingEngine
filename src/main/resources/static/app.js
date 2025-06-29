let stompClient = null;

function connect() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);

        // This subscription listens for trade notifications.
        // Note: Without a userId, the backend's `sendToUser` will fail.
        // For now, this will only work if you modify the backend to broadcast trades
        // or once you implement user authentication.
        stompClient.subscribe('/user/queue/trades', function (trade) {
            showTrade(JSON.parse(trade.body));
        });

        // Subscription for public market data updates
        stompClient.subscribe('/topic/market-data', function (marketData) {
            updateMarketData(JSON.parse(marketData.body));
        });
    });
}

function disconnect() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    console.log("Disconnected");
}

function sendOrder() {
    // Construct the order object with field names that match the Order.java class
    // for correct JSON deserialization on the backend.
    const order = {
        symbol: "AAPL", // Hardcoded as requested
        price: parseFloat($("#price").val()),
        originalQuantity: parseInt($("#quantity").val()), // Key name matches Java field
        side: $("#side").val(),
        type: 'LIMIT', // A LIMIT order is appropriate since a price is provided
        limitPrice: parseFloat($("#price").val()), // Required for LIMIT order
        orderTimestamp: new Date().toISOString() // Key name matches Java field
        // orderId and userId are omitted as requested.
    };

    stompClient.send("/app/submit-order", {}, JSON.stringify(order));
    console.log("Sent Order:", order);
}

function showTrade(trade) {
    // This function now correctly populates the updated trade history table
    // using fields from the Trade.java model.
    $("#trade-history-body").append(
        `<tr>
            <td>${trade.buyOrderId}</td>
            <td>${trade.sellOrderId}</td>
            <td>${trade.price}</td>
            <td>${trade.quantity}</td>
        </tr>`
    );
}

function updateMarketData(orderBookSummary) {
    const marketDataBody = $("#market-data-body");
    marketDataBody.empty(); // Clear existing data

    if (orderBookSummary.bids) {
        orderBookSummary.bids.forEach(bid => {
            marketDataBody.append(
                `<tr class="buy">
                    <td>${bid.price}</td>
                    <td>${bid.quantity}</td>
                    <td>BUY</td>
                </tr>`
            );
        });
    }

    if (orderBookSummary.asks) {
        orderBookSummary.asks.forEach(ask => {
            marketDataBody.append(
                `<tr class="sell">
                    <td>${ask.price}</td>
                    <td>${ask.quantity}</td>
                    <td>SELL</td>
                </tr>`
            );
        });
    }
}

$(function () {
    connect();
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
    $("#orderSubmitForm").on('submit', function() { sendOrder(); });
});