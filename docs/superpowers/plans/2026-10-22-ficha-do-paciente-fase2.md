# Ficha do Paciente (Fase 2) Implementation Plan

> **Para o agente executor (opencode):** este documento é uma ORDEM, não uma sugestão. Execute task a task, **na ordem**, marcando cada checkbox (`- [x]` → `- [x]`). Ao fim de cada task rode a verificação indicada; só faça o commit se ela passar. **Um commit por task.** Não faça push, merge, rebase nem PR. Não altere nada fora dos arquivos listados na task, exceto quando a task mandar. Se algo do plano não bater com o código real (nome de classe, método, assinatura), **pare, não invente**: escreva a divergência no fim deste arquivo, na seção "Divergências encontradas", e siga a solução mais próxima do padrão existente citado na task.

**Goal:** Completar a ficha do paciente (`/patients/[id]`) com quatro abas clínicas novas: **Anamnese** (respostas sobre um snapshot do modelo + selo de alerta no cabeçalho), **Evoluções** (rascunho → finalizada imutável), **Documentos** (receita/atestado/termo a partir de modelos, com PDF) e **foto do paciente**. Também substitui a tela mock `/settings/documents` (hoje só estado local) por modelos de documento persistidos.

**Specs anteriores (leia antes, só para contexto):** `docs/superpowers/specs/2026-10-12-ficha-do-paciente-design.md` (seção "Fase 2") e `docs/superpowers/plans/2026-10-12-ficha-do-paciente.md` (Fase 1, mesmo estilo deste plano).

**Tech Stack:** Spring Boot hexagonal (`backend/`), Next.js App Router + TanStack Query + shadcn/ui (`frontend-refine/`), PostgreSQL + Flyway, OpenPDF (`com.lowagie`, já no `pom.xml`), JUnit 5 + Mockito, Vitest.

---

## Global Constraints (valem para TODAS as tasks)

1. **Branch e pasta:** trabalhe **somente** no worktree `C:\Users\ghilh\Desktop\PROJETO\crm\crm\.worktrees\pacientes-fase2`, branch `feature/pacientes-fase2` (já criada a partir de `origin/crm-improvements-deploy-phase` e contendo este plano). Confirme com `git branch --show-current` antes da Task 1. Não troque de branch.
2. **Migration única = `V105__patient_clinical_record.sql`.** A última existente é `V104__patients_module.sql`. Antes de criar, rode `ls backend/src/main/resources/db/migration | tail -3`; se já existir uma `V105`, use o próximo número livre e anote em "Divergências encontradas". **Nunca editar migrations antigas.** Sem Docker local: a migration só roda de verdade no E2E do CI, então revise à mão: nomes de tabela/índice/constraint não podem colidir com os existentes (`git grep -n "CREATE TABLE <nome>\|<nome_do_indice>" backend/src/main/resources/db/migration` deve voltar vazio antes de criar).
3. **Padrão RLS obrigatório em toda tabela nova** (copie de `V097__patient_record.sql`): coluna `company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE`, `ENABLE` + `FORCE ROW LEVEL SECURITY`, `CREATE POLICY tenant_isolation_policy ... USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id())`, `GRANT SELECT, INSERT, UPDATE, DELETE ON <tabela> TO crm_app`.
4. **Backend segue exatamente o pacote `masterdata/contact/note`** como modelo (leia todos os arquivos dele antes da Task 2):
   - `domain/` (classe de domínio com `create(...)` + `reconstitute(...)`, validação lançando `ContactValidationException`, exceção `XxxNotFoundException`),
   - `application/dto`, `application/port/in` (UseCase), `application/port/out` (Repository), `application/service`,
   - `infrastructure/persistence` (`XxxJpaEntity`, `XxxJpaRepository extends JpaRepository`, `XxxRepositoryImpl`),
   - `web/XxxController`.
   - Serviços: `TenantContext.setCompanyId(companyId)` no início de cada método e `TenantContext.clear()` no `finally`; `requireOwnedContact(companyId, contactId)` igual ao `ContactNoteService`; auditoria com `TenantAuditRecorder.record(companyId, AuditAction.X, AuditModule.CONTACTS, RESOURCE, id, "mensagem pt-BR", actorId, null)`.
   - Controllers: só a rota `/api/v1/companies/{companyId}/patients/{contactId}/...` (NÃO criar alias `/contacts/...`, a Fase 2 é só do módulo Pacientes); `@CurrentCompanyId("Você só pode acessar ... da sua própria empresa.")`; `@PreAuthorize("hasAuthority('contact:read')")` para leitura e `'contact:update'` para escrita; admin detectado com o mesmo `isAdmin(principal)` do `ContactNoteController`.
   - Colunas JSONB mapeadas com `@JdbcTypeCode(SqlTypes.JSON)` (exemplo: `analytics/audit/infrastructure/persistence/AuditLogJpaEntity.java`).
   - **Nenhuma permissão nova.** Pacientes: `contact:read` / `contact:update`. Modelos de documento (configurações): `anamnesis:read` / `anamnesis:manage` (mesma área "Configurações → Documentos").
5. **Pacote novo do backend:** `com.becommerce.crm.clinical` (não existe ainda), com subpacotes `anamnesis`, `evolution`, `document`, `documenttemplate`. Cada um com a estrutura do item 4. A foto fica em `masterdata/contact` (é um campo do contato).
6. **Módulo da empresa:** as rotas `/patients/**` já são barradas pelo `ModuleGuardInterceptor` quando PATIENTS está desligado. A rota nova `/document-templates/**` precisa ser adicionada ao mapa `RESOURCE_MODULES` (Task 6).
7. **Frontend:** código novo em `frontend-refine/src/features/clinical/` (ao lado de `patients/`), seguindo `features/clinical/patients` (services com `api` de `@/lib/api`, hooks TanStack Query com `useMutationDefaults`, componentes shadcn de `@/components/ui/*`). Textos em pt-BR. Sem cores hex: só tokens do tema (`text-muted-foreground`, `bg-card`, `border`, `text-destructive`, etc.).
8. **Estilo:** Checkstyle do backend até 200 colunas. Prettier: `printWidth 100`, aspas duplas, `trailingComma all`. **Todo arquivo do frontend alterado** passa em `npx prettier --check --end-of-line auto <arquivos>`.
9. **Verificações:**
   - Backend: `cd backend && mvn -q test` (ITs com Testcontainers rodam só no CI).
   - Frontend: `cd frontend-refine && npm run lint && npx tsc --noEmit && npx prettier --check --end-of-line auto <arquivos alterados> && npx vitest run --pool=threads`. Em testes, **sem** `user-event`/jest-dom: use `fireEvent` e `screen.getByText(...)`.
