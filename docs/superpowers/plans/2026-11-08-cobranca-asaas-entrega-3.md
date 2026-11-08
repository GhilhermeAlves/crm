# Cobrança Asaas — Entrega 3 (telas) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mostrar a cobrança na interface: faixa de mensalidade vencida, página `/pagamento-pendente` (com "Já paguei") para empresa bloqueada, "Plano e cobrança" nas configurações da clínica, cartão "Cobrança" no detalhe da empresa e coluna de status na lista de empresas (CRM_ADMIN).

**Architecture:** Nova feature `frontend-refine/src/features/billing/` (types, service, hooks, componentes) no padrão das outras features (axios `api` com `baseURL: "/api/v1"`, TanStack Query, `useMutationDefaults`). O interceptador do axios trata 402 `BILLING_BLOCKED` mandando para `/pagamento-pendente`, rota fora do `DashboardLayout` (o layout chama endpoints que respondem 402 quando a empresa está bloqueada). Um endpoint novo no backend devolve o status de cobrança de todas as empresas para a coluna da lista.

**Tech Stack:** Next.js 14 (app router), React 18, TypeScript, TanStack Query 5, axios, Tailwind + componentes `@/components/ui/*`, lucide-react, Vitest + Testing Library. Backend: Spring Boot (JdbcTemplate), JUnit 5 + Mockito.

**Spec:** `docs/superpowers/specs/2026-11-08-cobranca-asaas-design.md`

