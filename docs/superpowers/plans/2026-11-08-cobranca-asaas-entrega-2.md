# Cobrança Asaas — Entrega 2 (webhook, job diário, bloqueio) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Receber pagamentos/atrasos do Asaas por webhook, conciliar diariamente, bloquear com 402 a empresa com 10+ dias de atraso e expor os endpoints de cobrança da clínica ("Já paguei") e do CRM_ADMIN ("Liberar por X dias", faturas).

**Architecture:** Continua o módulo `com.becommerce.crm.billing` da entrega 1. Um único caminho aplica um pagamento vindo do Asaas (`BillingPaymentService.apply`), usado pelo webhook, pelo "Já paguei" e pelo job diário — e sempre a partir de `GET /v3/payments/{id}` (nunca do corpo do webhook). Toda leitura/escrita em `billing_*` roda dentro de `BillingTenant.run(companyId, ...)` com `saveAndFlush`, porque as tabelas têm RLS FORCE e o webhook/job não têm tenant. O bloqueio é um `HandlerInterceptor` no padrão do `ModuleGuardInterceptor`.

**Tech Stack:** Java 21, Spring Boot (RestClient, Spring Data JPA, JdbcTemplate, `@Scheduled`, `HandlerInterceptor`), JUnit 5 + Mockito + `MockRestServiceServer` + MockMvc standalone.

**Spec:** `docs/superpowers/specs/2026-11-08-cobranca-asaas-design.md`

## Global Constraints

- Atraso: venceu sem pagar → `PAST_DUE` (uso normal). 10 dias de atraso → `BLOCKED`.
- Bloqueio: login funciona; API responde 402 `{"code":"BILLING_BLOCKED"}` exceto `/api/v1/billing/**`, identidade (`/api/v1/me/**`, `/api/v1/auth/**`, `/api/v1/me/memberships`, `GET /api/v1/companies/me`) e rotas públicas (`/api/v1/public/**`). Dados nunca são apagados.
- CRM_ADMIN nunca é bloqueado; empresa `EXEMPT` não é cobrada nem bloqueada.
- Webhook: `POST /api/v1/public/billing/asaas/webhook`; header `asaas-access-token` comparado em tempo constante com `ASAAS_WEBHOOK_TOKEN` (vazio → sempre 401); inválido → 401; evento repetido ignorado via `billing_webhook_events`; eventos tratados `PAYMENT_CREATED`, `PAYMENT_CONFIRMED`, `PAYMENT_RECEIVED`, `PAYMENT_OVERDUE`, `PAYMENT_REFUNDED`, `PAYMENT_DELETED`, outros registrados e ignorados; antes de liberar consulta `GET /v3/payments/{id}`; sempre 200 depois de registrar.
- Job diário às 03:30 (America/Sao_Paulo): bloqueia `PAST_DUE` com `overdue_since ≤ hoje − 10` sem `grace_until` vigente; reenvia `SYNC_PENDING`; expira `grace_until`.
- Chave/token do Asaas nunca em código, log ou mensagem de erro. Logs do webhook sem payload completo.
- Ações de CRM_ADMIN (liberar por X dias) registradas na auditoria, no tenant da empresa alvo.
- CRM_ADMIN sempre passa em qualquer regra nova.
- Sem Docker local: testes de backend são rodados pelo usuário (`mvn test -Dtest=...`), nunca pelo agente; o agente só roda `mvn -q -o -DskipTests test-compile`.
- Sem migration nova: `billing_accounts`, `billing_invoices` e `billing_webhook_events` já existem (V130).
- Fora desta entrega: telas, interceptador 402 do frontend, nginx, env da VPS (entregas 3 e 4).

## File Structure

```
backend/src/main/java/com/becommerce/crm/billing/
  domain/BillingAccount.java            (+ onOverdue, onPaid, shouldBlock, block, grant, expireGrace, nextDueAt, isPriceSyncPending)
  domain/BillingInvoice.java            (+ fromAsaas, refresh, isOpen)
  infrastructure/AsaasClient.java       (+ getPayment, getSubscriptionReference, record AsaasPayment)
  infrastructure/BillingInvoiceJpaRepository.java (+ consultas)
  infrastructure/BillingWebhookEventStore.java    (novo; JdbcTemplate)
  application/BillingTenant.java        (novo; troca de tenant compartilhada)
  application/BillingPaymentService.java (novo; aplica pagamento do Asaas)
  application/AsaasWebhookService.java  (novo)
  application/BillingDailyJob.java      (novo)
  application/BillingAccessService.java (novo; "está bloqueada?" com cache curto)
  application/BillingService.java       (+ grant, retrySync, invoices, checkPayment; usa BillingTenant)
  web/AsaasWebhookController.java       (novo)
  web/BillingGuardInterceptor.java      (novo)
  web/BillingGuardWebConfig.java        (novo)
  web/BillingBlockedException.java      (novo)
  web/BillingController.java            (novo; empresa logada)
  web/BillingAdminController.java       (+ grant, invoices)
backend/src/main/java/com/becommerce/crm/shared/security/config/SecurityConfig.java (+ permitAll webhook)
backend/src/main/java/com/becommerce/crm/shared/web/handler/GlobalExceptionHandler.java (+ 402)
backend/src/main/resources/application.yml (+ app.billing.daily-cron)
backend/src/test/java/com/becommerce/crm/billing/... (espelha acima)
```

---

### Task 1: Regras de atraso, bloqueio e carência no domínio

**Files:**
- Modify: `backend/src/main/java/com/becommerce/crm/billing/domain/BillingAccount.java`
- Modify: `backend/src/main/java/com/becommerce/crm/billing/domain/BillingInvoice.java`
- Modify: `backend/src/main/java/com/becommerce/crm/billing/infrastructure/BillingInvoiceJpaRepository.java`
- Test: `backend/src/test/java/com/becommerce/crm/billing/domain/BillingAccountTest.java` (acrescentar), `backend/src/test/java/com/becommerce/crm/billing/domain/BillingInvoiceTest.java` (novo)

**Interfaces:**
- Produces (`BillingAccount`): `static final int BLOCK_AFTER_DAYS = 10`; `void onOverdue(LocalDate since)`; `void onPaid()`; `boolean shouldBlock(LocalDate today)`; `void block()`; `void grant(LocalDate until)`; `void expireGrace(LocalDate today)`; `void nextDueAt(LocalDate due)`; `boolean isPriceSyncPending()`.
- Produces (`BillingInvoice`): `static BillingInvoice fromAsaas(UUID companyId, String asaasPaymentId, LocalDate dueDate, int valueCents, InvoiceStatus status, String invoiceUrl, LocalDateTime paidAt)`; `void refresh(LocalDate dueDate, int valueCents, InvoiceStatus status, String invoiceUrl, LocalDateTime paidAt)`; `boolean isOpen()` (PENDING ou OVERDUE).
- Produces (repositório): `Optional<BillingInvoice> findByAsaasPaymentId(String id)`; `Optional<BillingInvoice> findFirstByCompanyIdAndStatusOrderByDueDateAsc(UUID companyId, InvoiceStatus status)`; `List<BillingInvoice> findByCompanyIdAndStatusInAndDueDateBefore(UUID companyId, Collection<InvoiceStatus> statuses, LocalDate date)`; `List<BillingInvoice> findByCompanyIdAndStatusIn(UUID companyId, Collection<InvoiceStatus> statuses)`.

Regras:
- `onOverdue(since)`: `EXEMPT` não muda nada. `ACTIVE` → `PAST_DUE`. `PAST_DUE`/`BLOCKED` mantêm o status. `overdueSince` passa a ser `since` (a fatura vencida mais antiga, calculada pelo chamador).
- `onPaid()`: `PAST_DUE`/`BLOCKED` → `ACTIVE`; zera `overdueSince` e `graceUntil`. Outros status só zeram `overdueSince`.
- `shouldBlock(today)`: `status == PAST_DUE` e `overdueSince != null` e `!overdueSince.isAfter(today.minusDays(10))` e (`graceUntil == null` ou `graceUntil.isBefore(today)`).
- `block()`: só se `PAST_DUE` → `BLOCKED`.
- `grant(until)`: `graceUntil = until`; se `BLOCKED` → `PAST_DUE`. Lança `IllegalArgumentException` se `until` for nulo.
- `expireGrace(today)`: se `graceUntil != null && graceUntil.isBefore(today)` → `graceUntil = null`.
- `nextDueAt(due)`: grava se `nextDueDate == null` ou `due.isAfter(nextDueDate)`.
- `isPriceSyncPending()`: `hasSubscription() && syncError != null && status != SYNC_PENDING`.

- [ ] **Step 1: Testes da conta (acrescentar em `BillingAccountTest`)**

```java
    private BillingAccount active() {
        BillingAccount a = BillingAccount.start(company, CompanyPlan.STARTER, 19700);
        a.activated("cus_1", "sub_1", LocalDate.of(2026, 11, 15));
        return a;
    }

    @Test
    void overdueMovesActiveToPastDue() {
        BillingAccount a = active();
        a.onOverdue(LocalDate.of(2026, 11, 15));
        assertEquals(BillingStatus.PAST_DUE, a.getStatus());
        assertEquals(LocalDate.of(2026, 11, 15), a.getOverdueSince());
    }

    @Test
    void overdueIgnoredWhenExempt() {
        BillingAccount a = active();
        a.markExempt();
        a.onOverdue(LocalDate.of(2026, 11, 15));
        assertEquals(BillingStatus.EXEMPT, a.getStatus());
        assertNull(a.getOverdueSince());
    }

    @Test
    void blocksOnlyAfterTenDays() {
        BillingAccount a = active();
        a.onOverdue(LocalDate.of(2026, 11, 15));
        assertFalse(a.shouldBlock(LocalDate.of(2026, 11, 24)));
        assertTrue(a.shouldBlock(LocalDate.of(2026, 11, 25)));
    }

    @Test
    void graceDelaysBlockUntilItExpires() {
        BillingAccount a = active();
        a.onOverdue(LocalDate.of(2026, 11, 15));
        a.grant(LocalDate.of(2026, 11, 30));
        assertFalse(a.shouldBlock(LocalDate.of(2026, 11, 30)));
        assertTrue(a.shouldBlock(LocalDate.of(2026, 12, 1)));
    }

    @Test
    void grantUnblocksAndPaymentReactivates() {
        BillingAccount a = active();
        a.onOverdue(LocalDate.of(2026, 11, 15));
        a.block();
        assertEquals(BillingStatus.BLOCKED, a.getStatus());
        a.grant(LocalDate.of(2026, 11, 30));
        assertEquals(BillingStatus.PAST_DUE, a.getStatus());
        a.block();
        a.onPaid();
        assertEquals(BillingStatus.ACTIVE, a.getStatus());
        assertNull(a.getOverdueSince());
        assertNull(a.getGraceUntil());
    }

    @Test
    void blockOnlyFromPastDue() {
        BillingAccount a = active();
        a.block();
        assertEquals(BillingStatus.ACTIVE, a.getStatus());
    }

    @Test
    void expireGraceClearsPastDate() {
        BillingAccount a = active();
        a.grant(LocalDate.of(2026, 11, 20));
        a.expireGrace(LocalDate.of(2026, 11, 20));
        assertEquals(LocalDate.of(2026, 11, 20), a.getGraceUntil());
        a.expireGrace(LocalDate.of(2026, 11, 21));
        assertNull(a.getGraceUntil());
    }

    @Test
    void nextDueOnlyMovesForward() {
        BillingAccount a = active();
        a.nextDueAt(LocalDate.of(2026, 11, 10));
        assertEquals(LocalDate.of(2026, 11, 15), a.getNextDueDate());
        a.nextDueAt(LocalDate.of(2026, 12, 15));
        assertEquals(LocalDate.of(2026, 12, 15), a.getNextDueDate());
    }

    @Test
    void priceSyncPendingOnlyWithSubscriptionAndError() {
        BillingAccount a = active();
        assertFalse(a.isPriceSyncPending());
        a.priceSyncFailed("timeout");
        assertTrue(a.isPriceSyncPending());
    }
```

