# Sprint 5 — Tenant (Isolamento Multi-Tenant via RLS)

## Identificação
- **Sprint:** 5
- **Nome:** Tenant
- **Data Início:** 2026-08-01
- **Data Fim:** 2026-08-02
- **Status:** ✅ Concluída
- **Responsável:** AI Agent
- **Fase:** Segurança

## Objetivo
Implementar e validar isolamento real de dados entre tenants no PostgreSQL usando Row Level Security (RLS) com `FORCE ROW LEVEL SECURITY`, aplicação via role não-superuser (`crm_app`) e contexto de tenant propagado pela aplicação (`SET app.current_company_id`).

## Escopo
- Migrations V011–V021 (infraestrutura de multi-tenancy, RLS em 18 tabelas tenant-scoped)
- Criação da role de produção `crm_app` (LOGIN, NOSUPERUSER, NOBYPASSRLS) com privilégios mínimos
- Correção dos riscos V018 + RoleDataSeeder (migração de roles globais para a empresa real + seeder resiliente ao RLS)
- Validação cross-tenant (SELECT/INSERT/UPDATE/DELETE) e concorrência A/B na VPS

---

## Migrations Aplicadas (Flyway V001–V021)

| Migration | Descrição | Status |
|-----------|-----------|--------|
| V001–V010 | Schema base (já existente) | ✅ |
| V011 | audit_logs safety net | ✅ |
| V012 | companies.id DEFAULT gen_random_uuid() | ✅ |
| V013 | Schema `app` + `app.current_tenant_id()` | ✅ |
| V014 | company_settings + subscriptions | ✅ |
| V015 | contacts, contact_addresses, contact_custom_fields, tags, contact_tags | ✅ |
| V016 | leads | ✅ |
| V017 | pipelines, stages, opportunities, opportunity_history | ✅ |
| V018 | company_id NOT NULL + migração de roles globais para empresa real | ✅ |
| V019 | RLS em 10 tabelas tenant-scoped | ✅ |
| V020 | RLS em refresh_tokens + password_reset_tokens (12 tabelas) | ✅ |
| **V021** | **RLS estendido para contacts/leads/tags e relacionadas (18 tabelas)** | ✅ |
| **V022** | **Bootstrap de identidade: policy SELECT em `users` via `app.current_keycloak_sub`** | ✅ |

---

## Matriz de Isolamento RLS (Validação na VPS)

Ambiente: `crm-vps` (srv1348261), PostgreSQL 17.10, banco `crm_main`, role `crm_app` (LOGIN, NOSUPERUSER, **NOBYPASSRLS**).

### 1. Cobertura RLS — 18 tabelas tenant-scoped (RLS = true, FORCE RLS = true)

| Tabela | RLS | FORCE RLS | Policy (ALL) | Critério de tenant |
|--------|-----|-----------|--------------|--------------------|
| users | ✅ | ✅ | tenant_isolation_policy | company_id = tenant |
| roles | ✅ | ✅ | tenant_isolation_policy | company_id = tenant |
| user_roles | ✅ | ✅ | tenant_isolation_policy | company_id = tenant |
| audit_logs | ✅ | ✅ | tenant_isolation_policy | company_id = tenant |
| company_settings | ✅ | ✅ | tenant_isolation_policy | company_id = tenant |
| subscriptions | ✅ | ✅ | tenant_isolation_policy | company_id = tenant |
| pipelines | ✅ | ✅ | tenant_isolation_policy | company_id = tenant |
| stages | ✅ | ✅ | tenant_isolation_policy | company_id = tenant |
| opportunities | ✅ | ✅ | tenant_isolation_policy | company_id = tenant |
| opportunity_history | ✅ | ✅ | tenant_isolation_policy | via opportunities.company_id |
| refresh_tokens | ✅ | ✅ | tenant_isolation_policy | via users.company_id |
| password_reset_tokens | ✅ | ✅ | tenant_isolation_policy | via users.company_id |
| contacts | ✅ | ✅ | tenant_isolation_policy | company_id = tenant |
| contact_addresses | ✅ | ✅ | tenant_isolation_policy | via contacts.company_id |
| contact_custom_fields | ✅ | ✅ | tenant_isolation_policy | via contacts.company_id |
| tags | ✅ | ✅ | tenant_isolation_policy | company_id = tenant |
| contact_tags | ✅ | ✅ | tenant_isolation_policy | via contacts.company_id E tags.company_id |
| leads | ✅ | ✅ | tenant_isolation_policy | company_id = tenant |

