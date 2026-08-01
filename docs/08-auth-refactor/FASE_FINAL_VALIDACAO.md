# Fase Final — Validação E2E em Produção (Sprint 4)

| | |
|---|---|
| **Data** | 2026-08-01 |
| **Ambiente** | VPS `crm-vps` (`/opt/crm`, `https://srv1348261.hstgr.cloud`) |
| **Versão do código** | Sprint 4 (commit local `091c84d`) aplicado no working tree da VPS |
| **Estado** | Validado — aguardando aprovação (sem push, sem Sprint 5) |

## 1. Resumo

A camada de identidade do Sprint 4 (`auth-service` + backend com `AUTH_IDENTITY_LAYER_ENABLED=true`) foi validada de ponta a ponta em produção, incluindo resolução de identidade, RBAC, provisionamento, auditoria, observabilidade e rollback por flag. Um bug de observabilidade no healthcheck do Keycloak foi encontrado e corrigido.

## 2. Resultados por área

### 2.1 Resolução de identidade (auth-service → backend)

| Cenário | Resultado |
|---|---|
| Usuário CRM existente | `/internal/auth/current-user` → `200 RESOLVED`, `provider=keycloak`, `keycloakSub`, roles `[AGENT]`, 12 permissões de negócio |
| `/api/v1/auth/me` (backend via auth-service) | `200` com id/email/companyId/status corretos |
| Principal no SecurityContext | `CurrentUser[provider=keycloak, keycloakSub, sessionId, permissions]` |
| Sem token | `401` |

### 2.2 RBAC

| Rota | Resultado |
|---|---|
| `/api/v1/auth/me` (AGENT autenticado) | `200` |
| `/api/v1/audit` (AGENT sem `audit:read`) | `403` |
| `/api/v1/users` (AGENT sem `user:read`) | `403` |
| Rota inexistente | `404` JSON padronizado |

### 2.3 Provisionamento

| Cenário | Resultado |
|---|---|
| Usuário Keycloak **sem** registro CRM | auth-service → `PROVISIONING_REQUIRED`; backend log `AuthServiceCurrentUserResolver ... provisionando localmente`; registro criado no DB (`users.keycloak_sub` preenchido, status ACTIVE) |
| Usuário Keycloak **com** registro CRM | auth-service → `RESOLVED` com roles `[AGENT]` |
| Registro provisionado | `novo.crm@crm.local` (sub `09d817c6-...`) criado em `users` |

### 2.4 Auditoria

- Tabela `audit_logs` existe com colunas `user_id/user_name/user_email/action/module/status/created_at`.
- `AuditInterceptor` lê o `CurrentUser` do SecurityContext (resolvido via auth-service) e popula o `AuditContext`.
- **Bug pré-existente (fora do Sprint 4)**: gravação de auditoria falha com `ObjectOptimisticLockingFailureException` (merge em entidade com versão obsoleta) em `AuditEventListener`. O `AuditService` captura e loga o erro, mas o registro não é persistido. Necessita correção futura.

### 2.5 Observabilidade

| Item | Resultado |
|---|---|
| `GET /auth/health` (auth-service, 8082) | `{"status":"UP"}` `200` |
| `GET /actuator/health` (backend, 8081) | `{"status":"UP"}` |
| Frontend | `200` |
| Erro 500 (register com bug `company_id` pré-existente) | JSON padronizado `{"status":500,"error":"Internal Server Error","message":"An unexpected error occurred",...}` — sem vazamento de stack |
| Refresh token Keycloak | válido `200` / inválido `400` |
| Authorization endpoint | redirect válido `302` / redirect inválido (evil.com) `400` (anti open-redirect) |

### 2.6 Correção aplicada: healthcheck do Keycloak