- [ ] **Step 2: Teste da fatura (`BillingInvoiceTest`, novo)**

```java
package com.becommerce.crm.billing.domain;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class BillingInvoiceTest {
    @Test
    void fromAsaasAndRefresh() {
        UUID company = UUID.randomUUID();
        BillingInvoice i = BillingInvoice.fromAsaas(company, "pay_1", LocalDate.of(2026, 11, 15), 19700,
                InvoiceStatus.PENDING, "https://pix/1", null);
        assertNotNull(i.getId());
        assertEquals(company, i.getCompanyId());
        assertTrue(i.isOpen());

        LocalDateTime paid = LocalDateTime.of(2026, 11, 14, 10, 0);
        i.refresh(LocalDate.of(2026, 11, 15), 19700, InvoiceStatus.RECEIVED, "https://pix/1", paid);
        assertEquals(InvoiceStatus.RECEIVED, i.getStatus());
        assertEquals(paid, i.getPaidAt());
        assertFalse(i.isOpen());
    }
}
```

- [ ] **Step 3: Usuário roda e vê falhar** — `cd backend && mvn test -Dtest="BillingAccountTest,BillingInvoiceTest"` → FAIL (métodos não existem).

- [ ] **Step 4: Implementação em `BillingAccount`** (acrescentar abaixo de `changePrice`):

```java
    public static final int BLOCK_AFTER_DAYS = 10;

    public void onOverdue(LocalDate since) {
        if (status == BillingStatus.EXEMPT) return;
        if (status == BillingStatus.ACTIVE) status = BillingStatus.PAST_DUE;
        this.overdueSince = since;
        touch();
    }

    public void onPaid() {
        if (status == BillingStatus.PAST_DUE || status == BillingStatus.BLOCKED) {
            status = BillingStatus.ACTIVE;
            this.graceUntil = null;
        }
        this.overdueSince = null;
        touch();
    }

    public boolean shouldBlock(LocalDate today) {
        return status == BillingStatus.PAST_DUE
                && overdueSince != null
                && !overdueSince.isAfter(today.minusDays(BLOCK_AFTER_DAYS))
                && (graceUntil == null || graceUntil.isBefore(today));
    }

    public void block() {
        if (status != BillingStatus.PAST_DUE) return;
        status = BillingStatus.BLOCKED;
        touch();
    }

    public void grant(LocalDate until) {
        if (until == null) throw new IllegalArgumentException("Informe até quando liberar.");
        this.graceUntil = until;
        if (status == BillingStatus.BLOCKED) status = BillingStatus.PAST_DUE;
        touch();
    }

    public void expireGrace(LocalDate today) {
        if (graceUntil != null && graceUntil.isBefore(today)) {
            graceUntil = null;
            touch();
        }
    }

    public void nextDueAt(LocalDate due) {
        if (due != null && (nextDueDate == null || due.isAfter(nextDueDate))) {
            nextDueDate = due;
            touch();
        }
    }

    public boolean isPriceSyncPending() {
        return hasSubscription() && syncError != null && status != BillingStatus.SYNC_PENDING;
    }
```

- [ ] **Step 5: Implementação em `BillingInvoice`** (acrescentar abaixo do construtor protegido):

```java
    public static BillingInvoice fromAsaas(UUID companyId, String asaasPaymentId, LocalDate dueDate, int valueCents,
                                           InvoiceStatus status, String invoiceUrl, LocalDateTime paidAt) {
        BillingInvoice i = new BillingInvoice();
        i.id = UUID.randomUUID();
        i.companyId = companyId;
        i.asaasPaymentId = asaasPaymentId;
        i.refresh(dueDate, valueCents, status, invoiceUrl, paidAt);
        return i;
    }

    public void refresh(LocalDate dueDate, int valueCents, InvoiceStatus status, String invoiceUrl,
                        LocalDateTime paidAt) {
        this.dueDate = dueDate;
        this.valueCents = valueCents;
        this.status = status;
        this.invoiceUrl = invoiceUrl;
        this.paidAt = paidAt;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isOpen() {
        return status == InvoiceStatus.PENDING || status == InvoiceStatus.OVERDUE;
    }
```

- [ ] **Step 6: Repositório de faturas**

```java
package com.becommerce.crm.billing.infrastructure;

import com.becommerce.crm.billing.domain.BillingInvoice;
import com.becommerce.crm.billing.domain.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BillingInvoiceJpaRepository extends JpaRepository<BillingInvoice, UUID> {
    List<BillingInvoice> findTop24ByCompanyIdOrderByDueDateDesc(UUID companyId);

    Optional<BillingInvoice> findByAsaasPaymentId(String asaasPaymentId);

    Optional<BillingInvoice> findFirstByCompanyIdAndStatusOrderByDueDateAsc(UUID companyId, InvoiceStatus status);

    List<BillingInvoice> findByCompanyIdAndStatusIn(UUID companyId, Collection<InvoiceStatus> statuses);

    List<BillingInvoice> findByCompanyIdAndStatusInAndDueDateBefore(UUID companyId,
                                                                    Collection<InvoiceStatus> statuses,
                                                                    LocalDate date);
}
```

- [ ] **Step 7: Usuário roda e vê passar** — `cd backend && mvn test -Dtest="BillingAccountTest,BillingInvoiceTest"` → PASS.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/becommerce/crm/billing backend/src/test/java/com/becommerce/crm/billing
git commit -m "feat(cobranca): regras de atraso, bloqueio e carência"
```

---

### Task 2: Consultar pagamento e assinatura no Asaas

**Files:**
- Modify: `backend/src/main/java/com/becommerce/crm/billing/infrastructure/AsaasClient.java`
- Test: `backend/src/test/java/com/becommerce/crm/billing/infrastructure/AsaasClientTest.java` (acrescentar)

**Interfaces:**
- Produces: `public record AsaasPayment(String id, String subscription, String externalReference, String status, boolean deleted, LocalDate dueDate, int valueCents, String invoiceUrl, LocalDate paymentDate)` (aninhado em `AsaasClient`); `AsaasPayment getPayment(String paymentId)`; `String getSubscriptionReference(String subscriptionId)` (devolve `externalReference` ou `null`).

Mapeamento do JSON do Asaas: `id`, `subscription`, `externalReference`, `status`, `deleted` (padrão `false`), `dueDate` (`yyyy-MM-dd`), `value` (decimal em reais → centavos, arredondando `HALF_UP`), `invoiceUrl`, `paymentDate` (ou `clientPaymentDate` se `paymentDate` vier nulo; ambos opcionais). Corpo vazio/sem `id` → `AsaasException("Asaas não devolveu o id.")`.

- [ ] **Step 1: Testes (acrescentar em `AsaasClientTest`)**

```java
    @Test
    void getPaymentMapsFieldsAndCents() {
        server.expect(requestTo(BASE + "/payments/pay_1"))
              .andExpect(method(HttpMethod.GET))
              .andExpect(header("access_token", "KEY123"))
              .andRespond(withSuccess("""
                      {"id":"pay_1","subscription":"sub_1","externalReference":"co-1","status":"RECEIVED",
                       "dueDate":"2026-11-15","value":197.00,"invoiceUrl":"https://pix/1","paymentDate":"2026-11-14"}
                      """, MediaType.APPLICATION_JSON));
        AsaasClient.AsaasPayment p = client.getPayment("pay_1");
        assertEquals("sub_1", p.subscription());
        assertEquals("co-1", p.externalReference());
        assertEquals("RECEIVED", p.status());
        assertFalse(p.deleted());
        assertEquals(LocalDate.of(2026, 11, 15), p.dueDate());
        assertEquals(19700, p.valueCents());
        assertEquals("https://pix/1", p.invoiceUrl());
        assertEquals(LocalDate.of(2026, 11, 14), p.paymentDate());
    }

    @Test
    void getPaymentWithoutBodyIsAsaasError() {
        server.expect(requestTo(BASE + "/payments/pay_1"))
              .andRespond(withSuccess("", MediaType.APPLICATION_JSON));
        assertThrows(AsaasClient.AsaasException.class, () -> client.getPayment("pay_1"));
    }

    @Test
    void subscriptionReference() {
        server.expect(requestTo(BASE + "/subscriptions/sub_1"))
              .andExpect(method(HttpMethod.GET))
              .andRespond(withSuccess("{\"id\":\"sub_1\",\"externalReference\":\"co-1\"}", MediaType.APPLICATION_JSON));
        assertEquals("co-1", client.getSubscriptionReference("sub_1"));
    }
```

- [ ] **Step 2: Usuário roda e vê falhar** — `cd backend && mvn test -Dtest=AsaasClientTest` → FAIL.

- [ ] **Step 3: Implementação** — em `AsaasClient`, acrescentar:

```java
    public record AsaasPayment(String id, String subscription, String externalReference, String status,
                               boolean deleted, LocalDate dueDate, int valueCents, String invoiceUrl,
                               LocalDate paymentDate) {}

    public AsaasPayment getPayment(String paymentId) {
        JsonNode n = get("/payments/{id}", paymentId);
        String id = requireId(n);
        String paid = text(n, "paymentDate");
        if (paid == null) paid = text(n, "clientPaymentDate");
        return new AsaasPayment(
                id,
                text(n, "subscription"),
                text(n, "externalReference"),
                text(n, "status"),
                n.path("deleted").asBoolean(false),
                LocalDate.parse(n.path("dueDate").asText()),
                n.path("value").decimalValue().movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).intValueExact(),
                text(n, "invoiceUrl"),
                paid == null ? null : LocalDate.parse(paid));
    }

    public String getSubscriptionReference(String subscriptionId) {
        JsonNode n = get("/subscriptions/{id}", subscriptionId);
        requireId(n);
        return text(n, "externalReference");
    }

    private JsonNode get(String path, String id) {
        return call(() -> rest.get().uri(props.baseUrl() + path, id)
                .headers(h -> { h.set("access_token", props.apiKey()); h.set("User-Agent", "clin3dpro-crm"); })
                .retrieve().body(JsonNode.class));
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.path(field);
        return v.isMissingNode() || v.isNull() || v.asText().isBlank() ? null : v.asText();
    }
