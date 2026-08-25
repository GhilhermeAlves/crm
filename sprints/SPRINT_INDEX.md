# Índice de Sprints

> **Documento oficial do roadmap e do estado das Sprints deste projeto — fonte de verdade.**
> Código implementado **sem** atualização deste índice **não** contabiliza Sprint concluída.
> A atualização do índice faz parte da própria Sprint (ver `Definition of Done` no fim).

## Convenção de numeração

| Faixa | Área |
|-------|------|
| 0 – 3.3 | Fundação / Knowledge Layer |
| 4.x | Infraestrutura |
| 5 – 6.10 | Segurança (Tenant + Access Gateway / OIDC) |
| **7.x** | **Identidade / Autenticação** |
| 8 | SaaS — Empresas |
| 9 | CRM — Contatos (User & Permission Management) |
| 10 | CRM — Leads |
| 11 | CRM — Pipeline |
| 12 | CRM — orientado à ação (Activities/Tasks/Dashboard) |
| 13–15 | CRM — Automação de Workflows (13 base · 14 disparo por inatividade · 15 auditoria) |
| 16 | Omnichannel — WhatsApp |
| 17 | Omnichannel — Campanhas |
| 18 | Omnichannel — Automações |
| 19 | Analytics — Dashboard |
| 20 | IA |

## Planejamento

| Sprint | Nome | Status | Data | Responsável | Dependência |
|--------|------|--------|------|-------------|-------------|
| 0 | Planejamento | ✅ Concluída | 2026-07-15 | Architect | — |
| 1 | Fundação | ✅ Concluída | 2026-07-15 | Architect | Sprint 0 |
| 2 | Correções | ✅ Concluída | 2026-07-15 | Architect | Sprint 1 |

## Knowledge Layer

| Sprint | Nome | Status | Data | Responsável | Dependência |
|--------|------|--------|------|-------------|-------------|
| 3.1 | Knowledge Layer | ✅ Concluída | 2026-07-15 | Architect | Sprint 2 |
| 3.2 | AI Runtime Layer | ✅ Concluída | 2026-07-15 | Architect | Sprint 3.1 |
| 3.3 | Sprint Management Layer | ✅ Concluída | 2026-07-15 | AI Agent | Sprint 3.2 |

## Infraestrutura

| Sprint | Nome | Status | Data | Responsável | Dependência |
|--------|------|--------|------|-------------|-------------|
| 4.1 | Infraestrutura Auth | ↪️ Absorvida | 2026-08-09 | AI Agent | Sprint 3.2 |
| 4.2 | Usuários | ↪️ Absorvida | 2026-08-09 | AI Agent | 4.1 |
| 4.3 | Login | ↪️ Absorvida | 2026-08-09 | AI Agent | 4.1, 4.2 |
| 4.4 | Frontend Auth | ↪️ Absorvida | 2026-08-09 | AI Agent | 4.3 |
| 4.5 | Testes Auth | ↪️ Absorvida | 2026-08-09 | AI Agent | 4.3, 4.4 |

> **Fase 4.x (Infraestrutura Auth) — absorvida pela arquitetura Keycloak/OIDC (Sprints 5–7).**
> O plano original da fase 4 (auth monolítico em Spring Security/JWT: 4.1 infra, 4.2 usuários,
> 4.3 login, 4.4 frontend auth, 4.5 testes auth) foi **substituído pelo Access Gateway OIDC** com
> Keycloak. A fase Segurança (5, 6.0–6.10) entregou tenant/RLS, gateway de autenticação e sessão
> Redis; a fase Identidade (7.x) entregou login Google, account linking, telefone/OTP e recuperação
> de conta. 4.1 foi encerrada em 2026-07-15 (REPORT próprio, 93/100) e 4.3 teve Review aprovado,
> mas suas entregas (JWT próprio, CORS aberto, `permitAll`) foram substituídas/descontinuadas pelo
> gateway. **4.2/4.4/4.5 não serão executadas como fase própria** — seu escopo está coberto por
> 5–7.x. Consolidação registrada em 2026-08-09.

## Segurança