**Base:** branch `feature/cobranca-asaas-entrega-3`, criada a partir de `feature/cobranca-asaas-entrega-2` (PR #123 ainda aberto — esta entrega usa os endpoints dele).

## Global Constraints

- Faixa no topo quando `PAST_DUE`: "Mensalidade venceu em dd/mm" + "Pagar com Pix" (abre `invoice_url`). Só admins da clínica (permissão `company:update`) veem o botão; demais veem o aviso.
- `/pagamento-pendente`: valor, botão Pix, "Já paguei", aviso de que os dados estão guardados.
- Configurações → "Plano e cobrança": plano, valor, próximo vencimento, histórico (admins da clínica — permissão `company:update`).
- CRM_ADMIN (detalhe da empresa): cartão "Cobrança" com status, valor editável, próximo vencimento, histórico, botões "Ativar cobrança", "Marcar como isenta", "Liberar por X dias" (1 a 30), "Tentar novamente". Lista de empresas: coluna de status da cobrança.
- Frontend: interceptador HTTP trata 402 `BILLING_BLOCKED` → rota `/pagamento-pendente`. A página só pode chamar `/api/v1/billing/**` e `/api/v1/me/**`/`/api/v1/auth/**` (todo o resto responde 402 para empresa bloqueada).
- CRM_ADMIN nunca é bloqueado e sempre passa em qualquer regra nova.
- Valores vêm em centavos (`monthlyPriceCents`, `valueCents`); exibir como `R$ 197,00` (pt-BR). Datas `LocalDate` vêm como `"yyyy-MM-dd"`: formatar sem passar por `new Date(str)` (fuso desloca 1 dia).
- Textos da interface em pt-BR.
- Toda alteração em arquivo do frontend passa por `npx prettier --check --end-of-line auto <arquivos>` (corrigir com `--write`).
- Testes do frontend: o agente pode rodar `npx vitest run <arquivos>` e `npm run typecheck` dentro de `frontend-refine`. Testes do backend: nunca `mvn test` pelo agente; só `cd backend && mvn -q -o -DskipTests test-compile`, e o usuário roda o `mvn test`.
- Status de cobrança e rótulos: `NOT_STARTED` "Não iniciada", `SYNC_PENDING` "Pendente de envio", `ACTIVE` "Em dia", `PAST_DUE` "Vencida", `BLOCKED` "Bloqueada", `EXEMPT` "Isenta". Fatura: `PENDING` "Aberta", `CONFIRMED` "Confirmada", `RECEIVED` "Paga", `OVERDUE` "Vencida", `REFUNDED` "Estornada", `CANCELLED` "Cancelada".

## Contrato da API (entregas 1 e 2 — já existe)

```
BillingView  { status, plan, monthlyPriceCents, nextDueDate, syncError, hasSubscription,
               overdueSince, graceUntil, openInvoiceUrl }      (datas "yyyy-MM-dd" ou null)
InvoiceView  { id, dueDate, valueCents, status, invoiceUrl, paidAt }   (paidAt ISO datetime ou null)

Clínica (empresa logada):
  GET  /api/v1/billing                 → BillingView (syncError sempre null)
  GET  /api/v1/billing/invoices        → InvoiceView[]
  POST /api/v1/billing/check-payment   → BillingView
CRM_ADMIN:
  GET  /api/v1/admin/billing/companies/{id}            → BillingView
  GET  /api/v1/admin/billing/companies/{id}/invoices   → InvoiceView[]
  POST /api/v1/admin/billing/companies/{id}/activate   → BillingView   (também "Tentar novamente")
  POST /api/v1/admin/billing/companies/{id}/exempt     → BillingView
  PUT  /api/v1/admin/billing/companies/{id}/price  {monthlyPriceCents} → BillingView
  POST /api/v1/admin/billing/companies/{id}/grant  {days}              → BillingView
Erros: 400 {"message"}, 403, 409 {"message"}; bloqueio 402 {"code":"BILLING_BLOCKED", "message"}.
```

## File Structure

```
backend/src/main/java/com/becommerce/crm/billing/application/BillingOverviewService.java   (novo)
backend/src/main/java/com/becommerce/crm/billing/web/BillingAdminOverviewController.java   (novo)
backend/src/test/java/com/becommerce/crm/billing/application/BillingOverviewServiceTest.java
backend/src/test/java/com/becommerce/crm/billing/web/BillingAdminOverviewControllerTest.java

frontend-refine/src/features/billing/
  types.ts                      tipos + rótulos
  format.ts                     formatCents, formatLocalDate
  service.ts                    chamadas HTTP
  hooks.ts                      queries/mutations
  components/BillingStatusBadge.tsx
  components/InvoiceTable.tsx
  components/PastDueBanner.tsx
  components/PaymentPendingView.tsx
  components/BillingSettingsView.tsx
  components/AdminBillingCard.tsx
frontend-refine/src/lib/api.ts                                  (+ 402)
frontend-refine/src/lib/constants.ts                            (+ rotas)
frontend-refine/src/app/(billing)/layout.tsx                    (novo; ProtectedRoute sem DashboardLayout)
frontend-refine/src/app/(billing)/pagamento-pendente/page.tsx   (novo)
frontend-refine/src/app/(dashboard)/settings/billing/page.tsx   (novo)
frontend-refine/src/components/layout/DashboardLayout.tsx       (+ PastDueBanner)
frontend-refine/src/components/layout/navigation.ts             (+ "Plano e cobrança")
frontend-refine/src/features/identity/tenants/components/TenantDetails.tsx (+ AdminBillingCard)
frontend-refine/src/features/identity/tenants/components/TenantTable.tsx   (+ coluna Cobrança)
frontend-refine/src/__tests__/features/billing/*.test.ts(x)
```

---

### Task 1: Backend — status de cobrança de todas as empresas (coluna da lista)

**Files:**
- Create: `backend/src/main/java/com/becommerce/crm/billing/application/BillingOverviewService.java`
- Create: `backend/src/main/java/com/becommerce/crm/billing/web/BillingAdminOverviewController.java`
- Test: `backend/src/test/java/com/becommerce/crm/billing/application/BillingOverviewServiceTest.java`, `backend/src/test/java/com/becommerce/crm/billing/web/BillingAdminOverviewControllerTest.java`

**Interfaces:**
- Consumes: `BillingTenant.run(UUID, Supplier)`, `BillingAccountJpaRepository#findById`.
- Produces: `record CompanyBillingStatus(UUID companyId, String status)`; `BillingOverviewService#statuses(): List<CompanyBillingStatus>` (uma entrada por empresa; sem conta → `"NOT_STARTED"`); `GET /api/v1/admin/billing/companies` → `List<CompanyBillingStatus>` (403 para não-CRM_ADMIN).

`billing_accounts` tem RLS FORCE: a leitura é feita empresa a empresa dentro de `BillingTenant.run`, com a lista de ids vinda de `SELECT id FROM companies` (sem RLS, mesmo padrão do `BillingDailyJob`). Falha numa empresa → status `"UNKNOWN"` e segue.

- [ ] **Step 1: Testes**

```java
package com.becommerce.crm.billing.application;

import com.becommerce.crm.billing.domain.BillingAccount;
import com.becommerce.crm.billing.infrastructure.BillingAccountJpaRepository;
import com.becommerce.crm.masterdata.company.domain.CompanyPlan;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BillingOverviewServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final BillingAccountJpaRepository accounts = mock(BillingAccountJpaRepository.class);
    private final BillingOverviewService service = new BillingOverviewService(jdbc, accounts);

    @Test
    void oneStatusPerCompany() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        when(jdbc.queryForList(anyString(), eq(UUID.class))).thenReturn(List.of(a, b, c));
        BillingAccount acc = BillingAccount.start(a, CompanyPlan.STARTER, 19700);
        acc.activated("cus", "sub", LocalDate.of(2026, 11, 15));
        when(accounts.findById(a)).thenReturn(Optional.of(acc));
        when(accounts.findById(b)).thenReturn(Optional.empty());
        when(accounts.findById(c)).thenThrow(new RuntimeException("db"));

        var result = service.statuses();

        assertEquals(List.of(
                new BillingOverviewService.CompanyBillingStatus(a, "ACTIVE"),
                new BillingOverviewService.CompanyBillingStatus(b, "NOT_STARTED"),
                new BillingOverviewService.CompanyBillingStatus(c, "UNKNOWN")), result);
    }
}
```

```java
package com.becommerce.crm.billing.web;

import com.becommerce.crm.billing.application.BillingOverviewService;
import com.becommerce.crm.billing.application.BillingOverviewService.CompanyBillingStatus;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BillingAdminOverviewControllerTest {
    private final BillingOverviewService service = mock(BillingOverviewService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new BillingAdminOverviewController(service))
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
    void crmAdminListsStatuses() throws Exception {
        loginAs("CRM_ADMIN");
        UUID id = UUID.randomUUID();
        when(service.statuses()).thenReturn(List.of(new CompanyBillingStatus(id, "PAST_DUE")));
        mvc.perform(get("/api/v1/admin/billing/companies"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].companyId").value(id.toString()))
           .andExpect(jsonPath("$[0].status").value("PAST_DUE"));
    }

    @Test
    void nonAdminForbidden() throws Exception {
        loginAs("ADMIN");
        mvc.perform(get("/api/v1/admin/billing/companies")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
```

- [ ] **Step 2: Usuário roda e vê falhar** — `cd backend && mvn test -Dtest="BillingOverviewServiceTest,BillingAdminOverviewControllerTest"` → FAIL.

- [ ] **Step 3: Implementação**

```java
package com.becommerce.crm.billing.application;

import com.becommerce.crm.billing.infrastructure.BillingAccountJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Status de cobrança de todas as empresas (coluna da lista do CRM_ADMIN). */
@Service
public class BillingOverviewService {

    private static final Logger log = LoggerFactory.getLogger(BillingOverviewService.class);

    public record CompanyBillingStatus(UUID companyId, String status) {}

    private final JdbcTemplate jdbc;
    private final BillingAccountJpaRepository accounts;

    public BillingOverviewService(JdbcTemplate jdbc, BillingAccountJpaRepository accounts) {
        this.jdbc = jdbc;
        this.accounts = accounts;
    }

    public List<CompanyBillingStatus> statuses() {
        List<UUID> ids = jdbc.queryForList("SELECT id FROM companies ORDER BY created_at", UUID.class);
        List<CompanyBillingStatus> result = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            String status;
            try {
                status = BillingTenant.run(id, () -> accounts.findById(id)
                        .map(a -> a.getStatus().name())
                        .orElse("NOT_STARTED"));
            } catch (RuntimeException e) {
                log.warn("[BILLING] status da empresa {} indisponível: {}", id, e.getMessage());
                status = "UNKNOWN";
            }
            result.add(new CompanyBillingStatus(id, status));
        }
        return result;
    }
}
```

Antes de usar `ORDER BY created_at`, confirmar que a coluna existe em `companies`; se não, usar só `SELECT id FROM companies`.

```java
package com.becommerce.crm.billing.web;

import com.becommerce.crm.billing.application.BillingOverviewService;
import com.becommerce.crm.billing.application.BillingOverviewService.CompanyBillingStatus;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
public class BillingAdminOverviewController {

    private final BillingOverviewService service;

    public BillingAdminOverviewController(BillingOverviewService service) { this.service = service; }

    @GetMapping("/api/v1/admin/billing/companies")
    @PreAuthorize("isAuthenticated()")
    public List<CompanyBillingStatus> statuses(@AuthenticationPrincipal CurrentUser user) {
        if (user == null || !user.isCrmAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Só o administrador do CRM gerencia a cobrança.");
        }
        return service.statuses();
    }
}
```

(O `BillingAdminController` existente usa `@RequestMapping("/api/v1/admin/billing/companies/{companyId}")`; este caminho sem `{companyId}` não colide.)

- [ ] **Step 4: Compilar** — `cd backend && mvn -q -o -DskipTests test-compile`. Usuário roda: `cd backend && mvn test -Dtest="BillingOverviewServiceTest,BillingAdminOverviewControllerTest"` → PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/becommerce/crm/billing backend/src/test/java/com/becommerce/crm/billing
git commit -m "feat(cobranca): status de cobrança de todas as empresas para o CRM_ADMIN"
```

---

### Task 2: Feature `billing` — tipos, formatação, serviço e hooks

**Files:**
- Create: `frontend-refine/src/features/billing/types.ts`, `format.ts`, `service.ts`, `hooks.ts`
- Test: `frontend-refine/src/__tests__/features/billing/format.test.ts`

**Interfaces:**
- Produces (`types.ts`): `BillingStatus`, `InvoiceStatus`, `BillingView`, `InvoiceView`, `CompanyBillingStatus`, `BILLING_STATUS_LABELS`, `INVOICE_STATUS_LABELS`.
- Produces (`format.ts`): `formatCents(cents: number): string` → `"R$ 197,00"`; `formatLocalDate(date: string | null | undefined, style?: "short" | "long"): string` (`"2026-11-15"` → `"15/11/2026"`, `"short"` → `"15/11"`, nulo → `"—"`), sem `new Date(string)`.
- Produces (`service.ts`): `BillingService` (objeto) com `my()`, `myInvoices()`, `checkPayment()`, `adminView(id)`, `adminInvoices(id)`, `activate(id)`, `exempt(id)`, `changePrice(id, cents)`, `grant(id, days)`, `statuses()`.
- Produces (`hooks.ts`): `BILLING_KEYS`; `useMyBilling(enabled?: boolean)`, `useMyInvoices()`, `useCheckPayment()`, `useAdminBilling(companyId)`, `useAdminInvoices(companyId)`, `useActivateBilling(companyId)`, `useExemptBilling(companyId)`, `useChangeBillingPrice(companyId)`, `useGrantBilling(companyId)`, `useBillingStatuses(enabled: boolean)`.

- [ ] **Step 1: Teste de formatação**

```ts
import { describe, expect, it } from "vitest";
import { formatCents, formatLocalDate } from "@/features/billing/format";

describe("billing format", () => {
  it("formata centavos em reais", () => {
    expect(formatCents(19700).replace(/\s/g, " ")).toBe("R$ 197,00");
    expect(formatCents(4990).replace(/\s/g, " ")).toBe("R$ 49,90");
  });

  it("formata LocalDate sem deslocar o dia", () => {
    expect(formatLocalDate("2026-11-15")).toBe("15/11/2026");
    expect(formatLocalDate("2026-11-01", "short")).toBe("01/11");
    expect(formatLocalDate(null)).toBe("—");
  });
});
```

(`Intl` usa espaço não separável depois de `R$`; o `replace` normaliza.)

- [ ] **Step 2: Rodar e ver falhar** — `cd frontend-refine && npx vitest run src/__tests__/features/billing/format.test.ts` → FAIL.

- [ ] **Step 3: `types.ts`**

```ts
export type BillingStatus =
  | "NOT_STARTED"
  | "SYNC_PENDING"
  | "ACTIVE"
  | "PAST_DUE"
  | "BLOCKED"
  | "EXEMPT";

export type InvoiceStatus =
  | "PENDING"
  | "CONFIRMED"
  | "RECEIVED"
  | "OVERDUE"
  | "REFUNDED"
  | "CANCELLED";

export type BillingView = {
  status: BillingStatus;
  plan: string;
  monthlyPriceCents: number;
  nextDueDate: string | null;
  syncError: string | null;
  hasSubscription: boolean;
  overdueSince: string | null;
  graceUntil: string | null;
  openInvoiceUrl: string | null;
};

export type InvoiceView = {
  id: string;
  dueDate: string;
  valueCents: number;
  status: InvoiceStatus;
  invoiceUrl: string | null;
  paidAt: string | null;
};

export type CompanyBillingStatus = { companyId: string; status: BillingStatus | "UNKNOWN" };

export const BILLING_STATUS_LABELS: Record<BillingStatus | "UNKNOWN", string> = {
  NOT_STARTED: "Não iniciada",
  SYNC_PENDING: "Pendente de envio",
  ACTIVE: "Em dia",
  PAST_DUE: "Vencida",
  BLOCKED: "Bloqueada",
  EXEMPT: "Isenta",
  UNKNOWN: "—",
};

export const INVOICE_STATUS_LABELS: Record<InvoiceStatus, string> = {
  PENDING: "Aberta",
  CONFIRMED: "Confirmada",
  RECEIVED: "Paga",
  OVERDUE: "Vencida",
  REFUNDED: "Estornada",
  CANCELLED: "Cancelada",
};
```

- [ ] **Step 4: `format.ts`**

```ts
const BRL = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

export function formatCents(cents: number): string {
  return BRL.format(cents / 100);
}

/** "yyyy-MM-dd" → "dd/MM/yyyy" (ou "dd/MM"). Não usa Date: evita deslocar o dia pelo fuso. */
export function formatLocalDate(
  date: string | null | undefined,
  style: "short" | "long" = "long",
): string {
  if (!date) return "—";
  const [y, m, d] = date.slice(0, 10).split("-");
  if (!y || !m || !d) return "—";
  return style === "short" ? `${d}/${m}` : `${d}/${m}/${y}`;
}
```

- [ ] **Step 5: `service.ts`**

```ts
import api from "@/lib/api";
import type { BillingView, CompanyBillingStatus, InvoiceView } from "./types";

const admin = (companyId: string) => `/admin/billing/companies/${companyId}`;

export const BillingService = {
  my: async () => (await api.get<BillingView>("/billing")).data,
  myInvoices: async () => (await api.get<InvoiceView[]>("/billing/invoices")).data,
  checkPayment: async () => (await api.post<BillingView>("/billing/check-payment")).data,

  adminView: async (companyId: string) => (await api.get<BillingView>(admin(companyId))).data,
  adminInvoices: async (companyId: string) =>
    (await api.get<InvoiceView[]>(`${admin(companyId)}/invoices`)).data,
  activate: async (companyId: string) =>
    (await api.post<BillingView>(`${admin(companyId)}/activate`)).data,
  exempt: async (companyId: string) =>
    (await api.post<BillingView>(`${admin(companyId)}/exempt`)).data,
  changePrice: async (companyId: string, monthlyPriceCents: number) =>
    (await api.put<BillingView>(`${admin(companyId)}/price`, { monthlyPriceCents })).data,
  grant: async (companyId: string, days: number) =>
    (await api.post<BillingView>(`${admin(companyId)}/grant`, { days })).data,
  statuses: async () => (await api.get<CompanyBillingStatus[]>("/admin/billing/companies")).data,
};
```

- [ ] **Step 6: `hooks.ts`**

```ts
"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMutationDefaults } from "@/lib/query/mutation-utils";
import { BillingService } from "./service";
import type { BillingView } from "./types";

export const BILLING_KEYS = {
  my: ["billing", "my"] as const,
  myInvoices: ["billing", "my", "invoices"] as const,
  admin: (id: string) => ["billing", "admin", id] as const,
  adminInvoices: (id: string) => ["billing", "admin", id, "invoices"] as const,
  statuses: ["billing", "statuses"] as const,
};

export function useMyBilling(enabled = true) {
  return useQuery({
    queryKey: BILLING_KEYS.my,
    queryFn: BillingService.my,
    enabled,
    staleTime: 5 * 60 * 1000,
    retry: false,
  });
}

export function useMyInvoices() {
  return useQuery({ queryKey: BILLING_KEYS.myInvoices, queryFn: BillingService.myInvoices });
}

/** "Já paguei": consulta o Asaas na hora; se liberou, recarrega a aplicação no início. */
export function useCheckPayment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: BillingService.checkPayment,
    onSuccess: (view: BillingView) => {
      queryClient.setQueryData(BILLING_KEYS.my, view);
      queryClient.invalidateQueries({ queryKey: BILLING_KEYS.myInvoices });
    },
  });
}