**Tabelas globais (sem RLS):** companies, permissions, role_permissions (somente leitura para `crm_app`). flyway_schema_history (manutenção Flyway).

### 2. Testes Cross-Tenant (SELECT/INSERT/UPDATE/DELETE)

| # | Cenário | Resultado Esperado | Resultado Real |
|---|---------|--------------------|----------------|
| A.2 | Tenant A SELECT de dados do Tenant B | 0 linhas | ✅ 0 linhas |
| A.3 | Tenant A INSERT com company_id do B | violação RLS | ✅ RLS violation |
| A.4 | Tenant A UPDATE em registro do B | 0 linhas | ✅ UPDATE 0 |
| A.5 | Tenant A DELETE em registro do B | 0 linhas | ✅ DELETE 0 |
| A.6 | Tenant A UPDATE/DELETE cross via WHERE company_id | 0 linhas | ✅ 0 |
| A.7 | Tenant A INSERT/SELECT próprio | OK | ✅ 2 contatos |
| B.1 | Tenant B vê apenas seus dados | só dados B | ✅ 1/1/1/1 |
| B.2 | Tenant B SELECT de dados do A | 0 linhas | ✅ 0 |
| B.3 | Tenant B INSERT com company_id do A | violação RLS | ✅ RLS violation |
| B.4 | Tenant B UPDATE em registro do A | 0 linhas | ✅ UPDATE 0 |
| B.5 | Tenant B DELETE em registro do A | 0 linhas | ✅ DELETE 0 |
| B.6 | Tenant B INSERT próprios (tag, contact_tag, lead) | OK | ✅ |
| B.7 | contact_tag contato B + tag do A | violação RLS | ✅ RLS violation |
| B.8 | contact_tag contato do A + tag B | violação RLS | ✅ RLS violation |
| B.9 | Tenant B UPDATE cross em tag do A | 0 linhas | ✅ UPDATE 0 |
| B.10 | Tenant B DELETE cross em tag do A | 0 linhas | ✅ DELETE 0 |
| 3 | Sem contexto de tenant (fail-closed) | 0 linhas | ✅ 0 |

### 3. Concorrência A/B (análogo ao TenantIsolationConcurrencyIT)

| Métrica | Resultado |
|---------|-----------|
| Operações simultâneas | 8 (4 por tenant) |
| Crescimento por tenant | incremental 1→4 (cada tenant viu só o seu) |
| Contaminação cruzada | **0** |
| Cleanup pós-teste | ✅ |

---

## Correções de Riscos (Pré-Deploy)

| Risco | Correção |
|-------|----------|
| V018 migrava roles para `00000000-...-0001` (empresa inexistente em prod) | V018 reescrito: migra para a primeira empresa ACTIVE real (11111111-...) |
| RoleDataSeeder rodava sem TenantContext → crash com RLS FORCE | Seeder agora seta `TenantContext.setCompanyId(...)` e busca roles por `findByNameAndCompanyId(roleName, companyId)` |
| AuthService.assignDefaultRole buscava roles na empresa fixa do sistema | Agora busca por `user.getCompanyId()` |

## Evidência de Boot (produção)

- Flyway: V021 → V022 aplicadas com sucesso, banco em v022
- `RoleDataSeeder - Role seeding completed (company=11111111-2222-3333-4444-555555555555)` sob `crm_app` com `SET app.current_company_id`
- `CrmApplication Started`, health `{"status":"UP"}`
- Pool Hikari: conexões `crm_app` (PostgreSQL JDBC Driver)
- auth-service: `Started AuthServiceApplication`, health UP

---

## Correção do Bloqueio de Login E2E (RLS FORCE × bootstrap de identidade)

### Causa raiz
Com RLS `FORCE` e a role `crm_app` (NOBYPASSRLS), o bootstrap de login consulta
`users` por `keycloak_sub` **antes** de conhecer o `company_id` (chicken-and-egg):
`crm_app` sem `app.current_company_id` não enxerga nem a própria linha
(fail-closed 0 rows) → `findExistingKeycloakUser` vazio → tentativa de
provisionamento → `Role padrão não encontrada no banco: AGENT` → **401**. O
antigo `crm_admin` (BYPASSRLS) mascarava o problema. Reproduzido por SQL: sem
tenant → 0 rows; com `SET app.current_company_id = '11111111-...'` → 1 row.

