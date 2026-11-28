# Anamnese como fonte única — Plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A anamnese vem pré-preenchida com o cadastro, completa o cadastro ao ser enviada (com divergências em vez de sobrescrita) e alimenta o exame tricológico (Copiloto e laudo), substituindo o quiz.

**Architecture:** Perguntas de anamnese ganham um `binding` opcional (enum fechado). O snapshot da anamnese já serializa `AnamnesisQuestionResponse`, então o vínculo viaja junto. Na conclusão, `AnamnesisProfileSync` aplica os vínculos `PERSON_*` no cadastro via um método novo e estreito do `PersonService`; diferenças viram `person_profile_divergences`. O exame aponta para a anamnese (`anamnesis_id`) e lê os vínculos `TRICHO_*` por `AnamnesisClinicalView`, no mesmo formato de chaves do antigo quiz, para não mexer nas regras do Copiloto.

**Tech Stack:** Spring Boot 3 / Java 21, Flyway (PostgreSQL 17, RLS FORCE), JUnit 5 + Mockito, Testcontainers nos `*IT`; Next.js 15 + React Query + Vitest no `frontend-refine`.

**Spec:** `docs/superpowers/specs/2026-11-28-anamnese-fonte-unica-design.md`

## Global Constraints

- Branch `feature/anamnese-fonte-unica` (worktree `crm-anamnese`), base `crm-improvements-deploy-phase`. Commitar local; **sem push** até o usuário pedir.
- Próxima migration: **V163** (última é V162). Sem banco local: conferir nome de tabela/coluna inexistente, RLS `ENABLE` + `FORCE` + `tenant_isolation_policy` + `GRANT` ao `crm_app` no padrão da V147; migrations de dados em tabelas com RLS FORCE rodam por empresa com `set_config('app.current_company_id', ...)` como na V147.
- **Nunca rodar `mvn test`** — passar o comando ao usuário. Frontend: rodar `npx vitest run <arquivos>` e `npx prettier --check --end-of-line auto <arquivos>` em todo arquivo do frontend alterado (usar o `node_modules` de `crm/frontend-refine` copiando o arquivo para lá se o worktree não tiver dependências).
- Toda guarda nova deixa o `CRM_ADMIN` passar.
- Textos ao usuário em pt-BR.
- Perguntas `PERSON_*` **nunca** entram no input da IA.
- Mutations que alteram o paciente invalidam todas as queries afetadas (pessoa, paciente, anamnese, divergências, exame).
- O preço do catálogo no procedimento já é aplicado por `ProcedureDrawer` (`catalogCents`); **fora do plano** (era falta de preço cadastrado).

---

## Mapa de arquivos

Backend (`backend/src/main/java/com/becommerce/crm/...`):
- Create `masterdata/anamnesis/domain/AnamnesisBinding.java` — enum dos vínculos, grupo, tipos aceitos, chave do quiz e códigos aceitos.
- Modify `masterdata/anamnesis/domain/AnamnesisQuestion.java`, `.../dto/AnamnesisQuestionRequest.java`, `.../dto/AnamnesisQuestionResponse.java`, `.../infrastructure/persistence/AnamnesisQuestionJpaEntity.java` + `AnamnesisQuestionRepositoryImpl.java`, `.../application/service/AnamnesisService.java` (validação).
- Create `clinical/anamnesis/application/service/AnamnesisBindings.java` — lê snapshot + respostas e devolve valores por vínculo (usado por prefill, sync e visão clínica).
- Create `clinical/anamnesis/application/service/AnamnesisProfileSync.java`.
- Create `clinical/anamnesis/divergence/` — `ProfileDivergence` (JPA entity + repo), `ProfileDivergenceService`, `ProfileDivergenceController`.
- Modify `masterdata/person/PersonService.java` — `applyAnamnesisPatch(...)`.
- Modify `clinical/anamnesis/application/service/PatientAnamnesisService.java` — prefill no `start`/`get`, chamada ao sync na conclusão, gancho para ligar exame aberto.
- Modify `clinical/anamnesis/application/dto/PatientAnamnesisResponse.java` — campo `prefill`.
- Create `clinical/trichology/application/port/AnamnesisClinicalView.java` + implementação `clinical/anamnesis/application/service/AnamnesisClinicalViewImpl.java`.
- Modify `clinical/trichology/...`: `AssessmentService`, `ExamCopilotService`, `ImageAnalysisService`, `ReportService`, `AssessmentController`, `PublicTrichologyController`, domínio/JPA do assessment (`anamnesisId`), remove `QuizEngine`.
- Modify `shared/web/handler/GlobalExceptionHandler.java` (+ teste).

Migration: `backend/src/main/resources/db/migration/V163__anamnesis_single_source.sql`.

Frontend (`frontend-refine/src/...`):
- Modify `features/masterdata/anamnesis/components/AnamnesisModelEditor.tsx` + types/schema do modelo — seletor "Vincular a".
- Modify `features/clinical/anamnesis/components/AnamnesisFields.tsx` (+ formulário que o usa) — prefill.
- Create `features/clinical/anamnesis/components/ProfileDivergenceBanner.tsx` + service/hook.
- Modify `features/clinical/trichology/components/AssessmentSteps.tsx`, `AssessmentWorkspace.tsx`, `CopilotPanel.tsx`, `services/trichology.service.ts`, `hooks/useTrichology.ts`, `types/trichology.types.ts`; delete `QuizForm.tsx`, quiz em `PublicPages.tsx`, `app/(public)/quiz-capilar/[token]/page.tsx`.
- Modify `features/clinical/budgets/utils/blocks.ts`.

---

### Task 1: Migration V163

**Files:**
- Create: `backend/src/main/resources/db/migration/V163__anamnesis_single_source.sql`
- Test: `backend/src/test/java/com/becommerce/crm/clinical/anamnesis/infrastructure/persistence/AnamnesisSingleSourceMigrationIT.java`

**Interfaces:**
- Produces: coluna `anamnesis_questions.binding VARCHAR(40)`; tabela `person_profile_divergences`; coluna `trichology_assessments.anamnesis_id UUID`; códigos de vínculo (lista fechada abaixo) usados pelas Tasks 2–6.

- [ ] **Step 1: Escrever o IT (falha porque a V163 não existe)**

Seguir o padrão de `PatientProcedureIsolationIT` (bootstrap `agent-rls-bootstrap.sql`, role `crm_app` NOBYPASSRLS, tabelas mínimas). Aplicar V091, V117, V105, V147 (só o necessário: criar `persons` mínima com `company_id, id, UNIQUE(company_id,id)` se a V147 inteira não couber), V152 mínima (`trichology_assessments(id, company_id)`) e a V163. Testes:

```java
@Test void bindingIsUniquePerModel() {
    // insere duas perguntas no mesmo model_id com binding 'PERSON_NAME' → a segunda lança SQLException (23505)
}
@Test void bindingRejectsUnknownCode() {
    // binding 'FOO' → SQLException (23514, CHECK)
}
@Test void divergencesAreIsolatedByTenant() {
    // insere divergência em A e em B como superuser; com app.current_company_id=A, SELECT conta 1
}
@Test void capillaryModelGetsBindingsAndClinicalSection() {
    // seed: modelo "Ficha de Anamnese Capilar" com seção e perguntas "Nome completo", "Data de nascimento",
    // "Sexo" (opções Feminino/Masculino com value 'Feminino'/'Masculino'), "Telefone / WhatsApp", "E-mail",
    // "Cidade / endereço", "Qual é sua principal queixa ou disfunção capilar?" ANTES da V163;
    // depois da V163: bindings PERSON_NAME, PERSON_BIRTH_DATE, PERSON_SEX, PERSON_MOBILE, PERSON_EMAIL,
    // PERSON_ADDRESS, TRICHO_CHIEF_COMPLAINT; opções do Sexo com value 'F'/'M';
    // existe seção 'Avaliação tricológica' com 11 perguntas TRICHO_*; rodar o bloco de dados 2x não duplica.
}
```

- [ ] **Step 2: Escrever a V163**

