# Ficha do Paciente (Fase 3b — Orçamentos) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Execute na ordem, um commit por task, sem push/merge/PR. Se o código real não bater com o plano, siga o padrão existente mais próximo e registre no relatório.

**Goal:** Aba **Orçamentos** na ficha do paciente: orçamento a partir de procedimentos "A realizar", desconto, aprovação com parcelas em `financial_transactions`, marcar como perdido, visualizar/imprimir com blocos configuráveis; integração com a ficha clínica da 3a.

**Architecture:** Pacote backend `com.becommerce.crm.clinical.budget`, cópia estrutural de `clinical/procedure` (3a, já no branch base): domínio + ports + service + JPA + controller. Parcelas via `FinancialTransaction.createSeries(...)` e `FinancialTransactionRepository.saveAll(...)` existentes. Frontend em `frontend-refine/src/features/clinical/budgets/`, cópia estrutural de `features/clinical/procedures`.

**Tech Stack:** Spring Boot hexagonal, PostgreSQL + Flyway, JUnit 5 + Mockito + AssertJ, Testcontainers (CI); Next.js + TanStack Query + shadcn/ui, Vitest + Testing Library.

**Spec:** `docs/superpowers/specs/2026-10-23-orcamentos-design.md` — leia antes; é a autoridade.

## Global Constraints

1. **Branch** `feature/pacientes-fase3b`, pasta `C:\Users\ghilh\Desktop\PROJETO\crm\crm-deploy`. Não troque de branch.
2. **Migration única `V107__budgets.sql`** (última existente: V106). Conferir colisão: `git grep -n "budgets\|budget_items\|budget_transactions" backend/src/main/resources/db/migration` vazio antes. Nunca editar migrations antigas. Sem Docker local.
3. **RLS** em toda tabela nova: `company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE`, ENABLE + FORCE, `tenant_isolation_policy` USING/WITH CHECK `company_id = app.current_tenant_id()`, `GRANT SELECT, INSERT, UPDATE, DELETE ... TO crm_app` (bloco `DO $$` como o fim da V106).
4. **Backend:** modelos a copiar: `clinical/procedure/**` (service com `TenantContext` set/clear em `try/finally`, `requireOwnedContact`, `requireOwned`, auditoria `AuditModule.CONTACTS` recurso `"Budget"`, mensagens pt-BR; controller com `@CurrentCompanyId`, `@PreAuthorize` `contact:read`/`contact:update`; `canCreateFinance` lido de `principal.permissions()`). Nenhuma permissão nova.
5. **Exceções:** 400 `ContactValidationException`; 409 `ContactConflictException`; 404 `BudgetNotFoundException` (handler novo ao lado de `PatientProcedureNotFoundException` no `GlobalExceptionHandler`); 403 `CrmAccessDeniedException`.
6. **Frontend:** pt-BR; só tokens do tema; mutations com `useMutationDefaults`; testes com `fireEvent` + `screen.*` + `toBeTruthy()/toBeNull()`, sem user-event/jest-dom. Modelos: `features/clinical/procedures/**` e seus testes.
7. **Estilo:** Checkstyle ≤ 200 colunas; Prettier (`printWidth 100`, aspas duplas, `trailingComma all`) em todo arquivo do frontend alterado (`npx prettier --check --end-of-line auto`).
8. **Verificações:** backend `cd backend && mvn -q test`; frontend `cd frontend-refine && npm run lint && npx tsc --noEmit && npx vitest run --pool=threads` + Prettier. Local: `SidebarGroup.test.tsx` falha por ambiente (node_modules via junction) — ignore só essas 5 falhas.
9. **Commits:** Conventional pt-BR, linha em branco e trailer `Co-Authored-By: <modelo> <noreply@anthropic.com>`.

---

### Task 1: Migration `V107__budgets.sql`

**Files:** Create `backend/src/main/resources/db/migration/V107__budgets.sql`

- [ ] Cabeçalho de comentário (estilo V106) explicando as três tabelas e que as permissões são reaproveitadas.
- [ ] SQL:

