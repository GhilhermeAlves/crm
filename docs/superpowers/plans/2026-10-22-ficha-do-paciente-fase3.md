# Ficha do Paciente (Fase 3) — Ficha clínica e Orçamentos — Implementation Plan

> **Para o agente executor:** este documento é uma ORDEM, não uma sugestão. Execute task a task, **na ordem**, marcando cada checkbox (`- [ ]` → `- [x]`). Ao fim de cada task rode a verificação indicada; só faça o commit se ela passar. **Um commit por task.** Não faça push, merge, rebase nem PR. Não altere nada fora dos arquivos listados na task, exceto quando a task mandar. Se algo do plano não bater com o código real (nome de classe, método, assinatura), **pare, não invente**: escreva a divergência no fim deste arquivo, em "Divergências encontradas", e siga a solução mais próxima do padrão existente citado na task.

**Goal:** Adicionar à ficha do paciente a aba **Ficha clínica** (odontograma + procedimentos por dente) e a aba **Orçamentos** (rascunho → aprovado pelo atendente → parcelas no financeiro), mais a importação dos 341 procedimentos padrão no Catálogo.

**Spec (leia inteiro antes da Task 1):** `docs/superpowers/specs/2026-10-22-ficha-clinica-orcamentos-design.md`. Contexto: `docs/superpowers/plans/2026-10-22-ficha-do-paciente-fase2.md` (mesmo estilo e padrões).

**Tech Stack:** Spring Boot hexagonal (`backend/`), Next.js App Router + TanStack Query + shadcn/ui (`frontend-refine/`), PostgreSQL + Flyway, JUnit 5 + Mockito, Vitest.

**Entrega em duas partes:** Parte A (Tasks 1–7, PR "3A") e Parte B (Tasks 8–14, PR "3B"). A Parte B só começa depois que a Parte A estiver commitada.

---

## Global Constraints (valem para TODAS as tasks)

1. **Pré-requisito:** a Fase 2 (`feature/pacientes-fase2`) precisa estar **concluída** (Tasks 8–12 do plano dela marcadas). Antes da Task 1 rode `git log --oneline -3 feature/pacientes-fase2` e confirme que o último commit é `docs(pacientes): marca a Fase 2 da ficha como implementada`. Se não for, **pare** e avise.
2. **Branch:** `feature/pacientes-fase3` (criada a partir de `feature/pacientes-fase2` em 22937315, com o spec e este plano). Antes da Task 1, traga o resto da Fase 2: `git rebase feature/pacientes-fase2`; se houver conflito, `git rebase --abort`, pare e avise. Confirme com `git branch --show-current`.
3. **Migrations:** Parte A = `V106__patient_procedures.sql`; Parte B = `V107__budgets.sql`. Antes de criar, `ls backend/src/main/resources/db/migration | tail -3`; se o número já existir, use o próximo livre e registre em "Divergências encontradas". **Nunca editar migrations antigas.** Sem Docker local: revise à mão que nomes de tabela/índice/constraint não colidem (`git grep -n "<nome>" backend/src/main/resources/db/migration` vazio).
4. **Padrão RLS em toda tabela nova** (copie da `V105__patient_clinical_record.sql`): `company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE`, `ENABLE` + `FORCE ROW LEVEL SECURITY`, `CREATE POLICY tenant_isolation_policy ... USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id())`, `GRANT SELECT, INSERT, UPDATE, DELETE ON <tabela> TO crm_app`.
5. **Backend segue o pacote `clinical/evolution`** (Fase 2) como modelo exato: `domain/` (`create` + `reconstitute`, validação com `ContactValidationException`, `XxxNotFoundException` em `domain/exception`), `application/dto`, `application/port/in` (UseCase), `application/port/out` (Repository), `application/service`, `infrastructure/persistence` (`JpaEntity`, `JpaRepository`, `RepositoryImpl`), `web/Controller`. Serviços: `TenantContext.setCompanyId` / `clear()` em `finally`, `requireOwnedContact`, auditoria via `TenantAuditRecorder.record(..., AuditModule.CONTACTS, ...)`. Exceções novas registradas no `GlobalExceptionHandler` como a `PatientEvolutionNotFoundException` (404) e conflitos de estado como 409 (veja como a Fase 2 trata "evolução finalizada").
6. **Rotas e permissões:** só `/api/v1/companies/{companyId}/patients/{contactId}/...`. **Nenhuma permissão nova**: leitura `contact:read`, escrita **e aprovação de orçamento** `contact:update`. A importação de procedimentos usa `catalog:manage`. As rotas `/patients/**` já são barradas pelo `ModuleGuardInterceptor` quando o módulo PATIENTS está desligado.
7. **Regra de negócio central:** procedimento da ficha **nunca** cria lançamento financeiro. Só a aprovação de orçamento cria `financial_transactions`.
8. **Frontend:** código novo em `frontend-refine/src/features/clinical/procedures/` e `frontend-refine/src/features/clinical/budgets/`, seguindo `features/clinical/evolutions` (service com `api` de `@/lib/api`, hooks TanStack Query com `useMutationDefaults`, shadcn de `@/components/ui/*`). Textos pt-BR. Sem cores hex: só tokens do tema; as cores de estado do odontograma usam `text-primary` (A realizar), `text-emerald-600 dark:text-emerald-400` (Realizado — mesmo token já usado no app para "pago"; confirme com `git grep -n "emerald-600" frontend-refine/src | head -3`) e `text-muted-foreground` (Pré-existente).
9. **Estilo:** Checkstyle até 200 colunas. Prettier `printWidth 100`, aspas duplas, `trailingComma all`. Todo arquivo do frontend alterado passa em `npx prettier --check --end-of-line auto <arquivos>`.
10. **Verificações:**
    - Backend: `cd backend && mvn -q test`.
    - Frontend: `cd frontend-refine && npm run lint && npx tsc --noEmit && npx prettier --check --end-of-line auto <arquivos alterados> && npx vitest run --pool=threads <pasta de testes da task>`. Testes **sem** `user-event`/jest-dom: `fireEvent` + `screen.getByText(...)`.
