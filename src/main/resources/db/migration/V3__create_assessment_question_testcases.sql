-- Test cases a question's answers are run against.
--
-- Deleting a question removes its test cases with it, so the question service
-- does not have to delete them one by one first.

CREATE TABLE IF NOT EXISTS assessment_question_testcases (
    id                     VARCHAR(255)                NOT NULL,
    assessment_question_id VARCHAR(255)                NOT NULL,
    "position"             INTEGER                     NOT NULL,
    input                  TEXT                        NOT NULL,
    expected_output        TEXT                        NOT NULL,
    time_limit_seconds     INTEGER                     NOT NULL,
    memory_limit_mb        INTEGER                     NOT NULL,
    type                   VARCHAR(255)                NOT NULL,
    created_at             TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at             TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT assessment_question_testcases_pkey PRIMARY KEY (id),
    CONSTRAINT uk_assessment_question_testcase_question_position UNIQUE (assessment_question_id, "position"),
    CONSTRAINT assessment_question_testcases_type_check CHECK (type IN ('PUBLIC', 'PRIVATE', 'EVALUATION')),
    CONSTRAINT fk_assessment_question_testcases_question FOREIGN KEY (assessment_question_id)
        REFERENCES assessment_questions (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_assessment_question_testcase_question_position
    ON assessment_question_testcases (assessment_question_id, "position");
