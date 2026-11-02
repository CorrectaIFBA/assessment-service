-- Assessments no longer monitor the page, bind a session to one tab or abandon sessions.
-- A session now ends only by submission, by its deadline, by faculty or by archiving.

-- an abandoned session ended early with what was submitted, the same as an expired one
UPDATE assessment_sessions SET state = 'EXPIRED' WHERE state = 'ABANDONED';
UPDATE assessment_session_events SET type = 'EXPIRED' WHERE type = 'ABANDONED';

-- page activity and tab ownership events have no meaning anymore
DELETE FROM assessment_session_events
WHERE type IN (
    'OTHER_INSTANCE_REJECTED', 'INSTANCE_TAKEOVER', 'HEARTBEAT_GAP',
    'TAB_HIDDEN', 'TAB_VISIBLE', 'FOCUS_LOST', 'FOCUS_RETURNED', 'FULLSCREEN_EXITED',
    'PASTE_BLOCKED', 'PASTE', 'QUESTION_OPENED', 'CODE_RUN', 'CODE_SUBMITTED'
);

ALTER TABLE assessment_sessions
    DROP COLUMN IF EXISTS client_instance_id,
    DROP COLUMN IF EXISTS focused_seconds,
    DROP COLUMN IF EXISTS rule_needs_to_finish_in_one_sitting,
    DROP COLUMN IF EXISTS rule_abandonment_tolerance_seconds,
    DROP COLUMN IF EXISTS rule_monitor_for_cheating;

ALTER TABLE assessment_parameters
    DROP COLUMN IF EXISTS needs_to_finish_in_one_sitting,
    DROP COLUMN IF EXISTS abandonment_tolerance_seconds,
    DROP COLUMN IF EXISTS monitor_for_cheating;