```sql
CREATE TABLE budgets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    contact_id      UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    title           VARCHAR(160) NOT NULL,
    status          VARCHAR(10) NOT NULL DEFAULT 'DRAFT',
    discount_cents  BIGINT NOT NULL DEFAULT 0,
    notes           TEXT,
    payment_method  VARCHAR(20),
    installments    INT,
    first_due_date  DATE,
    approved_at     TIMESTAMP,
    approved_by     UUID REFERENCES users(id) ON DELETE SET NULL,
    lost_at         TIMESTAMP,
    created_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    updated_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_budgets_status CHECK (status IN ('DRAFT', 'APPROVED', 'LOST')),
    CONSTRAINT chk_budgets_discount CHECK (discount_cents >= 0),
    CONSTRAINT chk_budgets_installments CHECK (installments IS NULL OR installments BETWEEN 1 AND 24),
    CONSTRAINT chk_budgets_payment_method CHECK (payment_method IS NULL OR payment_method IN
        ('BOLETO', 'CREDIT_CARD', 'DEBIT_CARD', 'PIX', 'CASH', 'TRANSFER', 'CHECK', 'OTHER')),
    CONSTRAINT chk_budgets_approved CHECK (status <> 'APPROVED' OR approved_at IS NOT NULL)
);

CREATE INDEX idx_budgets_company_contact ON budgets (company_id, contact_id);

CREATE TABLE budget_items (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id    UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    budget_id     UUID NOT NULL REFERENCES budgets(id) ON DELETE CASCADE,
    procedure_id  UUID NOT NULL REFERENCES patient_procedures(id) ON DELETE CASCADE,
    price_cents   BIGINT NOT NULL,
    position      INT NOT NULL,
    CONSTRAINT chk_budget_items_price CHECK (price_cents >= 0),
    CONSTRAINT uq_budget_items_budget_procedure UNIQUE (budget_id, procedure_id)
);

CREATE INDEX idx_budget_items_procedure ON budget_items (procedure_id);

CREATE TABLE budget_transactions (
    budget_id       UUID NOT NULL REFERENCES budgets(id) ON DELETE CASCADE,
    transaction_id  UUID NOT NULL REFERENCES financial_transactions(id) ON DELETE CASCADE,
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    PRIMARY KEY (budget_id, transaction_id)
);
```
seguido do bloco `DO $$` de RLS/GRANT para `ARRAY['budgets', 'budget_items', 'budget_transactions']` (cópia do fim da V106).
- [ ] Conferir nomes únicos (`git grep -n "idx_budget\|chk_budget\|uq_budget" backend/src/main/resources/db/migration` só acha a V107).
- [ ] Commit `feat(pacientes): migration dos orçamentos`.

---

### Task 2: Domínio do orçamento

**Files:** Create `backend/src/main/java/com/becommerce/crm/clinical/budget/domain/{BudgetStatus,BudgetItem,Budget}.java`, `domain/exception/BudgetNotFoundException.java`; Test `backend/src/test/java/com/becommerce/crm/clinical/budget/domain/BudgetTest.java`

**Produces:**
- `enum BudgetStatus { DRAFT, APPROVED, LOST }`
- `record BudgetItem(UUID id, UUID procedureId, long priceCents, int position)` (construtor compacto: `priceCents < 0` → `ContactValidationException("O valor do item não pode ser negativo.")`)
- `BudgetNotFoundException(UUID id)` → mensagem `"Orçamento não encontrado: " + id`
- `Budget.create(UUID companyId, UUID contactId, String title, List<BudgetItem> items, long discountCents, String notes, UUID actorId)`
- `Budget.reconstitute(...)` com todos os campos na ordem: id, companyId, contactId, title, status, discountCents, notes, paymentMethod (`PaymentMethod`), installments (`Integer`), firstDueDate, approvedAt, approvedBy, lostAt, items, transactionIds (`List<UUID>`), createdBy, updatedBy, createdAt, updatedAt.
- `void edit(String title, List<BudgetItem> items, long discountCents, String notes, UUID actorId)` — só DRAFT (senão `ContactConflictException("Só orçamentos em rascunho podem ser editados.")`).
- `void approve(PaymentMethod method, int installments, LocalDate firstDueDate, UUID actorId)` — só DRAFT (409 "Só orçamentos em rascunho podem ser aprovados."); `method`/`firstDueDate` nulos → 400; `installments` fora de 1..24 → 400 "O número de parcelas deve estar entre 1 e 24.".
- `void linkTransactions(List<UUID> ids)`
- `void markLost(UUID actorId)` — de DRAFT ou APPROVED; LOST → 409 "Orçamento já está marcado como perdido.".
- `void removeItemForProcedure(UUID procedureId, UUID actorId)` — só DRAFT; remove e reindexa `position`.
- `long subtotalCents()` (soma dos itens), `long totalCents()` (subtotal − desconto), `boolean isDraft()`, `isApproved()`, `isLost()`, getters (listas imutáveis).
- Validação comum (create/edit): título obrigatório, trim, ≤ 160 ("O título do orçamento é obrigatório." / "...no máximo 160 caracteres."); `items` não vazio ("Inclua ao menos um procedimento no orçamento."); procedimento repetido → 400; `discountCents < 0` → 400; `discountCents > subtotal` → 400 "O desconto não pode ser maior que o subtotal."; notas trim/null, ≤ 5000. Posições reatribuídas 0..n-1 na ordem recebida.

