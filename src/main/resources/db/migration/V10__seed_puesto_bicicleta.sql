-- V9__seed_puesto_bicicleta.sql
-- Poblar datos de prueba para puestos y bicicletas en la zona de Zacapa

-- Insertar puestos de prueba
INSERT IGNORE INTO puesto (id, nombre, codigo, direccion, latitud, longitud, capacidad_total, capacidad_disponible, estado) VALUES
(1, 'Puesto Parque Central Zacapa', 'PST-ZAC-001', 'Parque Central de Zacapa, Barrio El Centro', 14.9722, -89.5305, 10, 8, 'ACTIVO'),
(2, 'Puesto CC Pradera Zacapa', 'PST-ZAC-002', 'Centro Comercial Pradera, Calzada Alvaro Arzú', 14.9654, -89.5398, 10, 9, 'ACTIVO');

-- Insertar bicicletas de prueba asociadas a los puestos
-- Dejamos latitud y longitud como NULL para que el sistema use por defecto el fallback simulado de Zacapa
INSERT IGNORE INTO bicicleta (id, codigo, marca, modelo, color, tipo, tamano_llanta, precio_por_hora, estado, codigo_qr, puesto_id, latitud, longitud) VALUES
(1, 'BIC-ZAC-001', 'Giant', 'Escape 3', 'Negro', 'URBANA', 28.0, 15.00, 'DISPONIBLE', 'CYCLIX-BICI-BIC-ZAC-001', 1, NULL, NULL),
(2, 'BIC-ZAC-002', 'Trek', 'Marlin 5', 'Azul', 'MONTAÑA', 29.0, 20.00, 'DISPONIBLE', 'CYCLIX-BICI-BIC-ZAC-002', 1, NULL, NULL),
(3, 'BIC-ZAC-003', 'Specialized', 'Turbo Vado', 'Rojo', 'ELECTRICA', 27.5, 30.00, 'DISPONIBLE', 'CYCLIX-BICI-BIC-ZAC-003', 2, NULL, NULL);