10. **Commits:** Conventional em pt-BR (`feat(pacientes): ...`, `test(pacientes): ...`), terminando com uma linha em branco e `Co-Authored-By: opencode <noreply@opencode.ai>`.

---

## Modelo de dados (referência para as tasks)

| Tabela | Para quê | Regras |
|---|---|---|
| `patient_anamneses` | Uma anamnese preenchida de um paciente | Guarda **cópia (snapshot) do modelo** no momento do preenchimento + respostas. Editar o modelo depois NÃO altera anamneses antigas. `DRAFT` editável pelo autor/admin; `COMPLETED` imutável. Várias por paciente (histórico); a "atual" é a `COMPLETED` mais recente. |
| `patient_evolutions` | Evolução clínica (o que foi feito na consulta) | `DRAFT` editável/excluível pelo autor (ou admin). `FINALIZED` **imutável para todos, inclusive admin** — reforçado por trigger no banco. |
| `document_templates` | Modelos de receita/atestado/termo da empresa | CRUD em Configurações → Documentos. Corpo com variáveis `{{...}}`. |
| `patient_documents` | Documento emitido para o paciente | Texto **já renderizado** gravado no momento da emissão (não muda se o modelo mudar). Imutável. Só admin exclui. PDF gerado sob demanda. |
| `contacts.photo_storage_id` | Foto do paciente | Aponta para `storage_objects` (arquivo do paciente). |

---

## Task 1: Migration `V105__patient_clinical_record.sql`

**Files:** Create `backend/src/main/resources/db/migration/V105__patient_clinical_record.sql`

- [x] Cabeçalho de comentário no estilo da V097 explicando as 4 tabelas, a coluna de foto, o trigger de imutabilidade e que as permissões são reaproveitadas (`contact:read/update`, `anamnesis:read/manage`).
- [x] Escreva exatamente este SQL (pode ajustar só comentários):

```sql
-- ===========================================================================
-- 1. patient_anamneses — anamnese preenchida (snapshot do modelo + respostas)
-- ===========================================================================
CREATE TABLE patient_anamneses (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id     UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    contact_id     UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    model_id       UUID REFERENCES anamnesis_models(id) ON DELETE SET NULL,
    model_name     VARCHAR(120) NOT NULL,
    model_version  INT NOT NULL,
    snapshot       JSONB NOT NULL,
    answers        JSONB NOT NULL DEFAULT '{}'::jsonb,
    alerts         JSONB NOT NULL DEFAULT '[]'::jsonb,
    status         VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by     UUID REFERENCES users(id) ON DELETE SET NULL,
    completed_by   UUID REFERENCES users(id) ON DELETE SET NULL,
    completed_at   TIMESTAMP,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_patient_anamneses_status CHECK (status IN ('DRAFT', 'COMPLETED')),
    CONSTRAINT chk_patient_anamneses_completed
        CHECK ((status = 'COMPLETED') = (completed_at IS NOT NULL))
);
CREATE INDEX idx_patient_anamneses_contact
    ON patient_anamneses (company_id, contact_id, created_at DESC);

-- ===========================================================================
-- 2. patient_evolutions — evolução clínica (rascunho -> finalizada imutável)
-- ===========================================================================
CREATE TABLE patient_evolutions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    contact_id      UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    appointment_id  UUID REFERENCES appointments(id) ON DELETE SET NULL,
    body            TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    finalized_by    UUID REFERENCES users(id) ON DELETE SET NULL,
    finalized_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_patient_evolutions_status CHECK (status IN ('DRAFT', 'FINALIZED')),
    CONSTRAINT chk_patient_evolutions_body CHECK (length(trim(body)) > 0),
    CONSTRAINT chk_patient_evolutions_finalized
        CHECK ((status = 'FINALIZED') = (finalized_at IS NOT NULL))
);
CREATE INDEX idx_patient_evolutions_contact
    ON patient_evolutions (company_id, contact_id, created_at DESC);

-- Evolução finalizada não pode ser alterada nem excluída (prontuário), nem por admin.
-- Exceção: exclusão em cascata do paciente/empresa (pg_trigger_depth() > 1).
CREATE OR REPLACE FUNCTION app.prevent_finalized_evolution_change() RETURNS trigger AS $$
BEGIN
    IF OLD.status = 'FINALIZED' AND pg_trigger_depth() = 1 THEN
        IF TG_OP = 'DELETE' THEN
            RAISE EXCEPTION 'Evolução finalizada não pode ser excluída.' USING ERRCODE = 'check_violation';
        END IF;
        RAISE EXCEPTION 'Evolução finalizada não pode ser alterada.' USING ERRCODE = 'check_violation';
    END IF;
    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_patient_evolutions_immutable
    BEFORE UPDATE OR DELETE ON patient_evolutions
    FOR EACH ROW EXECUTE FUNCTION app.prevent_finalized_evolution_change();

-- ===========================================================================
-- 3. document_templates — modelos de documento da empresa
-- ===========================================================================
CREATE TABLE document_templates (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    type        VARCHAR(20) NOT NULL,
    title       VARCHAR(160) NOT NULL,
    body        TEXT NOT NULL,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_by  UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_document_templates_type
        CHECK (type IN ('PRESCRIPTION', 'CERTIFICATE', 'CONSENT', 'OTHER')),
    CONSTRAINT chk_document_templates_body CHECK (length(trim(body)) > 0)
);
CREATE INDEX idx_document_templates_company ON document_templates (company_id, type, is_active);

-- ===========================================================================
-- 4. patient_documents — documento emitido (texto final congelado)
-- ===========================================================================
CREATE TABLE patient_documents (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id             UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    contact_id             UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    template_id            UUID REFERENCES document_templates(id) ON DELETE SET NULL,
    type                   VARCHAR(20) NOT NULL,
    title                  VARCHAR(160) NOT NULL,
    body                   TEXT NOT NULL,
    professional_name      VARCHAR(160) NOT NULL,
    professional_registry  VARCHAR(40),
    issued_by              UUID REFERENCES users(id) ON DELETE SET NULL,
    issued_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_patient_documents_type
        CHECK (type IN ('PRESCRIPTION', 'CERTIFICATE', 'CONSENT', 'OTHER')),
    CONSTRAINT chk_patient_documents_body CHECK (length(trim(body)) > 0)
);
CREATE INDEX idx_patient_documents_contact
    ON patient_documents (company_id, contact_id, issued_at DESC);

-- ===========================================================================
-- 5. contacts.photo_storage_id — foto do paciente
-- ===========================================================================
ALTER TABLE contacts
    ADD COLUMN photo_storage_id UUID REFERENCES storage_objects(id) ON DELETE SET NULL;

-- ===========================================================================
-- RLS FORCE + GRANT
-- ===========================================================================
DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['patient_anamneses', 'patient_evolutions',
                             'document_templates', 'patient_documents']
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
        EXECUTE format('CREATE POLICY tenant_isolation_policy ON %I '
                    || 'USING (company_id = app.current_tenant_id()) '
                    || 'WITH CHECK (company_id = app.current_tenant_id())', t);
        EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON %I TO crm_app', t);
    END LOOP;
END $$;
```