- [ ] **Teste que falha** com estes casos (escreva o código dos testes):
  1. `create` calcula subtotal 25000 e total 20000 para itens 15000+10000 e desconto 5000; status DRAFT; posições 0 e 1.
  2. título em branco → 400; itens vazios → 400; procedimento repetido → 400; desconto maior que subtotal → 400.
  3. `approve(PIX, 3, data, actor)` → APPROVED, `approvedAt` não nulo, `approvedBy` = actor; `approve` de novo → 409; `installments` 0 e 25 → 400.
  4. `edit` em APPROVED → 409.
  5. `markLost` de DRAFT e de APPROVED funciona; de LOST → 409.
  6. `removeItemForProcedure` em DRAFT remove e reindexa; em APPROVED → 409.
- [ ] Ver falhar (`mvn -q test -Dtest=BudgetTest`), implementar (estilo de `clinical/procedure/domain/PatientProcedure.java`: construtor privado, `create` + `reconstitute`, `touch()` atualiza `updatedAt` e `updatedBy`), ver passar.
- [ ] Commit `feat(pacientes): domínio do orçamento`.

---

### Task 3: Persistência

**Files:** Create `.../clinical/budget/application/port/out/BudgetRepository.java`, `.../infrastructure/persistence/{BudgetJpaEntity,BudgetItemJpaEntity,BudgetJpaRepository,BudgetRepositoryImpl}.java`

**Produces:**
```java
public interface BudgetRepository {
    Budget save(Budget budget);
    Optional<Budget> findById(UUID id);
    List<Budget> findByCompanyIdAndContactIdOrderByUpdatedAtDesc(UUID companyId, UUID contactId);
    void delete(Budget budget);
    /** Orçamentos não perdidos (DRAFT/APPROVED) que contêm algum destes procedimentos, por procedimento. */
    Map<UUID, Budget> findActiveByProcedureIds(UUID companyId, Collection<UUID> procedureIds);
}
```
- [ ] `BudgetItemJpaEntity`: `@Entity @Table(name = "budget_items")` com `id`, `companyId`, `procedureId`, `priceCents`, `position` e `@ManyToOne(fetch = LAZY) @JoinColumn(name = "budget_id") BudgetJpaEntity budget`.
- [ ] `BudgetJpaEntity`: colunas de `budgets` (status e payment_method como String), `@OneToMany(mappedBy = "budget", cascade = ALL, orphanRemoval = true, fetch = EAGER) @OrderBy("position") List<BudgetItemJpaEntity> items`, e `@ElementCollection(fetch = EAGER) @CollectionTable(name = "budget_transactions", joinColumns = @JoinColumn(name = "budget_id")) @Column(name = "transaction_id") Set<UUID> transactionIds` — **atenção:** `budget_transactions.company_id` é NOT NULL e o `@ElementCollection` simples não grava essa coluna. Use, em vez disso, um `@Embeddable BudgetTransactionLink { UUID companyId; UUID transactionId; }` (mesmo padrão do `PatientProcedureToothJpaEntity` da 3a) e preencha `companyId` no mapeamento.
- [ ] `BudgetJpaRepository`: `findByCompanyIdAndContactIdOrderByUpdatedAtDesc` e
```java
    @Query("select distinct b from BudgetJpaEntity b join b.items i "
            + "where b.companyId = :companyId and b.status <> 'LOST' and i.procedureId in :procedureIds")
    List<BudgetJpaEntity> findActiveByProcedureIds(UUID companyId, Collection<UUID> procedureIds);
```
- [ ] `BudgetRepositoryImpl`: `save` mescla itens preservando ids (mapeie domínio → entidade criando entidades de item com o `id` do domínio e `budget` = entidade pai); `findActiveByProcedureIds` devolve `Map` procedimento → orçamento (vazio se `procedureIds` vazio, sem consultar).
- [ ] `mvn -q -DskipTests compile`; commit `feat(pacientes): persistência dos orçamentos`.

