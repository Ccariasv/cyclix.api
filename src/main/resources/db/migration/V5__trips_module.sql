CREATE TABLE trip_statuses (
                               name VARCHAR(50) PRIMARY KEY,
                               description VARCHAR(255) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO trip_statuses (name, description) VALUES
                                                  ('ACTIVE', 'Viaje activo'),
                                                  ('COMPLETED', 'Viaje finalizado correctamente'),
                                                  ('CANCELLED', 'Viaje cancelado');

CREATE TABLE trips (
                       id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                       user_id BIGINT UNSIGNED NOT NULL,
                       bike_id BIGINT UNSIGNED NOT NULL,
                       status VARCHAR(50) NOT NULL,
                       start_latitude DECIMAL(10, 7) NOT NULL,
                       start_longitude DECIMAL(10, 7) NOT NULL,
                       end_latitude DECIMAL(10, 7) NULL,
                       end_longitude DECIMAL(10, 7) NULL,
                       started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       ended_at TIMESTAMP NULL,
                       distance_km DECIMAL(10, 2) NULL,
                       duration_seconds BIGINT NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                       CONSTRAINT fk_trips_user
                           FOREIGN KEY (user_id) REFERENCES user(id),

                       CONSTRAINT fk_trips_status
                           FOREIGN KEY (status) REFERENCES trip_statuses(name),

                       CONSTRAINT chk_trips_start_latitude
                           CHECK (start_latitude >= -90 AND start_latitude <= 90),

                       CONSTRAINT chk_trips_start_longitude
                           CHECK (start_longitude >= -180 AND start_longitude <= 180),

                       CONSTRAINT chk_trips_end_latitude
                           CHECK (end_latitude IS NULL OR (end_latitude >= -90 AND end_latitude <= 90)),

                       CONSTRAINT chk_trips_end_longitude
                           CHECK (end_longitude IS NULL OR (end_longitude >= -180 AND end_longitude <= 180)),

                       CONSTRAINT chk_trips_distance_not_negative
                           CHECK (distance_km IS NULL OR distance_km >= 0),

                       CONSTRAINT chk_trips_duration_not_negative
                           CHECK (duration_seconds IS NULL OR duration_seconds >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_trips_user_id ON trips (user_id);
CREATE INDEX idx_trips_bike_id ON trips (bike_id);
CREATE INDEX idx_trips_status_started_at ON trips (status, started_at);
CREATE INDEX idx_trips_user_status ON trips (user_id, status);