### Correções aplicadas
| Correção | Detalhe |
|----------|---------|
| **V022** — `identity_bootstrap_policy` em `users` | `CREATE POLICY ... FOR SELECT USING (keycloak_sub = NULLIF(current_setting('app.current_keycloak_sub', true), ''))`. Adiciona um **OR** sobre o isolamento por tenant: a própria linha fica visível pelo `sub` antes de o tenant ser conhecido. RLS FORCE mantido em `users` e nas demais 17 tabelas. |
| **GUC `app.current_keycloak_sub`** | `TenantContext` ganhou `keycloakSub` (ThreadLocal); `TenantAwareDataSource` (backend e auth-service) aplica `SET/RESET app.current_keycloak_sub` por conexão, junto do `SET/RESET app.current_company_id`. |
| **Resolvers** | `LocalCurrentUserResolver` (backend) e `CurrentUserResolutionService` (auth-service) definem o `sub` antes da consulta de bootstrap e o `company_id` assim que resolvido; limpeza via `TenantContext.clear()` (try/finally). |
| **TenantFilter (backend)** | Reposicionado no chain de segurança: `addFilterAfter(tenantFilter, BearerTokenAuthenticationFilter.class)` (antes era ancorado em `UsernamePasswordAuthenticationFilter`, que não existe no resource server → o filtro rodava **antes** da autenticação e nunca aplicava o tenant em requisições autenticadas). |
| **Flyway com privilégio de DDL** | `crm_app` não é dono das tabelas → não pode `CREATE POLICY`. O compose do backend agora passa `SPRING_FLYWAY_URL/USER/PASSWORD` (usuário `crm_admin`, superuser) para o Flyway migrar com privilégio, mantendo o runtime com `crm_app` (princípio do menor privilégio). |

### E2E real (produção, 2026-08-02)
```
password grant (admin-cli, HTTPS) → sub 78490eac-150e-44db-b2c4-d7999c1c3801,
iss https://srv1348261.hstgr.cloud/realms/CRM, realm_access ADMIN

GET /api/v1/auth/me → HTTP 200
{id: 974bbedb-..., email: ghilherme007@gmail.com, name: PAULO Administrador,
 companyId: 11111111-2222-3333-4444-555555555555, status: ACTIVE, isActive: true}

auth-service GET /internal/auth/current-user → HTTP 200 RESOLVED
{userId: 974bbedb-..., roles: [AGENT], permissions: [11 RBAC permissions]}
```
A resolução do `CurrentUser` ocorreu com `companyId` e `roles/permissions` reais
do banco (log `[TENANT] SET app.current_company_id` em requisição autenticada).

### Limitação conhecida (registrada na validação — **resolvida abaixo**)
O provisionamento de um usuário **novo** (sub inexistente) ainda exige INSERT em
`users` sob RLS FORCE, cujo `WITH CHECK` exige `company_id = app.current_tenant_id()`
— não coberto pela policy SELECT de bootstrap. Efeito: primeiro login de usuário
totalmente novo retorna 401 em vez de auto-provisionar. Requer extensão futura
(ex.: `SET LOCAL app.current_company_id` dentro da transação de provisionamento).

### Correção do provisionamento de usuário novo (pós-validação, 2026-08-02)
#### Causa raiz
No `AuthService.createProvisionedUser`, o INSERT em `users` ocorria **antes** de o
`company_id` ser definido no `TenantContext` — o GUC `app.current_company_id` só era
setado depois do retorno do `provisionKeycloakUser` (em `LocalCurrentUserResolver`).
Sob RLS FORCE, o `WITH CHECK (company_id = app.current_tenant_id())` avaliava
`company_id = NULL` → **INSERT bloqueado** (`DataIntegrityViolationException` → 401),
mesmo quando o tenant era determinável. Além disso, o fallback para "primeira
empresa ativa" escolhia um tenant arbitrariamente (proibido).

#### Correção aplicada (apenas camada de aplicação — **nenhuma migration nova, nenhum bypass de RLS**)
| Correção | Detalhe |
|----------|---------|
| **GUC de tenant definido antes do INSERT** | `createProvisionedUser` agora resolve o `company_id` e chama `TenantContext.setCompanyId(companyId)` **antes** do primeiro JDBC (o datasource aplica `SET app.current_company_id` na aquisição da conexão, no primeiro statement). O `WITH CHECK` é satisfeito legitimamente para o usuário recém-criado — mesmo padrão já comprovado do `RoleDataSeeder`. RLS FORCE e `crm_app` (NOBYPASSRLS) permanecem intactos. |
| **Tenant de fonte confiável apenas** | `resolveDefaultCompanyId` usa **somente** `app.auth.provisioning.default-company-id` (`AUTH_DEFAULT_COMPANY_ID`). Removido o fallback "primeira empresa ativa" (inventava tenant). |
| **PROVISIONING_REQUIRED explícito** | Sem tenant configurado → `UserProvisioningException("PROVISIONING_REQUIRED: nenhum tenant determinável...")` — **nenhum INSERT arbitrário**, nenhum tenant inventado. |

