# Sprint 6.5 — Result

**Data:** 2026-08-04 · **Ambiente:** `https://srv1348261.hstgr.cloud` (VPS `76.13.237.238`)

## Objetivo

Hardening, observabilidade e correções do Access Gateway OIDC: tolerância a falhas de Redis,
respostas de erro padronizadas e seguras, correção da recursão do crm-backend no path de JWT
inválido, validação de concorrência de refresh e validação em produção (VPS) dos cenários de
segurança e robustez.

## Alterações

- **auth-service `GlobalExceptionHandler`** — handler explícito para
  `HttpRequestMethodNotSupportedException` → **405 METHOD_NOT_ALLOWED** (antes caía no catch-all →
  500). Erros de infraestrutura são traduzidos para `{status, code, error, message, timestamp}` e
  detalhes internos nunca vazam ao cliente (500 genérico sem stack trace).
- **auth-service `RedisGatewaySessionStore`** — falhas do Redis (ex.: `QueryTimeoutException`,
  `RedisConnectionFailureException`) traduzidas para
  `OidcGatewayException("REDIS_UNAVAILABLE", 503, "Dependência de sessão indisponível (Redis).")`.
  Helpers `redisOps` (leitura) e `redisRun` (escrita) centralizam a tradução de `DataAccessException`.
- **crm-backend `SecurityConfig`** — removido o bean `authenticationManager()` auto-referente
  (recursão de proxy AOP) que causava `StackOverflowError` no path de JWT inválido. Backend agora
  responde **401 RFC 6750** limpo.

## Testes (auth-service)

- `GatewayOidcRefreshTest` — **concorrência anti-replay**:
  - `shouldSerializeConcurrentRefreshesWithoutReusingRefreshToken` — 2 refreshes concorrentes na
    mesma sessão serializados pelo lock por sessão; nenhum refresh token apresentado mais de 1 vez;
    a requisição perdedora recebe o token já rotacionado.
  - `shouldRefreshTwoDifferentSessionsConcurrentlyWithoutCrossTalk` — sessões distintas em paralelo
    sem interferência (sem cross-talk).
- `OidcGatewayControllerTest` — `shouldRejectGetOnRefreshEndpoint`: `GET /auth/refresh` → **405**,
  `refresh` nunca invocado (`never()`).
- `GlobalExceptionHandlerTest` (novo) — padronização: OIDC 502, `CRM_ACCESS_DENIED` 403,
  405 METHOD_NOT_ALLOWED, erro genérico 500 sem vazamento de detalhes.
- `RedisGatewaySessionStoreTest` — `shouldTranslateRedisFailureToDomainUnavailableError` e
  `shouldTranslateRedisFailureOnWriteToDomainUnavailableError`: leitura e escrita com Redis fora →
  `REDIS_UNAVAILABLE` 503 (sem vazamento "refused").

**Resultado:** `mvn clean verify` → **168 testes, 0 falhas, BUILD SUCCESS**.

## Frontend

- **lint:** PASS (0 erros; warnings pré-existentes não-bloqueantes).
- **typecheck:** PASS (`tsc --noEmit`).
- **tests:** PASS — 28/28 (4 arquivos).
- **build:** PASS — `next build`.

## Concorrência e CSRF

- **Concorrência:** lock por sessão serializa refresh concorrente; teste anti-replay garante que o
  mesmo refresh token nunca é apresentado duas vezes e que o perdedor recebe o token rotacionado;
  sessões diferentes não interferem entre si.
- **CSRF:** endpoints que mutam a sessão exigem cookie + header `X-XSRF-TOKEN` (cookie não-HttpOnly
  para cookie-to-header); sem o header → 403 CSRF (validado no E2E com refresh real).

## Redis — Persistência e Tolerância a Falha

- **Persistência:** sessão em Redis (TTL 8h), tombstone em logout.
- **Restart persistence:** PASS — sessão criada → `docker restart crm-auth-service` → health 200 em
  <2s → mesma sessão `/me` 200 (Redis não reiniciado).
- **Redis failure:** PASS — `cd /opt/crm && docker compose stop redis` (container exited, porta
  fechada) → `/api/v1/auth/me` e `/auth/refresh` → **503 `REDIS_UNAVAILABLE`** controlado (sem loop,
  sem sessão parcial); `authorize` → 302 (não cria sessão órfã); health do app 200; logs mostram
  `QueryTimeoutException` + reconexões do ConnectionWatchdog. `start redis` → PONG → sessão antiga
  200 + novo login 200; **recuperação automática sem restart do app**.

## JWT inválido / StackOverflow (fix)

Validação em VPS: `Authorization: Bearer not.a.jwt` → **401** com
`WWW-Authenticate: Bearer error="invalid_token"` e `Content-Length: 0` (corpo vazio); **0
StackOverflowError/recursão** nos logs; log limpo: `JwtAuthenticationProvider ... Malformed token`.
PASS.