```sql
-- Anamnese como fonte única (spec 2026-11-28): vínculos de pergunta, divergências
-- de cadastro e exame tricológico ligado à anamnese.

-- 1. Vínculo da pergunta
ALTER TABLE anamnesis_questions ADD COLUMN binding VARCHAR(40);
ALTER TABLE anamnesis_questions ADD CONSTRAINT chk_anamnesis_questions_binding CHECK (binding IS NULL OR binding IN (
    'PERSON_NAME', 'PERSON_BIRTH_DATE', 'PERSON_SEX', 'PERSON_CPF', 'PERSON_MOBILE', 'PERSON_EMAIL', 'PERSON_ADDRESS',
    'TRICHO_SEX', 'TRICHO_PATTERN', 'TRICHO_ONSET', 'TRICHO_DAILY_SHEDDING', 'TRICHO_FAMILY_HISTORY',
    'TRICHO_RECENT_TRIGGERS', 'TRICHO_POSTPARTUM', 'TRICHO_SCALP_SYMPTOMS', 'TRICHO_HAIR_PROCEDURES',
    'TRICHO_ACNE_HIRSUTISM', 'TRICHO_IRREGULAR_CYCLES', 'TRICHO_ANABOLIC_USE', 'TRICHO_CHIEF_COMPLAINT'));
CREATE UNIQUE INDEX uq_anamnesis_questions_model_binding
    ON anamnesis_questions (model_id, binding) WHERE binding IS NOT NULL;

-- 2. Divergências entre anamnese e cadastro
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
    CONSTRAINT fk_ppd_person FOREIGN KEY (company_id, person_id) REFERENCES persons (company_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_ppd_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DISMISSED'))
);
CREATE INDEX idx_ppd_person_pending ON person_profile_divergences (company_id, person_id) WHERE status = 'PENDING';
ALTER TABLE person_profile_divergences ENABLE ROW LEVEL SECURITY;
ALTER TABLE person_profile_divergences FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON person_profile_divergences
    USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON person_profile_divergences TO crm_app;

-- 3. Exame ligado à anamnese
ALTER TABLE trichology_assessments ADD COLUMN anamnesis_id UUID REFERENCES patient_anamneses(id) ON DELETE SET NULL;

-- 4. Dados: modelos "Ficha de Anamnese Capilar" (tabelas com RLS FORCE → por empresa)
DO $$
DECLARE c RECORD; m RECORD; s_id UUID; q_id UUID; next_order INT;
BEGIN
    FOR c IN SELECT id FROM companies LOOP
        PERFORM set_config('app.current_company_id', c.id::TEXT, TRUE);
        FOR m IN SELECT id FROM anamnesis_models WHERE company_id = c.id AND name = 'Ficha de Anamnese Capilar' LOOP
            -- vínculos por texto exato (primeira ocorrência por sort)
            UPDATE anamnesis_questions q SET binding = v.b FROM (VALUES
                ('Nome completo', 'PERSON_NAME'), ('Data de nascimento', 'PERSON_BIRTH_DATE'), ('Sexo', 'PERSON_SEX'),
                ('Telefone / WhatsApp', 'PERSON_MOBILE'), ('E-mail', 'PERSON_EMAIL'),
                ('Cidade / endereço', 'PERSON_ADDRESS'),
                ('Qual é sua principal queixa ou disfunção capilar?', 'TRICHO_CHIEF_COMPLAINT')) AS v(t, b)
            WHERE q.id = (SELECT q2.id FROM anamnesis_questions q2 JOIN anamnesis_sections s ON s.id = q2.section_id
                          WHERE q2.model_id = m.id AND q2.text = v.t ORDER BY s.sort_order, q2.sort_order LIMIT 1)
              AND NOT EXISTS (SELECT 1 FROM anamnesis_questions q3 WHERE q3.model_id = m.id AND q3.binding = v.b);
            UPDATE anamnesis_question_options o SET value = CASE o.label WHEN 'Feminino' THEN 'F' WHEN 'Masculino' THEN 'M' END
            WHERE o.question_id IN (SELECT id FROM anamnesis_questions WHERE model_id = m.id AND binding = 'PERSON_SEX')
              AND o.label IN ('Feminino', 'Masculino');

            IF NOT EXISTS (SELECT 1 FROM anamnesis_questions WHERE model_id = m.id AND binding = 'TRICHO_ONSET') THEN
                SELECT COALESCE(MAX(sort_order), 0) + 1 INTO next_order FROM anamnesis_sections WHERE model_id = m.id;
                INSERT INTO anamnesis_sections (model_id, company_id, title, is_professional, sort_order)
                VALUES (m.id, c.id, 'Avaliação tricológica', FALSE, next_order) RETURNING id INTO s_id;
                PERFORM app_tmp_add_question(s_id, m.id, c.id, 1, 'Há quanto tempo a queda ou o afinamento começou?', 'SINGLE_SELECT', TRUE, 'TRICHO_ONSET',
                    ARRAY['LT_3M|Menos de 3 meses', '3_12M|De 3 a 12 meses', 'GT_1Y|Mais de 1 ano']);
                PERFORM app_tmp_add_question(s_id, m.id, c.id, 2, 'Como é a perda ou o afinamento?', 'SINGLE_SELECT', TRUE, 'TRICHO_PATTERN',
                    ARRAY['DIFFUSE_SHEDDING|Queda espalhada pela cabeça', 'THINNING_TOP|Afinamento no topo, entradas ou coroa',
                          'PATCHES|Falhas arredondadas', 'SCALP_SYMPTOMS|Principalmente coceira, ardor ou descamação',
                          'BREAKAGE|Fios quebrando no comprimento']);
                PERFORM app_tmp_add_question(s_id, m.id, c.id, 3, 'Quantos fios você vê caindo por dia (banho, travesseiro, escova)?', 'SINGLE_SELECT', TRUE, 'TRICHO_DAILY_SHEDDING',
                    ARRAY['LT_50|Menos de 50', '50_100|Entre 50 e 100', 'GT_100|Mais de 100']);
                PERFORM app_tmp_add_question(s_id, m.id, c.id, 4, 'Nos últimos 6 meses aconteceu algo destes?', 'MULTI_SELECT', TRUE, 'TRICHO_RECENT_TRIGGERS',
                    ARRAY['FEVER_INFECTION|Febre alta, COVID ou infecção', 'SURGERY|Cirurgia',
                          'WEIGHT_LOSS|Perda de peso rápida ou dieta restritiva', 'SEVERE_STRESS|Estresse intenso',
                          'NEW_MEDICATION|Remédio novo', 'NONE|Nenhum']);
                PERFORM app_tmp_add_question(s_id, m.id, c.id, 5, 'Pais ou irmãos têm calvície ou afinamento?', 'YES_NO', TRUE, 'TRICHO_FAMILY_HISTORY', NULL);
                PERFORM app_tmp_add_question(s_id, m.id, c.id, 6, 'Sente algo no couro cabeludo?', 'MULTI_SELECT', TRUE, 'TRICHO_SCALP_SYMPTOMS',
                    ARRAY['ITCH|Coceira', 'BURNING|Ardor', 'PAIN|Dor', 'OILINESS|Oleosidade', 'FLAKING|Descamação', 'NONE|Nada']);
                PERFORM app_tmp_add_question(s_id, m.id, c.id, 7, 'Faz procedimentos no cabelo?', 'MULTI_SELECT', FALSE, 'TRICHO_HAIR_PROCEDURES',
                    ARRAY['CHEMICAL|Química/alisamento', 'HEAT|Chapinha ou secador quase todo dia',
                          'TIGHT_STYLES|Penteados presos/apertados', 'NONE|Nenhum']);
                PERFORM app_tmp_add_question(s_id, m.id, c.id, 8, '(Mulheres) Teve parto nos últimos 12 meses?', 'YES_NO', FALSE, 'TRICHO_POSTPARTUM', NULL);
                PERFORM app_tmp_add_question(s_id, m.id, c.id, 9, '(Mulheres) Ciclo menstrual irregular?', 'YES_NO', FALSE, 'TRICHO_IRREGULAR_CYCLES', NULL);
                PERFORM app_tmp_add_question(s_id, m.id, c.id, 10, '(Mulheres) Acne ou aumento de pelos no rosto/corpo?', 'YES_NO', FALSE, 'TRICHO_ACNE_HIRSUTISM', NULL);
                PERFORM app_tmp_add_question(s_id, m.id, c.id, 11, '(Homens) Usa ou usou testosterona/anabolizantes?', 'YES_NO', FALSE, 'TRICHO_ANABOLIC_USE', NULL);
                UPDATE anamnesis_models SET version = version + 1, updated_at = now() WHERE id = m.id;
            END IF;
        END LOOP;
    END LOOP;
    PERFORM set_config('app.current_company_id', '', TRUE);
END $$;
```

Antes do bloco `DO`, criar a função auxiliar e, depois dele, removê-la:

```sql
CREATE FUNCTION app_tmp_add_question(p_section UUID, p_model UUID, p_company UUID, p_order INT, p_text TEXT,
                                     p_type TEXT, p_required BOOLEAN, p_binding TEXT, p_options TEXT[])
RETURNS VOID LANGUAGE plpgsql AS $f$
DECLARE q_id UUID; opt TEXT; i INT := 0;
BEGIN
    INSERT INTO anamnesis_questions (section_id, model_id, company_id, text, question_type, is_required, binding, sort_order)
    VALUES (p_section, p_model, p_company, p_text, p_type, p_required, p_binding, p_order) RETURNING id INTO q_id;
    IF p_options IS NOT NULL THEN
        FOREACH opt IN ARRAY p_options LOOP
            i := i + 1;
            INSERT INTO anamnesis_question_options (question_id, company_id, label, value, sort_order)
            VALUES (q_id, p_company, split_part(opt, '|', 2), split_part(opt, '|', 1), i);
        END LOOP;
    END IF;
END $f$;
-- ... bloco DO acima ...
DROP FUNCTION app_tmp_add_question(UUID, UUID, UUID, INT, TEXT, TEXT, BOOLEAN, TEXT, TEXT[]);
```

Conferir em V091 os nomes exatos e colunas obrigatórias de `anamnesis_sections`, `anamnesis_questions` e `anamnesis_question_options` (ex.: `updated_at`, `complement_trigger` com default) e ajustar os `INSERT`.

- [ ] **Step 3: Passar o comando ao usuário**

Run (usuário): `cd backend && mvn verify -Dit.test=AnamnesisSingleSourceMigrationIT -Dtest=none -DfailIfNoTests=false`
Expected: 4 testes passam.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/resources/db/migration/V163__anamnesis_single_source.sql backend/src/test/java/com/becommerce/crm/clinical/anamnesis/infrastructure/persistence/AnamnesisSingleSourceMigrationIT.java
git commit -m "feat(anamnese): V163 — vínculo das perguntas, divergências e exame ligado à anamnese"
```

---

### Task 2: Vínculo no modelo de anamnese (backend)

**Files:**
- Create: `backend/src/main/java/com/becommerce/crm/masterdata/anamnesis/domain/AnamnesisBinding.java`
- Modify: `AnamnesisQuestion.java` (campo `binding`, em `create`/`reconstitute`), `AnamnesisQuestionRequest.java`, `AnamnesisQuestionResponse.java`, `AnamnesisQuestionJpaEntity.java`, `AnamnesisQuestionRepositoryImpl.java`, `AnamnesisService.java:~308`
- Test: `backend/src/test/java/com/becommerce/crm/masterdata/anamnesis/domain/AnamnesisBindingTest.java`, `backend/src/test/java/com/becommerce/crm/masterdata/anamnesis/application/service/AnamnesisServiceBindingTest.java`

**Interfaces:**
- Produces:
  - `enum AnamnesisBinding { ...; Group group(); Set<AnamnesisQuestionType> types(); String quizKey(); Set<String> codes(); static Optional<AnamnesisBinding> parse(String) }` com `enum Group { PERSON, TRICHO }`.
  - `AnamnesisQuestionRequest.binding` (`AnamnesisBinding`, nullable) e `AnamnesisQuestionResponse.binding` (mesmo tipo; vai no snapshot JSON como string).

- [ ] **Step 1: Teste do enum**

```java
class AnamnesisBindingTest {
    @Test void trichoBindingsExposeQuizKeysAndCodes() {
        assertEquals("pattern", AnamnesisBinding.TRICHO_PATTERN.quizKey());
        assertTrue(AnamnesisBinding.TRICHO_PATTERN.codes().contains("DIFFUSE_SHEDDING"));
        assertEquals("sex", AnamnesisBinding.TRICHO_SEX.quizKey());
        assertNull(AnamnesisBinding.PERSON_NAME.quizKey());
    }
    @Test void personSexIsAlsoUsedAsTrichoSex() {
        assertEquals("sex", AnamnesisBinding.PERSON_SEX.quizKey()); // sexo do cadastro serve para o laboratório
    }
    @Test void acceptsOnlyCompatibleTypes() {
        assertTrue(AnamnesisBinding.PERSON_BIRTH_DATE.types().contains(AnamnesisQuestionType.DATE));
        assertFalse(AnamnesisBinding.PERSON_BIRTH_DATE.types().contains(AnamnesisQuestionType.SHORT_TEXT));
    }
}
```

- [ ] **Step 2: Implementar `AnamnesisBinding`**

```java
package com.becommerce.crm.masterdata.anamnesis.domain;

import java.util.*;
import static com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisQuestionType.*;

/** Vínculo de uma pergunta com o cadastro da pessoa ou com o exame tricológico (spec 2026-11-28). */
public enum AnamnesisBinding {
    PERSON_NAME(Group.PERSON, Set.of(SHORT_TEXT), null, Set.of()),
    PERSON_BIRTH_DATE(Group.PERSON, Set.of(DATE), null, Set.of()),
    PERSON_SEX(Group.PERSON, Set.of(SINGLE_SELECT), "sex", Set.of("F", "M")),
    PERSON_CPF(Group.PERSON, Set.of(SHORT_TEXT), null, Set.of()),
    PERSON_MOBILE(Group.PERSON, Set.of(SHORT_TEXT), null, Set.of()),
    PERSON_EMAIL(Group.PERSON, Set.of(SHORT_TEXT), null, Set.of()),
    PERSON_ADDRESS(Group.PERSON, Set.of(SHORT_TEXT, LONG_TEXT), null, Set.of()),
    TRICHO_SEX(Group.TRICHO, Set.of(SINGLE_SELECT), "sex", Set.of("F", "M")),
    TRICHO_PATTERN(Group.TRICHO, Set.of(SINGLE_SELECT, MULTI_SELECT), "pattern",
            Set.of("DIFFUSE_SHEDDING", "THINNING_TOP", "PATCHES", "SCALP_SYMPTOMS", "BREAKAGE")),
    TRICHO_ONSET(Group.TRICHO, Set.of(SINGLE_SELECT), "onset", Set.of("LT_3M", "3_12M", "GT_1Y")),
    TRICHO_DAILY_SHEDDING(Group.TRICHO, Set.of(SINGLE_SELECT), "daily_shedding", Set.of("LT_50", "50_100", "GT_100")),
    TRICHO_FAMILY_HISTORY(Group.TRICHO, Set.of(YES_NO, YES_NO_WITH_TEXT), "family_history", Set.of()),
    TRICHO_RECENT_TRIGGERS(Group.TRICHO, Set.of(MULTI_SELECT), "recent_triggers",
            Set.of("FEVER_INFECTION", "SURGERY", "WEIGHT_LOSS", "SEVERE_STRESS", "NEW_MEDICATION", "NONE")),
    TRICHO_POSTPARTUM(Group.TRICHO, Set.of(YES_NO, YES_NO_WITH_TEXT), "postpartum", Set.of()),
    TRICHO_SCALP_SYMPTOMS(Group.TRICHO, Set.of(MULTI_SELECT), "scalp_symptoms",
            Set.of("ITCH", "BURNING", "PAIN", "OILINESS", "FLAKING", "NONE")),
    TRICHO_HAIR_PROCEDURES(Group.TRICHO, Set.of(MULTI_SELECT), "hair_procedures",
            Set.of("CHEMICAL", "HEAT", "TIGHT_STYLES", "NONE")),
    TRICHO_ACNE_HIRSUTISM(Group.TRICHO, Set.of(YES_NO, YES_NO_WITH_TEXT), "acne_hirsutism", Set.of()),
    TRICHO_IRREGULAR_CYCLES(Group.TRICHO, Set.of(YES_NO, YES_NO_WITH_TEXT), "irregular_cycles", Set.of()),
    TRICHO_ANABOLIC_USE(Group.TRICHO, Set.of(YES_NO, YES_NO_WITH_TEXT), "anabolic_use", Set.of()),
    TRICHO_CHIEF_COMPLAINT(Group.TRICHO, Set.of(SHORT_TEXT, LONG_TEXT), "chief_complaint", Set.of());

    public enum Group { PERSON, TRICHO }

    private final Group group;
    private final Set<AnamnesisQuestionType> types;
    private final String quizKey;
    private final Set<String> codes;