11. **Commits:** Conventional pt-BR (`feat(pacientes): ...`), terminando com linha em branco e a linha de co-autoria do agente executor.

---

# PARTE A — Catálogo de procedimentos + Ficha clínica

## Task 1: Migration `V106__patient_procedures.sql`

**Files:** Create `backend/src/main/resources/db/migration/V106__patient_procedures.sql`

- [ ] Cabeçalho de comentário no estilo da V105: tabela de procedimentos do paciente (odontograma), sem permissões novas, `budget_id` sem FK (a FK chega na V107).
- [ ] SQL:

```sql
CREATE TABLE patient_procedures (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id           UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    contact_id           UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    catalog_item_id      UUID REFERENCES catalog_items(id) ON DELETE SET NULL,
    name                 VARCHAR(160) NOT NULL,
    status               VARCHAR(15) NOT NULL DEFAULT 'TO_DO',
    dentition            VARCHAR(10) NOT NULL DEFAULT 'PERMANENT',
    teeth                SMALLINT[] NOT NULL DEFAULT '{}',
    faces                VARCHAR(1)[] NOT NULL DEFAULT '{}',
    price_cents          BIGINT,
    professional_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    notes                TEXT,
    budget_id            UUID,
    performed_at         DATE,
    created_by           UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_patient_procedures_status CHECK (status IN ('TO_DO', 'DONE', 'PRE_EXISTING')),
    CONSTRAINT chk_patient_procedures_dentition CHECK (dentition IN ('PERMANENT', 'DECIDUOUS')),
    CONSTRAINT chk_patient_procedures_price CHECK (price_cents IS NULL OR price_cents >= 0),
    CONSTRAINT chk_patient_procedures_name CHECK (length(trim(name)) > 0),
    CONSTRAINT chk_patient_procedures_done CHECK (status = 'DONE' OR performed_at IS NULL)
);
CREATE INDEX idx_patient_procedures_contact ON patient_procedures (company_id, contact_id, created_at DESC);
CREATE INDEX idx_patient_procedures_budget ON patient_procedures (budget_id) WHERE budget_id IS NOT NULL;

ALTER TABLE patient_procedures ENABLE ROW LEVEL SECURITY;
ALTER TABLE patient_procedures FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON patient_procedures
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON patient_procedures TO crm_app;
```

- [ ] Verificação: `git grep -n "patient_procedures" backend/src/main/resources/db/migration` só mostra a V106. Commit: `feat(pacientes): migration V106 com procedimentos do paciente`

## Task 2: Backend — importar procedimentos padrão no Catálogo

**Files:**
- Create: `backend/src/main/resources/catalog/procedimentos-odontologicos.tsv` (cópia byte a byte de `docs/superpowers/specs/2026-10-22-procedimentos-padrao.tsv`)
- Create: `backend/src/main/java/com/becommerce/crm/masterdata/catalog/application/service/DefaultProceduresCatalog.java`
- Create: `backend/src/main/java/com/becommerce/crm/masterdata/catalog/application/dto/ImportDefaultProceduresResponse.java`
- Modify: `CatalogItemRepository` (porta) + implementação JPA, `CatalogService`, `CatalogController`
- Test: `backend/src/test/java/com/becommerce/crm/masterdata/catalog/application/service/DefaultProceduresCatalogTest.java` e caso novo em `CatalogServiceTest` (crie se não existir, no padrão de `PatientEvolutionServiceTest`)

**Interfaces:**
- Produces: `POST /api/v1/companies/{companyId}/catalog/import-default-procedures` → `200 {"created": int, "skipped": int}`; categoria gravada: `"Procedimento odontológico"`.

- [ ] Teste do parser (falha primeiro):

```java
class DefaultProceduresCatalogTest {
    @Test
    void carregaOs341ProcedimentosComPrecoEmReais() {
        List<DefaultProceduresCatalog.Entry> entries = DefaultProceduresCatalog.load();
        assertThat(entries).hasSize(341);
        assertThat(entries).contains(new DefaultProceduresCatalog.Entry("Aparelho protetor bucal", new BigDecimal("1000.00")));
        // preço 0.00 vira null ("sem preço")
        assertThat(entries).contains(new DefaultProceduresCatalog.Entry("Avaliação", null));
        assertThat(entries).extracting(DefaultProceduresCatalog.Entry::name).doesNotHaveDuplicates();
    }
}
```

- [ ] Implementação:

```java
/** Lista padrão de procedimentos odontológicos (extraída do Capim em 2026-10-22). */
public final class DefaultProceduresCatalog {

    public static final String CATEGORY = "Procedimento odontológico";
    private static final String RESOURCE = "/catalog/procedimentos-odontologicos.tsv";

    public record Entry(String name, BigDecimal price) {}

    private DefaultProceduresCatalog() {}

    public static List<Entry> load() {
        try (InputStream in = DefaultProceduresCatalog.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Recurso " + RESOURCE + " não encontrado.");
            }
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("﻿", "");
            return text.lines()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty())
                    .map(DefaultProceduresCatalog::parse)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Entry parse(String line) {
        int tab = line.lastIndexOf('\t');
        String name = line.substring(0, tab).strip();
        BigDecimal price = new BigDecimal(line.substring(tab + 1).strip());
        return new Entry(name, price.signum() == 0 ? null : price);
    }
}
```

