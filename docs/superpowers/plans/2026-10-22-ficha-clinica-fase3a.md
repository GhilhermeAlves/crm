# Ficha do Paciente (Fase 3a — Ficha clínica) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Este documento é uma ORDEM: execute task a task, **na ordem**, marcando cada checkbox. Ao fim de cada task rode a verificação indicada; só faça o commit se ela passar. **Um commit por task.** Sem push, merge, rebase ou PR. Se algo do plano não bater com o código real (nome de classe, método, assinatura), **pare, não invente**: registre em "Divergências encontradas" no fim deste arquivo e siga a solução mais próxima do padrão citado.

**Goal:** Nova aba **Ficha clínica** na ficha do paciente (`/patients/[id]/ficha-clinica`) com odontograma (permanente/decídua) e procedimentos por dente/face nos estados A realizar / Realizado / Pré-existente, com lançamento financeiro opcional ao realizar.

**Architecture:** Pacote backend novo `com.becommerce.crm.clinical.procedure`, cópia estrutural de `clinical/evolution` (domínio + ports + service + JPA + controller). Procedimento é agregado com a lista de dentes (tabela filha). Lançamento usa `FinancialTransaction.create(...)` + `FinancialTransactionRepository` existentes. Frontend em `frontend-refine/src/features/clinical/procedures/`, cópia estrutural de `features/clinical/evolutions`; o odontograma é SVG puro, e o resumo por dente é calculado no cliente.

**Tech Stack:** Spring Boot hexagonal (`backend/`), PostgreSQL + Flyway, JUnit 5 + Mockito + AssertJ, Testcontainers (só no CI); Next.js App Router + TanStack Query + shadcn/ui (`frontend-refine/`), Vitest + Testing Library.

**Spec:** `docs/superpowers/specs/2026-10-22-ficha-clinica-design.md` — leia antes. Este plano **corrige** três pontos do spec para seguir o código real (a Task 11 atualiza o spec):
1. Rotas são `/api/v1/companies/{companyId}/patients/{contactId}/procedures` (padrão da Fase 2), não `/api/contacts/...`.
2. Valor zero com lançamento → **400** (`ContactValidationException`, padrão existente), não 422. Conflitos → **409** via `ContactConflictException` (já mapeada).
3. A aba entra **logo após "Consultas"** (não existe aba "Visão geral" em `PatientTabs`). O lançamento usa `PaymentMethod.OTHER` (campo obrigatório no financeiro; editável depois na tela Financeiro) e categoria `TransactionCategory.PROCEDURE`.

## Global Constraints

1. **Branch:** `feature/pacientes-fase3a` (já existe, contém o spec e este plano), pasta `C:\Users\ghilh\Desktop\PROJETO\crm\crm-deploy`. Confirme com `git branch --show-current` antes da Task 1. Não troque de branch.
2. **Migration única `V106__patient_procedures.sql`.** Antes de criar: `ls backend/src/main/resources/db/migration | tail -3` (última deve ser V105; se já houver V106, use o próximo número e anote). `git grep -n "patient_procedures\|patient_procedure_teeth" backend/src/main/resources/db/migration` deve voltar vazio. Nunca editar migrations antigas. Sem Docker local: o E2E do CI é o primeiro Postgres real.
3. **RLS em toda tabela nova:** `company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE`, `ENABLE` + `FORCE ROW LEVEL SECURITY`, policy `tenant_isolation_policy` com `USING`/`WITH CHECK (company_id = app.current_tenant_id())`, `GRANT SELECT, INSERT, UPDATE, DELETE ... TO crm_app` (bloco `DO $$` igual ao fim da V105).
4. **Backend:** serviços com `TenantContext.setCompanyId(companyId)` no início e `TenantContext.clear()` no `finally`; `requireOwnedContact` igual ao `PatientEvolutionService`; auditoria `auditor.record(companyId, AuditAction.X, AuditModule.CONTACTS, "PatientProcedure", id, "mensagem pt-BR", actorId, null)`. Controller: `@CurrentCompanyId(ACCESS_MESSAGE)`, `@PreAuthorize("hasAuthority('contact:read')")` leitura e `'contact:update'` escrita. **Nenhuma permissão nova.** Gerar lançamento exige também `finance:create`, checado no service a partir de `principal.permissions()` (controller passa `boolean canCreateFinance`); sem ela → `CrmAccessDeniedException` (403).
5. **Exceções:** validação → `ContactValidationException` (400); regra de estado → `ContactConflictException` (409); não encontrado → `PatientProcedureNotFoundException` (404, handler novo no `GlobalExceptionHandler`); permissão → `CrmAccessDeniedException` (403).
6. **Frontend:** textos em pt-BR; só tokens do tema (sem hex) — estados: A realizar `fill-destructive`/`text-destructive`, Realizado `fill-primary`/`text-primary`, Pré-existente `fill-muted-foreground`/`text-muted-foreground`. Mutations com `useMutationDefaults` (`@/lib/query/mutation-utils`). Testes **sem** `user-event`/jest-dom: `fireEvent` + `screen.getByText/queryByText` + `expect(...).toBeTruthy()/toBeNull()`.
7. **Estilo:** Checkstyle até 200 colunas. Prettier `printWidth 100`, aspas duplas, `trailingComma all`; todo arquivo do frontend alterado passa em `npx prettier --check --end-of-line auto <arquivos>` (rodar de dentro de `frontend-refine`).
8. **Verificações:** backend `cd backend && mvn -q test`; frontend `cd frontend-refine && npm run lint && npx tsc --noEmit && npx vitest run --pool=threads` + Prettier.
9. **Commits:** Conventional em pt-BR (`feat(pacientes): ...`), terminando com linha em branco e `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Estrutura de arquivos

Backend `backend/src/main/java/com/becommerce/crm/clinical/procedure/`:

| Arquivo | Responsabilidade |
|---|---|
| `domain/ProcedureState.java` | enum `PLANNED, DONE, PRE_EXISTING` |
| `domain/Dentition.java` | enum `PERMANENT, DECIDUOUS` + `isValidTooth(int)` |
| `domain/ToothFace.java` | enum `M, D, O, V, L` |
| `domain/ProcedureTooth.java` | record `(int tooth, Set<ToothFace> faces)` |
| `domain/PatientProcedure.java` | agregado: criação, edição, conclusão, regras de estado |
| `domain/exception/PatientProcedureNotFoundException.java` | 404 |
| `application/dto/*.java` | `PatientProcedureRequest`, `CompleteProcedureRequest`, `ProcedureToothDto`, `PatientProcedureResponse` |
| `application/port/in/PatientProcedureUseCase.java` | casos de uso |
| `application/port/out/PatientProcedureRepository.java` | persistência |
| `application/service/PatientProcedureService.java` | orquestra contato, catálogo, financeiro, auditoria |
| `infrastructure/persistence/*.java` | `PatientProcedureJpaEntity`, `PatientProcedureToothJpaEntity`, `PatientProcedureJpaRepository`, `PatientProcedureRepositoryImpl` |
| `web/PatientProcedureController.java` | REST |

Frontend `frontend-refine/src/features/clinical/procedures/`:

| Arquivo | Responsabilidade |
|---|---|
| `types/procedure.types.ts` | tipos da API |
| `utils/teeth.ts` | dentes por dentição/quadrante, prioridade de estado, rótulos |
| `services/procedure.service.ts` | chamadas HTTP |
| `hooks/useProcedures.ts` | query + mutations |
| `components/Odontogram.tsx` | SVG; modo filtro e modo seleção |
| `components/ProcedureDrawer.tsx` | criar/editar (Sheet) |
| `components/CompleteProcedureDialog.tsx` | marcar como realizado |
| `components/ProcedureList.tsx` | "Ficha geral" com filtros e ações |
| `components/PatientClinicalChartTab.tsx` | compõe a aba |

---

### Task 1: Migration `V106__patient_procedures.sql`

**Files:**
- Create: `backend/src/main/resources/db/migration/V106__patient_procedures.sql`

- [ ] **Step 1: Conferir número e nomes livres** — rode os dois comandos do Global Constraint 2.
- [ ] **Step 2: Criar o arquivo** com exatamente:

```sql
-- ===========================================================================
-- V106 — Ficha clínica do paciente (ficha do paciente, Fase 3a)
--
-- patient_procedures: procedimento odontológico do paciente (serviço do
--   catálogo), em um de três estados: PLANNED (a realizar), DONE (realizado),
--   PRE_EXISTING (pré-existente). Pode gerar um lançamento em
--   financial_transactions quando DONE.
-- patient_procedure_teeth: dentes (FDI) e faces de cada procedimento. Sem
--   linhas = procedimento da boca toda.
-- Permissões reaproveitadas: contact:read / contact:update (+ finance:create
-- para gerar lançamento). O vínculo com orçamento (budget_id) chega na 3b.
-- ===========================================================================
CREATE TABLE patient_procedures (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id               UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    contact_id               UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    catalog_item_id          UUID NOT NULL REFERENCES catalog_items(id),
    name_snapshot            VARCHAR(160) NOT NULL,
    state                    VARCHAR(20) NOT NULL,
    dentition                VARCHAR(10) NOT NULL,
    price_cents              BIGINT NOT NULL DEFAULT 0,
    professional_id          UUID REFERENCES users(id) ON DELETE SET NULL,
    notes                    TEXT,
    performed_at             DATE,
    financial_transaction_id UUID REFERENCES financial_transactions(id) ON DELETE SET NULL,
    created_by               UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_patient_procedures_state CHECK (state IN ('PLANNED', 'DONE', 'PRE_EXISTING')),
    CONSTRAINT chk_patient_procedures_dentition CHECK (dentition IN ('PERMANENT', 'DECIDUOUS')),
    CONSTRAINT chk_patient_procedures_price CHECK (price_cents >= 0),
    CONSTRAINT chk_patient_procedures_performed CHECK (state <> 'DONE' OR performed_at IS NOT NULL)
);

CREATE INDEX idx_patient_procedures_company_contact ON patient_procedures (company_id, contact_id);

CREATE TABLE patient_procedure_teeth (
    procedure_id UUID NOT NULL REFERENCES patient_procedures(id) ON DELETE CASCADE,
    company_id   UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    tooth        SMALLINT NOT NULL,
    faces        VARCHAR(1)[] NOT NULL DEFAULT '{}',
    PRIMARY KEY (procedure_id, tooth),
    CONSTRAINT chk_patient_procedure_teeth_faces CHECK (faces <@ ARRAY['M', 'D', 'O', 'V', 'L']::VARCHAR(1)[])
);

-- ===========================================================================
-- RLS FORCE + GRANT
-- ===========================================================================
DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['patient_procedures', 'patient_procedure_teeth']
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

- [ ] **Step 3: Revisão manual** — nomes de índice/constraint únicos (`git grep -n "idx_patient_procedures\|chk_patient_procedure" backend/src/main/resources/db/migration` só acha a V106).
- [ ] **Step 4: Commit** — `git add backend/src/main/resources/db/migration/V106__patient_procedures.sql && git commit -m "feat(pacientes): migration da ficha clínica (procedimentos e dentes)"` (+ trailer).

---

### Task 2: Domínio do procedimento

**Files:**
- Create: `backend/src/main/java/com/becommerce/crm/clinical/procedure/domain/{ProcedureState,Dentition,ToothFace,ProcedureTooth,PatientProcedure}.java`, `domain/exception/PatientProcedureNotFoundException.java`
- Test: `backend/src/test/java/com/becommerce/crm/clinical/procedure/domain/PatientProcedureTest.java`

**Interfaces — Produces:**
- `PatientProcedure.create(UUID companyId, UUID contactId, UUID catalogItemId, String name, ProcedureState state, Dentition dentition, long priceCents, UUID professionalId, String notes, LocalDate performedAt, List<ProcedureTooth> teeth, UUID createdBy)`
- `PatientProcedure.reconstitute(...)` com todos os campos (ordem dos campos da classe).
- `void edit(ProcedureState state, Dentition dentition, long priceCents, UUID professionalId, String notes, List<ProcedureTooth> teeth)` — só PLANNED/PRE_EXISTING; `state` aceita só PLANNED/PRE_EXISTING.
- `void editNotes(String notes)` — qualquer estado.
- `void complete(LocalDate performedAt, UUID professionalId)` — só de PLANNED.
- `void linkTransaction(UUID transactionId)`, `boolean isDone()`, getters.
- `Dentition.isValidTooth(int tooth)`.

- [ ] **Step 1: Escrever o teste que falha**

```java
package com.becommerce.crm.clinical.procedure.domain;

import com.becommerce.crm.masterdata.contact.domain.exception.ContactConflictException;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PatientProcedureTest {

    private static final UUID COMPANY = UUID.randomUUID();
    private static final UUID CONTACT = UUID.randomUUID();
    private static final UUID ITEM = UUID.randomUUID();

    private static PatientProcedure planned(Dentition dentition, List<ProcedureTooth> teeth) {
        return PatientProcedure.create(COMPANY, CONTACT, ITEM, "Restauração", ProcedureState.PLANNED,
                dentition, 15000, null, null, null, teeth, null);
    }

    @Test
    void validatesTeethAgainstDentition() {
        assertThat(Dentition.PERMANENT.isValidTooth(11)).isTrue();
        assertThat(Dentition.PERMANENT.isValidTooth(19)).isFalse();
        assertThat(Dentition.PERMANENT.isValidTooth(51)).isFalse();
        assertThat(Dentition.DECIDUOUS.isValidTooth(85)).isTrue();
        assertThat(Dentition.DECIDUOUS.isValidTooth(56)).isFalse();
        assertThatThrownBy(() -> planned(Dentition.DECIDUOUS,
                List.of(new ProcedureTooth(11, EnumSet.noneOf(ToothFace.class)))))
                .isInstanceOf(ContactValidationException.class)
                .hasMessageContaining("Dente 11");
    }

    @Test
    void rejectsDuplicatedTooth() {
        assertThatThrownBy(() -> planned(Dentition.PERMANENT, List.of(
                new ProcedureTooth(11, EnumSet.of(ToothFace.M)),
                new ProcedureTooth(11, EnumSet.of(ToothFace.D)))))
                .isInstanceOf(ContactValidationException.class);
    }

    @Test
    void doneRequiresPerformedAt() {
        assertThatThrownBy(() -> PatientProcedure.create(COMPANY, CONTACT, ITEM, "Limpeza",
                ProcedureState.DONE, Dentition.PERMANENT, 0, null, null, null, List.of(), null))
                .isInstanceOf(ContactValidationException.class)
                .hasMessageContaining("data");
    }

    @Test
    void completesPlannedOnly() {
        PatientProcedure procedure = planned(Dentition.PERMANENT, List.of());
        UUID professional = UUID.randomUUID();
        procedure.complete(LocalDate.of(2026, 10, 22), professional);
        assertThat(procedure.isDone()).isTrue();
        assertThat(procedure.getPerformedAt()).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(procedure.getProfessionalId()).isEqualTo(professional);
        assertThatThrownBy(() -> procedure.complete(LocalDate.now(), null))
                .isInstanceOf(ContactConflictException.class);
    }

    @Test
    void doneAcceptsOnlyNotes() {
        PatientProcedure procedure = planned(Dentition.PERMANENT, List.of());
        procedure.complete(LocalDate.now(), null);
        procedure.editNotes("  Sem intercorrências  ");
        assertThat(procedure.getNotes()).isEqualTo("Sem intercorrências");
        assertThatThrownBy(() -> procedure.edit(ProcedureState.PLANNED, Dentition.PERMANENT, 100, null,
                null, List.of()))
                .isInstanceOf(ContactConflictException.class);
    }

    @Test
    void editCannotJumpToDone() {
        PatientProcedure procedure = planned(Dentition.PERMANENT, List.of());
        procedure.edit(ProcedureState.PRE_EXISTING, Dentition.PERMANENT, 0, null, null, List.of());
        assertThat(procedure.getState()).isEqualTo(ProcedureState.PRE_EXISTING);
        assertThatThrownBy(() -> procedure.edit(ProcedureState.DONE, Dentition.PERMANENT, 0, null, null,
                List.of()))
                .isInstanceOf(ContactValidationException.class);
    }

    @Test
    void negativePriceIsRejected() {
        assertThatThrownBy(() -> PatientProcedure.create(COMPANY, CONTACT, ITEM, "X", ProcedureState.PLANNED,
                Dentition.PERMANENT, -1, null, null, null, List.of(), null))
                .isInstanceOf(ContactValidationException.class);
    }
}
```

- [ ] **Step 2: Rodar e ver falhar** — `cd backend && mvn -q test -Dtest=PatientProcedureTest` → erro de compilação (classes inexistentes).
- [ ] **Step 3: Implementar**

`ProcedureState.java`:
```java
package com.becommerce.crm.clinical.procedure.domain;

/** Estado do procedimento na ficha clínica. Persistido como {@code String} (V106). */
public enum ProcedureState {
    PLANNED,
    DONE,
    PRE_EXISTING
}
```

`ToothFace.java`:
```java
package com.becommerce.crm.clinical.procedure.domain;

