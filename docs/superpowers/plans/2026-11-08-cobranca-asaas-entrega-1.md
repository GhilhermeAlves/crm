# Cobrança Asaas — Entrega 1 (dados, cliente Asaas, ativação) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Criar a base da cobrança: tabelas, regras da conta de cobrança, cliente HTTP do Asaas, ativação automática/manual da assinatura Pix e sincronização do valor quando o plano muda.

**Architecture:** Novo módulo `com.becommerce.crm.billing` no padrão do módulo `communication.telegram` (entidades JPA em `domain`, serviços em `application`, cliente HTTP e repositórios em `infrastructure`, controllers em `web`). A empresa é criada normalmente; um listener pós-commit ativa a cobrança se `app.billing.enabled=true`. Falha no Asaas nunca impede criar/editar empresa: a conta fica `SYNC_PENDING`.

**Tech Stack:** Java 21, Spring Boot (RestClient, Spring Data JPA, `@TransactionalEventListener`, `@Async`), Flyway, PostgreSQL com RLS, JUnit 5 + Mockito + `MockRestServiceServer`.

**Spec:** `docs/superpowers/specs/2026-11-08-cobranca-asaas-design.md`

## Global Constraints

- Provedor Asaas; `billingType = PIX`; `cycle = MONTHLY`; primeiro vencimento = hoje + 7 dias.
- Preço padrão (centavos): ESSENTIAL 4990, STARTER 19700, PROFESSIONAL 39700, BUSINESS 79700, ENTERPRISE 150000.
- Env: `ASAAS_BASE_URL` (padrão sandbox `https://api-sandbox.asaas.com/v3`), `ASAAS_API_KEY`, `ASAAS_WEBHOOK_TOKEN`, `BILLING_ENABLED` (padrão `false`).
- Chave do Asaas nunca em código, log ou mensagem de erro.
- Nada de paciente vai ao Asaas: só nome fantasia, CPF/CNPJ e e-mail da empresa.
- CRM_ADMIN sempre passa em qualquer regra nova.
- Tabelas com `company_id`: RLS `ENABLE` + `FORCE` + `tenant_isolation_policy` + `GRANT ... TO crm_app` (padrão da V128).
- Sem Docker local: o 1º Postgres real é o E2E do CI. Testes de backend são rodados pelo usuário (`mvn test -Dtest=...`), nunca pelo agente.
- Fora desta entrega: webhook, job diário, bloqueio 402, telas (entregas 2 e 3).

## File Structure

```
backend/src/main/resources/db/migration/V130__billing.sql          (novo)
backend/src/main/java/com/becommerce/crm/masterdata/company/domain/CompanyPlan.java   (+ preço)
backend/src/main/java/com/becommerce/crm/billing/
  domain/BillingStatus.java          estados da conta
  domain/BillingAccount.java         entidade + regras (activated, syncFailed, exempt, changePrice)
  domain/InvoiceStatus.java          estados da fatura (usado na entrega 2)
  domain/BillingInvoice.java         entidade da fatura (usada na entrega 2)
  infrastructure/BillingAccountJpaRepository.java
  infrastructure/BillingInvoiceJpaRepository.java
  infrastructure/AsaasClient.java    HTTP: criar cliente, criar/atualizar assinatura
  application/BillingProperties.java @ConfigurationProperties("app.billing")
  application/BillingService.java    casos de uso
  application/BillingCompanyListener.java  reage a CompanyCreated/Updated
  web/BillingAdminController.java    endpoints do CRM_ADMIN
backend/src/main/resources/application.yml   (+ app.billing)
backend/src/test/java/com/becommerce/crm/billing/... (espelha acima)
```

---

### Task 1: Preço por plano, migration e entidades

**Files:**
- Modify: `backend/src/main/java/com/becommerce/crm/masterdata/company/domain/CompanyPlan.java`
- Create: `backend/src/main/resources/db/migration/V130__billing.sql`
- Create: `backend/src/main/java/com/becommerce/crm/billing/domain/{BillingStatus,BillingAccount,InvoiceStatus,BillingInvoice}.java`
- Create: `backend/src/main/java/com/becommerce/crm/billing/infrastructure/{BillingAccountJpaRepository,BillingInvoiceJpaRepository}.java`
- Test: `backend/src/test/java/com/becommerce/crm/masterdata/company/domain/CompanyPlanTest.java`, `backend/src/test/java/com/becommerce/crm/billing/domain/BillingAccountTest.java`

**Interfaces:**
- Produces: `CompanyPlan#defaultMonthlyPriceCents(): int`; `BillingStatus {NOT_STARTED, SYNC_PENDING, ACTIVE, PAST_DUE, BLOCKED, EXEMPT}`; `BillingAccount.start(UUID companyId, CompanyPlan plan, int priceCents)`; métodos `activated(String customerId, String subscriptionId, LocalDate nextDue)`, `syncFailed(String error)`, `markExempt()`, `changePrice(CompanyPlan plan, int cents)`, `isChargeable()`; getters de todos os campos.

- [ ] **Step 1: Teste do preço por plano**

```java
package com.becommerce.crm.masterdata.company.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CompanyPlanTest {
    @Test
    void defaultPricesInCents() {
        assertEquals(4990, CompanyPlan.ESSENTIAL.defaultMonthlyPriceCents());
        assertEquals(19700, CompanyPlan.STARTER.defaultMonthlyPriceCents());
        assertEquals(39700, CompanyPlan.PROFESSIONAL.defaultMonthlyPriceCents());
        assertEquals(79700, CompanyPlan.BUSINESS.defaultMonthlyPriceCents());
        assertEquals(150000, CompanyPlan.ENTERPRISE.defaultMonthlyPriceCents());
    }
}
```

- [ ] **Step 2: Testes da conta de cobrança**