---

### Task 4: Service, DTOs e casos de uso

**Files:** Create `.../clinical/budget/application/dto/{BudgetRequest,ApproveBudgetRequest,BudgetSummaryResponse,BudgetResponse}.java`, `port/in/BudgetUseCase.java`, `service/BudgetService.java`; Test `backend/src/test/java/com/becommerce/crm/clinical/budget/application/service/BudgetServiceTest.java`

**Produces:**
```java
public record BudgetRequest(@NotBlank @Size(max = 160) String title, @NotEmpty List<UUID> procedureIds,
                            @NotNull @PositiveOrZero Long discountCents, @Size(max = 5000) String notes,
                            Map<UUID, Long> itemPrices) {}
public record ApproveBudgetRequest(@NotNull PaymentMethod paymentMethod, @Min(1) @Max(24) int installments,
                                   @NotNull LocalDate firstDueDate) {}
public record BudgetSummaryResponse(UUID id, String title, BudgetStatus status, long subtotalCents,
                                    long discountCents, long totalCents, LocalDateTime updatedAt, String updatedByName) {}
public record BudgetResponse(UUID id, UUID contactId, String title, BudgetStatus status, long subtotalCents,
        long discountCents, long totalCents, String notes, PaymentMethod paymentMethod, Integer installments,
        LocalDate firstDueDate, LocalDateTime approvedAt, LocalDateTime lostAt, List<Item> items,
        List<Installment> installmentsList, LocalDateTime updatedAt, String updatedByName) {
    public record Item(UUID procedureId, String name, List<ProcedureToothDto> teeth, String professionalName, long priceCents) {}
    public record Installment(UUID id, LocalDate dueDate, long amountCents, TransactionStatus status) {}
}
public interface BudgetUseCase {
    List<BudgetSummaryResponse> list(UUID companyId, UUID contactId);
    BudgetResponse get(UUID companyId, UUID contactId, UUID budgetId);
    BudgetResponse create(UUID companyId, UUID contactId, BudgetRequest request, UUID actorId);
    BudgetResponse update(UUID companyId, UUID contactId, UUID budgetId, BudgetRequest request, UUID actorId);
    BudgetResponse approve(UUID companyId, UUID contactId, UUID budgetId, ApproveBudgetRequest request, UUID actorId, boolean canCreateFinance);
    BudgetResponse lose(UUID companyId, UUID contactId, UUID budgetId, UUID actorId);
    void delete(UUID companyId, UUID contactId, UUID budgetId, UUID actorId);
}
```
Regras (construtor: `BudgetRepository`, `PatientProcedureRepository`, `ContactRepository`, `FinancialTransactionRepository`, `UserRepository`, `TenantAuditRecorder`):
- **Montar itens** (create/update): para cada `procedureId` em ordem, carregar o procedimento (`PatientProcedureRepository.findById`), exigir mesma empresa e contato (senão 400 "Procedimento não encontrado para este paciente."), estado `PLANNED` (senão 400 "Só procedimentos \"A realizar\" podem entrar no orçamento."), e sem outro orçamento não perdido (`budgetRepository.findActiveByProcedureIds`; ignorar o próprio orçamento no update; senão 409 "O procedimento <nome> já está no orçamento <título>."). Preço: `itemPrices.get(id)` se presente; senão, no update, o preço já salvo no item do mesmo procedimento; senão `procedure.getPriceCents()`. O id do item: reaproveita o do item existente do mesmo procedimento, senão `UUID.randomUUID()`.
- **approve:** sem `canCreateFinance` → 403 "Você não tem permissão para gerar lançamentos financeiros."; `budget.approve(...)`; se `totalCents() > 0`: `FinancialTransaction.createSeries(companyId, INCOME, "Orçamento: " + título, PROCEDURE, total, 0L, method, firstDueDate, contactId, null, null, null, null, installments, actorId)` → `transactionRepository.saveAll` → `budget.linkTransactions(ids)`. Auditoria "Orçamento aprovado.".
- **lose:** se APPROVED: carregar cada lançamento vinculado; algum `PAID` → 409 "Orçamento com parcela paga não pode ser marcado como perdido."; os `PENDING` → `cancel()` + `save`. Depois `markLost`. Auditoria "Orçamento marcado como perdido.".
- **delete:** APPROVED → 409 "Orçamento aprovado não pode ser excluído. Marque como perdido antes."; senão `repository.delete`. Auditoria.
- **list/get:** nomes de usuário com cache (como 3a); `get` monta itens com nome/dentes/profissional do procedimento (dentes via `ProcedureToothDto`) e parcelas a partir de `transactionIds` (ordenadas por vencimento).
- [ ] **Testes** (escreva o código; mocks como `PatientProcedureServiceTest`): cria com subtotal/total corretos; procedimento de outro paciente → 400; procedimento DONE → 400; procedimento em outro orçamento ativo → 409; update mantém preço salvo do item e aceita `itemPrices`; approve gera 3 parcelas somando o total com resto na 1ª (ex.: total 10000 → 3334/3333/3333), categoria PROCEDURE, descrição "Orçamento: <título>"; approve sem permissão → 403 e nada salvo; approve com total 0 → sem parcelas; lose com parcela PAID → 409; lose com PENDING → canceladas; delete APPROVED → 409; delete DRAFT → apagado.
- [ ] Ver falhar, implementar, ver passar; commit `feat(pacientes): regras e parcelas dos orçamentos`.

