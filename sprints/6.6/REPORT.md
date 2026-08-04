# Sprint 6.6 — Result

**Data:** 2026-08-04 · **Ambiente:** `https://srv1348261.hstgr.cloud` (VPS `76.13.237.238`)

## Objetivo

Health/readiness reais com sondagem de dependências (Redis e Keycloak), Correlation ID dedicado para
observabilidade multi-serviço e Rate limiting distribuído (Redis) nos endpoints do Access Gateway,
validados em produção (VPS) incluindo transições de falha/recuperação de dependências.

## Alterações

### Health / Readiness

- **`GET /auth/health`** — liveness: retorna apenas `{"status":"UP"}` quando o processo responde;
  **não** depende de Redis/Keycloak (usado pelo healthcheck do container).
- **`GET /auth/health/ready`** — readiness real: `200 {"status":"UP","checks":{"redis":"UP","keycloak":"UP"}}`
  ou `503 {"status":"DOWN",...}` com cada dependência `UP`/`DOWN`; nenhum detalhe administrativo
  (sem stack, sem credenciais, sem URI internas).
- **`DependencyProbe`** (novo) — `redisReachable()` via `PING` no `StringRedisTemplate`;
  `keycloakReachable()` via `OidcProviderMetadata.isReachable()` (sondagem fresca do discovery,
  sem exceção, sem criar segundo mecanismo de discovery).
- **`OidcProviderMetadata`** — extraído `fetchDiscovery()`; novo `isReachable()`: sucesso → true;
  qualquer falha (ex.: 503) → false.
- **`SecurityConfig`** — `/auth/health/**` público (liveness + readiness).
- **`Dockerfile`** (auth-service) — `wget ca-certificates` no runtime (imagem JRE) para o healthcheck.
- **`docker/docker-compose.yml`** — healthcheck do auth-service:
  `wget -qO- http://localhost:8080/auth/health` (interval 30s, timeout 5s, retries 3).

### Correlation ID

- **`CorrelationIdFilter`** (novo, ordem `HIGHEST_PRECEDENCE` em `/*`) — aceita/valida o header
  `X-Correlation-Id` (`^[A-Za-z0-9_.\-:]{8,128}$`); ausente/inválido → gera
  `SecureTokenGenerator.urlSafe(16)`; define o contexto `ThreadLocal` (`CorrelationIdContext`) e o
  MDC `correlationId`; **sempre** emite o header na resposta (inclusive erros/429); limpa contexto e
  MDC ao final da requisição (finally).
- **Propagação** — `ApiRelayController` envia ao backend o `X-Correlation-Id` do contexto (nunca o
  bruto do cliente); `GlobalExceptionHandler` inclui `correlationId` no corpo dos erros
  (403/405/429/500/502). Nunca usa JWT/cookies/segredos como valor.
- **Logs** — padrão do log com `%X{correlationId:-}` (vazio quando ausente).

### Rate limiting

- **`GatewayRateLimiter`** (novo) — janela fixa distribuída em Redis via script Lua (`INCR`+`EXPIRE`),
  chave `gateway:ratelimit:<bucket>:<key>:<windowStart>` (atômico por chave no cluster/multi-instância).
- **`GatewayRateLimitFilter`** (novo, ordem `HIGHEST_PRECEDENCE+1`) — aplicado apenas a
  `/auth/authorize`, `/auth/callback`, `/auth/refresh`, `/auth/logout`:
  - `authorize`/`callback` → key = IP real (primeiro valor confiável de `X-Forwarded-For`
    do `$proxy_add_x_forwarded_for` do nginx, senão `X-Real-IP`, senão `remoteAddr`).
  - `refresh`/`logout` → key = `sessionToken` opaco do cookie (fallback IP).
  - Excedido → `429` JSON `{status, code:"RATE_LIMIT_EXCEEDED", error, message, timestamp,
    correlationId}` + `Retry-After` (segundos) + `X-Correlation-Id`.
- **Config** (`OidcGatewayProperties`, prefixo `auth.gateway`, envs `AUTH_GATEWAY_RATE_LIMIT_*`):
  `rateLimitEnabled` (true), `rateLimitWindow` (60s), `rateLimitAuthorize` (20), `rateLimitCallback`
  (20), `rateLimitRefresh` (30), `rateLimitLogout` (20). `limit <= 0` desativa o bucket.
  **`/api/*` não é rate-limitado.**
