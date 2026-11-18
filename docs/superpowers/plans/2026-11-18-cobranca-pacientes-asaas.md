# Cobrança de pacientes pelo Asaas — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Receitas pendentes com contato viram cobranças Asaas (link Pix/boleto/cartão), enviadas por e-mail (Asaas) e WhatsApp (CRM), com baixa automática pelo webhook e baixa na recepção refletida no Asaas.

**Architecture:** Módulo isolado `finance.patientbilling` com tabelas próprias (não altera o domínio de lançamentos/contatos). Liga-se ao resto por eventos AFTER_COMMIT publicados por `FinancialTransactionService` e `BudgetService`, e pelo roteamento do webhook (`externalReference` = `tx:<companyId>:<transactionId>`). Reaproveita `AsaasClient`, `BillingProperties`, `BillingTenant`, o webhook e o reprocessamento da V140.

**Tech Stack:** Spring Boot (Java 25), JdbcTemplate, PostgreSQL RLS, Next.js/React Query, Vitest, JUnit/Mockito.

**Spec:** `docs/superpowers/specs/2026-11-18-cobranca-pacientes-asaas-design.md`

## Global Constraints

- Ajuste ao spec: dados Asaas ficam em tabelas próprias (`patient_charges`, `patient_billing_customers`, `patient_billing_companies`), não em colunas de `financial_transactions`/`contacts`/`companies` — mesmo comportamento, menos acoplamento.
- Migration **V141** (base tem V140). Toda tabela com `company_id`: RLS ENABLE+FORCE, `tenant_isolation_policy` com `app.current_tenant_id()`, GRANT para `crm_app`.
- Chamadas HTTP ao Asaas nunca dentro de `@Transactional`.
- `billingType=UNDEFINED`; cliente com notificações do Asaas ligadas; `receiveInCash` com `notifyCustomer=false`.
- CRM_ADMIN = papel `SUPER_ADMIN` (`CurrentUser.isPlatformAdmin()`).
- Falha do Asaas/WhatsApp nunca bloqueia a operação financeira; vira status `FAILED`/aviso.
- Testes de backend: o usuário roda `mvn test` (ou o CI); frontend: `npx vitest run --pool=threads`, `tsc`, Prettier `--end-of-line auto`.

---

### Task 1: Migration V141 + PatientChargeStore

**Files:**
- Create: `backend/src/main/resources/db/migration/V141__patient_billing.sql`
- Create: `backend/src/main/java/com/becommerce/crm/finance/patientbilling/PatientChargeStore.java`

**Interfaces — Produces:**
- `record PatientCharge(UUID transactionId, UUID companyId, String asaasPaymentId, String invoiceUrl, String status, String error)`
- `PatientChargeStore`: `Optional<PatientCharge> find(UUID companyId, UUID transactionId)`, `List<PatientCharge> findAll(UUID companyId, Collection<UUID> txIds)`, `void upsert(PatientCharge c)`, `void setStatus(UUID companyId, UUID txId, String status, String error)`, `Optional<String> customerId(UUID companyId, UUID contactId)`, `void saveCustomer(UUID companyId, UUID contactId, String asaasCustomerId)`, `boolean enabled(UUID companyId)`, `void setEnabled(UUID companyId, boolean on)`. Todos rodam dentro de `BillingTenant.run(companyId, …)`.

- [ ] SQL das 3 tabelas com RLS/GRANT; store com JdbcTemplate (`INSERT … ON CONFLICT … DO UPDATE`).
- [ ] Commit.

### Task 2: AsaasClient — cobrança avulsa

**Files:** Modify `billing/infrastructure/AsaasClient.java`; Test `AsaasClientTest` (MockRestServiceServer, padrão existente).

**Produces:** `String createPatientCustomer(String name, String cpf, String email, String mobile, String ref)`; `record CreatedPayment(String id, String invoiceUrl)`; `CreatedPayment createPayment(String customerId, long valueCents, LocalDate dueDate, String description, String ref)`; `void receiveInCash(String paymentId, LocalDate date, long valueCents)`; `void deletePayment(String paymentId)` (404 = ok).