---

### Task 5: Controller

**Files:** Create `.../clinical/budget/web/BudgetController.java`; Modify `shared/web/handler/GlobalExceptionHandler.java` (handler 404 de `BudgetNotFoundException`, ao lado do de `PatientProcedureNotFoundException`); Test `.../clinical/budget/web/BudgetControllerTest.java`

- [ ] Rotas base `/api/v1/companies/{companyId}/patients/{contactId}/budgets` (GET lista, GET `/{budgetId}`, POST → 201, PUT `/{budgetId}`, POST `/{budgetId}/approve`, POST `/{budgetId}/lose`, DELETE → 204), `ACCESS_MESSAGE = "Você só pode acessar orçamentos da sua própria empresa."`, leitura `contact:read`, demais `contact:update`, `@Valid` nos corpos, `canCreateFinance` como em `PatientProcedureController`.
- [ ] Teste unitário (padrão `PatientProcedureControllerTest`): approve repassa `true`/`false` conforme `finance:create`; create → 201; delete → 204.
- [ ] `mvn -q test -Dtest='Budget*'`; commit `feat(pacientes): endpoints dos orçamentos`.

---

### Task 6: Integração com a ficha clínica (backend)

**Files:** Modify `.../clinical/procedure/application/dto/PatientProcedureResponse.java`, `.../clinical/procedure/application/service/PatientProcedureService.java`, `PatientProcedureServiceTest.java`

- [ ] `PatientProcedureResponse` ganha o último componente `BudgetRef budget` com `public record BudgetRef(UUID id, String title, BudgetStatus status) {}` (ajuste todos os `new PatientProcedureResponse(` existentes).
- [ ] `PatientProcedureService` recebe `BudgetRepository` no construtor (atualize o teste). `list`: uma chamada `findActiveByProcedureIds(companyId, ids)` e preenche `budget`; respostas individuais também.
- [ ] `complete` e `create`: se `generateTransaction` e o procedimento está em orçamento `APPROVED` → 400 "Este procedimento já é cobrado pelo orçamento aprovado." (create nunca está em orçamento; só `complete` precisa da checagem).
- [ ] `delete`: orçamento ativo `APPROVED` → 409 "Procedimento de orçamento aprovado não pode ser excluído."; `DRAFT` → `budget.removeItemForProcedure(id, actorId)`; se o orçamento ficar sem itens, exclua o orçamento; senão `budgetRepository.save(budget)`. Depois segue a exclusão normal.
- [ ] Testes novos: list preenche `budget`; complete com orçamento aprovado + gerar → 400; delete com orçamento aprovado → 409; delete com rascunho remove o item (verify save) e com rascunho de item único exclui o orçamento.
- [ ] `mvn -q test` completo; commit `feat(pacientes): vincula procedimentos aos orçamentos`.