| Sprint | Nome | Status | Data | Responsável | Dependência |
|--------|------|--------|------|-------------|-------------|
| 5 | Tenant | ✅ Concluída | 2026-08-01 | AI Agent | 4.1 |
| 6 | Access Gateway | ✅ Concluída | 2026-08-02 | AI Agent | 5 |
| 6.1 | Gateway OIDC (authorize/callback) | ✅ Concluída | 2026-08-02 | AI Agent | 6 |
| 6.2 | Gateway OIDC (logout + ciclo de vida da sessão) | ✅ Concluída | 2026-08-02 | AI Agent | 6.1 |
| 6.3 | Gateway OIDC (integração de produção / E2E) | ✅ Concluída | 2026-08-03 | AI Agent | 6.2 |
| 6.4 | Migração do frontend para o Access Gateway (BFF relay) | ✅ Concluída | 2026-08-04 | AI Agent | 6.3 |
| 6.5 | Gateway OIDC (hardening, observabilidade e correções) | ✅ Concluída | 2026-08-04 | AI Agent | 6.4 |
| 6.6 | Gateway OIDC (health/readiness, correlation ID e rate limiting) | ✅ Concluída | 2026-08-04 | AI Agent | 6.5 |
| 6.7 | Gateway OIDC (rate limit do relay `/api/*` por usuário autenticado) | ✅ Concluída | 2026-08-04 | AI Agent | 6.6 |
| 6.8 | Gateway OIDC (hardening, concorrência do rate limiting e fechamento) | ✅ Concluída | 2026-08-05 | AI Agent | 6.7 |
| 6.9 | Gateway OIDC (auditoria final de segurança/arquitetura, correção do login manual e fechamento) | ✅ Concluída | 2026-08-05 | AI Agent | 6.8 |
| 6.10 | Production Infrastructure Hardening & Final Closure (etapa auth/Gateway encerrada) | ✅ Concluída | 2026-08-05 | AI Agent | 6.9 |

## Identidade / Autenticação

| Sprint | Nome | Status | Data | Responsável | Dependência |
|--------|------|--------|------|-------------|-------------|
| 7.0 | Identity Provider Architecture (catálogo de provedores + `kc_idp_hint` + fundação do login) | ✅ Concluída | 2026-08-05 | AI Agent | 6.10 |
| 7.1 | Login/Cadastro com Google | ✅ Concluída | 2026-08-06 | AI Agent | 7.0 |
| 7.2 | Account Linking | ✅ Concluída | 2026-08-08 | AI Agent | 7.1 |
| 7.3 | Telefone/OTP | ✅ Concluída | 2026-08-08 | AI Agent | 7.1 |
| 7.4 | Telefone → OTP → senha Keycloak (login completo por telefone na UI, catálogo `phone-enabled`, rota nginx direta) | ✅ Concluída | 2026-08-08 | AI Agent | 7.2, 7.3 |
| 7.5 | Recuperação de conta — forgot/reset-password com reset REAL no Keycloak (service account `crm-keycloak-admin`, rotas nginx diretas, RLS V027/V028) | ✅ Concluída | 2026-08-08 | AI Agent | 7.3, 7.4 |

> **7.2 — Account Linking ✅ Concluída (2026-08-08).** Débito registrado em 2026-08-08 resolvido:
> - ✅ `sprints/7.2/REPORT.md` criado (este fechamento);
> - ✅ E2E em produção validado na VPS (Redis ativo, sobrevivência a reinício, expiração lógica,
>   CSRF 403, senha incorreta 401 sem consumo);
> - ✅ `RedisPendingLinkStore` (chave `gateway:pending-link:<token>`, TTL nativo, uso único)
>   alinhado a `AUTH_GATEWAY_SESSION_STORE=redis`; `InMemoryPendingLinkStore` condicional;
> - ✅ `/auth/link-status` e `/auth/link` com testes unitários (`GatewayOidcLinkingTest`,
>   `OidcGatewayControllerTest`, `RedisPendingLinkStoreTest`);
> - ✅ Bug CSRF crítico corrigido: `GatewayCsrfFilter` agora registrado também para `/auth/link`
>   (commit `8851595`).

## SaaS

| Sprint | Nome | Status | Data | Responsável | Dependência |
|--------|------|--------|------|-------------|-------------|
| 8 | Empresas | ✅ Concluída | 2026-08-09 | AI Agent | 7.5 |
| 8.1 | Company Foundation | ✅ Concluída | 2026-08-09 | AI Agent | 8 (plano) |
| 8.2 | Membership | ✅ Concluída | 2026-08-09 | AI Agent | 8.1 |
| 8.3 | Onboarding | ✅ Concluída | 2026-08-10 | AI Agent | 8.2 |
| 8.4 | Company Switcher | ✅ Concluída | 2026-08-10 | AI Agent | 8.2 |
| 8.5 | Invitations | ✅ Concluída | 2026-08-11 | AI Agent | 8.2, 8.3 |
| 8.6 | SaaS Hardening | ✅ Concluída | 2026-08-12 | AI Agent | 8.4, 8.5 |

> **Planejamento (próxima sprint — não implementar nesta etapa):** a Sprint 8 será dividida
> internamente em **8.1 Company Foundation · 8.2 Membership · 8.3 Onboarding ·
> 8.4 Company Switcher · 8.5 Invitations · 8.6 SaaS Hardening** (padrão decimal já adotado em
> 6.x/7.x). Plano detalhado (entregas, critérios de aceite, decisões de escopo D1–D6 e
> dependências externas) em **`sprints/8/SPRINT_PLAN.md`**.