```java
package com.becommerce.crm.billing.domain;

import com.becommerce.crm.masterdata.company.domain.CompanyPlan;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class BillingAccountTest {
    private final UUID company = UUID.randomUUID();

    @Test
    void startsNotStartedWithPlanPrice() {
        BillingAccount a = BillingAccount.start(company, CompanyPlan.STARTER, 19700);
        assertEquals(BillingStatus.NOT_STARTED, a.getStatus());
        assertEquals(19700, a.getMonthlyPriceCents());
        assertEquals(CompanyPlan.STARTER, a.getPlan());
        assertTrue(a.isChargeable());
    }

    @Test
    void activatedStoresAsaasIdsAndClearsError() {
        BillingAccount a = BillingAccount.start(company, CompanyPlan.STARTER, 19700);
        a.syncFailed("timeout");
        a.activated("cus_1", "sub_1", LocalDate.of(2026, 11, 15));
        assertEquals(BillingStatus.ACTIVE, a.getStatus());
        assertEquals("cus_1", a.getAsaasCustomerId());
        assertEquals("sub_1", a.getAsaasSubscriptionId());
        assertEquals(LocalDate.of(2026, 11, 15), a.getNextDueDate());
        assertNull(a.getSyncError());
    }

    @Test
    void syncFailedKeepsIdsAndTruncatesError() {
        BillingAccount a = BillingAccount.start(company, CompanyPlan.STARTER, 19700);
        a.syncFailed("x".repeat(600));
        assertEquals(BillingStatus.SYNC_PENDING, a.getStatus());
        assertEquals(500, a.getSyncError().length());
    }

    @Test
    void exemptIsNotChargeable() {
        BillingAccount a = BillingAccount.start(company, CompanyPlan.STARTER, 19700);
        a.markExempt();
        assertEquals(BillingStatus.EXEMPT, a.getStatus());
        assertFalse(a.isChargeable());
    }

    @Test
    void changePriceRejectsNonPositive() {
        BillingAccount a = BillingAccount.start(company, CompanyPlan.STARTER, 19700);
        assertThrows(IllegalArgumentException.class, () -> a.changePrice(CompanyPlan.STARTER, 0));
        a.changePrice(CompanyPlan.BUSINESS, 79700);
        assertEquals(CompanyPlan.BUSINESS, a.getPlan());
        assertEquals(79700, a.getMonthlyPriceCents());
    }
}
```

- [ ] **Step 3: Usuário roda e vê falhar (não compila)**

Run: `cd backend && mvn test -Dtest="CompanyPlanTest,BillingAccountTest"`
Expected: FAIL — `defaultMonthlyPriceCents` / `BillingAccount` não existem.

- [ ] **Step 4: Preço em `CompanyPlan`** — adicionar 4º argumento e getter:

```java
    ESSENTIAL(1, 1_024, 500, 4_990),
    STARTER(2, 5_120, 2_000, 19_700),
    PROFESSIONAL(5, 25_600, 10_000, 39_700),
    BUSINESS(15, 51_200, 50_000, 79_700),
    ENTERPRISE(50, 512_000, 200_000, 150_000);

    // campo + construtor recebem int defaultMonthlyPriceCents
    public int defaultMonthlyPriceCents() { return defaultMonthlyPriceCents; }
```

- [ ] **Step 5: Migration `V130__billing.sql`**

```sql
-- V130__billing.sql
-- Cobrança automática (Asaas): conta por empresa, faturas e idempotência do webhook.

CREATE TABLE billing_accounts (
    company_id            UUID PRIMARY KEY REFERENCES companies(id) ON DELETE CASCADE,
    status                VARCHAR(20) NOT NULL,
    plan                  VARCHAR(20) NOT NULL,
    monthly_price_cents   INTEGER NOT NULL CHECK (monthly_price_cents > 0),
    asaas_customer_id     VARCHAR(64),
    asaas_subscription_id VARCHAR(64),
    next_due_date         DATE,
    overdue_since         DATE,
    grace_until           DATE,
    sync_error            VARCHAR(500),
    created_at            TIMESTAMP NOT NULL DEFAULT now(),
    updated_at            TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE billing_invoices (
    id               UUID PRIMARY KEY,
    company_id       UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    asaas_payment_id VARCHAR(64) NOT NULL UNIQUE,
    due_date         DATE NOT NULL,
    value_cents      INTEGER NOT NULL,
    status           VARCHAR(20) NOT NULL,
    invoice_url      VARCHAR(500),
    paid_at          TIMESTAMP,
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_billing_invoices_company_due ON billing_invoices(company_id, due_date DESC);

-- Tabela de plataforma (sem company_id/RLS): só o webhook escreve.
CREATE TABLE billing_webhook_events (
    asaas_event_id VARCHAR(100) PRIMARY KEY,
    event          VARCHAR(60) NOT NULL,
    received_at    TIMESTAMP NOT NULL DEFAULT now(),
    processed_at   TIMESTAMP
);
GRANT SELECT, INSERT, UPDATE ON billing_webhook_events TO crm_app;

ALTER TABLE billing_accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE billing_accounts FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON billing_accounts
    USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON billing_accounts TO crm_app;

ALTER TABLE billing_invoices ENABLE ROW LEVEL SECURITY;
ALTER TABLE billing_invoices FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON billing_invoices
    USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON billing_invoices TO crm_app;
```

Antes do commit: `ls backend/src/main/resources/db/migration | sort -V | tail -1` deve ser V129 (se outra branch já usou V130, renumerar).

- [ ] **Step 6: Enums e entidades**

`BillingStatus.java`:
```java
package com.becommerce.crm.billing.domain;

public enum BillingStatus { NOT_STARTED, SYNC_PENDING, ACTIVE, PAST_DUE, BLOCKED, EXEMPT }
```

`InvoiceStatus.java`:
```java
package com.becommerce.crm.billing.domain;

public enum InvoiceStatus { PENDING, CONFIRMED, RECEIVED, OVERDUE, REFUNDED, CANCELLED }
```

