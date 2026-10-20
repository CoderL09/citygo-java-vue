-- Extend baseline without changing V1 checksums. Package contents are immutable per row/version.
ALTER TABLE packages ADD COLUMN category_id BIGINT NULL AFTER id,
    ADD COLUMN family_id BIGINT NULL AFTER version,
    ADD CONSTRAINT fk_packages_category FOREIGN KEY (category_id) REFERENCES categories(id),
    ADD UNIQUE KEY uk_packages_family_version (family_id, version);
ALTER TABLE contacts ADD COLUMN default_user_id BIGINT
    GENERATED ALWAYS AS (CASE WHEN is_default = 1 THEN user_id ELSE NULL END) STORED,
    ADD UNIQUE KEY uk_contacts_default_user (default_user_id);
CREATE INDEX idx_orders_user_created ON orders(user_id, created_at, id);
CREATE INDEX idx_cart_user_created ON cart_items(user_id, id);
