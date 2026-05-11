-- V1__init_events.sql
-- Schema inicial del event-service
-- Historias: US-01, US-02, US-03, US-11, US-12

CREATE TABLE events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    venue VARCHAR(255) NOT NULL,  -- (se modifica definitivamente a venue)
    event_date TIMESTAMP NOT NULL,
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    available_tickets INTEGER NOT NULL CHECK (available_tickets >= 0),
    price NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    status VARCHAR(50) NOT NULL, -- (se elimina el default ACTIVE para evitar redundancia)
    organizer_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Índices para filtrado y consulta (US-02, US-03, RQ-13)
CREATE INDEX idx_events_status       ON events(status);
CREATE INDEX idx_events_category     ON events(category);
CREATE INDEX idx_events_event_date   ON events(event_date);
CREATE INDEX idx_events_organizer_id ON events(organizer_id);
CREATE INDEX idx_events_active_available
    ON events(status, available_tickets);