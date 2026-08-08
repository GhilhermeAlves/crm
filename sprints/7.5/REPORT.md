# Sprint 7.5 — Recuperação de conta: reset de senha real no Keycloak

**Data:** 2026-08-08 — **Ambiente:** local + VPS (produção `srv1348261.hstgr.cloud`) — **Status:** ✅ Concluída

## Identificação

- **Sprint:** 7.5
- **Nome:** Recuperação de conta — `forgot-password`/`reset-password` com reset REAL da credencial no Keycloak
- **Responsável:** AI Agent
- **Fase:** Segurança — Identidade / Keycloak
- **Dependência:** Sprints 6.x (Gateway/OIDC/sessão/RLS) + 7.2 (account linking) + 7.3 (telefone/OTP) + 7.4 (login por telefone)

## Objetivo

Implementar e validar em produção o fluxo público de recuperação de conta:

- `POST /api/v1/auth/forgot-password` — recebe o e-mail, gera um token de reset de curta vida e o entrega via `EmailService` (o token nunca é logado por padrão).
- `POST /api/v1/auth/reset-password` — recebe o token + nova senha, valida o token (não usado e não expirado) e redefine a credencial:

  - Conta com `keycloak_sub` → o auth-service redefine a senha **no Keycloak** via Admin REST;
  - Conta legada local (pré identity-layer) → hash próprio atualizado em `users.password_hash`.

## Arquitetura

| Componente | Papel |
|---|---|
| nginx | `location /api/v1/auth/forgot-password` e `location /api/v1/auth/reset-password` → `proxy_pass http://localhost:8081` (direto ao `crm-backend`, público, sem sessão do gateway) — mesmo padrão do bloco phone/OTP |
| `AuthService` (backend) | `forgotPassword(email)` → usa `app.current_identity_email` (V025) para localizar a linha; gera token UUID e define `app.current_reset_token` (V027) para o INSERT; chama `EmailService`. `resetPassword(token,new)` → `findByToken`, valida `isValid()`, delega via `AuthServiceClient` quando `keycloakSub`, senão `user.updatePassword` local; `token.markAsUsed()` |
| `auth-service` | `POST /internal/auth/reset-password` protegido por `InternalApiTokenFilter` (segredo `AUTH_INTERNAL_API_TOKEN`, header `X-Internal-Api-Token`); `CredentialResetClient` redefine a senha via Admin REST do Keycloak |
| `crm-keycloak-admin` | client confidencial no realm `CRM` com `serviceAccountsEnabled`; service account com papel `manage-users` (`realm-management`); credenciais via `AUTH_KEYCLOAK_ADMIN_CLIENT_ID/SECRET` |

## Migrações / RLS

- **V027** — policy de bootstrap por token em `password_reset_tokens` (SELECT/INSERT/UPDATE) e `users` (SELECT/UPDATE) via GUC `app.current_reset_token`; RLS FORCE mantido.
- **V028** — remove a policy `tenant_isolation_policy` (join em `users`) de `password_reset_tokens` (criada na V020). Sem ela, o acesso à tabela é exclusivamente por posse do token, eliminando a recursão infinita (erro 42P17 / `infinite recursion detected in policy for relation "users"`).

## Problemas encontrados e correções

1. **`BeanDefinitionOverrideException` no auth-service (crash loop):** `InternalApiTokenFilter` era `@Component` e também registrado via `FilterRegistrationBean` no `GatewayConfig` — colisão de bean. Correção: removido `@Component`; o bean é criado no `GatewayConfig` (mesmo padrão do `ApiRateLimitFilter`). O teste `InternalAuthControllerTest` passou a registrar o bean via `@TestConfiguration`.
   **Nota:** `@TestConfiguration` fica em `org.springframework.boot.test.context`, não em `org.springframework.context.annotation`.
2. **Recursão infinita do RLS no `forgot-password`:**
   A policy V027 de `users` subconsulta `password_reset_tokens`, cuja policy V020 junta `users` → ciclo (42P17). Migração **V028** remove a policy de join; o fluxo usa o GUC por token. Resultado: `forgot-password` → **202**.
3. **`duplicate key value violates unique constraint "password_reset_tokens_token_key"`:**
   `PasswordResetTokenRepositoryImpl.save()` sempre criava nova entidade (INSERT), mesmo ao marcar o token como usado após o reset. Correção: `save()` preserva o `id` já presente no agregado (carregado via `findByToken`).