> **8.1 — Company Foundation ✅ Concluída (2026-08-09).**
> - ✅ Empresa como entidade de primeiro plano: `CompanyController` passa a mapear
>   `/api/v1/companies` com alias de compatibilidade `/api/v1/tenants`;
> - ✅ Leitura por membro (`GET /`, `/me`, `/{id}` com `isAuthenticated()` + escopo no serviço),
>   leitura cross-tenant restrita a SUPER_ADMIN;
> - ✅ API de settings da empresa (`GET/PUT /companies/{id}/settings`, `settings:view/update`,
>   escopo restrito à própria empresa);
> - ✅ `companies.max_contacts` (V029, default 500) exposto no modelo;
> - ✅ Bug corrigido: upsert de settings gerava `StaleObjectStateException` (500) —
>   `Persistable` + `existsById` para decidir `persist` vs `merge`;
> - ✅ Suíte backend verde (125 testes) + E2E em produção **33/33 PASS**;
> - 📄 `sprints/8.1/REPORT.md`.

> **8.2 — Membership ✅ Concluída (2026-08-09).**
> - ✅ `memberships` como entidade de primeiro plano do multi-tenant com **RLS FORCE** no banco
>   (`membership_own_policy` + `membership_tenant_policy`; `crm_admin` BYPASSRLS, `crm_app`
>   NOBYPASSRLS isolado por empresa);
> - ✅ API de gestão: `GET/PUT/DELETE /companies/{id}/members` (`membership:view`/`membership:manage`),
>   roles `AGENT|ADMIN|MANAGER`, validações de último admin/membro e própria empresa;
> - ✅ **Gate de acesso**: usuário sem membership ativa não resolve `CurrentUser` (401), em
>   profundidade (auth-service + `LocalCurrentUserResolver`);
> - ✅ Sync de `user_roles` ao alterar/remover membership (promoção, demissão, revogação);
> - ✅ Backfill no deploy (6 memberships `AGENT/ACTIVE`); V030 corrigida (`RAISE`) + V031 (GRANT
>   `crm_app`); backup `crm_main_20260809-160919.dump` antes do deploy;
> - ✅ Suíte backend verde (143 testes) + auth-service verde (280 testes) + E2E em produção
>   **19/19 PASS** (8 RLS + 9 API + gate);
> - 📄 `sprints/8.2/REPORT.md`.

> **8.3 — Onboarding ✅ Concluída (2026-08-10).**
> - ✅ `users.company_id` nullable (V032) + RLS `identity_onboarding_insert_policy`;
> - ✅ Provisionamento company-less no auth-service (`resolveDefaultCompanyId` → null);
> - ✅ `POST /api/v1/onboarding/companies` (cria empresa, seed de papéis via `RoleSeedService`,
>   membership `OWNER/ACTIVE`, eleva company ativa, concede acesso CRM);
> - ✅ `CurrentUser.companyId` nullable (backend + auth-service); usuário sem empresa resolve com
>   roles/permissões vazios e pula o gate do CRM (redirect `/onboarding`);
> - ✅ Frontend: rota `/onboarding` + gate (`ProtectedRoute`), feature onboarding (form/schema);
> - ✅ Backend 153, auth-service 282, frontend 56 testes; typecheck OK;
> - 📄 `sprints/8.3/REPORT.md`.

> **8.4 — Company Switcher ✅ Concluída (2026-08-10).**
> - ✅ Troca de empresa ativa **sem relogar**: `POST /api/v1/me/switch-company` e `GET
>   /api/v1/me/companies` (`MeController`/`MeService`), membros obtidos via
>   `companies.memberships` (RLS FORCE);
> - ✅ Empresa ativa autoritativa = `users.company_id`; `TenantContext`/`TenantFilter`/RLS
>   (GUC `app.current_company_id`) trocam de vista na hora; auth-service reflete o snapshot no
>   `GatewaySession` via `withCompanyId` (`CurrentUserResolutionService`);
> - ✅ Frontend: `CompanySwitcher` (UserMenu), `useAuthMutations.switchCompany` + auth.service;
> - ✅ RLS: teste de integração real (Testcontainers) `switchingActiveCompany_togglesTenantIsolation`
>   prova A→B→A sem vazar dados (0 cross-tenant);
> - ✅ Testes verdes: **backend 163, auth-service 284, frontend 59**, typecheck OK, lint sem erros;
> - ✅ **Reconciliação Git+deploy**: main reconciliado (7.4/7.5 da VPS semanticamente = 8.1–8.3),
>   push `c55fde9→3225201`, VPS sincronizada `--ff-only`, **build+deploy prod OK** (V031 Flyway
>   aplicado), serviços healthy, `/me` endpooints registrados;
> - ⚠️ Verificação manual pós-deploy pendente: login real com conta multi-empresa no browser;
> - 📄 `sprints/8.4/REPORT.md`.

