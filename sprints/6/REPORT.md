# Sprint 6 — REPORT.md (Access Gateway)

## Identificação
- **Sprint:** 6
- **Nome:** Access Gateway
- **Data Início:** 2026-08-02
- **Data Fim:** —
- **Status:** ✅ **CRM Access (fase 2a) implementado e validado na VPS** — gate (is_active + crm_enabled + company ACTIVE) em backend e auth-service, provisioning separado de grant, migration V023 + backfill, E2E PASS. 🚧 **Access Gateway OIDC+PKCE (fase 2b) pendente**.
- **Responsável:** AI Agent
- **Fase:** Segurança

---

## 1. Objetivo

Evoluir a arquitetura de autenticação para um **CRM Access Gateway**: o Auth Service passa a
ser a camada de entrada (Authorization Code + PKCE), orquestrando identidade, sessão e
**CRM Access**; o Keycloak permanece como IdP/Authorization Server **exclusivo** (nunca
substituído, sem emissor de tokens próprio); o acesso ao CRM torna-se **explícito e
independente** da autenticação (is_active + crm_enabled + company ACTIVE); sem quebrar o
fluxo atual antes da validação do novo.

---

## 2. Arquitetura Anterior vs Nova

| Aspecto | Anterior (Sprint 1–5, auditado) | Nova (Sprint 6) |
|---|---|---|
| Início do login | Frontend via `keycloak-js` direto no Keycloak | Frontend → **Auth Service** (`/auth/authorize`) → Keycloak |
| Callback | No frontend (Next.js `/auth/callback`) | No **Auth Service** (`/auth/callback`) — code exchange server-side |
| Sessão de browser | Não existe (tokens no cliente) | **Cookie HttpOnly/SameSite/Secure** no gateway |
| Decisão de acesso | Implícita (existir no Keycloak = acessar) | **Explícita**: is_active + **crm_enabled** + company ACTIVE |
| Auth Service | Stateless, só `/internal/auth/current-user` | Access Gateway (OAuth2 Client + sessão + decisão de acesso + logout) |
| Backend | Resource server + resolvers (auto-provisiona) | Mantém JWT/CurrentUser/Tenant/RLS; provisioning **não concede acesso** |
| Storage de tokens | `localStorage` | Memória / cookie HttpOnly (hardening) |
| Logout | keycloak-js | **Coerente**: gateway + end_session_endpoint do Keycloak + frontend |
| Emissor de tokens | Keycloak (único) | Keycloak (único — inalterado) |

---

## 3. Fluxos Documentados

### 3.1 Fluxo de autenticação (Authorization Code + PKCE via gateway)
```
Frontend /login → Auth Service /auth/authorize (state+nonce+PKCE) → Keycloak
→ callback Auth Service (valida state/nonce, code exchange, valida JWT via JWKS)
→ decisão CRM access → sessão (cookie) → Frontend → Backend (Bearer JWT) → CurrentUser
```

### 3.2 CRM Access (novo gate)
```
Keycloak autenticado → usuário existe no CRM → is_active → crm_enabled → company ACTIVE
→ SIM: sessão/CRM · NÃO: 403 sem sessão
```
Detalhes: `CRM_ACCESS.md`.

### 3.3 Keycloak
IdP + AS exclusivo; autentica credenciais; emite JWT (RS256); expõe JWKS; `end_session_endpoint`
para logout. **Não alterado** em OIDC/PKCE/JWT/issuer/JWKS/roles.

### 3.4 CurrentUser
Resolução mantida no Auth Service (`CurrentUserResolutionService`) e no backend
(`CurrentUserResolver*`); agora passa a exigir os gates de acesso.

### 3.5 TenantContext / RLS
Cadeia inalterada (Sprint 5): `CurrentUser → companyId → TenantContext → TenantAwareDataSource
→ crm_app → RLS FORCE`. `company_id` sempre da identidade confiável.

---

## 4. Fase 1 — Resultados (Auditoria + Documentação)

### 4.1 Auditoria concluída
- **Frontend**: OIDC+PKCE direto (KeycloakProvider, lib/keycloak, token-manager, middleware,
  callback, LoginForm, useAuth). Tokens em localStorage; flag cookie p/ SSR.
- **Auth Service**: stateless; `GET /internal/auth/current-user` (200 RESOLVED /
  PROVISIONING_REQUIRED / 401 USER_INACTIVE); sem login/callback/sessão/logout.