- [ ] Porta: `Set<String> findNamesLowercase(UUID companyId)` (nomes de **todos** os itens da empresa, `lower(trim(name))`) e `List<CatalogItem> saveAll(List<CatalogItem> items)`. Implemente na `RepositoryImpl` com `@Query("select lower(trim(c.name)) from CatalogItemJpaEntity c where c.companyId = :companyId")` (ajuste o nome da entidade ao real).
- [ ] `CatalogService.importDefaultProcedures(UUID companyId)` (`@Transactional`): carrega entradas, pula as que já existem por nome (case-insensitive, trim), cria o resto com `CatalogItem.create(companyId, CatalogItemType.SERVICE, name, null, DefaultProceduresCatalog.CATEGORY, price, "BRL", null, null, null)`, `saveAll`, audita uma vez (`AuditAction.CREATE`, `AuditModule.CATALOG`, mensagem `"Procedimentos padrão importados: <created>"`), retorna `new ImportDefaultProceduresResponse(created, skipped)`.
- [ ] Teste do serviço (Mockito): com repositório retornando `Set.of("avaliação")`, `created == 340` e `skipped == 1`; segunda chamada com todos os nomes existentes → `created == 0`.
- [ ] Controller: `@PostMapping("/import-default-procedures")` com `@PreAuthorize("hasAuthority('catalog:manage')")`, mesmo estilo dos outros métodos do `CatalogController`.
- [ ] Verificação backend. Commit: `feat(catalogo): importação dos procedimentos odontológicos padrão`

## Task 3: Backend — procedimentos do paciente (`clinical/procedure`)

**Files (todos em `backend/src/main/java/com/becommerce/crm/clinical/procedure/`):**
`domain/PatientProcedure.java`, `domain/ProcedureStatus.java` (`TO_DO`, `DONE`, `PRE_EXISTING`), `domain/Dentition.java` (`PERMANENT`, `DECIDUOUS`), `domain/exception/PatientProcedureNotFoundException.java`, `domain/exception/PatientProcedureLockedException.java`, `application/dto/PatientProcedureRequest.java`, `application/dto/PatientProcedureResponse.java`, `application/port/in/PatientProcedureUseCase.java`, `application/port/out/PatientProcedureRepository.java`, `application/service/PatientProcedureService.java`, `infrastructure/persistence/PatientProcedureJpaEntity.java`, `...JpaRepository.java`, `...RepositoryImpl.java`, `web/PatientProcedureController.java`; Modify `shared/web/handler/GlobalExceptionHandler.java`.
Test: `backend/src/test/java/com/becommerce/crm/clinical/procedure/domain/PatientProcedureTest.java`, `.../application/service/PatientProcedureServiceTest.java`.

**Interfaces (Produces):**

```java
public record PatientProcedureRequest(
        UUID catalogItemId,
        @NotBlank @Size(max = 160) String name,
        @NotNull ProcedureStatus status,
        @NotNull Dentition dentition,
        List<Integer> teeth,          // FDI; null = []
        List<String> faces,           // "M","D","O","V","L"; null = []
        @PositiveOrZero Long priceCents,
        UUID professionalUserId,
        @Size(max = 2000) String notes) {}

public record PatientProcedureResponse(
        UUID id, UUID contactId, UUID catalogItemId, String name, ProcedureStatus status, Dentition dentition,
        List<Integer> teeth, List<String> faces, Long priceCents, UUID professionalUserId, String professionalName,
        String notes, UUID budgetId, LocalDate performedAt, LocalDateTime createdAt) {}
```

Endpoints (`PatientProcedureController`, base `/api/v1/companies/{companyId}/patients/{contactId}/procedures`):
`GET ?status=` (`contact:read`), `POST` (201), `PUT /{procedureId}`, `POST /{procedureId}/done`, `DELETE /{procedureId}` (204) — escrita com `contact:update`.

- [ ] Testes de domínio primeiro (`PatientProcedureTest`):
  - dente fora da dentição → `ContactValidationException` (permanente: quadrantes 1–4, unidade 1–8; decídua: quadrantes 5–8, unidade 1–5);
  - face fora de `M,D,O,V,L` → exceção; faces sem dente → exceção;
  - `markDone(LocalDate)` muda para `DONE` e grava `performedAt`; alterar status para outro estado limpa `performedAt`;
  - o serviço lança `PatientProcedureLockedException` (nova, em `domain/exception`, mapeada para **409** no `GlobalExceptionHandler`, mensagem "Procedimento vinculado a orçamento aprovado. Edite pelo orçamento.") ao editar/excluir procedimento travado.
- [ ] Domínio: campos da V106; `budgetId` só tem getter e `linkToBudget(UUID)` / `unlinkBudget()` (usados na Parte B). Validação de dentes:

```java
static void validateTeeth(Dentition dentition, List<Integer> teeth) {
    for (Integer tooth : teeth) {
        int quadrant = tooth / 10;
        int unit = tooth % 10;
        boolean ok = dentition == Dentition.PERMANENT
                ? quadrant >= 1 && quadrant <= 4 && unit >= 1 && unit <= 8
                : quadrant >= 5 && quadrant <= 8 && unit >= 1 && unit <= 5;
        if (!ok) {
            throw new ContactValidationException("Dente " + tooth + " inválido para a dentição informada.");
        }
    }
}
```

