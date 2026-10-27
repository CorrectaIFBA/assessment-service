-- Hibernate's ddl-auto=update added columns but never removed them, so databases
-- created before Flyway still carry columns of fields that left the entities.
-- Those columns are NOT NULL, and since no INSERT fills them any more, they made
-- every insert into their table fail. None of them exists in a database created
-- by V1, hence the IF EXISTS.

-- A question used to hold one draft in one language. Each such draft becomes the
-- question's only allowed language before its columns go.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'assessment_questions'
          AND column_name = 'draft_code_language'
    ) THEN
        INSERT INTO assessment_question_code_drafts (question_id, language, code)
        SELECT question.id, question.draft_code_language, COALESCE(question.draft_code, '')
        FROM assessment_questions question
        WHERE question.draft_code_language IS NOT NULL
          AND NOT EXISTS (
              SELECT 1 FROM assessment_question_code_drafts draft WHERE draft.question_id = question.id
          );
    END IF;
END
$$;

ALTER TABLE assessment_questions
    DROP COLUMN IF EXISTS code_snippet,
    DROP COLUMN IF EXISTS draft_code,
    DROP COLUMN IF EXISTS draft_code_language;

ALTER TABLE assessment_parameters
    DROP COLUMN IF EXISTS can_view_questions_out_of_order,
    DROP COLUMN IF EXISTS question_order;