- [x] **Antes de commitar, confirme** (cada comando deve achar a referência esperada):
  - `git grep -n "CREATE SCHEMA.*app\|FUNCTION app.current_tenant_id" backend/src/main/resources/db/migration` → o schema `app` existe. Se a função `app.current_tenant_id` estiver em outro schema, crie a função do trigger no mesmo schema e anote a divergência.
  - `git grep -n "CREATE TABLE appointments\|CREATE TABLE storage_objects" backend/src/main/resources/db/migration` → as duas tabelas referenciadas existem.
  - `git grep -n "patient_anamneses\|patient_evolutions\|document_templates\|patient_documents\|photo_storage_id" backend/src/main/resources/db/migration` → só aparece na V105.
- [x] Commit: `feat(pacientes): migration V105 com anamnese, evoluções, documentos e foto`

---

## Task 2: Backend — Modelos de documento (`clinical/documenttemplate`)

**Files (criar):**
- `backend/src/main/java/com/becommerce/crm/clinical/documenttemplate/domain/DocumentType.java` — enum `PRESCRIPTION, CERTIFICATE, CONSENT, OTHER`.
- `.../documenttemplate/domain/DocumentTemplate.java`
- `.../documenttemplate/domain/exception/DocumentTemplateNotFoundException.java`
- `.../documenttemplate/application/dto/DocumentTemplateRequest.java` — record `(@NotNull DocumentType type, @NotBlank @Size(max=160) String title, @NotBlank @Size(max=20000) String body, Boolean active)`.
- `.../documenttemplate/application/dto/DocumentTemplateResponse.java` — record `(UUID id, DocumentType type, String title, String body, boolean active, LocalDateTime createdAt, LocalDateTime updatedAt)`.
- `.../documenttemplate/application/port/in/DocumentTemplateUseCase.java`
- `.../documenttemplate/application/port/out/DocumentTemplateRepository.java`
- `.../documenttemplate/application/service/DocumentTemplateService.java`
- `.../documenttemplate/infrastructure/persistence/{DocumentTemplateJpaEntity, DocumentTemplateJpaRepository, DocumentTemplateRepositoryImpl}.java`
- `.../documenttemplate/web/DocumentTemplateController.java`
- `backend/src/test/java/com/becommerce/crm/clinical/documenttemplate/application/service/DocumentTemplateServiceTest.java`

**Regras:**
- [x] Domínio: `create(companyId, type, title, body, createdBy)` faz `trim`, rejeita vazio e tamanhos acima dos limites com `ContactValidationException` (mensagens pt-BR). `edit(type, title, body, active)`.
- [x] Endpoints (todos com `@CurrentCompanyId("Você só pode acessar modelos de documento da sua própria empresa.")`):
  - `GET    /api/v1/companies/{companyId}/document-templates?type=PRESCRIPTION&activeOnly=true` → `anamnesis:read`. Ordenado por `type`, depois `title`.
  - `GET    /api/v1/companies/{companyId}/document-templates/{id}` → `anamnesis:read`.
  - `POST   /api/v1/companies/{companyId}/document-templates` → `anamnesis:manage`, 201.
  - `PUT    /api/v1/companies/{companyId}/document-templates/{id}` → `anamnesis:manage`.
  - `DELETE /api/v1/companies/{companyId}/document-templates/{id}` → `anamnesis:manage`, 204. Documentos já emitidos ficam (FK `SET NULL`).
- [x] Serviço valida que o modelo é da mesma empresa (`requireOwned` → `DocumentTemplateNotFoundException` → 404). Registre a exceção no `GlobalExceptionHandler` do mesmo jeito que `ContactNoteNotFoundException` está registrada (procure com `git grep -n ContactNoteNotFoundException backend/src/main`).
- [x] Auditoria com `AuditModule.SETTINGS`, `RESOURCE = "DocumentTemplate"`.
- [x] **Semente sob demanda:** se o `GET` de lista encontrar a empresa **sem nenhum modelo**, crie e grave 3 modelos padrão antes de responder (idempotente, como o modelo padrão de anamnese em `AnamnesisService`):
  - CERTIFICATE "Atestado de comparecimento": `Atesto, para os devidos fins, que {{paciente.nome}}, CPF {{paciente.cpf}}, esteve em atendimento odontológico nesta clínica em {{data}}, no período das ____ às ____.`
  - CERTIFICATE "Atestado de afastamento": `Atesto, para os devidos fins, que {{paciente.nome}}, CPF {{paciente.cpf}}, necessita de ____ dia(s) de afastamento de suas atividades a partir de {{data}}, por motivo de tratamento odontológico.`
  - PRESCRIPTION "Receituário simples": `Paciente: {{paciente.nome}}\n\nUso oral:\n\n1. ________________________________\n   Tomar ____________________________\n\n{{cidade}}, {{data}}.`