- [ ] Serviço: igual ao `PatientEvolutionService` (tenant, `requireOwnedContact`, auditoria, nome do profissional resolvido via `UserRepository` com cache em `Map`). Se `catalogItemId` vier, valida que o item é da empresa (`CatalogItemRepository.findById` + `companyId`), senão 404 do catálogo. **Editar ou excluir procedimento com `budgetId` de orçamento aprovado é proibido** — na Parte A não há orçamento, então implemente a checagem por um método da porta `boolean isLockedByBudget(UUID procedureId)` que na Parte A retorna sempre `false` (RepositoryImpl) e será trocado na Task 10. Marcar como realizado é sempre permitido.
- [ ] Mapeie `teeth` como `@JdbcTypeCode(SqlTypes.ARRAY) Short[]`/`Integer[]` e `faces` como `String[]` com `columnDefinition` igual ao SQL; confira um exemplo de array no projeto com `git grep -n "SqlTypes.ARRAY" backend/src/main/java | head`. Se não houver exemplo, use `@JdbcTypeCode(SqlTypes.ARRAY)` e registre em "Divergências".
- [ ] Teste de serviço: criar com contato de outra empresa → `ContactNotFoundException`; listar filtra por status; `done` grava `performedAt = hoje`.
- [ ] Verificação backend. Commit: `feat(pacientes): procedimentos do paciente com odontograma no backend`

## Task 4: Frontend — botão "Importar procedimentos padrão" no Catálogo

**Files:** Modify `frontend-refine/src/features/masterdata/catalog/services/catalog.service.ts`, `.../hooks/useCatalog.ts`, `frontend-refine/src/app/(dashboard)/catalog/page.tsx`. Test: `frontend-refine/src/__tests__/features/masterdata/catalog/ImportDefaultProcedures.test.tsx`.

- [ ] Service `importDefaultProcedures(companyId): Promise<{ created: number; skipped: number }>` → `POST /companies/${companyId}/catalog/import-default-procedures`.
- [ ] Hook `useImportDefaultProcedures(companyId)` com `useMutationDefaults`, invalidando as queries do catálogo; toast de sucesso `"<created> procedimentos importados (<skipped> já existiam)."`.
- [ ] Na página, ao lado do botão de novo item, `Button variant="outline"` "Importar procedimentos padrão" visível só com `can("catalog:manage")`; abre `AlertDialog`: "Isso adiciona 341 procedimentos odontológicos com preços sugeridos. Itens com o mesmo nome não são duplicados." → "Importar".
- [ ] Teste: clicar no botão e confirmar chama o service mockado uma vez.
- [ ] Verificação frontend. Commit: `feat(catalogo): botão para importar procedimentos padrão`

## Task 5: Frontend — tipos, services, hooks e componente `Odontogram`

**Files:** Create `frontend-refine/src/features/clinical/procedures/types/procedure.types.ts`, `services/procedure.service.ts`, `hooks/useProcedures.ts`, `components/Odontogram.tsx`, `lib/teeth.ts`. Test: `frontend-refine/src/__tests__/features/clinical/procedures/Odontogram.test.tsx`, `teeth.test.ts`.

**Interfaces (Produces):**

```ts
export type ProcedureStatus = "TO_DO" | "DONE" | "PRE_EXISTING";
export type Dentition = "PERMANENT" | "DECIDUOUS";
export interface PatientProcedure {
  id: string; contactId: string; catalogItemId: string | null; name: string;
  status: ProcedureStatus; dentition: Dentition; teeth: number[]; faces: string[];
  priceCents: number | null; professionalUserId: string | null; professionalName: string | null;
  notes: string | null; budgetId: string | null; performedAt: string | null; createdAt: string;
}
export type PatientProcedureRequest = Omit<PatientProcedure,
  "id" | "contactId" | "professionalName" | "budgetId" | "performedAt" | "createdAt">;
export const PROCEDURE_STATUS_LABEL: Record<ProcedureStatus, string> =
  { TO_DO: "A realizar", DONE: "Realizado", PRE_EXISTING: "Pré-existente" };
```

`lib/teeth.ts`:

```ts
export const PERMANENT_UPPER = [18, 17, 16, 15, 14, 13, 12, 11, 21, 22, 23, 24, 25, 26, 27, 28];
export const PERMANENT_LOWER = [48, 47, 46, 45, 44, 43, 42, 41, 31, 32, 33, 34, 35, 36, 37, 38];
export const DECIDUOUS_UPPER = [55, 54, 53, 52, 51, 61, 62, 63, 64, 65];
export const DECIDUOUS_LOWER = [85, 84, 83, 82, 81, 71, 72, 73, 74, 75];
const PRIORITY: ProcedureStatus[] = ["TO_DO", "DONE", "PRE_EXISTING"];
/** Estado exibido no dente: A realizar > Realizado > Pré-existente. */
export function toothStatus(tooth: number, procedures: PatientProcedure[]): ProcedureStatus | null {
  const states = procedures.filter((p) => p.teeth.includes(tooth)).map((p) => p.status);
  return PRIORITY.find((s) => states.includes(s)) ?? null;
}
```