export function useAdminBilling(companyId: string) {
  return useQuery({
    queryKey: BILLING_KEYS.admin(companyId),
    queryFn: () => BillingService.adminView(companyId),
    enabled: Boolean(companyId),
  });
}

export function useAdminInvoices(companyId: string) {
  return useQuery({
    queryKey: BILLING_KEYS.adminInvoices(companyId),
    queryFn: () => BillingService.adminInvoices(companyId),
    enabled: Boolean(companyId),
  });
}

function useAdminMutation<TVars>(
  companyId: string,
  fn: (vars: TVars) => Promise<BillingView>,
  successMessage: string,
  errorMessage: string,
) {
  const { onSuccess, onError } = useMutationDefaults<BillingView, TVars>();
  return useMutation({
    mutationFn: fn,
    onSuccess: onSuccess({
      successMessage,
      invalidateKeys: [
        [...BILLING_KEYS.admin(companyId)],
        [...BILLING_KEYS.statuses],
      ],
    }),
    onError: onError({ errorMessage }),
  });
}

export function useActivateBilling(companyId: string) {
  return useAdminMutation<void>(
    companyId,
    () => BillingService.activate(companyId),
    "Cobrança enviada ao Asaas",
    "Não foi possível ativar a cobrança",
  );
}

export function useExemptBilling(companyId: string) {
  return useAdminMutation<void>(
    companyId,
    () => BillingService.exempt(companyId),
    "Empresa marcada como isenta",
    "Não foi possível isentar a empresa",
  );
}

export function useChangeBillingPrice(companyId: string) {
  return useAdminMutation<number>(
    companyId,
    (cents) => BillingService.changePrice(companyId, cents),
    "Valor mensal atualizado",
    "Não foi possível alterar o valor",
  );
}

export function useGrantBilling(companyId: string) {
  return useAdminMutation<number>(
    companyId,
    (days) => BillingService.grant(companyId, days),
    "Empresa liberada",
    "Não foi possível liberar a empresa",
  );
}

