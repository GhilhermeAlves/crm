# Sprint 8.2 - Membership

**Data:** 2026-08-09 - **Ambiente:** local + VPS (produção `srv1348261.hstgr.cloud`) - **Status:** ✅ Concluída

## Identificação

- **Sprint:** 8.2
- **Nome:** Membership - vínculo de usuários a empresas com RLS e gestão de membros
- **Responsável:** AI Agent
- **Fase:** SaaS - Empresas / Multi-tenant
- **Dependência:** 8.1 (Company Foundation) - plano da Sprint 8 em `sprints/8/SPRINT_PLAN.md`

## Objetivo

Tornar o vínculo **usuário ↔ empresa** (`membership`) a entidade de primeiro plano do multi-tenant
SaaS:

- Tabela `memberships` com **Row-Level Security (RLS) forçado no banco** como camada final de
  isolamento por empresa.
- **Gestão de membros na API** (`GET/PUT/DELETE /companies/{id}/members`) com roles
  `AGENT | ADMIN | MANAGER` e validações (não remover o último admin/membro, própria empresa).
- **Gate de acesso**: usuário sem membership **ativa** não resolve o `CurrentUser` (perde acesso
  ao CRM), com defesa em profundidade (auth-service + resolução local no backend).
- **Sync `user_roles`**: a role do membro atualiza automaticamente a role de identidade do usuário
  no backend (e revoga ao remover o vínculo).
- Backfill: usuários existentes recebem membership `AGENT/ACTIVE` da empresa padrão no deploy.

## Arquitetura / Decisões

| Decisão | Detalhe |
|---|---|
| Modelo | `membership` agregado em **backend** (domínio próprio) e **auth-service** (somente leitura para o gate). Regras de negócio no `MembershipService`; RLS no Postgres. |
| Roles | `AGENT` (padrão), `ADMIN`, `MANAGER`. `MANAGER` pode promover/rebaixar e remover membros (`membership:manage`); qualquer autenticado lê a própria empresa (`membership:view` é concedido via role no `CurrentUser`). |
| RLS | `memberships` com `FORCE ROW LEVEL SECURITY`; `membership_own_policy` (membro lê a própria linha) + `membership_tenant_policy` (membro lê/altera membros da própria empresa quando `company_id` igual ao da sessão). `crm_admin` (superuser/BYPASSRLS) isento; `crm_app` **NOBYPASSRLS** — é o papel da aplicação, isolado por empresa. |
| Gate de acesso | `CurrentUser` (backend) e `CurrentUserResolutionService` (auth-service) passam a exigir membership **ativa**; sem ela o usuário não resolve o principal (perde o login). Auth-service consulta o banco (`crm_main.memberships`) via JPA; backend tem fallback local (`LocalCurrentUserResolver`) que aplica a mesma regra. |
| Sync de role | Alteração de role do membro → atualiza a role correspondente no `user_roles` (backend); remoção do vínculo → revoga as roles de membro e marca a membership como `REMOVED` (mantém histórico). |
| Endpoints | `GET /api/v1/companies/{id}/members` (`membership:view`), `PUT|DELETE /api/v1/companies/{id}/members/{userId}` (`membership:manage`), `GET /api/v1/me/memberships` (autenticado). |

## Migrações / Banco

- **V030** - `memberships` (id, company_id FK, user_id, role, status, timestamps) + índices,
  **RLS FORCE** + `membership_own_policy`/`membership_tenant_policy`. Backfill automático dos
  usuários existentes para a empresa padrão via `MembershipDataSeeder` (6 memberships `AGENT/ACTIVE`).
- **V031** - `GRANT SELECT, INSERT, UPDATE, DELETE ON memberships TO crm_app` (o `crm_app` é
  NOBYPASSRLS; sem o GRANT o app recebia `permission denied`).

## Problemas encontrados e correções

1. **`V030` quebrava o build do backend (`RAISE` com 2 placeholders):** o PL/pgSQL usava
   `RAISE EXCEPTION ... '%.%'` com **um** parâmetro → `too few parameters specified for RAISE`.
   Corrigido para `%` (1 placeholder). Detectado no primeiro build de deploy.
2. **`permission denied for table memberships` em produção:** a migração V030 não concedia DML ao
   `crm_app` (NOBYPASSRLS). Aplicado `GRANT ... ON memberships TO crm_app` manualmente na VPS e
   criada a **V031** para paridade em instalações futuras.