- [ ] `teeth.test.ts`: dente com TO_DO e DONE → `"TO_DO"`; sem procedimento → `null`.
- [ ] Service/hooks no padrão de `evolutions` (`list(companyId, contactId, status?)`, `create`, `update`, `markDone`, `remove`); query key `["patient-procedures", companyId, contactId]`.
- [ ] `Odontogram` props: `{ dentition: Dentition; procedures: PatientProcedure[]; selected?: number[]; onToothClick?: (tooth: number) => void }`. Duas fileiras (superior/inferior) de `button` com `aria-label="Dente <n>"`, número abaixo, ícone de dente simples (SVG inline, `currentColor`) colorido pelo `toothStatus` conforme Global Constraint 8; selecionado com `ring-2 ring-primary`.
- [ ] Teste: renderiza 32 botões na permanente e 20 na decídua; clicar em "Dente 11" chama `onToothClick(11)`.
- [ ] Verificação frontend. Commit: `feat(pacientes): odontograma e serviços de procedimentos no frontend`

## Task 6: Frontend — aba "Ficha clínica"

**Files:** Create `frontend-refine/src/app/(dashboard)/patients/[id]/ficha-clinica/page.tsx`, `frontend-refine/src/features/clinical/procedures/components/ClinicalChart.tsx`, `ProcedureDrawer.tsx`, `ProcedureList.tsx`; Modify `frontend-refine/src/features/clinical/patients/components/PatientTabs.tsx`. Test: `frontend-refine/src/__tests__/features/clinical/procedures/ProcedureDrawer.test.tsx`.

- [ ] `PatientTabs`: insira `{ label: "Ficha clínica", segment: "ficha-clinica" }` **como primeira aba depois de "Consultas"** (preserve a ordem que a Fase 2 deixou para as demais).
- [ ] `ClinicalChart` (grid 2 colunas em `lg`, 1 coluna no celular): à esquerda título "Odontograma", botão "+ Novo procedimento" (só com `can("contact:update")`), `Tabs`/toggle Permanente/Decídua, `Odontogram`; abaixo filtros (`RadioGroup` ou botões) Ficha geral / A realizar / Realizado / Pré-existente e `ProcedureList`. À direita, o componente de anotações já existente da aba Anotações (`PatientNotes` — reaproveite, não duplique).
- [ ] Clicar num dente abre o `ProcedureDrawer` com aquele dente pré-selecionado e a dentição atual.
- [ ] `ProcedureDrawer` (`Sheet` lado direito), react-hook-form + zod:
  - Estado (3 botões segmentados, default "A realizar");
  - Procedimento (`Combobox`/`Command` buscando no catálogo com `useCatalog` filtrando `type=SERVICE`; ao escolher preenche nome e preço sugerido); permite texto livre se não houver item;
  - Valor (input monetário no padrão do financeiro — procure o componente/format usado em `features/finance` e reaproveite), Dentes (multi-seleção com os números da dentição), Faces (checkboxes M, D, O/I, V, L/P, só habilitado com 1 dente), Profissional (mesmo select de profissionais usado na agenda), Observações.
  - **Não** existe opção de gerar lançamento financeiro.
  - Editar usa o mesmo drawer; procedimento vinculado a orçamento aprovado abre em modo leitura com aviso "Vinculado ao orçamento aprovado — edite pelo orçamento.".
- [ ] `ProcedureList`: tabela Procedimento, Dente/Faces (`11 (M, O)`), Estado (`Badge`), Valor, Orçamento (badge "Em orçamento" se `budgetId`), Ações (`DropdownMenu`: Marcar como realizado, Editar, Excluir com `AlertDialog`). Estado vazio: "Adicione procedimentos para acompanhar o tratamento do paciente.".
- [ ] Teste: submeter o drawer com dente 11, face O e estado "Realizado" envia `{ teeth: [11], faces: ["O"], status: "DONE" }`.
- [ ] Verificação frontend. Commit: `feat(pacientes): aba Ficha clínica com odontograma e procedimentos`

## Task 7: Fechamento da Parte A

- [ ] Rode backend e frontend completos (item 10). Cole as últimas linhas em "Resultado final — Parte A" no fim deste arquivo.
- [ ] Atualize o status no spec (`docs/superpowers/specs/2026-10-22-ficha-clinica-orcamentos-design.md`): linha "**3A — IMPLEMENTADA**".
- [ ] Commit: `docs(pacientes): marca a Fase 3A como implementada`. **Pare e avise** (a Parte B começa depois do ok do usuário).

---

# PARTE B — Orçamentos

## Task 8: Migration `V107__budgets.sql`

**Files:** Create `backend/src/main/resources/db/migration/V107__budgets.sql`