#### Fluxo final de provisionamento
1. Primeira chamada (`/auth/me`): auth-service → `PROVISIONING_REQUIRED` (sem usuário) → backend provisiona localmente: resolve `AUTH_DEFAULT_COMPANY_ID`, seta o GUC de tenant, INSERT em `users` + role `AGENT` (passa no `WITH CHECK`) → HTTP 200 com `CurrentUser` completo.
2. Segunda chamada: auth-service resolve o usuário existente (`RESOLVED`) → HTTP 200.
3. Sem `AUTH_DEFAULT_COMPANY_ID` configurado → `PROVISIONING_REQUIRED` explícito (401 com mensagem clara), sem inventar tenant.
4. Usuário existente/inativo → fluxo anterior preservado (reuso/sync/rejeição).

#### Testes (5 cenários obrigatórios)
| # | Cenário | Teste | Resultado |
|---|---------|-------|-----------|
| 1 | Usuário existente resolve → normal | `shouldReuseExistingUserOnSubsequentLogin`, `shouldLinkAndSyncExistingUserFoundByEmail` | ✅ |
| 2 | Novo com tenant determinável → INSERT + tenant no contexto | `shouldProvisionNewUserOnFirstLogin` (assert `TenantContext.getCompanyId()` = tenant configurado) | ✅ |
| 3 | Novo sem tenant determinável → PROVISIONING_REQUIRED, sem INSERT | `shouldSignalProvisioningRequiredWhenNoTenantConfigured` | ✅ |
| 4 | Tenant indevido/arbitrário bloqueado (sem fallback) | `shouldSignalProvisioningRequiredWhenConfiguredTenantInvalid` + RLS `WITH CHECK` (validado na VPS, A.3/B.3) | ✅ |
| 5 | `crm_app` segue sujeito ao RLS (sem bypass) | nenhuma policy nova/alterada; RLS FORCE intacto; suite backend 19 classes / 0 falhas + auth-service | ✅ |

#### Configuração de deploy (obrigatória para auto-provisionamento)
Para habilitar o provisionamento de usuário novo em produção, definir o tenant
explícito no backend:
- `AUTH_DEFAULT_COMPANY_ID=11111111-2222-3333-4444-555555555555` no `.env` da VPS
- adicionar `- AUTH_DEFAULT_COMPANY_ID=${AUTH_DEFAULT_COMPANY_ID}` ao serviço `backend` no compose
Sem isso, o comportamento é **fail-closed** (PROVISIONING_REQUIRED), nunca um tenant inventado.

---

## Pendências

- [x] ~~Login E2E via Keycloak~~ — resolvido: usuário real do realm
      `ghilherme007@gmail.com` (password grant via HTTPS) → `/auth/me` **200**
- [x] ~~Provisionamento de usuário novo sob RLS FORCE~~ — resolvido: GUC de tenant
      definido antes do INSERT + tenant somente de `AUTH_DEFAULT_COMPANY_ID`
      (PROVISIONING_REQUIRED explícito quando não configurado); pendente **deploy**
      do backend com `AUTH_DEFAULT_COMPANY_ID` no `.env`/compose
- [ ] Publicar arquivos Sprint 5 na VPS (se aplicável)

---

## Conclusão

✅ **Sprint 5 APROVADA** — RLS ativo e FORÇADO em 18 tabelas tenant-scoped,
validado cross-tenant (SELECT/INSERT/UPDATE/DELETE) e em concorrência A/B com a
role real de produção `crm_app` (não-superuser, sem bypassrls). O bloqueio de
login E2E (bootstrap de identidade × RLS FORCE) foi corrigido e validado em
produção: password grant real do Keycloak → `/api/v1/auth/me` **200** com
`companyId`, roles e permissions reais. Nenhuma tabela de dados multi-tenant
acessível pela aplicação ficou fora do isolamento RLS.

---

*Data: 2026-08-02*