`BillingAccount.java`:
```java
package com.becommerce.crm.billing.domain;

import com.becommerce.crm.masterdata.company.domain.CompanyPlan;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "billing_accounts")
public class BillingAccount {

    private static final int MAX_ERROR = 500;

    @Id @Column(name = "company_id") private UUID companyId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private BillingStatus status;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private CompanyPlan plan;
    @Column(name = "monthly_price_cents", nullable = false) private int monthlyPriceCents;
    @Column(name = "asaas_customer_id") private String asaasCustomerId;
    @Column(name = "asaas_subscription_id") private String asaasSubscriptionId;
    @Column(name = "next_due_date") private LocalDate nextDueDate;
    @Column(name = "overdue_since") private LocalDate overdueSince;
    @Column(name = "grace_until") private LocalDate graceUntil;
    @Column(name = "sync_error") private String syncError;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt = LocalDateTime.now();

    protected BillingAccount() {}

    public static BillingAccount start(UUID companyId, CompanyPlan plan, int priceCents) {
        BillingAccount a = new BillingAccount();
        a.companyId = companyId;
        a.status = BillingStatus.NOT_STARTED;
        a.plan = plan;
        a.setPrice(priceCents);
        return a;
    }

    public void activated(String customerId, String subscriptionId, LocalDate nextDue) {
        this.asaasCustomerId = customerId;
        this.asaasSubscriptionId = subscriptionId;
        this.nextDueDate = nextDue;
        this.status = BillingStatus.ACTIVE;
        this.syncError = null;
        touch();
    }

    /** Guarda o cliente criado no Asaas mesmo se a assinatura falhar depois (evita cliente duplicado). */
    public void customerCreated(String customerId) {
        this.asaasCustomerId = customerId;
        touch();
    }

    public void syncFailed(String error) {
        this.status = BillingStatus.SYNC_PENDING;
        String e = error == null ? "erro desconhecido" : error;
        this.syncError = e.length() > MAX_ERROR ? e.substring(0, MAX_ERROR) : e;
        touch();
    }

    /** Valor/plano trocados mas ainda não enviados ao Asaas: mantém o status, registra o erro. */
    public void priceSyncFailed(String error) {
        String e = error == null ? "erro desconhecido" : error;
        this.syncError = e.length() > MAX_ERROR ? e.substring(0, MAX_ERROR) : e;
        touch();
    }

    public void priceSynced() {
        this.syncError = null;
        touch();
    }

    public void markExempt() {
        this.status = BillingStatus.EXEMPT;
        touch();
    }

    public void changePrice(CompanyPlan newPlan, int cents) {
        this.plan = newPlan;
        setPrice(cents);
        touch();
    }

    public boolean isChargeable() { return status != BillingStatus.EXEMPT; }

    public boolean hasSubscription() { return asaasSubscriptionId != null; }

    private void setPrice(int cents) {
        if (cents <= 0) throw new IllegalArgumentException("Valor mensal deve ser maior que zero.");
        this.monthlyPriceCents = cents;
    }

    private void touch() { this.updatedAt = LocalDateTime.now(); }

    public UUID getCompanyId() { return companyId; }
    public BillingStatus getStatus() { return status; }
    public CompanyPlan getPlan() { return plan; }
    public int getMonthlyPriceCents() { return monthlyPriceCents; }
    public String getAsaasCustomerId() { return asaasCustomerId; }
    public String getAsaasSubscriptionId() { return asaasSubscriptionId; }
    public LocalDate getNextDueDate() { return nextDueDate; }
    public LocalDate getOverdueSince() { return overdueSince; }
    public LocalDate getGraceUntil() { return graceUntil; }
    public String getSyncError() { return syncError; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

`BillingInvoice.java` (só mapeamento; regras na entrega 2):
```java
package com.becommerce.crm.billing.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "billing_invoices")
public class BillingInvoice {
    @Id private UUID id;
    @Column(name = "company_id", nullable = false) private UUID companyId;
    @Column(name = "asaas_payment_id", nullable = false, unique = true) private String asaasPaymentId;
    @Column(name = "due_date", nullable = false) private LocalDate dueDate;
    @Column(name = "value_cents", nullable = false) private int valueCents;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private InvoiceStatus status;
    @Column(name = "invoice_url") private String invoiceUrl;
    @Column(name = "paid_at") private LocalDateTime paidAt;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt = LocalDateTime.now();

    protected BillingInvoice() {}

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getAsaasPaymentId() { return asaasPaymentId; }
    public LocalDate getDueDate() { return dueDate; }
    public int getValueCents() { return valueCents; }
    public InvoiceStatus getStatus() { return status; }
    public String getInvoiceUrl() { return invoiceUrl; }
    public LocalDateTime getPaidAt() { return paidAt; }
}
```

Repositórios:
```java
package com.becommerce.crm.billing.infrastructure;

import com.becommerce.crm.billing.domain.BillingAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface BillingAccountJpaRepository extends JpaRepository<BillingAccount, UUID> {}
```
```java
package com.becommerce.crm.billing.infrastructure;

import com.becommerce.crm.billing.domain.BillingInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface BillingInvoiceJpaRepository extends JpaRepository<BillingInvoice, UUID> {
    List<BillingInvoice> findTop24ByCompanyIdOrderByDueDateDesc(UUID companyId);
}
```

- [ ] **Step 7: Usuário roda e vê passar**

Run: `cd backend && mvn test -Dtest="CompanyPlanTest,BillingAccountTest,CompanyServiceTest"`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/resources/db/migration/V130__billing.sql backend/src/main/java/com/becommerce/crm/billing backend/src/main/java/com/becommerce/crm/masterdata/company/domain/CompanyPlan.java backend/src/test/java/com/becommerce/crm/billing backend/src/test/java/com/becommerce/crm/masterdata/company/domain/CompanyPlanTest.java
git commit -m "feat(cobranca): tabelas, conta de cobrança e preço por plano"
```

---

### Task 2: Configuração e cliente HTTP do Asaas

**Files:**
- Create: `backend/src/main/java/com/becommerce/crm/billing/application/BillingProperties.java`
- Create: `backend/src/main/java/com/becommerce/crm/billing/infrastructure/AsaasClient.java`
- Modify: `backend/src/main/resources/application.yml` (bloco `app.billing`, ao lado de `app.telegram`)
- Modify: `backend/.env.example`, `docker/.env.example` (variáveis vazias)
- Test: `backend/src/test/java/com/becommerce/crm/billing/infrastructure/AsaasClientTest.java`

**Interfaces:**
- Produces: `BillingProperties(boolean enabled, String baseUrl, String apiKey, String webhookToken)` com `configured(): boolean` (= enabled e apiKey não vazia); `AsaasClient#createCustomer(String name, String cpfCnpj, String email, String externalReference): String`; `AsaasClient#createSubscription(String customerId, int valueCents, LocalDate nextDueDate, String description, String externalReference): String`; `AsaasClient#updateSubscriptionValue(String subscriptionId, int valueCents, String description): void`; `AsaasClient.AsaasException extends RuntimeException`.

- [ ] **Step 1: Teste do cliente**

```java
package com.becommerce.crm.billing.infrastructure;

import com.becommerce.crm.billing.application.BillingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AsaasClientTest {
    private static final String BASE = "https://api-sandbox.asaas.com/v3";
    private MockRestServiceServer server;
    private AsaasClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AsaasClient(builder, new BillingProperties(true, BASE, "KEY123", "WH"));
    }

    @Test
    void createCustomerSendsKeyAndReturnsId() {
        server.expect(requestTo(BASE + "/customers"))
              .andExpect(method(HttpMethod.POST))
              .andExpect(header("access_token", "KEY123"))
              .andExpect(jsonPath("$.cpfCnpj").value("12345678909"))
              .andExpect(jsonPath("$.externalReference").value("co-1"))
              .andRespond(withSuccess("{\"id\":\"cus_1\"}", MediaType.APPLICATION_JSON));
        assertEquals("cus_1", client.createCustomer("Clínica", "123.456.789-09", "a@b.com", "co-1"));
    }

    @Test
    void createSubscriptionIsMonthlyPixWithDecimalValue() {
        server.expect(requestTo(BASE + "/subscriptions"))
              .andExpect(jsonPath("$.customer").value("cus_1"))
              .andExpect(jsonPath("$.billingType").value("PIX"))
              .andExpect(jsonPath("$.cycle").value("MONTHLY"))
              .andExpect(jsonPath("$.value").value(197.00))
              .andExpect(jsonPath("$.nextDueDate").value("2026-11-15"))
              .andRespond(withSuccess("{\"id\":\"sub_1\"}", MediaType.APPLICATION_JSON));
        assertEquals("sub_1", client.createSubscription("cus_1", 19700, LocalDate.of(2026, 11, 15), "Plano Starter", "co-1"));
    }

    @Test
    void updateSubscriptionDoesNotTouchPendingPayments() {
        server.expect(requestTo(BASE + "/subscriptions/sub_1"))
              .andExpect(method(HttpMethod.PUT))
              .andExpect(jsonPath("$.value").value(49.90))
              .andExpect(jsonPath("$.updatePendingPayments").value(false))
              .andRespond(withSuccess("{\"id\":\"sub_1\"}", MediaType.APPLICATION_JSON));
        client.updateSubscriptionValue("sub_1", 4990, "Plano Essencial");
        server.verify();
    }

    @Test
    void errorNeverLeaksApiKey() {
        server.expect(requestTo(BASE + "/customers"))
              .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                      .body("{\"errors\":[{\"description\":\"CPF inválido\"}]}")
                      .contentType(MediaType.APPLICATION_JSON));
        var ex = assertThrows(AsaasClient.AsaasException.class,
                () -> client.createCustomer("C", "1", "a@b.com", "co-1"));
        assertTrue(ex.getMessage().contains("CPF inválido"));
        assertFalse(ex.toString().contains("KEY123"));
    }
}
```

