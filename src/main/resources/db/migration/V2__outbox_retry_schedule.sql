ALTER TABLE outbox_events
    ADD COLUMN next_attempt_at TIMESTAMPTZ;

UPDATE outbox_events
SET next_attempt_at = occurred_at
WHERE next_attempt_at IS NULL;

ALTER TABLE outbox_events
    ALTER COLUMN next_attempt_at SET NOT NULL;

CREATE INDEX idx_outbox_events_ready
    ON outbox_events (next_attempt_at, occurred_at, id)
    WHERE processed_at IS NULL;
