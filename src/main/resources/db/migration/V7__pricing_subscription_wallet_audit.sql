CREATE TABLE pricing_rules (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    priority INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    base_fare DECIMAL(12,2) NOT NULL,
    included_minutes INT NOT NULL,
    extra_fare_per_block DECIMAL(12,2) NOT NULL,
    extra_block_minutes INT NOT NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    start_time TIME NULL,
    end_time TIME NULL,
    days_of_week VARCHAR(120) NULL,
    holiday_mode VARCHAR(20) NOT NULL DEFAULT 'ANY',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_pricing_rules_base_fare CHECK (base_fare >= 0),
    CONSTRAINT chk_pricing_rules_included_minutes CHECK (included_minutes > 0),
    CONSTRAINT chk_pricing_rules_extra_fare CHECK (extra_fare_per_block >= 0),
    CONSTRAINT chk_pricing_rules_extra_block_minutes CHECK (extra_block_minutes > 0),
    CONSTRAINT chk_pricing_rules_holiday_mode CHECK (holiday_mode IN ('ANY', 'HOLIDAY_ONLY', 'NON_HOLIDAY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE holidays (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    holiday_date DATE NOT NULL,
    name VARCHAR(150) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_holidays_date UNIQUE (holiday_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE subscription_plans (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    monthly_price DECIMAL(12,2) NOT NULL,
    included_hours INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_subscription_plan_name UNIQUE (name),
    CONSTRAINT chk_subscription_plan_price CHECK (monthly_price >= 0),
    CONSTRAINT chk_subscription_plan_included_hours CHECK (included_hours > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_subscriptions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    plan_id BIGINT UNSIGNED NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    starts_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    included_minutes INT NOT NULL,
    consumed_minutes INT NOT NULL DEFAULT 0,
    remaining_minutes INT NOT NULL,
    auto_renew BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_subscriptions_user FOREIGN KEY (user_id) REFERENCES user(id),
    CONSTRAINT fk_user_subscriptions_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans(id),
    CONSTRAINT chk_user_subscriptions_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT chk_user_subscriptions_included_minutes CHECK (included_minutes > 0),
    CONSTRAINT chk_user_subscriptions_consumed_minutes CHECK (consumed_minutes >= 0),
    CONSTRAINT chk_user_subscriptions_remaining_minutes CHECK (remaining_minutes >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE wallets (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'GTQ',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_wallet_user UNIQUE (user_id),
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES user(id),
    CONSTRAINT chk_wallet_balance_non_negative CHECK (balance >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE wallet_transactions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    wallet_id BIGINT UNSIGNED NOT NULL,
    type VARCHAR(30) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    balance_before DECIMAL(12,2) NOT NULL,
    balance_after DECIMAL(12,2) NOT NULL,
    description VARCHAR(255) NULL,
    reference_type VARCHAR(50) NULL,
    reference_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_wallet_transactions_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id),
    CONSTRAINT chk_wallet_transactions_type CHECK (type IN ('TOP_UP', 'TRIP_CHARGE', 'REFUND', 'ADJUSTMENT')),
    CONSTRAINT chk_wallet_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_wallet_transactions_balance_before CHECK (balance_before >= 0),
    CONSTRAINT chk_wallet_transactions_balance_after CHECK (balance_after >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE audit_log (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    user_id BIGINT UNSIGNED NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT NULL,
    details TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audit_log_user FOREIGN KEY (user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE trips
    ADD COLUMN pricing_rule_id BIGINT UNSIGNED NULL,
    ADD COLUMN pricing_rule_name VARCHAR(120) NULL,
    ADD COLUMN subscription_applied BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN subscription_minutes_covered INT NULL,
    ADD COLUMN billable_minutes INT NULL,
    ADD COLUMN base_fare_applied DECIMAL(12,2) NULL,
    ADD COLUMN included_minutes_applied INT NULL,
    ADD COLUMN extra_fare_per_block_applied DECIMAL(12,2) NULL,
    ADD COLUMN extra_block_minutes_applied INT NULL,
    ADD COLUMN extra_amount DECIMAL(12,2) NULL,
    ADD COLUMN total_amount DECIMAL(12,2) NULL,
    ADD COLUMN wallet_charged_amount DECIMAL(12,2) NULL;

ALTER TABLE trips
    ADD CONSTRAINT fk_trips_pricing_rule
        FOREIGN KEY (pricing_rule_id) REFERENCES pricing_rules(id);

CREATE INDEX idx_pricing_rules_active_priority ON pricing_rules (active, priority);
CREATE INDEX idx_pricing_rules_date ON pricing_rules (start_date, end_date);
CREATE INDEX idx_holidays_date_active ON holidays (holiday_date, active);
CREATE INDEX idx_user_subscriptions_user_status ON user_subscriptions (user_id, status);
CREATE INDEX idx_wallet_transactions_wallet_created ON wallet_transactions (wallet_id, created_at);
CREATE INDEX idx_audit_log_event_created ON audit_log (event_type, created_at);

INSERT INTO pricing_rules (
    name, priority, active, base_fare, included_minutes, extra_fare_per_block, extra_block_minutes, holiday_mode
) VALUES (
    'Tarifa estándar', 1, TRUE, 20.00, 120, 5.00, 30, 'ANY'
);

INSERT INTO subscription_plans (name, monthly_price, included_hours, active)
VALUES
    ('Plan Básico 20h', 100.00, 20, TRUE),
    ('Plan Plus 50h', 200.00, 50, TRUE),
    ('Plan Pro 100h', 350.00, 100, TRUE);
