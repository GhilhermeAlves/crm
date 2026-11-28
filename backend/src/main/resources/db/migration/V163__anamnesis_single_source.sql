-- Anamnese como fonte única (spec docs/superpowers/specs/2026-11-28-anamnese-fonte-unica-design.md):
-- 1. vínculo das perguntas com o cadastro da pessoa e com o exame tricológico;
-- 2. divergências entre a anamnese e o cadastro (não sobrescreve dados existentes);
-- 3. exame tricológico ligado à anamnese;
-- 4. dados: modelos "Ficha de Anamnese Capilar" ganham vínculos e a seção "Avaliação tricológica".

-- ===========================================================================
-- 1. Vínculo da pergunta
-- ===========================================================================
ALTER TABLE anamnesis_questions ADD COLUMN binding VARCHAR(40);
ALTER TABLE anamnesis_questions ADD CONSTRAINT chk_anamnesis_questions_binding CHECK (binding IS NULL OR binding IN (
    'PERSON_NAME', 'PERSON_BIRTH_DATE', 'PERSON_SEX', 'PERSON_CPF', 'PERSON_MOBILE', 'PERSON_EMAIL', 'PERSON_ADDRESS',
    'TRICHO_SEX', 'TRICHO_PATTERN', 'TRICHO_ONSET', 'TRICHO_DAILY_SHEDDING', 'TRICHO_FAMILY_HISTORY',
    'TRICHO_RECENT_TRIGGERS', 'TRICHO_POSTPARTUM', 'TRICHO_SCALP_SYMPTOMS', 'TRICHO_HAIR_PROCEDURES',
    'TRICHO_ACNE_HIRSUTISM', 'TRICHO_IRREGULAR_CYCLES', 'TRICHO_ANABOLIC_USE', 'TRICHO_CHIEF_COMPLAINT'));
CREATE UNIQUE INDEX uq_anamnesis_questions_model_binding
    ON anamnesis_questions (model_id, binding) WHERE binding IS NOT NULL;