4. **`ObjectOptimisticLockingFailureException` no INSERT do `forgot-password`:**
   `PasswordResetToken.create()` pré-atribuía `id = UUID.randomUUID()`, fazendo o `save()` tratá-lo como UPDATE de linha inexistente. Correção: `create()` deixa o `id` nulo (gerado no INSERT); o `id` real chega apenas via `findByToken`.
5. **`AUTH_INTERNAL_API_TOKEN` ausente no container auth-service (401 INTERNAL_API_TOKEN_INVALID):**
   o token foi adicionado ao `.env` e ao bloco do backend, mas o bloco do `auth-service` no compose não repassava a variável. Adicionada e container recriado; `/reset-password` passou a devolver **200** em vez de 500 (401 interno virando erro genérico).
6. **`forgot-password` 500 no primeiro deploy:**
   código antigo sem V027/V028 no fluxo. Após deploy com as migrações corretas → 202.

## E2E em produção (evidências)

1. `POST https://srv1348261.hstgr.cloud/api/v1/auth/forgot-password {"email":"ghilherme007@gmail.com"}` → **202**.
2. Token persistido em `password_reset_tokens` (SELECT direto no Postgres da VPS).
3. `POST /api/v1/auth/reset-password {"token":"<token>","newPassword":"NovaSenha!2026#"}` → **200**.
4. Log do backend: `Credencial de usuário resetada no Keycloak (sub=…)`.
5. No banco, o token ficou com `used = true`.
6. Login no Keycloak com a **nova** senha → OK (token emitido); com a **anterior** → `invalid_grant` (rejeitado). Prova que a credencial REAL foi alterada no Keycloak.
7. Token aleatório desconhecido → **400** `Invalid reset token`; token já usado → **400** `Reset token has expired or already been used` (corrigido: antes era 500 genérico).
8. `GET /auth/health`, `/actuator/health` → UP (backend e auth-service).

## Configuração de produção

- Keycloak: criado o client `crm-keycloak-admin` e atribuído `manage-users` ao service account (via Admin API com admin token do realm `master`). Service account com token `client_credentials` validado (lista de usuários do realm OK).
- `.env` da VPS adicionado:
  `AUTH_KEYCLOAK_ADMIN_CLIENT_ID`, `AUTH_KEYCLOAK_ADMIN_CLIENT_SECRET`, `AUTH_INTERNAL_API_TOKEN`.
- Compose da VPS: envs `AUTH_INTERNAL_API_TOKEN` no `backend` e no `auth-service`; `AUTH_KEYCLOAK_ADMIN_*` no `auth-service`.
- nginx `crm.conf` com os dois novos locations e `nginx -t` OK, recarregado.

## Testes locais

- **Backend:** `mvn test` → suites verdes, incluindo novo `AuthServiceResetPasswordTest` (6 cenários: conta Keycloak delega, legado hash local, token expirado/usado, token desconhecido, usuário não encontrado, limpeza de contexto).
- **Auth-service:** `mvn test` → **246 testes, 0 falhas** (incluindo `InternalAuthControllerTest` com o novo `InternalApiTokenFilter` + `@MockBean CredentialResetClient`).

## Pendências / recomendações

- **Provedor de e-mail real (SMTP)** em produção — hoje o envio usa provedor fake/desenvolvedor em dev; token só logado com `MAIL_LOG_TOKEN=true`.
- Considerar script de bootstrap idempotente do client `crm-keycloak-admin` para novos ambientes.
- Opcional: revocation global de sessões ao resetar senha (roadmap pós-7.5).

## Resultado

STATUS: **CONCLUÍDA** (validade em produção).

- `forgot-password` → 202 e token persistido;
- `reset-password` → 200 com mudança **real** da credencial no Keycloak;
- token inválido/usado → 400 (não mais 500);
- service account dedicado provisionado e utilizado (sem bypass de browser);
- RLS FORCE mantido (GUC por token; V027+V028 aplicadas);
- suítes locais verdes (backend + auth-service) e nginx `nginx -t` OK.

## Próxima sprint (roadmap)

- **Sprint 7 — Empresas** (módulo SaaS) após a entrega da recuperação.