    AnamnesisBinding(Group group, Set<AnamnesisQuestionType> types, String quizKey, Set<String> codes) {
        this.group = group; this.types = types; this.quizKey = quizKey; this.codes = codes;
    }
    public Group group() { return group; }
    public Set<AnamnesisQuestionType> types() { return types; }
    /** Chave usada pelas regras do Copiloto (formato do antigo quiz); {@code null} quando não alimenta o exame. */
    public String quizKey() { return quizKey; }
    /** Valores aceitos nas opções; vazio = sem restrição. */
    public Set<String> codes() { return codes; }
}
```

- [ ] **Step 3: Teste do serviço (falha)**

`AnamnesisServiceBindingTest` com os mocks já usados nos testes do `AnamnesisService` (copiar o setup de um teste existente do pacote). Casos:
- criar modelo com duas perguntas `PERSON_NAME` → `AnamnesisValidationException("O vínculo \"PERSON_NAME\" já está em outra pergunta.")`;
- `PERSON_BIRTH_DATE` em `SHORT_TEXT` → exceção "O vínculo ... exige pergunta do tipo ...";
- `TRICHO_ONSET` com opção `value = "foo"` → exceção listando os códigos aceitos;
- `TRICHO_*` em empresa sem módulo TRICHOLOGY → exceção "Vínculos do exame tricológico exigem o módulo Tricologia." (CRM_ADMIN passa);
- pergunta válida persiste `binding` e a resposta devolve `binding`.

- [ ] **Step 4: Implementar**

- `AnamnesisQuestion`: campo `private final AnamnesisBinding binding;` em construtor, `create(..., AnamnesisBinding binding)` e `reconstitute(..., AnamnesisBinding binding)`, getter `getBinding()`. Atualizar todas as chamadas (compilador aponta; inclui `AnamnesisDefaultTemplate`/`AnamnesisTemplateDraft` passando `null`).
- `AnamnesisQuestionRequest`: novo componente `AnamnesisBinding binding` (último).
- `AnamnesisQuestionResponse`: novo componente `AnamnesisBinding binding` (último) e `from` passa `q.getBinding()`. Snapshots antigos sem o campo desserializam com `null` (Jackson já ignora ausente).
- JPA: `@Enumerated(EnumType.STRING) @Column(name = "binding", length = 40) private AnamnesisBinding binding;` + mapeamento no `RepositoryImpl`.
- `AnamnesisService`: método privado `validateBindings(UUID companyId, List<AnamnesisSectionRequest> sections)` chamado em create/update antes de salvar:

```java
private void validateBindings(UUID companyId, List<AnamnesisSectionRequest> sections) {
    Set<AnamnesisBinding> seen = EnumSet.noneOf(AnamnesisBinding.class);
    for (AnamnesisSectionRequest s : sections == null ? List.<AnamnesisSectionRequest>of() : sections) {
        for (AnamnesisQuestionRequest q : s.questions() == null ? List.<AnamnesisQuestionRequest>of() : s.questions()) {
            AnamnesisBinding b = q.binding();
            if (b == null) continue;
            if (!seen.add(b)) throw new AnamnesisValidationException("O vínculo \"" + b + "\" já está em outra pergunta.");
            if (!b.types().contains(q.type()))
                throw new AnamnesisValidationException("O vínculo \"" + b + "\" exige pergunta do tipo " + b.types() + ".");
            if (b.group() == AnamnesisBinding.Group.TRICHO && !isPrivileged()
                    && !modulesService.isEnabled(companyId, CompanyModule.TRICHOLOGY))
                throw new AnamnesisValidationException("Vínculos do exame tricológico exigem o módulo Tricologia.");
            if (!b.codes().isEmpty() && q.options() != null) {
                for (AnamnesisOptionRequest o : q.options()) {
                    if (!b.codes().contains(o.value()))
                        throw new AnamnesisValidationException("Opção \"" + o.value() + "\" inválida para " + b
                                + ". Use: " + String.join(", ", new TreeSet<>(b.codes())) + ".");
                }
            }
        }
    }
}
```

Injetar `CompanyModulesService` no construtor do `AnamnesisService`; `isPrivileged()` igual ao de `TrichologyAccess` (CurrentUser com `isPlatformAdmin()` ou papel `CRM_ADMIN`). Conferir o nome do campo de opções no `AnamnesisOptionRequest` (`value`).

- [ ] **Step 5: Passar o comando ao usuário**

Run (usuário): `cd backend && mvn test -Dtest='AnamnesisBindingTest,AnamnesisService*Test'`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/becommerce/crm/masterdata/anamnesis backend/src/test/java/com/becommerce/crm/masterdata/anamnesis
git commit -m "feat(anamnese): vínculo das perguntas com o cadastro e o exame tricológico"
```

---

### Task 3: Leitura dos vínculos e pré-preenchimento

**Files:**
- Create: `backend/src/main/java/com/becommerce/crm/clinical/anamnesis/application/service/AnamnesisBindings.java`
- Modify: `PatientAnamnesisService.java` (`start`, `get`, `current`, conversão para resposta), `PatientAnamnesisResponse.java`, e o DTO do formulário público em `clinical/anamnesis/formrequest/PatientFormDtos.java` + `PatientFormRequestService`/`OpenFormService` (onde montam a resposta do formulário)
- Modify: `masterdata/person/PersonService.java` — `ProfileValues profileValues(UUID companyId, UUID personId)`
- Test: `backend/src/test/java/com/becommerce/crm/clinical/anamnesis/application/service/AnamnesisBindingsTest.java`

**Interfaces:**
- Produces:
  - `record PersonService.ProfileValues(String name, LocalDate birthDate, String gender, String cpf, String mobile, String email, String address)`; `ProfileValues profileValues(UUID companyId, UUID personId)` (sem permissão própria; chamado dentro de serviços que já checaram acesso).
  - `final class AnamnesisBindings`:
    - `static Map<AnamnesisBinding, UUID> questionIds(AnamnesisModelResponse snapshot)`
    - `static Map<AnamnesisBinding, Object> values(AnamnesisModelResponse snapshot, Map<UUID, PatientAnamnesisAnswer> answers)` (YES_NO → `Boolean`; `TRICHO_*` de YES_NO convertidos depois para `"true"/"false"` pela Task 6)
    - `static Map<UUID, Object> prefill(AnamnesisModelResponse snapshot, PersonService.ProfileValues profile)`
  - `PatientAnamnesisResponse.prefill` (`Map<UUID, Object>`, vazio quando concluída).

- [ ] **Step 1: Teste (falha)**

```java
class AnamnesisBindingsTest {
    // helper que monta um AnamnesisModelResponse com uma seção e perguntas (id, type, binding)
    @Test void prefillMapsProfileToBoundQuestions() {
        var snap = snapshot(q(NAME_ID, SHORT_TEXT, PERSON_NAME), q(BIRTH_ID, DATE, PERSON_BIRTH_DATE),
                            q(SEX_ID, SINGLE_SELECT, PERSON_SEX), q(FREE_ID, LONG_TEXT, null));
        var profile = new PersonService.ProfileValues("Ana", LocalDate.of(1990, 5, 10), "FEMALE", null, null, null, null);
        var prefill = AnamnesisBindings.prefill(snap, profile);
        assertEquals("Ana", prefill.get(NAME_ID));
        assertEquals("1990-05-10", prefill.get(BIRTH_ID));
        assertEquals("F", prefill.get(SEX_ID));
        assertFalse(prefill.containsKey(FREE_ID));
    }
    @Test void valuesReadAnswersByBinding() {
        var snap = snapshot(q(ONSET_ID, SINGLE_SELECT, TRICHO_ONSET), q(FAM_ID, YES_NO, TRICHO_FAMILY_HISTORY));
        var answers = Map.of(ONSET_ID, new PatientAnamnesisAnswer("LT_3M", null),
                             FAM_ID, new PatientAnamnesisAnswer(true, null));
        var values = AnamnesisBindings.values(snap, answers);
        assertEquals("LT_3M", values.get(TRICHO_ONSET));
        assertEquals(Boolean.TRUE, values.get(TRICHO_FAMILY_HISTORY));
    }
    @Test void snapshotWithoutBindingsYieldsEmpty() { /* binding null em todas → mapas vazios */ }
}
```

Conferir em `PersonService`/`PersonDtos` os valores reais de `gender` (ex.: `FEMALE`/`MALE` ou `F`/`M`) e mapear para `F`/`M` em `AnamnesisBindings.toSexCode(String)` / `fromSexCode(String)`.

- [ ] **Step 2: Implementar**

`AnamnesisBindings` (classe utilitária `final`, sem Spring):