```sql
CREATE TABLE budgets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    contact_id      UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    title           VARCHAR(160),
    valid_until     DATE,
    status          VARCHAR(10) NOT NULL DEFAULT 'DRAFT',
    discount_type   VARCHAR(10),
    discount_value  NUMERIC(12, 2) NOT NULL DEFAULT 0,
    subtotal_cents  BIGINT NOT NULL DEFAULT 0,
    total_cents     BIGINT NOT NULL DEFAULT 0,
    notes           TEXT,
    payment_plan    JSONB,
    approved_by     UUID REFERENCES users(id) ON DELETE SET NULL,
    approved_at     TIMESTAMP,
    lost_reason     VARCHAR(255),
    created_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    updated_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_budgets_status CHECK (status IN ('DRAFT', 'APPROVED', 'LOST', 'CANCELLED')),
    CONSTRAINT chk_budgets_discount_type CHECK (discount_type IS NULL OR discount_type IN ('AMOUNT', 'PERCENT')),
    CONSTRAINT chk_budgets_discount_value CHECK (discount_value >= 0),
    CONSTRAINT chk_budgets_totals CHECK (subtotal_cents >= 0 AND total_cents >= 0 AND total_cents <= subtotal_cents),
    CONSTRAINT chk_budgets_approved CHECK ((status IN ('APPROVED', 'CANCELLED')) = (approved_at IS NOT NULL))
);
CREATE INDEX idx_budgets_contact ON budgets (company_id, contact_id, updated_at DESC);

CREATE TABLE budget_items (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id            UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    budget_id             UUID NOT NULL REFERENCES budgets(id) ON DELETE CASCADE,
    patient_procedure_id  UUID REFERENCES patient_procedures(id) ON DELETE SET NULL,
    name                  VARCHAR(160) NOT NULL,
    teeth                 SMALLINT[] NOT NULL DEFAULT '{}',
    price_cents           BIGINT NOT NULL,
    professional_user_id  UUID REFERENCES users(id) ON DELETE SET NULL,
    position              INT NOT NULL,
    CONSTRAINT chk_budget_items_price CHECK (price_cents >= 0)
);
CREATE INDEX idx_budget_items_budget ON budget_items (budget_id, position);

ALTER TABLE patient_procedures
    ADD CONSTRAINT fk_patient_procedures_budget FOREIGN KEY (budget_id) REFERENCES budgets(id) ON DELETE SET NULL;

ALTER TABLE financial_transactions ADD COLUMN budget_id UUID REFERENCES budgets(id) ON DELETE SET NULL;
CREATE INDEX idx_financial_transactions_budget ON financial_transactions (budget_id) WHERE budget_id IS NOT NULL;
```

- [ ] Mais o bloco RLS + GRANT (Global Constraint 4) para `budgets` e `budget_items`.
- [ ] Verificação: colisões de nome vazias (`budgets`, `budget_items`, nomes de índice). Commit: `feat(pacientes): migration V107 com orçamentos`

## Task 9: Backend — domínio do orçamento (cálculos)

**Files:** Create em `backend/src/main/java/com/becommerce/crm/clinical/budget/domain/`: `Budget.java`, `BudgetItem.java` (record), `BudgetStatus.java`, `DiscountType.java`, `PaymentPlan.java` (record), `exception/BudgetNotFoundException.java`, `exception/BudgetStateException.java` (409). Test: `backend/src/test/java/com/becommerce/crm/clinical/budget/domain/BudgetTest.java`.

**Interfaces (Produces):**

```java
public record BudgetItem(UUID id, UUID patientProcedureId, String name, List<Integer> teeth, long priceCents,
                         UUID professionalUserId, int position) {}
public record PaymentPlan(long downPaymentCents, PaymentMethod downPaymentMethod, int installments,
                          LocalDate firstDueDate, PaymentMethod method) {}
// Budget: create(companyId, contactId, title, validUntil, items, discountType, discountValue, notes, plan, userId)
//         updateDraft(...mesmos campos..., userId); approve(userId, now); markLost(reason, userId); cancel(userId)
//         long getSubtotalCents(); long getTotalCents(); long discountCents()
```

- [ ] Testes (escreva todos antes):
  - subtotal = soma dos itens; desconto `AMOUNT` 100,00 em 1.000,00 → total 90000; `PERCENT` 10 → 90000; `PERCENT` 33,33 em 100,00 → desconto arredondado `HALF_UP` para 3333 → total 6667;
  - desconto maior que subtotal ou `PERCENT` > 100 → `ContactValidationException`;
  - `approve` sem itens → `ContactValidationException("Adicione ao menos um procedimento ao orçamento.")`;
  - `approve`/`updateDraft`/`markLost` fora de `DRAFT` → `BudgetStateException`; `cancel` só de `APPROVED`;
  - `PaymentPlan` inválido: entrada ≥ total, parcelas fora de 1–36, sem `firstDueDate` quando o restante > 0 → exceção.
- [ ] Implemente o mínimo para passar; os totais são **sempre** recalculados no domínio (o cliente nunca manda total).
- [ ] Verificação backend. Commit: `feat(pacientes): domínio do orçamento com desconto e plano de pagamento`

## Task 10: Backend — serviço, persistência e API de orçamentos

**Files:** em `clinical/budget/`: `application/dto/BudgetRequest.java`, `BudgetResponse.java`, `BudgetSummaryResponse.java`, `LoseBudgetRequest.java`; `application/port/in/BudgetUseCase.java`; `application/port/out/BudgetRepository.java`; `application/service/BudgetService.java`; `infrastructure/persistence/BudgetJpaEntity.java`, `BudgetItemJpaEntity.java`, `BudgetJpaRepository.java`, `BudgetRepositoryImpl.java`; `web/BudgetController.java`. Modify: `clinical/procedure/infrastructure/persistence/PatientProcedureRepositoryImpl.java` (`isLockedByBudget` real), `finance/transaction/infrastructure/persistence/*JpaEntity` (coluna `budget_id`, só mapeamento), `GlobalExceptionHandler`. Test: `.../budget/application/service/BudgetServiceTest.java`.

**Interfaces (Produces):**

```java
public record BudgetRequest(@Size(max = 160) String title, LocalDate validUntil,
                            @NotEmpty List<@Valid Item> items, DiscountType discountType,
                            @PositiveOrZero BigDecimal discountValue, @Size(max = 600) String notes,
                            @Valid PaymentPlan paymentPlan) {
    /** Use procedureId para item vindo da ficha; sem procedureId, o item cria um procedimento TO_DO. */
    public record Item(UUID procedureId, @NotBlank @Size(max = 160) String name, List<Integer> teeth,
                       @NotNull @PositiveOrZero Long priceCents, UUID professionalUserId, UUID catalogItemId) {}
}
public record BudgetResponse(UUID id, String title, LocalDate validUntil, BudgetStatus status,
                             List<BudgetItem> items, DiscountType discountType, BigDecimal discountValue,
                             long subtotalCents, long discountCents, long totalCents, String notes,
                             PaymentPlan paymentPlan, long paidCents, String approvedByName,
                             LocalDateTime approvedAt, String updatedByName, LocalDateTime updatedAt) {}
```