- [ ] Testes de corpo/rota (billingType UNDEFINED, notifyCustomer false, 404 no delete).
- [ ] Implementação + commit.

### Task 3: PatientBillingService

**Files:** Create `finance/patientbilling/PatientBillingService.java`, `PatientBillingException.java`; Test `PatientBillingServiceTest`.

**Consumes:** Tasks 1–2; `FinancialTransactionRepository.findById`, `ContactRepository.findById`, `WhatsAppConnectionService.sendText`, `FinancialTransactionService.pay`.

**Produces:** `record ChargeView(UUID transactionId, String status, String invoiceUrl, String error, boolean whatsappSent, String whatsappError)`; `ChargeView charge(UUID companyId, UUID txId)`; `ChargeView resendWhatsApp(UUID companyId, UUID txId)`; `void onPaidInClinic(UUID companyId, UUID txId)`; `void onCancelled(UUID companyId, UUID txId)`; `boolean applyWebhook(AsaasPayment p)`; `static String ref(UUID companyId, UUID txId)`.

Regras de `charge`: recurso ligado; lançamento INCOME + PENDING + contactId; contato com CPF; cobrança ativa (PENDING) existente → devolve a mesma; vencimento = max(dueDate, hoje SP); falha Asaas → status FAILED + error.

- [ ] Testes: cria e envia WhatsApp; reaproveita cliente; sem CPF; desligado; falha Asaas = FAILED; WhatsApp falha não derruba; receiveInCash só se PENDING; delete só se PENDING; webhook RECEIVED baixa o lançamento e marca RECEIVED antes do pay; webhook repetido idempotente.
- [ ] Implementação + commit.

### Task 4: Eventos e webhook

**Files:** Modify `FinancialTransactionService` (publica `FinancialTransactionChangedEvent(companyId, txId, Kind{PAID,CANCELLED,DELETED})` via setter `@Autowired(required=false)`), `ApproveBudgetRequest` (+ `Boolean chargeViaAsaas`, construtor de 3 args mantido), `BudgetService.approve` (publica `BudgetChargeRequestedEvent(companyId, List<UUID> txIds)`), `BillingPaymentService.apply` (desvia `tx:`); Create `PatientBillingListener` (`@TransactionalEventListener(AFTER_COMMIT, fallbackExecution=true)`).

- [ ] Testes: apply com ref `tx:` chama `applyWebhook` e não toca licença; listener chama `onPaidInClinic`/`onCancelled`/`charge`.
- [ ] Commit.

### Task 5: API

**Files:** Create `PatientBillingController` (`/api/v1/companies/{companyId}/patient-billing`: `GET` → `{enabled}`; `GET /charges?transactionIds=` → `List<ChargeView>`; `POST /transactions/{id}/charge`; `POST /transactions/{id}/whatsapp`; `finance:read`/`finance:manage`), endpoint admin `PUT /api/v1/admin/billing/companies/{id}/patient-billing {enabled}` em `BillingAdminController` (só SUPER_ADMIN, auditado). Erros de regra → 400 com `message`.

- [ ] Testes de controller (standalone MockMvc, padrão do BillingAdminControllerTest).
- [ ] Commit.

### Task 6: Frontend

**Files:** Create `frontend-refine/src/features/finance/patient-billing/{service.ts,hooks.ts,ChargeActions.tsx}`; Modify `TransactionsTable.tsx` (coluna "Cobrança Asaas" com selo + ações), diálogo de aprovação do orçamento (checkbox), `AdminBillingCard` (interruptor).

- [ ] Testes Vitest: ações por status, checkbox só com recurso ligado.
- [ ] tsc, Prettier, commit.

### Task 7: Entrega

- [ ] Push, PR, CI verde, merge, deploy backend+frontend, conferir V140/V141.
- [ ] Teste real: lançamento de R$ 5,00 para contato do próprio dono, link, pagamento, baixa automática.
