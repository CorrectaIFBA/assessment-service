-- Sessions record when they started and when they stop accepting submissions,
-- the tab that owns them, how long the coding page stayed open, and the
-- parameters that drive them, frozen at start.

ALTER TABLE assessment_sessions
    ADD COLUMN IF NOT EXISTS started_at                         TIMESTAMP(6) WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS expires_at                         TIMESTAMP(6) WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS client_instance_id                 VARCHAR(64),
    ADD COLUMN IF NOT EXISTS open_seconds                       INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS focused_seconds                    INTEGER,
    ADD COLUMN IF NOT EXISTS rule_needs_to_finish_in_one_sitting BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS rule_abandonment_tolerance_seconds INTEGER,
    ADD COLUMN IF NOT EXISTS rule_monitor_for_cheating          BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE assessment_sessions SET started_at = created_at WHERE started_at IS NULL;
ALTER TABLE assessment_sessions ALTER COLUMN started_at SET NOT NULL;

-- null: a one-sitting session is never abandoned automatically
ALTER TABLE assessment_parameters
    ADD COLUMN IF NOT EXISTS abandonment_tolerance_seconds INTEGER;

CREATE TABLE IF NOT EXISTS assessment_session_events (
    id               VARCHAR(255)                NOT NULL,
    session_id       VARCHAR(255)                NOT NULL,
    type             VARCHAR(64)                 NOT NULL,
    occurred_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    received_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    actor_profile_id VARCHAR(255),
    detail           TEXT,
    CONSTRAINT assessment_session_events_pkey PRIMARY KEY (id),
    CONSTRAINT fk_assessment_session_events_session FOREIGN KEY (session_id)
        REFERENCES assessment_sessions (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_assessment_session_events_session
    ON assessment_session_events (session_id, occurred_at);