```

E trocar `requireId` para aceitar corpo nulo (resposta vazia) e usar o `JsonNode` importado:

```java
    private static String requireId(JsonNode node) {
        String id = node == null ? "" : node.path("id").asText("");
        if (id.isBlank()) throw new AsaasException("Asaas não devolveu o id.");
        return id;
    }
```

- [ ] **Step 4: Usuário roda e vê passar** — `cd backend && mvn test -Dtest=AsaasClientTest` → PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/becommerce/crm/billing/infrastructure/AsaasClient.java backend/src/test/java/com/becommerce/crm/billing/infrastructure/AsaasClientTest.java
git commit -m "feat(cobranca): consulta pagamento e assinatura no Asaas"
```

---

### Task 3: Aplicar pagamento do Asaas (tenant compartilhado)

**Files:**
- Create: `backend/src/main/java/com/becommerce/crm/billing/application/BillingTenant.java`
- Create: `backend/src/main/java/com/becommerce/crm/billing/application/BillingPaymentService.java`
- Modify: `backend/src/main/java/com/becommerce/crm/billing/application/BillingService.java` (o `inTenant` privado passa a delegar para `BillingTenant.run`)
- Test: `backend/src/test/java/com/becommerce/crm/billing/application/BillingPaymentServiceTest.java`

**Interfaces:**
- Consumes: Task 1 (regras, repositório de faturas), Task 2 (`AsaasClient#getPayment`, `#getSubscriptionReference`, `AsaasPayment`).
- Produces: `final class BillingTenant { static <T> T run(UUID companyId, Supplier<T> work) }` (troca o `TenantContext` se for diferente e restaura no `finally`; `clear()` se não havia); `BillingPaymentService#apply(String asaasPaymentId): Optional<UUID>` (devolve a empresa afetada ou vazio se o pagamento não é de nenhuma conta nossa); `BillingPaymentService#refreshOpen(UUID companyId, LocalDate before): int` (reconsulta no Asaas as faturas abertas com vencimento antes de `before`; `before = null` → todas as abertas; devolve quantas aplicou); `static InvoiceStatus toInvoiceStatus(AsaasPayment p)`.

Regras de `apply`:
1. `p = asaas.getPayment(id)` (nunca usa dados do webhook).
2. Empresa: `p.externalReference()`; se nulo e `p.subscription() != null` → `asaas.getSubscriptionReference(p.subscription())`. Referência ausente ou não-UUID → log `warn` sem payload e `Optional.empty()`.
3. Dentro de `BillingTenant.run(companyId, ...)`: busca a conta; se não existe ou `p.subscription()` é diferente de `acc.getAsaasSubscriptionId()` → `Optional.empty()` (pagamento que não é da assinatura desta empresa).
4. Cria/atualiza a fatura (`findByAsaasPaymentId` → `refresh` ou `fromAsaas`), `saveAndFlush`.
5. Recalcula a conta: se existe fatura `OVERDUE` (`findFirstByCompanyIdAndStatusOrderByDueDateAsc`) → `acc.onOverdue(maisAntiga.getDueDate())`; senão → `acc.onPaid()`. Se a fatura aplicada está `PENDING` → `acc.nextDueAt(p.dueDate())`. `saveAndFlush(acc)`.
6. Chama `access.evict(companyId)` (Task 5 cria `BillingAccessService`; nesta task injete-o já — ver Interfaces da Task 5: `void evict(UUID companyId)`).

Mapeamento `toInvoiceStatus`: `deleted` → `CANCELLED`; `RECEIVED`, `RECEIVED_IN_CASH` → `RECEIVED`; `CONFIRMED` → `CONFIRMED`; `OVERDUE` → `OVERDUE`; `REFUNDED`, `REFUND_REQUESTED`, `REFUND_IN_PROGRESS`, `CHARGEBACK_REQUESTED`, `CHARGEBACK_DISPUTE`, `AWAITING_CHARGEBACK_REVERSAL` → `REFUNDED`; qualquer outro → `PENDING`. `paidAt` = `p.paymentDate().atStartOfDay()` só quando o status é `RECEIVED` ou `CONFIRMED`.

Como a Task 5 ainda não existe nesta task, **crie aqui** o esqueleto mínimo de `BillingAccessService` com o método `evict` (a Task 5 completa a classe):

```java
package com.becommerce.crm.billing.application;

import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Responde "a empresa está bloqueada pela cobrança?" com cache curto. Completada na Task 5. */
@Service
public class BillingAccessService {
    final ConcurrentHashMap<UUID, Object> cache = new ConcurrentHashMap<>();

    public void evict(UUID companyId) { cache.remove(companyId); }
}
```

- [ ] **Step 1: Testes**

```java
package com.becommerce.crm.billing.application;

import com.becommerce.crm.billing.domain.*;
import com.becommerce.crm.billing.infrastructure.AsaasClient;
import com.becommerce.crm.billing.infrastructure.AsaasClient.AsaasPayment;
import com.becommerce.crm.billing.infrastructure.BillingAccountJpaRepository;
import com.becommerce.crm.billing.infrastructure.BillingInvoiceJpaRepository;
import com.becommerce.crm.masterdata.company.domain.CompanyPlan;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingPaymentServiceTest {
    private final UUID companyId = UUID.randomUUID();
    @Mock AsaasClient asaas;
    @Mock BillingAccountJpaRepository accounts;
    @Mock BillingInvoiceJpaRepository invoices;
    @Mock BillingAccessService access;
    BillingPaymentService service;
    BillingAccount acc;

    @BeforeEach
    void setUp() {
        service = new BillingPaymentService(asaas, accounts, invoices, access);
        acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        acc.activated("cus_1", "sub_1", LocalDate.of(2026, 11, 15));
        lenient().when(accounts.findById(companyId)).thenReturn(Optional.of(acc));
        lenient().when(invoices.findByAsaasPaymentId(any())).thenReturn(Optional.empty());
        lenient().when(invoices.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(accounts.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void clear() { TenantContext.clear(); }

    private AsaasPayment payment(String status, String ref) {
        return new AsaasPayment("pay_1", "sub_1", ref, status, false, LocalDate.of(2026, 11, 15), 19700,
                "https://pix/1", "RECEIVED".equals(status) ? LocalDate.of(2026, 11, 20) : null);
    }

    @Test
    void overduePaymentMovesAccountToPastDue() {
        when(asaas.getPayment("pay_1")).thenReturn(payment("OVERDUE", companyId.toString()));
        BillingInvoice overdue = BillingInvoice.fromAsaas(companyId, "pay_1", LocalDate.of(2026, 11, 15), 19700,
                InvoiceStatus.OVERDUE, null, null);
        when(invoices.findFirstByCompanyIdAndStatusOrderByDueDateAsc(companyId, InvoiceStatus.OVERDUE))
                .thenReturn(Optional.of(overdue));

        assertEquals(Optional.of(companyId), service.apply("pay_1"));
        assertEquals(BillingStatus.PAST_DUE, acc.getStatus());
        assertEquals(LocalDate.of(2026, 11, 15), acc.getOverdueSince());
        verify(access).evict(companyId);
    }

    @Test
    void receivedPaymentReactivatesBlockedAccount() {
        acc.onOverdue(LocalDate.of(2026, 11, 15));
        acc.block();
        when(asaas.getPayment("pay_1")).thenReturn(payment("RECEIVED", companyId.toString()));
        when(invoices.findFirstByCompanyIdAndStatusOrderByDueDateAsc(companyId, InvoiceStatus.OVERDUE))
                .thenReturn(Optional.empty());

        service.apply("pay_1");

        assertEquals(BillingStatus.ACTIVE, acc.getStatus());
        verify(invoices).saveAndFlush(argThat(i -> i.getStatus() == InvoiceStatus.RECEIVED && i.getPaidAt() != null));
    }

    @Test
    void paymentOfAnotherSubscriptionIsIgnored() {
        when(asaas.getPayment("pay_1")).thenReturn(new AsaasPayment("pay_1", "sub_X", companyId.toString(),
                "RECEIVED", false, LocalDate.of(2026, 11, 15), 19700, null, LocalDate.of(2026, 11, 20)));

        assertTrue(service.apply("pay_1").isEmpty());
        verify(invoices, never()).saveAndFlush(any());
    }

    @Test
    void missingReferenceFallsBackToSubscription() {
        when(asaas.getPayment("pay_1")).thenReturn(payment("PENDING", null));
        when(asaas.getSubscriptionReference("sub_1")).thenReturn(companyId.toString());
        when(invoices.findFirstByCompanyIdAndStatusOrderByDueDateAsc(companyId, InvoiceStatus.OVERDUE))
                .thenReturn(Optional.empty());

        assertEquals(Optional.of(companyId), service.apply("pay_1"));
    }

    @Test
    void writesRunInsideTargetTenantAndRestoreCaller() {
        UUID caller = UUID.randomUUID();
        TenantContext.setCompanyId(caller);
        AtomicReference<UUID> seen = new AtomicReference<>();
        when(asaas.getPayment("pay_1")).thenReturn(payment("PENDING", companyId.toString()));
        when(invoices.findFirstByCompanyIdAndStatusOrderByDueDateAsc(companyId, InvoiceStatus.OVERDUE))
                .thenReturn(Optional.empty());
        when(accounts.saveAndFlush(any())).thenAnswer(i -> { seen.set(TenantContext.getCompanyId()); return i.getArgument(0); });

        service.apply("pay_1");

        assertEquals(companyId, seen.get());
        assertEquals(caller, TenantContext.getCompanyId());
    }

    @Test
    void statusMapping() {
        assertEquals(InvoiceStatus.RECEIVED, BillingPaymentService.toInvoiceStatus(payment("RECEIVED_IN_CASH", null)));
        assertEquals(InvoiceStatus.REFUNDED, BillingPaymentService.toInvoiceStatus(payment("CHARGEBACK_REQUESTED", null)));
        assertEquals(InvoiceStatus.PENDING, BillingPaymentService.toInvoiceStatus(payment("AWAITING_RISK_ANALYSIS", null)));
        assertEquals(InvoiceStatus.CANCELLED, BillingPaymentService.toInvoiceStatus(
                new AsaasPayment("pay_1", "sub_1", null, "PENDING", true, LocalDate.of(2026, 11, 15), 1, null, null)));
    }

    @Test
    void refreshOpenReappliesEachOpenInvoice() {
        BillingInvoice open = BillingInvoice.fromAsaas(companyId, "pay_1", LocalDate.of(2026, 11, 15), 19700,
                InvoiceStatus.PENDING, null, null);
        when(invoices.findByCompanyIdAndStatusInAndDueDateBefore(eq(companyId), anyCollection(), eq(LocalDate.of(2026, 11, 20))))
                .thenReturn(List.of(open));
        when(asaas.getPayment("pay_1")).thenReturn(payment("RECEIVED", companyId.toString()));
        when(invoices.findFirstByCompanyIdAndStatusOrderByDueDateAsc(companyId, InvoiceStatus.OVERDUE))
                .thenReturn(Optional.empty());

        assertEquals(1, service.refreshOpen(companyId, LocalDate.of(2026, 11, 20)));
    }
}
```