> **8.5 — Invitations ✅ Concluída (2026-08-11).**
> - ✅ **Governança (Parte 1)**: versionamento de migrações reconciliado (V032–V035 no Git; V034 DML
>   + V035 lockdown de grants), grants padronizados (companies full DML; permissions e
>   `flyway_schema_history` SELECT-only), `users.company_id` nullable, seed de papéis no `createCompany`
>   (transacional via `RoleSeedService`), `SPRING_FLYWAY_OUT_OF_ORDER=true` no compose;
> - ✅ **Backend (Parte 2)**: `V036__invitations.sql` (tabela `invitations` + 4 policies RLS +
>   GUC `app.invitation_token_hash` + fns `app.set_invitation_token_context`/`current_invitation_token_hash`
>   + grants), token 32B base64url → SHA-256 hex (varchar(64)) persistido, única `PENDING` por
>   `(company_id, email)`, status `PENDING/ACCEPTED/REVOKED/EXPIRED` (decline→REVOKED), roles
>   `ADMIN/MANAGER/AGENT/VIEWER` (SUPER_ADMIN/OWNER bloqueados);
> - ✅ API: `GET/POST /companies/{id}/invitations` + `DELETE /{invitationId}` (`membership:view`/
>   `membership:manage`), `InvitationTokenContextHolder` + policies token p/ leitura/aceite cross-contexto;
> - ✅ RLS provado em produção (smoke): sem contexto → 0 linhas; com token → 1 linha (isolamento real);
> - ✅ Frontend: `/invitations` (lista + status badge + criar/revogar), `CreateInvitationDialog`, hooks e
>   serviço React Query, link na Sidebar (`membership:view`); typecheck OK, lint sem erros novos;
> - ✅ Testes verdes: **backend 179**, typecheck OK;
> - ✅ Deploy prod: Cherry-pick/rephase V032–V035 + V036 aplicadas (Flyway history `031..036`),
>   backend e frontend rebuild/deploy OK, `/invitations` respondendo;
> - ✅ Reconciliação (`cbee5ce`): frontend de aceite/recusa (`/invitations/accept`), rate limit
>   (`InvitationRateLimiter`), link absoluto no e-mail (`INVITATION_BASE_URL`); endpoints
>   validados no VPS (401 sem sessão); backend 179 verdes;
> - ✅ Finalização (`3519cba`): usuário sem empresa ativa passa a ter a empresa convidada como
>   ativa no aceite (Company Switcher/8.4); testes ampliados p/ token já usado/revogado,
>   membership duplicada, usuário em outra empresa e autorização cross-company (403); backend
>   **185 verdes** (invitation 21), frontend 66 testes verdes; única pendência = validação
>   manual autenticada no browser (sem credenciais de teste);
> - 📄 `sprints/8.5/REPORT.md`.

> **8.6 — SaaS Hardening ✅ Concluída (2026-08-12).** Última sprint da fase SaaS.
> - ✅ **Enforcement**: `QuotaExceededException`→HTTP 422 (`QUOTA_EXCEEDED`);
>   `CompanyQuotaService` (`assertCanAddContact`, `assertCanAddSpace`, `usage`);
>   `max_users` no `InvitationService` (create = activeMembers + PENDING convites; accept
>   bloqueia atinge limite e e-mail já membro); `max_contacts` (módulo mínimo `Contact`,
>   V015); `max_storage_mb` (módulo mínimo `Storage`, V037 `storage_objects` blob);
>   defaults `5 users / 500 contatos / 1024 MB`;
> - ✅ **Quotas/uso**: `GET /companies/{id}/usage` (`CompanyUsageResponse`) + feature
>   `usage` no frontend (types/service/hook + teste);
> - ✅ **Auditoria de tenant**: `AuditModule` +`MEMBERSHIPS`/`INVITATIONS`,
>   `TenantAuditRecorder` (lê `AuditContext`, fallback ator, seta/restaura tenant); eventos
>   de convite (criado/aceito/revogado), membership removida, switch company (`TENANTS UPDATE`);
> - ✅ **Revisão RLS/gateway**: corrigido set de TenantContext nos novos serviços
>   Contact/Storage (SUPER_ADMIN cross-tenant); policies revisadas (memberships/invitations/
>   storage_objects);
> - ✅ **Docs corrigidos para RLS real**: `MULTI_TENANCY.md` (2.0), `DATABASE_MAP.md` (2.0),
>   `BACKEND_MAP.md` (1.1, Implementação Vigente) — design antigo schema-per-tenant marcado
>   como superado;
> - ✅ **Testes**: backend **210 PASS** (antes 185), frontend **68 PASS** (antes 66), typecheck
>   + lint OK;
> - 📄 `sprints/8.6/REPORT.md`.

