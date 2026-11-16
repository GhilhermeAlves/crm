# Simulador de vendas no crédito — Plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Simular parcelamento no crédito (1x–12x) com taxas da maquininha por clínica e modelos de mercado, no orçamento e no Financeiro.

**Architecture:** Backend Spring (módulo `finance/cardfee`) com duas tabelas: `card_fee_rates` (por clínica, RLS) e `card_fee_presets` + `card_fee_preset_rates` (globais, escrita só CRM_ADMIN). Frontend Next (`features/finance/credit`) com função pura de cálculo, hooks React Query, componente `CreditSimulator` reaproveitado no orçamento, numa página do Financeiro e telas de configuração/admin.

**Tech Stack:** Java 21 / Spring Boot / JPA / Flyway / Postgres RLS; Next.js + React Query + shadcn/ui + Vitest.

**Spec:** `docs/superpowers/specs/2026-11-15-simulador-credito-design.md`

## Global Constraints

- Migration `V139__card_fees.sql` (próximo após V138; conferir que não existe outra V139 na branch base antes de commitar).
- Tabela por clínica: RLS `ENABLE` + `FORCE` + `tenant_isolation_policy` com `app.current_tenant_id()` + `GRANT ... TO crm_app` (padrão V113).
- CRM_ADMIN (`CurrentUser.isPlatformAdmin()`, papel `SUPER_ADMIN` no frontend) sempre passa em toda guarda nova.
- Taxa: `0 ≤ taxa < 100`, 2 casas; parcelas 1–12.
- Testes de backend: o usuário roda (`mvn test`); não rodar.
- Frontend: `npx vitest run <arquivo>` e `npx prettier --check --end-of-line auto <arquivos>` em todo arquivo alterado.
- Textos da UI em pt-BR. Nada gravado no orçamento.

---

### Task 1: Migration + carga dos modelos

**Files:** Create `backend/src/main/resources/db/migration/V139__card_fees.sql`

- [ ] Criar tabelas `card_fee_rates` (id, company_id FK companies ON DELETE CASCADE, installments SMALLINT 1–12, rate_percent NUMERIC(5,2) CHECK 0 ≤ x < 100, updated_at; UNIQUE(company_id, installments); RLS + GRANT), `card_fee_presets` (id, name UNIQUE, estimated BOOL, checked_at DATE, source_note, active BOOL, created_at, updated_at; GRANT, sem RLS) e `card_fee_preset_rates` (preset_id FK ON DELETE CASCADE, installments, rate_percent; PK(preset_id, installments); GRANT).
- [ ] Carga: 8 modelos do spec; `rate(N) = round(t1 + (t12 − t1)·(N−1)/11, 2)` via `generate_series(1,12)`; `estimated = true`, `checked_at = '2026-11-13'`.
- [ ] Commit `feat(finance): tabelas de taxas do cartão e modelos de maquininha`.

### Task 2: Backend — taxas da clínica

**Files:** Create em `backend/src/main/java/com/becommerce/crm/finance/cardfee/`: `CardFeeRateJpaEntity`, `CardFeeRateJpaRepository`, `CardFeeDtos`, `CardFeeService`, `CardFeeController`. Test: `backend/src/test/java/com/becommerce/crm/finance/cardfee/CardFeeServiceTest.java`, `CardFeeControllerTest.java`.

**Interfaces (Produces):**
- `GET /api/v1/companies/{companyId}/card-fees` (`finance:read`) → `[{installments:int, ratePercent:number}]` ordenado.
- `PUT /api/v1/companies/{companyId}/card-fees` (`finance:manage`) body `{rates:[{installments, ratePercent}]}` → mesma lista. Substitui tudo (delete + insert), na RLS da empresa (`TenantContext`).
- `CardFeeDtos.validate(List<RateItem>)`: exatamente 12, parcelas 1–12 sem repetição, 0 ≤ taxa < 100, escala ≤ 2 → `ResponseStatusException(400)` com mensagem pt-BR.

- [ ] Teste do serviço (validação: 11 itens → 400; parcela 13 → 400; repetida → 400; taxa 100 → 400; taxa −1 → 400; válida → salva 12 e devolve ordenado).
- [ ] Teste do controller (standalone MockMvc, padrão `BillingAdminOverviewControllerTest`): GET 200 com lista; PUT 200 chama serviço.
- [ ] Implementar; commit.

### Task 3: Backend — modelos (presets)

**Files:** `CardFeePresetJpaEntity` (com `@ElementCollection` → `card_fee_preset_rates`), `CardFeePresetJpaRepository`, `CardFeePresetService`, `CardFeePresetController`, `CardFeePresetAdminController`; testes `CardFeePresetAdminControllerTest`, `CardFeePresetServiceTest`.

