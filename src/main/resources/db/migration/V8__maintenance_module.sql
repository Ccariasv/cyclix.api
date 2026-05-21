INSERT INTO roles (name, description)
SELECT 'MAINTENANCE', 'Personal operativo de mantenimiento'
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE name = 'MAINTENANCE'
);

CREATE TABLE maintenance_orders (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT UNSIGNED NULL,
    bike_id BIGINT UNSIGNED NOT NULL,
    assigned_to_user_id BIGINT UNSIGNED NULL,
    created_by_user_id BIGINT UNSIGNED NOT NULL,
    priority VARCHAR(30) NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    result_status VARCHAR(50) NULL,
    reported_issue TEXT NOT NULL,
    diagnosis TEXT NULL,
    resolution_notes TEXT NULL,
    current_location VARCHAR(180) NULL,
    estimated_minutes INT NULL,
    out_of_service_reason TEXT NULL,
    assigned_at TIMESTAMP NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_maintenance_orders_ticket
        FOREIGN KEY (ticket_id) REFERENCES support_tickets(id),

    CONSTRAINT fk_maintenance_orders_bike
        FOREIGN KEY (bike_id) REFERENCES bicicleta(id),

    CONSTRAINT fk_maintenance_orders_assigned_user
        FOREIGN KEY (assigned_to_user_id) REFERENCES user(id),

    CONSTRAINT fk_maintenance_orders_created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES user(id),

    CONSTRAINT uq_maintenance_orders_ticket UNIQUE (ticket_id),

    CONSTRAINT chk_maintenance_orders_priority
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),

    CONSTRAINT chk_maintenance_orders_type
        CHECK (type IN ('CORRECTIVE', 'PREVENTIVE', 'INSPECTION', 'BRAKES', 'TIRES', 'CHAIN', 'ELECTRICAL', 'BATTERY', 'FRAME', 'GENERAL')),

    CONSTRAINT chk_maintenance_orders_status
        CHECK (status IN ('PENDING', 'ASSIGNED', 'IN_REVIEW', 'IN_REPAIR', 'WAITING_PARTS', 'PAUSED', 'FINALIZED')),

    CONSTRAINT chk_maintenance_orders_result_status
        CHECK (result_status IS NULL OR result_status IN ('STAYS_IN_MAINTENANCE', 'AVAILABLE', 'OUT_OF_SERVICE')),

    CONSTRAINT chk_maintenance_orders_estimated_minutes
        CHECK (estimated_minutes IS NULL OR estimated_minutes > 0),

    CONSTRAINT chk_maintenance_orders_reported_issue
        CHECK (CHAR_LENGTH(TRIM(reported_issue)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE maintenance_order_history (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    maintenance_order_id BIGINT UNSIGNED NOT NULL,
    changed_by_user_id BIGINT UNSIGNED NOT NULL,
    action VARCHAR(50) NOT NULL,
    previous_status VARCHAR(50) NULL,
    new_status VARCHAR(50) NULL,
    note TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_maintenance_history_order
        FOREIGN KEY (maintenance_order_id) REFERENCES maintenance_orders(id),

    CONSTRAINT fk_maintenance_history_user
        FOREIGN KEY (changed_by_user_id) REFERENCES user(id),

    CONSTRAINT chk_maintenance_history_action
        CHECK (action IN ('CREATED', 'ASSIGNED', 'PROGRESS_UPDATED', 'RESOLVED')),

    CONSTRAINT chk_maintenance_history_previous_status
        CHECK (previous_status IS NULL OR previous_status IN ('PENDING', 'ASSIGNED', 'IN_REVIEW', 'IN_REPAIR', 'WAITING_PARTS', 'PAUSED', 'FINALIZED')),

    CONSTRAINT chk_maintenance_history_new_status
        CHECK (new_status IS NULL OR new_status IN ('PENDING', 'ASSIGNED', 'IN_REVIEW', 'IN_REPAIR', 'WAITING_PARTS', 'PAUSED', 'FINALIZED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_maintenance_orders_bike_status ON maintenance_orders (bike_id, status);
CREATE INDEX idx_maintenance_orders_assigned_status ON maintenance_orders (assigned_to_user_id, status);
CREATE INDEX idx_maintenance_orders_created_at ON maintenance_orders (created_at);
CREATE INDEX idx_maintenance_history_order_created_at ON maintenance_order_history (maintenance_order_id, created_at);