> **9 — User & Permission Management ✅ Concluída (2026-08-12).**
> - ✅ **Contexto reutilizado**: estrutura USUÁRIO → MEMBERSHIP → ROLE/PERFIL → PERMISSIONS,
>   `CurrentUser`, `@EnableMethodSecurity`, RLS e Company Switcher existentes — nenhuma
>   estrutura de users/roles/permissions duplicada; roles canônicas preservadas.
> - ✅ **Contact CRUD completo**: `update`/`delete` (`@PreAuthorize('contact:update'/'contact:delete')`),
>   `ContactService` + `requireOwnedActive` + auditoria (`AuditAction.UPDATE/DELETE`);
> - ✅ **`/auth/me` com permissões**: `UserResponse.withCurrentUser` expõe
>   `roles`/`membershipRole`/`permissions` da empresa ativa (antes expunha sem permissões);
> - ✅ **Autorização real**: guardas por permission (403) + isolamento multi-empresa provado
>   (`LocalCurrentUserResolverTest` — ADMIN A ≠ VIEWER B sem vazar);
> - ✅ **Frontend**: `User` +`roles/membershipRole/permissions`, `useAuth` deriva do `/auth/me`,
>   hook `useAuthorization` (`can`/`cannot`/`hasRole`/`isSuperAdmin`); telas `/settings/users`
>   e `/settings/roles` reusando features members + rbac; menu protegido por permission e
>   re-derivação a cada Company Switcher;
> - ✅ **Testes**: backend **215 PASS** (antes 210, +5 contact/403/multi-company), frontend
>   **74 PASS** (antes 69, +useAuthorization), typecheck OK, lint sem erros novos;
> - 📄 `sprints/9/REPORT.md`.

> **10 — Leads ✅ Concluída (2026-08-13).** Próxima etapa do funil (CRM).
> - ✅ **Camada de aplicação sobre base já existente**: tabela `leads` (V016) + RLS
>   FORCE (V021) e permissões `lead:*` (V007/`RoleSeedService`) reaproveitadas — **sem
>   nova migration** e sem duplicar RBAC/RLS/CurrentUser/Company Switcher.
> - ✅ **Backend** (espelho do módulo `contact`): `Lead`/enums (`LeadStatus/Source/
>   Classification`) + `LeadService` com `TenantContext` isolado por empresa, validação
>   de contato pertencente/ativo na mesma empresa (defense-in-depth), unicidade por
>   `(contact_id, company_id)` e auditoria (`AuditModule.LEADS`); `LeadController`
>   `/api/v1/companies/{companyId}/leads` com `@PreAuthorize('lead:*')` +
>   `requireCompanyAccess`; listagem com filtros + paginação (`PageResponse`);
>   handlers 404/409 no `GlobalExceptionHandler`.
> - ✅ **Frontend**: feature `features/leads` (types/schema Zod/serviço/hooks React
>   Query/componentes) + páginas `/leads`, `/leads/new`, `/leads/[id]`, `/leads/[id]/edit`;
>   Sidebar gated por `lead:read` e botões por `lead:create`/`lead:delete`.
> - ✅ **Testes** (**validados 2026-08-13**): backend **242** (antes 215; +21 leads —
>   service/controller/`LeadIsolationIT` 8/8 Testcontainers/PostgreSQL 17 + RLS provando
>   isolamento cross-tenant real na tabela `leads` — e +6 `InvitationRateLimiterTest`),
>   frontend **96** (antes 74, +schema/useLeads/componentes) — suíte completa
>   **96/96 verdes** (17 arquivos) + typecheck + lint OK, build prod OK.
> - ✅ **Deploy + validação VPS** (`20001d2..cf381ed`): rebuild backend+frontend e `up -d`
>   (sem nova migration), `/actuator/health` 200, endpoints de leads 401 sem sessão,
>   `/leads` 307→login, 0 ERROR no backend.
> - ✅ **Débito fechado — rate limiter de convites → Redis** (herdado de 8.5/8.6/9):
>   `InvitationRateLimiter` migrado de janela em memória para **janela fixa distribuída em
>   Redis** (Lua `INCR`+`EXPIRE`, padrão do `GatewayRateLimiter`); contrato
>   `tryCreate`/`tryAccept` preservado, limites 20/h create e 10/h accept, fail-open
>   controlado quando o Redis cai, `prune()` removido (TTL nativo); teste
>   `InvitationRateLimiterTest` 6/6.
> - ⚠️ **Débito**: E2E autenticado manual herdado (sem credenciais de teste).
> - 📄 `sprints/10/REPORT.md`.