```java
public final class AnamnesisBindings {
    private AnamnesisBindings() {}

    public static Map<AnamnesisBinding, UUID> questionIds(AnamnesisModelResponse snapshot) {
        Map<AnamnesisBinding, UUID> ids = new EnumMap<>(AnamnesisBinding.class);
        if (snapshot == null || snapshot.sections() == null) return ids;
        for (AnamnesisSectionResponse s : snapshot.sections())
            for (AnamnesisQuestionResponse q : s.questions())
                if (q.binding() != null) ids.putIfAbsent(q.binding(), q.id());
        return ids;
    }

    public static Map<AnamnesisBinding, Object> values(AnamnesisModelResponse snapshot,
                                                       Map<UUID, PatientAnamnesisAnswer> answers) {
        Map<AnamnesisBinding, Object> out = new EnumMap<>(AnamnesisBinding.class);
        questionIds(snapshot).forEach((b, id) -> {
            PatientAnamnesisAnswer a = answers == null ? null : answers.get(id);
            if (a != null && a.value() != null && !(a.value() instanceof String s && s.isBlank())) out.put(b, a.value());
        });
        return out;
    }

    public static Map<UUID, Object> prefill(AnamnesisModelResponse snapshot, PersonService.ProfileValues p) {
        Map<UUID, Object> out = new LinkedHashMap<>();
        if (p == null) return out;
        questionIds(snapshot).forEach((b, id) -> {
            Object v = switch (b) {
                case PERSON_NAME -> p.name();
                case PERSON_BIRTH_DATE -> p.birthDate() == null ? null : p.birthDate().toString();
                case PERSON_SEX, TRICHO_SEX -> toSexCode(p.gender());
                case PERSON_CPF -> p.cpf();
                case PERSON_MOBILE -> p.mobile();
                case PERSON_EMAIL -> p.email();
                case PERSON_ADDRESS -> p.address();
                default -> null;
            };
            if (v != null) out.put(id, v);
        });
        return out;
    }

    static String toSexCode(String gender) { /* mapear valores reais de persons.gender → "F"/"M"; outros → null */ }
    static String fromSexCode(String code) { /* inverso; usado pela Task 4 */ }
}
```

`PersonService.profileValues`: ler pessoa, contato MOBILE e EMAIL principais (`person_contacts` com `is_primary`/slot — conferir o nome no repositório usado por `addChannel`) e o endereço principal formatado em uma linha (`street, number - district, city/UF`). CPF com 11 dígitos sem máscara.

`PatientAnamnesisService`: em `start` e `get` (quando status ≠ COMPLETED), `prefill = AnamnesisBindings.prefill(readSnapshot(a), personService.profileValues(companyId, contactId))`; o `contactId` é o id da pessoa (contacts e persons compartilham id — V147). Concluída → `Map.of()`.

Formulário público (link e modo recepção): incluir o mesmo `prefill` no DTO que o `PublicPatientForm` recebe. **Só** os ids das perguntas vinculadas.

- [ ] **Step 3: Comando ao usuário**

Run (usuário): `cd backend && mvn test -Dtest='AnamnesisBindingsTest,PatientAnamnesisService*Test,PatientFormRequestService*Test'`
Expected: PASS (testes existentes que constroem `PatientAnamnesisResponse` precisam do novo campo — ajustar).

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/becommerce/crm/clinical/anamnesis backend/src/main/java/com/becommerce/crm/masterdata/person/PersonService.java backend/src/test/java/com/becommerce/crm/clinical/anamnesis
git commit -m "feat(anamnese): pré-preenche perguntas vinculadas com o cadastro"
```

---

### Task 4: Anamnese completa o cadastro (sync + divergências)

**Files:**
- Create: `backend/src/main/java/com/becommerce/crm/clinical/anamnesis/application/service/AnamnesisProfileSync.java`
- Create: `backend/src/main/java/com/becommerce/crm/clinical/anamnesis/divergence/ProfileDivergenceJpaEntity.java`, `ProfileDivergenceJpaRepository.java`, `ProfileDivergenceService.java`, `ProfileDivergenceController.java`
- Modify: `masterdata/person/PersonService.java` — `applyAnamnesisPatch`
- Modify: `PatientAnamnesisService.java` — chamar o sync na conclusão (`save` com `complete=true`)
- Test: `backend/src/test/java/com/becommerce/crm/clinical/anamnesis/application/service/AnamnesisProfileSyncTest.java`, `.../divergence/ProfileDivergenceServiceTest.java`

**Interfaces:**
- Consumes: `AnamnesisBindings.values`, `AnamnesisBindings.fromSexCode`, `PersonService.profileValues`.
- Produces:
  - `enum ProfileField { NAME, BIRTH_DATE, GENDER, CPF, MOBILE, EMAIL, ADDRESS }` (em `PersonService`).
  - `PersonService.applyAnamnesisPatch(UUID companyId, UUID personId, ProfileField field, String value, UUID userIdOrNull, String origin)` — aplica um campo com as validações da edição manual, grava `person_history` com `origin`; CPF válido e pessoa provisória → `provisional=false`. Lança `PersonException` em violação.
  - `AnamnesisProfileSync.sync(UUID companyId, UUID personId, UUID anamnesisId, AnamnesisModelResponse snapshot, Map<UUID, PatientAnamnesisAnswer> answers, UUID userIdOrNull)` → `SyncResult(int applied, int divergences)`.
  - REST: `GET /api/v1/companies/{companyId}/patients/{contactId}/profile-divergences` (`contact:read`), `POST .../profile-divergences/{id}/accept` e `.../dismiss` (`contact:update`), resposta `List<ProfileDivergenceView(id, field, currentValue, proposedValue, reason, createdAt)>`.

- [ ] **Step 1: Teste do sync (falha)**

```java
class AnamnesisProfileSyncTest {
    PersonService persons = mock(PersonService.class);
    ProfileDivergenceJpaRepository divergences = mock(ProfileDivergenceJpaRepository.class);
    AnamnesisProfileSync sync = new AnamnesisProfileSync(persons, divergences);

    @Test void emptyProfileFieldIsFilled() {
        when(persons.profileValues(C, P)).thenReturn(profile(null, null));
        sync.sync(C, P, A, snap(NAME), answers(NAME, "Ana Souza"), U);
        verify(persons).applyAnamnesisPatch(C, P, ProfileField.NAME, "Ana Souza", U, "Anamnese");
        verifyNoInteractions(divergences);
    }
    @Test void equalValueAfterNormalizationIsIgnored() {
        when(persons.profileValues(C, P)).thenReturn(profileMobile("34999990000"));
        sync.sync(C, P, A, snap(MOBILE), answers(MOBILE, "(34) 99999-0000"), U);
        verify(persons, never()).applyAnamnesisPatch(any(), any(), any(), any(), any(), any());
        verifyNoInteractions(divergences);
    }
    @Test void differentValueBecomesDivergence() {
        when(persons.profileValues(C, P)).thenReturn(profile("Ana", null));
        sync.sync(C, P, A, snap(NAME), answers(NAME, "Ana Souza"), U);
        verify(persons, never()).applyAnamnesisPatch(any(), any(), any(), any(), any(), any());
        verify(divergences).save(argThat(d -> d.field.equals("NAME") && d.proposedValue.equals("Ana Souza")
                && d.currentValue.equals("Ana")));
    }
    @Test void invalidCpfBecomesDivergenceWithReason() { /* "123" → reason "CPF inválido" */ }
    @Test void cpfOfAnotherPersonBecomesDivergence() {
        // applyAnamnesisPatch lança PersonException("CPF já cadastrado...") → divergência com esse reason
    }
    @Test void unexpectedErrorInOneFieldDoesNotStopOthers() { /* RuntimeException no NAME, MOBILE ainda aplicado */ }
}
```

- [ ] **Step 2: Implementar o sync**

```java
@Component
public class AnamnesisProfileSync {
    private static final Logger log = LoggerFactory.getLogger(AnamnesisProfileSync.class);
    private final PersonService persons;
    private final ProfileDivergenceJpaRepository divergences;

    public AnamnesisProfileSync(PersonService persons, ProfileDivergenceJpaRepository divergences) {
        this.persons = persons; this.divergences = divergences;
    }

    public record SyncResult(int applied, int divergences) {}