- [ ] **Step 2: Usuário roda e vê falhar** — `cd backend && mvn test -Dtest=BillingPaymentServiceTest` → FAIL.

- [ ] **Step 3: `BillingTenant`**

```java
package com.becommerce.crm.billing.application;

import com.becommerce.crm.shared.tenant.context.TenantContext;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * As tabelas billing_* têm RLS FORCE e o TenantAwareDataSource aplica o tenant por statement.
 * Toda leitura/escrita da cobrança roda aqui dentro, com saveAndFlush, para que o SQL saia com o
 * tenant da empresa alvo — mesmo vindo do webhook, do job ou de um CRM_ADMIN logado em outra empresa.
 */
public final class BillingTenant {
    private BillingTenant() {}

    public static <T> T run(UUID companyId, Supplier<T> work) {
        UUID previous = TenantContext.getCompanyId();
        boolean swap = previous == null || !previous.equals(companyId);
        try {
            if (swap) TenantContext.setCompanyId(companyId);
            return work.get();
        } finally {
            if (swap) {
                if (previous == null) TenantContext.clear(); else TenantContext.setCompanyId(previous);
            }
        }
    }
}
```

Em `BillingService`, o corpo do `inTenant` privado vira `return BillingTenant.run(companyId, work);` (manter o método privado para não mexer nos chamadores).

- [ ] **Step 4: `BillingPaymentService`**

```java
package com.becommerce.crm.billing.application;

import com.becommerce.crm.billing.domain.BillingAccount;
import com.becommerce.crm.billing.domain.BillingInvoice;
import com.becommerce.crm.billing.domain.InvoiceStatus;
import com.becommerce.crm.billing.infrastructure.AsaasClient;
import com.becommerce.crm.billing.infrastructure.AsaasClient.AsaasPayment;
import com.becommerce.crm.billing.infrastructure.BillingAccountJpaRepository;
import com.becommerce.crm.billing.infrastructure.BillingInvoiceJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Único caminho que aplica um pagamento: sempre a partir do que o Asaas diz em GET /payments/{id}. */
@Service
public class BillingPaymentService {

    private static final Logger log = LoggerFactory.getLogger(BillingPaymentService.class);
    static final EnumSet<InvoiceStatus> OPEN = EnumSet.of(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE);

    private final AsaasClient asaas;
    private final BillingAccountJpaRepository accounts;
    private final BillingInvoiceJpaRepository invoices;
    private final BillingAccessService access;

    public BillingPaymentService(AsaasClient asaas, BillingAccountJpaRepository accounts,
                                 BillingInvoiceJpaRepository invoices, BillingAccessService access) {
        this.asaas = asaas;
        this.accounts = accounts;
        this.invoices = invoices;
        this.access = access;
    }

    public Optional<UUID> apply(String asaasPaymentId) {
        AsaasPayment p = asaas.getPayment(asaasPaymentId);
        Optional<UUID> company = companyOf(p);
        if (company.isEmpty()) {
            log.warn("[BILLING] pagamento {} sem empresa reconhecida; ignorado", p.id());
            return Optional.empty();
        }
        UUID companyId = company.get();
        boolean applied = BillingTenant.run(companyId, () -> applyInTenant(companyId, p));
        if (!applied) {
            log.warn("[BILLING] pagamento {} não pertence à assinatura da empresa {}; ignorado", p.id(), companyId);
            return Optional.empty();
        }
        access.evict(companyId);
        return company;
    }

    public int refreshOpen(UUID companyId, LocalDate before) {
        List<BillingInvoice> open = BillingTenant.run(companyId, () -> before == null
                ? invoices.findByCompanyIdAndStatusIn(companyId, OPEN)
                : invoices.findByCompanyIdAndStatusInAndDueDateBefore(companyId, OPEN, before));
        int applied = 0;
        for (BillingInvoice i : open) {
            if (apply(i.getAsaasPaymentId()).isPresent()) applied++;
        }
        return applied;
    }

    private boolean applyInTenant(UUID companyId, AsaasPayment p) {
        Optional<BillingAccount> found = accounts.findById(companyId);
        if (found.isEmpty() || !Objects.equals(p.subscription(), found.get().getAsaasSubscriptionId())) {
            return false;
        }
        BillingAccount acc = found.get();
        InvoiceStatus status = toInvoiceStatus(p);
        LocalDateTime paidAt = (status == InvoiceStatus.RECEIVED || status == InvoiceStatus.CONFIRMED)
                && p.paymentDate() != null ? p.paymentDate().atStartOfDay() : null;
        BillingInvoice invoice = invoices.findByAsaasPaymentId(p.id())
                .map(i -> { i.refresh(p.dueDate(), p.valueCents(), status, p.invoiceUrl(), paidAt); return i; })
                .orElseGet(() -> BillingInvoice.fromAsaas(companyId, p.id(), p.dueDate(), p.valueCents(),
                        status, p.invoiceUrl(), paidAt));
        invoices.saveAndFlush(invoice);

        invoices.findFirstByCompanyIdAndStatusOrderByDueDateAsc(companyId, InvoiceStatus.OVERDUE)
                .ifPresentOrElse(oldest -> acc.onOverdue(oldest.getDueDate()), acc::onPaid);
        if (status == InvoiceStatus.PENDING) acc.nextDueAt(p.dueDate());
        accounts.saveAndFlush(acc);
        return true;
    }

    private Optional<UUID> companyOf(AsaasPayment p) {
        String ref = p.externalReference();
        if (ref == null && p.subscription() != null) ref = asaas.getSubscriptionReference(p.subscription());
        if (ref == null) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(ref));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    static InvoiceStatus toInvoiceStatus(AsaasPayment p) {
        if (p.deleted()) return InvoiceStatus.CANCELLED;
        return switch (p.status() == null ? "" : p.status()) {
            case "RECEIVED", "RECEIVED_IN_CASH" -> InvoiceStatus.RECEIVED;
            case "CONFIRMED" -> InvoiceStatus.CONFIRMED;
            case "OVERDUE" -> InvoiceStatus.OVERDUE;
            case "REFUNDED", "REFUND_REQUESTED", "REFUND_IN_PROGRESS", "CHARGEBACK_REQUESTED",
                 "CHARGEBACK_DISPUTE", "AWAITING_CHARGEBACK_REVERSAL" -> InvoiceStatus.REFUNDED;
            default -> InvoiceStatus.PENDING;
        };
    }
}
```

- [ ] **Step 5: Usuário roda e vê passar** — `cd backend && mvn test -Dtest="BillingPaymentServiceTest,BillingServiceTest"` → PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/becommerce/crm/billing backend/src/test/java/com/becommerce/crm/billing
git commit -m "feat(cobranca): aplica pagamento consultando o Asaas"
```

---

### Task 4: Webhook do Asaas

**Files:**
- Create: `backend/src/main/java/com/becommerce/crm/billing/infrastructure/BillingWebhookEventStore.java`
- Create: `backend/src/main/java/com/becommerce/crm/billing/application/AsaasWebhookService.java`
- Create: `backend/src/main/java/com/becommerce/crm/billing/web/AsaasWebhookController.java`
- Modify: `backend/src/main/java/com/becommerce/crm/shared/security/config/SecurityConfig.java` (permitAll do webhook, ao lado do Telegram)
- Test: `backend/src/test/java/com/becommerce/crm/billing/application/AsaasWebhookServiceTest.java`, `backend/src/test/java/com/becommerce/crm/billing/web/AsaasWebhookControllerTest.java`

**Interfaces:**
- Consumes: Task 3 (`BillingPaymentService#apply`), `BillingProperties#webhookToken()`.
- Produces: `BillingWebhookEventStore#register(String eventId, String event): boolean` (true = já processado, não fazer nada); `#markProcessed(String eventId)`; `AsaasWebhookService#tokenMatches(String header): boolean`; `AsaasWebhookService#handle(JsonNode body): void`; endpoint `POST /api/v1/public/billing/asaas/webhook` → 401 sem token válido, 200 `{"received":true}` caso contrário.

Regras:
- `tokenMatches`: `props.webhookToken()` vazio/nulo → `false`; senão `MessageDigest.isEqual` dos bytes UTF-8.
- `handle`: `eventId = body.id` (se ausente: `event + ":" + payment.id`); `event = body.event`. `register` grava com `INSERT ... ON CONFLICT (asaas_event_id) DO NOTHING`; se a linha já existia e `processed_at` não é nulo → retorna (repetido). Eventos fora da lista tratada → `markProcessed` e retorna. Eventos tratados → `paymentService.apply(body.payment.id)` e `markProcessed`. Exceção no `apply` (ex.: Asaas fora) → log `warn` com `eventId`/`event` e mensagem, **sem** `markProcessed` (uma reentrega do Asaas tenta de novo; o job diário também reconcilia) e **não** propaga.
- Log nunca imprime o corpo inteiro.
- O controller lê o corpo como `JsonNode`, checa o token **antes** de qualquer coisa e sempre devolve 200 depois de `handle` (mesmo com corpo sem `payment`).

- [ ] **Step 1: Testes do serviço**

```java
package com.becommerce.crm.billing.application;

import com.becommerce.crm.billing.infrastructure.AsaasClient;
import com.becommerce.crm.billing.infrastructure.BillingWebhookEventStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AsaasWebhookServiceTest {
    private final BillingPaymentService payments = mock(BillingPaymentService.class);
    private final BillingWebhookEventStore events = mock(BillingWebhookEventStore.class);
    private final AsaasWebhookService service =
            new AsaasWebhookService(payments, events, new BillingProperties(true, "u", "KEY", "WH-TOKEN"));

    private JsonNode body(String event) throws Exception {
        return new ObjectMapper().readTree(
                "{\"id\":\"evt_1\",\"event\":\"" + event + "\",\"payment\":{\"id\":\"pay_1\"}}");
    }

    @Test
    void tokenMustMatch() {
        assertTrue(service.tokenMatches("WH-TOKEN"));
        assertFalse(service.tokenMatches("other"));
        assertFalse(service.tokenMatches(null));
    }

    @Test
    void blankConfiguredTokenRejectsEverything() {
        var s = new AsaasWebhookService(payments, events, new BillingProperties(true, "u", "KEY", ""));
        assertFalse(s.tokenMatches(""));
    }

    @Test
    void paymentEventIsAppliedAndMarked() throws Exception {
        when(events.register("evt_1", "PAYMENT_RECEIVED")).thenReturn(false);
        when(payments.apply("pay_1")).thenReturn(Optional.of(UUID.randomUUID()));
        service.handle(body("PAYMENT_RECEIVED"));
        verify(payments).apply("pay_1");
        verify(events).markProcessed("evt_1");
    }

    @Test
    void repeatedEventIsIgnored() throws Exception {
        when(events.register("evt_1", "PAYMENT_RECEIVED")).thenReturn(true);
        service.handle(body("PAYMENT_RECEIVED"));
        verifyNoInteractions(payments);
    }

    @Test
    void otherEventsAreRecordedAndIgnored() throws Exception {
        when(events.register("evt_1", "PAYMENT_UPDATED")).thenReturn(false);
        service.handle(body("PAYMENT_UPDATED"));
        verifyNoInteractions(payments);
        verify(events).markProcessed("evt_1");
    }

    @Test
    void asaasFailureLeavesEventUnprocessedWithoutThrowing() throws Exception {
        when(events.register("evt_1", "PAYMENT_OVERDUE")).thenReturn(false);
        when(payments.apply("pay_1")).thenThrow(new AsaasClient.AsaasException("Asaas indisponível."));
        assertDoesNotThrow(() -> service.handle(body("PAYMENT_OVERDUE")));
        verify(events, never()).markProcessed(any());
    }
}
```

