-- V12__seed_demo_users_bicycles_trips.sql
-- Datos demo orientados a Zacapa, Zacapa, Guatemala

INSERT IGNORE INTO roles (name, description) VALUES
    ('USER', 'Usuario final de la plataforma'),
    ('ADMIN', 'Administrador del sistema'),
    ('MAINTENANCE', 'Personal operativo de mantenimiento');

INSERT IGNORE INTO user_statuses (name, description) VALUES
    ('ACTIVE', 'Usuario activo'),
    ('INACTIVE', 'Usuario inactivo');

-- Password plano para todos los usuarios demo: Test1234*
INSERT IGNORE INTO user (
    first_name,
    last_name,
    email,
    phone,
    password_hash,
    role_id,
    status_id,
    email_verified,
    last_login_at
)
VALUES
(
    'Diego',
    'Admin',
    'admin@cyclix.test',
    '88880001',
    '$2y$10$vk/hfdcsiMo.w8O8Op6anOrbUfG/hFclbM2MJ7gyTg8jDmbw3tlYm',
    (SELECT id FROM roles WHERE name = 'ADMIN'),
    (SELECT id FROM user_statuses WHERE name = 'ACTIVE'),
    TRUE,
    NOW()
),
(
    'Laura',
    'Mora',
    'laura@cyclix.test',
    '88880002',
    '$2y$10$vk/hfdcsiMo.w8O8Op6anOrbUfG/hFclbM2MJ7gyTg8jDmbw3tlYm',
    (SELECT id FROM roles WHERE name = 'USER'),
    (SELECT id FROM user_statuses WHERE name = 'ACTIVE'),
    TRUE,
    NOW()
),
(
    'Demo',
    'Usuario',
    'demo.user@cyclix.test',
    '88880005',
    '$2y$10$vk/hfdcsiMo.w8O8Op6anOrbUfG/hFclbM2MJ7gyTg8jDmbw3tlYm',
    (SELECT id FROM roles WHERE name = 'USER'),
    (SELECT id FROM user_statuses WHERE name = 'ACTIVE'),
    TRUE,
    NOW()
),
(
    'Mario',
    'Tecnico',
    'maintenance@cyclix.test',
    '88880004',
    '$2y$10$vk/hfdcsiMo.w8O8Op6anOrbUfG/hFclbM2MJ7gyTg8jDmbw3tlYm',
    (SELECT id FROM roles WHERE name = 'MAINTENANCE'),
    (SELECT id FROM user_statuses WHERE name = 'ACTIVE'),
    TRUE,
    NOW()
);

INSERT IGNORE INTO puesto (id, nombre, codigo, direccion, latitud, longitud, capacidad_total, capacidad_disponible, estado) VALUES
(1, 'Puesto Parque Central Zacapa', 'PST-ZAC-001', 'Parque Central, Barrio El Centro, Zacapa, Zacapa, Guatemala', 14.9722, -89.5305, 10, 8, 'ACTIVO'),
(2, 'Puesto CC Pradera Zacapa', 'PST-ZAC-002', 'Centro Comercial Pradera, Calzada Álvaro Arzú, Zacapa, Zacapa, Guatemala', 14.9654, -89.5398, 10, 9, 'ACTIVO'),
(3, 'Universidad Mariano Gálvez Zacapa', 'PST-ZAC-003', '4ta. Calle 2-31, Zona 2, Zacapa, Zacapa, Guatemala', 14.9705, -89.5280, 10, 10, 'ACTIVO'),
(4, 'Estadio Municipal de Zacapa', 'PST-ZAC-004', 'Estadio Municipal David Ordóñez Bardales, Zacapa, Zacapa, Guatemala', 14.9669, -89.5269, 10, 10, 'ACTIVO');

UPDATE puesto
SET
    nombre = 'Puesto Parque Central Zacapa',
    direccion = 'Parque Central, Barrio El Centro, Zacapa, Zacapa, Guatemala',
    latitud = 14.9722,
    longitud = -89.5305,
    updated_at = CURRENT_TIMESTAMP
WHERE codigo = 'PST-ZAC-001';

UPDATE puesto
SET
    nombre = 'Puesto CC Pradera Zacapa',
    direccion = 'Centro Comercial Pradera, Calzada Álvaro Arzú, Zacapa, Zacapa, Guatemala',
    latitud = 14.9654,
    longitud = -89.5398,
    updated_at = CURRENT_TIMESTAMP
WHERE codigo = 'PST-ZAC-002';