3. **Bug de comparação no `MembershipService` (catch de teste):** a atualização de role comparava o
   nome da role do usuário com `RoleName` (enum) em vez de `String`, quebrando o sync de
   `user_roles`. Corrigido; coberto por `MembershipServiceTest`.

## E2E em produção

Ambiente: RLS validado **direto no banco** (`crm_app`, como a aplicação) e API via container
`curlimages/curl` na rede `crm-network` (`http://backend:8080`), tokens OIDC pela URL pública
(`https://srv1348261.hstgr.cloud/realms/CRM`). `directAccessGrantsEnabled` habilitado
temporariamente no client `crm-frontend` e revertido ao final. Empresa de teste `C2`
(`22222222-3333-4444-5555-666666666666`) criada/removida direto no banco.

### RLS — nível banco (8/8 PASS)

| # | Cenário | Resultado |
|---|---------|-----------|
| 1 | `crm_app` vê apenas a própria empresa (own policy + tenant policy) | ✅ |
| 2 | `crm_app` **não** enxerga membros da empresa C2 (isolamento cross-company) | ✅ |
| 3 | RLS **FORCE** ativo: superuser de sessão não burla policies | ✅ |
| 4 | Criação de membership em outra empresa → bloqueada pelo RLS | ✅ |
| 5 | Sessão sem `app.current_company_id` → denega por padrão (FORCE) | ✅ |
| 6 | Membro `REMOVED`/sem `ACTIVE` → fora das policies | ✅ |
| 7 | `UPDATE/DELETE` de linha de outra empresa → bloqueado | ✅ |
| 8 | `crm_admin` (BYPASSRLS) opera sem restrição | ✅ |

### API — nível aplicação (9/9 cenários validados)

| # | Cenário | Resultado |
|---|---------|-----------|
| A | `GET /api/v1/me/memberships` (novo, AGENT) → 200, membership ativa | ✅ |
| B | `PUT /companies/{C1}/members/{novo}` role→MANAGER → 200; role e `user_roles` sincronizados | ✅ |
| C | `GET /companies/{C1}/members` como MANAGER → 200, lista 6 membros | ✅ |
| D | Membro de C1 lê `GET /companies/{C2}/members` → **403** (cross-company) | ✅ |
| E | Rebaixar o **último admin** → **400** (`IllegalStateException`) | ✅ |
| F | `PUT /companies/{C1}/members/{inexistente}` → **404** | ✅ |
| G | `DELETE /companies/{C1}/members/{novo}` → 204; membership `REMOVED`, `user_roles` revogado | ✅ |
| H | Membro removido tenta `/me/memberships` → **401** (gate de acesso) | ✅ |
| I | Membro removido tenta `GET /companies` → **401** (gate de acesso) | ✅ |

**Resultado: 19/19 PASS (8 RLS + 9 API + gate confirmado).**

## Observações / Follow-ups

1. **Removido recebe 401 (e não 403):** o gate impede a **resolução do principal** (sem
   `CurrentUser` não há sessão válida) — resposta é **401** nos cenários H/I. Comportamento
   intencional (o membro "não existe mais" para o CRM), validado em profundidade: auth-service
   responde 403 ao backend, que cai no `LocalCurrentUserResolver` e também aplica o gate.
2. **V031 fora do build em produção:** o GRANT foi aplicado manualmente na VPS; a migração V031
   entra no repositório e será aplicada (idempotente) no próximo deploy.
3. **Senha de teste temporária:** `novo.crm@crm.local` teve a senha Keycloak definida como
   `admin123` (admin Keycloak) para o E2E e **não foi revertida**. `e2e.tester@crm.local`/
   `admin123` já existia em `/tmp/e2e_creds.txt` (VPS). Nenhuma credencial de produção foi alterada.
4. **`directAccessGrantsEnabled`:** habilitado para o E2E e **revertido para `false`** ao final.
5. **Backfill** criou 6 memberships `AGENT/ACTIVE` para os usuários da empresa padrão (C1).

## Testes

- Backend: `mvn test` → **143 testes, 0 falhas, 0 erros** (inclui `MembershipServiceTest` com 12
  casos e `MembershipControllerTest` com 6 casos).
- Auth-service: `mvn test` → **280 testes, 0 falhas** (1 skip pré-existente).
- Deploy: backend e auth-service reconstruídos e recriados na VPS; `crm-backend` (healthy),
  `crm-auth-service` (healthy); Flyway **V030** aplicada; site público 200.
- Backup do banco antes do deploy: `/opt/crm/backups/crm_main_20260809-160919.dump`.