- [ ] **Step 2: Usuário roda e vê falhar**

Run: `cd backend && mvn test -Dtest=AsaasClientTest` → FAIL (classes não existem).

- [ ] **Step 3: `BillingProperties`**

```java
package com.becommerce.crm.billing.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.billing")
public record BillingProperties(boolean enabled, String baseUrl, String apiKey, String webhookToken) {
    public boolean configured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
```

Registrar com `@EnableConfigurationProperties(BillingProperties.class)` numa `@Configuration` nova `billing/application/BillingConfiguration.java`:
```java
package com.becommerce.crm.billing.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BillingProperties.class)
class BillingConfiguration {}
```

`application.yml`, dentro de `app:` (mesmo nível de `telegram:`):
```yaml
  billing:
    enabled: ${BILLING_ENABLED:false}
    base-url: ${ASAAS_BASE_URL:https://api-sandbox.asaas.com/v3}
    api-key: ${ASAAS_API_KEY:}
    webhook-token: ${ASAAS_WEBHOOK_TOKEN:}
```

`.env.example` (backend e docker): `BILLING_ENABLED=false`, `ASAAS_BASE_URL=https://api-sandbox.asaas.com/v3`, `ASAAS_API_KEY=`, `ASAAS_WEBHOOK_TOKEN=`.

- [ ] **Step 4: `AsaasClient`**

```java
package com.becommerce.crm.billing.infrastructure;

import com.becommerce.crm.billing.application.BillingProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** Cliente da API v3 do Asaas. Nunca inclua a chave em mensagens de erro ou logs. */
@Component
public class AsaasClient {

    private final RestClient rest;
    private final BillingProperties props;

    public AsaasClient(RestClient.Builder builder, BillingProperties props) {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(5_000);
        rf.setReadTimeout(15_000);
        this.rest = builder.requestFactory(rf).build();
        this.props = props;
    }

    public static class AsaasException extends RuntimeException {
        public AsaasException(String message) { super(message); }
    }

    public String createCustomer(String name, String cpfCnpj, String email, String externalReference) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("cpfCnpj", cpfCnpj.replaceAll("\\D", ""));
        body.put("email", email);
        body.put("externalReference", externalReference);
        return post("/customers", body).path("id").asText();
    }

    public String createSubscription(String customerId, int valueCents, LocalDate nextDueDate,
                                     String description, String externalReference) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("customer", customerId);
        body.put("billingType", "PIX");
        body.put("cycle", "MONTHLY");
        body.put("value", toReais(valueCents));
        body.put("nextDueDate", nextDueDate.toString());
        body.put("description", description);
        body.put("externalReference", externalReference);
        return post("/subscriptions", body).path("id").asText();
    }

    public void updateSubscriptionValue(String subscriptionId, int valueCents, String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("value", toReais(valueCents));
        body.put("description", description);
        body.put("updatePendingPayments", false);
        call(() -> rest.put().uri(props.baseUrl() + "/subscriptions/{id}", subscriptionId)
                .headers(h -> { h.set("access_token", props.apiKey()); h.set("User-Agent", "clin3dpro-crm"); })
                .contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().body(JsonNode.class));
    }

    private JsonNode post(String path, Map<String, Object> body) {
        return call(() -> rest.post().uri(props.baseUrl() + path)
                .headers(h -> { h.set("access_token", props.apiKey()); h.set("User-Agent", "clin3dpro-crm"); })
                .contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().body(JsonNode.class));
    }

    private JsonNode call(java.util.function.Supplier<JsonNode> request) {
        try {
            return request.get();
        } catch (RestClientResponseException e) {
            throw new AsaasException("Asaas recusou (" + e.getStatusCode().value() + "): " + describe(e));
        } catch (RestClientException e) {
            throw new AsaasException("Asaas indisponível.");
        }
    }

    private static String describe(RestClientResponseException e) {
        try {
            JsonNode errors = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(e.getResponseBodyAsString()).path("errors");
            if (errors.isArray() && !errors.isEmpty()) return errors.get(0).path("description").asText();
        } catch (Exception ignored) {
            // corpo não-JSON
        }
        return "erro sem descrição";
    }

    static BigDecimal toReais(int cents) { return BigDecimal.valueOf(cents, 2); }
}
```

- [ ] **Step 5: Usuário roda e vê passar** — `mvn test -Dtest=AsaasClientTest` → PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/becommerce/crm/billing backend/src/test/java/com/becommerce/crm/billing backend/src/main/resources/application.yml backend/.env.example docker/.env.example
git commit -m "feat(cobranca): cliente do Asaas e configuração"
```

---

### Task 3: BillingService (ativar, isentar, trocar valor, tentar de novo)

**Files:**
- Create: `backend/src/main/java/com/becommerce/crm/billing/application/BillingService.java`
- Test: `backend/src/test/java/com/becommerce/crm/billing/application/BillingServiceTest.java`

**Interfaces:**
- Consumes: Task 1 (`BillingAccount`, repositórios, `CompanyPlan#defaultMonthlyPriceCents`), Task 2 (`AsaasClient`, `BillingProperties`), `CompanyRepository#findById(UUID): Optional<Company>`, `Clock` (bean existente).
- Produces: `record BillingView(String status, String plan, int monthlyPriceCents, LocalDate nextDueDate, String syncError, boolean hasSubscription)`; métodos `BillingView view(UUID companyId)`, `BillingView activate(UUID companyId)`, `BillingView markExempt(UUID companyId)`, `BillingView changePrice(UUID companyId, int cents)`, `void onPlanChanged(UUID companyId, CompanyPlan newPlan)`. Erros de regra: `BillingRuleException extends RuntimeException` (→ 400 no controller).