- [x] Teste (`@ExtendWith(MockitoExtension.class)`, como `ContactNoteServiceTest`): cria; rejeita título vazio; outra empresa → NotFound; lista vazia → semeia 3; lista com modelos → não semeia.
- [x] Verificação: `cd backend && mvn -q test`.
- [x] Commit: `feat(pacientes): modelos de documento por empresa`

---

## Task 3: Backend — Anamnese do paciente (`clinical/anamnesis`)

**Files (criar):** estrutura completa do item 4 dos Global Constraints, com:
- `domain/PatientAnamnesis.java`, `domain/PatientAnamnesisStatus.java` (`DRAFT, COMPLETED`), `domain/AnamnesisAlertExtractor.java`, `domain/exception/PatientAnamnesisNotFoundException.java`
- `application/dto/PatientAnamnesisResponse.java`, `StartPatientAnamnesisRequest.java`, `SavePatientAnamnesisRequest.java`, `PatientAnamnesisAnswer.java`
- `application/port/in/PatientAnamnesisUseCase.java`, `application/port/out/PatientAnamnesisRepository.java`, `application/service/PatientAnamnesisService.java`
- `infrastructure/persistence/{PatientAnamnesisJpaEntity, PatientAnamnesisJpaRepository, PatientAnamnesisRepositoryImpl}.java`
- `web/PatientAnamnesisController.java`
- Testes: `.../clinical/anamnesis/application/service/PatientAnamnesisServiceTest.java` e `.../clinical/anamnesis/domain/AnamnesisAlertExtractorTest.java`

**Formato dos JSON:**
- `snapshot`: o `AnamnesisModelResponse` inteiro (o mesmo DTO que `AnamnesisService.getById` devolve), serializado com o `ObjectMapper` do Spring. Na JPA guarde como `String` com `@JdbcTypeCode(SqlTypes.JSON)`; no DTO de resposta devolva já desserializado como `AnamnesisModelResponse snapshot`.
- `answers`: objeto `{ "<questionId>": { "value": <string|number|boolean|string[]|null>, "complement": <string|null> } }`. DTO: `record PatientAnamnesisAnswer(Object value, String complement)` e `Map<UUID, PatientAnamnesisAnswer> answers`.
- `alerts`: lista de strings (texto pronto para exibir).

**Endpoints** (base `/api/v1/companies/{companyId}/patients/{contactId}/anamneses`):
- [x] `GET    ` → lista (mais recente primeiro) → `contact:read`. Resposta `List<PatientAnamnesisResponse>`.
- [x] `GET    /current` → a `COMPLETED` mais recente, ou **204 sem corpo** se não houver → `contact:read`. (O cabeçalho do paciente usa isto para o selo.)
- [x] `GET    /{id}` → `contact:read`.
- [x] `POST   ` body `StartPatientAnamnesisRequest(UUID modelId)` → `contact:update`, 201. Busca o modelo com `AnamnesisService.getById(companyId, modelId)` (reaproveite, **não** acesse os repositórios de anamnese diretamente); rejeita modelo inativo (`ContactValidationException("Este modelo de anamnese está inativo.")`); grava `DRAFT` com `snapshot`, `model_name`, `model_version` do modelo e `answers = {}`. Se `modelId` vier nulo, use o modelo padrão da empresa (procure em `AnamnesisService` como ele acha/provisiona o `is_default` e reaproveite o mesmo método; se for `private`, torne-o público sem mudar o comportamento).
- [x] `PUT    /{id}` body `SavePatientAnamnesisRequest(Map<UUID, PatientAnamnesisAnswer> answers, boolean complete)` → `contact:update`.
  - Só `DRAFT` pode ser salva; `COMPLETED` → `ContactValidationException("Anamnese concluída não pode ser alterada. Inicie uma nova.")`.
  - Só o autor ou admin (mesma regra e mensagem do `ContactNoteService.assertCanModify`, trocando "anotações" por "anamneses").
  - Ignore (descarte) chaves de `answers` que não são perguntas do `snapshot`.
  - Se `complete = true`: valida que toda pergunta `required` do snapshot tem `value` não vazio (string não branca, lista não vazia, boolean/number não nulo). Se faltar, `ContactValidationException("Responda as perguntas obrigatórias: <texto1>; <texto2>")` com no máximo 3 textos. Passando, marca `COMPLETED`, `completed_by`, `completed_at = now()` e calcula `alerts` com o `AnamnesisAlertExtractor`.
- [x] `DELETE /{id}` → `contact:update`, 204. Só `DRAFT`, só autor ou admin.

**`AnamnesisAlertExtractor.extract(AnamnesisModelResponse snapshot, Map<UUID, PatientAnamnesisAnswer> answers): List<String>`** — gera um alerta para cada pergunta com `highlight = true` que foi respondida "positivamente":
- `YES_NO` / `YES_NO_WITH_TEXT`: `value` é `true` ou a string `"YES"`/`"SIM"` (sem diferenciar maiúsculas).
- `SHORT_TEXT` / `LONG_TEXT` / `NUMBER`: `value` não vazio.
- `SINGLE_SELECT` / `MULTI_SELECT`: algum valor selecionado.
- Texto do alerta: `"<texto da pergunta>"`, e se houver `complement` não vazio: `"<texto da pergunta>: <complement>"`. Para select, sem complement: `"<texto>: <labels das opções escolhidas separadas por ', '>"`.
- [x] Testes do extractor: cobre os 4 grupos de tipo, pergunta sem `highlight` não gera alerta, complement anexado.
- [x] Testes do serviço: inicia com modelo padrão; inicia com modelo inativo → erro; salvar COMPLETED → erro; concluir com obrigatória vazia → erro com o texto da pergunta; concluir calcula `alerts`; outro usuário não-admin não edita; `/current` vazio.
- [x] Auditoria `AuditModule.CONTACTS`, `RESOURCE = "PatientAnamnesis"`. Exceção NotFound registrada no `GlobalExceptionHandler`.
- [x] Verificação: `cd backend && mvn -q test`.
- [x] Commit: `feat(pacientes): anamnese preenchida com snapshot do modelo e alertas`

---

## Task 4: Backend — Evoluções (`clinical/evolution`)

