ALTER TABLE orders ADD COLUMN fulfillment_version INT NOT NULL DEFAULT 0;
CREATE TABLE order_fulfillments (
 order_item_id BIGINT PRIMARY KEY, session_id BIGINT NOT NULL, snapshot JSON NOT NULL, version INT NOT NULL,
 updated_at DATETIME(3) NOT NULL,
 CONSTRAINT fk_fulfill_item FOREIGN KEY(order_item_id) REFERENCES order_items(id),
 CONSTRAINT fk_fulfill_session FOREIGN KEY(session_id) REFERENCES sessions(id)
);
CREATE TABLE order_reschedules (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, order_id BIGINT NOT NULL, request_key VARCHAR(64) NOT NULL,
 expected_version INT NOT NULL, from_session_id BIGINT NOT NULL, to_session_id BIGINT NOT NULL,
 quantity INT NOT NULL, before_snapshot JSON NOT NULL, after_snapshot JSON NOT NULL, actor_id BIGINT NOT NULL, created_at DATETIME(3) NOT NULL,
 UNIQUE KEY uk_reschedule_key(order_id,request_key),
 CONSTRAINT fk_reschedule_order FOREIGN KEY(order_id) REFERENCES orders(id),
 CONSTRAINT fk_reschedule_from FOREIGN KEY(from_session_id) REFERENCES sessions(id),
 CONSTRAINT fk_reschedule_to FOREIGN KEY(to_session_id) REFERENCES sessions(id)
);
CREATE TABLE checkin_tickets (
 token_hash CHAR(64) PRIMARY KEY, order_id BIGINT NOT NULL, order_version INT NOT NULL, expires_at DATETIME(3) NOT NULL,
 used_at DATETIME(3) NULL, consumed_by BIGINT NULL, created_at DATETIME(3) NOT NULL,
 KEY idx_ticket_order(order_id,created_at),
 CONSTRAINT fk_ticket_order FOREIGN KEY(order_id) REFERENCES orders(id)
);
CREATE TABLE order_checkins (
 order_id BIGINT PRIMARY KEY, source VARCHAR(16) NOT NULL, operator_id BIGINT NOT NULL,
 token_hash CHAR(64) NULL, checked_at DATETIME(3) NOT NULL,
 CONSTRAINT fk_checkin_order FOREIGN KEY(order_id) REFERENCES orders(id)
);
