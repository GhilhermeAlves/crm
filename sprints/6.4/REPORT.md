# Sprint 6.4 — Result

**Data:** 2026-08-03/04 · **Ambiente:** `https://srv1348261.hstgr.cloud` (VPS `76.13.237.238`)

## Arquitetura

- **frontend → Gateway** — autenticação delegada ao Access Gateway (auth-service); browser envia apenas o cookie `crm_session` (HttpOnly).
- **cookie-only** — nenhum JWT/token em localStorage/sessionStorage; sessão reconstruída do cookie server-side (Redis).
- **API relay** — auth-service atua como BFF: `/api/**` resolve a sessão e injeta `Authorization: Bearer` server-side; o browser nunca vê o token.
- **backend JWT Resource Server** — crm-backend valida o access token do usuário contra o Keycloak interno (`crm-keycloak:8080`) e resolve `CurrentUser` via camada de identidade (auth-service `/internal/auth/current-user`).

## E2E

Realizado com usuário de teste dedicado provisionado no realm CRM (`e2e.tester@crm.local`, com CRM Access). Fluxo executado via cliente HTTP real (login com o formulário do Keycloak, PKCE S256, cookie jar):

- **login:** PASS — `GET /auth/authorize?redirect=/dashboard` → 302 para o Authorization Endpoint do Keycloak.
- **callback:** PASS — POST de credenciais → 302 `/auth/callback` → 302 `/dashboard`; sessão criada (`crm_session`) + cookie CSRF.
- **sessão:** PASS — cookie `crm_session` presente após o callback.
- **reload:** PASS — sessão persiste; novo "browser" com o mesmo cookie → `GET /api/v1/auth/me` = 200 (sessão em Redis, sem JWT no cliente).
- **API 200:** PASS — `GET /api/v1/auth/me` (cookie) → 200 com `e2e.tester@crm.local`; sem `Authorization` na resposta.
- **refresh:** PASS — `POST /auth/refresh` (cookie + `X-XSRF-TOKEN`) → **204**, corpo vazio, nenhum token devolvido; API segue 200 após refresh.
- **logout:** PASS — `GET /auth/logout` → 302 para `end_session_endpoint` do Keycloak; cookie de sessão expirado; API posterior → 401 `SESSION_NOT_FOUND`.
- **protected route:** PASS — sem sessão `GET /dashboard` → 307 `/login?redirect=%2Fdashboard`; com sessão → 200 (página do app).
- **CRM Access:** PASS — antes do grant: 403 `CRM_ACCESS_DENIED` real (`{"code":"CRM_ACCESS_DENIED","message":"Usuário sem acesso ao CRM (crm_enabled=false)...","status":403}`); usuário não provisionado → 403 `PROVISIONING_REQUIRED`.
- **open redirect:** PASS — `https://evil.example`, `//evil.example`, `https://evil.example/path` → 400 `OPEN_REDIRECT` ("Redirect não permitido pela allowlist."); paths relativos de mesma origem aceitos.

## Segurança

- **localStorage:** somente `sidebar-collapsed` (preferência de UI via `useLocalStorage` em `src/store/sidebar.tsx`) — mantido.
- **sessionStorage:** nenhum uso no código.
- **cookies:** `crm_session` → `Secure; HttpOnly; SameSite=Lax; Path=/` (capturado do header real); `XSRF-TOKEN` → `Secure`, não-HttpOnly (necessário para cookie-to-header). Valor do cookie não registrado.
- **Authorization header:** nunca enviado pelo browser (`api.ts` usa `withCredentials` apenas; teste unitário garante ausência do header); respostas do relay não devolvem `Authorization`.
- **tokens no browser:** nenhum `access_token`/`refresh_token`/`id_token` no bundle, storage ou respostas.
- **Keycloak direto:** zero referências a `/realms/CRM/protocol/openid-connect/token` no bundle frontend; login/callback acontecem via gateway server-side.
- **secrets:** client secret e credenciais apenas via env (`${AUTH_GATEWAY_CLIENT_SECRET}`, `.env`); nada hardcoded no repo; valor de cookie/JWT não logado.
- **logs:** varredura de logs (auth-service e backend) sem vazamento de tokens, secrets ou valores de cookie (apenas nome de coluna `password_hash` em SQL, sem valores).

## Deploy