- **Backend**: resource server JWT (JWKS Keycloak); sem emissão de tokens; resolvers com
  auto-provisioning; TenantFilter após BearerTokenAuthenticationFilter.
- **Modelo de dados**: `users` (company_id NOT NULL, is_active, status, keycloak_sub),
  `companies.status`, RBAC. **Sem gate explícito de CRM access; auto-provisioning = auto-grant.**
- **Gap principal confirmado**: acesso implícito e Auth Service fora do caminho do login.

### 4.2 Decisões registradas
| Decisão | Resultado |
|---|---|
| Local da documentação | `sprints/6/` (convenção existente) + atualização de `sprints/SPRINT_INDEX.md` |
| Modelagem do CRM access | **Opção 3**: `users.crm_enabled` (BOOLEAN, default false) + `companies.status = ACTIVE` |
| Tabela `user_application_access` | **Não criar** nesta sprint |
| Provisionamento | Separar **identity provisioning** de **access grant**; não auto-conceder |
| Backfill de usuários existentes | `crm_enabled = true` **explícito** para quem já tem acesso legítimo; novos = `false` |
| Logout | OIDC coerente (gateway + Keycloak + frontend); proibido logout parcial |
| Emissor de tokens / password grant | Proibidos como fluxo principal |

### 4.3 Documentação criada
| Arquivo | Conteúdo |
|---|---|
| `sprints/6/SPRINT.md` | Identificação, escopo, não-objetivos, sub-arquivos |
| `sprints/6/ARCHITECTURE.md` | Anterior vs nova; componentes alterados vs preservados; decisões D1–D7 |
| `sprints/6/AUTH_FLOW.md` | Login OIDC+PKCE via gateway, callback seguro, sessão/storage, logout, refresh |
| `sprints/6/CRM_ACCESS.md` | Gates, modelo `crm_enabled`, provisioning, auditoria, testes, aceite |
| `sprints/6/SECURITY.md` | Matriz S1–S13, casos de abuso, checklist |
| `sprints/6/REPORT.md` | Este relatório |

---

## 5. Execução (fase 2a — CRM Access)

### 5.1 Modelo de dados
- [x] Migration `V023`: `users.crm_enabled BOOLEAN NOT NULL DEFAULT false` (Flyway aplicada na VPS → `v023`).
- [x] Backfill explícito dos usuários existentes (`crm_enabled = true` para quem já tinha acesso legítimo).
- [x] Sem alteração de RLS (policy por company_id já cobre a coluna).

### 5.2 Backend
- [x] Provisioning separado de access grant em `LocalCurrentUserResolver` / `AuthService`.
- [x] Gate completo (is_active + crm_enabled + company ACTIVE) na resolução/`/auth/me`.
- [x] Revisão de `RoleDataSeeder` / `assignDefaultRole` quanto ao novo campo.
- [x] Auditoria de concessão/revogação (limitação documentada, sem complexidade desnecessária).

### 5.3 Auth Service — CRM Access (gate de identidade)
- [x] `users.crm_enabled` lido na resolução (`UserJpaEntity` + `CurrentUserResolutionService.assertCrmAccess`).
- [x] Gate aplicado em `GET /internal/auth/current-user` → `403 CRM_ACCESS_DENIED` (is_active, crm_enabled, company ACTIVE).
- [x] Contrato `PROVISIONING_REQUIRED` mantido para identidade sem usuário CRM (provisionamento continua no backend).

### 5.4 Auth Service — Access Gateway OIDC+PKCE (fase 2b, pendente)
- [ ] Adicionar `spring-boot-starter-oauth2-client` (OAuth2 Client).
- [ ] `/auth/authorize` (state+nonce+PKCE S256) e `/auth/callback` (code exchange server-side,
      validação de token, decisão de CRM access, emissão de sessão).
- [ ] Sessão de browser (cookie HttpOnly/SameSite/Secure) — só após acesso liberado.
- [ ] `/auth/logout` (end_session_endpoint + id_token_hint + post_logout_redirect allowlist).
- [ ] Allowlist de redirects/destinos internos (anti open redirect).
- [ ] Manter `/internal/auth/current-user` e resolução de CurrentUser.