export function useBillingStatuses(enabled: boolean) {
  return useQuery({
    queryKey: BILLING_KEYS.statuses,
    queryFn: BillingService.statuses,
    enabled,
    staleTime: 60 * 1000,
  });
}
```

Conferir a assinatura real de `useMutationDefaults` em `src/lib/query/mutation-utils.ts` (genéricos e o formato de `onSuccess({...})`/`onError({...})`) e ajustar a chamada se for diferente; o `useUpdateCompanyModules` em `features/identity/modules/hooks/useCompanyModules.ts` é o exemplo.

- [ ] **Step 7: Rodar** — `cd frontend-refine && npx vitest run src/__tests__/features/billing/format.test.ts && npm run typecheck` → PASS. `npx prettier --check --end-of-line auto src/features/billing src/__tests__/features/billing`.

- [ ] **Step 8: Commit**

```bash
git add frontend-refine/src/features/billing frontend-refine/src/__tests__/features/billing
git commit -m "feat(cobranca): feature de cobrança no frontend (tipos, serviço, hooks)"
```

---

### Task 3: 402 → `/pagamento-pendente`

**Files:**
- Modify: `frontend-refine/src/lib/api.ts`
- Modify: `frontend-refine/src/lib/constants.ts` (`PAYMENT_PENDING: "/pagamento-pendente"`, `SETTINGS_BILLING: "/settings/billing"`)
- Create: `frontend-refine/src/app/(billing)/layout.tsx`, `frontend-refine/src/app/(billing)/pagamento-pendente/page.tsx`
- Create: `frontend-refine/src/features/billing/components/PaymentPendingView.tsx`
- Test: `frontend-refine/src/__tests__/features/billing/api-402.test.ts`, `frontend-refine/src/__tests__/features/billing/PaymentPendingView.test.tsx`

**Interfaces:**
- Consumes: Task 2 (`useMyBilling`, `useCheckPayment`, `formatCents`, `formatLocalDate`).
- Produces: `export function isBillingBlocked(error: unknown): boolean` em `src/lib/api.ts`; `PaymentPendingView` (sem props).

Regras:
- Interceptador: resposta 402 com `data.code === "BILLING_BLOCKED"` → se `window.location.pathname` não começa com `/pagamento-pendente`, `window.location.assign("/pagamento-pendente")`; sempre rejeita a promise. Antes do tratamento do 401 existente (que fica igual).
- `(billing)/layout.tsx`: `ProtectedRoute` + container centralizado simples. **Não** usa `DashboardLayout` (que chama módulos/menu — 402 para empresa bloqueada).
- `PaymentPendingView`:
  - carregando → skeleton/texto "Carregando…";
  - status diferente de `BLOCKED` → mensagem "Sua empresa está liberada." e botão "Voltar ao sistema" (`window.location.assign("/")`);
  - `BLOCKED` → título "Pagamento pendente", texto "A mensalidade de {valor} está em atraso desde {dd/mm/aaaa}. Seus dados continuam guardados e voltam assim que o pagamento for confirmado.", botão "Pagar com Pix" (link `openInvoiceUrl`, `target="_blank" rel="noopener noreferrer"`, só se houver URL), botão "Já paguei" (chama `useCheckPayment`; se a resposta vier com status ≠ `BLOCKED` → `window.location.assign("/")`; se continuar `BLOCKED` → texto "Ainda não identificamos o pagamento. Pode levar alguns minutos depois do Pix."), e texto de ajuda "Dúvidas? Fale com o suporte do CRM.".

- [ ] **Step 1: Teste do interceptador (`api-402.test.ts`)**

```ts
import { afterEach, describe, expect, it, vi } from "vitest";
import api, { isBillingBlocked } from "@/lib/api";

describe("402 BILLING_BLOCKED", () => {
  const assign = vi.fn();

  afterEach(() => {
    assign.mockReset();
    vi.unstubAllGlobals();
  });

  function stubLocation(pathname: string) {
    vi.stubGlobal("location", { ...window.location, pathname, assign });
  }

  function blocked() {
    return { response: { status: 402, data: { code: "BILLING_BLOCKED" } }, config: {} };
  }

  // O interceptador de resposta registrado em lib/api.ts.
  const reject = (error: unknown) =>
    (api.interceptors.response as unknown as { handlers: { rejected: (e: unknown) => Promise<unknown> }[] })
      .handlers[0].rejected(error);

  it("reconhece o erro de bloqueio", () => {
    expect(isBillingBlocked(blocked())).toBe(true);
    expect(isBillingBlocked({ response: { status: 402, data: {} } })).toBe(false);
    expect(isBillingBlocked({ response: { status: 403, data: { code: "BILLING_BLOCKED" } } })).toBe(false);
  });

  it("manda para /pagamento-pendente", async () => {
    stubLocation("/crm");
    await expect(reject(blocked())).rejects.toBeTruthy();
    expect(assign).toHaveBeenCalledWith("/pagamento-pendente");
  });

  it("não redireciona se já está na página", async () => {
    stubLocation("/pagamento-pendente");
    await expect(reject(blocked())).rejects.toBeTruthy();
    expect(assign).not.toHaveBeenCalled();
  });
});
```

Se `window.location` não puder ser substituído com `vi.stubGlobal` no ambiente jsdom do projeto, usar `Object.defineProperty(window, "location", { value: {...}, writable: true })` e restaurar no `afterEach`.

- [ ] **Step 2: Teste da view (`PaymentPendingView.test.tsx`)**

```tsx
import { describe, expect, it, vi, beforeEach } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { PaymentPendingView } from "@/features/billing/components/PaymentPendingView";

const state = vi.hoisted(() => ({
  billing: null as null | Record<string, unknown>,
  mutate: vi.fn(),
}));

vi.mock("@/features/billing/hooks", () => ({
  useMyBilling: () => ({ data: state.billing, isLoading: false }),
  useCheckPayment: () => ({ mutate: state.mutate, isPending: false }),
}));

const blocked = {
  status: "BLOCKED",
  plan: "STARTER",
  monthlyPriceCents: 19700,
  nextDueDate: "2026-12-15",
  syncError: null,
  hasSubscription: true,
  overdueSince: "2026-11-15",
  graceUntil: null,
  openInvoiceUrl: "https://pix/1",
};

