ALTER TABLE orders ADD COLUMN close_intent VARCHAR(16) NULL,
    ADD COLUMN close_retry_at DATETIME(3) NULL;
ALTER TABLE payment_attempts ADD COLUMN active_order_id BIGINT GENERATED ALWAYS AS
    (CASE WHEN status IN ('PENDING','UNKNOWN') THEN order_id ELSE NULL END) STORED,
    ADD UNIQUE KEY uk_payment_active_order(active_order_id),
    ADD COLUMN next_check_at DATETIME(3) NULL,
    ADD UNIQUE KEY uk_payment_external(channel,external_trade_no);
-- A full refund is unique for each actual payment, including excess/late-payment compensation.
ALTER TABLE refunds ADD COLUMN compensation TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN next_check_at DATETIME(3) NULL,
    ADD UNIQUE KEY uk_refund_payment(payment_attempt_id);
CREATE TABLE payment_anomalies (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    payment_attempt_id BIGINT NOT NULL, order_id BIGINT NOT NULL,
    reason VARCHAR(128) NOT NULL, resolved TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL, UNIQUE KEY uk_anomaly_payment(payment_attempt_id)
) ENGINE=InnoDB;
-- Durable simulated external provider, independent of local business transaction commits.
CREATE TABLE mock_gateway_trades (
    request_no VARCHAR(64) NOT NULL PRIMARY KEY, order_no VARCHAR(32) NOT NULL,
    amount_cents INT NOT NULL, status VARCHAR(16) NOT NULL, external_no VARCHAR(64) NULL,
    occurred_at DATETIME(3) NULL
) ENGINE=InnoDB;
CREATE TABLE mock_gateway_refunds LIKE mock_gateway_trades;
CREATE TABLE job_failures (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, job_name VARCHAR(64) NOT NULL,
    target_id BIGINT NULL, error_code VARCHAR(128) NOT NULL, created_at DATETIME(3) NOT NULL
) ENGINE=InnoDB;