UPDATE puesto
SET
    nombre = 'Universidad Mariano Gálvez Zacapa',
    direccion = '4ta. Calle 2-31, Zona 2, Zacapa, Zacapa, Guatemala',
    latitud = 14.9705,
    longitud = -89.5280,
    updated_at = CURRENT_TIMESTAMP
WHERE codigo = 'PST-ZAC-003';

UPDATE puesto
SET
    nombre = 'Estadio Municipal de Zacapa',
    direccion = 'Estadio Municipal David Ordóñez Bardales, Zacapa, Zacapa, Guatemala',
    latitud = 14.9669,
    longitud = -89.5269,
    updated_at = CURRENT_TIMESTAMP
WHERE codigo = 'PST-ZAC-004';

INSERT IGNORE INTO bicicleta (id, codigo, marca, modelo, color, tipo, tamano_llanta, precio_por_hora, estado, codigo_qr, puesto_id, latitud, longitud) VALUES
(1, 'BIC-ZAC-001', 'Giant', 'Escape 3', 'Negro', 'URBANA', 28.0, 15.00, 'DISPONIBLE', 'CYCLIX-BICI-BIC-ZAC-001', 1, NULL, NULL),
(2, 'BIC-ZAC-002', 'Trek', 'Marlin 5', 'Azul', 'MONTAÑA', 29.0, 20.00, 'DISPONIBLE', 'CYCLIX-BICI-BIC-ZAC-002', 1, NULL, NULL),
(3, 'BIC-ZAC-003', 'Specialized', 'Turbo Vado', 'Rojo', 'ELECTRICA', 27.5, 30.00, 'DISPONIBLE', 'CYCLIX-BICI-BIC-ZAC-003', 2, NULL, NULL),
(4, 'BIC-ZAC-004', 'Scott', 'Sub Cross 30', 'Verde', 'URBANA', 28.0, 15.00, 'DISPONIBLE', 'CYCLIX-BICI-BIC-ZAC-004', 1, 14.9720, -89.5300),
(5, 'BIC-ZAC-005', 'Cannondale', 'Trail 8', 'Gris', 'MONTAÑA', 29.0, 20.00, 'DISPONIBLE', 'CYCLIX-BICI-BIC-ZAC-005', 2, 14.9650, -89.5390),
(6, 'BIC-ZAC-006', 'Merida', 'Crossway 100', 'Blanco', 'URBANA', 28.0, 15.00, 'DISPONIBLE', 'CYCLIX-BICI-BIC-ZAC-006', 3, 14.9705, -89.5280),
(7, 'BIC-ZAC-007', 'GT', 'Aggressor Expert', 'Amarillo', 'MONTAÑA', 29.0, 20.00, 'DISPONIBLE', 'CYCLIX-BICI-BIC-ZAC-007', 4, 14.9669, -89.5269);

UPDATE bicicleta
SET
    codigo = 'BIC-ZAC-004',
    codigo_qr = 'CYCLIX-BICI-BIC-ZAC-004',
    puesto_id = 1,
    latitud = 14.9720,
    longitud = -89.5300,
    updated_at = CURRENT_TIMESTAMP
WHERE codigo IN ('BIC-DEMO-004', 'BIC-ZAC-004')
  AND id = 4;

UPDATE bicicleta
SET
    codigo = 'BIC-ZAC-005',
    codigo_qr = 'CYCLIX-BICI-BIC-ZAC-005',
    puesto_id = 2,
    latitud = 14.9650,
    longitud = -89.5390,
    updated_at = CURRENT_TIMESTAMP
WHERE codigo IN ('BIC-DEMO-005', 'BIC-ZAC-005')
  AND id = 5;

UPDATE bicicleta
SET
    codigo = 'BIC-ZAC-006',
    codigo_qr = 'CYCLIX-BICI-BIC-ZAC-006',
    puesto_id = 3,
    latitud = 14.9705,
    longitud = -89.5280,
    updated_at = CURRENT_TIMESTAMP
WHERE codigo = 'BIC-ZAC-006'
  AND id = 6;

UPDATE bicicleta
SET
    codigo = 'BIC-ZAC-007',
    codigo_qr = 'CYCLIX-BICI-BIC-ZAC-007',
    puesto_id = 4,
    latitud = 14.9669,
    longitud = -89.5269,
    updated_at = CURRENT_TIMESTAMP
WHERE codigo = 'BIC-ZAC-007'
  AND id = 7;