Endpoints em `/api/v1/companies/{companyId}/patients/{contactId}/budgets`: `GET` (lista, `contact:read`), `GET /{id}`, `POST` (201), `PUT /{id}` (só DRAFT), `DELETE /{id}` (só DRAFT, 204), `POST /{id}/approve`, `POST /{id}/lose` (body `LoseBudgetRequest(String reason)`), `POST /{id}/cancel` — escrita com `contact:update`.

- [ ] Testes de serviço (Mockito) primeiro:
  - criar com item sem `procedureId` → salva novo `PatientProcedure` TO_DO e o vincula (`linkToBudget`);
  - item com `procedureId` já vinculado a outro orçamento DRAFT/APPROVED → `BudgetStateException` (409);
  - procedimento de outro paciente → `ContactNotFoundException`-equivalente 404;
  - **aprovar** com plano `{entrada 20000 PIX, 3 parcelas CREDIT_CARD a partir de 2026-11-10}` e total 100000 → `FinancialTransactionRepository.saveAll` recebe 1 + 3 lançamentos: entrada 20000 vencendo hoje; parcelas via `FinancialTransaction.createSeries(companyId, INCOME, "Orçamento <título ou nº> – parcela", PROCEDURE, 80000, 0, CREDIT_CARD, 2026-11-10, contactId, null, null, null, null, 3, userId)`; todos depois ligados ao orçamento por `BudgetRepository.linkTransactions(budgetId, ids)`;
  - aprovar sem plano → 1 lançamento à vista com `PaymentMethod.OTHER`, vencimento hoje, valor = total;
  - `lose` → status LOST e `unlinkBudget()` em todos os procedimentos;
  - `cancel` → status CANCELLED, lançamentos `PENDING` do orçamento recebem `cancel()`, pagos ficam; procedimentos desvinculados;
  - nada disso cria lançamento fora do `approve` (verifique `verifyNoInteractions` nos outros casos).
- [ ] `BudgetRepository` (porta): `save`, `findById`, `findByContact(companyId, contactId)`, `delete`, `boolean isProcedureInActiveBudget(UUID procedureId, UUID excludeBudgetId)`, `void linkTransactions(UUID budgetId, List<UUID> transactionIds)` (UPDATE nativo `financial_transactions SET budget_id = :budgetId WHERE id IN (:ids)`), `List<UUID> findTransactionIds(UUID budgetId)`, `long sumPaidCents(UUID budgetId)` (`SUM(amount_cents) WHERE status = 'PAID'`).
- [ ] `PatientProcedureRepositoryImpl.isLockedByBudget`: `true` quando o procedimento tem `budget_id` de orçamento `APPROVED` (query nativa com join).
- [ ] `payment_plan` como JSONB com `@JdbcTypeCode(SqlTypes.JSON)` no padrão da Fase 2.
- [ ] Tudo do `approve` numa transação (`@Transactional`); auditoria `"Orçamento aprovado: <total formatado>"`.
- [ ] Verificação backend. Commit: `feat(pacientes): orçamentos com aprovação gerando parcelas no financeiro`

## Task 11: Frontend — tipos, services e hooks de orçamento

**Files:** Create `frontend-refine/src/features/clinical/budgets/types/budget.types.ts`, `services/budget.service.ts`, `hooks/useBudgets.ts`, `lib/budgetMath.ts`. Test: `frontend-refine/src/__tests__/features/clinical/budgets/budgetMath.test.ts`.

- [ ] Tipos espelhando `BudgetRequest`/`BudgetResponse` da Task 10 (`BudgetStatus = "DRAFT" | "APPROVED" | "LOST" | "CANCELLED"`, rótulos `Rascunho`, `Aprovado`, `Perdido`, `Cancelado`).
- [ ] `budgetMath.ts` — **só para pré-visualização** (o backend é a fonte da verdade), mesmas regras da Task 9:

```ts
export function discountCents(subtotal: number, type: "AMOUNT" | "PERCENT" | null, value: number): number {
  if (!type || value <= 0) return 0;
  return type === "AMOUNT" ? Math.round(value * 100) : Math.round((subtotal * value) / 100);
}
export function previewInstallments(remainingCents: number, installments: number): number[] {
  const base = Math.floor(remainingCents / installments);
  const rest = remainingCents % installments;
  return Array.from({ length: installments }, (_, i) => base + (i === 0 ? rest : 0));
}
```

- [ ] Testes: 10% de 100000 → 10000; `previewInstallments(10000, 3)` → `[3334, 3333, 3333]`.
- [ ] Hooks: `useBudgets`, `useBudget`, `useCreateBudget`, `useUpdateBudget`, `useDeleteBudget`, `useApproveBudget`, `useLoseBudget`, `useCancelBudget`; mutações invalidam `["budgets", companyId, contactId]`, `["patient-procedures", companyId, contactId]` e as queries do financeiro do paciente (procure a key usada na aba `financeiro`).
- [ ] Verificação frontend. Commit: `feat(pacientes): serviços e hooks de orçamento no frontend`

