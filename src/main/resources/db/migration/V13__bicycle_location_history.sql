CREATE TABLE bicycle_location_history (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    bike_id BIGINT UNSIGNED NOT NULL,
    trip_id BIGINT UNSIGNED NULL,
    station_id BIGINT UNSIGNED NULL,
    event_type VARCHAR(40) NOT NULL,
    source VARCHAR(40) NOT NULL,
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    recorded_at TIMESTAMP NOT NULL,
    received_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_bicycle_location_history_bike
        FOREIGN KEY (bike_id) REFERENCES bicicleta(id),

    CONSTRAINT fk_bicycle_location_history_trip
        FOREIGN KEY (trip_id) REFERENCES trips(id),

    CONSTRAINT fk_bicycle_location_history_station
        FOREIGN KEY (station_id) REFERENCES puesto(id),

    CONSTRAINT chk_bicycle_location_history_latitude
        CHECK (latitude >= -90 AND latitude <= 90),

    CONSTRAINT chk_bicycle_location_history_longitude
        CHECK (longitude >= -180 AND longitude <= 180)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_bicycle_location_history_bike_recorded_at
    ON bicycle_location_history (bike_id, recorded_at);

CREATE INDEX idx_bicycle_location_history_trip_recorded_at
    ON bicycle_location_history (trip_id, recorded_at);

CREATE INDEX idx_bicycle_location_history_station_recorded_at
    ON bicycle_location_history (station_id, recorded_at);