- [ ] **Step 2: Teste do controller**

```java
package com.becommerce.crm.billing.web;

import com.becommerce.crm.billing.application.AsaasWebhookService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AsaasWebhookControllerTest {
    private static final String URL = "/api/v1/public/billing/asaas/webhook";
    private final AsaasWebhookService service = mock(AsaasWebhookService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AsaasWebhookController(service)).build();

    @Test
    void invalidTokenIs401() throws Exception {
        when(service.tokenMatches("bad")).thenReturn(false);
        mvc.perform(post(URL).header("asaas-access-token", "bad")
                        .contentType("application/json").content("{\"id\":\"evt_1\"}"))
           .andExpect(status().isUnauthorized());
        verify(service, never()).handle(any());
    }

    @Test
    void validTokenIs200() throws Exception {
        when(service.tokenMatches("ok")).thenReturn(true);
        mvc.perform(post(URL).header("asaas-access-token", "ok")
                        .contentType("application/json").content("{\"id\":\"evt_1\",\"event\":\"PAYMENT_RECEIVED\"}"))
           .andExpect(status().isOk());
        verify(service).handle(any());
    }
}
```

- [ ] **Step 3: Usuário roda e vê falhar** — `cd backend && mvn test -Dtest="AsaasWebhookServiceTest,AsaasWebhookControllerTest"` → FAIL.

- [ ] **Step 4: `BillingWebhookEventStore`** (tabela de plataforma, sem RLS — JdbcTemplate direto)

```java
package com.becommerce.crm.billing.infrastructure;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.List;

/** Idempotência do webhook (billing_webhook_events, sem RLS: tabela de plataforma). */
@Component
public class BillingWebhookEventStore {

    private final JdbcTemplate jdbc;

    public BillingWebhookEventStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Registra o evento. Devolve true se ele já tinha sido processado (nada a fazer). */
    public boolean register(String eventId, String event) {
        int inserted = jdbc.update(
                "INSERT INTO billing_webhook_events (asaas_event_id, event) VALUES (?, ?) "
                        + "ON CONFLICT (asaas_event_id) DO NOTHING", eventId, truncate(event, 60));
        if (inserted == 1) return false;
        List<Object> processed = jdbc.queryForList(
                "SELECT processed_at FROM billing_webhook_events WHERE asaas_event_id = ?", Object.class, eventId);
        return !processed.isEmpty() && processed.get(0) != null;
    }

    public void markProcessed(String eventId) {
        jdbc.update("UPDATE billing_webhook_events SET processed_at = now() WHERE asaas_event_id = ?", eventId);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "UNKNOWN";
        return s.length() > max ? s.substring(0, max) : s;
    }
}
```

- [ ] **Step 5: `AsaasWebhookService`**

```java
package com.becommerce.crm.billing.application;

import com.becommerce.crm.billing.infrastructure.BillingWebhookEventStore;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

@Service
public class AsaasWebhookService {

    private static final Logger log = LoggerFactory.getLogger(AsaasWebhookService.class);
    static final Set<String> HANDLED = Set.of("PAYMENT_CREATED", "PAYMENT_CONFIRMED", "PAYMENT_RECEIVED",
            "PAYMENT_OVERDUE", "PAYMENT_REFUNDED", "PAYMENT_DELETED");

    private final BillingPaymentService payments;
    private final BillingWebhookEventStore events;
    private final BillingProperties props;

    public AsaasWebhookService(BillingPaymentService payments, BillingWebhookEventStore events,
                               BillingProperties props) {
        this.payments = payments;
        this.events = events;
        this.props = props;
    }

    public boolean tokenMatches(String header) {
        String expected = props.webhookToken();
        if (expected == null || expected.isBlank() || header == null) return false;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                header.getBytes(StandardCharsets.UTF_8));
    }

    public void handle(JsonNode body) {
        String event = body.path("event").asText("UNKNOWN");
        String paymentId = body.path("payment").path("id").asText("");
        String eventId = body.path("id").asText("");
        if (eventId.isBlank()) eventId = event + ":" + paymentId;

        if (events.register(eventId, event)) return;
        if (!HANDLED.contains(event) || paymentId.isBlank()) {
            events.markProcessed(eventId);
            return;
        }
        try {
            payments.apply(paymentId);
            events.markProcessed(eventId);
        } catch (RuntimeException e) {
            log.warn("[BILLING] webhook {} ({}) não aplicado: {}", eventId, event, e.getMessage());
        }
    }
}
```

- [ ] **Step 6: `AsaasWebhookController`**

```java
package com.becommerce.crm.billing.web;

import com.becommerce.crm.billing.application.AsaasWebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/** Webhook público do Asaas. Autenticação pelo header asaas-access-token. */
@RestController
@RequestMapping("/api/v1/public/billing/asaas/webhook")
public class AsaasWebhookController {

    private final AsaasWebhookService service;

    public AsaasWebhookController(AsaasWebhookService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<Map<String, Object>> receive(
            @RequestHeader(value = "asaas-access-token", required = false) String token,
            @RequestBody JsonNode body) {
        if (!service.tokenMatches(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("received", false));
        }
        service.handle(body);
        return ResponseEntity.ok(Map.of("received", true));
    }
}
```

- [ ] **Step 7: `SecurityConfig`** — logo depois da linha do Telegram (`.requestMatchers(HttpMethod.POST, "/api/v1/telegram/webhook/**").permitAll()`):

```java
                .requestMatchers(HttpMethod.POST, "/api/v1/public/billing/asaas/webhook").permitAll()
```

Conferir se algum filtro (ex.: rate limit, tenant filter) recusa requisições sem JWT em `/api/v1/public/**`; seguir o que já é feito para `/api/v1/public/forms/**`.

- [ ] **Step 8: Usuário roda e vê passar** — `cd backend && mvn test -Dtest="AsaasWebhookServiceTest,AsaasWebhookControllerTest"` → PASS.

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/com/becommerce/crm/billing backend/src/test/java/com/becommerce/crm/billing backend/src/main/java/com/becommerce/crm/shared/security/config/SecurityConfig.java
git commit -m "feat(cobranca): webhook do Asaas com token e idempotência"
```

---

### Task 5: Bloqueio 402 da empresa

**Files:**
- Modify: `backend/src/main/java/com/becommerce/crm/billing/application/BillingAccessService.java` (completa o esqueleto da Task 3)
- Create: `backend/src/main/java/com/becommerce/crm/billing/web/BillingBlockedException.java`
- Create: `backend/src/main/java/com/becommerce/crm/billing/web/BillingGuardInterceptor.java`
- Create: `backend/src/main/java/com/becommerce/crm/billing/web/BillingGuardWebConfig.java`
- Modify: `backend/src/main/java/com/becommerce/crm/shared/web/handler/GlobalExceptionHandler.java` (+ handler 402, ao lado do `ModuleDisabledException`)
- Test: `backend/src/test/java/com/becommerce/crm/billing/application/BillingAccessServiceTest.java`, `backend/src/test/java/com/becommerce/crm/billing/web/BillingGuardInterceptorTest.java`

**Interfaces:**
- Produces: `BillingAccessService#isBlocked(UUID companyId): boolean` (consulta a conta dentro de `BillingTenant.run`; cache por 60 s com `Clock`); `#evict(UUID)`; `BillingBlockedException extends RuntimeException`; resposta 402 `{"status":402,"code":"BILLING_BLOCKED","error":"Payment Required","message":"Mensalidade em atraso: regularize o pagamento para continuar.","timestamp":...}`.

Regras do interceptor (`preHandle`):
- Sem `CurrentUser` autenticado → passa.
- `user.isCrmAdmin()` → passa.
- Caminho liberado → passa: é exatamente `/api/v1/billing` ou `/api/v1/me`; começa com `/api/v1/billing/`, `/api/v1/me/`, `/api/v1/auth/`, `/api/v1/public/`, `/api/v1/admin/billing/`; ou é `GET /api/v1/companies/me`.
- `user.companyId()` nulo → passa.
- `access.isBlocked(companyId)` → lança `BillingBlockedException`.
- Registrado em `/api/v1/**`.

- [ ] **Step 1: Testes do serviço de acesso**

```java
package com.becommerce.crm.billing.application;

import com.becommerce.crm.billing.domain.BillingAccount;
import com.becommerce.crm.billing.infrastructure.BillingAccountJpaRepository;
import com.becommerce.crm.masterdata.company.domain.CompanyPlan;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BillingAccessServiceTest {
    private final UUID companyId = UUID.randomUUID();
    private final BillingAccountJpaRepository accounts = mock(BillingAccountJpaRepository.class);
    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-26T12:00:00Z"));
    private final BillingAccessService service = new BillingAccessService(accounts, clock);

    private BillingAccount blocked() {
        BillingAccount a = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        a.activated("cus_1", "sub_1", LocalDate.of(2026, 11, 15));
        a.onOverdue(LocalDate.of(2026, 11, 15));
        a.block();
        return a;
    }

    @Test
    void blockedAccountIsBlockedAndCached() {
        when(accounts.findById(companyId)).thenReturn(Optional.of(blocked()));
        assertTrue(service.isBlocked(companyId));
        assertTrue(service.isBlocked(companyId));
        verify(accounts, times(1)).findById(companyId);
    }

    @Test
    void cacheExpiresAfter60sAndEvictForcesReload() {
        when(accounts.findById(companyId)).thenReturn(Optional.empty());
        assertFalse(service.isBlocked(companyId));
        clock.plusSeconds(61);
        service.isBlocked(companyId);
        service.evict(companyId);
        service.isBlocked(companyId);
        verify(accounts, times(3)).findById(companyId);
    }

    static final class MutableClock extends Clock {
        private Instant now;
        MutableClock(Instant now) { this.now = now; }
        void plusSeconds(long s) { now = now.plusSeconds(s); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
```

- [ ] **Step 2: Testes do interceptor**

