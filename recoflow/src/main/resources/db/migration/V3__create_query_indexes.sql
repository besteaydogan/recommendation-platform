CREATE INDEX idx_product_views_user_viewed_at
    ON product_views (user_id, viewed_at DESC);

CREATE INDEX idx_product_views_user_product
    ON product_views (user_id, product_id);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id
    ON order_items (order_id);

CREATE INDEX IF NOT EXISTS idx_order_items_product_order
    ON order_items (product_id, order_id);

CREATE INDEX IF NOT EXISTS idx_products_category_product
    ON products (category_id, product_id);