---

### Task 7: Isolamento (IT, CI)

**Files:** Test `backend/src/test/java/com/becommerce/crm/clinical/budget/infrastructure/persistence/BudgetIsolationIT.java`

- [ ] Copiar o esqueleto de `clinical/procedure/infrastructure/persistence/PatientProcedureIsolationIT.java`: aplicar **V106 e depois V107** (cada uma num único `execute`), tabelas mínimas antes (`contacts`, `users`, `catalog_items`, `financial_transactions`, todas só `id UUID PRIMARY KEY`). Semear por tenant: contato, item de catálogo, procedimento, orçamento, item e um lançamento mínimo (`INSERT INTO financial_transactions(id)` como superusuário) + `budget_transactions`.
- [ ] Testes: cada tenant vê só seus `budgets`, `budget_items` e `budget_transactions`; sem contexto nada visível; B não insere orçamento com `company_id` de A; CHECK de status inválido e de `installments = 25` falham; `APPROVED` sem `approved_at` falha.
- [ ] `mvn -q -DskipTests test-compile`; commit `test(pacientes): isolamento de tenant dos orçamentos`.

---

### Task 8: Frontend — tipos, service, hooks

**Files:** Create `frontend-refine/src/features/clinical/budgets/{types/budget.types.ts,services/budget.service.ts,hooks/useBudgets.ts,utils/installments.ts}`; Modify `features/clinical/procedures/types/procedure.types.ts` (campo `budget: { id: string; title: string; status: BudgetStatus } | null`, importando `BudgetStatus`); Test `src/__tests__/features/clinical/budgets/installments.test.ts`

- [ ] Tipos espelhando os DTOs da Task 4 (`BudgetStatus`, `PaymentMethod` reaproveitado de `@/features/finance/types/finance.types` se existir com os mesmos valores, senão declare), `BudgetRequest`, `ApproveBudgetRequest`, `BudgetSummary`, `BudgetDetail` (com `items` e `installmentsList`).
- [ ] `utils/installments.ts`: `splitInstallments(totalCents: number, count: number, firstDueDate: string): { dueDate: string; amountCents: number }[]` — divisão igual com resto na 1ª, vencimentos mensais (`yyyy-MM-dd`, use `date-fns` `addMonths` se já é dependência; confira em `package.json`). Teste: 10000 em 3 → 3334/3333/3333 e datas 2026-11-10, 2026-12-10, 2027-01-10.
- [ ] `BUDGET_STATUS_LABELS = { DRAFT: "Rascunho", APPROVED: "Aprovado", LOST: "Perdido" }` em `utils/installments.ts` ou arquivo `utils/labels.ts`.
- [ ] Service (rotas da Task 5) e hooks: `budgetKeys.all(companyId, contactId)`, `useBudgets`, `useBudget(budgetId | null)`, `useCreateBudget`, `useUpdateBudget({budgetId,payload})`, `useApproveBudget({budgetId,payload})`, `useLoseBudget(budgetId)`, `useDeleteBudget(budgetId)`. Todas as mutations invalidam orçamentos, `procedureKeys.all(companyId, contactId)` (de `features/clinical/procedures/hooks/useProcedures`) e `["finance", companyId]`.
- [ ] Ajuste testes da 3a que constroem `PatientProcedureResponse` completo (adicione `budget: null`) para o tsc passar.
- [ ] Verificação + commit `feat(pacientes): cliente da API de orçamentos`.

---

### Task 9: Formulário e aprovação

**Files:** Create `features/clinical/budgets/components/{BudgetFormDialog,ApproveBudgetDialog}.tsx`; Tests `src/__tests__/features/clinical/budgets/{BudgetFormDialog,ApproveBudgetDialog}.test.tsx`