> **11 — Pipeline ✅ Concluída (2026-08-13).** Funil de vendas por empresa.
> - ✅ **DB**: base `pipelines`/`stages`/`opportunities`/`opportunity_stage_history`
>   (V033–V037, RLS FORCE) já existente; **nova migration V038** adiciona as permissões
>   `pipeline:*`/`stage:*`/`opportunity:*`/`pipeline.metrics:*`.
> - ✅ **Backend** (módulo `com.becommerce.crm.pipeline`): `Pipeline`/`Stage`/`Opportunity`
>   + enums, `PipelineService`/`StageService`/`OpportunityService`/`OpportunityMetricsService`
>   com `TenantContext` isolado por empresa, regras (movimento ±1, conclusão/cancelamento só
>   no último estágio, histórico imutável), controllers
>   `/api/v1/companies/{companyId}/pipelines*` (+ stages/opportunities/metrics) com
>   `@PreAuthorize('pipeline:*')` + `requireCompanyAccess`; handlers 404/409.
> - ✅ **Frontend**: feature `features/pipeline` (types/schema Zod/serviço/hooks React
>   Query/componentes `PipelineBoard`/`OpportunityCard`/`PipelineMetricsStrip` + diálogos)
>   e página `app/(dashboard)/pipeline/page.tsx`; Sidebar/item gated por `pipeline:read`.
> - ✅ **Testes** (**validados 2026-08-13**): backend **267** (antes 228; +39 — service/
>   controller/`PipelineIsolationIT` Testcontainers/PostgreSQL + RLS provando isolamento
>   cross-tenant em `pipelines`/`stages`/`opportunities`), frontend **106** (antes 96;
>   +schema/OpportunityCard) — suíte **106/106** + typecheck/lint (novos arquivos) OK.
> - ✅ **Deploy + validação VPS**: rebuild backend+frontend e `up -d` (**V038 aplicada**,
>   Flyway `v038`), backend iniciou com 0 ERROR, `/pipeline` 307→login, endpoints de
>   pipelines registrados.
> - ⚠️ **Débito**: E2E autenticado manual herdado; scoring/distribuição/conversão
>   (L-020/030/040, P-0xx avançadas) para Sprints 20 (IA).
> - 📄 `sprints/11/REPORT.md`.

> **12 — CRM Orientado à Ação ✅ Concluída (2026-08-13).** Activities (Timeline) + Tasks
> (Follow-up) + Dashboard operacional determinístico.
> - ✅ **DB**: novas migrations **V039** (`activities` + `tasks`: company_id, vínculos
>   contact/opportunity **nullable**, enums, índices, RLS FORCE + `tenant_isolation_policy`)
>   e **V040** (permissões `activity:*`/`task:*` + `dashboard:operational`, grants via loop
>   dinâmico da V034).
> - ✅ **Backend** (módulos `activity`/`task`/`dashboard`): `ActivityService`/`TaskService`
>   (TenantContext isolado, validação de ownership contact/opportunity, auditoria
>   `AuditModule.ACTIVITIES`/`TASKS`, transições de status com regra de task concluída),
>   `DashboardService` (inteligência determinística: dias parado ≥7 = stale, score por
>   valor+probabilidade+tempo parado, sugestão de follow-up, tarefas de hoje, atividades
>   recentes); controllers `/activities`, `/tasks`, `/dashboard/operational` com
>   `@PreAuthorize('activity:*'/'task:*'/'dashboard:operational')` + `requireCompanyAccess`.
> - ✅ **Frontend**: features `activities` (timeline + diálogo) e `tasks` (lista com
>   transições) + **dashboard reescrito** estático→orientado à ação (KPIs reais, lista de
>   atenção, tarefas de hoje, atividades recentes) gated por permissões; Sidebar + rota
>   `/tasks` e `/activities`.
> - ✅ **Testes** (**validados 2026-08-13**): backend **281** (antes 267; +`ActivityServiceTest`,
>   `TaskServiceTest`, `DashboardServiceTest`, `ActivityTaskIsolationIT` RLS real em
>   `activities`/`tasks`), frontend **114** (antes 106; +activity.schema/task.schema) —
>   suíte verde + typecheck/lint (novos arquivos) OK.
> - ⚠️ **Débito**: E2E autenticado manual herdado; IA/Inbox/Workflow para Sprints 16/17 (modelo já preparado).
> - ✅ **Deploy + validação VPS**: `git pull --ff-only` + rebuild backend/frontend + `up -d`,
>   **V039/V040 aplicadas** (Flyway `v040`), backend 0 ERROR, `/activities`/`/tasks` 307→login,
>   endpoints registrados, serviços healthy.
> - 📄 `sprints/12/REPORT.md`.

