# Sprint 6 — REPORT.md (Access Gateway)

## Identificação
- **Sprint:** 6
- **Nome:** Access Gateway
- **Data Início:** 2026-08-02
- **Data Fim:** —
- **Status:** 🚧 Em andamento (**fase 1 — auditoria + documentação concluída**; implementação pendente)
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

## 5. Pendências de Implementação (fase 2 em diante)

### 5.1 Modelo de dados
- [ ] Migration `V023` (ou número sequencial real): `users.crm_enabled BOOLEAN NOT NULL DEFAULT false`.
- [ ] Backfill explícito dos usuários existentes (regra de concessão documentada).
- [ ] Sem alteração de RLS (policy por company_id já cobre a coluna).

### 5.2 Backend
- [ ] Separar provisioning de access grant em `LocalCurrentUserResolver` / `AuthService`.
- [ ] Aplicar gate completo (is_active + crm_enabled + company ACTIVE) na resolução/`/auth/me`.
- [ ] Revisar `RoleDataSeeder` / `assignDefaultRole` quanto ao novo campo.
- [ ] Auditoria de concessão/revogação (limitação documentada, sem complexidade desnecessária).

### 5.3 Auth Service (Access Gateway)
- [ ] Adicionar `spring-boot-starter-oauth2-client` (OAuth2 Client).
- [ ] `/auth/authorize` (state+nonce+PKCE S256) e `/auth/callback` (code exchange server-side,
      validação de token, decisão de CRM access, emissão de sessão).
- [ ] Sessão de browser (cookie HttpOnly/SameSite/Secure) — só após acesso liberado.
- [ ] `/auth/logout` (end_session_endpoint + id_token_hint + post_logout_redirect allowlist).
- [ ] Allowlist de redirects/destinos internos (anti open redirect).
- [ ] Manter `/internal/auth/current-user` e resolução de CurrentUser.

### 5.4 Frontend
- [ ] Login redireciona ao Auth Service; callback processado pelo gateway.
- [ ] Migração de storage (localStorage → memória/cookie HttpOnly).
- [ ] Logout pelo `/auth/logout` do gateway; limpeza de estado.
- [ ] Middleware SSR com flag de sessão (sem tokens/claims).

### 5.5 Keycloak
- [ ] Client OIDC do Auth Service (redirect URIs allowlist, PKCE).
- [ ] Validação do `end_session_endpoint` para logout coerente.

---

## 6. Testes (a implementar)

### 6.1 Unitários
- [ ] CRM access granted (ativo + crm_enabled + company ACTIVE) → permitido.
- [ ] CRM access denied: is_active=false; crm_enabled=false; company SUSPENDED; company INACTIVE.
- [ ] Usuário inexistente no CRM → negado/provisionamento conforme regra.
- [ ] Usuário autorizado → CurrentUser resolvido (`/auth/me` 200).
- [ ] Usuário sem CRM access → 403, sem sessão.
- [ ] Token inválido/expirado; issuer inválido; state inválido; PKCE inválido.
- [ ] Logout completo (sessões encerradas).

### 6.2 Integração
- [ ] Keycloak → Auth Service → CRM → CurrentUser → TenantContext → RLS.

### 6.3 E2E (na VPS)
- [ ] Usuário autorizado: login → sessão → `/auth/me` 200 → tenant aplicado.
- [ ] Usuário sem CRM access: DENIED (403, sem sessão).
- [ ] Usuário inativo: DENIED.
- [ ] Empresa suspensa: DENIED (todos os usuários).
- [ ] Tenants A/B reais com isolamento (Sprint 5).

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
- [ ] Commits por etapa (sem misturar Sprint 6 em commits de outra etapa).
- [ ] Build na VPS: `docker compose --env-file /opt/crm/docker/.env -f /opt/crm/docker/docker-compose.yml up -d --build`.
- [ ] Health: backend, auth-service, keycloak, frontend, postgres UP.
- [ ] Logs sem erro; Flyway em nova versão (V023+).
- [ ] E2E (seção 6.3) na VPS.
- [ ] `git rev-parse HEAD` da VPS = HEAD commitado local.

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

- [ ] **Implementação** da Sprint 6 ainda não iniciada (fase 1 = auditoria + documentação).
- [ ] **Provisionamento** permanece no backend (migração futura, MIGRATION_PLAN).
- [ ] **Auditoria** de quem/Quando concedeu/revogou `crm_enabled` depende do modelo de auditoria
      atual (limitação a documentar, sem criar complexidade desnecessária).
- [ ] Sprint 5 pendências não commitadas permanecem no working tree (não pertencem à Sprint 6).
- [ ] Deploy da Sprint 5 com `AUTH_DEFAULT_COMPANY_ID` ainda pendente na VPS (registrado em sprints/5/REPORT.md).

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
🚧 **Fase 1 concluída** (auditoria + documentação). Implementação pendente — veredito a ser
emitido ao final, com todas as evidências acima.

---

*Data: 2026-08-02*