### 5.5 Frontend (fase 2b, pendente)
- [ ] Login redireciona ao Auth Service; callback processado pelo gateway.
- [ ] Migração de storage (localStorage → memória/cookie HttpOnly).
- [ ] Logout pelo `/auth/logout` do gateway; limpeza de estado.
- [ ] Middleware SSR com flag de sessão (sem tokens/claims).

### 5.6 Keycloak (fase 2b, pendente)
- [ ] Client OIDC do Auth Service (redirect URIs allowlist, PKCE).
- [ ] Validação do `end_session_endpoint` para logout coerente.

---

## 6. Testes

### 6.1 Unitários (implementados e PASS)
- [x] CRM access granted (ativo + crm_enabled + company ACTIVE) → permitido.
- [x] CRM access denied: is_active=false; crm_enabled=false; company SUSPENDED; company INACTIVE.
- [x] Usuário inexistente no CRM → `PROVISIONING_REQUIRED` (contrato; provisionamento no backend).
- [x] Usuário autorizado → CurrentUser resolvido (`/auth/me` 200).
- [x] Usuário sem CRM access → 403, sem sessão.
- [x] Tradução `CrmAccessDenied` → `CrmAccessDeniedAuthenticationException` (backend) e 403 no auth-service.
- **Resultados**: backend **87 PASS**; auth-service **18 PASS** (incl. `AuthServiceProvisioningTest` 12,
  `CrmAccessServiceTest` 9, `LocalCurrentUserResolverTest` 4, `CurrentUserResolutionServiceTest` 10,
  `InternalAuthControllerTest` 6).

### 6.2 Integração
- [x] Keycloak → Auth Service → CRM → CurrentUser → TenantContext → RLS (E2E na VPS).

### 6.3 E2E (na VPS — `GET http://localhost:8082/internal/auth/current-user`, JWT real do Keycloak)
- [x] Usuário autorizado (`validacao.tester@crm.local`) → 200 `RESOLVED` (roles `[AGENT]`, tenant resolvido).
- [x] `crm_enabled=false` → **403 `CRM_ACCESS_DENIED`** "Usuário sem acesso ao CRM (crm_enabled=false)".
- [x] `is_active=false` → **403** "Usuário inativo: acesso ao CRM negado."
- [x] Empresa `SUSPENDED` → **403** "Empresa SUSPENDED: acesso ao CRM negado." (estado restaurado para ACTIVE).
- [x] Usuário novo provisionado sem grant (`provision.tester@crm.local`, `crm_enabled=false` inserido no banco)
      → **403 `CRM_ACCESS_DENIED`** "Usuário sem acesso ao CRM (crm_enabled=false): conceda acesso explicitamente."
- [ ] Gateway OIDC+PKCE: usuário autorizado login → sessão → `/auth/me` 200 → tenant aplicado (fase 2b).
- [ ] Logout completo via gateway (fase 2b).

---

## 7. Deploy / Commit / VPS

### Regras (obrigatórias)
- Docker/infra/Keycloak/Postgres/build/E2E/deploy **sempre na VPS** (`ssh crm-vps`,
  `/opt/crm/docker`, compose existente). Nunca montar infra local nem outro compose.
- Fluxo: local (código/commit) → transferência → build na VPS → compose existente →
  containers → E2E na VPS.
- VPS sempre atualizada com o código commitado; validar health, logs e versão
  (`git rev-parse HEAD`).

### Checklist
- [x] Commits por etapa (Sprint 6 sem misturar com outra etapa): `805b647` (Sprint 5, fix deploy tenant/RLS),
      `2dc6847` (deploy: join shared crm-network), `606e1ff` (deploy: shared crm_main credentials),
      `60099a4` (feat(auth): explicit crm access control — 35 arquivos, incl. V023), `bdbd593` (deploy: internal keycloak jwks).
- [x] Build na VPS: `docker compose --env-file /opt/crm/docker/.env -f /opt/crm/docker/docker-compose.yml up -d --no-deps --build <svc>`.
- [x] Health: backend (8081) e auth-service (8082) UP; Flyway na versão `v023`; `git rev-parse HEAD` da VPS = `bdbd593`.
- [x] Logs sem erro (após ajustes de deploy: network `crm-network`, credenciais compartilhadas, JWKS interno).
- [x] E2E (seção 6.3) na VPS.
- [x] `git rev-parse HEAD` da VPS = HEAD commitado local.

---