> **13 — CRM · Automação de Workflows ✅ Concluída (2026-08-14).** Motor de automação determinístico
> (sem IA) que responde "o que fazer automaticamente quando um evento do CRM acontece?".
> - ✅ **DB**: `V041__workflow_tables.sql` (tabelas `workflows`/`workflow_conditions`/
>   `workflow_actions`/`workflow_executions`, RLS FORCE, chave única de idempotência
>   `(company_id, workflow_action_id, event_id)`, insert nativo `ON CONFLICT DO NOTHING`) e
>   `V042__workflow_permissions.sql` (`workflow:create/read/update/delete`).
> - ✅ **Backend**: `Workflow`/`WorkflowCondition`/`WorkflowAction`/`WorkflowExecution` + enums
>   (`TriggerEvent`, `ActionType`, `ExecutionStatus`, `ConditionOperator`); `WorkflowService`
>   (CRUD + ativar/desativar + execuções), `WorkflowConditionEvaluator` (campos fechados),
>   `WorkflowActionRunner` (ação em `REQUIRES_NEW` + idempotência), `WorkflowExecutor` (guarda de
>   recursão); eventos `WorkflowTriggerEvent` publicados por Opportunity/Task/Activity;
>   `WorkflowTemplateSeeder` (seeds por empresa); `WorkflowController` com `@PreAuthorize`
>   `workflow:*` + empresa ativa; `RoleSeedService` atualizado (ADMIN/MANAGER CRUD, AGENT/VIEWER leitura).
> - ✅ **Frontend**: feature `workflows` (types/service/hooks/schema Zod + `WorkflowTable`/
>   `WorkflowForm`/`WorkflowExecutionsPanel`/`DeleteWorkflowDialog`) e rotas `/workflows`, gated por
>   `workflow:read`; `tsc --noEmit` e `next lint` limpos.
> - ✅ **Qualidade**: backend **315** testes (incluindo `WorkflowIsolationIT` RLS/idempotência),
>   frontend **128** testes.
> - ✅ **Deploy + validação VPS**: `git pull --ff-only` + rebuild, **V041/V042 aplicadas**
>   (Flyway `v042`), topologia canônica `docker/docker-compose.yml` (rede `crm-network`,
>   backend 8081 / frontend 3000), backend 0 ERROR, `/workflows` 307→login, endpoints 401 sem sessão.
> - ⚠️ **Débito**: E2E autenticado manual herdado; Inbox/IA/WebSocket para sprints futuras.
> - 📄 `sprints/13/REPORT.md`.

> **14 — CRM · Automação de Workflows (disparo por inatividade) ✅ Concluída (2026-08-14).**
> Continuação da 13.
> - ✅ **Novo — disparo por inatividade (`OPPORTUNITY_STALE`)**: novo trigger no enum; factory
>   `WorkflowTriggerEvent.opportunityStale` com `context.opportunity.daysWithoutActivity` e
>   `eventId` determinístico (idempotente entre varreduras); `WorkflowStaleOpportunityScanner`
>   (cron diário `0 0 7 * * *`, configurável) que detecta oportunidades em aberto sem atividade há
>   7+ dias e publica o evento; `WorkflowSchedulingConfig` (`@EnableScheduling`); template seed
>   "Follow-up após oportunidade parada" + novo `WorkflowStaleOpportunityScannerTest` (3 casos).
> - ✅ **Qualidade** (**validado 2026-08-14**): backend **318** testes (antes 315; +3 scanner) —
>   suíte completa **318/318 verdes**; `WorkflowIsolationIT` (Testcontainers/RLS/idempotência) para CI;
>   frontend 128 testes verdes.
> - ✅ **Deploy + validação VPS**: rebuild + `up -d`, **V041/V042 aplicadas** (Flyway `v042`),
>   backend 0 ERROR, `/workflows` 307→login, endpoints protegidos 401 sem sessão, serviços healthy.
> - ⚠️ **Débito**: E2E autenticado manual herdado; Inbox de eventos e integração IA (recomendação de
>   ações), WebSocket de notificações e métricas/rate limits de automação para sprints futuras.
> - 📄 `sprints/14/REPORT.md`.

> **15 — CRM · Auditoria do Workflow Automation (FASE 1) ✅ Concluída (2026-08-14).**
> Auditoria do que já existe em Workflow (Sprints 13/14) antes de implementar melhorias de
> observabilidade, categorizando cada item como `JÁ EXISTE` / `PARCIALMENTE` / `NÃO EXISTE` /
> `NÃO É NECESSÁRIO` nas dimensões domínio, aplicação, persistência, eventos, frontend e testes.
> - 📄 `sprints/15/AUDIT.md`.

## CRM

| Sprint | Nome | Status | Data | Responsável | Dependência |
|--------|------|--------|------|-------------|-------------|
| 9 | User & Permission Management (Contatos CRUD) | ✅ Concluída | 2026-08-12 | AI Agent | 8.6 |
| 10 | Leads | ✅ Concluída | 2026-08-13 | AI Agent | 9 |
| 11 | Pipeline | ✅ Concluída | 2026-08-13 | AI Agent | 10 |
| 12 | CRM orientado à ação (Activities/Tasks/Dashboard) | ✅ Concluída | 2026-08-13 | AI Agent | 11 |
| 13 | CRM — Automação de Workflows (base) | ✅ Concluída | 2026-08-14 | AI Agent | 12 |
| 14 | CRM — Workflows (disparo por inatividade OPPORTUNITY_STALE) | ✅ Concluída | 2026-08-14 | AI Agent | 13 |
| 15 | CRM — Auditoria do Workflow Automation (FASE 1) | ✅ Concluída | 2026-08-14 | AI Agent | 14 |

