-- V117__anamnesis_question_date.sql
-- Novo tipo de pergunta de anamnese: DATE (ex.: data de nascimento com calendário).

ALTER TABLE anamnesis_questions DROP CONSTRAINT IF EXISTS chk_anamnesis_questions_type;
ALTER TABLE anamnesis_questions ADD CONSTRAINT chk_anamnesis_questions_type CHECK (question_type IN
    ('SHORT_TEXT', 'LONG_TEXT', 'YES_NO', 'YES_NO_WITH_TEXT', 'NUMBER', 'SINGLE_SELECT', 'MULTI_SELECT', 'DATE'));