Regras:
- `activate`: exige `props.configured()` senão `BillingRuleException("Cobrança desligada: configure ASAAS_API_KEY e BILLING_ENABLED.")`; exige CPF/CNPJ da empresa senão `BillingRuleException("Cadastre o CPF ou CNPJ da empresa antes de ativar a cobrança.")`; cria a conta se não existe (preço = padrão do plano); se `EXEMPT` → regra "Empresa isenta não é cobrada."; se já tem assinatura → devolve a view (idempotente); reaproveita `asaasCustomerId` se já existe; `nextDueDate = LocalDate.now(clock).plusDays(7)`; erro do Asaas → `syncFailed(msg)` e salva (não relança).
- `onPlanChanged`: sem conta → nada; conta com preço igual ao padrão do plano antigo → novo preço = padrão do novo plano; preço negociado → mantém o preço, só troca o plano; se tem assinatura, envia o valor ao Asaas (erro → `priceSyncFailed`).
- `changePrice`: altera preço; se tem assinatura, envia ao Asaas (erro → `priceSyncFailed`).
- Todas as leituras/escritas rodam com `TenantContext` da empresa alvo (padrão `withTenant` do `CompanyQuotaService`), restaurando o contexto anterior.

- [ ] **Step 1: Testes**

```java
package com.becommerce.crm.billing.application;

import com.becommerce.crm.billing.domain.BillingAccount;
import com.becommerce.crm.billing.domain.BillingStatus;
import com.becommerce.crm.billing.infrastructure.AsaasClient;
import com.becommerce.crm.billing.infrastructure.BillingAccountJpaRepository;
import com.becommerce.crm.masterdata.company.application.port.output.CompanyRepository;
import com.becommerce.crm.masterdata.company.domain.Company;
import com.becommerce.crm.masterdata.company.domain.CompanyPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-08T12:00:00Z"), ZoneOffset.UTC);
    private final UUID companyId = UUID.randomUUID();

    @Mock BillingAccountJpaRepository accounts;
    @Mock CompanyRepository companies;
    @Mock AsaasClient asaas;
    @Mock Company company;
    BillingService service;

    @BeforeEach
    void setUp() {
        service = new BillingService(accounts, companies, asaas,
                new BillingProperties(true, "https://x", "KEY", "WH"), CLOCK);
        lenient().when(companies.findById(companyId)).thenReturn(Optional.of(company));
        lenient().when(company.getPlan()).thenReturn(CompanyPlan.STARTER);
        lenient().when(company.getTradingName()).thenReturn("Clínica Sorriso");
        lenient().when(company.getEmail()).thenReturn("c@s.com");
        lenient().when(company.getCnpj()).thenReturn("123.456.789-09");
        lenient().when(accounts.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void activateCreatesCustomerAndPixSubscriptionDueIn7Days() {
        when(accounts.findById(companyId)).thenReturn(Optional.empty());
        when(asaas.createCustomer("Clínica Sorriso", "123.456.789-09", "c@s.com", companyId.toString())).thenReturn("cus_1");
        when(asaas.createSubscription(eq("cus_1"), eq(19700), eq(LocalDate.of(2026, 11, 15)), anyString(), eq(companyId.toString())))
                .thenReturn("sub_1");

        var view = service.activate(companyId);

        assertEquals("ACTIVE", view.status());
        assertEquals(LocalDate.of(2026, 11, 15), view.nextDueDate());
    }

    @Test
    void activateWithoutDocumentIsRejected() {
        when(company.getCnpj()).thenReturn(null);
        assertThrows(BillingRuleException.class, () -> service.activate(companyId));
        verifyNoInteractions(asaas);
    }

    @Test
    void activateWhenDisabledIsRejected() {
        service = new BillingService(accounts, companies, asaas, new BillingProperties(false, "https://x", "", ""), CLOCK);
        assertThrows(BillingRuleException.class, () -> service.activate(companyId));
        verifyNoInteractions(asaas);
    }

    @Test
    void asaasFailureMarksSyncPendingWithoutThrowing() {
        when(accounts.findById(companyId)).thenReturn(Optional.empty());
        when(asaas.createCustomer(any(), any(), any(), any())).thenThrow(new AsaasClient.AsaasException("Asaas indisponível."));

        var view = service.activate(companyId);

        assertEquals("SYNC_PENDING", view.status());
        assertEquals("Asaas indisponível.", view.syncError());
    }

    @Test
    void retryReusesExistingCustomer() {
        BillingAccount acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        acc.customerCreated("cus_1");
        acc.syncFailed("timeout");
        when(accounts.findById(companyId)).thenReturn(Optional.of(acc));
        when(asaas.createSubscription(eq("cus_1"), anyInt(), any(), anyString(), anyString())).thenReturn("sub_1");

        assertEquals("ACTIVE", service.activate(companyId).status());
        verify(asaas, never()).createCustomer(any(), any(), any(), any());
    }

    @Test
    void activateIsIdempotentWhenSubscriptionExists() {
        BillingAccount acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        acc.activated("cus_1", "sub_1", LocalDate.of(2026, 11, 15));
        when(accounts.findById(companyId)).thenReturn(Optional.of(acc));

        service.activate(companyId);
        verifyNoInteractions(asaas);
    }

    @Test
    void planChangeUpdatesDefaultPriceAndAsaas() {
        BillingAccount acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        acc.activated("cus_1", "sub_1", LocalDate.of(2026, 11, 15));
        when(accounts.findById(companyId)).thenReturn(Optional.of(acc));

        service.onPlanChanged(companyId, CompanyPlan.BUSINESS);

        assertEquals(79700, acc.getMonthlyPriceCents());
        verify(asaas).updateSubscriptionValue(eq("sub_1"), eq(79700), anyString());
    }

    @Test
    void planChangeKeepsNegotiatedPrice() {
        BillingAccount acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 15000);
        when(accounts.findById(companyId)).thenReturn(Optional.of(acc));

        service.onPlanChanged(companyId, CompanyPlan.BUSINESS);

        assertEquals(15000, acc.getMonthlyPriceCents());
        assertEquals(CompanyPlan.BUSINESS, acc.getPlan());
        verifyNoInteractions(asaas);
    }

    @Test
    void exemptCompanyCannotBeActivated() {
        BillingAccount acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        acc.markExempt();
        when(accounts.findById(companyId)).thenReturn(Optional.of(acc));
        assertThrows(BillingRuleException.class, () -> service.activate(companyId));
    }

    @Test
    void changePriceFailureKeepsStatusAndRecordsError() {
        BillingAccount acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        acc.activated("cus_1", "sub_1", LocalDate.of(2026, 11, 15));
        when(accounts.findById(companyId)).thenReturn(Optional.of(acc));
        doThrow(new AsaasClient.AsaasException("Asaas indisponível.")).when(asaas).updateSubscriptionValue(any(), anyInt(), any());

        var view = service.changePrice(companyId, 17000);

        assertEquals("ACTIVE", view.status());
        assertEquals(17000, view.monthlyPriceCents());
        assertEquals("Asaas indisponível.", view.syncError());
    }
}
```