```java
package com.becommerce.crm.billing.web;

import com.becommerce.crm.billing.application.BillingAccessService;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BillingGuardInterceptorTest {
    private final UUID company = UUID.randomUUID();
    private final BillingAccessService access = mock(BillingAccessService.class);
    private final BillingGuardInterceptor guard = new BillingGuardInterceptor(access);

    private void loginAs(String role) {
        CurrentUser u = new CurrentUser(UUID.randomUUID(), "x@y.com", company, company,
                List.of(role), List.of(), "sub", null, "keycloak", "X", role);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u, null, List.of()));
    }

    private boolean call(String method, String uri) throws Exception {
        return guard.preHandle(new MockHttpServletRequest(method, uri), new MockHttpServletResponse(), new Object());
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void blockedCompanyGets402OnBusinessRoutes() {
        loginAs("ADMIN");
        when(access.isBlocked(company)).thenReturn(true);
        assertThrows(BillingBlockedException.class,
                () -> call("GET", "/api/v1/companies/" + company + "/patients"));
    }

    @Test
    void billingAndIdentityRoutesPass() throws Exception {
        loginAs("ADMIN");
        when(access.isBlocked(company)).thenReturn(true);
        assertTrue(call("GET", "/api/v1/billing"));
        assertTrue(call("POST", "/api/v1/billing/check-payment"));
        assertTrue(call("GET", "/api/v1/me"));
        assertTrue(call("GET", "/api/v1/auth/me"));
        assertTrue(call("GET", "/api/v1/companies/me"));
        assertTrue(call("GET", "/api/v1/me/memberships"));
    }

    @Test
    void similarPrefixesAreNotOpen() {
        loginAs("ADMIN");
        when(access.isBlocked(company)).thenReturn(true);
        assertThrows(BillingBlockedException.class, () -> call("GET", "/api/v1/messages"));
        assertThrows(BillingBlockedException.class, () -> call("GET", "/api/v1/billing-report"));
    }

    @Test
    void crmAdminAlwaysPasses() throws Exception {
        loginAs("CRM_ADMIN");
        assertTrue(call("GET", "/api/v1/companies/" + company + "/patients"));
        verifyNoInteractions(access);
    }

    @Test
    void activeCompanyPasses() throws Exception {
        loginAs("ADMIN");
        when(access.isBlocked(company)).thenReturn(false);
        assertTrue(call("GET", "/api/v1/companies/" + company + "/patients"));
    }

    @Test
    void anonymousPasses() throws Exception {
        assertTrue(call("POST", "/api/v1/public/billing/asaas/webhook"));
        verifyNoInteractions(access);
    }
}
```

- [ ] **Step 3: Usuário roda e vê falhar** — `cd backend && mvn test -Dtest="BillingAccessServiceTest,BillingGuardInterceptorTest"` → FAIL.

- [ ] **Step 4: `BillingAccessService` completo** (substitui o esqueleto)

```java
package com.becommerce.crm.billing.application;

import com.becommerce.crm.billing.domain.BillingStatus;
import com.becommerce.crm.billing.infrastructure.BillingAccountJpaRepository;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** "A empresa está bloqueada pela cobrança?" — consultado a cada requisição, por isso o cache de 60 s. */
@Service
public class BillingAccessService {

    static final long TTL_SECONDS = 60;

    private record Entry(boolean blocked, Instant at) {}

    private final BillingAccountJpaRepository accounts;
    private final Clock clock;
    private final ConcurrentHashMap<UUID, Entry> cache = new ConcurrentHashMap<>();

    public BillingAccessService(BillingAccountJpaRepository accounts, Clock clock) {
        this.accounts = accounts;
        this.clock = clock;
    }

    public boolean isBlocked(UUID companyId) {
        Instant now = clock.instant();
        Entry e = cache.get(companyId);
        if (e != null && e.at().plusSeconds(TTL_SECONDS).isAfter(now)) return e.blocked();
        boolean blocked = BillingTenant.run(companyId, () -> accounts.findById(companyId)
                .map(a -> a.getStatus() == BillingStatus.BLOCKED)
                .orElse(false));
        cache.put(companyId, new Entry(blocked, now));
        return blocked;
    }

    public void evict(UUID companyId) { cache.remove(companyId); }
}
```

- [ ] **Step 5: Exceção, interceptor e registro**

```java
package com.becommerce.crm.billing.web;

/** Empresa bloqueada por mensalidade em atraso (HTTP 402, code BILLING_BLOCKED). */
public class BillingBlockedException extends RuntimeException {
    public BillingBlockedException() {
        super("Mensalidade em atraso: regularize o pagamento para continuar.");
    }
}
```

```java
package com.becommerce.crm.billing.web;

import com.becommerce.crm.billing.application.BillingAccessService;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.List;

/**
 * Empresa com cobrança BLOCKED recebe 402 (BILLING_BLOCKED) em tudo, menos cobrança, identidade e rotas
 * públicas — assim a tela de pagamento pendente continua funcionando. CRM_ADMIN passa sempre.
 */
@Component
public class BillingGuardInterceptor implements HandlerInterceptor {

    private static final List<String> OPEN_EXACT = List.of("/api/v1/billing", "/api/v1/me");
    private static final List<String> OPEN_PREFIXES = List.of(
            "/api/v1/billing/", "/api/v1/me/", "/api/v1/auth/", "/api/v1/public/", "/api/v1/admin/billing/");

    private final BillingAccessService access;

    public BillingGuardInterceptor(BillingAccessService access) { this.access = access; }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CurrentUser user)) return true;
        if (user.isCrmAdmin()) return true;
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (isOpen(request.getMethod(), path)) return true;
        if (user.companyId() == null) return true;
        if (access.isBlocked(user.companyId())) throw new BillingBlockedException();
        return true;
    }

    static boolean isOpen(String method, String path) {
        if ("GET".equals(method) && "/api/v1/companies/me".equals(path)) return true;
        return OPEN_EXACT.contains(path) || OPEN_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
```

(Prefixos terminam em `/` de propósito: `/api/v1/me` sem barra casaria `/api/v1/messages`. `/api/v1/me/memberships` entra pelo prefixo `/api/v1/me/`.)

```java
package com.becommerce.crm.billing.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class BillingGuardWebConfig implements WebMvcConfigurer {

    private final BillingGuardInterceptor guard;

    public BillingGuardWebConfig(BillingGuardInterceptor guard) { this.guard = guard; }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(guard).addPathPatterns("/api/v1/**");
    }
}
```

- [ ] **Step 6: Handler 402** — em `GlobalExceptionHandler`, ao lado de `handleModuleDisabledException` (import `com.becommerce.crm.billing.web.BillingBlockedException`):

```java
    @ExceptionHandler(BillingBlockedException.class)
    public ResponseEntity<Map<String, Object>> handleBillingBlocked(BillingBlockedException ex) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
            .body(Map.of(
                "status", 402,
                "code", "BILLING_BLOCKED",
                "error", "Payment Required",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }
```

Conferir se o `@ExceptionHandler` de `ModuleDisabledException` também alcança exceções lançadas em `preHandle` (sim: interceptors rodam dentro do `DispatcherServlet`, então o `@ControllerAdvice` trata). Se houver outro `WebMvcConfigurer` com ordem explícita, manter o guard depois do `ModuleGuardInterceptor` não é necessário — os dois são independentes.

- [ ] **Step 7: Usuário roda e vê passar** — `cd backend && mvn test -Dtest="BillingAccessServiceTest,BillingGuardInterceptorTest,BillingPaymentServiceTest"` → PASS.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/becommerce/crm/billing backend/src/test/java/com/becommerce/crm/billing backend/src/main/java/com/becommerce/crm/shared/web/handler/GlobalExceptionHandler.java
git commit -m "feat(cobranca): bloqueio 402 da empresa com mensalidade em atraso"
```

---

### Task 6: Job diário

**Files:**
- Create: `backend/src/main/java/com/becommerce/crm/billing/application/BillingDailyJob.java`
- Modify: `backend/src/main/java/com/becommerce/crm/billing/application/BillingService.java` (+ `retrySync`)
- Modify: `backend/src/main/resources/application.yml` (`app.billing.daily-cron`)
- Test: `backend/src/test/java/com/becommerce/crm/billing/application/BillingDailyJobTest.java`, `BillingServiceTest` (acrescentar)

**Interfaces:**
- Consumes: Task 1 (regras), Task 3 (`BillingPaymentService#refreshOpen`), Task 5 (`BillingAccessService#evict`), `BillingService#activate`.
- Produces: `BillingService#retrySync(UUID companyId): void` — se a conta está `SYNC_PENDING` → `activate(companyId)` (engole `BillingRuleException` com log `info`); se `isPriceSyncPending()` → reenvia o valor (`pushPrice` + `saveAndFlush`, dentro do tenant). `BillingDailyJob#run(): void`, agendado por `@Scheduled(cron = "${app.billing.daily-cron:0 30 3 * * *}", zone = "America/Sao_Paulo")`.

Ordem por empresa (cada empresa em `try/catch` próprio, log `error` sem dados sensíveis):
1. Lista empresas com `jdbc.queryForList("SELECT id FROM companies", UUID.class)` (companies não tem RLS — mesmo padrão do `TelegramCleanupScheduler`).
2. Lê a conta dentro de `BillingTenant.run`; sem conta, `EXEMPT` ou `NOT_STARTED` → próxima.
3. Se `props.configured()`: `billingService.retrySync(id)` e `payments.refreshOpen(id, today)` (reconcilia faturas vencidas que o webhook possa ter perdido).
4. Relê a conta; `expireGrace(today)`; se `shouldBlock(today)` → `block()`; `saveAndFlush`; `access.evict(id)`.

`today = LocalDate.now(clock.withZone(ZoneId.of("America/Sao_Paulo")))`.

- [ ] **Step 1: Teste do job**