describe("PaymentPendingView", () => {
  beforeEach(() => {
    state.billing = blocked;
    state.mutate.mockReset();
  });

  it("mostra valor, atraso e o Pix", () => {
    render(<PaymentPendingView />);
    expect(screen.getByText("Pagamento pendente")).toBeTruthy();
    expect(screen.getByText(/197,00/)).toBeTruthy();
    expect(screen.getByText(/15\/11\/2026/)).toBeTruthy();
    expect(screen.getByRole("link", { name: /Pagar com Pix/ }).getAttribute("href")).toBe("https://pix/1");
  });

  it("Já paguei consulta o pagamento", () => {
    render(<PaymentPendingView />);
    fireEvent.click(screen.getByRole("button", { name: /Já paguei/ }));
    expect(state.mutate).toHaveBeenCalled();
  });

  it("empresa liberada vê o caminho de volta", () => {
    state.billing = { ...blocked, status: "ACTIVE" };
    render(<PaymentPendingView />);
    expect(screen.getByText("Sua empresa está liberada.")).toBeTruthy();
  });
});
```

- [ ] **Step 3: Rodar e ver falhar** — `cd frontend-refine && npx vitest run src/__tests__/features/billing` → FAIL.

- [ ] **Step 4: `api.ts`** — exportar o helper e tratar o 402 no início do `rejected`:

```ts
export function isBillingBlocked(error: unknown): boolean {
  const response = (error as { response?: { status?: number; data?: { code?: string } } })?.response;
  return response?.status === 402 && response.data?.code === "BILLING_BLOCKED";
}
```

```ts
  async (error) => {
    // 402 BILLING_BLOCKED: mensalidade em atraso — leva à tela de pagamento (sem loop).
    if (isBillingBlocked(error)) {
      if (typeof window !== "undefined" && !window.location.pathname.startsWith("/pagamento-pendente")) {
        window.location.assign("/pagamento-pendente");
      }
      return Promise.reject(error);
    }
    // 403 (CRM_ACCESS_DENIED/CSRF) NUNCA vira login — é decisão de autorização.
    if (error.response?.status !== 401) {
```

(`isBillingBlocked` é declarado acima do `api.interceptors.response.use`.)

- [ ] **Step 5: `constants.ts`** — dentro de `ROUTES`: `PAYMENT_PENDING: "/pagamento-pendente",` e `SETTINGS_BILLING: "/settings/billing",`.

- [ ] **Step 6: Layout e página**

`app/(billing)/layout.tsx`:
```tsx
"use client";

import { ProtectedRoute } from "@/components/layout/ProtectedRoute";

/** Rotas de cobrança fora do DashboardLayout: o layout chama endpoints que respondem 402 quando bloqueada. */
export default function BillingLayout({ children }: { children: React.ReactNode }) {
  return (
    <ProtectedRoute>
      <main className="flex min-h-dvh items-center justify-center bg-muted/30 p-4">{children}</main>
    </ProtectedRoute>
  );
}
```

`app/(billing)/pagamento-pendente/page.tsx`:
```tsx
"use client";

import { PaymentPendingView } from "@/features/billing/components/PaymentPendingView";

export default function PaymentPendingPage() {
  return <PaymentPendingView />;
}
```

Conferir que o `ProtectedRoute` (e o `AuthProvider`) só chama `/auth/me`/`/me/**` — se chamar outro endpoint que responda 402, a página entraria em loop de redirecionamento (o guard `startsWith("/pagamento-pendente")` impede o loop, mas a página não carregaria): nesse caso, anotar no relatório.

- [ ] **Step 7: `PaymentPendingView.tsx`**

```tsx
"use client";

import { useState } from "react";
import { Lock } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useCheckPayment, useMyBilling } from "../hooks";
import { formatCents, formatLocalDate } from "../format";

export function PaymentPendingView() {
  const { data, isLoading } = useMyBilling();
  const check = useCheckPayment();
  const [notFound, setNotFound] = useState(false);

  if (isLoading || !data) {
    return <p className="text-sm text-muted-foreground">Carregando…</p>;
  }

  if (data.status !== "BLOCKED") {
    return (
      <Card className="w-full max-w-md">
        <CardContent className="space-y-4 pt-6 text-center">
          <p>Sua empresa está liberada.</p>
          <Button onClick={() => window.location.assign("/")}>Voltar ao sistema</Button>
        </CardContent>
      </Card>
    );
  }

  const onCheck = () => {
    setNotFound(false);
    check.mutate(undefined, {
      onSuccess: (view) => {
        if (view.status !== "BLOCKED") window.location.assign("/");
        else setNotFound(true);
      },
    });
  };

  return (
    <Card className="w-full max-w-md">
      <CardHeader className="items-center text-center">
        <Lock className="h-10 w-10 text-amber-600" />
        <CardTitle>Pagamento pendente</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4 text-center">
        <p className="text-sm">
          A mensalidade de <strong>{formatCents(data.monthlyPriceCents)}</strong> está em atraso desde{" "}
          {formatLocalDate(data.overdueSince)}. Seus dados continuam guardados e voltam assim que o
          pagamento for confirmado.
        </p>
        <div className="flex flex-col gap-2">
          {data.openInvoiceUrl && (
            <Button asChild>
              <a href={data.openInvoiceUrl} target="_blank" rel="noopener noreferrer">
                Pagar com Pix
              </a>
            </Button>
          )}
          <Button variant="outline" onClick={onCheck} disabled={check.isPending}>
            {check.isPending ? "Verificando…" : "Já paguei"}
          </Button>
        </div>
        {notFound && (
          <p className="text-sm text-muted-foreground">
            Ainda não identificamos o pagamento. Pode levar alguns minutos depois do Pix.
          </p>
        )}
        <p className="text-xs text-muted-foreground">Dúvidas? Fale com o suporte do CRM.</p>
      </CardContent>
    </Card>
  );
}
```

(O teste mocka `mutate` sem chamar `onSuccess`; ele só verifica que o clique dispara a consulta.)

- [ ] **Step 8: Rodar** — `cd frontend-refine && npx vitest run src/__tests__/features/billing && npm run typecheck` → PASS; prettier `--check` nos arquivos alterados.

- [ ] **Step 9: Commit**

```bash
git add frontend-refine/src/lib/api.ts frontend-refine/src/lib/constants.ts "frontend-refine/src/app/(billing)" frontend-refine/src/features/billing frontend-refine/src/__tests__/features/billing
git commit -m "feat(cobranca): tela de pagamento pendente e redirecionamento no 402"
```

---

### Task 4: Faixa de mensalidade vencida

**Files:**
- Create: `frontend-refine/src/features/billing/components/PastDueBanner.tsx`
- Modify: `frontend-refine/src/components/layout/DashboardLayout.tsx` (faixa acima do `<main>`, abaixo do `<Header />`)
- Test: `frontend-refine/src/__tests__/features/billing/PastDueBanner.test.tsx`

**Interfaces:**
- Consumes: `useMyBilling`, `formatLocalDate`, `useAuthorization().can("company:update")`, `useAuthorization().isCrmAdmin`.

Regras:
- CRM_ADMIN não vê a faixa (a cobrança dele é a da plataforma) e não dispara a consulta: `useMyBilling(!isCrmAdmin)`.
- Só aparece com `status === "PAST_DUE"`.
- Texto: "Mensalidade venceu em {dd/mm}." usando `overdueSince` (formato `"short"`). Se `graceUntil` existir: acrescenta " Liberada até {dd/mm}.".
- Com `can("company:update")` e `openInvoiceUrl`: botão/link "Pagar com Pix" (`target="_blank" rel="noopener noreferrer"`). Sem a permissão: "Avise o administrador da clínica." no lugar do botão.
- Estilo de alerta igual ao `MailNotConfiguredNotice` (âmbar), `role="alert"`.

- [ ] **Step 1: Teste**

```tsx
import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { PastDueBanner } from "@/features/billing/components/PastDueBanner";

const state = vi.hoisted(() => ({
  billing: null as null | Record<string, unknown>,
  canUpdate: true,
  isCrmAdmin: false,
  enabledArg: undefined as undefined | boolean,
}));

vi.mock("@/features/billing/hooks", () => ({
  useMyBilling: (enabled: boolean) => {
    state.enabledArg = enabled;
    return { data: enabled ? state.billing : undefined };
  },
}));

vi.mock("@/features/identity/auth/hooks/useAuthorization", () => ({
  useAuthorization: () => ({ can: () => state.canUpdate, isCrmAdmin: state.isCrmAdmin }),
}));

const pastDue = {
  status: "PAST_DUE",
  plan: "STARTER",
  monthlyPriceCents: 19700,
  nextDueDate: "2026-12-15",
  syncError: null,
  hasSubscription: true,
  overdueSince: "2026-11-15",
  graceUntil: null,
  openInvoiceUrl: "https://pix/1",
};

describe("PastDueBanner", () => {
  beforeEach(() => {
    state.billing = pastDue;
    state.canUpdate = true;
    state.isCrmAdmin = false;
  });

  it("admin da clínica vê o aviso e o Pix", () => {
    render(<PastDueBanner />);
    expect(screen.getByRole("alert").textContent).toContain("Mensalidade venceu em 15/11.");
    expect(screen.getByRole("link", { name: /Pagar com Pix/ }).getAttribute("href")).toBe("https://pix/1");
  });

  it("sem permissão vê só o aviso", () => {
    state.canUpdate = false;
    render(<PastDueBanner />);
    expect(screen.queryByRole("link", { name: /Pagar com Pix/ })).toBeNull();
    expect(screen.getByText(/Avise o administrador da clínica/)).toBeTruthy();
  });

  it("em dia não mostra nada", () => {
    state.billing = { ...pastDue, status: "ACTIVE" };
    const { container } = render(<PastDueBanner />);
    expect(container.innerHTML).toBe("");
  });

  it("CRM_ADMIN não consulta nem vê", () => {
    state.isCrmAdmin = true;
    const { container } = render(<PastDueBanner />);
    expect(state.enabledArg).toBe(false);
    expect(container.innerHTML).toBe("");
  });

  it("mostra até quando foi liberada", () => {
    state.billing = { ...pastDue, graceUntil: "2026-11-30" };
    render(<PastDueBanner />);
    expect(screen.getByRole("alert").textContent).toContain("Liberada até 30/11.");
  });
});
```

- [ ] **Step 2: Rodar e ver falhar** — `npx vitest run src/__tests__/features/billing/PastDueBanner.test.tsx` → FAIL.

- [ ] **Step 3: Componente**

```tsx
"use client";

import { AlertTriangle } from "lucide-react";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";
import { useMyBilling } from "../hooks";
import { formatLocalDate } from "../format";

/** Aviso de mensalidade vencida (uso normal). Só quem administra a clínica vê o botão de Pix. */
export function PastDueBanner() {
  const { can, isCrmAdmin } = useAuthorization();
  const { data } = useMyBilling(!isCrmAdmin);

  if (isCrmAdmin || !data || data.status !== "PAST_DUE") return null;

  return (
    <div
      role="alert"
      className="flex flex-wrap items-center gap-3 border-b border-amber-300 bg-amber-50 px-4 py-2 text-sm text-amber-900 dark:border-amber-700 dark:bg-amber-950 dark:text-amber-100"
    >
      <AlertTriangle className="h-4 w-4 shrink-0" />
      <p className="flex-1">
        Mensalidade venceu em {formatLocalDate(data.overdueSince, "short")}.
        {data.graceUntil && <> Liberada até {formatLocalDate(data.graceUntil, "short")}.</>}
      </p>
      {can("company:update") ? (
        data.openInvoiceUrl && (
          <a
            href={data.openInvoiceUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="rounded-md bg-amber-600 px-3 py-1 font-medium text-white hover:bg-amber-700"
          >
            Pagar com Pix
          </a>
        )
      ) : (
        <span>Avise o administrador da clínica.</span>
      )}
    </div>
  );
}
```

- [ ] **Step 4: `DashboardLayout.tsx`** — importar `PastDueBanner` e renderizar entre `<Header />` e `<main ...>`:

```tsx
        <Header />
        <PastDueBanner />
        <main className="flex-1 overflow-y-auto p-4 lg:p-6">
```

Conferir se algum teste existente renderiza `DashboardLayout` sem `QueryClientProvider` (grep `DashboardLayout` em `src/__tests__`); se houver, mockar `@/features/billing/components/PastDueBanner` nesse teste.

- [ ] **Step 5: Rodar** — `npx vitest run src/__tests__/features/billing && npm run typecheck`; prettier `--check`.

- [ ] **Step 6: Commit**

```bash
git add frontend-refine/src/features/billing frontend-refine/src/components/layout/DashboardLayout.tsx frontend-refine/src/__tests__
git commit -m "feat(cobranca): faixa de mensalidade vencida"
```

---

### Task 5: Configurações → "Plano e cobrança"

**Files:**
- Create: `frontend-refine/src/features/billing/components/BillingStatusBadge.tsx`
- Create: `frontend-refine/src/features/billing/components/InvoiceTable.tsx`
- Create: `frontend-refine/src/features/billing/components/BillingSettingsView.tsx`
- Create: `frontend-refine/src/app/(dashboard)/settings/billing/page.tsx`
- Modify: `frontend-refine/src/components/layout/navigation.ts` (item "Plano e cobrança" logo depois de "E-mail", `href: ROUTES.SETTINGS_BILLING`, `icon: CreditCard`, `permission: "company:update"`)
- Test: `frontend-refine/src/__tests__/features/billing/BillingSettingsView.test.tsx`

**Interfaces:**
- Consumes: `useMyBilling`, `useMyInvoices`, `formatCents`, `formatLocalDate`, rótulos de `types.ts`.
- Produces: `BillingStatusBadge({ status })`; `InvoiceTable({ invoices, isLoading })` (colunas Vencimento, Valor, Situação, Pix — link "Abrir" só para fatura `PENDING`/`OVERDUE` com `invoiceUrl`; vazio → "Nenhuma fatura ainda."); `BillingSettingsView` (sem props).

Regras de `BillingSettingsView`:
- Cartão "Plano e cobrança": Plano (nome do plano com a primeira letra maiúscula, ex. `STARTER` → "Starter"), Valor mensal, Situação (`BillingStatusBadge`), Próximo vencimento.
- `NOT_STARTED` ou `EXEMPT`: mensagem "A cobrança automática não está ativa para esta empresa." e sem histórico.
- `PAST_DUE`/`BLOCKED` com `openInvoiceUrl`: botão "Pagar com Pix".
- Cartão "Histórico" com `InvoiceTable`.
- Página `settings/billing/page.tsx`: `PageTitle` "Plano e cobrança" + `BillingSettingsView`.

`BillingStatusBadge` cores: `ACTIVE` verde, `PAST_DUE` âmbar, `BLOCKED` vermelho, `EXEMPT` azul, demais cinza — usar `Badge` de `@/components/ui/badge` com `className` (seguir `TenantStatusBadge` como referência de estilo).

- [ ] **Step 1: Teste**

```tsx
import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { BillingSettingsView } from "@/features/billing/components/BillingSettingsView";

const state = vi.hoisted(() => ({
  billing: null as null | Record<string, unknown>,
  invoices: [] as Record<string, unknown>[],
}));

vi.mock("@/features/billing/hooks", () => ({
  useMyBilling: () => ({ data: state.billing, isLoading: false }),
  useMyInvoices: () => ({ data: state.invoices, isLoading: false }),
}));

const active = {
  status: "ACTIVE",
  plan: "STARTER",
  monthlyPriceCents: 19700,
  nextDueDate: "2026-12-15",
  syncError: null,
  hasSubscription: true,
  overdueSince: null,
  graceUntil: null,
  openInvoiceUrl: null,
};

describe("BillingSettingsView", () => {
  beforeEach(() => {
    state.billing = active;
    state.invoices = [
      { id: "i1", dueDate: "2026-11-15", valueCents: 19700, status: "RECEIVED", invoiceUrl: "https://pix/1", paidAt: "2026-11-14T10:00:00" },
      { id: "i2", dueDate: "2026-12-15", valueCents: 19700, status: "PENDING", invoiceUrl: "https://pix/2", paidAt: null },
    ];
  });

  it("mostra plano, valor, situação e vencimento", () => {
    render(<BillingSettingsView />);
    expect(screen.getByText("Starter")).toBeTruthy();
    expect(screen.getAllByText(/197,00/).length).toBeGreaterThan(0);
    expect(screen.getByText("Em dia")).toBeTruthy();
    expect(screen.getAllByText("15/12/2026").length).toBeGreaterThan(0);
  });

  it("histórico com link só na fatura aberta", () => {
    render(<BillingSettingsView />);
    expect(screen.getByText("Paga")).toBeTruthy();
    const links = screen.getAllByRole("link", { name: "Abrir" });
    expect(links).toHaveLength(1);
    expect(links[0].getAttribute("href")).toBe("https://pix/2");
  });

  it("sem cobrança ativa", () => {
    state.billing = { ...active, status: "NOT_STARTED", hasSubscription: false };
    render(<BillingSettingsView />);
    expect(screen.getByText("A cobrança automática não está ativa para esta empresa.")).toBeTruthy();
  });
});
```

- [ ] **Step 2: Rodar e ver falhar.**

- [ ] **Step 3: Componentes**

`BillingStatusBadge.tsx`:
```tsx
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { BILLING_STATUS_LABELS, type BillingStatus } from "../types";

const COLORS: Partial<Record<BillingStatus | "UNKNOWN", string>> = {
  ACTIVE: "bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-200",
  PAST_DUE: "bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-200",
  BLOCKED: "bg-red-100 text-red-800 dark:bg-red-950 dark:text-red-200",
  EXEMPT: "bg-sky-100 text-sky-800 dark:bg-sky-950 dark:text-sky-200",
};

export function BillingStatusBadge({ status }: { status: BillingStatus | "UNKNOWN" }) {
  return (
    <Badge variant="secondary" className={cn("font-medium", COLORS[status])}>
      {BILLING_STATUS_LABELS[status]}
    </Badge>
  );
}
```

`InvoiceTable.tsx`:
```tsx
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { formatCents, formatLocalDate } from "../format";
import { INVOICE_STATUS_LABELS, type InvoiceView } from "../types";

export function InvoiceTable({ invoices, isLoading }: { invoices?: InvoiceView[]; isLoading?: boolean }) {
  if (isLoading) return <p className="text-sm text-muted-foreground">Carregando…</p>;
  if (!invoices || invoices.length === 0) {
    return <p className="text-sm text-muted-foreground">Nenhuma fatura ainda.</p>;
  }
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Vencimento</TableHead>
          <TableHead>Valor</TableHead>
          <TableHead>Situação</TableHead>
          <TableHead className="text-right">Pix</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {invoices.map((i) => {
          const open = i.status === "PENDING" || i.status === "OVERDUE";
          return (
            <TableRow key={i.id}>
              <TableCell>{formatLocalDate(i.dueDate)}</TableCell>
              <TableCell>{formatCents(i.valueCents)}</TableCell>
              <TableCell>{INVOICE_STATUS_LABELS[i.status]}</TableCell>
              <TableCell className="text-right">
                {open && i.invoiceUrl ? (
                  <a href={i.invoiceUrl} target="_blank" rel="noopener noreferrer" className="text-primary underline">
                    Abrir
                  </a>
                ) : (
                  "—"
                )}
              </TableCell>
            </TableRow>
          );
        })}
      </TableBody>
    </Table>
  );
}
```

`BillingSettingsView.tsx`:
```tsx
"use client";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useMyBilling, useMyInvoices } from "../hooks";
import { formatCents, formatLocalDate } from "../format";
import { BillingStatusBadge } from "./BillingStatusBadge";
import { InvoiceTable } from "./InvoiceTable";

export function planLabel(plan: string): string {
  return plan.charAt(0) + plan.slice(1).toLowerCase();
}

export function BillingSettingsView() {
  const { data, isLoading } = useMyBilling();
  const invoices = useMyInvoices();

  if (isLoading || !data) return <p className="text-sm text-muted-foreground">Carregando…</p>;

  const inactive = data.status === "NOT_STARTED" || data.status === "EXEMPT";
  const late = data.status === "PAST_DUE" || data.status === "BLOCKED";

  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Plano e cobrança</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3 text-sm">
          <Row label="Plano" value={planLabel(data.plan)} />
          <Row label="Valor mensal" value={formatCents(data.monthlyPriceCents)} />
          <div className="flex items-center justify-between">
            <span className="text-muted-foreground">Situação</span>
            <BillingStatusBadge status={data.status} />
          </div>
          <Row label="Próximo vencimento" value={formatLocalDate(data.nextDueDate)} />
          {inactive && (
            <p className="text-muted-foreground">A cobrança automática não está ativa para esta empresa.</p>
          )}
          {late && data.openInvoiceUrl && (
            <Button asChild>
              <a href={data.openInvoiceUrl} target="_blank" rel="noopener noreferrer">
                Pagar com Pix
              </a>
            </Button>
          )}
        </CardContent>
      </Card>
      {!inactive && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Histórico</CardTitle>
          </CardHeader>
          <CardContent>
            <InvoiceTable invoices={invoices.data} isLoading={invoices.isLoading} />
          </CardContent>
        </Card>
      )}
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between">
      <span className="text-muted-foreground">{label}</span>
      <span className="font-medium">{value}</span>
    </div>
  );
}
```

`app/(dashboard)/settings/billing/page.tsx`:
```tsx
"use client";