## Omnichannel

| Sprint | Nome | Status | Data | Responsável | Dependência |
|--------|------|--------|------|-------------|-------------|
| 16 | WhatsApp | ✅ Concluída | 2026-08-23 | AI Agent | 12 |
| 17 | Campanhas | ✅ Concluída | 156b2d9/e77dea8 | V055–V062: CRUD + ciclo de vida, templates, dispatcher WhatsApp, execução agendada idempotente, RLS + isolation ITs, frontend /campaigns (lista/wizard/360); CI/CD GREEN e deploy VPS validado | 16 |
| 18 | Automações | ✅ Concluída | 792d3ab | Extensão do motor Workflow: triggers CONTACT_CREATED/LEAD_STATUS_CHANGED/CAMPAIGN_COMPLETED, operadores CONTAINS/IS_NULL/IS_NOT_NULL, ações SEND_NOTIFICATION/EXECUTE_CAMPAIGN; CI/CD GREEN e deploy VPS validado | 17 |

## Analytics

| Sprint | Nome | Status | Data | Responsável | Dependência |
|--------|------|--------|------|-------------|-------------|
| 19 | Analytics | ✅ Concluída | 2350658/17f1c8f | V063 analytics:read; endpoint agregado /analytics/summary (~19 KPIs SQL, comparacao temporal, serie diária); frontend /reports com recharts; AnalyticsIsolationIT PASS; CI/CD GREEN e deploy VPS validado | 18 |

## IA

| Sprint | Nome | Status | Data | Responsável | Dependência |
|--------|------|--------|------|-------------|-------------|
| 20 | IA | ✅ Concluída | 2026-08-23 | AI Agent | 12 |

---

## Resumo

| Fase | Total | ✅ Concluída | 🚧 Em andamento | ⏳ Pendente | ↪️ Absorvida |
|------|-------|-------------|-----------------|-------------|--------------|
| Planejamento | 3 | 3 | 0 | 0 | 0 |
| Knowledge Layer | 3 | 3 | 0 | 0 | 0 |
| Infraestrutura | 5 | 0 | 0 | 0 | 5 |
| Segurança | 12 | 12 | 0 | 0 | 0 |
| Identidade / Autenticação | 6 | 6 | 0 | 0 | 0 |
| SaaS | 7 | 7 | 0 | 0 | 0 |
| CRM | 8 | 8 | 0 | 0 | 0 |
| Omnichannel | 3 | 1 | 0 | 2 | 0 |
| Analytics | 1 | 0 | 0 | 1 | 0 |
| IA | 1 | 1 | 0 | 0 | 0 |
| **Total** | **49** | **41** | **0** | **3** | **5** |

---

## Definition of Done (obrigatório para toda Sprint)

Uma Sprint **somente** pode receber **`✅ Concluída`** quando **todos** os itens abaixo
forem atendidos (e, ao concluir, **a atualização do `SPRINT_INDEX.md` faz parte da própria
Sprint** — nunca é tarefa posterior/opcional):

- [ ] Implementação concluída
- [ ] Testes concluídos (suítes verdes)
- [ ] Build validado
- [ ] Integração validada
- [ ] E2E realizado quando aplicável
- [ ] Produção / VPS validada quando aplicável
- [ ] Documentação atualizada (inclui `sprints/[N]/REPORT.md` etc.)
- [ ] Migrações validadas quando aplicável
- [ ] Git commit realizado
- [ ] Working tree limpo/validado
- [ ] Débitos conhecidos registrados
- [ ] `SPRINT_INDEX.md` atualizado (status ✅, data real, responsável, resumo e última atualização)


**Fluxo obrigatório (não iniciar a próxima Sprint antes do fim da anterior):**

```
Implementar → Testar → Validar → Documentar → Commit → Atualizar SPRINT_INDEX.md → Marcar ✅ Concluída → Iniciar próxima Sprint
```

---

> **Governança:** uma implementação só é considerada sprint concluída quando código, testes, documentação, índice, CI/CD e deploy/validação na VPS estiverem consistentes.
> **Entrega funcional (sem sprint):** Notificações In-app — implementada e em produção; ver `sprints/notifications/REPORT.md`.

*última atualização: 2026-08-23 — Sprint 16 (WhatsApp) e Sprint 20 (IA) concluídas; regularização documental pós-Sprint 16.*