```java
package com.becommerce.crm.billing.application;

import com.becommerce.crm.billing.domain.BillingAccount;
import com.becommerce.crm.billing.domain.BillingStatus;
import com.becommerce.crm.billing.infrastructure.BillingAccountJpaRepository;
import com.becommerce.crm.masterdata.company.domain.CompanyPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BillingDailyJobTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-26T09:30:00Z"), ZoneOffset.UTC);
    private final UUID companyId = UUID.randomUUID();
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final BillingAccountJpaRepository accounts = mock(BillingAccountJpaRepository.class);
    private final BillingService billing = mock(BillingService.class);
    private final BillingPaymentService payments = mock(BillingPaymentService.class);
    private final BillingAccessService access = mock(BillingAccessService.class);
    private BillingAccount acc;

    private BillingDailyJob job(boolean enabled) {
        return new BillingDailyJob(jdbc, accounts, billing, payments, access,
                new BillingProperties(enabled, "u", enabled ? "KEY" : "", "W"), CLOCK);
    }

    @BeforeEach
    void setUp() {
        when(jdbc.queryForList(anyString(), eq(UUID.class))).thenReturn(List.of(companyId));
        acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        acc.activated("cus_1", "sub_1", LocalDate.of(2026, 11, 15));
        when(accounts.findById(companyId)).thenReturn(Optional.of(acc));
        when(accounts.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void blocksAfterTenDaysAndReconciles() {
        acc.onOverdue(LocalDate.of(2026, 11, 16));
        job(true).run();
        assertEquals(BillingStatus.BLOCKED, acc.getStatus());
        verify(billing).retrySync(companyId);
        verify(payments).refreshOpen(companyId, LocalDate.of(2026, 11, 26));
        verify(access).evict(companyId);
    }

    @Test
    void doesNotBlockBeforeTenDays() {
        acc.onOverdue(LocalDate.of(2026, 11, 17));
        job(true).run();
        assertEquals(BillingStatus.PAST_DUE, acc.getStatus());
    }

    @Test
    void respectsGrace() {
        acc.onOverdue(LocalDate.of(2026, 11, 1));
        acc.grant(LocalDate.of(2026, 11, 26));
        job(true).run();
        assertEquals(BillingStatus.PAST_DUE, acc.getStatus());
    }

    @Test
    void expiredGraceIsClearedAndBlocks() {
        acc.onOverdue(LocalDate.of(2026, 11, 1));
        acc.grant(LocalDate.of(2026, 11, 25));
        job(true).run();
        assertNull(acc.getGraceUntil());
        assertEquals(BillingStatus.BLOCKED, acc.getStatus());
    }

    @Test
    void disabledStillBlocksButDoesNotCallAsaas() {
        acc.onOverdue(LocalDate.of(2026, 11, 1));
        job(false).run();
        assertEquals(BillingStatus.BLOCKED, acc.getStatus());
        verifyNoInteractions(billing, payments);
    }

    @Test
    void exemptIsSkipped() {
        acc.markExempt();
        job(true).run();
        verifyNoInteractions(billing, payments);
        verify(accounts, never()).saveAndFlush(any());
    }

    @Test
    void failureInOneCompanyDoesNotStopTheJob() {
        UUID other = UUID.randomUUID();
        when(jdbc.queryForList(anyString(), eq(UUID.class))).thenReturn(List.of(other, companyId));
        when(accounts.findById(other)).thenThrow(new RuntimeException("db"));
        acc.onOverdue(LocalDate.of(2026, 11, 1));
        job(true).run();
        assertEquals(BillingStatus.BLOCKED, acc.getStatus());
    }
}
```

- [ ] **Step 2: Testes de `retrySync` (acrescentar em `BillingServiceTest`)**

```java
    @Test
    void retrySyncReactivatesPendingAccount() {
        BillingAccount acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        acc.customerCreated("cus_1");
        acc.syncFailed("timeout");
        when(accounts.findById(companyId)).thenReturn(Optional.of(acc));
        when(asaas.createSubscription(eq("cus_1"), anyInt(), any(), anyString(), anyString())).thenReturn("sub_1");

        service.retrySync(companyId);

        assertEquals(BillingStatus.ACTIVE, acc.getStatus());
    }

    @Test
    void retrySyncResendsPendingPrice() {
        BillingAccount acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        acc.activated("cus_1", "sub_1", LocalDate.of(2026, 11, 15));
        acc.priceSyncFailed("timeout");
        when(accounts.findById(companyId)).thenReturn(Optional.of(acc));

        service.retrySync(companyId);

        verify(asaas).updateSubscriptionValue(eq("sub_1"), eq(19700), anyString());
        assertNull(acc.getSyncError());
    }
```

- [ ] **Step 3: Usuário roda e vê falhar** — `cd backend && mvn test -Dtest="BillingDailyJobTest,BillingServiceTest"` → FAIL.

- [ ] **Step 4: `retrySync` em `BillingService`**

```java
    @Transactional
    public void retrySync(UUID companyId) {
        BillingAccount acc = inTenant(companyId, () -> accounts.findById(companyId).orElse(null));
        if (acc == null) return;
        if (acc.getStatus() == BillingStatus.SYNC_PENDING) {
            try {
                activate(companyId);
            } catch (BillingRuleException e) {
                log.info("Reenvio da cobrança de {} não feito: {}", companyId, e.getMessage());
            }
            return;
        }
        if (acc.isPriceSyncPending()) {
            inTenant(companyId, () -> {
                pushPrice(acc);
                return accounts.saveAndFlush(acc);
            });
        }
    }
```

- [ ] **Step 5: `BillingDailyJob`**

```java
package com.becommerce.crm.billing.application;

import com.becommerce.crm.billing.domain.BillingAccount;
import com.becommerce.crm.billing.domain.BillingStatus;
import com.becommerce.crm.billing.infrastructure.BillingAccountJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * 1×/dia (03:30, depois do backup): reenvia ao Asaas o que ficou pendente, reconcilia faturas vencidas
 * (caso o webhook tenha se perdido), expira carências e bloqueia quem passou de 10 dias de atraso.
 */
@Component
public class BillingDailyJob {

    private static final Logger log = LoggerFactory.getLogger(BillingDailyJob.class);
    static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private final JdbcTemplate jdbc;
    private final BillingAccountJpaRepository accounts;
    private final BillingService billing;
    private final BillingPaymentService payments;
    private final BillingAccessService access;
    private final BillingProperties props;
    private final Clock clock;

    public BillingDailyJob(JdbcTemplate jdbc, BillingAccountJpaRepository accounts, BillingService billing,
                           BillingPaymentService payments, BillingAccessService access,
                           BillingProperties props, Clock clock) {
        this.jdbc = jdbc;
        this.accounts = accounts;
        this.billing = billing;
        this.payments = payments;
        this.access = access;
        this.props = props;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.billing.daily-cron:0 30 3 * * *}", zone = "America/Sao_Paulo")
    public void run() {
        LocalDate today = LocalDate.now(clock.withZone(ZONE));
        List<UUID> companyIds = jdbc.queryForList("SELECT id FROM companies", UUID.class);
        int blocked = 0;
        for (UUID companyId : companyIds) {
            try {
                if (process(companyId, today)) blocked++;
            } catch (Exception e) {
                log.error("[BILLING] job diário falhou para a empresa {}: {}", companyId, e.getMessage());
            }
        }
        log.info("[BILLING] job diário concluído: {} empresas, {} bloqueadas hoje", companyIds.size(), blocked);
    }

    private boolean process(UUID companyId, LocalDate today) {
        BillingAccount acc = BillingTenant.run(companyId, () -> accounts.findById(companyId).orElse(null));
        if (acc == null || acc.getStatus() == BillingStatus.EXEMPT || acc.getStatus() == BillingStatus.NOT_STARTED) {
            return false;
        }
        if (props.configured()) {
            billing.retrySync(companyId);
            payments.refreshOpen(companyId, today);
        }
        return BillingTenant.run(companyId, () -> {
            BillingAccount fresh = accounts.findById(companyId).orElseThrow();
            boolean before = fresh.getStatus() == BillingStatus.BLOCKED;
            fresh.expireGrace(today);
            if (fresh.shouldBlock(today)) fresh.block();
            accounts.saveAndFlush(fresh);
            access.evict(companyId);
            return !before && fresh.getStatus() == BillingStatus.BLOCKED;
        });
    }
}
```

(O job não é `@Transactional`: cada `saveAndFlush`/chamada do `BillingService` tem sua própria transação, e uma falha numa empresa não desfaz as outras.)

- [ ] **Step 6: `application.yml`** — dentro de `app.billing:` (bloco criado na entrega 1):

```yaml
    daily-cron: ${BILLING_DAILY_CRON:0 30 3 * * *}
```

`@EnableScheduling` já existe (`WorkflowSchedulingConfig`).

- [ ] **Step 7: Usuário roda e vê passar** — `cd backend && mvn test -Dtest="BillingDailyJobTest,BillingServiceTest"` → PASS.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/becommerce/crm/billing backend/src/test/java/com/becommerce/crm/billing backend/src/main/resources/application.yml
git commit -m "feat(cobranca): job diário de reconciliação e bloqueio"
```

---

### Task 7: Endpoints da clínica ("Já paguei") e do CRM_ADMIN ("Liberar por X dias", faturas)

**Files:**
- Modify: `backend/src/main/java/com/becommerce/crm/billing/application/BillingService.java` (+ `grant`, `invoices`, `checkPayment`, campos novos em `BillingView`)
- Create: `backend/src/main/java/com/becommerce/crm/billing/web/BillingController.java`
- Modify: `backend/src/main/java/com/becommerce/crm/billing/web/BillingAdminController.java` (+ `POST /grant`, `GET /invoices`)
- Test: `BillingServiceTest` (acrescentar), `backend/src/test/java/com/becommerce/crm/billing/web/BillingControllerTest.java` (novo), `BillingAdminControllerTest` (acrescentar e ajustar construtores de `BillingView`)

**Interfaces:**
- Consumes: Task 1 (`grant`, repositório de faturas), Task 3 (`BillingPaymentService#refreshOpen`), Task 5 (`BillingAccessService#evict`).
- Produces:
  - `BillingView` passa a ser `record BillingView(String status, String plan, int monthlyPriceCents, LocalDate nextDueDate, String syncError, boolean hasSubscription, LocalDate overdueSince, LocalDate graceUntil, String openInvoiceUrl)` (`openInvoiceUrl` = `invoiceUrl` da fatura aberta de vencimento mais antigo, ou `null`). **Todos** os `new BillingView(...)` existentes nos testes ganham `, null, null, null` no fim.
  - `record InvoiceView(UUID id, LocalDate dueDate, int valueCents, String status, String invoiceUrl, LocalDateTime paidAt)` (aninhado em `BillingService`).
  - `BillingView grant(UUID companyId, int days)` (1 a 30, senão `BillingRuleException("Libere entre 1 e 30 dias.")`; sem conta → `BillingRuleException("Empresa sem cobrança ativa.")`; `until = hoje + days`; `evict`).
  - `List<InvoiceView> invoices(UUID companyId)` (últimas 24).
  - `BillingView checkPayment(UUID companyId)` (exige `props.configured()`, senão devolve a view sem consultar; chama `payments.refreshOpen(companyId, null)`; `evict`; devolve a view).
  - Construtor: `BillingService(BillingAccountJpaRepository accounts, BillingInvoiceJpaRepository invoices, CompanyRepository companies, AsaasClient asaas, BillingPaymentService payments, BillingAccessService access, BillingProperties props, Clock clock)` — atualizar `BillingServiceTest#setUp` (e o caso `activateWhenDisabledIsRejected`) com `@Mock BillingInvoiceJpaRepository invoices; @Mock BillingPaymentService payments; @Mock BillingAccessService access;`.
  - Clínica (`/api/v1/billing`, qualquer usuário autenticado da empresa; a empresa vem de `user.companyId()`, nunca do caminho): `GET /api/v1/billing` → `BillingView`; `GET /api/v1/billing/invoices` → `List<InvoiceView>`; `POST /api/v1/billing/check-payment` → `BillingView`. Sem empresa → 403.
  - CRM_ADMIN: `POST /api/v1/admin/billing/companies/{companyId}/grant` body `{"days": 7}` → `BillingView`, auditado no tenant alvo com a descrição `"Cobrança: liberada por " + days + " dias"` e `Map.of("days", days)`; `GET /api/v1/admin/billing/companies/{companyId}/invoices` → `List<InvoiceView>`.