    public SyncResult sync(UUID companyId, UUID personId, UUID anamnesisId, AnamnesisModelResponse snapshot,
                           Map<UUID, PatientAnamnesisAnswer> answers, UUID userId) {
        Map<AnamnesisBinding, Object> values = AnamnesisBindings.values(snapshot, answers);
        PersonService.ProfileValues current = persons.profileValues(companyId, personId);
        String origin = userId == null ? "Anamnese (paciente)" : "Anamnese";
        int applied = 0, diverged = 0;
        for (var e : values.entrySet()) {
            PersonService.ProfileField field = fieldOf(e.getKey());
            if (field == null) continue;
            String proposed = asText(field, e.getValue());
            if (proposed == null) continue;
            String existing = currentOf(field, current);
            try {
                if (field == PersonService.ProfileField.CPF && !TaxIdValidator.isValidCpf(proposed)) {
                    diverged += diverge(companyId, personId, anamnesisId, field, existing, proposed, "CPF inválido");
                } else if (existing == null || existing.isBlank()) {
                    persons.applyAnamnesisPatch(companyId, personId, field, proposed, userId, origin);
                    applied++;
                } else if (!normalize(field, existing).equals(normalize(field, proposed))) {
                    diverged += diverge(companyId, personId, anamnesisId, field, existing, proposed, null);
                }
            } catch (PersonException ex) {
                diverged += diverge(companyId, personId, anamnesisId, field, existing, proposed, ex.getMessage());
            } catch (RuntimeException ex) {
                log.warn("Falha ao aplicar {} da anamnese {} no cadastro {}", field, anamnesisId, personId, ex);
            }
        }
        return new SyncResult(applied, diverged);
    }
    // fieldOf: PERSON_NAME→NAME, PERSON_BIRTH_DATE→BIRTH_DATE, PERSON_SEX→GENDER, PERSON_CPF→CPF,
    //          PERSON_MOBILE→MOBILE, PERSON_EMAIL→EMAIL, PERSON_ADDRESS→ADDRESS; demais → null
    // asText: trim; CPF/MOBILE só dígitos; GENDER via AnamnesisBindings.fromSexCode; BIRTH_DATE ISO yyyy-MM-dd
    // normalize: lower-case + trim + espaços simples; CPF/MOBILE só dígitos
    // diverge: não duplica PENDING igual (findByCompanyIdAndPersonIdAndFieldAndProposedValueAndStatus) → save; retorna 1 ou 0
}
```

- [ ] **Step 3: `PersonService.applyAnamnesisPatch`**

Reaproveitar as rotinas internas de `update`, `addChannel`/`updateChannel` e `addAddress` (sem exigir `CurrentUser`):
- `NAME` → `name`; `BIRTH_DATE` → `birth_or_foundation_date` (`LocalDate.parse`); `GENDER` → `gender`.
- `CPF` → `TaxIdValidator.normalize`; se `uq_persons_tax_id` já tiver outra pessoa (`check(companyId, cpf, null, personId)` não vazio) → `throw new PersonException("CPF já cadastrado em outra pessoa.")`; grava e, se `provisional`, `provisional = false`.
- `MOBILE`/`EMAIL` → canal principal (cria se não existir).
- `ADDRESS` → se não há endereço, cria um "Residencial" com o texto em `street`.
- Grava `PersonHistoryService.record(companyId, personId, userId, "UPDATE", "persons", ...)` com `origin` no detalhe (conferir assinatura).
- Sincronização com `contacts` (`PersonContactBridge`) igual à edição manual.

- [ ] **Step 4: Ligar na conclusão**

Em `PatientAnamnesisService.save`, depois de `anamnesis.complete(...)` e do `repository.save`, na mesma transação:

```java
profileSync.sync(companyId, contactId, anamnesis.getId(), snapshot, answers, actorId);
```

O caminho do formulário público (`PatientFormRequestService:228`) já chama `anamnesisUseCase.save(...)`; conferir se passa `actorId` nulo e, se passar um id técnico, trocar por `null` para o histórico sair como "Anamnese (paciente)".

- [ ] **Step 5: Divergências — serviço, controller e teste**

`ProfileDivergenceService`:
- `list(companyId, personId)` → PENDING, mais recentes primeiro.
- `accept(companyId, personId, id, CurrentUser)` → `persons.applyAnamnesisPatch(..., field, proposedValue, user.userId(), "Anamnese (aceito)")`, status `ACCEPTED`, `resolved_by/at`; `PersonException` → 409 com a mensagem.
- `dismiss(...)` → status `DISMISSED`.
- Divergência de outra pessoa/empresa → 404.

Teste: aceitar aplica e marca ACCEPTED; ignorar marca DISMISSED sem aplicar; aceitar CPF duplicado mantém PENDING e propaga erro.

Controller com `@PreAuthorize("hasAuthority('contact:read')")` no GET e `contact:update` nos POST, `@CurrentCompanyId`.

- [ ] **Step 6: Comando ao usuário**

Run (usuário): `cd backend && mvn test -Dtest='AnamnesisProfileSyncTest,ProfileDivergenceServiceTest,PatientAnamnesisService*Test,PersonService*Test'`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/becommerce/crm/clinical/anamnesis backend/src/main/java/com/becommerce/crm/masterdata/person backend/src/test/java/com/becommerce/crm/clinical/anamnesis
git commit -m "feat(anamnese): completa o cadastro da pessoa e registra divergências"
```

---

### Task 5: Visão clínica da anamnese e exame ligado

**Files:**
- Create: `backend/src/main/java/com/becommerce/crm/clinical/trichology/application/port/AnamnesisClinicalView.java`
- Create: `backend/src/main/java/com/becommerce/crm/clinical/anamnesis/application/service/AnamnesisClinicalViewImpl.java`
- Modify: domínio do assessment (`TrichologyAssessment` estado + `anamnesisId`), `TrichologyRepositoryAdapter.java` (`e.anamnesisId`), entidade JPA do assessment, `AssessmentService.open` e `detail`, `TrichologyDtos.AssessmentDetail` (`anamnesisId`, `anamnesisCompletedAt`)
- Modify: `PatientAnamnesisService.save` — ao concluir, ligar exames abertos sem anamnese
- Test: `backend/src/test/java/com/becommerce/crm/clinical/anamnesis/application/service/AnamnesisClinicalViewImplTest.java`, ajuste em testes de `AssessmentService`

**Interfaces:**
- Produces:

```java
public interface AnamnesisClinicalView {
    record ClinicalAnamnesis(UUID anamnesisId, Instant completedAt, Map<String, Object> quizLike,
                             List<QuestionAnswer> answers) {}
    record QuestionAnswer(String section, String question, String answer) {}
    Optional<ClinicalAnamnesis> latestCompleted(UUID companyId, UUID contactId);
    Optional<ClinicalAnamnesis> byId(UUID companyId, UUID contactId, UUID anamnesisId);
}
```

  - `quizLike`: chaves `AnamnesisBinding.quizKey()` → valor no formato do antigo quiz: SELECT → `String`, MULTI → `List<String>`, YES_NO → `"true"`/`"false"` (as regras comparam strings), texto → `String`.
  - `answers`: todas as perguntas **não profissionais e sem vínculo `PERSON_*`**, respostas legíveis (rótulo da opção, "Sim"/"Não", complemento entre parênteses). Perguntas sem resposta ficam fora.
  - `TrichologyAssessment.state().anamnesisId()` (UUID, nullable).

- [ ] **Step 1: Teste (falha)**

```java
class AnamnesisClinicalViewImplTest {
    @Test void quizLikeUsesQuizKeysAndStringBooleans() {
        // snapshot com TRICHO_ONSET (SINGLE) "LT_3M", TRICHO_FAMILY_HISTORY (YES_NO) true,
        // TRICHO_RECENT_TRIGGERS (MULTI) ["SURGERY"], PERSON_SEX "F"
        // → quizLike = {onset=LT_3M, family_history="true", recent_triggers=[SURGERY], sex=F}
    }
    @Test void answersExcludePersonBindingsAndProfessionalSections() {
        // PERSON_NAME "Ana", pergunta livre "Fuma?" false, seção profissional → answers só tem "Fuma?" = "Não"
    }
    @Test void selectAnswersUseOptionLabels() { /* "LT_3M" → "Menos de 3 meses" */ }
    @Test void latestCompletedIgnoresDrafts() { /* repo devolve só COMPLETED mais recente */ }
}
```

- [ ] **Step 2: Implementar**

`AnamnesisClinicalViewImpl` usa `PatientAnamnesisRepository` (o mesmo usado por `current(...)`: `findFirstBy...Status...OrderByCompletedAtDesc` — conferir nome), `readSnapshot` (extrair para helper compartilhado se for privado) e `AnamnesisBindings.values`. Executa com `TenantContext.setCompanyId` / `clear` como os demais serviços.

`AssessmentService.open(...)`: depois de criar, `clinicalView.latestCompleted(companyId, contactId).ifPresent(a -> state.anamnesisId(a.anamnesisId()))`; se a queixa vier vazia e `quizLike.chief_complaint` existir, usar como `chiefComplaint`.

`PatientAnamnesisService.save` (ao concluir): `assessmentLinker.linkOpenAssessments(companyId, contactId, anamnesisId)` — método novo em `AssessmentService` (via porta `TrichologyAnamnesisLinker` no pacote trichology para evitar ciclo) que liga exames com status ≠ PUBLISHED/CANCELLED e `anamnesis_id` nulo. Só roda com `app.trichology.enabled` e módulo ativo; senão no-op.

- [ ] **Step 3: Comando ao usuário**

