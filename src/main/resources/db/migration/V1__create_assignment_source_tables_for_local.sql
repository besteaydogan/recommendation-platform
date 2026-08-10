CREATE TABLE IF NOT EXISTS products (
    product_id VARCHAR(50) PRIMARY KEY,
    category_id VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
    order_id NUMERIC PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS order_items (
    id SERIAL PRIMARY KEY,
    product_id VARCHAR(50) NOT NULL,
    quantity NUMERIC NOT NULL,
    order_id NUMERIC NOT NULL,
    CONSTRAINT fk_order_id
        FOREIGN KEY (order_id)
        REFERENCES orders (order_id)
);