**Files (criar):** estrutura completa, com `domain/PatientEvolution.java`, `domain/PatientEvolutionStatus.java` (`DRAFT, FINALIZED`), `domain/exception/PatientEvolutionNotFoundException.java`, DTOs `PatientEvolutionRequest(@NotBlank @Size(max=20000) String body, UUID appointmentId)` e `PatientEvolutionResponse(UUID id, UUID contactId, UUID appointmentId, String body, PatientEvolutionStatus status, UUID createdBy, String createdByName, UUID finalizedBy, String finalizedByName, LocalDateTime finalizedAt, LocalDateTime createdAt, LocalDateTime updatedAt)`, serviço, persistência, controller e `PatientEvolutionServiceTest`.

**Endpoints** (base `/api/v1/companies/{companyId}/patients/{contactId}/evolutions`):
- [x] `GET` → lista, mais recente primeiro, com nomes de autor (cache de nomes igual ao `ContactNoteService.authorName`) → `contact:read`.
- [x] `POST` → cria `DRAFT` → `contact:update`, 201. Se `appointmentId` vier, valide que a consulta existe, é da mesma empresa e do **mesmo paciente** (use o repositório/porta de agendamentos já existente; procure com `git grep -n "interface .*AppointmentRepository" backend/src/main`). Senão `ContactValidationException("Consulta não encontrada para este paciente.")`.
- [x] `PUT /{id}` → edita `body`/`appointmentId` de um `DRAFT` → `contact:update`. Autor ou admin.
- [x] `POST /{id}/finalize` → `DRAFT` → `FINALIZED` → `contact:update`. **Só o autor** (nem admin finaliza evolução de outro: quem assina é quem escreveu). Mensagem: `"Só o autor pode finalizar esta evolução."`.
- [x] `DELETE /{id}` → só `DRAFT`, autor ou admin, 204.
- [x] Qualquer `PUT`/`finalize`/`DELETE` em `FINALIZED` → `ContactValidationException("Evolução finalizada não pode ser alterada.")` **no serviço** (o trigger do banco é a segunda barreira).
- [x] Testes: cria; edita rascunho; finaliza; editar/excluir finalizada → erro; admin não finaliza de outro; consulta de outro paciente → erro.
- [x] Auditoria `AuditModule.CONTACTS`, `RESOURCE = "PatientEvolution"`; finalizar usa `AuditAction.UPDATE` com mensagem `"Evolução finalizada."`.
- [x] Verificação: `cd backend && mvn -q test`.
- [x] Commit: `feat(pacientes): evoluções clínicas com finalização imutável`

---

## Task 5: Backend — Documentos do paciente + PDF (`clinical/document`)

**Files (criar):** estrutura completa, com `domain/PatientDocument.java`, `domain/DocumentPlaceholderRenderer.java`, `domain/exception/PatientDocumentNotFoundException.java`, `application/service/PatientDocumentService.java`, `application/service/PatientDocumentPdfRenderer.java`, DTOs, persistência, `web/PatientDocumentController.java`, e testes `DocumentPlaceholderRendererTest`, `PatientDocumentServiceTest`, `PatientDocumentPdfRendererTest`.

**DTOs:**
- `PreviewPatientDocumentRequest(@NotNull UUID templateId)` → `PreviewPatientDocumentResponse(DocumentType type, String title, String body)`.
- `IssuePatientDocumentRequest(UUID templateId, @NotNull DocumentType type, @NotBlank @Size(max=160) String title, @NotBlank @Size(max=20000) String body, @NotBlank @Size(max=160) String professionalName, @Size(max=40) String professionalRegistry)`.
- `PatientDocumentResponse(UUID id, UUID contactId, UUID templateId, DocumentType type, String title, String body, String professionalName, String professionalRegistry, UUID issuedBy, String issuedByName, LocalDateTime issuedAt)`.

**`DocumentPlaceholderRenderer.render(String body, Map<String,String> values)`**: troca `{{chave}}` (com ou sem espaços dentro das chaves) pelo valor; chave desconhecida ou valor nulo vira `""`. Variáveis suportadas (monte o mapa no serviço):
| Variável | Valor |
|---|---|
| `paciente.nome` | nome + sobrenome do contato |
| `paciente.cpf` | CPF do contato formatado `000.000.000-00` (vazio se não houver) |
| `paciente.nascimento` | data de nascimento `dd/MM/yyyy` |
| `paciente.prontuario` | `record_number` |
| `data` | hoje `dd/MM/yyyy` (fuso `America/Sao_Paulo`) |
| `data.extenso` | hoje por extenso, ex.: `22 de outubro de 2026` |
| `cidade` | cidade do contato; se vazia, `""` |
| `empresa.nome` | `trading_name` da empresa |
| `profissional.nome` | nome do usuário logado |

(Confira os nomes reais dos getters de CPF/nascimento/cidade na classe de domínio do contato antes de usar.)

**Endpoints** (base `/api/v1/companies/{companyId}/patients/{contactId}/documents`):
- [x] `GET` → lista, mais recente primeiro → `contact:read`.
- [x] `POST /preview` → renderiza o modelo com os dados do paciente e devolve o texto **sem gravar** → `contact:read`. Modelo inativo → erro.
- [x] `POST` → emite (grava o `body` exatamente como veio — o usuário pode ter editado o preview) → `contact:update`, 201.
- [x] `GET /{id}/pdf` → `contact:read`, `Content-Type: application/pdf`, `Content-Disposition: inline; filename="<tipo>-<sobrenome-ou-nome>-<yyyy-MM-dd>.pdf"` (sem acentos/espaços no nome do arquivo). Veja `AccountingReportController` para montar a resposta.
- [x] `DELETE /{id}` → `contact:update` **e** admin (não-admin → `CrmAccessDeniedException("Só administradores podem excluir documentos emitidos.")`), 204.