INSERT INTO wallets (user_id, balance, currency)
SELECT u.id, 500.00, 'GTQ'
FROM user u
WHERE u.email IN (
    'admin@cyclix.test',
    'laura@cyclix.test',
    'demo.user@cyclix.test',
    'maintenance@cyclix.test'
)
ON DUPLICATE KEY UPDATE
    balance = GREATEST(wallets.balance, 500.00),
    updated_at = CURRENT_TIMESTAMP;

INSERT IGNORE INTO trips (
    id,
    user_id,
    bike_id,
    status,
    start_latitude,
    start_longitude,
    end_latitude,
    end_longitude,
    started_at,
    ended_at,
    distance_km,
    duration_seconds,
    pricing_rule_id,
    pricing_rule_name,
    subscription_applied,
    subscription_minutes_covered,
    billable_minutes,
    base_fare_applied,
    included_minutes_applied,
    extra_fare_per_block_applied,
    extra_block_minutes_applied,
    extra_amount,
    total_amount,
    wallet_charged_amount
)
VALUES
(
    1001,
    (SELECT id FROM user WHERE email = 'laura@cyclix.test'),
    1,
    'COMPLETED',
    14.9722000,
    -89.5305000,
    14.9750000,
    -89.5280000,
    '2026-05-10 09:00:00',
    '2026-05-10 09:35:00',
    2.50,
    2100,
    1,
    'Tarifa estándar',
    FALSE,
    NULL,
    35,
    20.00,
    120,
    5.00,
    30,
    0.00,
    20.00,
    20.00
),
(
    1002,
    (SELECT id FROM user WHERE email = 'laura@cyclix.test'),
    2,
    'COMPLETED',
    14.9654000,
    -89.5398000,
    14.9680000,
    -89.5360000,
    '2026-05-15 14:00:00',
    '2026-05-15 14:50:00',
    3.20,
    3000,
    1,
    'Tarifa estándar',
    FALSE,
    NULL,
    50,
    20.00,
    120,
    5.00,
    30,
    0.00,
    20.00,
    20.00
),
(
    1003,
    (SELECT id FROM user WHERE email = 'demo.user@cyclix.test'),
    4,
    'COMPLETED',
    14.9720000,
    -89.5300000,
    14.9745000,
    -89.5275000,
    '2026-05-18 08:30:00',
    '2026-05-18 09:00:00',
    1.80,
    1800,
    1,
    'Tarifa estándar',
    FALSE,
    NULL,
    30,
    20.00,
    120,
    5.00,
    30,
    0.00,
    20.00,
    20.00
),
(
    1004,
    (SELECT id FROM user WHERE email = 'admin@cyclix.test'),
    3,
    'COMPLETED',
    14.9654000,
    -89.5398000,
    14.9670000,
    -89.5380000,
    '2026-05-19 16:00:00',
    '2026-05-19 16:25:00',
    1.20,
    1500,
    1,
    'Tarifa estándar',
    FALSE,
    NULL,
    25,
    20.00,
    120,
    5.00,
    30,
    0.00,
    20.00,
    20.00
),
(
    1005,
    (SELECT id FROM user WHERE email = 'demo.user@cyclix.test'),
    5,
    'ACTIVE',
    14.9650000,
    -89.5390000,
    NULL,
    NULL,
    '2026-05-22 10:00:00',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    FALSE,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL
),
(
    1006,
    (SELECT id FROM user WHERE email = 'laura@cyclix.test'),
    6,
    'COMPLETED',
    14.9705000,
    -89.5280000,
    14.9718000,
    -89.5268000,
    '2026-05-20 11:00:00',
    '2026-05-20 11:40:00',
    2.10,
    2400,
    1,
    'Tarifa estándar',
    FALSE,
    NULL,
    40,
    20.00,
    120,
    5.00,
    30,
    0.00,
    20.00,
    20.00
),
(
    1007,
    (SELECT id FROM user WHERE email = 'admin@cyclix.test'),
    7,
    'COMPLETED',
    14.9669000,
    -89.5269000,
    14.9682000,
    -89.5254000,
    '2026-05-21 17:30:00',
    '2026-05-21 18:05:00',
    1.90,
    2100,
    1,
    'Tarifa estándar',
    FALSE,
    NULL,
    35,
    20.00,
    120,
    5.00,
    30,
    0.00,
    20.00,
    20.00
);

UPDATE bicicleta
SET estado = 'EN_USO',
    updated_at = CURRENT_TIMESTAMP
WHERE codigo = 'BIC-ZAC-005'
  AND EXISTS (
      SELECT 1
      FROM trips
      WHERE id = 1005
        AND status = 'ACTIVE'
  );