- **Fail-controlled:** com Redis fora o rate limiter **permite** (com warning), nunca derruba/loop
  de 429 em cascata. Decisão documentada.
- **`RateLimitExceededException`** (novo, domain) — status 429, código `RATE_LIMIT_EXCEEDED`,
  `retryAfterSeconds`.

## Testes (auth-service)

Novos/alterados — `mvn clean verify` → **203 testes, 0 falhas, BUILD SUCCESS** (antes: 168).

- `CorrelationIdFilterTest` (novo, 6): gera quando ausente; preserva válido; regenera inválido;
  header sempre na resposta; contexto/MDC disponível durante a requisição e limpo ao final;
  propagação em erro.
- `GatewayRateLimiterTest` (novo, 7): dentro do limite; excedido → `RateLimitExceededException`;
  chaves distintas independentes; Redis fora → fail-controlled (permite); `limit<=0` desativa;
  concorrência (200 tasks) sem ultrapassar o limite.
- `GatewayRateLimitFilterTest` (novo, 8): authorize por IP; `X-Forwarded-For` respeitado;
  refresh/logout por sessionToken; fallback IP; 429 + `Retry-After` + `correlationId` no corpo e
  header; `/api/*` não interceptado; desabilitado → passa direto.
- `HealthControllerTest` (novo, 7): liveness sem depender de dependências; readiness UP; Redis DOWN;
  Keycloak DOWN; recuperação; sem exposição de detalhes.
- `GlobalExceptionHandlerTest` (modificado): +429 com `Retry-After`; `correlationId` no corpo;
  sem vazamento de detalhes.
- `ApiRelayControllerTest` (modificado): +`shouldPropagateCorrelationIdToBackend`.
- `OidcProviderMetadataTest` (modificado): +3 `isReachable` (sucesso / falha 503 / recuperação).
- `InternalAuthControllerTest` (modificado): `@MockBean DependencyProbe` (HealthController no slice
  `WebMvcTest`).

## Frontend

- **lint:** PASS (0 erros; warnings pré-existentes não-bloqueantes).
- **typecheck:** PASS (`tsc --noEmit`).
- **tests:** PASS — 28/28 (4 arquivos).
- **build:** PASS — `next build`.

## E2E VPS (22 checks, 22 PASS)

Fluxo real via cliente HTTP (formulário do Keycloak, PKCE S256, cookie jar) com
`e2e.tester@crm.local` (credenciais apenas em `/tmp` da VPS, redefinidas para `admin123` via
`setpass.sh`; o arquivo `/tmp/e2e_creds.txt` estava com a senha da 6.4 — corrigido; um lock temporário
de brute-force do Keycloak expirou sozinho).

- **Correlation ID (4/4):** liveness retorna `X-Correlation-Id`; header válido de entrada é
  preservado; inválido (curto) é regenerado; erro 401 do relay inclui `X-Correlation-Id`.
- **Rate limit authorize (4/4):** 429 no 21º request (limite 20/60s) com `Retry-After` +
  `X-Correlation-Id`; corpo JSON `RATE_LIMIT_EXCEEDED`; recupera após a janela (302).
- **Login + refresh (4/4):** login completo → callback → `crm_session` + `XSRF-TOKEN`;
  `POST /auth/refresh` 429 no 31º (limite 30/60s); logout emite `X-Correlation-Id`.
- **Readiness transitions (7/7):** baseline UP; **redis down → 503 `redis:DOWN`** (liveness
  permanece 200; authorize responde 302 sem 429 — fail-controlled); **redis up → 200 UP**;
  **keycloak down → 503 `keycloak:DOWN`**; **keycloak up → 200 UP** (recuperação automática,
  polling via readiness até ficar UP).
- **Fluxo pós-transições (3/3):** login completo novamente; relay `/api/v1/users` → 403 (RBAC do
  backend, permissões não alteradas — decisão legítima do upstream, relay funcionando); logout 302.

## Deploy

- scp de `Dockerfile` + `src` (tar) para `/opt/crm/auth-service`; healthcheck adicionado ao
  `auth-service` no `/opt/crm/docker/docker-compose.yml` (inserção cirúrgica; backup
  `docker-compose.yml.bak-6.6-pre` + `.env.bak-6.6-pre`); `docker compose build auth-service` +
  `up -d --no-deps auth-service`; container `Up (health: healthy)`; `/auth/health` e
  `/auth/health/ready` → 200 via local e nginx (`https://srv1348261.hstgr.cloud/auth/health/ready`).
