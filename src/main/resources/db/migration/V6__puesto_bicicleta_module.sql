CREATE TABLE puesto (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    codigo VARCHAR(50) NOT NULL,
    direccion VARCHAR(255) NOT NULL,
    latitud DECIMAL(10, 7) NOT NULL,
    longitud DECIMAL(10, 7) NOT NULL,
    capacidad_total INT NOT NULL DEFAULT 10,
    capacidad_disponible INT NOT NULL DEFAULT 10,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_puesto_codigo UNIQUE (codigo),

    CONSTRAINT chk_puesto_latitud
        CHECK (latitud >= -90 AND latitud <= 90),

    CONSTRAINT chk_puesto_longitud
        CHECK (longitud >= -180 AND longitud <= 180),

    CONSTRAINT chk_puesto_capacidad_total
        CHECK (capacidad_total >= 1 AND capacidad_total <= 100),

    CONSTRAINT chk_puesto_capacidad_disponible
        CHECK (capacidad_disponible >= 0 AND capacidad_disponible <= capacidad_total),

    CONSTRAINT chk_puesto_estado
        CHECK (estado IN ('ACTIVO', 'INACTIVO', 'MANTENIMIENTO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE bicicleta (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL,
    marca VARCHAR(100) NOT NULL,
    modelo VARCHAR(100) NOT NULL,
    color VARCHAR(50) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    tamano_llanta DOUBLE NOT NULL,
    precio_por_hora DECIMAL(10, 2) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'DISPONIBLE',
    codigo_qr VARCHAR(255) NULL,
    puesto_id BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_bicicleta_codigo UNIQUE (codigo),
    CONSTRAINT uq_bicicleta_codigo_qr UNIQUE (codigo_qr),

    CONSTRAINT fk_bicicleta_puesto
        FOREIGN KEY (puesto_id) REFERENCES puesto(id),

    CONSTRAINT chk_bicicleta_tipo
        CHECK (tipo IN ('URBANA', 'MONTAÑA', 'ELECTRICA')),

    CONSTRAINT chk_bicicleta_estado
        CHECK (estado IN ('DISPONIBLE', 'EN_USO', 'MANTENIMIENTO', 'FUERA_DE_SERVICIO', 'RESERVADA')),

    CONSTRAINT chk_bicicleta_tamano_llanta
        CHECK (tamano_llanta >= 10.0 AND tamano_llanta <= 36.0),

    CONSTRAINT chk_bicicleta_precio_por_hora
        CHECK (precio_por_hora > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_puesto_estado_capacidad ON puesto (estado, capacidad_disponible);
CREATE INDEX idx_bicicleta_estado ON bicicleta (estado);
CREATE INDEX idx_bicicleta_tipo ON bicicleta (tipo);
CREATE INDEX idx_bicicleta_puesto_id ON bicicleta (puesto_id);
