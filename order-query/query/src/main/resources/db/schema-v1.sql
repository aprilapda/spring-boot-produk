-- V1: independent Order Query projection and inbox tables.
-- Idempotent bootstrap only: never drop or replace existing persistent data.
CREATE TABLE IF NOT EXISTS order_projection (
    order_id BIGINT PRIMARY KEY,
    aggregate_version BIGINT NOT NULL CHECK (aggregate_version > 0),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    owner_subject VARCHAR(255),
    response_json CLOB,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_order_projection_payload_state CHECK (
        (is_deleted = TRUE AND response_json IS NULL AND owner_subject IS NULL)
        OR
        (is_deleted = FALSE AND response_json IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_order_projection_owner_subject
    ON order_projection (owner_subject);

CREATE TABLE IF NOT EXISTS processed_order_event (
    event_id VARCHAR(36) PRIMARY KEY,
    aggregate_id BIGINT NOT NULL,
    aggregate_version BIGINT NOT NULL CHECK (aggregate_version > 0),
    event_type VARCHAR(32) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_processed_order_event_aggregate_version
        UNIQUE (aggregate_id, aggregate_version)
);
