-- CityGo V1 baseline schema
-- Money is stored as integer cents (INT). Dates/times are UTC (DATETIME(3));
-- statistics convert to Asia/Shanghai at query time.

CREATE TABLE users
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(64)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    nickname      VARCHAR(64)  NULL,
    phone         VARCHAR(20)  NULL,
    role          VARCHAR(16)  NOT NULL DEFAULT 'USER', -- USER | OPERATOR | ADMIN
    enabled       TINYINT(1)   NOT NULL DEFAULT 1,
    created_at    DATETIME(3)  NOT NULL,
    updated_at    DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE categories
(
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    category_type VARCHAR(16) NOT NULL, -- ACTIVITY | PACKAGE
    name          VARCHAR(64) NOT NULL,
    sort_order    INT         NOT NULL DEFAULT 0,
    status        TINYINT(1)  NOT NULL DEFAULT 1,
    created_at    DATETIME(3) NOT NULL,
    updated_at    DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_categories_type_status (category_type, status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE file_objects
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    owner_id      BIGINT       NULL,
    owner_type    VARCHAR(16)  NULL, -- STAFF
    storage_key   VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    status        VARCHAR(16)  NOT NULL DEFAULT 'TEMP', -- TEMP | USED
    created_at    DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_file_objects_storage_key (storage_key),
    KEY idx_file_objects_owner (owner_id, owner_type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE activities
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    category_id BIGINT       NOT NULL,
    name        VARCHAR(128) NOT NULL,
    description TEXT         NULL,
    address     VARCHAR(255) NOT NULL,
    status      VARCHAR(16)  NOT NULL DEFAULT 'ON_SALE', -- ON_SALE | OFF_SALE
    created_by  BIGINT       NULL,
    created_at  DATETIME(3)  NOT NULL,
    updated_at  DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_activities_category (category_id),
    KEY idx_activities_status (status),
    CONSTRAINT fk_activities_category FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE activity_images
(
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    activity_id    BIGINT      NOT NULL,
    file_object_id BIGINT      NOT NULL,
    sort_order     INT         NOT NULL DEFAULT 0,
    created_at     DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_activity_images_activity (activity_id),
    CONSTRAINT fk_activity_images_activity FOREIGN KEY (activity_id) REFERENCES activities (id),
    CONSTRAINT fk_activity_images_file FOREIGN KEY (file_object_id) REFERENCES file_objects (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE sessions
(
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    activity_id      BIGINT      NOT NULL,
    start_time       DATETIME(3) NOT NULL,
    end_time         DATETIME(3) NOT NULL,
    booking_deadline DATETIME(3) NOT NULL,
    price_cents      INT         NOT NULL,
    capacity         INT         NOT NULL,
    available        INT         NOT NULL,
    status           VARCHAR(16) NOT NULL DEFAULT 'ON_SALE', -- ON_SALE | OFF_SALE
    created_at       DATETIME(3) NOT NULL,
    updated_at       DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_sessions_activity (activity_id),
    KEY idx_sessions_start_time (start_time),
    CONSTRAINT fk_sessions_activity FOREIGN KEY (activity_id) REFERENCES activities (id),
    CONSTRAINT chk_sessions_price CHECK (price_cents >= 0),
    CONSTRAINT chk_sessions_capacity CHECK (capacity >= 0),
    CONSTRAINT chk_sessions_available CHECK (available >= 0 AND available <= capacity),
    CONSTRAINT chk_sessions_time CHECK (start_time < end_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE packages
(
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    name               VARCHAR(128) NOT NULL,
    description        TEXT         NULL,
    package_price_cents INT          NOT NULL,
    version            INT          NOT NULL DEFAULT 1,
    status             VARCHAR(16)  NOT NULL DEFAULT 'ON_SALE', -- ON_SALE | OFF_SALE
    created_by         BIGINT       NULL,
    created_at         DATETIME(3)  NOT NULL,
    updated_at         DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_packages_status (status),
    CONSTRAINT chk_packages_price CHECK (package_price_cents >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE package_items
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    package_id BIGINT      NOT NULL,
    session_id BIGINT      NOT NULL,
    sort_order INT         NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_package_items_package_session (package_id, session_id),
    KEY idx_package_items_session (session_id),
    CONSTRAINT fk_package_items_package FOREIGN KEY (package_id) REFERENCES packages (id),
    CONSTRAINT fk_package_items_session FOREIGN KEY (session_id) REFERENCES sessions (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE contacts
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    name       VARCHAR(64) NOT NULL,
    phone      VARCHAR(20) NOT NULL,
    is_default TINYINT(1)  NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_contacts_user (user_id),
    CONSTRAINT fk_contacts_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE cart_items
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    item_type  VARCHAR(16) NOT NULL, -- SESSION | PACKAGE
    item_id    BIGINT      NOT NULL,
    quantity   TINYINT     NOT NULL DEFAULT 1,
    selected   TINYINT(1)  NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_items_user_item (user_id, item_type, item_id),
    CONSTRAINT fk_cart_items_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_cart_items_quantity CHECK (quantity BETWEEN 1 AND 5)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE orders
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    order_no         VARCHAR(32)  NOT NULL,
    user_id          BIGINT       NOT NULL,
    total_cents      INT          NOT NULL,
    status           VARCHAR(32)  NOT NULL DEFAULT 'PENDING_PAYMENT',
    -- PENDING_PAYMENT | PENDING_CONFIRMATION | CONFIRMED | COMPLETED
    -- | CANCELLED | CLOSED | REFUNDING | REFUNDED
    idempotency_key  VARCHAR(64)  NULL,
    request_digest   VARCHAR(64)  NULL,
    contact_name     VARCHAR(64)  NOT NULL,
    contact_phone    VARCHAR(20)  NOT NULL,
    remark           VARCHAR(255) NULL,
    expire_at        DATETIME(3)  NULL,
    paid_at          DATETIME(3)  NULL,
    confirmed_at     DATETIME(3)  NULL,
    completed_at     DATETIME(3)  NULL,
    cancelled_at     DATETIME(3)  NULL,
    closed_at        DATETIME(3)  NULL,
    refunded_at      DATETIME(3)  NULL,
    created_at       DATETIME(3)  NOT NULL,
    updated_at       DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_order_no (order_no),
    UNIQUE KEY uk_orders_user_idem (user_id, idempotency_key),
    KEY idx_orders_user (user_id),
    KEY idx_orders_status (status),
    KEY idx_orders_expire (expire_at),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_orders_total CHECK (total_cents >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE order_items
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    order_id          BIGINT       NOT NULL,
    item_type         VARCHAR(16)  NOT NULL, -- SESSION | PACKAGE
    item_id           BIGINT       NOT NULL,
    name              VARCHAR(128) NOT NULL,
    session_id        BIGINT       NULL,     -- for SESSION lines and snapshot reference
    session_snapshot  JSON         NULL,     -- start/end/deadline/price/activity snapshot
    package_snapshot  JSON         NULL,     -- package composition snapshot
    quantity          TINYINT      NOT NULL,
    unit_price_cents  INT          NOT NULL,
    line_total_cents  INT          NOT NULL,
    created_at        DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_order_items_order (order_id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT chk_order_items_quantity CHECK (quantity >= 1)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE order_allocations
(
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    order_id    BIGINT      NOT NULL,
    session_id  BIGINT      NOT NULL,
    quantity    INT         NOT NULL,
    released    TINYINT(1)  NOT NULL DEFAULT 0,
    released_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_allocations_order_session (order_id, session_id),
    KEY idx_allocations_session (session_id),
    CONSTRAINT fk_allocations_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_allocations_session FOREIGN KEY (session_id) REFERENCES sessions (id),
    CONSTRAINT chk_allocations_quantity CHECK (quantity >= 1)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE payment_attempts
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    request_no        VARCHAR(64)  NOT NULL,
    order_id          BIGINT       NOT NULL,
    channel           VARCHAR(16)  NOT NULL, -- MOCK | ALIPAY_SANDBOX
    amount_cents      INT          NOT NULL,
    status            VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    -- PENDING | SUCCESS | FAILED | CLOSED | UNKNOWN
    external_trade_no VARCHAR(64)  NULL,
    raw_notification  JSON         NULL,
    paid_at           DATETIME(3)  NULL,
    created_at        DATETIME(3)  NOT NULL,
    updated_at        DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_attempts_request_no (request_no),
    KEY idx_payment_attempts_order (order_id),
    CONSTRAINT fk_payment_attempts_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT chk_payment_attempts_amount CHECK (amount_cents >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE refunds
(
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    refund_no          VARCHAR(64)  NOT NULL,
    payment_attempt_id BIGINT       NOT NULL,
    order_id           BIGINT       NOT NULL,
    amount_cents       INT          NOT NULL,
    status             VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    -- PENDING | PROCESSING | SUCCESS | FAILED
    reason             VARCHAR(255) NULL,
    requested_by       BIGINT       NULL,
    fail_reason        VARCHAR(255) NULL,
    success_at         DATETIME(3)  NULL,
    created_at         DATETIME(3)  NOT NULL,
    updated_at         DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refunds_refund_no (refund_no),
    KEY idx_refunds_order (order_id),
    CONSTRAINT fk_refunds_payment_attempt FOREIGN KEY (payment_attempt_id) REFERENCES payment_attempts (id),
    CONSTRAINT fk_refunds_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT chk_refunds_amount CHECK (amount_cents >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE order_events
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    order_id    BIGINT       NOT NULL,
    from_status VARCHAR(32)  NOT NULL,
    to_status   VARCHAR(32)  NOT NULL,
    actor_type  VARCHAR(16)  NOT NULL, -- SYSTEM | USER | STAFF
    actor_id    BIGINT       NULL,
    reason      VARCHAR(255) NULL,
    created_at  DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_order_events_order (order_id, created_at),
    CONSTRAINT fk_order_events_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE notifications
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    event_type VARCHAR(32)  NOT NULL,
    title      VARCHAR(128) NOT NULL,
    content    VARCHAR(512) NOT NULL,
    biz_key    VARCHAR(96)  NOT NULL,
    created_at DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notifications_biz_key (biz_key)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE notification_recipients
(
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    notification_id BIGINT      NOT NULL,
    recipient_type  VARCHAR(16) NOT NULL, -- USER | STAFF
    recipient_id    BIGINT      NOT NULL,
    is_read         TINYINT(1)  NOT NULL DEFAULT 0,
    read_at         DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_nf_recipients (notification_id, recipient_type, recipient_id),
    KEY idx_nf_recipients_recipient (recipient_type, recipient_id, is_read),
    CONSTRAINT fk_nf_recipients_notification FOREIGN KEY (notification_id) REFERENCES notifications (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE outbox_events
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    event_type    VARCHAR(64)  NOT NULL,
    payload       JSON         NOT NULL,
    biz_key       VARCHAR(96)  NOT NULL,
    status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING', -- PENDING | PROCESSING | DONE | FAILED
    retry_count   INT          NOT NULL DEFAULT 0,
    next_retry_at DATETIME(3)  NULL,
    last_error    VARCHAR(255) NULL,
    created_at    DATETIME(3)  NOT NULL,
    updated_at    DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_outbox_biz_key (biz_key),
    KEY idx_outbox_status_retry (status, next_retry_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE platform_settings
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    setting_key   VARCHAR(64)  NOT NULL,
    setting_value VARCHAR(255) NOT NULL,
    description   VARCHAR(255) NULL,
    updated_by    BIGINT       NULL,
    updated_at    DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_platform_settings_key (setting_key)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE audit_logs
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    operator_id BIGINT       NOT NULL,
    action      VARCHAR(64)  NOT NULL,
    target_type VARCHAR(64)  NOT NULL,
    target_id   BIGINT       NULL,
    summary     VARCHAR(512) NULL,
    success     TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_audit_logs_created (created_at),
    KEY idx_audit_logs_operator (operator_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO platform_settings (setting_key, setting_value, description, updated_at)
VALUES ('accepting_orders', 'true', 'Platform accepting new orders switch', NOW(3));