**`PatientDocumentPdfRenderer.render(PatientDocument doc, String companyName, String patientName): byte[]`** com OpenPDF (copie a forma de abrir `Document`/`PdfWriter`/fontes do `AccountingReportRenderer`):
- A4, margens 50pt. Cabeçalho: nome da empresa em negrito 14pt, linha separadora.
- Título do documento centralizado, negrito 13pt (`RECEITUÁRIO` para PRESCRIPTION, `ATESTADO` para CERTIFICATE, `TERMO` para CONSENT, ou o `title` para OTHER).
- `Paciente: <nome>` em 11pt.
- Corpo em 11pt, preservando quebras de linha (`\n` → novo parágrafo).
- Rodapé de assinatura: linha `______________________________`, `professionalName`, e `professionalRegistry` se houver; data de emissão `dd/MM/yyyy`.
- [x] Testes: renderer de variáveis (todas as chaves, chave desconhecida, espaços), PDF começa com `%PDF` e tem tamanho > 1000 bytes, delete por não-admin → erro, preview de outra empresa → NotFound.
- [x] Auditoria `AuditModule.CONTACTS`, `RESOURCE = "PatientDocument"`.
- [x] Verificação: `cd backend && mvn -q test`.
- [x] Commit: `feat(pacientes): documentos do paciente com modelos e PDF`

---

## Task 6: Backend — Foto do paciente + guarda de módulo

**Files (modificar):** classe de domínio do contato, `ContactJpaEntity` (ou equivalente), o mapper/response do contato, `masterdata/contact/web/PatientController.java`, `masterdata/company/web/ModuleGuardInterceptor.java` e o teste existente dele (procure `ModuleGuardInterceptorTest`).

- [x] Adicione `photoStorageId` (UUID, nulo) ao domínio, entidade JPA (`@Column(name = "photo_storage_id")`) e à resposta do contato (`ContactResponse` ou o DTO que `GET /patients/{id}` devolve) como `photoStorageId`.
- [x] No `PatientController` (ou em um `PatientPhotoController` novo no mesmo pacote, se o controller ficar grande):
  - `PUT    /api/v1/companies/{companyId}/patients/{contactId}/photo` (multipart `file`) → `contact:update`. Aceita só `image/jpeg`, `image/png`, `image/webp` e até **5 MB**; senão `ContactValidationException("A foto deve ser JPG, PNG ou WEBP de até 5 MB.")`. Grava com `StorageUseCase.uploadForContact(...)` (mesmo método do `ContactFileController`), salva o id em `photo_storage_id`. Se já havia foto, **apague o arquivo antigo** depois de gravar o novo (use o método de exclusão já existente do `StorageUseCase`). Responde o contato atualizado.
  - `GET    .../photo` → `contact:read`. Devolve os bytes com o `Content-Type` do arquivo e `Cache-Control: private, max-age=300`; 404 se não houver foto. Reaproveite o método de download que o `StorageController` usa.
  - `DELETE .../photo` → `contact:update`, 204. Zera a coluna e apaga o arquivo.
- [x] `ModuleGuardInterceptor.RESOURCE_MODULES`: adicione `Map.entry("document-templates", CompanyModule.PATIENTS)`. No teste do interceptor, acrescente um caso para `document-templates` → PATIENTS.
- [x] Testes do serviço/controle da foto: tipo inválido → erro; acima de 5 MB → erro; troca apaga a anterior.
- [x] Verificação: `cd backend && mvn -q test`.
- [x] Commit: `feat(pacientes): foto do paciente e módulo nos modelos de documento`

> **Checkpoint backend:** antes de seguir, rode `cd backend && mvn -q verify -DskipITs` (ou `mvn -q test` se `verify` exigir Docker) e confirme 0 falhas e 0 violações de Checkstyle.

---

## Task 7: Frontend — tipos, services e hooks

**Files (criar):**
- `frontend-refine/src/features/clinical/anamnesis/{types/patient-anamnesis.types.ts, services/patient-anamnesis.service.ts, hooks/usePatientAnamnesis.ts}`
- `frontend-refine/src/features/clinical/evolutions/{types/evolution.types.ts, services/evolution.service.ts, hooks/useEvolutions.ts}`
- `frontend-refine/src/features/clinical/documents/{types/document.types.ts, services/patient-document.service.ts, services/document-template.service.ts, hooks/usePatientDocuments.ts, hooks/useDocumentTemplates.ts}`

**Files (modificar):** `features/clinical/patients/services/patient.service.ts` e `hooks/usePatient.ts` (foto); o tipo `Contact` em `features/masterdata/contacts/types/contact.types.ts` ganha `photoStorageId?: string | null`.

- [x] Tipos espelham exatamente os DTOs das Tasks 2–6 (nomes em camelCase iguais aos records Java). Para o snapshot da anamnese, **reaproveite** os tipos de `features/masterdata/anamnesis/types/anamnesis.types.ts` (não duplique).
- [x] Services usam `api` de `@/lib/api` e a base `/companies/${companyId}/patients/${contactId}/...` (e `/companies/${companyId}/document-templates`).
  - PDF: `api.get(url, { responseType: "blob" })` e a função `openPatientDocumentPdf(companyId, contactId, id)` abre o blob numa aba nova (`URL.createObjectURL` + `window.open`, revogando a URL após 60 s).
  - Foto: `uploadPhoto` com `FormData` (campo `file`), `deletePhoto`, e `fetchPhotoUrl` que baixa o blob e devolve uma object URL.
  - `getCurrent` da anamnese: status 204 → `null`.
- [x] Hooks no padrão de `usePatient.ts`: queries com chave `["patient-anamneses", companyId, contactId]`, `["patient-anamnesis-current", companyId, contactId]`, `["patient-evolutions", ...]`, `["patient-documents", ...]`, `["document-templates", companyId]`, `["patient-photo", companyId, contactId, photoStorageId]`; mutations com `useMutationDefaults` e mensagens pt-BR ("Anamnese salva", "Anamnese concluída", "Evolução salva", "Evolução finalizada", "Documento emitido", "Foto atualizada", "Modelo salvo", etc.). Mutations de foto invalidam também as chaves de `patientKeys(companyId)`.
- [x] Verificação frontend (item 9). Commit: `feat(pacientes): services e hooks da Fase 2 da ficha`

---

## Task 8: Frontend — Abas novas e rotas

**Files (modificar):** `features/clinical/patients/components/PatientTabs.tsx`
**Files (criar):** `app/(dashboard)/patients/[id]/anamnese/page.tsx`, `.../evolucoes/page.tsx`, `.../documentos/page.tsx` (cada uma no mesmo formato de `.../anotacoes/page.tsx`: pega `id`, `companyId`, `usePatient`, loader "Carregando …", e renderiza o componente da feature).

