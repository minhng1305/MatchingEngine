CREATE TABLE public.users (
    email VARCHAR(255) PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL
);

---------------------------------------------------------------------------------------
CREATE TYPE order_side_enum AS ENUM ('BUY', 'SELL');
CREATE TYPE order_type_enum AS ENUM ('MARKET', 'LIMIT');
CREATE TYPE order_status_enum AS ENUM ('PENDING', 'FILLED', 'PARTIALLY_FILLED', 'CANCELLED');

CREATE TABLE public.orders (
    order_id UUID PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL,
    symbol VARCHAR(255) NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    original_quantity INTEGER NOT NULL,
    current_quantity INTEGER NOT NULL,
    side order_side_enum,
    type order_type_enum,
    limit_price DOUBLE PRECISION NOT NULL,
    order_timestamp TIMESTAMP NOT NULL,
    status order_status_enum,
    CONSTRAINT fk_user_email FOREIGN KEY(user_email) REFERENCES public.users(email)
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