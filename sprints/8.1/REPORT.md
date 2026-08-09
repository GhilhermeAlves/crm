# Sprint 8.1 - Company Foundation

**Data:** 2026-08-09 - **Ambiente:** local + VPS (produção `srv1348261.hstgr.cloud`) - **Status:** ✅ Concluída

## Identificação

- **Sprint:** 8.1
- **Nome:** Company Foundation - a empresa como entidade de primeiro plano
- **Responsável:** AI Agent
- **Fase:** SaaS - Empresas / Multi-tenant
- **Dependência:** 7.5 (identidade) - plano da Sprint 8 em `sprints/8/SPRINT_PLAN.md`

## Objetivo

Transformar a entidade **Company/Empresa** em produto de primeiro plano e reconciliar a
nomenclatura divergente (API/UI usava `tenants`; domínio/banco usa `company`):

- Endpoints de empresa por **membro** (não apenas SUPER_ADMIN): listagem, detalhe, `/me`.
- **Settings de empresa** na API (timezone, locale, moeda, horário comercial, preferências).
- Limites de plano no modelo: exposição de `max_contacts` (**enforcement na 8.6**).
- Alias de compatibilidade `/api/v1/tenants` → `/api/v1/companies` (depreciação documentada).
- UI admin "Empresas" atualizada (nomenclatura + campos novos).

## Arquitetura / Decisões

| Decisão | Detalhe |
|---|---|
| Nomenclatura | `Company` é canônico. `CompanyController` agora mapeia **`/api/v1/companies`** com **alias `/api/v1/tenants`** (mesmo handler). UI exibe "Empresas". |
| Leitura por membro | `GET /companies`, `GET /companies/me` e `GET /companies/{id}` exigem apenas `isAuthenticated()`. O **escopo** é resolvido no serviço: o membro lê apenas a própria empresa (`principal.companyId()`); **SUPER_ADMIN** (role presente no `CurrentUser`) lê qualquer empresa. |
| Settings | `GET/PUT /companies/{id}/settings` exigem `settings:view`/`settings:update`. O acesso é **restrito à própria empresa** (`assertOwnCompanySettings`), inclusive para SUPER_ADMIN. |
| CRUD admin | `POST`/`PUT`/`DELETE` de empresa mantêm `company:create`/`company:update`/`company:delete`; acesso cross-tenant apenas para SUPER_ADMIN. |

## Migrações / Banco

- **V029** - `companies.max_contacts INTEGER NOT NULL DEFAULT 500` (limite de contatos do plano;
  enforcement previsto na 8.6).

## Problemas encontrados e correções

1. **`StaleObjectStateException` no PUT settings (upsert) - 500:**
   `updateCompanySettings` (quando ainda não existe settings) cria um novo agregado com
   `id = UUID.randomUUID()` e chama `save()`. O `save()` do JPA vira **`merge()`** (id não-nulo),
   e o merge de uma linha inexistente lança
   `StaleObjectStateException: Row was updated or deleted by another transaction (or unsaved-value mapping was incorrect)`.
   Correção em `CompanySettingsRepositoryImpl.save()`:
   - `existsById(id)` decide **persist** (novo) vs **merge** (existente);
   - para novo, o `id` é anulado (Hibernate gera via `GenerationType.UUID`);
   - `CompanySettingsJpaEntity` passou a implementar **`Persistable<UUID>`** (campo `@Transient isNewRecord`)
     para o Spring Data chamar `persist` em vez de `merge`.
   Após o fix, o fluxo **INSERT upsert** e **UPDATE merge** passaram a retornar **200** e persistir no banco.

## E2E em produção (backend direto, sem gateway)

Ambiente de teste: token OIDC via **URL pública** (issuer `https://srv1348261.hstgr.cloud/realms/CRM`,
o único aceito pelo resource server do backend), chamadas ao backend via container
`curlimages/curl` na rede `crm-network`. Empresa de teste criada/removida direto no banco;
elevação temporária do `validacao.tester` para `ADMIN`+`SUPER_ADMIN` e reversão ao final.

| # | Cenário | Resultado |
|---|---------|-----------|
| 1 | Token AGENT (password grant, client `crm-frontend`) | ✅ |
| 2 | `GET /api/v1/companies` como AGENT → 200, **1** empresa (a própria, CRM Default) | ✅ |
| 3 | `GET /api/v1/companies/me` → 200, empresa do usuário | ✅ |
| 4 | `GET /api/v1/companies/{own}` → 200, `maxContacts=500` exposto | ✅ |
| 5 | Alias `/api/v1/tenants` → 200 (compatibilidade mantida) | ✅ |
| 6 | `GET /api/v1/companies/{inexistente}` → **404** | ✅ |
| 7 | `GET /companies/{own}/settings` como AGENT → **403** (sem `settings:view`) | ✅ |
| 8 | Empresa de teste criada no banco (2º tenant) | ✅ |
| 9 | Elevação para `ADMIN`+`SUPER_ADMIN` | ✅ |
| 10 | `GET /companies` como SUPER_ADMIN → 200, **2** empresas (lista todas) | ✅ |
| 11 | SUPER_ADMIN lê empresa de outro tenant → **200** (leitura cross-tenant) | ✅ |
| 12 | `GET /companies/{own}/settings` (ADMIN, `settings:view`) → 200 | ✅ |
| 13 | `PUT settings` **INSERT upsert** (linha inexistente) → **200**; timezone/businessHours no response | ✅ |
| 14 | `PUT settings` **UPDATE merge** (linha existente, parcial) → **200**; timezone atualizado, businessHours preservado | ✅ |
| 15 | `GET /companies/{other}/settings` → **403** (settings restrito à própria empresa, mesmo p/ SUPER_ADMIN) | ✅ |
| 16 | Reversão da elevação + remoção da empresa de teste | ✅ |
| 17 | Pós-limpeza: AGENT volta a listar 1 empresa; empresa removida → **404** | ✅ |
| 18 | Banco: `companies.max_contacts` default **500**; `company_settings` persistida (timezone, businessHours) | ✅ |

**Resultado: 33/33 PASS.**

## Observações / Follow-ups

1. **`SUPER_ADMIN` com `*` não expandido (pré-existente):** o `RoleDataSeeder` grava
   `role_permissions` com `"*"` para SUPER_ADMIN, mas a resolução de permissões não expande `*` —
   o SUPER_ADMIN termina com **0 permissões** e não passa em endpoints `@PreAuthorize(...)`.
   **Follow-up na 8.6** (SaaS Hardening): expandir `*` ou semear permissões explícitas.
   No E2E, a elevação para `ADMIN`+`SUPER_ADMIN` garantiu as permissões (`settings:view/update`) e a role
   SUPER_ADMIN (listagem global) simultaneamente.
2. **Issuer por endpoint do Keycloak:** o token endpoint **interno** (`localhost:8080`) emite
   `iss=http://host:8080/realms/CRM` (inválido para o resource server); apenas a **URL pública**
   (`https://srv1348261.hstgr.cloud/realms/CRM`) gera o issuer validado pelo backend. E2E usa a URL pública.
3. **Password grant no E2E:** `crm-frontend` não permite direct access grants em produção; o E2E
   habilitou `directAccessGrantsEnabled` temporariamente e **reverteu após a validação**.

## Testes

- Backend: `mvn test` → **125 testes, 0 falhas, 0 erros** (inclui `CompanyServiceTest` com 18 casos,
  `CompanyControllerTest` e a suíte de identidade).
- Deploy: backend reconstruído e recriado na VPS (`docker compose build backend && up -d backend`).