Run (usuário): `cd backend && mvn test -Dtest='AnamnesisClinicalViewImplTest,AssessmentService*Test'`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/becommerce/crm/clinical backend/src/test/java/com/becommerce/crm/clinical
git commit -m "feat(tricologia): exame ligado à anamnese concluída mais recente"
```

---

### Task 6: Copiloto, laboratório, imagens e laudo usam a anamnese; quiz removido (backend)

**Files:**
- Modify: `clinical/trichology/application/service/ExamCopilotService.java`, `AssessmentService.java:~343` (sexo do laboratório), `ImageAnalysisService.java:~183`, `ReportService.java:~376`
- Modify: `clinical/trichology/web/AssessmentController.java` (remove `quiz-definition`, `quiz-link`, `PUT quiz`), `PublicTrichologyController.java` (remove quiz; mantém `verify`), `SecurityConfig` se citar rota do quiz
- Delete: `clinical/trichology/domain/QuizEngine.java` e DTOs `QuizDefinition`/`QuizLinkResponse`/requests do quiz
- Test: `ExamCopilotServiceTest` (criar ou ajustar), `ReportServiceAnamnesisTest`

**Interfaces:**
- Consumes: `AnamnesisClinicalView.byId`, `ClinicalAnamnesis.quizLike()`, `.answers()`.
- Produces: método `Map<String, Object> clinicalAnswers(UUID companyId, TrichologyAssessment a)` em `AssessmentService` — `anamnesisId` presente → `quizLike` da anamnese; senão → `quiz_answers` antigo (ou `Map.of()`). Todos os consumidores passam a chamá-lo.

- [ ] **Step 1: Testes (falham)**

```java
@Test void hypothesesUseAnamnesisAnswers() {
    // assessment com anamnesisId; clinicalView devolve quizLike {pattern=DIFFUSE_SHEDDING, daily_shedding=GT_100}
    // → hypotheses contém TELOGEN_EFFLUVIUM com suporte "Anamnese: pattern = DIFFUSE_SHEDDING"
}
@Test void legacyAssessmentStillUsesQuizAnswers() { /* anamnesisId null, quiz_answers preenchido → mesma hipótese */ }
@Test void pendingSaysNoAnamnesisInsteadOfQuiz() {
    // sem anamnesisId e sem quiz → pendência "Sem anamnese concluída."
}
@Test void reportInputHasAnamnesisWithoutPersonData() {
    // ReportService monta input: contém "anamnesis" (lista question/answer) e NÃO contém "quiz";
    // nenhum valor do input contém o nome/CPF/telefone do paciente usados nas respostas PERSON_*
}
```

- [ ] **Step 2: Implementar**

- `ExamCopilotService.hypotheses(a, linked)` → receber `Map<String, Object> answers` como parâmetro (vindo de `clinicalAnswers`) em vez de ler `a.state().quizAnswers()`. Texto do suporte: `"Anamnese: " + key + " = " + value`.
- Pendências: troca `"Quiz do paciente não respondido."` por `"Sem anamnese concluída."` quando `anamnesisId == null && quizAnswers == null`.
- `AssessmentService:~343`: `String sex = (String) clinicalAnswers(companyId, a).get("sex");`
- `ImageAnalysisService:~183`: passar `clinicalAnswers(...)` no lugar de `quizAnswers()`.
- `ReportService:~376`: `input.put("anamnesis", view.answers())` quando houver anamnese ligada; exames antigos mantêm `input.put("quiz", ...)`. Atualizar o prompt do relatório (mesmo arquivo ou recurso de prompt) trocando a menção a "quiz" por "anamnese (perguntas e respostas)"; incrementar `PROMPT_VERSION`.
- Remover endpoints, `QuizEngine` e DTOs do quiz; apagar testes que só testavam o quiz. Manter `quiz_answers`, `quiz_completed_at` e afins no domínio/JPA (leitura de exames antigos).

- [ ] **Step 3: Comando ao usuário**

Run (usuário): `cd backend && mvn test -Dtest='ExamCopilotService*Test,ReportService*Test,AssessmentService*Test,ImageAnalysisService*Test'` e depois `cd backend && mvn test` (suíte completa, pela remoção de classes).
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add -A backend
git commit -m "feat(tricologia): Copiloto e laudo usam a anamnese; quiz removido"
```

---

### Task 7: Frontend — vínculo no editor de modelos

**Files:**
- Modify: `frontend-refine/src/features/masterdata/anamnesis/components/AnamnesisModelEditor.tsx`, tipos/schemas do modelo em `features/masterdata/anamnesis/types` e `.../schemas` (onde estiverem `complementTrigger`)
- Create: `frontend-refine/src/features/masterdata/anamnesis/utils/bindings.ts`
- Test: `frontend-refine/src/__tests__/features/masterdata/anamnesis/bindings.test.ts`, ajuste do teste do editor se existir

**Interfaces:**
- Produces (`bindings.ts`):

```ts
export type AnamnesisBinding =
  | "PERSON_NAME" | "PERSON_BIRTH_DATE" | "PERSON_SEX" | "PERSON_CPF" | "PERSON_MOBILE" | "PERSON_EMAIL" | "PERSON_ADDRESS"
  | "TRICHO_SEX" | "TRICHO_PATTERN" | "TRICHO_ONSET" | "TRICHO_DAILY_SHEDDING" | "TRICHO_FAMILY_HISTORY"
  | "TRICHO_RECENT_TRIGGERS" | "TRICHO_POSTPARTUM" | "TRICHO_SCALP_SYMPTOMS" | "TRICHO_HAIR_PROCEDURES"
  | "TRICHO_ACNE_HIRSUTISM" | "TRICHO_IRREGULAR_CYCLES" | "TRICHO_ANABOLIC_USE" | "TRICHO_CHIEF_COMPLAINT";

export type BindingInfo = { value: AnamnesisBinding; label: string; group: "PERSON" | "TRICHO";
  types: AnamnesisQuestionType[]; codes?: { value: string; label: string }[] };

export const BINDINGS: BindingInfo[];               // mesma lista do backend, rótulos pt-BR
export function bindingsFor(type: AnamnesisQuestionType, trichologyEnabled: boolean): BindingInfo[];
export function usedBindings(sections: { questions: { binding?: AnamnesisBinding | null }[] }[]): Set<AnamnesisBinding>;
```

Rótulos: Cadastro → "Nome", "Data de nascimento", "Sexo", "CPF", "Celular", "E-mail", "Endereço"; Exame → "Sexo (exame)", "Padrão da queda", "Início", "Queda diária", "Histórico familiar", "Gatilhos recentes", "Pós-parto", "Sintomas do couro cabeludo", "Procedimentos no cabelo", "Acne ou hirsutismo", "Ciclo irregular", "Uso de anabolizantes", "Queixa principal". `codes` com os mesmos valores/rótulos da V163.

- [ ] **Step 1: Teste (falha)**

```ts
import { describe, expect, it } from "vitest";
import { bindingsFor, usedBindings } from "@/features/masterdata/anamnesis/utils/bindings";

describe("bindingsFor", () => {
  it("só oferece vínculos compatíveis com o tipo", () => {
    expect(bindingsFor("DATE", true).map((b) => b.value)).toEqual(["PERSON_BIRTH_DATE"]);
  });
  it("esconde vínculos do exame sem o módulo Tricologia", () => {
    expect(bindingsFor("SINGLE_SELECT", false).every((b) => b.group === "PERSON")).toBe(true);
  });
});
describe("usedBindings", () => {
  it("junta os vínculos de todas as seções", () => {
    const used = usedBindings([{ questions: [{ binding: "PERSON_NAME" }] }, { questions: [{ binding: null }] }]);
    expect([...used]).toEqual(["PERSON_NAME"]);
  });
});
```

Run: `npx vitest run src/__tests__/features/masterdata/anamnesis/bindings.test.ts` → FAIL (módulo não existe).

- [ ] **Step 2: Implementar `bindings.ts` e o editor**

No editor, em cada pergunta, abaixo do tipo: `Select` "Vincular a" com opção "Nenhum" + grupos "Cadastro" e "Exame tricológico" (`SelectGroup`/`SelectLabel`), itens já usados em outra pergunta desabilitados. Ao escolher vínculo com `codes`, se a pergunta não tiver opções, preencher as opções com `codes`; se tiver, mostrar abaixo "Valores esperados: LT_3M, 3_12M, GT_1Y" e destacar em vermelho as opções fora da lista. Trocar o tipo da pergunta para um incompatível limpa o vínculo. Selo "Cadastro" ou "Exame" na pergunta vinculada. `trichologyEnabled` via `useCompanyModules(companyId)` (`enabledModules.includes("TRICHOLOGY")`). Incluir `binding` no payload de salvar e no tipo `AnamnesisQuestion`.

