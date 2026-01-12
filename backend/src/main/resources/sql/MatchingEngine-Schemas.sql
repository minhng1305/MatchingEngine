REATE TABLE public.users (
    user_id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
	ledger_balance DOUBLE PRECISION,
	available_balance DOUBLE PRECISION,
	current_esg_points INTEGER NOT NULL
);
---------------------------------------------------------------------------------------
CREATE TABLE public.portfolios (
    user_id UUID NOT NULL,
    symbol VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, symbol),
    CONSTRAINT fk_portfolio_user FOREIGN KEY(user_id) REFERENCES public.users(user_id)
);
---------------------------------------------------------------------------------------
CREATE TYPE order_side_enum AS ENUM ('BUY', 'SELL');
CREATE TYPE order_type_enum AS ENUM ('MARKET', 'LIMIT');
CREATE TYPE order_status_enum AS ENUM ('PENDING', 'FILLED', 'PARTIALLY_FILLED', 'CANCELLED');

CREATE TABLE public.orders (
    order_id UUID PRIMARY KEY,
    user_id UUID NOT NULL, -- This column has been changed from user_email
    symbol VARCHAR(255) NOT NULL,
    price DOUBLE PRECISION, -- Price can be nullable for MARKET orders
    original_quantity INTEGER NOT NULL,
    current_quantity INTEGER NOT NULL,
    side order_side_enum,
    type order_type_enum,
    limit_price DOUBLE PRECISION, -- Limit price is only relevant for LIMIT orders
    order_timestamp TIMESTAMP NOT NULL,
    status order_status_enum,
    CONSTRAINT fk_user FOREIGN KEY(user_id) REFERENCES public.users(user_id) -- The constraint is updated
);
-------------------------------------------------------------------------------------
CREATE TABLE public.trades (
    trade_id UUID PRIMARY KEY,
    symbol VARCHAR(255) NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    quantity INTEGER NOT NULL,
    buy_order_id UUID NOT NULL,
    sell_order_id UUID NOT NULL,
    trade_timestamp TIMESTAMP NOT NULL,
    CONSTRAINT fk_buy_order FOREIGN KEY(buy_order_id) REFERENCES public.orders(order_id),
    CONSTRAINT fk_sell_order FOREIGN KEY(sell_order_id) REFERENCES public.orders(order_id)
);

CREATE INDEX idx_orders_user_id ON public.orders(user_id);
CREATE INDEX idx_orders_symbol_status ON public.orders(symbol, status);
CREATE INDEX idx_trades_symbol ON public.trades(symbol);