- [ ] `BudgetFormDialog({ companyId, contactId, open, onOpenChange, budget?: BudgetDetail | null })`: campos "Título", lista de procedimentos `PLANNED` do paciente (de `useProcedures`) sem orçamento ativo — ou já do próprio orçamento —, cada um com `Checkbox` (rótulo = nome + `formatTeeth`) e `Input` "Valor (R$)" por item marcado (inicia com o preço salvo no item ou o do procedimento); "Desconto (R$)"; "Observações"; resumo ao vivo "Subtotal", "Desconto", "Total" em BRL. "Salvar" desabilitado sem título, sem itens ou com desconto > subtotal (mostra "O desconto não pode ser maior que o subtotal."). Envia `procedureIds` na ordem da lista e `itemPrices`. Sem procedimentos disponíveis: "Nenhum procedimento \"A realizar\" disponível. Cadastre na Ficha clínica.".
- [ ] `ApproveBudgetDialog({ companyId, contactId, budget: BudgetDetail | BudgetSummary | null, onOpenChange })`: "Forma de pagamento" (`Select`, rótulos do financeiro), "Parcelas" (1–24), "1º vencimento" (hoje); prévia `splitInstallments` em lista "1ª parcela — dd/MM/yyyy — R$ …"; total 0 mostra "Orçamento sem valor: nenhuma parcela será gerada."; "Aprovar" chama `useApproveBudget`.
- [ ] Testes: formulário calcula subtotal/total ao marcar itens e digitar desconto; desconto maior desabilita salvar e mostra aviso; aprovação mostra 3 linhas de parcela com 3334/3333/3333 para total 10000 em 3x.
- [ ] Verificação + commit `feat(pacientes): formulário e aprovação de orçamentos`.

---

### Task 10: Lista, visualização/impressão e aba

**Files:** Create `features/clinical/budgets/components/{BudgetList,BudgetViewDialog,BudgetPrintView,PatientBudgetsTab}.tsx`, `src/app/(dashboard)/patients/[id]/orcamentos/page.tsx`, `src/app/(dashboard)/patients/[id]/orcamentos/[budgetId]/imprimir/page.tsx`; Modify `features/clinical/patients/components/PatientTabs.tsx`; Tests `src/__tests__/features/clinical/budgets/{BudgetList,BudgetPrintView}.test.tsx`

- [ ] `BudgetList({ companyId, contactId, budgets, canEdit, onEdit, onApprove, onView })`: tabela Título, Última edição (`dd/MM/yyyy HH:mm`), Editado por, Total (BRL), Status (`Badge` com rótulo). Ações por linha: "Visualizar" sempre; com `canEdit`: "Editar" e "Aprovar" (só DRAFT), "Marcar como perdido" (DRAFT/APPROVED; `AlertDialog` que, em APPROVED, avisa "As parcelas pendentes serão canceladas."), "Excluir" (DRAFT/LOST; `AlertDialog`). Vazio: "Nenhum orçamento para este paciente.".
- [ ] `BudgetViewDialog({ contactId, budgetId, onOpenChange })`: checkboxes "Valor total", "Valor por procedimento", "Dentistas", "Desconto", "Observações", "Forma de pagamento" (padrão todos; lembra a escolha em `localStorage` chave `budget-view-blocks`, com try/catch); botão "Abrir para impressão" → `window.open(\`/patients/${contactId}/orcamentos/${budgetId}/imprimir?blocos=${lista}\`)`.
- [ ] `BudgetPrintView({ budget, blocks, patientName })`: cabeçalho com título, paciente e data; tabela de procedimentos (coluna valor só com "Valor por procedimento", coluna dentista só com "Dentistas"); linhas de subtotal/desconto (bloco "Desconto") e total (bloco "Valor total"); observações; forma de pagamento + parcelas (bloco "Forma de pagamento", só se aprovado). A página `imprimir/page.tsx` lê `blocos` da URL, carrega `useBudget` e `usePatient`, renderiza e chama `window.print()` após carregar (`useEffect`).
- [ ] `PatientBudgetsTab({ companyId, contactId })`: botão "+ Novo orçamento" (com `can("contact:update")`), `BudgetList`, diálogos. Página `orcamentos/page.tsx` como `ficha-clinica/page.tsx`. Aba `{ label: "Orçamentos", segment: "orcamentos" }` logo após "Ficha clínica".
- [ ] Testes: lista mostra ações por status (DRAFT tem Aprovar/Editar; APPROVED não; LOST tem Excluir e não Marcar como perdido) e aviso de cancelamento em APPROVED; impressão oculta coluna de valores e total quando os blocos não estão selecionados.
- [ ] Verificação completa do frontend + commit `feat(pacientes): aba Orçamentos na ficha do paciente`.