-- ===========================================================================
-- 2. Divergências entre anamnese e cadastro
-- ===========================================================================
CREATE TABLE person_profile_divergences (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id     UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    person_id      UUID NOT NULL,
    anamnesis_id   UUID REFERENCES patient_anamneses(id) ON DELETE SET NULL,
    field          VARCHAR(40) NOT NULL,
    current_value  TEXT,
    proposed_value TEXT NOT NULL,
    reason         VARCHAR(255),
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    resolved_by    UUID REFERENCES users(id) ON DELETE SET NULL,
    resolved_at    TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_ppd_person FOREIGN KEY (company_id, person_id)
        REFERENCES persons (company_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_ppd_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DISMISSED')),
    CONSTRAINT ck_ppd_field CHECK (field IN ('NAME', 'BIRTH_DATE', 'GENDER', 'CPF', 'MOBILE', 'EMAIL', 'ADDRESS'))
);
CREATE INDEX idx_ppd_person_pending ON person_profile_divergences (company_id, person_id) WHERE status = 'PENDING';

ALTER TABLE person_profile_divergences ENABLE ROW LEVEL SECURITY;
ALTER TABLE person_profile_divergences FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON person_profile_divergences
    USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON person_profile_divergences TO crm_app;

-- ===========================================================================
-- 3. Exame tricológico ligado à anamnese
-- ===========================================================================
ALTER TABLE trichology_assessments
    ADD COLUMN anamnesis_id UUID REFERENCES patient_anamneses(id) ON DELETE SET NULL;

-- ===========================================================================
-- 4. Dados (tabelas de anamnese têm RLS FORCE → por empresa, como a V147)
-- ===========================================================================
CREATE FUNCTION app_tmp_v163_add_question(p_section UUID, p_model UUID, p_company UUID, p_order INT, p_text TEXT,
                                          p_type TEXT, p_required BOOLEAN, p_binding TEXT, p_options TEXT[])
RETURNS VOID LANGUAGE plpgsql AS $f$
DECLARE q_id UUID; opt TEXT; i INT := 0;
BEGIN
    INSERT INTO anamnesis_questions (section_id, model_id, company_id, text, question_type, is_required, binding,
                                     sort_order)
    VALUES (p_section, p_model, p_company, p_text, p_type, p_required, p_binding, p_order)
    RETURNING id INTO q_id;
    IF p_options IS NOT NULL THEN
        FOREACH opt IN ARRAY p_options LOOP
            i := i + 1;
            INSERT INTO anamnesis_question_options (question_id, company_id, label, value, sort_order)
            VALUES (q_id, p_company, split_part(opt, '|', 2), split_part(opt, '|', 1), i);
        END LOOP;
    END IF;
END $f$;

DO $$
DECLARE c RECORD; m RECORD; s_id UUID; next_order INT;
BEGIN
    FOR c IN SELECT id FROM companies LOOP
        PERFORM set_config('app.current_company_id', c.id::TEXT, TRUE);
        FOR m IN SELECT id FROM anamnesis_models WHERE company_id = c.id AND name = 'Ficha de Anamnese Capilar' LOOP
            UPDATE anamnesis_questions q SET binding = v.b, updated_at = CURRENT_TIMESTAMP
            FROM (VALUES ('Nome completo', 'PERSON_NAME'),
                         ('Data de nascimento', 'PERSON_BIRTH_DATE'),
                         ('Sexo', 'PERSON_SEX'),
                         ('Telefone / WhatsApp', 'PERSON_MOBILE'),
                         ('E-mail', 'PERSON_EMAIL'),
                         ('Cidade / endereço', 'PERSON_ADDRESS'),
                         ('Qual é sua principal queixa ou disfunção capilar?', 'TRICHO_CHIEF_COMPLAINT')) AS v(t, b)
            WHERE q.id = (SELECT q2.id FROM anamnesis_questions q2
                          JOIN anamnesis_sections s ON s.id = q2.section_id
                          WHERE q2.model_id = m.id AND trim(q2.text) = v.t
                          ORDER BY s.sort_order, q2.sort_order LIMIT 1)
              AND q.binding IS NULL
              AND NOT EXISTS (SELECT 1 FROM anamnesis_questions q3 WHERE q3.model_id = m.id AND q3.binding = v.b);

            UPDATE anamnesis_question_options o
               SET value = CASE o.label WHEN 'Feminino' THEN 'F' ELSE 'M' END, updated_at = CURRENT_TIMESTAMP
            WHERE o.question_id IN (SELECT id FROM anamnesis_questions WHERE model_id = m.id AND binding = 'PERSON_SEX')
              AND o.label IN ('Feminino', 'Masculino');

            IF NOT EXISTS (SELECT 1 FROM anamnesis_questions WHERE model_id = m.id AND binding = 'TRICHO_ONSET') THEN
                SELECT COALESCE(MAX(sort_order), 0) + 1 INTO next_order FROM anamnesis_sections WHERE model_id = m.id;
                INSERT INTO anamnesis_sections (model_id, company_id, title, is_professional, sort_order)
                VALUES (m.id, c.id, 'Avaliação tricológica', FALSE, next_order)
                RETURNING id INTO s_id;
                PERFORM app_tmp_v163_add_question(s_id, m.id, c.id, 1,
                    'Há quanto tempo a queda ou o afinamento começou?', 'SINGLE_SELECT', TRUE, 'TRICHO_ONSET',
                    ARRAY['LT_3M|Menos de 3 meses', '3_12M|De 3 a 12 meses', 'GT_1Y|Mais de 1 ano']);
                PERFORM app_tmp_v163_add_question(s_id, m.id, c.id, 2,
                    'Como é a perda ou o afinamento?', 'SINGLE_SELECT', TRUE, 'TRICHO_PATTERN',
                    ARRAY['DIFFUSE_SHEDDING|Queda espalhada pela cabeça',
                          'THINNING_TOP|Afinamento no topo, entradas ou coroa',
                          'PATCHES|Falhas arredondadas',
                          'SCALP_SYMPTOMS|Principalmente coceira, ardor ou descamação',
                          'BREAKAGE|Fios quebrando no comprimento']);
                PERFORM app_tmp_v163_add_question(s_id, m.id, c.id, 3,
                    'Quantos fios você vê caindo por dia (banho, travesseiro, escova)?', 'SINGLE_SELECT', TRUE,
                    'TRICHO_DAILY_SHEDDING',
                    ARRAY['LT_50|Menos de 50', '50_100|Entre 50 e 100', 'GT_100|Mais de 100']);
                PERFORM app_tmp_v163_add_question(s_id, m.id, c.id, 4,
                    'Nos últimos 6 meses aconteceu algo destes?', 'MULTI_SELECT', TRUE, 'TRICHO_RECENT_TRIGGERS',
                    ARRAY['FEVER_INFECTION|Febre alta, COVID ou infecção', 'SURGERY|Cirurgia',
                          'WEIGHT_LOSS|Perda de peso rápida ou dieta restritiva', 'SEVERE_STRESS|Estresse intenso',
                          'NEW_MEDICATION|Remédio novo', 'NONE|Nenhum']);
                PERFORM app_tmp_v163_add_question(s_id, m.id, c.id, 5,
                    'Pais ou irmãos têm calvície ou afinamento?', 'YES_NO', TRUE, 'TRICHO_FAMILY_HISTORY', NULL);
                PERFORM app_tmp_v163_add_question(s_id, m.id, c.id, 6,
                    'Sente algo no couro cabeludo?', 'MULTI_SELECT', TRUE, 'TRICHO_SCALP_SYMPTOMS',
                    ARRAY['ITCH|Coceira', 'BURNING|Ardor', 'PAIN|Dor', 'OILINESS|Oleosidade',
                          'FLAKING|Descamação', 'NONE|Nada']);
                PERFORM app_tmp_v163_add_question(s_id, m.id, c.id, 7,
                    'Faz procedimentos no cabelo?', 'MULTI_SELECT', FALSE, 'TRICHO_HAIR_PROCEDURES',
                    ARRAY['CHEMICAL|Química/alisamento', 'HEAT|Chapinha ou secador quase todo dia',
                          'TIGHT_STYLES|Penteados presos/apertados', 'NONE|Nenhum']);
                PERFORM app_tmp_v163_add_question(s_id, m.id, c.id, 8,
                    '(Mulheres) Teve parto nos últimos 12 meses?', 'YES_NO', FALSE, 'TRICHO_POSTPARTUM', NULL);
                PERFORM app_tmp_v163_add_question(s_id, m.id, c.id, 9,
                    '(Mulheres) Ciclo menstrual irregular?', 'YES_NO', FALSE, 'TRICHO_IRREGULAR_CYCLES', NULL);
                PERFORM app_tmp_v163_add_question(s_id, m.id, c.id, 10,
                    '(Mulheres) Acne ou aumento de pelos no rosto/corpo?', 'YES_NO', FALSE,
                    'TRICHO_ACNE_HIRSUTISM', NULL);
                PERFORM app_tmp_v163_add_question(s_id, m.id, c.id, 11,
                    '(Homens) Usa ou usou testosterona/anabolizantes?', 'YES_NO', FALSE,
                    'TRICHO_ANABOLIC_USE', NULL);
                UPDATE anamnesis_models SET version = version + 1, updated_at = CURRENT_TIMESTAMP WHERE id = m.id;
                RAISE NOTICE 'V163: modelo % da empresa % recebeu a seção Avaliação tricológica', m.id, c.id;
            END IF;
        END LOOP;
    END LOOP;
    PERFORM set_config('app.current_company_id', '', TRUE);
END $$;

DROP FUNCTION app_tmp_v163_add_question(UUID, UUID, UUID, INT, TEXT, TEXT, BOOLEAN, TEXT, TEXT[]);