- [ ] **Step 1: Testes do serviço (acrescentar em `BillingServiceTest`)**

```java
    @Test
    void grantUnblocksForNDays() {
        BillingAccount acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        acc.activated("cus_1", "sub_1", LocalDate.of(2026, 10, 15));
        acc.onOverdue(LocalDate.of(2026, 10, 15));
        acc.block();
        when(accounts.findById(companyId)).thenReturn(Optional.of(acc));

        var view = service.grant(companyId, 7);

        assertEquals("PAST_DUE", view.status());
        assertEquals(LocalDate.of(2026, 11, 15), view.graceUntil());
        verify(access).evict(companyId);
    }

    @Test
    void grantOutOfRangeIsRejected() {
        assertThrows(BillingRuleException.class, () -> service.grant(companyId, 0));
        assertThrows(BillingRuleException.class, () -> service.grant(companyId, 31));
    }

    @Test
    void checkPaymentRefreshesOpenInvoices() {
        BillingAccount acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        acc.activated("cus_1", "sub_1", LocalDate.of(2026, 11, 15));
        when(accounts.findById(companyId)).thenReturn(Optional.of(acc));

        service.checkPayment(companyId);

        verify(payments).refreshOpen(companyId, null);
        verify(access).evict(companyId);
    }
```

- [ ] **Step 2: Teste do controller da clínica (`BillingControllerTest`)**

```java
package com.becommerce.crm.billing.web;

import com.becommerce.crm.billing.application.BillingService;
import com.becommerce.crm.billing.application.BillingService.BillingView;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import com.becommerce.crm.shared.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BillingControllerTest {
    private final UUID company = UUID.randomUUID();
    private final BillingService service = mock(BillingService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new BillingController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();

    private void login(UUID companyId) {
        CurrentUser u = new CurrentUser(UUID.randomUUID(), "x@y.com", companyId, companyId,
                List.of("USER"), List.of(), "sub", null, "keycloak", "X", "USER");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u, null, List.of()));
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void viewUsesLoggedCompany() throws Exception {
        login(company);
        when(service.view(company)).thenReturn(
                new BillingView("PAST_DUE", "STARTER", 19700, null, null, true, null, null, "https://pix/1"));
        mvc.perform(get("/api/v1/billing"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.openInvoiceUrl").value("https://pix/1"));
    }

    @Test
    void checkPayment() throws Exception {
        login(company);
        when(service.checkPayment(company)).thenReturn(
                new BillingView("ACTIVE", "STARTER", 19700, null, null, true, null, null, null));
        mvc.perform(post("/api/v1/billing/check-payment"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void withoutCompanyIsForbidden() throws Exception {
        login(null);
        mvc.perform(get("/api/v1/billing")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
```

- [ ] **Step 3: Testes do admin (acrescentar em `BillingAdminControllerTest`)**

```java
    @Test
    void grantIsAudited() throws Exception {
        loginAs("CRM_ADMIN");
        when(service.grant(TARGET, 7)).thenReturn(
                new BillingView("PAST_DUE", "STARTER", 19700, null, null, true, null, null, null));
        mvc.perform(post(URL + "/grant").contentType("application/json").content("{\"days\":7}"))
           .andExpect(status().isOk());
        verify(auditor).record(eq(TARGET), eq(AuditAction.UPDATE), eq(AuditModule.SETTINGS), eq("Billing"),
                eq(TARGET.toString()), eq("Cobrança: liberada por 7 dias"), any(), any());
    }

    @Test
    void grantNonAdminForbidden() throws Exception {
        loginAs("ADMIN");
        mvc.perform(post(URL + "/grant").contentType("application/json").content("{\"days\":7}"))
           .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
```

E em todos os `new BillingView(...)` já existentes nesse arquivo, acrescentar `, null, null, null` no fim.

- [ ] **Step 4: Usuário roda e vê falhar** — `cd backend && mvn test -Dtest="BillingServiceTest,BillingControllerTest,BillingAdminControllerTest"` → FAIL.

- [ ] **Step 5: `BillingService`** — novo construtor (ver Interfaces), `BillingView` com 3 campos novos, e:

```java
    public record InvoiceView(UUID id, LocalDate dueDate, int valueCents, String status, String invoiceUrl,
                              LocalDateTime paidAt) {
        static InvoiceView of(BillingInvoice i) {
            return new InvoiceView(i.getId(), i.getDueDate(), i.getValueCents(), i.getStatus().name(),
                    i.getInvoiceUrl(), i.getPaidAt());
        }
    }

    static final int MAX_GRANT_DAYS = 30;

    @Transactional
    public BillingView grant(UUID companyId, int days) {
        if (days < 1 || days > MAX_GRANT_DAYS) throw new BillingRuleException("Libere entre 1 e 30 dias.");
        BillingView view = inTenant(companyId, () -> {
            BillingAccount acc = accounts.findById(companyId)
                    .orElseThrow(() -> new BillingRuleException("Empresa sem cobrança ativa."));
            acc.grant(LocalDate.now(clock).plusDays(days));
            return viewOf(accounts.saveAndFlush(acc));
        });
        access.evict(companyId);
        return view;
    }

    @Transactional(readOnly = true)
    public List<InvoiceView> invoices(UUID companyId) {
        return inTenant(companyId, () -> invoices.findTop24ByCompanyIdOrderByDueDateDesc(companyId).stream()
                .map(InvoiceView::of).toList());
    }

    public BillingView checkPayment(UUID companyId) {
        if (props.configured()) {
            payments.refreshOpen(companyId, null);
            access.evict(companyId);
        }
        return view(companyId);
    }

    /** View com a URL Pix da fatura aberta mais antiga (para a faixa e a tela de pagamento pendente). */
    private BillingView viewOf(BillingAccount a) {
        String url = invoices.findByCompanyIdAndStatusIn(a.getCompanyId(), BillingPaymentService.OPEN).stream()
                .min(java.util.Comparator.comparing(BillingInvoice::getDueDate))
                .map(BillingInvoice::getInvoiceUrl).orElse(null);
        return BillingView.of(a, url);
    }
```

`BillingView.of(BillingAccount a)` vira `of(BillingAccount a, String openInvoiceUrl)` e preenche `a.getOverdueSince()`, `a.getGraceUntil()`, `openInvoiceUrl`; `notStarted(plan)` preenche os três com `null`. Trocar **todos** os `BillingView.of(...)` existentes em `BillingService` por `viewOf(...)` (dentro do `inTenant`, para a consulta de faturas sair com o tenant certo). Em `checkPayment`, **não** anotar `@Transactional` (as chamadas ao Asaas ficam fora de transação; cada escrita do `BillingPaymentService` já faz `saveAndFlush`).

Nota para `BillingServiceTest`: com `viewOf`, `invoices.findByCompanyIdAndStatusIn(...)` é chamado nos testes existentes — o mock devolve lista vazia por padrão, então nada mais precisa ser stubado.

- [ ] **Step 6: `BillingController` (clínica)**

```java
package com.becommerce.crm.billing.web;

import com.becommerce.crm.billing.application.BillingService;
import com.becommerce.crm.billing.application.BillingService.BillingView;
import com.becommerce.crm.billing.application.BillingService.InvoiceView;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

/** Cobrança da empresa logada. Liberado pelo BillingGuardInterceptor mesmo com a empresa bloqueada. */
@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private final BillingService service;

    public BillingController(BillingService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public BillingView view(@AuthenticationPrincipal CurrentUser user) {
        return service.view(companyOf(user));
    }

    @GetMapping("/invoices")
    @PreAuthorize("isAuthenticated()")
    public List<InvoiceView> invoices(@AuthenticationPrincipal CurrentUser user) {
        return service.invoices(companyOf(user));
    }

    @PostMapping("/check-payment")
    @PreAuthorize("isAuthenticated()")
    public BillingView checkPayment(@AuthenticationPrincipal CurrentUser user) {
        return service.checkPayment(companyOf(user));
    }

    private static UUID companyOf(CurrentUser user) {
        if (user == null || user.companyId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário sem empresa.");
        }
        return user.companyId();
    }
}
```

- [ ] **Step 7: `BillingAdminController`** — acrescentar:

```java
    public record GrantRequest(@jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(30) int days) {}

    @PostMapping("/grant")
    @PreAuthorize("isAuthenticated()")
    public BillingView grant(@PathVariable UUID companyId, @Valid @RequestBody GrantRequest body,
                             @AuthenticationPrincipal CurrentUser user) {
        requireCrmAdmin(user);
        BillingView view = service.grant(companyId, body.days());
        inTenant(companyId, () -> auditor.record(companyId, AuditAction.UPDATE, AuditModule.SETTINGS, "Billing",
                companyId.toString(), "Cobrança: liberada por " + body.days() + " dias", user.userId(),
                Map.of("days", body.days())));
        return view;
    }

    @GetMapping("/invoices")
    @PreAuthorize("isAuthenticated()")
    public List<BillingService.InvoiceView> invoices(@PathVariable UUID companyId,
                                                     @AuthenticationPrincipal CurrentUser user) {
        requireCrmAdmin(user);
        return service.invoices(companyId);
    }
```

(import `java.util.List`.)

- [ ] **Step 8: Usuário roda e vê passar** — `cd backend && mvn test -Dtest="BillingServiceTest,BillingControllerTest,BillingAdminControllerTest"` → PASS.

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/com/becommerce/crm/billing backend/src/test/java/com/becommerce/crm/billing
git commit -m "feat(cobranca): \"Já paguei\", liberar por X dias e faturas"
```

---

### Task 8: Verificação da entrega

- [ ] **Step 1:** Usuário roda a suíte do módulo e vizinhos:
  `cd backend && mvn test -Dtest="BillingAccountTest,BillingInvoiceTest,AsaasClientTest,BillingPaymentServiceTest,AsaasWebhookServiceTest,AsaasWebhookControllerTest,BillingAccessServiceTest,BillingGuardInterceptorTest,BillingDailyJobTest,BillingServiceTest,BillingControllerTest,BillingAdminControllerTest,BillingCompanyListenerTest,ModuleGuardInterceptorTest"`
- [ ] **Step 2:** Push da branch → CI verde (E2E com Postgres real exercita o interceptor em `/api/v1/**` com contas inexistentes — tudo deve passar como antes).
- [ ] **Step 3 (antes da entrega 4):** no sandbox, `BILLING_ENABLED=true`, `ASAAS_API_KEY=<sandbox>`, `ASAAS_WEBHOOK_TOKEN=<gerado>`; cadastrar o webhook no painel sandbox apontando para um túnel local; pagar um Pix de teste e conferir fatura `RECEIVED` e conta `ACTIVE`; forçar `OVERDUE` e rodar o job com `BILLING_DAILY_CRON` de teste.