---

### Task 11: Integração na ficha clínica (frontend)

**Files:** Modify `features/clinical/procedures/components/{ProcedureList,CompleteProcedureDialog}.tsx` e seus testes

- [ ] `ProcedureList`: coluna "Orçamento" com o título e o status (`BUDGET_STATUS_LABELS`) ou "—". Excluir procedimento de orçamento APPROVED: botão desabilitado com `title="Procedimento de orçamento aprovado não pode ser excluído"`; em DRAFT o `AlertDialog` acrescenta "O procedimento será removido do orçamento <título>.".
- [ ] `CompleteProcedureDialog`: quando `procedure.budget?.status === "APPROVED"`, não mostra "Gerar lançamento financeiro" e mostra "Cobrado pelo orçamento <título>.".
- [ ] Testes para os três comportamentos.
- [ ] Verificação + commit `feat(pacientes): orçamento na ficha clínica`.

---

### Task 12: Fechamento

- [ ] Spec `2026-10-23-orcamentos-design.md`: status "Implementada" e qualquer ajuste ao que foi de fato construído; spec mãe `2026-10-12-ficha-do-paciente-design.md`: "Fase 3 — implementada (3a ficha clínica, 3b orçamentos)".
- [ ] Rodar todas as verificações e colar as últimas linhas em "Resultado final" abaixo; preencher "Divergências encontradas".
- [ ] Commit `docs(pacientes): marca a Fase 3b da ficha como implementada`.

## Critérios de aceite

1. Aba Orçamentos após Ficha clínica; criar/editar rascunho com procedimentos "A realizar", desconto e totais corretos.
2. Aprovar gera N parcelas (resto na 1ª) categoria Procedimento ligadas ao paciente; sem `finance:create` → 403.
3. Perder com parcela paga → 409; com pendentes → canceladas. Excluir aprovado → 409.
4. Procedimento não entra em dois orçamentos ativos; concluir procedimento de orçamento aprovado não gera lançamento duplo.
5. Visualizar/imprimir respeita os blocos escolhidos.
6. `BudgetIsolationIT` e todo o CI verdes.

## Divergências encontradas

- Página de impressão no grupo `app/(print)/` (layout próprio, sem sidebar/header), URL igual; no plano estava em `(dashboard)`.
- Blocos na URL com chaves `total,procedures,dentists,discount,notes,payment` (`utils/blocks.ts`), não rótulos.
- Célula "Orçamento" na ficha clínica mostra "título · status".
- Botões de confirmação "Confirmar" / "Confirmar exclusão" nos AlertDialogs.
- Task 3: `transactions` como `Set<BudgetTransactionLink>` (evita múltiplos bags EAGER, que quebrariam o boot com `MultipleBagFetchException`).
- Task 9: reset dos diálogos só ao abrir ou mudar o id do orçamento (não por nova referência de dados).

## Resultado final

Verificação executada em 2026-10-23 na branch `feature/pacientes-fase3b`:

- Backend `cd backend && mvn -q test`: saiu com código 0, sem falhas em testes unitários (surefire). Os ITs (failsafe, Testcontainers) não rodam nesse comando; `BudgetIsolationIT` e `PatientProcedureIsolationIT` dependem de Docker, ausente localmente. O CI do commit 52702da já tinha `BudgetIsolationIT` verde no Postgres real.
- Frontend `npm run lint`: código 0, 0 erros, 52 avisos pré-existentes (nenhum nos arquivos de orçamento).
- Frontend `npx tsc --noEmit`: sem saída (sem erros).
- Frontend `npx vitest run --pool=threads`: 116 arquivos passando, 1 falhando (`SidebarGroup.test.tsx`, 5 testes, falha de ambiente local); 592 testes passando, 5 falhando.
- Prettier `--check --end-of-line auto` nos 27 arquivos .ts/.tsx alterados na branch desde `d215ac8a`: "All matched files use Prettier code style!".