**Interfaces (Produces):**
- `GET /api/v1/card-fee-presets` (autenticado) → ativos: `[{id, name, estimated, checkedAt, sourceNote, rates:[{installments, ratePercent}]}]` ordenado por nome.
- `GET/POST/PUT/DELETE /api/v1/admin/card-fee-presets[/{id}]` → só `isPlatformAdmin()` (403 senão). Body `{name, estimated, checkedAt, sourceNote, active, rates[12]}` validado com `CardFeeDtos.validate`. DELETE = desativar (`active=false`).

- [ ] Testes: não-admin → 403 sem chamar serviço; admin cria/edita/desativa; lista pública só ativos.
- [ ] Implementar; commit.

### Task 4: Frontend — cálculo puro

**Files:** Create `frontend-refine/src/features/finance/credit/utils/creditSimulation.ts`; Test `frontend-refine/src/__tests__/features/finance/credit/creditSimulation.test.ts`.

**Interfaces (Produces):**
```ts
export type FeeRates = Record<number, number | null>; // 1..12 → % ou null
export type SimulationRow = { installments: number; ratePercent: number | null;
  patientTotalCents: number | null; installmentCents: number | null;
  firstInstallmentCents: number | null; netCents: number | null; costCents: number | null };
export function simulateCredit(amountCents: number, rates: FeeRates, passFeeToPatient: boolean): SimulationRow[];
export function isValidRate(rate: number | null | undefined): rate is number; // 0 <= r < 100
```
- [ ] Testes: R$1.000, 10% sem repasse → total 100000, líquido 90000, custo 10000, 10x de 10000; com repasse → total 111111, líquido 100000 (arredondado); resto na 1ª parcela (R$100,00 em 3x → 3334 / 3333); taxa 0; taxa null → linha toda null; 12 linhas sempre.
- [ ] Implementar; vitest; prettier; commit.

### Task 5: Frontend — hooks e serviço

**Files:** Create `frontend-refine/src/features/finance/credit/hooks/useCardFees.ts`.

**Interfaces (Produces):** `useCardFees(companyId)`, `useSaveCardFees(companyId)`, `useCardFeePresets()`, `useAdminCardFeePresets()`, `useSaveCardFeePreset()`, `useDeactivateCardFeePreset()`; tipos `CardFeeRate {installments; ratePercent}`, `CardFeePreset {id; name; estimated; checkedAt; sourceNote; active?; rates: CardFeeRate[]}`; helper `ratesToMap(list): FeeRates`, `mapToRates(map): CardFeeRate[]`. Padrão `useBudgetTitles` (api, `useMutationDefaults`, invalidação).

- [ ] Implementar; prettier; commit.

### Task 6: Frontend — `CreditSimulator` + seletor de modelo

**Files:** Create `features/finance/credit/components/CreditSimulator.tsx`, `PresetPicker.tsx`, `RatesNotice.tsx`; Test `__tests__/features/finance/credit/CreditSimulator.test.tsx`.

Props `CreditSimulator({ amountCents, companyId })`. Estado local das taxas iniciado das taxas da clínica; Switch "Repassar juros ao paciente"; tabela 1x–12x (Parcelas, Valor da parcela, Total do paciente, Taxa % editável, Clínica recebe, Custo); botões "Usar modelo" (PresetPicker) e "Voltar às taxas cadastradas"; sem taxas cadastradas → aviso com link `/finance/card-fees`.

- [ ] Teste (mock dos hooks): 12 linhas; ligar repasse altera total; editar taxa altera a linha; "Voltar às taxas cadastradas" restaura.
- [ ] Implementar; vitest; prettier; commit.

### Task 7: Frontend — páginas e entradas

**Files:**
- Create `app/(dashboard)/finance/card-fees/page.tsx` (12 inputs, PresetPicker, Salvar; só `finance:manage` edita).
- Create `app/(dashboard)/finance/credit-simulator/page.tsx` (campo de valor + CreditSimulator).
- Create `app/(dashboard)/card-fee-presets/page.tsx` + `layout.tsx` (guarda SUPER_ADMIN igual `tenants/layout.tsx`), lista + diálogo de edição.
- Modify `lib/constants.ts` (ROUTES), `components/layout/navigation.ts` (Financeiro com filhos: Controle, Simulador de crédito, Taxas do cartão; admin: "Modelos de taxa" `crmAdminOnly`).
- Modify `features/clinical/budgets/components/BudgetViewDialog.tsx`: botão "Simular no crédito" abre Dialog com `CreditSimulator` no total do orçamento.

- [ ] Implementar; `npx tsc --noEmit`; vitest dos testes de budgets/navigation; prettier; commit.

### Task 8: Verificação

- [ ] `npx vitest run`, `npx tsc --noEmit`, `npx eslint` nos arquivos alterados, prettier.
- [ ] Passar ao usuário: `cd backend && mvn test -Dtest='CardFee*'`.