## Task 12: Frontend — aba "Orçamentos" (lista + editor em 2 etapas)

**Files:** Create `frontend-refine/src/app/(dashboard)/patients/[id]/orcamentos/page.tsx`, `.../orcamentos/novo/page.tsx`, `.../orcamentos/[budgetId]/page.tsx`; `frontend-refine/src/features/clinical/budgets/components/BudgetList.tsx`, `BudgetEditor.tsx`, `BudgetItemsStep.tsx`, `BudgetPaymentStep.tsx`, `ApproveBudgetDialog.tsx`, `LoseBudgetDialog.tsx`; Modify `PatientTabs.tsx`. Test: `frontend-refine/src/__tests__/features/clinical/budgets/BudgetEditor.test.tsx`, `BudgetList.test.tsx`.

- [ ] `PatientTabs`: `{ label: "Orçamentos", segment: "orcamentos" }` logo depois de "Ficha clínica".
- [ ] `BudgetList`: botão "+ Novo orçamento"; tabela Título (ou "Orçamento de dd/MM/yyyy"), Última edição, Editado por, Saldo total, Status (`Badge`), Ações (`DropdownMenu`): Rascunho → Editar, Aprovar, Marcar como perdido, Excluir; Aprovado → Visualizar, Cancelar; Perdido/Cancelado → Visualizar. Ações de escrita só com `can("contact:update")`. Estado vazio "Nenhum orçamento criado".
- [ ] `BudgetEditor` (usado em `novo` e `[budgetId]` com DRAFT; outros status redirecionam para a visualização da Task 13):
  - **Etapa 1 · Procedimentos**: Título, Validade; `Odontogram` (reuso da Task 5) — clicar num dente abre o `ProcedureDrawer` em modo "item de orçamento" (sem campo de estado; cria item sem `procedureId`); botão "Adicionar da ficha" lista procedimentos `TO_DO` sem `budgetId` com checkbox; tabela de itens (Procedimento, Valor editável, Dente, Profissional, remover); Desconto (`Select` R$/% + valor); Saldo total ao vivo (`budgetMath`); Observações (máx. 600, contador "n / 600"). "Continuar" exige ≥ 1 item.
  - **Etapa 2 · Pagamento (opcional)**: texto "Essas condições vão para o financeiro quando o orçamento for aprovado."; switch "Definir forma de pagamento"; Entrada (valor + meio), Parcelas (1–36), 1º vencimento, Meio das parcelas; prévia das parcelas com `previewInstallments`. Botões Voltar / "Salvar orçamento".
  - Meios de pagamento com os rótulos já usados no financeiro (reaproveite o mapa existente em `features/finance`).
- [ ] `ApproveBudgetDialog`: resumo (total, entrada, n × valor) + aviso "Ao aprovar, as parcelas são lançadas no financeiro do paciente e o orçamento não pode mais ser editado." → "Aprovar orçamento".
- [ ] `LoseBudgetDialog`: motivo opcional → "Marcar como perdido". Cancelar aprovado usa `AlertDialog` com aviso "Parcelas pendentes serão canceladas; pagamentos já registrados permanecem.".
- [ ] Testes: Editor — adicionar 2 itens de 50000 com 10% de desconto mostra "R$ 900,00"; "Continuar" desabilitado sem itens. List — rascunho mostra "Aprovar"; aprovado não mostra "Editar".
- [ ] Verificação frontend. Commit: `feat(pacientes): aba Orçamentos com editor em duas etapas e aprovação`

## Task 13: Frontend — visualizar/imprimir orçamento e Financeiro por orçamento

**Files:** Create `frontend-refine/src/features/clinical/budgets/components/BudgetPrintView.tsx`, `BudgetViewOptionsDialog.tsx`; Modify `.../orcamentos/[budgetId]/page.tsx` e `frontend-refine/src/app/(dashboard)/patients/[id]/financeiro/page.tsx`. Test: `.../budgets/BudgetPrintView.test.tsx`.

- [ ] "Visualizar" abre `BudgetViewOptionsDialog` com checkboxes (todos marcados): Valor total, Valor por procedimento, Dentistas, Desconto, Observações, Forma de pagamento → mostra `BudgetPrintView` com só os blocos marcados e botão "Imprimir" (`window.print()`), com o mesmo cabeçalho de clínica/paciente e CSS de impressão usados nos documentos da Fase 2 (reaproveite, não duplique).
- [ ] Aba Financeiro: acima da lista atual, um card por orçamento **aprovado** do paciente (`useBudgets` filtrando `APPROVED`): título, `Progress` com `paidCents/totalCents`, "R$ pago de R$ total", link "Ver orçamento". Lançamentos continuam listados como hoje.
- [ ] Teste: desmarcar "Desconto" esconde a linha de desconto.
- [ ] Verificação frontend. Commit: `feat(pacientes): impressão do orçamento e progresso no financeiro`

## Task 14: Fechamento da Parte B

- [ ] Rode tudo (item 10) e cole as últimas linhas em "Resultado final — Parte B".
- [ ] Spec: linha "**3B — IMPLEMENTADA**". Spec original (`2026-10-12-ficha-do-paciente-design.md`): marque a Fase 3 como implementada.
- [ ] `git log --oneline feature/pacientes-fase2..HEAD` deve mostrar ~16 commits (spec + plano + 14 tasks).
- [ ] Commit: `docs(pacientes): marca a Fase 3 da ficha como implementada`. **Pare.** Não faça push.

---

## Divergências encontradas

(preencher durante a execução)

## Resultado final — Parte A

## Resultado final — Parte B