- **Nota operacional:** a VPS possui 2 projetos compose — `/opt/crm/docker-compose.yml`
  (infraestrutura: redis/postgres/keycloak/rabbitmq/minio) e `/opt/crm/docker/docker-compose.yml`
  (app). Para operar a infraestrutura: `cd /opt/crm && docker compose ...`. O alias de SSH
  `crm-vps` só existe na máquina de origem — scripts que rodam na VPS devem executar o docker
  diretamente (não `ssh crm-vps` auto-referente).

## Segurança

- Liveness/readiness não expõem URIs internas, credenciais, stack trace nem versões; `health/ready`
  publica apenas `UP`/`DOWN` por dependência.
- Rate limiter nunca vaza cookies/session tokens no corpo (429 apenas código + mensagem genérica +
  correlationId); o 429 não distingue sessões entre si para o cliente.
- Varredura de segredos: nenhum segredo novo no repositório; `.env.example` mantém valores vazios;
  credenciais E2E apenas em `/tmp` da VPS.
- Correlation ID é sempre gerado/validado por regex, nunca derivado de dados sensíveis.

## Arquivos alterados

**auth-service (novos):**
- `src/main/java/com/becommerce/auth/infrastructure/observability/CorrelationIdContext.java`
- `src/main/java/com/becommerce/auth/infrastructure/observability/CorrelationIdFilter.java`
- `src/main/java/com/becommerce/auth/infrastructure/gateway/GatewayRateLimiter.java`
- `src/main/java/com/becommerce/auth/infrastructure/gateway/GatewayRateLimitFilter.java`
- `src/main/java/com/becommerce/auth/domain/gateway/RateLimitExceededException.java`
- `src/main/java/com/becommerce/auth/infrastructure/health/DependencyProbe.java`
- `src/test/java/com/becommerce/auth/infrastructure/observability/CorrelationIdFilterTest.java`
- `src/test/java/com/becommerce/auth/infrastructure/gateway/GatewayRateLimiterTest.java`
- `src/test/java/com/becommerce/auth/infrastructure/gateway/GatewayRateLimitFilterTest.java`
- `src/test/java/com/becommerce/auth/presentation/rest/HealthControllerTest.java`

**auth-service (modificados):**
- `src/main/java/com/becommerce/auth/presentation/rest/HealthController.java` (liveness+readiness)
- `src/main/java/com/becommerce/auth/infrastructure/config/GatewayConfig.java` (registro de filtros)
- `src/main/java/com/becommerce/auth/infrastructure/config/SecurityConfig.java` (`/auth/health/**`)
- `src/main/java/com/becommerce/auth/infrastructure/gateway/OidcGatewayProperties.java` (rate limit)
- `src/main/java/com/becommerce/auth/infrastructure/gateway/OidcProviderMetadata.java` (`isReachable`)
- `src/main/java/com/becommerce/auth/presentation/rest/ApiRelayController.java` (propagação)
- `src/main/java/com/becommerce/auth/presentation/rest/handler/GlobalExceptionHandler.java` (429 +
  correlationId)
- `src/main/resources/application.yml` (envs rate limit + padrão de log)
- `Dockerfile` (wget no runtime) · `.env.example`
- testes: `GlobalExceptionHandlerTest`, `ApiRelayControllerTest`, `InternalAuthControllerTest`,
  `OidcProviderMetadataTest`

**docker:**
- `docker/docker-compose.yml` (healthcheck auth-service; sincronizado no VPS)

## Commits

- `b4a4789` — `feat(auth): add health readiness and correlation id`
- `ee49121` — `feat(auth): add gateway rate limiting`

## Pendências

- **Rate limit de `/api/*`** — não aplicado (por design); buckets adicionais por rota do relay podem
  ser configurados em sprint futura se necessário.
- **Limites por usuário autenticado** — authorize/callback usam IP; buckets por usuário (via sessão)
  podem ser adicionados sem alterar a política de fail-controlled.
- **Inspeção DevTools** validada por análise estática + verificação HTTP (cookies/headers), não por
  navegação manual.

## Status

**CONCLUÍDA**