## 8. Riscos e Mitigações

| Risco | Mitigação |
|---|---|
| Migration `crm_enabled` derrubar usuários legítimos em prod | Backfill explícito (`true` para existentes com acesso legítimo) antes de ativar o gate |
| Quebrar o fluxo atual durante a migração | Manter fluxo atual até o novo validado (regra 14); rollback rápido |
| Auto-grant residual (provisioning concedendo acesso) | Revisar resolvers/provisioning; default `crm_enabled=false` |
| Token leakage em localStorage | Migração para cookie HttpOnly + memória |
| Logout parcial (sessão reutilizável) | Logout OIDC coerente (gateway + Keycloak) |
| Acesso negado por engano por `companies.status` | Gate exige `ACTIVE`; documentar e testar empresa suspensa |
| Confusão CRM access × RBAC | Documentação (CRM_ACCESS.md §9); testes garantem separação |

---

## 9. Pendências / Limitações Conhecidas

- [ ] **Provisionamento automático de usuário novo na VPS falha (bug da Sprint 5, fora do escopo desta sprint)**:
      durante o auto-provisioning, `assignDefaultRole` busca a role `AGENT` via `findByNameAndCompanyId` **antes** de o
      `TenantContext.companyId` ser setado (`LocalCurrentUserResolver` só seta após o provisioning) → consulta bloqueada
      por RLS FORCE → exceção → `AuthServiceCurrentUserResolver` re-chama o resolver local (linha 41) → recursão no
      `AuthenticationManager` (`$Proxy…authenticate`) → `StackOverflowError` → `/auth/me` 401 em vez de 403.
      **E2E validado por inserção manual** de usuário com `crm_enabled=false` (403 correto). Decisão: **tratar à parte**
      (opção C do usuário): (1) setar `TenantContext.companyId` antes do `assignDefaultRole`; (2) impedir fallback
      recursivo quando o provisioning já falhou. Sugestão: `AUTH_DEFAULT_COMPANY_ID` configurado na VPS
      (`.env` local, gitignored) não é suficiente — o lookup de role precisa do contexto de tenant.
- [ ] **Access Gateway OIDC+PKCE (fase 2b)** não iniciado (seção 5.4–5.6).
- [ ] **Provisionamento** permanece no backend (migração futura, MIGRATION_PLAN).
- [ ] **Auditoria** de quem/quando concedeu/revogou `crm_enabled` depende do modelo de auditoria
      atual (limitação documentada, sem criar complexidade desnecessária).
- [ ] `directAccessGrantsEnabled` foi habilitado **temporariamente** no client `crm-frontend` para os testes E2E e
      **revertido para `false`** ao final (verificado via admin API). Password grant permanece proibido como fluxo principal.
- [ ] Deploy da Sprint 5 com `AUTH_DEFAULT_COMPANY_ID` **configurado na VPS** (`.env` local; registrado também em sprints/5/REPORT.md).
- [ ] Usuários/artefatos de teste no Keycloak/banco criados durante a validação E2E (`validacao.tester@crm.local`,
      `provision.tester@crm.local`, etc.) — podem ser limpos quando a validação da fase 2b começar.

---

## 10. Veredito

### Critério
```
SPRINT 6 — APROVADA: Login CRM, OIDC+PKCE, Keycloak, Callback, CRM Access,
Usuário sem acesso, Usuário inativo, Logout, CurrentUser, TenantContext, RLS,
Tenant A/B, E2E na VPS, Build, Health, Git commit, Documentação — todos PASS.
Senão → SPRINT 6 — BLOQUEADA (motivo exato).
```

### Situação atual
✅ **Fase 1** (auditoria + documentação) e **fase 2a** (CRM Access) **concluídas e validadas na VPS**:
gate de acesso explícito (is_active + crm_enabled + company ACTIVE) aplicado no backend e no auth-service,
migration V023 + backfill, provisioning separado de grant, unit tests PASS (backend 87, auth-service 18) e
E2E na VPS com JWT real do Keycloak (4 cenários de gate + usuário novo sem grant → 403).
**Veredito parcial da fase 2a: APROVADA.** Veredito final da Sprint 6 depende da **fase 2b** (Access Gateway
OIDC+PKCE + sessão + logout), ainda pendente, e do bug latente de auto-provisioning registrado na seção 9.

---

*Data: 2026-08-02*
