CREATE TABLE IF NOT EXISTS declined_registrations (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL PRIMARY KEY,
    registration_id BIGINT NOT NULL,
    reason VARCHAR(100) NOT NULL,
    CONSTRAINT fk_registrations FOREIGN KEY (registration_id) REFERENCES registrations(id)
);