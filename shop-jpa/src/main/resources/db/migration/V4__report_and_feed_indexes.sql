CREATE INDEX idx_orders_feed_cursor ON orders (placed_at DESC, id DESC);
CREATE INDEX idx_product_tag_tag_product ON product_tag (tag_id, product_id);