- [ ] **Step 2: Usuário roda e vê falhar** — `mvn test -Dtest=BillingServiceTest` → FAIL.

- [ ] **Step 3: Implementação**

`BillingRuleException.java`:
```java
package com.becommerce.crm.billing.application;

public class BillingRuleException extends RuntimeException {
    public BillingRuleException(String message) { super(message); }
}
```

`BillingService.java`:
```java
package com.becommerce.crm.billing.application;

import com.becommerce.crm.billing.domain.BillingAccount;
import com.becommerce.crm.billing.domain.BillingStatus;
import com.becommerce.crm.billing.infrastructure.AsaasClient;
import com.becommerce.crm.billing.infrastructure.BillingAccountJpaRepository;
import com.becommerce.crm.masterdata.company.application.port.output.CompanyRepository;
import com.becommerce.crm.masterdata.company.domain.Company;
import com.becommerce.crm.masterdata.company.domain.CompanyNotFoundException;
import com.becommerce.crm.masterdata.company.domain.CompanyPlan;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);
    static final int FIRST_DUE_DAYS = 7;

    private final BillingAccountJpaRepository accounts;
    private final CompanyRepository companies;
    private final AsaasClient asaas;
    private final BillingProperties props;
    private final Clock clock;

    public BillingService(BillingAccountJpaRepository accounts, CompanyRepository companies,
                          AsaasClient asaas, BillingProperties props, Clock clock) {
        this.accounts = accounts;
        this.companies = companies;
        this.asaas = asaas;
        this.props = props;
        this.clock = clock;
    }

    public record BillingView(String status, String plan, int monthlyPriceCents, LocalDate nextDueDate,
                              String syncError, boolean hasSubscription) {
        static BillingView of(BillingAccount a) {
            return new BillingView(a.getStatus().name(), a.getPlan().name(), a.getMonthlyPriceCents(),
                    a.getNextDueDate(), a.getSyncError(), a.hasSubscription());
        }
        static BillingView notStarted(CompanyPlan plan) {
            return new BillingView(BillingStatus.NOT_STARTED.name(), plan.name(),
                    plan.defaultMonthlyPriceCents(), null, null, false);
        }
    }

    @Transactional(readOnly = true)
    public BillingView view(UUID companyId) {
        Company company = requireCompany(companyId);
        return inTenant(companyId, () -> accounts.findById(companyId)
                .map(BillingView::of)
                .orElseGet(() -> BillingView.notStarted(company.getPlan())));
    }

    @Transactional
    public BillingView activate(UUID companyId) {
        if (!props.configured()) {
            throw new BillingRuleException("Cobrança desligada: configure ASAAS_API_KEY e BILLING_ENABLED.");
        }
        Company company = requireCompany(companyId);
        if (company.getCnpj() == null || company.getCnpj().isBlank()) {
            throw new BillingRuleException("Cadastre o CPF ou CNPJ da empresa antes de ativar a cobrança.");
        }
        return inTenant(companyId, () -> {
            BillingAccount acc = accounts.findById(companyId).orElseGet(() -> BillingAccount.start(
                    companyId, company.getPlan(), company.getPlan().defaultMonthlyPriceCents()));
            if (acc.getStatus() == BillingStatus.EXEMPT) {
                throw new BillingRuleException("Empresa isenta não é cobrada.");
            }
            if (acc.hasSubscription()) {
                return BillingView.of(acc);
            }
            String ref = companyId.toString();
            try {
                if (acc.getAsaasCustomerId() == null) {
                    acc.customerCreated(asaas.createCustomer(
                            company.getTradingName(), company.getCnpj(), company.getEmail(), ref));
                }
                LocalDate firstDue = LocalDate.now(clock).plusDays(FIRST_DUE_DAYS);
                String sub = asaas.createSubscription(acc.getAsaasCustomerId(), acc.getMonthlyPriceCents(),
                        firstDue, description(acc.getPlan()), ref);
                acc.activated(acc.getAsaasCustomerId(), sub, firstDue);
            } catch (AsaasClient.AsaasException e) {
                log.warn("Falha ao ativar cobrança da empresa {}: {}", companyId, e.getMessage());
                acc.syncFailed(e.getMessage());
            }
            return BillingView.of(accounts.save(acc));
        });
    }

    @Transactional
    public BillingView markExempt(UUID companyId) {
        Company company = requireCompany(companyId);
        return inTenant(companyId, () -> {
            BillingAccount acc = accounts.findById(companyId).orElseGet(() -> BillingAccount.start(
                    companyId, company.getPlan(), company.getPlan().defaultMonthlyPriceCents()));
            acc.markExempt();
            return BillingView.of(accounts.save(acc));
        });
    }

    @Transactional
    public BillingView changePrice(UUID companyId, int cents) {
        Company company = requireCompany(companyId);
        return inTenant(companyId, () -> {
            BillingAccount acc = accounts.findById(companyId).orElseGet(() -> BillingAccount.start(
                    companyId, company.getPlan(), company.getPlan().defaultMonthlyPriceCents()));
            try {
                acc.changePrice(acc.getPlan(), cents);
            } catch (IllegalArgumentException e) {
                throw new BillingRuleException(e.getMessage());
            }
            pushPrice(acc);
            return BillingView.of(accounts.save(acc));
        });
    }

    @Transactional
    public void onPlanChanged(UUID companyId, CompanyPlan newPlan) {
        inTenant(companyId, () -> {
            accounts.findById(companyId).ifPresent(acc -> {
                if (acc.getPlan() == newPlan) return;
                boolean defaultPrice = acc.getMonthlyPriceCents() == acc.getPlan().defaultMonthlyPriceCents();
                int cents = defaultPrice ? newPlan.defaultMonthlyPriceCents() : acc.getMonthlyPriceCents();
                acc.changePrice(newPlan, cents);
                if (defaultPrice) pushPrice(acc);
                accounts.save(acc);
            });
            return null;
        });
    }

    private void pushPrice(BillingAccount acc) {
        if (!acc.hasSubscription()) return;
        try {
            asaas.updateSubscriptionValue(acc.getAsaasSubscriptionId(), acc.getMonthlyPriceCents(),
                    description(acc.getPlan()));
            acc.priceSynced();
        } catch (AsaasClient.AsaasException e) {
            log.warn("Falha ao atualizar valor no Asaas da empresa {}: {}", acc.getCompanyId(), e.getMessage());
            acc.priceSyncFailed(e.getMessage());
        }
    }

    private static String description(CompanyPlan plan) {
        return "Mensalidade CRM - plano " + plan.name().charAt(0) + plan.name().substring(1).toLowerCase();
    }

    private Company requireCompany(UUID companyId) {
        return companies.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
    }

    private <T> T inTenant(UUID companyId, Supplier<T> work) {
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

Nota: `planChangeKeepsNegotiatedPrice` não chama o Asaas porque o valor não muda — correto.

- [ ] **Step 4: Usuário roda e vê passar** — `mvn test -Dtest=BillingServiceTest` → PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/becommerce/crm/billing backend/src/test/java/com/becommerce/crm/billing
git commit -m "feat(cobranca): ativar assinatura Pix, isentar e trocar valor"
```

