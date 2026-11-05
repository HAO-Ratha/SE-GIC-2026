-- PostgreSQL only (ON CONFLICT). Safe to run on every start.
INSERT INTO customer (id, name, email) VALUES (1, 'Sok Dara', 'sok@example.com') ON CONFLICT (id) DO NOTHING;
INSERT INTO customer (id, name, email) VALUES (2, 'Chan Sreyneang', 'chan@example.com') ON CONFLICT (id) DO NOTHING;

INSERT INTO product (id, name, price, stock) VALUES (1, 'Mechanical Keyboard', 49.90, 20) ON CONFLICT (id) DO NOTHING;
INSERT INTO product (id, name, price, stock) VALUES (2, 'Wireless Mouse',      25.00, 5)  ON CONFLICT (id) DO NOTHING;
INSERT INTO product (id, name, price, stock) VALUES (3, 'USB Cable',           10.00, 50) ON CONFLICT (id) DO NOTHING;
INSERT INTO product (id, name, price, stock) VALUES (4, 'Webcam',              59.50, 0)  ON CONFLICT (id) DO NOTHING;