- [ ] **Step 3: Rodar testes e Prettier**

Run: `npx vitest run src/__tests__/features/masterdata/anamnesis` e `npx prettier --check --end-of-line auto <arquivos alterados>`
Expected: PASS / "All matched files use Prettier code style!"

- [ ] **Step 4: Commit**

```bash
git add frontend-refine/src/features/masterdata/anamnesis frontend-refine/src/__tests__/features/masterdata/anamnesis
git commit -m "feat(anamnese): seletor de vínculo no editor de modelos"
```

---

### Task 8: Frontend — formulário pré-preenchido e divergências

**Files:**
- Modify: `frontend-refine/src/features/clinical/anamnesis/components/AnamnesisFields.tsx` e o formulário que inicializa os valores (`AnamnesisAnswerForm` e `PublicPatientForm` — conferir nomes pelos testes em `src/__tests__/features/clinical/anamnesis/`), tipos da anamnese (`prefill`)
- Create: `frontend-refine/src/features/clinical/anamnesis/components/ProfileDivergenceBanner.tsx`, `.../services/profileDivergence.service.ts`, `.../hooks/useProfileDivergences.ts`
- Modify: página/aba do paciente que mostra o cabeçalho (ex.: `app/(dashboard)/patients/[id]/layout.tsx`) para renderizar o banner
- Test: `src/__tests__/features/clinical/anamnesis/AnamnesisAnswerForm.test.tsx` (novo caso), `src/__tests__/features/clinical/anamnesis/ProfileDivergenceBanner.test.tsx`

**Interfaces:**
- Consumes: `PatientAnamnesisResponse.prefill: Record<string, unknown>`; endpoints da Task 4.
- Produces: `useProfileDivergences(companyId, contactId)`, `useResolveDivergence(companyId, contactId)` (`{ id, action: "accept" | "dismiss" }`).

- [ ] **Step 1: Testes (falham)**

```tsx
it("inicia as perguntas vinculadas com o prefill", () => {
  render(<AnamnesisAnswerForm anamnesis={{ ...draft, prefill: { [nameId]: "Ana" }, answers: {} }} /* demais props */ />);
  expect(screen.getByDisplayValue("Ana")).toBeInTheDocument();
});
it("resposta já salva vence o prefill", () => {
  render(<AnamnesisAnswerForm anamnesis={{ ...draft, prefill: { [nameId]: "Ana" }, answers: { [nameId]: { value: "Ana S." } } }} />);
  expect(screen.getByDisplayValue("Ana S.")).toBeInTheDocument();
});
```

```tsx
it("lista divergências e aceita", async () => {
  // mock do service com [{ id: "d1", field: "NAME", currentValue: "Ana", proposedValue: "Ana Souza" }]
  // render banner → texto "Dados da anamnese divergentes do cadastro (1)"; clicar "Aceitar" chama accept("d1")
});
```

- [ ] **Step 2: Implementar**

- Valor inicial de cada pergunta: `answers[id]?.value ?? prefill?.[id] ?? vazio`. Vale para modo recepção e link.
- Banner: some quando lista vazia; uma linha por campo "Nome: no cadastro **Ana** · na anamnese **Ana Souza**" com botões "Aceitar" e "Ignorar" (visíveis só com `contact:update`); motivo (`reason`) abaixo em texto pequeno. Rótulos dos campos: NAME "Nome", BIRTH_DATE "Nascimento", GENDER "Sexo", CPF "CPF", MOBILE "Celular", EMAIL "E-mail", ADDRESS "Endereço".
- Ao aceitar/ignorar e ao concluir uma anamnese: invalidar `["profile-divergences", companyId, contactId]`, a query da pessoa (`["person", ...]`), do paciente (`["patient", ...]`), da anamnese atual e da lista de pacientes (conferir as chaves usadas nos hooks existentes).

- [ ] **Step 3: Testes e Prettier**

Run: `npx vitest run src/__tests__/features/clinical/anamnesis` e Prettier nos arquivos alterados.
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add frontend-refine/src
git commit -m "feat(anamnese): formulário pré-preenchido e divergências com o cadastro"
```

---

### Task 9: Frontend — exame sem quiz

**Files:**
- Modify: `frontend-refine/src/features/clinical/trichology/components/AssessmentSteps.tsx`, `AssessmentWorkspace.tsx`, `CopilotPanel.tsx`, `PublicPages.tsx`, `services/trichology.service.ts`, `hooks/useTrichology.ts`, `types/trichology.types.ts`
- Delete: `frontend-refine/src/features/clinical/trichology/components/QuizForm.tsx`, `frontend-refine/src/app/(public)/quiz-capilar/[token]/page.tsx`
- Modify: `frontend-refine/src/lib/middleware-auth.ts` (rota pública do quiz)
- Test: `frontend-refine/src/features/clinical/trichology/__tests__/trichology-utils.test.ts` (ajustar referências ao quiz)

**Interfaces:**
- Consumes: `AssessmentDetail.anamnesisId`, `anamnesisCompletedAt` (Task 5).

- [ ] **Step 1: Remover aba e chamadas**

- Abas: "Captura", "Clínico", "Laboratório", "Relatório" (sem "Quiz"); aba inicial "Captura".
- No topo do exame, abaixo da queixa: "Anamnese de 28/11/2026" com link para `/patients/{id}/anamnese`, ou "Sem anamnese concluída" + botão "Enviar anamnese" que leva para `/patients/{id}/anamnese`.
- `CopilotPanel`: a pendência vem do backend; nada a mudar além de remover atalhos para o quiz, se houver.
- Remover do service/hook/types: `quizDefinition`, `createQuizLink`, `saveQuiz` e tipos relacionados. Remover a parte de quiz de `PublicPages.tsx` (manter a verificação do relatório) e a rota `quiz-capilar` do middleware.

- [ ] **Step 2: Typecheck, testes e Prettier**

Run: `npx tsc --noEmit -p .` · `npx vitest run src/features/clinical/trichology` · Prettier nos alterados.
Expected: sem erros; PASS.

- [ ] **Step 3: Commit**

```bash
git add -A frontend-refine/src
git commit -m "feat(tricologia): exame usa a anamnese; aba e link do quiz removidos"
```

---

### Task 10: Ajustes menores

**Files:**
- Modify: `frontend-refine/src/features/clinical/budgets/utils/blocks.ts:6`
- Modify: `backend/src/main/java/com/becommerce/crm/shared/web/handler/GlobalExceptionHandler.java:1202`
- Modify: `backend/src/test/java/com/becommerce/crm/shared/web/handler/GlobalExceptionHandlerTest.java:91`
- Modify: fallback de erro do frontend, se houver mensagem genérica em inglês (`git grep -n "unexpected" frontend-refine/src`)

- [ ] **Step 1: Teste do handler (falha)**

Em `GlobalExceptionHandlerTest:91`: `assertEquals("Não foi possível concluir a ação. Tente novamente.", response.getBody().get("message"));`

- [ ] **Step 2: Implementar**

- `GlobalExceptionHandler:1202`: `"message", "Não foi possível concluir a ação. Tente novamente.",`
- `blocks.ts:6`: `{ key: "dentists", label: "Profissionais" },` (a chave `dentists` continua, para não quebrar links de impressão).

- [ ] **Step 3: Comandos**

Run (usuário): `cd backend && mvn test -Dtest=GlobalExceptionHandlerTest` → PASS.
Run: Prettier em `blocks.ts`.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/becommerce/crm/shared/web/handler/GlobalExceptionHandler.java backend/src/test/java/com/becommerce/crm/shared/web/handler/GlobalExceptionHandlerTest.java frontend-refine/src/features/clinical/budgets/utils/blocks.ts
git commit -m "fix: mensagem de erro em português e rótulo Profissionais na impressão"
```

---

### Task 11: Verificação final

- [ ] **Step 1:** Usuário roda `cd backend && mvn verify` (unit + IT). Esperado: verde.
- [ ] **Step 2:** `cd frontend-refine && npx tsc --noEmit -p . && npx vitest run && npx eslint src --max-warnings=0` (com dependências instaladas). Esperado: verde.
- [ ] **Step 3:** Prettier em todos os arquivos do frontend alterados na branch: `git diff --name-only origin/crm-improvements-deploy-phase -- frontend-refine | sed 's|frontend-refine/||' | xargs npx prettier --check --end-of-line auto`.
- [ ] **Step 4:** Atualizar o painel de status e aguardar o usuário pedir push/PR. Após o deploy (`backend frontend`), refazer em produção com o "Paciente Teste Fluxo": anamnese pré-preenchida, envio, divergência aparece, exame mostra a anamnese, Copiloto com hipóteses, laudo sem dados pessoais.