---

### Task 4: Ativação automática e troca de plano via eventos

**Files:**
- Modify: `backend/src/main/java/com/becommerce/crm/masterdata/company/domain/event/CompanyUpdatedEvent.java` (+ campo `CompanyPlan plan`)
- Create: `backend/src/main/java/com/becommerce/crm/billing/application/BillingCompanyListener.java`
- Test: `backend/src/test/java/com/becommerce/crm/billing/application/BillingCompanyListenerTest.java`

**Interfaces:**
- Consumes: `CompanyCreatedEvent(UUID companyId, ...)`, `CompanyUpdatedEvent(UUID companyId, String companyName, String cnpj, CompanyPlan plan, LocalDateTime occurredAt)`, `BillingService#activate`, `BillingService#onPlanChanged`, `BillingProperties#configured`.

- [ ] **Step 1: Teste do listener**

```java
package com.becommerce.crm.billing.application;

import com.becommerce.crm.masterdata.company.domain.CompanyPlan;
import com.becommerce.crm.masterdata.company.domain.event.CompanyCreatedEvent;
import com.becommerce.crm.masterdata.company.domain.event.CompanyUpdatedEvent;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.mockito.Mockito.*;

class BillingCompanyListenerTest {
    private final BillingService service = mock(BillingService.class);
    private final UUID id = UUID.randomUUID();

    @Test
    void createdActivatesWhenConfigured() {
        new BillingCompanyListener(service, new BillingProperties(true, "u", "KEY", "W"))
                .onCreated(new CompanyCreatedEvent(id, "C", "1", "a@b.com", LocalDateTime.now()));
        verify(service).activate(id);
    }

    @Test
    void createdDoesNothingWhenDisabled() {
        new BillingCompanyListener(service, new BillingProperties(false, "u", "", ""))
                .onCreated(new CompanyCreatedEvent(id, "C", "1", "a@b.com", LocalDateTime.now()));
        verifyNoInteractions(service);
    }

    @Test
    void createdSwallowsRuleErrors() {
        doThrow(new BillingRuleException("sem documento")).when(service).activate(id);
        new BillingCompanyListener(service, new BillingProperties(true, "u", "KEY", "W"))
                .onCreated(new CompanyCreatedEvent(id, "C", null, "a@b.com", LocalDateTime.now()));
        verify(service).activate(id);
    }

    @Test
    void updatedForwardsPlan() {
        new BillingCompanyListener(service, new BillingProperties(false, "u", "", ""))
                .onUpdated(new CompanyUpdatedEvent(id, "C", "1", CompanyPlan.BUSINESS, LocalDateTime.now()));
        verify(service).onPlanChanged(id, CompanyPlan.BUSINESS);
    }
}
```

(`onUpdated` roda mesmo com cobrança desligada: só mexe em contas existentes e `pushPrice` só chama o Asaas se houver assinatura.)

- [ ] **Step 2: Usuário roda e vê falhar.**

- [ ] **Step 3: `CompanyUpdatedEvent` com plano**

```java
public record CompanyUpdatedEvent(
        UUID companyId,
        String companyName,
        String cnpj,
        CompanyPlan plan,
        LocalDateTime occurredAt
) {
    public static CompanyUpdatedEvent create(Company company) {
        return new CompanyUpdatedEvent(company.getId(), company.getLegalName(), company.getCnpj(),
                company.getPlan(), LocalDateTime.now());
    }
}
```

Depois: `git grep -n "new CompanyUpdatedEvent(" backend/` e ajustar cada chamada direta (testes/listeners) para o novo argumento.

- [ ] **Step 4: Listener**

```java
package com.becommerce.crm.billing.application;

import com.becommerce.crm.masterdata.company.domain.event.CompanyCreatedEvent;
import com.becommerce.crm.masterdata.company.domain.event.CompanyUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Liga a cobrança à criação/edição da empresa, depois do commit (falha no Asaas não desfaz nada). */
@Component
public class BillingCompanyListener {

    private static final Logger log = LoggerFactory.getLogger(BillingCompanyListener.class);

    private final BillingService service;
    private final BillingProperties props;

    public BillingCompanyListener(BillingService service, BillingProperties props) {
        this.service = service;
        this.props = props;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCreated(CompanyCreatedEvent event) {
        if (!props.configured()) return;
        try {
            service.activate(event.companyId());
        } catch (BillingRuleException e) {
            log.info("Cobrança não ativada para {}: {}", event.companyId(), e.getMessage());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUpdated(CompanyUpdatedEvent event) {
        if (event.plan() != null) {
            service.onPlanChanged(event.companyId(), event.plan());
        }
    }
}
```

Atenção: `OnboardingService` cria empresa sem publicar `CompanyCreatedEvent` — confirmar com `git grep -n "CompanyCreatedEvent" backend/src/main`. Se não publicar, essas empresas ficam `NOT_STARTED` e o CRM_ADMIN ativa pelo botão (comportamento aceito pela spec para empresas fora do fluxo do admin).

- [ ] **Step 5: Usuário roda e vê passar** — `mvn test -Dtest="BillingCompanyListenerTest,CompanyServiceTest,AuditEventListener*"` → PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/becommerce/crm/billing backend/src/main/java/com/becommerce/crm/masterdata/company/domain/event/CompanyUpdatedEvent.java backend/src/test
git commit -m "feat(cobranca): ativa ao criar empresa e sincroniza troca de plano"
```

---

### Task 5: Endpoints do CRM_ADMIN

**Files:**
- Create: `backend/src/main/java/com/becommerce/crm/billing/web/BillingAdminController.java`
- Test: `backend/src/test/java/com/becommerce/crm/billing/web/BillingAdminControllerTest.java`

**Interfaces:**
- Consumes: `BillingService` (Task 3), `CurrentUser#isCrmAdmin()`.
- Produces (usado pelas telas na entrega 3):
  - `GET  /api/v1/admin/billing/companies/{companyId}` → `BillingView`
  - `POST /api/v1/admin/billing/companies/{companyId}/activate` → `BillingView` (também serve de "Tentar novamente")
  - `POST /api/v1/admin/billing/companies/{companyId}/exempt` → `BillingView`
  - `PUT  /api/v1/admin/billing/companies/{companyId}/price` body `{"monthlyPriceCents": 17000}` → `BillingView`
  - Não-CRM_ADMIN → 403. `BillingRuleException` → 400 `{"message": ...}`.

- [ ] **Step 1: Teste**