- **nginx:** `/api/` → `localhost:8082` (relay BFF); `/auth/` → `8082`; `/realms/` → Keycloak; `nginx -t` OK + reload.
- **auth-service:** relay `/api/**` (GatewayApiRelay) + controller; `AUTH_GATEWAY_API_BACKEND_URL=http://backend:8080`; timeouts connect/read configurados.
- **frontend:** imagem rebuildada sem `keycloak-js`; envs `NEXT_PUBLIC_API_URL`/`NEXT_PUBLIC_WS_URL` removidos do compose; bundle novo servido.
- **backend:** correção de deploy — JWKS interno efetivo (`SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://crm-keycloak:8080/...`); o profile `dev` usava `jwk-set-uri` (propriedade legada) com `http://76.13.237.238:8080/...` (IP público, inacessível) que vencia o `jwks-uri` do compose; container recriado.
- **containers:** `crm-backend`, `crm-auth-service`, `crm-frontend`, `crm-keycloak` (healthy), `crm-postgres`, `crm-redis`, `crm-rabbitmq`, `crm-minio` — Up.
- **healthchecks:** Keycloak/postgres/redis/rabbitmq healthy; backend e auth-service sobem com sucesso (Spring Boot started).

## Build

- **lint:** PASS (0 erros; warnings pré-existentes de `<img>`/unused).
- **typecheck:** PASS (`tsc --noEmit`).
- **tests:** PASS — frontend 28/28 (4 arquivos); auth-service `mvn clean verify` 159/159 (inclui 13 novos de relay).
- **build:** PASS — `next build` (standalone) e `mvn package`.

## Arquivos alterados

**auth-service (relay):**
- `src/main/java/com/becommerce/auth/infrastructure/gateway/GatewayApiRelay.java` (novo)
- `src/main/java/com/becommerce/auth/presentation/rest/ApiRelayController.java` (novo)
- `src/test/java/com/becommerce/auth/infrastructure/gateway/GatewayApiRelayTest.java` (novo)
- `src/test/java/com/becommerce/auth/presentation/rest/ApiRelayControllerTest.java` (novo)
- `src/main/java/com/becommerce/auth/infrastructure/config/SecurityConfig.java`
- `src/main/java/com/becommerce/auth/infrastructure/gateway/OidcGatewayProperties.java`
- `src/main/resources/application.yml`
- `.env.example`

**frontend (migração Gateway):**
- `src/lib/gateway-auth.ts` + `gateway-auth.test.ts` (novos)
- `src/lib/api.ts` + `api.test.ts`
- `src/lib/middleware-auth.ts` + `middleware-auth.test.ts`
- `src/middleware.ts`
- `src/features/auth/hooks/useAuth.tsx` + `useAuth.test.ts`
- `src/components/ProtectedRoute.tsx`
- `src/app/auth/callback/content.tsx`
- `src/features/auth/components/LoginForm.tsx`
- `src/app/(dashboard)/dashboard/page.tsx`
- `src/providers/index.tsx`
- `package.json` / `package-lock.json` (remove `keycloak-js`; eslint 8)
- removidos: `src/lib/keycloak.ts`, `src/lib/jwt.ts`, `src/store/token-manager.ts`, `src/providers/KeycloakProvider.tsx`, `public/silent-check-sso.html` (+ testes)
- lint/typecheck: `src/app/(dashboard)/profile/page.tsx`, `users/page.tsx`, `users/new/page.tsx`, `users/[id]/edit/page.tsx`, `src/components/ui/input.tsx`

**infra:**
- `docker/docker-compose.yml` (JWKS interno legado `jwk-set-uri`)

## Commit

`<hash>` — ver seção final.

## Pendências

- **Defeito pré-existente no backend:** `StackOverflowError` (recursão no proxy de autenticação) quando o JWT é inválido no crm-backend. Retorna 401 corretamente ao cliente; ocorre apenas no path de falha e não afeta os cenários 6.4 (o relay se auto-corrige com refresh+retry). Recomenda-se correção em sprint futura.
- **Usuário de teste** `e2e.tester@crm.local` criado no realm CRM + banco (papel `AGENT`, `crm_enabled=true`) para o E2E; pode ser removido ao final da validação. Credenciais mantidas apenas em `/tmp` da VPS (não versionadas).
- **Inspeção DevTools** foi validada por análise estática do código + verificação HTTP (cookies/headers), não por navegação manual em browser; revisão visual opcional.

## Status

**CONCLUÍDA**
