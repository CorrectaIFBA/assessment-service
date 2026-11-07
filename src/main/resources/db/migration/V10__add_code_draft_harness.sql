-- Code that wraps the student's code to run the test cases, one per language.
-- Nullable for now; existing drafts get an empty harness.
ALTER TABLE assessment_question_code_drafts
    ADD COLUMN IF NOT EXISTS harness TEXT DEFAULT '';
