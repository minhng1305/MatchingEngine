CREATE TABLE public.users (
    user_id UUID PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL
);

-- ---------------------------------------------------------------------------------------

-- First, create the ENUM types
CREATE TYPE order_side AS ENUM ('BUY', 'SELL');
CREATE TYPE order_type AS ENUM ('MARKET', 'LIMIT');
CREATE TYPE order_status AS ENUM ('PENDING', 'FILLED', 'PARTIALLY_FILLED', 'CANCELLED');

-- Create the orders table
CREATE TABLE public.orders (
    order_id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    symbol VARCHAR(50) NOT NULL,
    price NUMERIC(19,8) NOT NULL,
    original_quantity INTEGER NOT NULL,
    current_quantity INTEGER NOT NULL,
    side order_side NOT NULL,
    type order_type NOT NULL,
    limit_price NUMERIC(19,8) NOT NULL,
    order_timestamp TIMESTAMP NOT NULL,
    status order_status NOT NULL DEFAULT 'PENDING'
);

-- Add indexes for common query patterns
CREATE INDEX idx_orders_user_id ON public.orders(user_id);
CREATE INDEX idx_orders_symbol ON public.orders(symbol);
CREATE INDEX idx_orders_status ON public.orders(status);

-------------------------------------------------------------------------------------------

CREATE TABLE public.trades (
    trade_id UUID PRIMARY KEY DEFAULT gen_random_uuid(), -- Added a primary key
    symbol VARCHAR(50) NOT NULL,
    price NUMERIC(19,8) NOT NULL,
    quantity INTEGER NOT NULL,
    buy_order_id UUID NOT NULL,
    sell_order_id UUID NOT NULL,
    trade_timestamp TIMESTAMP NOT NULL,
    
    -- Foreign key constraints
    CONSTRAINT fk_buy_order FOREIGN KEY (buy_order_id) 
        REFERENCES public.orders (order_id),
    CONSTRAINT fk_sell_order FOREIGN KEY (sell_order_id) 
        REFERENCES public.orders (order_id)
);

-- Add indexes for common query patterns
CREATE INDEX idx_trades_symbol ON public.trades(symbol);
CREATE INDEX idx_trades_timestamp ON public.trades(trade_timestamp);
CREATE INDEX idx_trades_buy_order ON public.trades(buy_order_id);
CREATE INDEX idx_trades_sell_order ON public.trades(sell_order_id);