import { PageTitle } from "@/components/common/PageTitle";
import { BillingSettingsView } from "@/features/billing/components/BillingSettingsView";

export default function BillingSettingsPage() {
  return (
    <div className="space-y-6">
      <PageTitle>Plano e cobrança</PageTitle>
      <BillingSettingsView />
    </div>
  );
}
```

`navigation.ts`: importar `CreditCard` de `lucide-react` e acrescentar depois do item "E-mail":
```ts
      {
        label: "Plano e cobrança",
        href: ROUTES.SETTINGS_BILLING,
        icon: CreditCard,
        permission: "company:update",
      },
```

Se existir teste de snapshot/contagem dos itens de navegação (grep `navigation` em `src/__tests__`), atualizar.

- [ ] **Step 4: Rodar** — `npx vitest run src/__tests__/features/billing && npm run typecheck`; prettier `--check`.

- [ ] **Step 5: Commit**

```bash
git add frontend-refine/src/features/billing "frontend-refine/src/app/(dashboard)/settings/billing" frontend-refine/src/components/layout/navigation.ts frontend-refine/src/__tests__
git commit -m "feat(cobranca): Plano e cobrança nas configurações"
```

---

### Task 6: Cartão "Cobrança" do CRM_ADMIN e coluna na lista de empresas

**Files:**
- Create: `frontend-refine/src/features/billing/components/AdminBillingCard.tsx`
- Modify: `frontend-refine/src/features/identity/tenants/components/TenantDetails.tsx` (cartão no grid, só para `isCrmAdmin`)
- Modify: `frontend-refine/src/features/identity/tenants/components/TenantTable.tsx` (coluna "Cobrança" depois de "Status", só para `isCrmAdmin`)
- Test: `frontend-refine/src/__tests__/features/billing/AdminBillingCard.test.tsx`

**Interfaces:**
- Consumes: `useAdminBilling`, `useAdminInvoices`, `useActivateBilling`, `useExemptBilling`, `useChangeBillingPrice`, `useGrantBilling`, `useBillingStatuses`, `BillingStatusBadge`, `InvoiceTable`, `planLabel`, `formatCents`, `formatLocalDate`.
- Produces: `AdminBillingCard({ companyId }: { companyId: string })`.

Regras do cartão:
- Linhas: Situação (badge), Valor mensal (com botão "Editar" → input em reais `type="number" step="0.01" min="0.01"` + "Salvar"/"Cancelar"; salva `Math.round(valor * 100)`; valor ≤ 0 desabilita "Salvar"), Próximo vencimento, "Atrasada desde" (se `overdueSince`), "Liberada até" (se `graceUntil`), erro de sincronização (se `syncError`, em texto vermelho pequeno).
- Botões:
  - "Ativar cobrança" quando `status === "NOT_STARTED"`.
  - "Tentar novamente" quando `status === "SYNC_PENDING"` (mesma mutation `activate`).
  - "Marcar como isenta" quando `status !== "EXEMPT"` e `!hasSubscription` (o backend recusa isentar com assinatura ativa); confirmação com `AlertDialog` ("A empresa deixa de ser cobrada e nunca é bloqueada.").
  - "Liberar por X dias" quando `status === "PAST_DUE"` ou `"BLOCKED"`: input numérico 1–30 (padrão 7) + botão "Liberar".
- Cartão "Histórico" (dentro do mesmo cartão, abaixo) com `InvoiceTable` de `useAdminInvoices`.
- Erros das mutations aparecem pelo toast padrão (`useMutationDefaults`), que já usa `response.data.message`.

Coluna da lista: `useBillingStatuses(isCrmAdmin)`; mapa `companyId → status`; célula com `BillingStatusBadge` (sem dado → "—").

- [ ] **Step 1: Teste**

```tsx
import { describe, expect, it, vi, beforeEach } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { AdminBillingCard } from "@/features/billing/components/AdminBillingCard";