## API relay

- auth-service atua como BFF: injeta `Authorization: Bearer` server-side; whitelist de headers (o
  browser nunca envia `Authorization`; `Cookie`/`X-XSRF-TOKEN` do cliente não são repassados ao
  upstream).
- `/api/v1/auth/me` (cookie) → 200; sem sessão → 401; RBAC: `/api/v1/users` (papel `USER`, sem
  `user:read`) → **403** (permissões não alteradas). PASS.

## Segurança

- **cookies:** `crm_session` → `Path=/; Max-Age=28800; Secure; HttpOnly; SameSite=Lax`;
  `XSRF-TOKEN` → `Path=/; Max-Age=28800; Secure; SameSite=Lax` (não-HttpOnly, intencional).
- **storage:** localStorage apenas `sidebar-collapsed`; sessionStorage nenhum; zero `keycloak-js`;
  nenhum token no bundle, storage ou respostas; browser nunca envia `Authorization`.

## E2E VPS

Fluxo real via cliente HTTP (formulário do Keycloak, PKCE S256, cookie jar) com
`e2e.tester@crm.local`:

- **login:** PASS — `authorize` → Keycloak → callback → `/dashboard`; cookies `crm_session` +
  `XSRF-TOKEN` emitidos com os atributos acima.
- **relay:** PASS — `GET /api/v1/auth/me` → 200 com a identidade da `e2e.tester`.
- **refresh:** PASS — `POST /auth/refresh` (cookie + header CSRF) → **204**, corpo vazio (0
  ocorrências de `access_token`/`refresh_token`/`id_token`); sessão segue válida → 200.
- **403 RBAC:** PASS — `GET /api/v1/users` (papel `USER`) → 403.
- **rota protegida sem sessão:** PASS — acesso sem cookie → login/redirect (401 no relay).
- **logout:** PASS — `GET /auth/logout` → 302; `/me` após logout → 401 `Sessão de gateway revogada.`

## Deploy

- scp dos 3 arquivos alterados (md5 conferido) → `/opt/crm/docker`; `docker compose build backend
  auth-service` + `docker compose up -d --no-deps backend auth-service`; backup
  `/opt/crm/docker/docker-compose.yml.bak-6.5-pre`; health 200; containers Up.
- **Nota operacional:** a VPS possui 2 projetos compose — `/opt/crm/docker-compose.yml`
  (infraestrutura: redis/postgres/keycloak/rabbitmq/minio) e `/opt/crm/docker/docker-compose.yml`
  (app). Para operar a infraestrutura: `cd /opt/crm && docker compose ...`; o Redis failure test
  parou pelo projeto errado inicialmente.

## Arquivos alterados

**auth-service:**
- `src/main/java/com/becommerce/auth/presentation/rest/handler/GlobalExceptionHandler.java` (405 +
  padronização)
- `src/main/java/com/becommerce/auth/infrastructure/gateway/RedisGatewaySessionStore.java`
  (`REDIS_UNAVAILABLE` 503)
- `src/test/java/com/becommerce/auth/presentation/rest/handler/GlobalExceptionHandlerTest.java` (novo)
- `src/test/java/com/becommerce/auth/application/gateway/service/GatewayOidcRefreshTest.java`
  (concorrência anti-replay)
- `src/test/java/com/becommerce/auth/infrastructure/gateway/RedisGatewaySessionStoreTest.java` (503)
- `src/test/java/com/becommerce/auth/presentation/rest/OidcGatewayControllerTest.java` (405 GET)

**crm-backend:**
- `src/main/java/com/becommerce/crm/infrastructure/security/config/SecurityConfig.java` (fix
  StackOverflow)

## Commits

- `2c0ba6b` — `fix(auth): prevent jwt failure recursion`
- `a95c3b1` — `feat(auth): harden oidc gateway session and relay`

## Pendências

- **CRM Access 403** (`crm_enabled=false`) não reproduzido em 6.5 — permissões não alteradas para
  fabricar PASS; cenário já validado em 6.4 com 403 `CRM_ACCESS_DENIED` real.
- **Healthcheck de dependências** — `/auth/health` reporta apenas "UP" e não expõe disponibilidade
  do Redis; recomendação de endpoint específico (ex.: `/auth/health/redis`) em sprint futura.
- **Correlation ID dedicado** — correlação usa o `state` do OIDC; header de correlação dedicado
  recomendado para observabilidade multi-serviço.
- **Rate limiting** — `/auth/refresh` e endpoints de sessão sem limitação de taxa.
- **Usuário de teste** `e2e.tester@crm.local` permanece provisionado (credenciais apenas em `/tmp`
  da VPS, não versionadas).
- Inspeção DevTools validada por análise estática + verificação HTTP (cookies/headers), não por
  navegação manual.

## Status

**CONCLUÍDA**
