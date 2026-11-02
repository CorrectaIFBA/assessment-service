-- Assessments and questions are addressed by slug in the web app's URLs:
-- /classes/{cohort}/activities/{assessment}/questions/{question}. A slug only
-- has to be unique where it is used, so inside its cohort or assessment.
-- Questions also get a title, which the slug starts from.

ALTER TABLE assessments ADD COLUMN IF NOT EXISTS slug VARCHAR(160);
ALTER TABLE assessment_questions ADD COLUMN IF NOT EXISTS title VARCHAR(255);
ALTER TABLE assessment_questions ADD COLUMN IF NOT EXISTS slug VARCHAR(160);

-- Same rule as the service: accents dropped, anything else that is not a
-- letter or a digit becomes a hyphen.
CREATE OR REPLACE FUNCTION pg_temp.to_slug(value TEXT, fallback TEXT) RETURNS TEXT AS $$
    SELECT COALESCE(NULLIF(
        trim(BOTH '-' FROM left(regexp_replace(lower(translate(value,
            'áàâãäåçéèêëíìîïñóòôõöúùûüýÿÁÀÂÃÄÅÇÉÈÊËÍÌÎÏÑÓÒÔÕÖÚÙÛÜÝ',
            'aaaaaaceeeeiiiinooooouuuuyyAAAAAACEEEEIIIINOOOOOUUUUY')), '[^a-z0-9]+', '-', 'g'), 150)),
    ''), fallback)
$$ LANGUAGE SQL IMMUTABLE;

-- Existing questions are titled by their place in the assessment.
UPDATE assessment_questions question
SET title = 'Questão ' || numbered.number
FROM (
    SELECT id, row_number() OVER (PARTITION BY assessment_id ORDER BY position) AS number
    FROM assessment_questions
) numbered
WHERE question.id = numbered.id AND question.title IS NULL;

-- Repeated slugs in the same scope get the row's order as a suffix; a
-- clash with a suffixed slug of another row would stop the migration
-- on the unique constraints below instead of guessing.
UPDATE assessments assessment
SET slug = CASE WHEN ranked.rank = 1 THEN ranked.base ELSE ranked.base || '-' || ranked.rank END
FROM (
    SELECT id, pg_temp.to_slug(title, 'atividade') AS base,
           row_number() OVER (PARTITION BY cohort_id, pg_temp.to_slug(title, 'atividade') ORDER BY created_at, id) AS rank
    FROM assessments
) ranked
WHERE assessment.id = ranked.id AND assessment.slug IS NULL;

UPDATE assessment_questions question
SET slug = CASE WHEN ranked.rank = 1 THEN ranked.base ELSE ranked.base || '-' || ranked.rank END
FROM (
    SELECT id, pg_temp.to_slug(title, 'questao') AS base,
           row_number() OVER (PARTITION BY assessment_id, pg_temp.to_slug(title, 'questao') ORDER BY position) AS rank
    FROM assessment_questions
) ranked
WHERE question.id = ranked.id AND question.slug IS NULL;

ALTER TABLE assessments ALTER COLUMN slug SET NOT NULL;
ALTER TABLE assessment_questions ALTER COLUMN title SET NOT NULL;
ALTER TABLE assessment_questions ALTER COLUMN slug SET NOT NULL;

ALTER TABLE assessments
    ADD CONSTRAINT uk_assessment_cohort_slug UNIQUE (cohort_id, slug);
ALTER TABLE assessment_questions
    ADD CONSTRAINT uk_assessment_question_assessment_slug UNIQUE (assessment_id, slug);