- [x] Ordem final das abas: `Consultas`, `Anamnese` (`anamnese`), `Evoluções` (`evolucoes`), `Documentos` (`documentos`), `Financeiro`, `Arquivos`, `Anotações`.
- [x] Commit junto com a Task 9 (as páginas importam os componentes dela).

---

## Task 9: Frontend — Componentes das abas

**Files (criar):**
- `features/clinical/anamnesis/components/PatientAnamnesisTab.tsx`
- `features/clinical/anamnesis/components/AnamnesisAnswerForm.tsx`
- `features/clinical/anamnesis/components/AnamnesisAnswerView.tsx`
- `features/clinical/evolutions/components/PatientEvolutionsTab.tsx`
- `features/clinical/documents/components/PatientDocumentsTab.tsx`
- `features/clinical/documents/components/IssueDocumentDialog.tsx`
- testes em `features/clinical/*/components/__tests__/`

**Anamnese (`PatientAnamnesisTab`):**
- [x] Sem nenhuma anamnese: estado vazio "Nenhuma anamnese preenchida." + `Select` com os modelos **ativos** (hook de `features/masterdata/anamnesis/hooks/useAnamnesis.ts`), padrão pré-selecionado, e botão "Iniciar anamnese".
- [x] Com `DRAFT`: mostra `AnamnesisAnswerForm` com botões "Salvar rascunho" (`complete: false`) e "Concluir" (`complete: true`), e "Descartar" (com `AlertDialog` de confirmação; não use `window.confirm`).
- [x] Com `COMPLETED` atual: mostra `AnamnesisAnswerView` (somente leitura, perguntas com `highlight` respondidas em destaque com `text-destructive`), cabeçalho "Preenchida por <nome> em dd/MM/yyyy" e botão "Nova anamnese".
- [x] Abaixo, "Histórico": lista das anteriores (data, modelo, status) que abrem em modo leitura.
- [x] `AnamnesisAnswerForm` renderiza cada pergunta do **snapshot** por seção (título da seção como `h3`), pelo tipo:
  - `SHORT_TEXT` → `Input`; `LONG_TEXT` → `Textarea`; `NUMBER` → `Input type="number"`;
  - `YES_NO` / `YES_NO_WITH_TEXT` → dois botões/`RadioGroup` "Sim"/"Não" (valor `"YES"`/`"NO"`);
  - `SINGLE_SELECT` → `RadioGroup` das opções; `MULTI_SELECT` → `Checkbox` por opção (valor = `string[]` de `value`).
  - Campo de complemento (`Input` com `complementLabel` como label) aparece quando `allowComplement` e o gatilho bate: `YES` → resposta "YES"; `NO` → "NO"; `ALWAYS` → sempre; `OPTION` → a opção `complementOptionValue` está selecionada.
  - Pergunta `required` mostra `*` no label.
  - Seções com `isProfessional = true` ficam por último, com subtítulo "Preenchimento do profissional".
- [x] Teste: complemento aparece só quando "Sim" é marcado (gatilho `YES`); `MULTI_SELECT` envia array.

**Evoluções (`PatientEvolutionsTab`):**
- [x] Topo: `Textarea` "Nova evolução" + `Select` opcional "Vincular à consulta" (consultas do paciente via `useContactAppointments`, rótulo `dd/MM/yyyy HH:mm – <tipo>`) + botão "Salvar rascunho".
- [x] Lista em linha do tempo, mais recente primeiro. Cada item: data, autor, `Badge` "Rascunho" (variant `outline`) ou "Finalizada" (variant `secondary`), texto com `whitespace-pre-wrap`.
- [x] Rascunho do próprio usuário: botões "Editar", "Finalizar" (com `AlertDialog`: "Depois de finalizada, a evolução não pode mais ser alterada nem excluída.") e "Excluir". Admin vê "Editar"/"Excluir" em rascunhos de outros, mas **não** "Finalizar". Finalizada: nenhum botão; mostra "Finalizada por <nome> em dd/MM/yyyy HH:mm".
- [x] Use o mesmo jeito de saber se o usuário é admin/autor que o `PatientNotes.tsx` usa (leia-o e copie).
- [x] Teste: finalizada não mostra botões; rascunho do autor mostra "Finalizar".

**Documentos (`PatientDocumentsTab` + `IssueDocumentDialog`):**
- [x] Botão "Emitir documento" abre o dialog: `Select` de modelo ativo (agrupado por tipo com os rótulos "Receituário", "Atestado", "Termo", "Outro") → chama `/preview` → mostra `Input` título e `Textarea` corpo (grande, editável) com o texto renderizado; campos "Profissional" (pré-preenchido com o nome do usuário logado) e "Registro (CRO)" (opcional) → "Emitir".
- [x] Lista: tipo, título, profissional, data; botão "Abrir PDF" (abre em nova aba); "Excluir" só para admin (com `AlertDialog`).
- [x] Sem modelos ativos: texto "Nenhum modelo de documento ativo." com link para `/settings/documents`.
- [x] Teste: dialog chama preview ao escolher modelo e envia o corpo editado.

- [x] Verificação frontend (item 9). Commit (Tasks 8+9): `feat(pacientes): abas de anamnese, evoluções e documentos na ficha`

---

## Task 10: Frontend — Foto e selo de alerta no cabeçalho

**Files (modificar):** `features/clinical/patients/components/PatientHeader.tsx` (+ teste em `__tests__/` se já houver um do header).

- [x] Avatar à esquerda do nome (componente `Avatar` de `@/components/ui/avatar`, se existir; senão um `div` redondo 64px com `overflow-hidden`): mostra a foto (hook `usePatientPhoto`) ou as iniciais. Ao passar o mouse, botão com ícone `Camera` (lucide) abre um `input type="file" accept="image/jpeg,image/png,image/webp"` escondido; valide no cliente (tipo e 5 MB, `toast` de erro igual ao do backend) antes de enviar. Se houver foto, item "Remover foto". Só aparece para quem tem `contact:update` (`useAuthorization().can`).
- [x] Selo de alerta: usando `usePatientAnamnesisCurrent`, se `alerts.length > 0` mostre um `Badge` variant `destructive` com ícone `AlertTriangle` e texto `"<n> alerta(s) na anamnese"`; ao clicar/hover (`Popover` ou `Tooltip` existentes em `components/ui`) lista os alertas e um link "Ver anamnese" para a aba. Se não houver anamnese concluída, mostre um `Badge` variant `outline` "Sem anamnese" que leva à aba.
- [x] Teste: com 2 alertas mostra "2 alerta(s) na anamnese"; sem anamnese mostra "Sem anamnese".
- [x] Verificação frontend. Commit: `feat(pacientes): foto e alertas da anamnese no cabeçalho do paciente`