- **Problema**: `docker-compose.yml` (VPS) tinha healthcheck do keycloak apontando para `GET /auth/health` na porta `8080` (rota do **auth-service**, não do Keycloak) → container `unhealthy` permanente (FailingStreak 8546).
- **Correção**: apontado para `GET /health` na porta de management `9000` (endpoint real do Keycloak com `KC_HEALTH_ENABLED=true`, retorna `{"status":"UP"}`).
- **Backup**: `/opt/crm/docker-compose.yml.bak-healthcheck`.
- **Resultado**: `crm-keycloak: Up (healthy)`.

### 2.7 Rollback por flag (documental)

| Flag `AUTH_IDENTITY_LAYER_ENABLED` | Comportamento observado |
|---|---|
| `true` | Backend consulta auth-service: `permissions=[12 permissões]`, `sessionId` preenchido, logs de `AuthServiceCurrentUserResolver` |
| `false` | Backend resolve localmente via JWT: `permissions=[]`, `sessionId=null`, sem chamada ao auth-service |
| `true` (revertido) | Volta a resolver via auth-service (`/me` 200, permissões completas) |

**Regra mantida**: flag permanece `true` na VPS (confirmado via env do container).

## 3. Correção das pendências pré-existentes

Ambas as pendências registradas na Fase Final foram corrigidas e validadas em produção (mesma data).

### 3.1 Optimistic locking na auditoria

- **Causa raiz**: `AuditLog.create()` gera `id = UUID.randomUUID()` no domínio, e `AuditLogRepositoryImpl.toJpaEntity()` copia esse id para `AuditLogJpaEntity`, que usa `@GeneratedValue(strategy = GenerationType.UUID)`. Com o id pré-preenchido, o Spring Data `save()` considera a entidade **não-nova** e chama `em.merge()` (UPDATE) em vez de `em.persist()` (INSERT). O `merge` de um id inexistente gera `StaleObjectStateException` / `ObjectOptimisticLockingFailureException` ("unsaved-value mapping was incorrect"). Não é concorrência real — é update duplicado causado pelo próprio fluxo de id.
- **Correção**: no `AuditLogRepositoryImpl.save()`, se o id não existe no banco, `entity.setId(null)` para o Hibernate gerar via `@GeneratedValue` (INSERT). Mesmo padrão já usado em `UserRepositoryImpl.save()`.
- **Validação em produção**: `forgot-password` → `audit_logs` com `RESET_PASSWORD/SUCCESS/company_id` correto; `register` → `CREATE/USERS`. Zero ocorrências de `Failed to record audit log` no log.

### 3.2 company_id NULL no register

- **Causa raiz**: `RegisterRequest.companyId` é opcional e o frontend envia `""`/null (form de registro sem empresa). `AuthService.register()` usava `request.companyId()` diretamente → `User` com `companyId=null` → viola `users.company_id NOT NULL` (500).
- **Correção**:
  - `AuthService.resolveCompanyForRegistration()`: se `companyId` informado, valida existência via `companyRepository.findById` (senão `IllegalStateException` → 400); se não informado, usa a empresa padrão ativa (`resolveDefaultCompanyId()`, mesmo mecanismo do provisionamento Keycloak).
  - Erros de validação agora retornam `400` (e não `500`) via handler existente de `IllegalStateException`.
  - Frontend (`auth.service.ts`): envia `companyId` apenas quando presente.
- **Validação em produção**: register sem companyId → `201` com `company_id=11111111-...` (company default); register com companyId válido → `201`; company inexistente → `400`; email duplicado → `400`.

### 3.3 Observações remanescentes

- Login via browser (fluxo OIDC/PKCE no frontend) requer interação manual; a infraestrutura OAuth2 (authz endpoint, token endpoint, refresh, redirect URIs) foi validada por API.

## 4. Estado final dos containers

| Container | Status |
|---|---|
| `crm-keycloak` | Up (healthy) |
| `crm-auth-service` | Up |
| `crm-backend` | Up |
| `crm-frontend` | Up |

## 5. Próximos passos

- Aguardar aprovação humana.
- Não fazer push nem iniciar Sprint 5 até aprovação explícita.
