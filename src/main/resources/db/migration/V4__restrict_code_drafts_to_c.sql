-- Only C is supported for now. Python and Java drafts are removed, and a
-- question that allowed only those languages gets an empty C draft, so every
-- question keeps at least one allowed language.
INSERT INTO assessment_question_code_drafts (question_id, language, code)
SELECT question.id, 'C', ''
FROM assessment_questions question
WHERE NOT EXISTS (
    SELECT 1 FROM assessment_question_code_drafts draft
    WHERE draft.question_id = question.id AND draft.language = 'C'
);

DELETE FROM assessment_question_code_drafts WHERE language <> 'C';

ALTER TABLE assessment_question_code_drafts
    DROP CONSTRAINT IF EXISTS assessment_question_code_drafts_language_check;

ALTER TABLE assessment_question_code_drafts
    ADD CONSTRAINT assessment_question_code_drafts_language_check CHECK (language IN ('C'));