const state = vi.hoisted(() => ({
  billing: null as null | Record<string, unknown>,
  activate: vi.fn(),
  exempt: vi.fn(),
  price: vi.fn(),
  grant: vi.fn(),
}));

vi.mock("@/features/billing/hooks", () => ({
  useAdminBilling: () => ({ data: state.billing, isLoading: false }),
  useAdminInvoices: () => ({ data: [], isLoading: false }),
  useActivateBilling: () => ({ mutate: state.activate, isPending: false }),
  useExemptBilling: () => ({ mutate: state.exempt, isPending: false }),
  useChangeBillingPrice: () => ({ mutate: state.price, isPending: false }),
  useGrantBilling: () => ({ mutate: state.grant, isPending: false }),
}));

const base = {
  status: "NOT_STARTED",
  plan: "STARTER",
  monthlyPriceCents: 19700,
  nextDueDate: null,
  syncError: null,
  hasSubscription: false,
  overdueSince: null,
  graceUntil: null,
  openInvoiceUrl: null,
};

describe("AdminBillingCard", () => {
  beforeEach(() => {
    state.billing = base;
    Object.values(state).forEach((v) => typeof v === "function" && (v as ReturnType<typeof vi.fn>).mockReset());
  });

  it("ativa a cobrança", () => {
    render(<AdminBillingCard companyId="co1" />);
    fireEvent.click(screen.getByRole("button", { name: "Ativar cobrança" }));
    expect(state.activate).toHaveBeenCalled();
  });

  it("tentar novamente com erro de sincronização", () => {
    state.billing = { ...base, status: "SYNC_PENDING", syncError: "Asaas indisponível." };
    render(<AdminBillingCard companyId="co1" />);
    expect(screen.getByText("Asaas indisponível.")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Tentar novamente" }));
    expect(state.activate).toHaveBeenCalled();
  });

  it("muda o valor em reais e envia centavos", () => {
    state.billing = { ...base, status: "ACTIVE", hasSubscription: true };
    render(<AdminBillingCard companyId="co1" />);
    fireEvent.click(screen.getByRole("button", { name: "Editar" }));
    fireEvent.change(screen.getByLabelText("Valor mensal (R$)"), { target: { value: "170.50" } });
    fireEvent.click(screen.getByRole("button", { name: "Salvar" }));
    expect(state.price).toHaveBeenCalledWith(17050, expect.anything());
  });

  it("libera por X dias quando bloqueada", () => {
    state.billing = { ...base, status: "BLOCKED", hasSubscription: true, overdueSince: "2026-11-01" };
    render(<AdminBillingCard companyId="co1" />);
    fireEvent.change(screen.getByLabelText("Dias"), { target: { value: "10" } });
    fireEvent.click(screen.getByRole("button", { name: "Liberar" }));
    expect(state.grant).toHaveBeenCalledWith(10);
  });

  it("não oferece isentar com assinatura ativa", () => {
    state.billing = { ...base, status: "ACTIVE", hasSubscription: true };
    render(<AdminBillingCard companyId="co1" />);
    expect(screen.queryByRole("button", { name: "Marcar como isenta" })).toBeNull();
  });
});
```

- [ ] **Step 2: Rodar e ver falhar.**

- [ ] **Step 3: `AdminBillingCard.tsx`**

```tsx
"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import {
  useActivateBilling,
  useAdminBilling,
  useAdminInvoices,
  useChangeBillingPrice,
  useExemptBilling,
  useGrantBilling,
} from "../hooks";
import { formatCents, formatLocalDate } from "../format";
import { BillingStatusBadge } from "./BillingStatusBadge";
import { InvoiceTable } from "./InvoiceTable";
import { planLabel } from "./BillingSettingsView";

