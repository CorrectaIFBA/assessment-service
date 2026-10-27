-- Schema of every entity as of the move to Flyway.
--
-- Databases created before Flyway already hold these tables (Hibernate's
-- ddl-auto=update built them), so every statement is guarded with IF NOT EXISTS:
-- a fresh database gets the full schema, an existing one only what it lacks.
-- Flyway baselines existing databases at version 0, so this script runs on both.

CREATE TABLE IF NOT EXISTS assessments (
    id               VARCHAR(255)                NOT NULL,
    owner_profile_id VARCHAR(255)                NOT NULL,
    cohort_id        VARCHAR(255),
    title            VARCHAR(255)                NOT NULL,
    description      VARCHAR(1024),
    state            VARCHAR(255)                NOT NULL,
    due_date         TIMESTAMP(6) WITH TIME ZONE,
    created_at       TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT assessments_pkey PRIMARY KEY (id),
    CONSTRAINT assessments_state_check CHECK (state IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);

CREATE INDEX IF NOT EXISTS idx_assessment_owner_profile ON assessments (owner_profile_id);
CREATE INDEX IF NOT EXISTS idx_assessment_cohort ON assessments (cohort_id);

CREATE TABLE IF NOT EXISTS assessment_parameters (
    id                                                    VARCHAR(255)                NOT NULL,
    assessment_id                                         VARCHAR(255)                NOT NULL,
    can_be_submitted_after_due_date                       BOOLEAN                     NOT NULL,
    needs_to_finish_in_one_sitting                        BOOLEAN                     NOT NULL,
    maximum_time_allowed                                  INTEGER,
    monitor_for_cheating                                  BOOLEAN                     NOT NULL,
    can_view_definitive_testcase_scoring_after_submission BOOLEAN                     NOT NULL,
    created_at                                            TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at                                            TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT assessment_parameters_pkey PRIMARY KEY (id),
    CONSTRAINT assessment_parameters_assessment_id_key UNIQUE (assessment_id),
    CONSTRAINT fk_assessment_parameters_assessment FOREIGN KEY (assessment_id) REFERENCES assessments (id)
);

CREATE INDEX IF NOT EXISTS idx_assessment_parameters_assessment ON assessment_parameters (assessment_id);

CREATE TABLE IF NOT EXISTS assessment_parameters_change_log (
    id                 VARCHAR(255)                NOT NULL,
    assessment_id      VARCHAR(255)                NOT NULL,
    changer_profile_id VARCHAR(255),
    parameter_name     VARCHAR(255)                NOT NULL,
    old_value          VARCHAR(255)                NOT NULL,
    new_value          VARCHAR(255)                NOT NULL,
    changed_at         TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT assessment_parameters_change_log_pkey PRIMARY KEY (id),
    CONSTRAINT fk_assessment_parameters_change_log_assessment FOREIGN KEY (assessment_id) REFERENCES assessments (id)
);

CREATE INDEX IF NOT EXISTS idx_assessment_parameters_change_log_assessment ON assessment_parameters_change_log (assessment_id);
CREATE INDEX IF NOT EXISTS idx_assessment_parameters_change_log_changed_at ON assessment_parameters_change_log (changed_at);
CREATE INDEX IF NOT EXISTS idx_assessment_parameters_change_log_changer_profile ON assessment_parameters_change_log (changer_profile_id);

CREATE TABLE IF NOT EXISTS assessment_questions (
    id                 VARCHAR(255)                NOT NULL,
    assessment_id      VARCHAR(255)                NOT NULL,
    "position"         INTEGER                     NOT NULL,
    statement          TEXT                        NOT NULL,
    maximum_grade      NUMERIC(38, 2)              NOT NULL,
    allow_copy_pasting BOOLEAN                     NOT NULL,
    created_at         TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT assessment_questions_pkey PRIMARY KEY (id),
    CONSTRAINT uk_assessment_question_assessment_position UNIQUE (assessment_id, "position"),
    CONSTRAINT fk_assessment_questions_assessment FOREIGN KEY (assessment_id) REFERENCES assessments (id)
);

CREATE INDEX IF NOT EXISTS idx_assessment_question_assessment_position ON assessment_questions (assessment_id, "position");

CREATE TABLE IF NOT EXISTS assessment_question_code_drafts (
    question_id VARCHAR(255) NOT NULL,
    language    VARCHAR(255) NOT NULL,
    code        TEXT         NOT NULL,
    CONSTRAINT uk_question_code_draft_language UNIQUE (question_id, language),
    CONSTRAINT assessment_question_code_drafts_language_check CHECK (language IN ('C', 'PYTHON', 'JAVA')),
    CONSTRAINT fk_assessment_question_code_drafts_question FOREIGN KEY (question_id) REFERENCES assessment_questions (id)
);

CREATE TABLE IF NOT EXISTS assessment_results (
    id                   VARCHAR(255)                NOT NULL,
    assessment_id        VARCHAR(255)                NOT NULL,
    student_profile_id   VARCHAR(255)                NOT NULL,
    final_score          NUMERIC(5, 2),
    general_comment      TEXT,
    graded_by_profile_id VARCHAR(255),
    graded_at            TIMESTAMP(6) WITH TIME ZONE,
    created_at           TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at           TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT assessment_results_pkey PRIMARY KEY (id),
    CONSTRAINT uk_assessment_result_assessment_student UNIQUE (assessment_id, student_profile_id),
    CONSTRAINT fk_assessment_results_assessment FOREIGN KEY (assessment_id) REFERENCES assessments (id)
);

CREATE INDEX IF NOT EXISTS idx_assessment_result_student_profile ON assessment_results (student_profile_id);

CREATE TABLE IF NOT EXISTS assessment_sessions (
    id                 VARCHAR(255)                NOT NULL,
    assessment_id      VARCHAR(255)                NOT NULL,
    student_profile_id VARCHAR(255)                NOT NULL,
    state              VARCHAR(255)                NOT NULL,
    last_heartbeat_at  TIMESTAMP(6) WITH TIME ZONE,
    finished_at        TIMESTAMP(6) WITH TIME ZONE,
    created_at         TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT assessment_sessions_pkey PRIMARY KEY (id),
    CONSTRAINT uk_assessment_session_assessment_student UNIQUE (assessment_id, student_profile_id),
    CONSTRAINT assessment_sessions_state_check CHECK (state IN ('IN_PROGRESS', 'SUBMITTED', 'EXPIRED', 'ABANDONED')),
    CONSTRAINT fk_assessment_sessions_assessment FOREIGN KEY (assessment_id) REFERENCES assessments (id)
);

CREATE INDEX IF NOT EXISTS idx_assessment_session_student_profile ON assessment_sessions (student_profile_id);