/** Face do dente: mesial, distal, oclusal/incisal, vestibular, lingual/palatina. */
public enum ToothFace {
    M,
    D,
    O,
    V,
    L
}
```

`Dentition.java`:
```java
package com.becommerce.crm.clinical.procedure.domain;

/** Dentição do odontograma, com os dentes válidos na numeração FDI. */
public enum Dentition {
    PERMANENT(8, 1),
    DECIDUOUS(5, 5);

    private final int teethPerQuadrant;
    private final int firstQuadrant;

    Dentition(int teethPerQuadrant, int firstQuadrant) {
        this.teethPerQuadrant = teethPerQuadrant;
        this.firstQuadrant = firstQuadrant;
    }

    public boolean isValidTooth(int tooth) {
        int quadrant = tooth / 10;
        int position = tooth % 10;
        return quadrant >= firstQuadrant && quadrant < firstQuadrant + 4
                && position >= 1 && position <= teethPerQuadrant;
    }
}
```

`ProcedureTooth.java`:
```java
package com.becommerce.crm.clinical.procedure.domain;

import java.util.Set;

/** Dente (FDI) atingido pelo procedimento e as faces envolvidas (pode ser vazio). */
public record ProcedureTooth(int tooth, Set<ToothFace> faces) {

    public ProcedureTooth {
        faces = faces == null ? Set.of() : Set.copyOf(faces);
    }
}
```

`exception/PatientProcedureNotFoundException.java`:
```java
package com.becommerce.crm.clinical.procedure.domain.exception;

import java.util.UUID;

public class PatientProcedureNotFoundException extends RuntimeException {

    public PatientProcedureNotFoundException(UUID id) {
        super("Procedimento não encontrado: " + id);
    }
}
```

`PatientProcedure.java`:
```java
package com.becommerce.crm.clinical.procedure.domain;