```java
package com.becommerce.crm.billing.web;

import com.becommerce.crm.billing.application.BillingRuleException;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BillingAdminControllerTest {
    private static final UUID TARGET = UUID.randomUUID();
    private static final String URL = "/api/v1/admin/billing/companies/" + TARGET;
    private final BillingService service = mock(BillingService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new BillingAdminController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();

    private void loginAs(String role) {
        UUID company = UUID.randomUUID();
        CurrentUser u = new CurrentUser(UUID.randomUUID(), "x@y.com", company, company,
                List.of(role), List.of(), "sub", null, "keycloak", "X", role);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u, null, List.of()));
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void crmAdminActivates() throws Exception {
        loginAs("CRM_ADMIN");
        when(service.activate(TARGET)).thenReturn(
                new BillingView("ACTIVE", "STARTER", 19700, LocalDate.of(2026, 11, 15), null, true));
        mvc.perform(post(URL + "/activate"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void nonAdminIsForbidden() throws Exception {
        loginAs("ADMIN");
        mvc.perform(post(URL + "/activate")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void ruleErrorIs400() throws Exception {
        loginAs("CRM_ADMIN");
        when(service.activate(TARGET)).thenThrow(new BillingRuleException("Cadastre o CPF ou CNPJ"));
        mvc.perform(post(URL + "/activate")).andExpect(status().isBadRequest());
    }

    @Test
    void changePrice() throws Exception {
        loginAs("CRM_ADMIN");
        when(service.changePrice(TARGET, 17000)).thenReturn(
                new BillingView("ACTIVE", "STARTER", 17000, null, null, true));
        mvc.perform(put(URL + "/price").contentType("application/json").content("{\"monthlyPriceCents\":17000}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.monthlyPriceCents").value(17000));
    }
}
```

`CurrentUser#isCrmAdmin()` é `roles.contains("CRM_ADMIN")` — por isso o teste passa o papel em `roles`.

- [ ] **Step 2: Usuário roda e vê falhar.**

- [ ] **Step 3: Controller**

```java
package com.becommerce.crm.billing.web;

import com.becommerce.crm.billing.application.BillingRuleException;
import com.becommerce.crm.billing.application.BillingService;
import com.becommerce.crm.billing.application.BillingService.BillingView;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.UUID;

/** Cobrança das empresas — só o administrador do CRM. */
@RestController
@RequestMapping("/api/v1/admin/billing/companies/{companyId}")
public class BillingAdminController {

    private final BillingService service;

    public BillingAdminController(BillingService service) { this.service = service; }

    public record PriceRequest(@Positive int monthlyPriceCents) {}

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public BillingView get(@PathVariable UUID companyId, @AuthenticationPrincipal CurrentUser user) {
        requireCrmAdmin(user);
        return service.view(companyId);
    }

    @PostMapping("/activate")
    @PreAuthorize("isAuthenticated()")
    public BillingView activate(@PathVariable UUID companyId, @AuthenticationPrincipal CurrentUser user) {
        requireCrmAdmin(user);
        return service.activate(companyId);
    }

    @PostMapping("/exempt")
    @PreAuthorize("isAuthenticated()")
    public BillingView exempt(@PathVariable UUID companyId, @AuthenticationPrincipal CurrentUser user) {
        requireCrmAdmin(user);
        return service.markExempt(companyId);
    }

    @PutMapping("/price")
    @PreAuthorize("isAuthenticated()")
    public BillingView price(@PathVariable UUID companyId, @Valid @RequestBody PriceRequest body,
                             @AuthenticationPrincipal CurrentUser user) {
        requireCrmAdmin(user);
        return service.changePrice(companyId, body.monthlyPriceCents());
    }

    @ExceptionHandler(BillingRuleException.class)
    public ResponseEntity<Map<String, String>> rule(BillingRuleException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

    private static void requireCrmAdmin(CurrentUser user) {
        if (user == null || !user.isCrmAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Só o administrador do CRM gerencia a cobrança.");
        }
    }
}
```

- [ ] **Step 4: Usuário roda e vê passar** — `mvn test -Dtest="BillingAdminControllerTest"` → PASS.

- [ ] **Step 5: Auditoria de isentar e mudar valor** — o controller recebe também `TenantAuditRecorder auditor` (construtor `BillingAdminController(BillingService service, TenantAuditRecorder auditor)`; no teste, `mock(TenantAuditRecorder.class)`). Após `markExempt` e `changePrice`:

```java
auditor.record(companyId, AuditAction.UPDATE, AuditModule.SETTINGS, "Billing", companyId.toString(),
        "Cobrança: empresa marcada como isenta", user.userId(), null);
```
```java
auditor.record(companyId, AuditAction.UPDATE, AuditModule.SETTINGS, "Billing", companyId.toString(),
        "Cobrança: valor mensal alterado", user.userId(), Map.of("monthlyPriceCents", body.monthlyPriceCents()));
```

Imports: `com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder`, `com.becommerce.crm.analytics.audit.domain.AuditAction`, `com.becommerce.crm.analytics.audit.domain.AuditModule`. Teste extra em `BillingAdminControllerTest`:

```java
    @Test
    void changePriceIsAudited() throws Exception {
        loginAs("CRM_ADMIN");
        when(service.changePrice(TARGET, 17000)).thenReturn(new BillingView("ACTIVE", "STARTER", 17000, null, null, true));
        mvc.perform(put(URL + "/price").contentType("application/json").content("{\"monthlyPriceCents\":17000}"))
           .andExpect(status().isOk());
        verify(auditor).record(eq(TARGET), eq(AuditAction.UPDATE), eq(AuditModule.SETTINGS), eq("Billing"),
                eq(TARGET.toString()), anyString(), any(), any());
    }
```

(o campo do teste passa a ser `private final TenantAuditRecorder auditor = mock(TenantAuditRecorder.class);` e o controller é montado com `new BillingAdminController(service, auditor)`).

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/becommerce/crm/billing backend/src/test/java/com/becommerce/crm/billing
git commit -m "feat(cobranca): endpoints do CRM_ADMIN para ativar, isentar e mudar valor"
```

---

### Task 6: Verificação da entrega

- [ ] **Step 1:** Usuário roda a suíte do módulo e vizinhos:
  `cd backend && mvn test -Dtest="CompanyPlanTest,BillingAccountTest,AsaasClientTest,BillingServiceTest,BillingCompanyListenerTest,BillingAdminControllerTest,CompanyServiceTest,CompanyControllerTest"`
- [ ] **Step 2:** Push da branch → CI verde (inclui E2E com Postgres real, que valida a V130 e o RLS).
- [ ] **Step 3:** Teste manual no sandbox (opcional nesta entrega, obrigatório antes da entrega 4): `BILLING_ENABLED=true`, `ASAAS_API_KEY=<sandbox>` local → `POST /api/v1/admin/billing/companies/{id}/activate` → conferir cliente e assinatura no painel sandbox do Asaas.
