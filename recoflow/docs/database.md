# RecoFlow Database

## Assignment-owned source tables

The original assignment image (`mvchub/postgres:13`, database `data-db`) was inspected directly. Its tables use the `public` schema:

- `products`: `product_id varchar(50)` primary key, `category_id varchar(50)`.
- `orders`: `order_id numeric` primary key, `user_id varchar(50)`.
- `order_items`: `id serial` primary key, `product_id varchar(50)`, `quantity numeric`, `order_id numeric`; `order_id` references `orders(order_id)`.

There is no category table, product foreign key, order timestamp, or order status. The assignment states that `orders` already contains only the last month. For an empty local database, migration V1 creates only this verified source shape and never replaces an existing table.

## RecoFlow-owned table

`product_views` stores one row per view. `message_id` is a UUID with a unique constraint and is the duplicate-event guard. User and product identifiers remain compatible with the assignment's `varchar(50)` identifiers. Timestamps use PostgreSQL `timestamp with time zone`.

`product_views.product_id` intentionally has no foreign key to `products`. The assignment event stream can contain unknown product IDs, and RecoFlow preserves those views instead of rejecting them. Category-based recommendation queries naturally consider only views whose product ID resolves through the `products` join.

## Query indexes

- `idx_product_views_user_viewed_at (user_id, viewed_at DESC)`: latest ten views for a user.
- `idx_product_views_user_product (user_id, product_id)`: delete a product from one user's history.
- `idx_order_items_order_id (order_id)`: join order items to purchasing users.
- `idx_order_items_product_order (product_id, order_id)`: category-filtered product joins while covering the order key.
- `idx_products_category_product (category_id, product_id)`: category filtering while covering product IDs.

The original source tables had only primary-key indexes. No separate `message_id` index is added because its unique constraint already creates one. No order-date index exists because the verified source schema has no order-date column.

## Last-Month Order Window

The assignment-provided `orders` table contains only orders from the last month. The verified schema does not include an order timestamp. RecoFlow therefore treats the source table itself as the monthly time window: bestseller queries count distinct purchasing users over all rows in that provided monthly dataset and do not infer time from order IDs or add an unsupported date predicate.

This is a dataset-boundary guarantee, not a rolling runtime cutoff. A production system that retains historical orders would require an indexed order timestamp and an explicit cutoff condition.

## Bestseller queries

Both bestseller queries join `orders`, `order_items`, and `products`, group once per product, and rank by `COUNT(DISTINCT orders.user_id)` descending with product ID ascending as the deterministic tie-breaker. The general query has no category predicate; the personalized query safely binds the selected category list and ranks all eligible products in one combined result. Both queries are bounded to ten rows.