import com.becommerce.crm.masterdata.contact.domain.exception.ContactConflictException;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Procedimento odontológico do paciente (ficha clínica, Fase 3a). {@code PLANNED}
 * e {@code PRE_EXISTING} são livremente editáveis; {@code DONE} só nasce com data
 * de realização (criação direta ou {@link #complete}) e depois só aceita notas.
 * Sem dentes = procedimento da boca toda.
 */
public class PatientProcedure {

    static final int MAX_NOTES_LENGTH = 5000;

    private final UUID id;
    private final UUID companyId;
    private final UUID contactId;
    private final UUID catalogItemId;
    private final String nameSnapshot;
    private ProcedureState state;
    private Dentition dentition;
    private long priceCents;
    private UUID professionalId;
    private String notes;
    private LocalDate performedAt;
    private UUID financialTransactionId;
    private List<ProcedureTooth> teeth;
    private final UUID createdBy;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private PatientProcedure(UUID id, UUID companyId, UUID contactId, UUID catalogItemId, String nameSnapshot,
                             ProcedureState state, Dentition dentition, long priceCents, UUID professionalId,
                             String notes, LocalDate performedAt, UUID financialTransactionId,
                             List<ProcedureTooth> teeth, UUID createdBy, LocalDateTime createdAt,
                             LocalDateTime updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.contactId = contactId;
        this.catalogItemId = catalogItemId;
        this.nameSnapshot = nameSnapshot;
        this.state = state;
        this.dentition = dentition;
        this.priceCents = priceCents;
        this.professionalId = professionalId;
        this.notes = notes;
        this.performedAt = performedAt;
        this.financialTransactionId = financialTransactionId;
        this.teeth = teeth;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PatientProcedure create(UUID companyId, UUID contactId, UUID catalogItemId, String name,
                                          ProcedureState state, Dentition dentition, long priceCents,
                                          UUID professionalId, String notes, LocalDate performedAt,
                                          List<ProcedureTooth> teeth, UUID createdBy) {
        if (state == null || dentition == null) {
            throw new ContactValidationException("Estado e dentição são obrigatórios.");
        }
        if (state == ProcedureState.DONE && performedAt == null) {
            throw new ContactValidationException("Informe a data de realização do procedimento.");
        }
        LocalDateTime now = LocalDateTime.now();
        return new PatientProcedure(UUID.randomUUID(), companyId, contactId, catalogItemId, name, state,
                dentition, validPrice(priceCents), professionalId, normalizeNotes(notes),
                state == ProcedureState.DONE ? performedAt : null, null, validTeeth(dentition, teeth),
                createdBy, now, now);
    }

    public static PatientProcedure reconstitute(UUID id, UUID companyId, UUID contactId, UUID catalogItemId,
                                                String nameSnapshot, ProcedureState state, Dentition dentition,
                                                long priceCents, UUID professionalId, String notes,
                                                LocalDate performedAt, UUID financialTransactionId,
                                                List<ProcedureTooth> teeth, UUID createdBy,
                                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new PatientProcedure(id, companyId, contactId, catalogItemId, nameSnapshot, state, dentition,
                priceCents, professionalId, notes, performedAt, financialTransactionId, List.copyOf(teeth),
                createdBy, createdAt, updatedAt);
    }

    public void edit(ProcedureState newState, Dentition newDentition, long newPriceCents, UUID newProfessionalId,
                     String newNotes, List<ProcedureTooth> newTeeth) {
        if (isDone()) {
            throw new ContactConflictException("Procedimento realizado só permite alterar as observações.");
        }
        if (newState != ProcedureState.PLANNED && newState != ProcedureState.PRE_EXISTING) {
            throw new ContactValidationException("Use \"Marcar como realizado\" para concluir o procedimento.");
        }
        if (newDentition == null) {
            throw new ContactValidationException("Estado e dentição são obrigatórios.");
        }
        this.teeth = validTeeth(newDentition, newTeeth);
        this.state = newState;
        this.dentition = newDentition;
        this.priceCents = validPrice(newPriceCents);
        this.professionalId = newProfessionalId;
        this.notes = normalizeNotes(newNotes);
        touch();
    }

    public void editNotes(String newNotes) {
        this.notes = normalizeNotes(newNotes);
        touch();
    }

    public void complete(LocalDate date, UUID professional) {
        if (state != ProcedureState.PLANNED) {
            throw new ContactConflictException("Só procedimentos \"A realizar\" podem ser marcados como realizados.");
        }
        if (date == null) {
            throw new ContactValidationException("Informe a data de realização do procedimento.");
        }
        this.state = ProcedureState.DONE;
        this.performedAt = date;
        if (professional != null) {
            this.professionalId = professional;
        }
        touch();
    }

    public void linkTransaction(UUID transactionId) {
        this.financialTransactionId = transactionId;
        touch();
    }

    public boolean isDone() {
        return state == ProcedureState.DONE;
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    private static long validPrice(long priceCents) {
        if (priceCents < 0) {
            throw new ContactValidationException("O valor do procedimento não pode ser negativo.");
        }
        return priceCents;
    }

    private static List<ProcedureTooth> validTeeth(Dentition dentition, List<ProcedureTooth> teeth) {
        if (teeth == null) {
            return List.of();
        }
        Set<Integer> seen = new HashSet<>();
        for (ProcedureTooth tooth : teeth) {
            if (!dentition.isValidTooth(tooth.tooth())) {
                throw new ContactValidationException("Dente " + tooth.tooth() + " não existe nesta dentição.");
            }
            if (!seen.add(tooth.tooth())) {
                throw new ContactValidationException("Dente " + tooth.tooth() + " informado mais de uma vez.");
            }
        }
        return List.copyOf(teeth);
    }

    private static String normalizeNotes(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_NOTES_LENGTH) {
            throw new ContactValidationException(
                    "As observações devem ter no máximo " + MAX_NOTES_LENGTH + " caracteres.");
        }
        return trimmed;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getContactId() { return contactId; }
    public UUID getCatalogItemId() { return catalogItemId; }
    public String getNameSnapshot() { return nameSnapshot; }
    public ProcedureState getState() { return state; }
    public Dentition getDentition() { return dentition; }
    public long getPriceCents() { return priceCents; }
    public UUID getProfessionalId() { return professionalId; }
    public String getNotes() { return notes; }
    public LocalDate getPerformedAt() { return performedAt; }
    public UUID getFinancialTransactionId() { return financialTransactionId; }
    public List<ProcedureTooth> getTeeth() { return teeth; }
    public UUID getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

> Confira que `ContactConflictException` tem construtor `(String message)` (`masterdata/contact/domain/exception/ContactConflictException.java` — tem).

- [ ] **Step 4: Rodar e ver passar** — `mvn -q test -Dtest=PatientProcedureTest` → PASS.
- [ ] **Step 5: Commit** — `feat(pacientes): domínio do procedimento da ficha clínica`.

---

### Task 3: Persistência

**Files:**
- Create: `backend/src/main/java/com/becommerce/crm/clinical/procedure/application/port/out/PatientProcedureRepository.java`
- Create: `.../infrastructure/persistence/{PatientProcedureJpaEntity,PatientProcedureToothJpaEntity,PatientProcedureJpaRepository,PatientProcedureRepositoryImpl}.java`

**Interfaces — Produces:**
```java
public interface PatientProcedureRepository {
    PatientProcedure save(PatientProcedure procedure);
    Optional<PatientProcedure> findById(UUID id);
    List<PatientProcedure> findByCompanyIdAndContactIdOrderByCreatedAtDesc(UUID companyId, UUID contactId);
    void delete(PatientProcedure procedure);
}
```

- [ ] **Step 1: Porta de saída** (`application/port/out/PatientProcedureRepository.java`) com a interface acima (imports `PatientProcedure`, `List`, `Optional`, `UUID`).
- [ ] **Step 2: Entidade dos dentes** — `PatientProcedureToothJpaEntity`:

```java
package com.becommerce.crm.clinical.procedure.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Linha de {@code patient_procedure_teeth}, coleção de {@link PatientProcedureJpaEntity}. */
@Embeddable
public class PatientProcedureToothJpaEntity {

    @Column(name = "company_id", nullable = false)
    private java.util.UUID companyId;

    @Column(name = "tooth", nullable = false)
    private short tooth;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "faces", columnDefinition = "varchar(1)[]", nullable = false)
    private String[] faces;

    public java.util.UUID getCompanyId() { return companyId; }
    public void setCompanyId(java.util.UUID companyId) { this.companyId = companyId; }
    public short getTooth() { return tooth; }
    public void setTooth(short tooth) { this.tooth = tooth; }
    public String[] getFaces() { return faces; }
    public void setFaces(String[] faces) { this.faces = faces; }
}
```

- [ ] **Step 3: Entidade principal** — `PatientProcedureJpaEntity` no mesmo formato de `PatientEvolutionJpaEntity` (campos `@Column` com getters/setters de uma linha) para: `id`, `company_id`, `contact_id`, `catalog_item_id`, `name_snapshot`, `state` (String), `dentition` (String), `price_cents` (long), `professional_id`, `notes`, `performed_at` (LocalDate), `financial_transaction_id`, `created_by`, `created_at`, `updated_at`; mais a coleção:

```java
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "patient_procedure_teeth", joinColumns = @JoinColumn(name = "procedure_id"))
    private List<PatientProcedureToothJpaEntity> teeth = new ArrayList<>();

    public List<PatientProcedureToothJpaEntity> getTeeth() { return teeth; }
    public void setTeeth(List<PatientProcedureToothJpaEntity> teeth) { this.teeth = teeth; }
```
(`@Entity @Table(name = "patient_procedures")`; imports `jakarta.persistence.*` usados e `java.util.ArrayList/List`.)

- [ ] **Step 4: JpaRepository**

```java
package com.becommerce.crm.clinical.procedure.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PatientProcedureJpaRepository extends JpaRepository<PatientProcedureJpaEntity, UUID> {

    List<PatientProcedureJpaEntity> findByCompanyIdAndContactIdOrderByCreatedAtDesc(UUID companyId, UUID contactId);
}
```

- [ ] **Step 5: RepositoryImpl** — mesmo formato de `PatientEvolutionRepositoryImpl` (`save`, `findById`, `findBy...`, `delete` via `deleteById`), com os mapeamentos:

```java
    private static PatientProcedureJpaEntity toEntity(PatientProcedure p) {
        PatientProcedureJpaEntity e = new PatientProcedureJpaEntity();
        e.setId(p.getId());
        e.setCompanyId(p.getCompanyId());
        e.setContactId(p.getContactId());
        e.setCatalogItemId(p.getCatalogItemId());
        e.setNameSnapshot(p.getNameSnapshot());
        e.setState(p.getState().name());
        e.setDentition(p.getDentition().name());
        e.setPriceCents(p.getPriceCents());
        e.setProfessionalId(p.getProfessionalId());
        e.setNotes(p.getNotes());
        e.setPerformedAt(p.getPerformedAt());
        e.setFinancialTransactionId(p.getFinancialTransactionId());
        e.setCreatedBy(p.getCreatedBy());
        e.setCreatedAt(p.getCreatedAt());
        e.setUpdatedAt(p.getUpdatedAt());
        e.setTeeth(p.getTeeth().stream().map(t -> {
            PatientProcedureToothJpaEntity te = new PatientProcedureToothJpaEntity();
            te.setCompanyId(p.getCompanyId());
            te.setTooth((short) t.tooth());
            te.setFaces(t.faces().stream().map(Enum::name).sorted().toArray(String[]::new));
            return te;
        }).collect(Collectors.toCollection(ArrayList::new)));
        return e;
    }

    private static PatientProcedure toDomain(PatientProcedureJpaEntity e) {
        List<ProcedureTooth> teeth = e.getTeeth().stream()
                .map(t -> new ProcedureTooth(t.getTooth(), Arrays.stream(t.getFaces())
                        .map(ToothFace::valueOf).collect(Collectors.toSet())))
                .sorted(Comparator.comparingInt(ProcedureTooth::tooth))
                .toList();
        return PatientProcedure.reconstitute(e.getId(), e.getCompanyId(), e.getContactId(), e.getCatalogItemId(),
                e.getNameSnapshot(), ProcedureState.valueOf(e.getState()), Dentition.valueOf(e.getDentition()),
                e.getPriceCents(), e.getProfessionalId(), e.getNotes(), e.getPerformedAt(),
                e.getFinancialTransactionId(), teeth, e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedAt());
    }
```

- [ ] **Step 6: Compilar** — `mvn -q -DskipTests compile` → OK (o mapeamento real é validado no IT da Task 6, no CI).
- [ ] **Step 7: Commit** — `feat(pacientes): persistência dos procedimentos da ficha clínica`.

---

### Task 4: Service, DTOs e casos de uso

**Files:**
- Create: `.../procedure/application/dto/{ProcedureToothDto,PatientProcedureRequest,CompleteProcedureRequest,PatientProcedureResponse}.java`
- Create: `.../procedure/application/port/in/PatientProcedureUseCase.java`
- Create: `.../procedure/application/service/PatientProcedureService.java`
- Test: `backend/src/test/java/com/becommerce/crm/clinical/procedure/application/service/PatientProcedureServiceTest.java`

**Interfaces — Consumes:** Task 2 e 3; `CatalogItemRepository.findById(UUID)` (`masterdata/catalog/application/port/output`), `CatalogItem#getCompanyId/getType/isActive/getName/getPrice/getCategory`; `FinancialTransactionRepository.save/findById` (`finance/transaction/application/port/output`); `FinancialTransaction.create(UUID id, UUID companyId, TransactionType type, String description, TransactionCategory category, long amountCents, long feeCents, PaymentMethod paymentMethod, LocalDate dueDate, UUID contactId, UUID professionalUserId, UUID catalogItemId, String notes, LocalDate paidAt, UUID createdBy)`, `cancel()`, `getStatus()`; `UserRepository.findById` (nome do profissional).

**Interfaces — Produces:**
```java
public record ProcedureToothDto(@Min(11) @Max(85) int tooth, Set<ToothFace> faces) {}

public record PatientProcedureRequest(
        @NotNull UUID catalogItemId,       // ignorado no PATCH
        @NotNull ProcedureState state,
        @NotNull Dentition dentition,
        @NotNull @PositiveOrZero Long priceCents,
        UUID professionalId,
        @Size(max = 5000) String notes,
        LocalDate performedAt,
        @Valid List<ProcedureToothDto> teeth,
        boolean generateTransaction,
        LocalDate dueDate) {}

public record CompleteProcedureRequest(@NotNull LocalDate performedAt, UUID professionalId,
                                       boolean generateTransaction, LocalDate dueDate) {}

public record PatientProcedureResponse(UUID id, UUID contactId, UUID catalogItemId, String name, String category,
        ProcedureState state, Dentition dentition, long priceCents, UUID professionalId, String professionalName,
        String notes, LocalDate performedAt, List<ProcedureToothDto> teeth, TransactionRef transaction,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
    public record TransactionRef(UUID id, TransactionStatus status) {}
}

public interface PatientProcedureUseCase {
    List<PatientProcedureResponse> list(UUID companyId, UUID contactId, ProcedureState state, String category, Integer tooth);
    PatientProcedureResponse create(UUID companyId, UUID contactId, PatientProcedureRequest request, UUID actorId, boolean canCreateFinance);
    PatientProcedureResponse update(UUID companyId, UUID contactId, UUID procedureId, PatientProcedureRequest request, UUID actorId);
    PatientProcedureResponse complete(UUID companyId, UUID contactId, UUID procedureId, CompleteProcedureRequest request, UUID actorId, boolean canCreateFinance);
    void delete(UUID companyId, UUID contactId, UUID procedureId, UUID actorId);
}
```
No PATCH, se o procedimento está `DONE`, o service chama `editNotes(request.notes())` **somente** se todos os demais campos do request forem iguais aos atuais (estado, dentição, preço, profissional, dentes); qualquer diferença → `ContactConflictException("Procedimento realizado só permite alterar as observações.")`. O frontend sempre envia o objeto completo.

- [ ] **Step 1: Escrever o teste que falha** (mesmo esqueleto de `PatientEvolutionServiceTest`: `@ExtendWith(MockitoExtension.class)`, mocks `PatientProcedureRepository repository`, `ContactRepository contactRepository`, `CatalogItemRepository catalogRepository`, `FinancialTransactionRepository transactionRepository`, `UserRepository userRepository`, `TenantAuditRecorder auditor`; `setUp` igual, mais `lenient().when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0))` e um `CatalogItem` mockado `SERVICE`, ativo, da empresa, nome "Restauração", preço `new BigDecimal("150.00")`, categoria "Dentística", devolvido por `catalogRepository.findById(ITEM_ID)`). Testes:

```java
    private PatientProcedureRequest request(ProcedureState state, long price, LocalDate performedAt,
                                            boolean generate) {
        return new PatientProcedureRequest(ITEM_ID, state, Dentition.PERMANENT, price, null, null, performedAt,
                List.of(new ProcedureToothDto(11, Set.of(ToothFace.M))), generate, null);
    }

    @Test
    void createsPlannedWithCatalogSnapshot() {
        PatientProcedureResponse r = service.create(COMPANY_ID, CONTACT_ID,
                request(ProcedureState.PLANNED, 15000, null, false), ACTOR_ID, false);
        assertThat(r.name()).isEqualTo("Restauração");
        assertThat(r.category()).isEqualTo("Dentística");
        assertThat(r.transaction()).isNull();
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void rejectsProductOrInactiveCatalogItem() {
        when(catalogItem.getType()).thenReturn(CatalogItemType.PRODUCT);
        assertThatThrownBy(() -> service.create(COMPANY_ID, CONTACT_ID,
                request(ProcedureState.PLANNED, 0, null, false), ACTOR_ID, false))
                .isInstanceOf(ContactValidationException.class);
    }

    @Test
    void doneWithGenerateCreatesPendingIncome() {
        PatientProcedureResponse r = service.create(COMPANY_ID, CONTACT_ID,
                request(ProcedureState.DONE, 15000, LocalDate.of(2026, 10, 22), true), ACTOR_ID, true);
        ArgumentCaptor<FinancialTransaction> captor = ArgumentCaptor.forClass(FinancialTransaction.class);
        verify(transactionRepository).save(captor.capture());
        FinancialTransaction tx = captor.getValue();
        assertThat(tx.getType()).isEqualTo(TransactionType.INCOME);
        assertThat(tx.getCategory()).isEqualTo(TransactionCategory.PROCEDURE);
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(tx.getAmountCents()).isEqualTo(15000);
        assertThat(tx.getContactId()).isEqualTo(CONTACT_ID);
        assertThat(tx.getDescription()).isEqualTo("Procedimento: Restauração – dentes 11");
        assertThat(tx.getDueDate()).isEqualTo(LocalDate.now());
        assertThat(r.transaction().status()).isEqualTo(TransactionStatus.PENDING);
    }

    @Test
    void generateWithoutFinancePermissionIsForbidden() {
        assertThatThrownBy(() -> service.create(COMPANY_ID, CONTACT_ID,
                request(ProcedureState.DONE, 15000, LocalDate.now(), true), ACTOR_ID, false))
                .isInstanceOf(CrmAccessDeniedException.class);
    }

    @Test
    void generateWithZeroPriceIsRejected() {
        assertThatThrownBy(() -> service.create(COMPANY_ID, CONTACT_ID,
                request(ProcedureState.DONE, 0, LocalDate.now(), true), ACTOR_ID, true))
                .isInstanceOf(ContactValidationException.class);
    }

    @Test
    void generateIgnoredWhenNotDone() {
        service.create(COMPANY_ID, CONTACT_ID, request(ProcedureState.PLANNED, 15000, null, true), ACTOR_ID, true);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void completeGeneratesTransaction() {
        PatientProcedure p = existing(ProcedureState.PLANNED);
        PatientProcedureResponse r = service.complete(COMPANY_ID, CONTACT_ID, p.getId(),
                new CompleteProcedureRequest(LocalDate.of(2026, 10, 22), null, true, LocalDate.of(2026, 11, 5)),
                ACTOR_ID, true);
        assertThat(r.state()).isEqualTo(ProcedureState.DONE);
        assertThat(r.transaction()).isNotNull();
    }

    @Test
    void updatingDoneOnlyAcceptsNotes() {
        PatientProcedure p = existing(ProcedureState.DONE);
        PatientProcedureRequest sameButNotes = new PatientProcedureRequest(ITEM_ID, ProcedureState.DONE,
                Dentition.PERMANENT, p.getPriceCents(), null, "ok", p.getPerformedAt(),
                List.of(new ProcedureToothDto(11, Set.of(ToothFace.M))), false, null);
        assertThat(service.update(COMPANY_ID, CONTACT_ID, p.getId(), sameButNotes, ACTOR_ID).notes())
                .isEqualTo("ok");
        PatientProcedureRequest changedPrice = new PatientProcedureRequest(ITEM_ID, ProcedureState.DONE,
                Dentition.PERMANENT, 99L, null, "ok", p.getPerformedAt(),
                List.of(new ProcedureToothDto(11, Set.of(ToothFace.M))), false, null);
        assertThatThrownBy(() -> service.update(COMPANY_ID, CONTACT_ID, p.getId(), changedPrice, ACTOR_ID))
                .isInstanceOf(ContactConflictException.class);
    }

    @Test
    void deleteWithPaidTransactionIsConflict() {
        PatientProcedure p = existingWithTransaction(TransactionStatus.PAID);
        assertThatThrownBy(() -> service.delete(COMPANY_ID, CONTACT_ID, p.getId(), ACTOR_ID))
                .isInstanceOf(ContactConflictException.class)
                .hasMessageContaining("pagamento registrado");
        verify(repository, never()).delete(any());
    }

    @Test
    void deleteWithPendingTransactionCancelsIt() {
        PatientProcedure p = existingWithTransaction(TransactionStatus.PENDING);
        service.delete(COMPANY_ID, CONTACT_ID, p.getId(), ACTOR_ID);
        assertThat(pendingTx.getStatus()).isEqualTo(TransactionStatus.CANCELLED);
        verify(transactionRepository).save(pendingTx);
        verify(repository).delete(p);
    }

    @Test
    void listFiltersByStateCategoryAndTooth() {
        PatientProcedure a = existing(ProcedureState.PLANNED);
        PatientProcedure b = PatientProcedure.create(COMPANY_ID, CONTACT_ID, ITEM_ID, "Limpeza",
                ProcedureState.PRE_EXISTING, Dentition.PERMANENT, 0, null, null, null, List.of(), null);
        when(repository.findByCompanyIdAndContactIdOrderByCreatedAtDesc(COMPANY_ID, CONTACT_ID))
                .thenReturn(List.of(a, b));
        assertThat(service.list(COMPANY_ID, CONTACT_ID, ProcedureState.PLANNED, null, null)).hasSize(1);
        assertThat(service.list(COMPANY_ID, CONTACT_ID, null, null, 11)).hasSize(1);
        assertThat(service.list(COMPANY_ID, CONTACT_ID, null, "Dentística", null)).hasSize(2);
        assertThat(service.list(COMPANY_ID, CONTACT_ID, null, "Outra", null)).isEmpty();
    }
```
Helpers no teste: `existing(state)` cria via `PatientProcedure.create(...)` com dente 11/face M, preço 15000 (para `DONE` passa `performedAt = LocalDate.of(2026,10,1)`), e faz `when(repository.findById(p.getId())).thenReturn(Optional.of(p))`; `existingWithTransaction(status)` cria um `DONE`, monta `pendingTx = FinancialTransaction.create(UUID.randomUUID(), COMPANY_ID, TransactionType.INCOME, "x", TransactionCategory.PROCEDURE, 15000, 0, PaymentMethod.OTHER, LocalDate.now(), CONTACT_ID, null, ITEM_ID, null, status == PAID ? LocalDate.now() : null, null)` (campo da classe), chama `p.linkTransaction(pendingTx.getId())` e `when(transactionRepository.findById(pendingTx.getId())).thenReturn(Optional.of(pendingTx))`.

- [ ] **Step 2: Rodar e ver falhar** — `mvn -q test -Dtest=PatientProcedureServiceTest` → erro de compilação.
- [ ] **Step 3: Implementar DTOs e UseCase** exatamente como em "Produces" (imports `jakarta.validation.constraints.*`, `jakarta.validation.Valid`).
- [ ] **Step 4: Implementar o service** — estrutura igual a `PatientEvolutionService` (construtor com os 6 colaboradores na ordem do teste, `try/finally` com `TenantContext`, `requireOwnedContact`, `requireOwned` lançando `PatientProcedureNotFoundException`, cache de nomes de usuário). Trechos específicos:

```java
    private CatalogItem requireService(UUID companyId, UUID catalogItemId) {
        CatalogItem item = catalogRepository.findById(catalogItemId)
                .filter(found -> found.getCompanyId().equals(companyId))
                .orElseThrow(() -> new ContactValidationException("Serviço não encontrado no catálogo."));
        if (item.getType() != CatalogItemType.SERVICE || !item.isActive()) {
            throw new ContactValidationException("Escolha um serviço ativo do catálogo.");
        }
        return item;
    }

    private void maybeGenerateTransaction(PatientProcedure procedure, boolean generate, LocalDate dueDate,
                                          UUID actorId, boolean canCreateFinance) {
        if (!generate || !procedure.isDone()) {
            return;
        }
        if (!canCreateFinance) {
            throw new CrmAccessDeniedException("Você não tem permissão para gerar lançamentos financeiros.");
        }
        if (procedure.getPriceCents() <= 0) {
            throw new ContactValidationException("Informe um valor maior que zero para gerar o lançamento.");
        }
        FinancialTransaction tx = FinancialTransaction.create(UUID.randomUUID(), procedure.getCompanyId(),
                TransactionType.INCOME, describe(procedure), TransactionCategory.PROCEDURE,
                procedure.getPriceCents(), 0L, PaymentMethod.OTHER, dueDate == null ? LocalDate.now() : dueDate,
                procedure.getContactId(), procedure.getProfessionalId(), procedure.getCatalogItemId(), null, null,
                actorId);
        transactionRepository.save(tx);
        procedure.linkTransaction(tx.getId());
    }

    static String describe(PatientProcedure procedure) {
        String teeth = procedure.getTeeth().isEmpty() ? "boca toda"
                : "dentes " + procedure.getTeeth().stream().map(t -> String.valueOf(t.tooth()))
                        .collect(Collectors.joining(", "));
        return "Procedimento: " + procedure.getNameSnapshot() + " – " + teeth;
    }
```
- `create`: `requireOwnedContact` → `requireService` → `PatientProcedure.create(companyId, contactId, item.getId(), item.getName(), ...teeth convertidos de DTO...)` → `maybeGenerateTransaction` → `repository.save` → auditoria `CREATE` "Procedimento registrado na ficha clínica." → resposta.
- `complete`: `requireOwned` → `procedure.complete(performedAt, professionalId)` → `maybeGenerateTransaction` → save → auditoria `UPDATE` "Procedimento marcado como realizado.".
- `update`: se `isDone()` aplica a regra de "só notas" descrita acima; senão `procedure.edit(...)`. Auditoria `UPDATE` "Procedimento atualizado.".
- `delete`: se `financialTransactionId != null`, carrega o lançamento; `PAID` → `ContactConflictException("Procedimento com pagamento registrado não pode ser excluído.")`; `PENDING` → `tx.cancel()` + `transactionRepository.save(tx)`. Depois `repository.delete(procedure)` e auditoria `DELETE` "Procedimento excluído da ficha clínica.".
- `list`: carrega por contato e filtra em memória: `state` igual; `tooth` presente em `getTeeth()`; `category` igual (case-insensitive) à categoria do item de catálogo. Para obter a categoria, use um `Map<UUID, String>` preenchido com `catalogRepository.findById` por `catalogItemId` distinto (o mesmo mapa alimenta o `category` da resposta).
- `toResponse`: `transaction` = `null` se não houver vínculo; senão `new TransactionRef(id, status)` lendo `transactionRepository.findById` (se o lançamento sumiu, `null`).

- [ ] **Step 5: Rodar e ver passar** — `mvn -q test -Dtest='PatientProcedure*Test'` → PASS.
- [ ] **Step 6: Commit** — `feat(pacientes): regras e lançamento financeiro dos procedimentos`.

---

### Task 5: Controller e tratamento de erro

**Files:**
- Create: `.../procedure/web/PatientProcedureController.java`
- Modify: `backend/src/main/java/com/becommerce/crm/shared/web/handler/GlobalExceptionHandler.java` (import + handler 404 ao lado de `handlePatientEvolutionNotFoundException`, ~linha 569)
- Test: `backend/src/test/java/com/becommerce/crm/clinical/procedure/web/PatientProcedureControllerTest.java`

- [ ] **Step 1: Teste que falha** — teste unitário puro do controller (sem Spring): mock de `PatientProcedureUseCase`, `CurrentUser` construído com `new CurrentUser(USER_ID, "a@b.c", COMPANY_ID, COMPANY_ID, List.of(), List.of("contact:update", "finance:create"), null, null, null, "Ana", "AGENT")`:

```java
    @Test
    void createPassesFinancePermission() {
        controller.create(COMPANY_ID, CONTACT_ID, request, principalWith("contact:update", "finance:create"));
        verify(useCase).create(COMPANY_ID, CONTACT_ID, request, USER_ID, true);
    }

    @Test
    void createWithoutFinancePermissionPassesFalse() {
        controller.create(COMPANY_ID, CONTACT_ID, request, principalWith("contact:update"));
        verify(useCase).create(COMPANY_ID, CONTACT_ID, request, USER_ID, false);
    }

    @Test
    void deleteReturnsNoContent() {
        assertThat(controller.delete(COMPANY_ID, CONTACT_ID, PROC_ID, principalWith("contact:update"))
                .getStatusCode().value()).isEqualTo(204);
    }
```
- [ ] **Step 2: Ver falhar** — `mvn -q test -Dtest=PatientProcedureControllerTest`.
- [ ] **Step 3: Implementar o controller** no formato de `PatientEvolutionController`, `ACCESS_MESSAGE = "Você só pode acessar procedimentos da sua própria empresa."`, base `/api/v1/companies/{companyId}/patients/{contactId}/procedures`:
  - `GET` (`contact:read`) com `@RequestParam(required = false) ProcedureState state, String category, Integer tooth`.
  - `POST` (`contact:update`) → 201.
  - `PATCH /{procedureId}` (`contact:update`).
  - `POST /{procedureId}/complete` (`contact:update`).
  - `DELETE /{procedureId}` (`contact:update`) → 204.
  - `private static boolean canCreateFinance(CurrentUser p) { return p.permissions() != null && p.permissions().contains("finance:create"); }`
- [ ] **Step 4: Handler 404** copiando `handlePatientEvolutionNotFoundException` com `PatientProcedureNotFoundException`.
- [ ] **Step 5: Ver passar** — `mvn -q test -Dtest='PatientProcedure*'`.
- [ ] **Step 6: Commit** — `feat(pacientes): endpoints da ficha clínica`.

---

### Task 6: Isolamento de tenant (IT, roda no CI)

**Files:**
- Test: `backend/src/test/java/com/becommerce/crm/clinical/procedure/infrastructure/persistence/PatientProcedureIsolationIT.java`

- [ ] **Step 1: Escrever o IT** copiando o esqueleto de `masterdata/anamnesis/infrastructure/persistence/AnamnesisIsolationIT.java` (container `postgres:17-alpine`, `agent-rls-bootstrap.sql`, role `crm_app`, `APP_USER = "crm_procedure_app_user"`, `TenantAwareDataSource`, `insertCompany`), trocando:
  - Tabelas mínimas antes da migration: `CREATE TABLE contacts (id UUID PRIMARY KEY)`, `CREATE TABLE users (id UUID PRIMARY KEY)`, `CREATE TABLE catalog_items (id UUID PRIMARY KEY)`, `CREATE TABLE financial_transactions (id UUID PRIMARY KEY)` (sem tabelas de RBAC — a V106 não semeia permissões).
  - Inserir 1 contato e 1 item de catálogo por tenant (como superusuário).
  - Aplicar `db/migration/V106__patient_procedures.sql` num único `execute`.
  - `seedProcedure(tenant, contact, item, name)` insere em `patient_procedures` (state `PLANNED`, dentition `PERMANENT`) e uma linha em `patient_procedure_teeth` (tooth 11, faces `'{M,O}'`) com o contexto do tenant.
  - Testes: `eachTenantSeesOnlyItsOwnProcedures` (string_agg dos `name_snapshot`), `eachTenantSeesOnlyItsOwnTeeth` (`count(*)` em `patient_procedure_teeth` = 1 por tenant), `noTenantContext_seesNothing`, `tenantB_cannotInsertIntoTenantA`, `doneRequiresPerformedAt` (UPDATE para `DONE` sem data → `SQLException`), `invalidFaceIsRejected` (faces `'{X}'` → `SQLException`).
- [ ] **Step 2: Compilar testes** — `mvn -q -DskipTests test-compile` (o IT roda só no CI com Testcontainers).
- [ ] **Step 3: Commit** — `test(pacientes): isolamento de tenant da ficha clínica`.

---

### Task 7: Frontend — tipos, utilitários, service e hooks

**Files:**
- Create: `frontend-refine/src/features/clinical/procedures/types/procedure.types.ts`
- Create: `frontend-refine/src/features/clinical/procedures/utils/teeth.ts`
- Create: `frontend-refine/src/features/clinical/procedures/services/procedure.service.ts`
- Create: `frontend-refine/src/features/clinical/procedures/hooks/useProcedures.ts`
- Test: `frontend-refine/src/__tests__/features/clinical/procedures/teeth.test.ts`

- [ ] **Step 1: Tipos**

```ts
export type ProcedureState = "PLANNED" | "DONE" | "PRE_EXISTING";
export type Dentition = "PERMANENT" | "DECIDUOUS";
export type ToothFace = "M" | "D" | "O" | "V" | "L";
export type TransactionStatus = "PENDING" | "PAID" | "CANCELLED";

export type ProcedureTooth = { tooth: number; faces: ToothFace[] };

export type PatientProcedureResponse = {
  id: string;
  contactId: string;
  catalogItemId: string;
  name: string;
  category: string | null;
  state: ProcedureState;
  dentition: Dentition;
  priceCents: number;
  professionalId: string | null;
  professionalName: string | null;
  notes: string | null;
  performedAt: string | null;
  teeth: ProcedureTooth[];
  transaction: { id: string; status: TransactionStatus } | null;
  createdAt: string;
  updatedAt: string;
};

export type PatientProcedureRequest = {
  catalogItemId: string;
  state: ProcedureState;
  dentition: Dentition;
  priceCents: number;
  professionalId?: string | null;
  notes?: string | null;
  performedAt?: string | null;
  teeth: ProcedureTooth[];
  generateTransaction: boolean;
  dueDate?: string | null;
};

export type CompleteProcedureRequest = {
  performedAt: string;
  professionalId?: string | null;
  generateTransaction: boolean;
  dueDate?: string | null;
};

export type ProcedureFilters = { state?: ProcedureState; category?: string; tooth?: number };
```

- [ ] **Step 2: Teste que falha** (`teeth.test.ts`):

```ts
import { describe, it, expect } from "vitest";
import {
  QUADRANTS,
  toothStates,
  dominantState,
} from "@/features/clinical/procedures/utils/teeth";
import type { PatientProcedureResponse } from "@/features/clinical/procedures/types/procedure.types";

const base = { teeth: [], dentition: "PERMANENT" } as unknown as PatientProcedureResponse;

describe("teeth utils", () => {
  it("lista os quadrantes de cada dentição", () => {
    expect(QUADRANTS.PERMANENT[0]).toEqual([18, 17, 16, 15, 14, 13, 12, 11]);
    expect(QUADRANTS.PERMANENT[1]).toEqual([21, 22, 23, 24, 25, 26, 27, 28]);
    expect(QUADRANTS.DECIDUOUS[3]).toEqual([81, 82, 83, 84, 85]);
  });

  it("prioriza A realizar > Realizado > Pré-existente", () => {
    expect(dominantState(["PRE_EXISTING", "DONE"])).toBe("DONE");
    expect(dominantState(["DONE", "PLANNED", "PRE_EXISTING"])).toBe("PLANNED");
    expect(dominantState([])).toBeNull();
  });

  it("agrega estado por dente e por face", () => {
    const list = [
      { ...base, state: "DONE", teeth: [{ tooth: 11, faces: ["M"] }] },
      { ...base, state: "PLANNED", teeth: [{ tooth: 11, faces: ["O"] }] },
      { ...base, state: "PRE_EXISTING", teeth: [] },
    ] as PatientProcedureResponse[];
    const map = toothStates(list, "PERMANENT");
    expect(map.get(11)?.state).toBe("PLANNED");
    expect(map.get(11)?.faces.M).toBe("DONE");
    expect(map.get(11)?.faces.O).toBe("PLANNED");
    expect(map.size).toBe(1);
  });
});
```
- [ ] **Step 3: Ver falhar** — `cd frontend-refine && npx vitest run --pool=threads src/__tests__/features/clinical/procedures/teeth.test.ts`.
- [ ] **Step 4: Implementar `utils/teeth.ts`**

```ts
import type {
  Dentition,
  PatientProcedureResponse,
  ProcedureState,
  ToothFace,
} from "../types/procedure.types";

const range = (quadrant: number, count: number, reverse: boolean) => {
  const teeth = Array.from({ length: count }, (_, i) => quadrant * 10 + i + 1);
  return reverse ? teeth.reverse() : teeth;
};

/** Quadrantes na ordem de exibição: sup. direito, sup. esquerdo, inf. esquerdo, inf. direito. */
export const QUADRANTS: Record<Dentition, number[][]> = {
  PERMANENT: [range(1, 8, true), range(2, 8, false), range(3, 8, false), range(4, 8, false)],
  DECIDUOUS: [range(5, 5, true), range(6, 5, false), range(7, 5, false), range(8, 5, false)],
};

export const FACES: ToothFace[] = ["M", "D", "O", "V", "L"];

export const STATE_LABELS: Record<ProcedureState, string> = {
  PLANNED: "A realizar",
  DONE: "Realizado",
  PRE_EXISTING: "Pré-existente",
};

export const DENTITION_LABELS: Record<Dentition, string> = {
  PERMANENT: "Permanente",
  DECIDUOUS: "Decídua",
};

export const STATE_FILL: Record<ProcedureState, string> = {
  PLANNED: "fill-destructive",
  DONE: "fill-primary",
  PRE_EXISTING: "fill-muted-foreground",
};

const PRIORITY: ProcedureState[] = ["PLANNED", "DONE", "PRE_EXISTING"];

export function dominantState(states: ProcedureState[]): ProcedureState | null {
  return PRIORITY.find((state) => states.includes(state)) ?? null;
}

export type ToothSummary = {
  state: ProcedureState;
  faces: Partial<Record<ToothFace, ProcedureState>>;
};

export function toothStates(
  procedures: PatientProcedureResponse[],
  dentition: Dentition,
): Map<number, ToothSummary> {
  const byTooth = new Map<number, { states: ProcedureState[]; faces: Map<ToothFace, ProcedureState[]> }>();
  for (const procedure of procedures) {
    if (procedure.dentition !== dentition) continue;
    for (const { tooth, faces } of procedure.teeth) {
      const entry = byTooth.get(tooth) ?? { states: [], faces: new Map() };
      entry.states.push(procedure.state);
      for (const face of faces) {
        entry.faces.set(face, [...(entry.faces.get(face) ?? []), procedure.state]);
      }
      byTooth.set(tooth, entry);
    }
  }
  const result = new Map<number, ToothSummary>();
  byTooth.forEach((entry, tooth) => {
    const faces: Partial<Record<ToothFace, ProcedureState>> = {};
    entry.faces.forEach((states, face) => {
      faces[face] = dominantState(states) as ProcedureState;
    });
    result.set(tooth, { state: dominantState(entry.states) as ProcedureState, faces });
  });
  return result;
}

export function formatTeeth(teeth: { tooth: number; faces: ToothFace[] }[]): string {
  if (teeth.length === 0) return "Boca toda";
  return teeth
    .map(({ tooth, faces }) => (faces.length ? `${tooth} (${faces.join("")})` : String(tooth)))
    .join(", ");
}
```
(O teste usa `dentition: "PERMANENT"` em todos os itens; Prettier pode quebrar a linha longa do `Map` — rode `npx prettier --write` no arquivo.)

- [ ] **Step 5: Service** — no formato de `evolution.service.ts`, base `` `/companies/${companyId}/patients/${contactId}/procedures` ``: `list(companyId, contactId, filters: ProcedureFilters)` (`api.get(url, { params: filters })`), `create`, `update(…, procedureId, payload)` com `api.patch`, `complete(…, procedureId, payload)` com `api.post(\`${url}/${procedureId}/complete\`, payload)`, `delete`.
- [ ] **Step 6: Hooks** (`useProcedures.ts`), usando `useMutationDefaults` como `useCatalog.ts`:

```ts
export const procedureKeys = {
  all: (companyId: string, contactId: string) => ["patient-procedures", companyId, contactId] as const,
};

export function useProcedures(companyId: string, contactId: string) {
  return useQuery({
    queryKey: procedureKeys.all(companyId, contactId),
    queryFn: () => procedureService.list(companyId, contactId, {}),
    enabled: Boolean(companyId && contactId),
  });
}
```
A lista é carregada **sem filtros** (o odontograma precisa de tudo); os filtros da Ficha geral são aplicados no cliente (Task 9). Mutations `useCreateProcedure`, `useUpdateProcedure` (`{ procedureId, payload }`), `useCompleteProcedure` (`{ procedureId, payload }`), `useDeleteProcedure` (`procedureId`), cada uma com `invalidateKeys: [procedureKeys.all(companyId, contactId), ["finance"]]` e mensagens "Procedimento salvo" / "Procedimento marcado como realizado" / "Procedimento excluído" e `errorMessage` "Não foi possível salvar o procedimento" / "...concluir..." / "...excluir...". Confira com `grep -rn "queryKey: \[\"finance" frontend-refine/src/features/finance/hooks` a chave raiz real do financeiro e use-a no lugar de `["finance"]` (anote se diferente).
- [ ] **Step 7: Ver passar + verificação** — vitest do arquivo, `npx tsc --noEmit`, Prettier nos 4 arquivos + teste.
- [ ] **Step 8: Commit** — `feat(pacientes): cliente da API da ficha clínica`.

---

### Task 8: Componente `Odontogram`

**Files:**
- Create: `frontend-refine/src/features/clinical/procedures/components/Odontogram.tsx`
- Test: `frontend-refine/src/__tests__/features/clinical/procedures/Odontogram.test.tsx`

**Interfaces — Produces:**
```ts
type OdontogramProps = {
  dentition: Dentition;
  onDentitionChange?: (d: Dentition) => void; // ausente = sem seletor
  summary: Map<number, ToothSummary>;          // de toothStates()
  selected: number[];                          // dentes destacados
  onToothClick: (tooth: number) => void;
  showLegend?: boolean;                        // padrão true
};
```

- [ ] **Step 1: Teste que falha**

```tsx
import { describe, it, expect, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { Odontogram } from "@/features/clinical/procedures/components/Odontogram";
import type { ToothSummary } from "@/features/clinical/procedures/utils/teeth";

describe("Odontogram", () => {
  it("pinta o dente pelo estado e informa o clique", () => {
    const onClick = vi.fn();
    const summary = new Map<number, ToothSummary>([[11, { state: "PLANNED", faces: { O: "PLANNED" } }]]);
    render(<Odontogram dentition="PERMANENT" summary={summary} selected={[]} onToothClick={onClick} />);
    const tooth = screen.getByRole("button", { name: "Dente 11, A realizar" });
    expect(tooth.querySelector(".fill-destructive")).toBeTruthy();
    fireEvent.click(tooth);
    expect(onClick).toHaveBeenCalledWith(11);
    expect(screen.getByRole("button", { name: "Dente 48" })).toBeTruthy();
  });

  it("troca para a dentição decídua", () => {
    const onChange = vi.fn();
    render(
      <Odontogram dentition="DECIDUOUS" onDentitionChange={onChange} summary={new Map()} selected={[]} onToothClick={vi.fn()} />,
    );
    expect(screen.getByRole("button", { name: "Dente 55" })).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Dente 18" })).toBeNull();
    fireEvent.click(screen.getByText("Permanente"));
    expect(onChange).toHaveBeenCalledWith("PERMANENT");
  });

  it("marca dentes selecionados", () => {
    render(<Odontogram dentition="PERMANENT" summary={new Map()} selected={[21]} onToothClick={vi.fn()} />);
    expect(screen.getByRole("button", { name: "Dente 21" }).getAttribute("aria-pressed")).toBe("true");
  });
});
```
- [ ] **Step 2: Ver falhar.**
- [ ] **Step 3: Implementar**

```tsx
"use client";

import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type { Dentition, ToothFace } from "../types/procedure.types";
import {
  DENTITION_LABELS,
  QUADRANTS,
  STATE_FILL,
  STATE_LABELS,
  type ToothSummary,
} from "../utils/teeth";

type OdontogramProps = {
  dentition: Dentition;
  onDentitionChange?: (dentition: Dentition) => void;
  summary: Map<number, ToothSummary>;
  selected: number[];
  onToothClick: (tooth: number) => void;
  showLegend?: boolean;
};

// Setores de um dente 40x40: centro = oclusal/incisal; bordas = V (cima), L (baixo), M/D (lados).
const FACE_PATHS: Record<ToothFace, string> = {
  V: "M0 0 L40 0 L30 10 L10 10 Z",
  L: "M0 40 L10 30 L30 30 L40 40 Z",
  M: "M0 0 L10 10 L10 30 L0 40 Z",
  D: "M40 0 L40 40 L30 30 L30 10 Z",
  O: "M10 10 L30 10 L30 30 L10 30 Z",
};

function Tooth({
  tooth,
  summary,
  selected,
  onClick,
}: {
  tooth: number;
  summary?: ToothSummary;
  selected: boolean;
  onClick: () => void;
}) {
  const label = summary ? `Dente ${tooth}, ${STATE_LABELS[summary.state]}` : `Dente ${tooth}`;
  return (
    <button
      type="button"
      aria-label={label}
      aria-pressed={selected}
      onClick={onClick}
      className={cn(
        "flex flex-col items-center gap-1 rounded-md p-1 hover:bg-muted",
        selected && "ring-2 ring-ring",
      )}
    >
      <svg viewBox="0 0 40 40" className="h-8 w-8" aria-hidden>
        {(Object.keys(FACE_PATHS) as ToothFace[]).map((face) => {
          const faceState = summary?.faces[face];
          return (
            <path
              key={face}
              d={FACE_PATHS[face]}
              className={cn("stroke-border", faceState ? STATE_FILL[faceState] : "fill-card")}
            />
          );
        })}
        {summary && Object.keys(summary.faces).length === 0 && (
          <circle cx="20" cy="20" r="6" className={STATE_FILL[summary.state]} />
        )}
      </svg>
      <span className="text-xs text-muted-foreground">{tooth}</span>
    </button>
  );
}

export function Odontogram({
  dentition,
  onDentitionChange,
  summary,
  selected,
  onToothClick,
  showLegend = true,
}: OdontogramProps) {
  const [upperRight, upperLeft, lowerLeft, lowerRight] = QUADRANTS[dentition];
  const row = (teeth: number[]) =>
    teeth.map((tooth) => (
      <Tooth
        key={tooth}
        tooth={tooth}
        summary={summary.get(tooth)}
        selected={selected.includes(tooth)}
        onClick={() => onToothClick(tooth)}
      />
    ));

  return (
    <div className="space-y-3">
      {onDentitionChange && (
        <div className="flex gap-2">
          {(Object.keys(DENTITION_LABELS) as Dentition[]).map((value) => (
            <Button
              key={value}
              type="button"
              size="sm"
              variant={value === dentition ? "default" : "outline"}
              onClick={() => onDentitionChange(value)}
            >
              {DENTITION_LABELS[value]}
            </Button>
          ))}
        </div>
      )}
      <div className="overflow-x-auto">
        <div className="inline-grid min-w-max grid-cols-2 gap-x-4 gap-y-2">
          <div className="flex justify-end border-b border-r pb-2 pr-2">{row(upperRight)}</div>
          <div className="flex border-b pb-2">{row(upperLeft)}</div>
          <div className="flex justify-end border-r pr-2">{row(lowerRight.slice().reverse())}</div>
          <div className="flex">{row(lowerLeft)}</div>
        </div>
      </div>
      {showLegend && (
        <div className="flex flex-wrap gap-4 text-xs text-muted-foreground">
          {(Object.keys(STATE_LABELS) as (keyof typeof STATE_LABELS)[]).map((state) => (
            <span key={state} className="flex items-center gap-1">
              <svg viewBox="0 0 10 10" className="h-3 w-3" aria-hidden>
                <circle cx="5" cy="5" r="5" className={STATE_FILL[state]} />
              </svg>
              {STATE_LABELS[state]}
            </span>
          ))}
        </div>
      )}
    </div>
  );
}
```
(Quadrante inferior direito: `QUADRANTS[...][3]` vem 41→48; exibimos 48→41 à esquerda da linha média, por isso o `reverse`.)

- [ ] **Step 4: Ver passar + lint/tsc/Prettier.**
- [ ] **Step 5: Commit** — `feat(pacientes): odontograma da ficha clínica`.

---

### Task 9: Drawer, diálogo de conclusão e lista

**Files:**
- Create: `.../procedures/components/ProcedureDrawer.tsx`, `CompleteProcedureDialog.tsx`, `ProcedureList.tsx`
- Test: `frontend-refine/src/__tests__/features/clinical/procedures/ProcedureDrawer.test.tsx`, `ProcedureList.test.tsx`

**Interfaces — Consumes:** `useCatalog(companyId, { type: "SERVICE", active: true, pageSize: 100 })` (`@/features/masterdata/catalog/hooks/useCatalog`, retorna `{ data: { content: CatalogItem[] } }`), `useMembers(companyId)` (`@/features/identity/members/hooks/useMembers`, itens `{ userId, name }`), `useAuthorization().can(permission)` (`@/features/identity/auth/hooks/useAuthorization`), hooks da Task 7, `Odontogram` da Task 8. UI: `Sheet`, `Dialog`, `AlertDialog`, `Select`, `Input`, `Label`, `Textarea`, `Checkbox`, `Button`, `Badge`, `Table` de `@/components/ui/*`.

**Interfaces — Produces:**
```ts
ProcedureDrawer({ companyId, contactId, open, onOpenChange, procedure?: PatientProcedureResponse | null })
CompleteProcedureDialog({ companyId, contactId, procedure: PatientProcedureResponse | null, onOpenChange })
ProcedureList({ companyId, contactId, procedures, toothFilter: number | null, onClearTooth, onEdit, onComplete })
```

- [ ] **Step 1: Testes que falham**

`ProcedureDrawer.test.tsx` (mocks com `vi.mock` para `useCatalog`, `useMembers`, `useAuthorization` com `can` controlado por estado hoisted, e `useProcedures` com `mutate: vi.fn()`):
```tsx
  it("checkbox de lançamento só aparece em Realizado e com finance:create", () => {
    permissions.value = ["finance:create"];
    renderDrawer(null);
    expect(screen.queryByText("Gerar lançamento financeiro")).toBeNull();
    fireEvent.click(screen.getByRole("radio", { name: "Realizado" }));
    expect(screen.getByText("Gerar lançamento financeiro")).toBeTruthy();
    permissions.value = [];
    renderDrawer(null);
    fireEvent.click(screen.getAllByRole("radio", { name: "Realizado" }).at(-1)!);
    expect(screen.getAllByText("Gerar lançamento financeiro")).toHaveLength(1);
  });

  it("procedimento realizado só deixa editar observações", () => {
    renderDrawer(makeProcedure({ state: "DONE", performedAt: "2026-10-22" }));
    expect(screen.getByText("Procedimento realizado: só as observações podem ser alteradas.")).toBeTruthy();
    expect((screen.getByLabelText("Valor (R$)") as HTMLInputElement).disabled).toBe(true);
    expect((screen.getByLabelText("Observações") as HTMLTextAreaElement).disabled).toBe(false);
  });

  it("selecionar dente mostra os chips de face", () => {
    renderDrawer(null);
    fireEvent.click(screen.getByRole("button", { name: "Dente 11" }));
    expect(screen.getByRole("button", { name: "Face M do dente 11" })).toBeTruthy();
  });
```
O estado no drawer usa `RadioGroup` (`@/components/ui/radio-group`) com `RadioGroupItem` + `Label htmlFor`, e as opções "A realizar", "Realizado", "Pré-existente" (em edição de procedimento `PLANNED`/`PRE_EXISTING`, a opção "Realizado" fica desabilitada — concluir é pelo diálogo).

`ProcedureList.test.tsx`:
```tsx
  it("mostra ações por estado e filtra por estado", () => {
    renderList([
      makeProcedure({ id: "p1", name: "Restauração", state: "PLANNED" }),
      makeProcedure({ id: "p2", name: "Limpeza", state: "DONE", performedAt: "2026-10-01" }),
    ]);
    expect(screen.getAllByText("Marcar como realizado")).toHaveLength(1);
    fireEvent.click(screen.getByRole("button", { name: "Filtrar: Realizado" }));
    expect(screen.queryByText("Restauração")).toBeNull();
    expect(screen.getByText("Limpeza")).toBeTruthy();
  });

  it("exclusão avisa cancelamento do lançamento pendente", () => {
    renderList([makeProcedure({ state: "DONE", performedAt: "2026-10-01", transaction: { id: "t1", status: "PENDING" } })]);
    fireEvent.click(screen.getByRole("button", { name: "Excluir procedimento" }));
    expect(screen.getByText("O lançamento financeiro pendente deste procedimento será cancelado.")).toBeTruthy();
  });

  it("procedimento com pagamento não pode ser excluído", () => {
    renderList([makeProcedure({ state: "DONE", performedAt: "2026-10-01", transaction: { id: "t1", status: "PAID" } })]);
    const button = screen.getByRole("button", { name: "Excluir procedimento" }) as HTMLButtonElement;
    expect(button.disabled).toBe(true);
    expect(button.getAttribute("title")).toBe("Procedimento com pagamento registrado não pode ser excluído");
  });

  it("filtro por dente vindo do odontograma", () => {
    renderList(
      [
        makeProcedure({ id: "p1", name: "Restauração", teeth: [{ tooth: 11, faces: ["M"] }] }),
        makeProcedure({ id: "p2", name: "Limpeza", teeth: [] }),
      ],
      11,
    );
    expect(screen.getByText("Restauração")).toBeTruthy();
    expect(screen.queryByText("Limpeza")).toBeNull();
    expect(screen.getByText("Dente 11")).toBeTruthy();
  });
```
`makeProcedure(overrides)` devolve um `PatientProcedureResponse` completo (id "p1", name "Restauração", category "Dentística", state "PLANNED", dentition "PERMANENT", priceCents 15000, teeth `[{tooth: 11, faces: ["M"]}]`, transaction null, datas fixas). `renderList(procedures, tooth = null)` renderiza `ProcedureList` com `onEdit/onComplete/onClearTooth = vi.fn()`.

- [ ] **Step 2: Ver falhar.**
- [ ] **Step 3: Implementar `ProcedureDrawer`**
  - `Sheet` lateral (`SheetContent side="right" className="w-full overflow-y-auto sm:max-w-lg"`), título "Novo procedimento" / "Editar procedimento".
  - Estado local inicializado de `procedure` ou padrão (`state "PLANNED"`, `dentition "PERMANENT"`, `teeth []`, `priceCents 0`, `generateTransaction false`, `performedAt` hoje em `yyyy-MM-dd`, `dueDate` hoje); reinicia quando `open`/`procedure` mudam (`useEffect`).
  - Campos com `Label htmlFor`: "Estado" (RadioGroup), "Serviço" (`Select` com serviços do catálogo; ao escolher, `priceCents = Math.round((item.price ?? 0) * 100)`; desabilitado em edição), "Valor (R$)" (`Input type="number" step="0.01"`, converte para centavos), "Data de realização" (`Input type="date"`, só quando estado `DONE`), "Dentes" (`<Odontogram dentition onDentitionChange summary={new Map()} selected={teeth.map(t => t.tooth)} onToothClick={toggleTooth} showLegend={false} />`; trocar a dentição limpa os dentes), chips de face por dente selecionado (`Button size="sm" variant={on ? "default" : "outline"} aria-label={\`Face ${face} do dente ${tooth}\`}`), "Profissional" (`Select` de `useMembers`), "Observações" (`Textarea`), e — só se `state === "DONE" && can("finance:create")` — `Checkbox` "Gerar lançamento financeiro" + "Vencimento" (`Input type="date"`) quando marcado.
  - `const locked = procedure?.state === "DONE"`: todos os campos exceto Observações recebem `disabled={locked}` e aparece o aviso "Procedimento realizado: só as observações podem ser alteradas.".
  - Salvar: monta `PatientProcedureRequest` (sempre completo; em `locked`, com os valores originais) e chama `create.mutate` ou `update.mutate({ procedureId, payload })`; `onSuccess` fecha o drawer. Botão "Salvar" desabilitado sem serviço escolhido ou com `isPending`.
- [ ] **Step 4: Implementar `CompleteProcedureDialog`** — `Dialog` aberto quando `procedure != null`; campos "Data de realização" (hoje), "Profissional" (pré-selecionado com `procedure.professionalId`), checkbox "Gerar lançamento financeiro" + "Vencimento" (só com `can("finance:create")`); botão "Confirmar" chama `useCompleteProcedure().mutate({ procedureId, payload }, { onSuccess: () => onOpenChange(false) })`.
- [ ] **Step 5: Implementar `ProcedureList`**
  - Card "Ficha geral". Filtros: botões de estado ("Todos" + os 3 rótulos, `aria-label={\`Filtrar: ${label}\`}`, `variant` default quando ativo) e `Select` "Tipo" com as categorias distintas não nulas da lista (+ "Todos os tipos"). Quando `toothFilter != null`, mostra `Badge` "Dente N" com botão "Limpar" (`onClearTooth`).
  - Filtragem no cliente: estado, categoria e `toothFilter` (`p.teeth.some(t => t.tooth === toothFilter)`).
  - `Table` com colunas Procedimento (nome + categoria em `text-muted-foreground`), Dentes (`formatTeeth`), Estado (`Badge` com `STATE_LABELS`), Valor (`(priceCents / 100).toLocaleString("pt-BR", { style: "currency", currency: "BRL" })`), Data (`performedAt` formatada `dd/MM/yyyy` ou "—"), Profissional, Lançamento ("Pendente"/"Pago"/"Cancelado"/"—").
  - Ações: "Editar" (`onEdit(p)`), "Marcar como realizado" (só `PLANNED`, `onComplete(p)`), botão ícone `aria-label="Excluir procedimento"`: desabilitado com `title="Procedimento com pagamento registrado não pode ser excluído"` quando `transaction?.status === "PAID"`; senão abre `AlertDialog` "Excluir procedimento?" com descrição "Esta ação não pode ser desfeita." e, se `transaction?.status === "PENDING"`, o parágrafo "O lançamento financeiro pendente deste procedimento será cancelado."; confirmar chama `useDeleteProcedure().mutate(p.id)`.
  - Estado vazio: "Nenhum procedimento registrado." (ou "Nenhum procedimento para este filtro." quando há filtro).
- [ ] **Step 6: Ver passar + lint/tsc/Prettier** nos 3 componentes e 2 testes.
- [ ] **Step 7: Commit** — `feat(pacientes): cadastro e lista de procedimentos da ficha clínica`.

---

### Task 10: Aba "Ficha clínica" e rota

**Files:**
- Create: `.../procedures/components/PatientClinicalChartTab.tsx`
- Create: `frontend-refine/src/app/(dashboard)/patients/[id]/ficha-clinica/page.tsx`
- Modify: `frontend-refine/src/features/clinical/patients/components/PatientTabs.tsx` (array `TABS`)
- Test: `frontend-refine/src/__tests__/features/clinical/procedures/PatientClinicalChartTab.test.tsx`

- [ ] **Step 1: Teste que falha** (mocks de `useProcedures` devolvendo uma lista com um `PLANNED` no dente 11, de `useAuthorization` com `can: () => true`, e dos componentes pesados `ProcedureDrawer`/`CompleteProcedureDialog` como stubs `() => null`):

```tsx
  it("clicar no dente filtra a ficha geral e clicar de novo limpa", () => {
    render(<PatientClinicalChartTab companyId="co1" contactId="c1" />);
    expect(screen.getByText("Limpeza")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Dente 11, A realizar" }));
    expect(screen.queryByText("Limpeza")).toBeNull();
    fireEvent.click(screen.getByRole("button", { name: "Dente 11, A realizar" }));
    expect(screen.getByText("Limpeza")).toBeTruthy();
  });

  it("botão de novo procedimento exige contact:update", () => {
    can.value = () => false;
    render(<PatientClinicalChartTab companyId="co1" contactId="c1" />);
    expect(screen.queryByText("+ Novo procedimento")).toBeNull();
  });
```
(lista mockada: "Restauração" PLANNED dente 11 e "Limpeza" PRE_EXISTING sem dentes.)

- [ ] **Step 2: Ver falhar.**
- [ ] **Step 3: Implementar a aba**

```tsx
"use client";

import { useMemo, useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";
import { useProcedures } from "../hooks/useProcedures";
import type { Dentition, PatientProcedureResponse } from "../types/procedure.types";
import { toothStates } from "../utils/teeth";
import { CompleteProcedureDialog } from "./CompleteProcedureDialog";
import { Odontogram } from "./Odontogram";
import { ProcedureDrawer } from "./ProcedureDrawer";
import { ProcedureList } from "./ProcedureList";

export function PatientClinicalChartTab({
  companyId,
  contactId,
}: {
  companyId: string;
  contactId: string;
}) {
  const { can } = useAuthorization();
  const { data: procedures = [], isLoading } = useProcedures(companyId, contactId);
  const [dentition, setDentition] = useState<Dentition>("PERMANENT");
  const [toothFilter, setToothFilter] = useState<number | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editing, setEditing] = useState<PatientProcedureResponse | null>(null);
  const [completing, setCompleting] = useState<PatientProcedureResponse | null>(null);
  const summary = useMemo(() => toothStates(procedures, dentition), [procedures, dentition]);

  if (isLoading) return <Skeleton className="h-64 w-full" />;

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0">
          <CardTitle className="text-base">Odontograma</CardTitle>
          {can("contact:update") && (
            <Button
              size="sm"
              onClick={() => {
                setEditing(null);
                setDrawerOpen(true);
              }}
            >
              + Novo procedimento
            </Button>
          )}
        </CardHeader>
        <CardContent>
          <Odontogram
            dentition={dentition}
            onDentitionChange={(value) => {
              setDentition(value);
              setToothFilter(null);
            }}
            summary={summary}
            selected={toothFilter == null ? [] : [toothFilter]}
            onToothClick={(tooth) => setToothFilter((current) => (current === tooth ? null : tooth))}
          />
        </CardContent>
      </Card>
      <ProcedureList
        companyId={companyId}
        contactId={contactId}
        procedures={procedures}
        toothFilter={toothFilter}
        onClearTooth={() => setToothFilter(null)}
        onEdit={(procedure) => {
          setEditing(procedure);
          setDrawerOpen(true);
        }}
        onComplete={setCompleting}
      />
      <ProcedureDrawer
        companyId={companyId}
        contactId={contactId}
        open={drawerOpen}
        onOpenChange={setDrawerOpen}
        procedure={editing}
      />
      <CompleteProcedureDialog
        companyId={companyId}
        contactId={contactId}
        procedure={completing}
        onOpenChange={(open) => !open && setCompleting(null)}
      />
    </div>
  );
}
```
As ações de editar/concluir/excluir na `ProcedureList` também só aparecem com `can("contact:update")` (ajuste na lista: receber `canEdit: boolean` — se adicionar a prop, atualize o teste da Task 9 passando `canEdit`).

- [ ] **Step 4: Página** `app/(dashboard)/patients/[id]/ficha-clinica/page.tsx` — cópia de `evolucoes/page.tsx` sem `usePatient`/`patient`: lê `params.id`, `useCompanyId()`, mostra `"Carregando..."` sem `companyId` e renderiza `<PatientClinicalChartTab companyId={companyId} contactId={contactId} />`.
- [ ] **Step 5: Aba** — em `PatientTabs.tsx`, inserir `{ label: "Ficha clínica", segment: "ficha-clinica" },` logo após `{ label: "Consultas", segment: "" },`. Rode `grep -rn "Consultas" src/__tests__/features/clinical/patients` e, se algum teste conferir a lista/ordem das abas, inclua "Ficha clínica" nele.
- [ ] **Step 6: Verificação completa do frontend** — `npm run lint && npx tsc --noEmit && npx vitest run --pool=threads` + Prettier nos arquivos da task.
- [ ] **Step 7: Commit** — `feat(pacientes): aba Ficha clínica na ficha do paciente`.

---

### Task 11: Fechamento

**Files:**
- Modify: `docs/superpowers/specs/2026-10-22-ficha-clinica-design.md` (seção 3: rotas reais; 422 → 400; aba após "Consultas"; `PaymentMethod.OTHER` e categoria `PROCEDURE`; status no topo "Implementada")
- Modify: `docs/superpowers/specs/2026-10-12-ficha-do-paciente-design.md` (linha da Fase 3: "3a implementada (branch `feature/pacientes-fase3a`), 3b pendente")
- Modify: este plano (seção "Resultado final")

- [ ] **Step 1: Atualizar os dois specs** como descrito acima.
- [ ] **Step 2: Rodar tudo** e colar as últimas linhas em "Resultado final":
  - `cd backend && mvn -q test`
  - `cd frontend-refine && npm run lint && npx tsc --noEmit && npx vitest run --pool=threads`
  - `npx prettier --check --end-of-line auto $(git diff --name-only origin/crm-improvements-deploy-phase -- frontend-refine | sed 's#^frontend-refine/##')` (de dentro de `frontend-refine`)
- [ ] **Step 3: Commit** — `docs(pacientes): marca a Fase 3a da ficha como implementada`.

## Critérios de aceite

1. Aba "Ficha clínica" aparece após "Consultas" para quem tem `contact:read`.
2. Odontograma permanente/decídua com cor por prioridade A realizar > Realizado > Pré-existente e faces pintadas; clique filtra/limpa a Ficha geral.
3. Criar procedimento com serviço do catálogo (preço sugerido editável), vários dentes com faces, ou boca toda.
4. Realizado com "Gerar lançamento" cria receita `PENDING` categoria Procedimento ligada ao paciente; sem `finance:create` a opção não aparece e a API devolve 403.
5. Realizado só aceita editar observações (409 caso contrário); concluir só a partir de A realizar.
6. Excluir com lançamento pago → 409; com pendente → lançamento cancelado.
7. RLS: `PatientProcedureIsolationIT` verde no CI.
8. Backend, frontend (lint/tsc/vitest), Prettier e CI (backend, frontend, E2E) verdes.

## Fora de escopo (NÃO implemente)

Orçamentos e `budget_id` (Fase 3b); anotações por dente; impressão/PDF do odontograma; histórico de alterações; dentes supranumerários.

## Divergências encontradas

Ver ledger do SDD; divergências registradas nos relatórios das tasks (chave de cache do financeiro ['finance', companyId]; canEdit em ProcedureList; useId nos campos do drawer).

## Resultado final

Verificação executada na Task 11 (branch `feature/pacientes-fase3a`, após `fe276615`):

- `cd backend && mvn -q test`: sem falhas, `EXIT=0` (log completo em `.superpowers/sdd/2026-10-22-ficha-clinica-fase3a/task-11-backend.log`; as linhas de stack trace no log são saída de testes que registram exceções esperadas).
- `cd frontend-refine && npm run lint`: `EXIT=0` (somente warnings preexistentes, sem erros).
- `npx tsc --noEmit`: `EXIT=0`, sem erros.
- `npx vitest run --pool=threads`:
  ```
  Test Files  1 failed | 110 passed (111)
       Tests  5 failed | 569 passed (574)
  ```
  As 5 falhas são todas em `src/__tests__/components/layout/SidebarGroup.test.tsx` (ambiente local: `node_modules` emprestado via junction; não relacionado a esta feature). Registrado como está, sem correção.
- Prettier (`npx prettier --check --end-of-line auto` nos 16 arquivos de `frontend-refine` alterados desde `origin/crm-improvements-deploy-phase`): "All matched files use Prettier code style!".

CI do GitHub (backend, frontend, E2E) é a verificação de referência para este fechamento e não foi consultado nesta execução.
