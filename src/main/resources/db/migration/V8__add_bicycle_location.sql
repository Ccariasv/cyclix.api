-- V8__add_bicycle_location.sql
-- Añade soporte para rastreo GPS a la tabla bicicleta

ALTER TABLE bicicleta 
    ADD COLUMN latitud DECIMAL(10, 7) NULL,
    ADD COLUMN longitud DECIMAL(10, 7) NULL,
    ADD CONSTRAINT chk_bicicleta_latitud CHECK (latitud IS NULL OR (latitud >= -90 AND latitud <= 90)),
    ADD CONSTRAINT chk_bicicleta_longitud CHECK (longitud IS NULL OR (longitud >= -180 AND longitud <= 180));