---

## Task 11: Frontend — Configurações → Documentos com dados reais

**Files (modificar):** `frontend-refine/src/app/(dashboard)/settings/documents/page.tsx` (hoje usa `INITIAL_TEMPLATES` em estado local — **remova o mock por completo**).

- [x] Troque o estado local por `useDocumentTemplates`. Tabela com colunas Tipo, Título, Ativo (`Switch` que chama o `PUT` com `active`), Ações (editar/excluir com `AlertDialog`).
- [x] Dialog de criar/editar com `Select` de tipo, `Input` título, `Textarea` corpo (min. 12 linhas) e, abaixo do corpo, a lista de variáveis da Task 5 como botões pequenos (`Button variant="outline" size="sm"`) que **inserem** `{{variavel}}` na posição do cursor do textarea.
- [x] Botões de criar/editar/excluir só para quem tem `anamnesis:manage`.
- [x] **Não mexa** em `settings/documents/prescriptions/page.tsx` (medicamentos padrão ficam para depois); registre isso em "Divergências encontradas" só se algo depender dele.
- [x] Verificação frontend. Commit: `feat(pacientes): modelos de documento persistidos em Configurações`

---

## Task 12: Fechamento

- [x] Atualize `docs/superpowers/specs/2026-10-12-ficha-do-paciente-design.md`: na seção de status, marque **Fase 2 — IMPLEMENTADA** (branch `feature/pacientes-fase2`) com 1 linha por entrega (anamnese, evoluções, documentos/PDF, foto, modelos em Configurações). Fase 3 continua pendente.
- [x] Rode **tudo** de novo e cole a saída resumida (últimas linhas) no fim deste arquivo, seção "Resultado final":
  - `cd backend && mvn -q test`
  - `cd frontend-refine && npm run lint && npx tsc --noEmit && npx vitest run --pool=threads`
  - `npx prettier --check --end-of-line auto $(git diff --name-only origin/crm-improvements-deploy-phase -- frontend-refine | sed 's#^frontend-refine/##')` (rodar de dentro de `frontend-refine`)
- [x] `git log --oneline origin/crm-improvements-deploy-phase..HEAD` deve mostrar ~11 commits, um por task.
- [x] Commit: `docs(pacientes): marca a Fase 2 da ficha como implementada`
- [x] **Pare aqui.** Não faça push. Avise que terminou.

---

## Critérios de aceite (o revisor vai conferir um a um)

1. Abrir um paciente mostra as abas na ordem da Task 8.
2. Iniciar anamnese com o modelo padrão, responder "Sim" numa pergunta destacada com complemento, concluir → o cabeçalho mostra "1 alerta(s) na anamnese" com o texto da pergunta e o complemento.
3. Editar o modelo de anamnese em Configurações **não** muda a anamnese já concluída.
4. Concluir com pergunta obrigatória vazia → erro listando a pergunta.
5. Evolução finalizada não tem botões e a API devolve erro em PUT/DELETE; o banco também barra (trigger).
6. Emitir atestado a partir do modelo padrão, editar o texto no dialog, emitir → "Abrir PDF" mostra o texto editado, nome da empresa, paciente e profissional.
7. Não-admin não vê "Excluir" em documentos e a API devolve 403.
8. Enviar foto PNG de 1 MB aparece no cabeçalho; GIF ou 6 MB é recusado com a mensagem pt-BR.
9. Configurações → Documentos lista os 3 modelos semeados numa empresa nova e persiste criação/edição após recarregar a página.
10. Empresa com módulo PATIENTS desligado recebe 403 `MODULE_DISABLED` em `/patients/**` e `/document-templates/**`.
11. CI verde (backend, frontend, E2E) no PR.

## Fora de escopo (NÃO implemente)

Odontograma, procedimentos e orçamentos (Fase 3); assinatura eletrônica/ICP-Brasil; envio de documento por WhatsApp/e-mail; lista de medicamentos padrão do receituário; anamnese preenchida pelo próprio paciente por link; logo da empresa no PDF.

## Divergências encontradas

- O agente executor commitou as Tasks 10 e 11 antes das Tasks 8+9 e sem rodar a verificação do frontend (o worktree não tinha `node_modules`). Os commits e603cc33, 7035ea76 e c75de871 isolados **não compilam**; a branch fica consistente a partir de 31c5e909.
- Corrigido em 31c5e909: hook `useCompanyId` inexistente (criado em `features/identity/auth/hooks`), `import { api }` → import default de `@/lib/api`, `Badge asChild` (não suportado), 24 `any` explícitos, `usePermission` chamado condicionalmente na aba Anamnese (regra de hooks) e rótulo de consulta usando `startTime`/`type` (inexistentes) → `startAt`/`title`.
- Os testes pedidos nas Tasks 9 e 10 não tinham sido escritos; adicionados no fechamento.

## Resultado final

Verificação em 2026-10-22 (HEAD após o fechamento):

- Backend `mvn -q test`: 1289 testes, 0 falhas, 0 erros (ITs com Testcontainers só no CI).
- Frontend `npx tsc --noEmit`: 0 erros. `npm run lint`: ok. Prettier: ok nos arquivos alterados.
- Vitest completo: 555 testes, 550 ok; as 5 falhas são de `SidebarGroup.test.tsx`, que já falhavam localmente na `crm-improvements-deploy-phase` antes desta fase e passam no CI.
- Testes novos: `PatientEvolutionsTab` (3), `AnamnesisAnswerForm` (2), `PatientDocumentsTab` (1), `PatientHeader` (2).
- Critérios de aceite 2–10 são manuais/E2E (sem banco local): conferir no app depois do CI verde. Critério 1 conferido no código (ordem das abas).
