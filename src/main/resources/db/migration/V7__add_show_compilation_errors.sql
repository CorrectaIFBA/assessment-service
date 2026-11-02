-- Whether the code editor underlines compilation errors while the student writes.
-- Existing assessments keep the behaviour they had: errors shown.

ALTER TABLE assessment_parameters
    ADD COLUMN IF NOT EXISTS show_compilation_errors BOOLEAN NOT NULL DEFAULT TRUE;