export function AdminBillingCard({ companyId }: { companyId: string }) {
  const { data, isLoading } = useAdminBilling(companyId);
  const invoices = useAdminInvoices(companyId);
  const activate = useActivateBilling(companyId);
  const exempt = useExemptBilling(companyId);
  const price = useChangeBillingPrice(companyId);
  const grant = useGrantBilling(companyId);
  const [editing, setEditing] = useState(false);
  const [reais, setReais] = useState("");
  const [days, setDays] = useState("7");

  if (isLoading || !data) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Cobrança</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">Carregando…</CardContent>
      </Card>
    );
  }

  const cents = Math.round(Number(reais.replace(",", ".")) * 100);
  const daysNumber = Number(days);
  const validDays = Number.isInteger(daysNumber) && daysNumber >= 1 && daysNumber <= 30;
  const late = data.status === "PAST_DUE" || data.status === "BLOCKED";

  return (
    <Card className="lg:col-span-2">
      <CardHeader>
        <CardTitle className="text-base">Cobrança</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4 text-sm">
        <div className="grid gap-3 sm:grid-cols-2">
          <div className="flex items-center justify-between">
            <span className="text-muted-foreground">Situação</span>
            <BillingStatusBadge status={data.status} />
          </div>
          <Info label="Plano" value={planLabel(data.plan)} />
          <div className="flex items-center justify-between gap-2">
            <span className="text-muted-foreground">Valor mensal</span>
            {editing ? (
              <div className="flex items-center gap-2">
                <Label htmlFor="billing-price" className="sr-only">
                  Valor mensal (R$)
                </Label>
                <Input
                  id="billing-price"
                  aria-label="Valor mensal (R$)"
                  type="number"
                  step="0.01"
                  min="0.01"
                  className="h-8 w-28"
                  value={reais}
                  onChange={(e) => setReais(e.target.value)}
                />
                <Button
                  size="sm"
                  disabled={!(cents > 0) || price.isPending}
                  onClick={() => price.mutate(cents, { onSuccess: () => setEditing(false) })}
                >
                  Salvar
                </Button>
                <Button size="sm" variant="ghost" onClick={() => setEditing(false)}>
                  Cancelar
                </Button>
              </div>
            ) : (
              <div className="flex items-center gap-2">
                <span className="font-medium">{formatCents(data.monthlyPriceCents)}</span>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => {
                    setReais((data.monthlyPriceCents / 100).toFixed(2));
                    setEditing(true);
                  }}
                >
                  Editar
                </Button>
              </div>
            )}
          </div>
          <Info label="Próximo vencimento" value={formatLocalDate(data.nextDueDate)} />
          {data.overdueSince && <Info label="Atrasada desde" value={formatLocalDate(data.overdueSince)} />}
          {data.graceUntil && <Info label="Liberada até" value={formatLocalDate(data.graceUntil)} />}
        </div>
        {data.syncError && <p className="text-xs text-red-600">{data.syncError}</p>}

        <div className="flex flex-wrap items-end gap-2">
          {data.status === "NOT_STARTED" && (
            <Button onClick={() => activate.mutate()} disabled={activate.isPending}>
              Ativar cobrança
            </Button>
          )}
          {data.status === "SYNC_PENDING" && (
            <Button onClick={() => activate.mutate()} disabled={activate.isPending}>
              Tentar novamente
            </Button>
          )}
          {data.status !== "EXEMPT" && !data.hasSubscription && (
            <AlertDialog>
              <AlertDialogTrigger asChild>
                <Button variant="outline">Marcar como isenta</Button>
              </AlertDialogTrigger>
              <AlertDialogContent>
                <AlertDialogHeader>
                  <AlertDialogTitle>Marcar como isenta?</AlertDialogTitle>
                  <AlertDialogDescription>
                    A empresa deixa de ser cobrada e nunca é bloqueada.
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel>Cancelar</AlertDialogCancel>
                  <AlertDialogAction onClick={() => exempt.mutate()}>Isentar</AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>
          )}
          {late && (
            <div className="flex items-end gap-2">
              <div className="space-y-1">
                <Label htmlFor="billing-grant-days">Dias</Label>
                <Input
                  id="billing-grant-days"
                  type="number"
                  min={1}
                  max={30}
                  className="h-9 w-20"
                  value={days}
                  onChange={(e) => setDays(e.target.value)}
                />
              </div>
              <Button
                variant="outline"
                disabled={!validDays || grant.isPending}
                onClick={() => grant.mutate(daysNumber)}
              >
                Liberar
              </Button>
            </div>
          )}
        </div>

        <Separator />
        <p className="font-medium">Histórico</p>
        <InvoiceTable invoices={invoices.data} isLoading={invoices.isLoading} />
      </CardContent>
    </Card>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between">
      <span className="text-muted-foreground">{label}</span>
      <span className="font-medium">{value}</span>
    </div>
  );
}
```

(O botão "Liberar" abre o rótulo "Liberar por X dias" da spec com o campo "Dias" ao lado; o teste usa `getByLabelText("Dias")`.)

- [ ] **Step 4: `TenantDetails.tsx`** — importar `useAuthorization` e `AdminBillingCard`; dentro do grid `lg:grid-cols-2`, ao final:

```tsx
        {isCrmAdmin && <AdminBillingCard companyId={tenant.id} />}
```

com `const { isCrmAdmin } = useAuthorization();` no topo do componente.

- [ ] **Step 5: `TenantTable.tsx`** — `const { isCrmAdmin } = useAuthorization();` (o hook já é importado no arquivo; reaproveitar a desestruturação existente se houver), `const statuses = useBillingStatuses(isCrmAdmin);`, `const billingByCompany = useMemo(() => new Map((statuses.data ?? []).map((s) => [s.companyId, s.status])), [statuses.data]);`. Cabeçalho `{isCrmAdmin && <TableHead>Cobrança</TableHead>}` depois de "Status"; célula correspondente:

```tsx
                {isCrmAdmin && (
                  <TableCell>
                    {billingByCompany.get(tenant.id) ? (
                      <BillingStatusBadge status={billingByCompany.get(tenant.id)!} />
                    ) : (
                      "—"
                    )}
                  </TableCell>
                )}
```

Se houver teste de `TenantTable`/`TenantDetails` em `src/__tests__`, mockar `@/features/billing/hooks` (e `AdminBillingCard`) nesses testes.

- [ ] **Step 6: Rodar** — `cd frontend-refine && npx vitest run src/__tests__/features/billing src/__tests__/features/identity && npm run typecheck`; prettier `--check` nos alterados.

- [ ] **Step 7: Commit**

```bash
git add frontend-refine/src/features/billing frontend-refine/src/features/identity/tenants/components frontend-refine/src/__tests__
git commit -m "feat(cobranca): cartão de cobrança e coluna na lista de empresas (CRM_ADMIN)"
```

---

### Task 7: Verificação da entrega

- [ ] **Step 1:** `cd frontend-refine && npm run typecheck && npm run lint && npx vitest run` → tudo verde (rodado pelo agente).
- [ ] **Step 2:** `cd frontend-refine && npx prettier --check --end-of-line auto $(git diff --name-only origin/feature/cobranca-asaas-entrega-2 -- 'src/**' | sed 's#^frontend-refine/##')` → sem diferenças.
- [ ] **Step 3:** Usuário roda o backend: `cd backend && mvn test -Dtest="BillingOverviewServiceTest,BillingAdminOverviewControllerTest"`.
- [ ] **Step 4 (manual, antes da entrega 4):** com o backend local e uma conta forçada a `BLOCKED` no banco, navegar no sistema → cai em `/pagamento-pendente`; com `PAST_DUE` → faixa no topo; CRM_ADMIN → cartão no detalhe da empresa e coluna na lista.
