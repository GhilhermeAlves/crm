# Sprint 6.7 — Result

**Data:** 2026-08-04 · **Ambiente:** `https://srv1348261.hstgr.cloud` (VPS `76.13.237.238`)

## Objetivo

Rate limiting do relay/BFF `/api/*` com buckets por **usuário autenticado** (identidade da sessão de
gateway) e fallback para IP real, como evolução incremental do rate limiter distribuído da Sprint 6.6
(sem reabrir os demais componentes). Também corrige um vetor de spoofing de IP em `X-Forwarded-For`
detectado na auditoria, validado em produção (VPS) incluindo isolamento usuário A/B, expiração de
janela e transições Redis down/up.

## Arquitetura (antes/depois)

### Antes (Sprint 6.6)

- `GatewayRateLimitFilter` (ordem `HIGHEST_PRECEDENCE+1`) protegia apenas `/auth/authorize`,
  `/auth/callback`, `/auth/refresh`, `/auth/logout`.
- **`/api/*` não era rate-limitado** (pendência documentada da 6.6); um usuário podia disparar
  requisições ilimitadas ao relay/backend.
- IP real lido do **primeiro** valor de `X-Forwarded-For` — porém o nginx real usa
  `$proxy_add_x_forwarded_for` (REMOTE_ADDR **anexado ao final**), então o primeiro valor é
  controlável pelo cliente (spoofing de IP no rate limit).
- Escrita do corpo `429` duplicada no filtro.

### Depois (Sprint 6.7)

- **`ApiRateLimitFilter`** (novo, ordem `HIGHEST_PRECEDENCE+1`, padrão `/api/*`) roda **antes** do
  `ApiRelayController` encaminhar ao backend.
- **Estratégia da chave do bucket:** bucket único por identidade para todo `/api/*` na janela
  (`gateway:ratelimit:api:<identity>:<windowStart>` via `GatewayRateLimiter` reutilizado).
  - Identidade principal = **`userId` (UUID) da `GatewaySession`** resolvida no servidor pelo
    `sessionToken` opaco do cookie `crm_session` (mecanismo existente; nunca headers/cookie caseiro
    do cliente).
  - Fallback = IP real via `ClientIpResolver`.
- **`ClientIpResolver`** (novo) — corrige o vetor de spoofing: `X-Real-IP` (sobrescrito pelo nginx,
  confiável) → **último** valor plausível de `X-Forwarded-For` (o real anexado pelo
  `$proxy_add_x_forwarded_for`) → `remoteAddr`. **Nunca** aceita o primeiro valor do XFF.
- **`RateLimitErrorResponse`** (novo) — extrai a escrita do `429` (`Retry-After` +
  `X-Correlation-Id` + corpo JSON padrão), reutilizada pelos dois filtros (DRY).
- **`GatewayRateLimitFilter`** (6.6) — passa a usar `ClientIpResolver` e `RateLimitErrorResponse`
  (mesmo comportamento de chaves, sem duplicação).

## Implementação

### `ApiRateLimitFilter`

- Guarda de path: só atua em `GET/POST/... /api/*`; outras rotas passam direto.
- Se `rateLimitEnabled=false` **ou** `rateLimitApi<=0` → passa direto (desativa o bucket sem afetar
  os endpoints `auth`).
- Resolução de identidade:
  1. `cookieFactory.readSessionToken(cookies)` → se presente, `sessionResolver.resolve(token)`;
  2. se `SessionLookup.status() == ACTIVE` → `session.userId()` como chave;
  3. qualquer outro caso (sem cookie, expirada, revogada, não encontrada) ou falha de resolução
     (Redis fora → `OidcGatewayException`) → **IP real** (`ClientIpResolver`).
- `rateLimiter.enforce("api", identity, rateLimitApi, rateLimitWindow)`.
- Excedido → `RateLimitErrorResponse` (429 + `Retry-After` + `X-Correlation-Id`).
- Nenhum token/sessão/cookie é logado; logs apenas `correlationId`, bucket lógico, resultado.

### Configuração (padrões 6.6 preservados)

- `OidcGatewayProperties` (+ getter/setter): `rateLimitApi` (default **60**), prefixo
  `auth.gateway`, env `AUTH_GATEWAY_RATE_LIMIT_API`.
- `application.yml`: `rate-limit-api: ${AUTH_GATEWAY_RATE_LIMIT_API:60}`.
- `.env.example`: `AUTH_GATEWAY_RATE_LIMIT_API=60`.
- Defaults da 6.6 intactos (`authorize` 20, `callback` 20, `refresh` 30, `logout` 20, window 60s).

### Registro

- `GatewayConfig.apiRateLimitFilter(...)` — `FilterRegistrationBean<ApiRateLimitFilter>` com
  `urlPatterns=["/api/*"]`, ordem `HIGHEST_PRECEDENCE+1` (antes do Spring Security e do relay).

## Estratégia da chave (decisão)

| Opção | Decisão |
|-------|---------|
| `api:{identity}` (bucket único por identidade) | **Escolhida** — previsível, 1 chave Redis por usuário ativo por janela, atômico com o Lua existente, sem bypass variando rota. |
| `api:{identity}:{rota}` (sub-buckets por rota) | Rejeitada por ora — cada rota teria orçamento próprio (um atacante alternaria paths); pode ser adicionada depois sem mudar o mecanismo (`GatewayRateLimiter.enforce` já parametriza bucket). |
| Identidade | `userId` da `GatewaySession` (server-side, opaca, não controlável pelo cliente). Fallback: IP real. |
| IP (fallback) | `ClientIpResolver` — nunca o primeiro valor de `X-Forwarded-For` (spoiler).

## Redis / Lua

- Reutiliza integralmente o `GatewayRateLimiter` da 6.6: script Lua `INCR`+`EXPIRE` com chave
  `gateway:ratelimit:<bucket>:<key>:<windowStart>` — atômico, TTL nativo, distribuído
  (multi-instância).
- **Fail-controlled:** com Redis fora, `sessionResolver` falha (`REDIS_UNAVAILABLE` 503) → identidade
  cai para IP → `GatewayRateLimiter` permite com warning. O relay devolve o erro de sessão
  (503) sem loop de erro e sem derrubar o gateway.

## Segurança

- **Correção de spoofing de IP (achado da auditoria):** nginx real usa
  `X-Forwarded-For $proxy_add_x_forwarded_for` (REMOTE_ADDR no final) — a implementação da 6.6 lia o
  primeiro valor (forjável). `ClientIpResolver` resolve pela cadeia confiável; adicionado teste
  `shouldIgnoreForgedFirstForwardedForWithProxyAddedChain`.
- **Identidade nunca escolhida pelo cliente:** o bucket é o `userId` da sessão servidor — headers
  arbitrários (`X-Forwarded-User`, `X-Forwarded-For`, `X-Real-IP` manipulados) e cookies caseiros
  não selecionam outro bucket (testado).
- **Sem bypass por cookie:** um cookie sem sessão ativa cai no bucket IP; não existe valor de cookie
  que escolha um bucket arbitrário.
- **Usuário autenticado não cai no bucket anônimo:** sessões diferentes → buckets distintos;
  anônimo (IP) não consome nem é bloqueado pelo bucket do usuário (testado).
- **429** não vaza tokens/cookies; apenas `code`/mensagem genérica + `correlationId`; nenhum segredo
  novo no repositório; credenciais E2E continuam apenas em `/tmp` da VPS.

## Testes (auth-service)

Novos/alterados — `mvn clean verify` → **219 testes, 0 falhas, BUILD SUCCESS** (antes: 203).

- `ApiRateLimitFilterTest` (novo, 12): bucket por usuário autenticado (`userId`); mesmo usuário
  compartilha o bucket entre paths; usuários A/B isolados; fallback IP sem cookie; fallback IP com
  sessão não ativa (`NOT_FOUND`); fallback IP com falha de resolução (Redis fora); 429 com
  `Retry-After` + `X-Correlation-Id` + corpo JSON; headers arbitrários não mudam a identidade;
  `rateLimitApi=0` desativa; `rateLimitEnabled=false` desativa; `/api/*` não interceptado;
  `/api/v1/...` interceptado.
- `ClientIpResolverTest` (novo, 5): prefere `X-Real-IP`; usa último valor plausível do XFF; ignora
  primeiro valor forjado com `$proxy_add_x_forwarded_for`; fallback `remoteAddr`; ignora entradas
  malformadas.
- `GatewayRateLimitFilterTest` (modificado, 8): adaptado ao novo construtor
  (`ClientIpResolver` + `RateLimitErrorResponse`); comportamento de chaves preservado.

## Testes E2E VPS (17 checks, 17 PASS)

Fluxo real via cliente HTTP (formulário Keycloak, PKCE S256, cookie jar), usuários
`e2e.tester@crm.local` (credenciais em `/tmp/e2e_creds.txt`) e `validacao.tester@crm.local` (senha
temporária `admin123` definida via API do Keycloak apenas para o teste de isolamento).

- **Login usuário A (1/1):** fluxo completo authorize → Keycloak → callback → sessão.
- **Rate limit `/api/*` (4/4):** primeiro request 403 (RBAC legítimo do backend — relay funcionando);
  429 no 61º request da sessão (limite 60/60s) com `Retry-After` + `X-Correlation-Id`; corpo JSON
  `RATE_LIMIT_EXCEEDED`; usuário A permanece 429 após exaurir o bucket.
- **Isolamento A/B (4/4):** usuário B (sessão distinta) continua com 403 (sem 429) após o bucket de A
  exaurido — buckets independentes; requisição anônima (sem cookie) usa bucket IP e não é bloqueada
  pelo bucket de A.
- **Expiração de janela (1/1):** após 60s o usuário A volta a 403 (bucket zerado).
- **Redis down/up (3/3):** com Redis fora, `/api/v1/users` responde 503 (erro de sessão, sem 429 e
  sem 500 em sequência — sem loop); com Redis de volta, responde 403 (recuperação).
- **Fluxo pós-transições (4/4):** login completo novamente; relay `/api/v1/users` 403; logout 302.

## Regressões

- **auth-service:** `mvn clean verify` → 219 testes, 0 falhas, BUILD SUCCESS.
- **frontend:** lint PASS (0 erros; warnings pré-existentes não-bloqueantes) · typecheck PASS ·
  tests 28/28 PASS · build PASS.
- **E2E VPS Sprint 6.6 (22 checks):** `e2e_66.py` → 22/22 PASS (correlation ID, rate limit authorize
  e refresh, readiness transitions Redis/Keycloak, fluxo pós-transições).

## Deploy

- Backup em `/opt/crm/auth-service.bak-6.7-pre` (a VPS compila o auth-service via docker build a
  partir de `/opt/crm/auth-service`; `pom.xml`, `Dockerfile` e `application.yml` conferem com o
  local antes do scp).
- scp de `src/` para `/opt/crm/auth-service/`; `docker compose build auth-service` +
  `up -d --no-deps auth-service` (somente o serviço alterado; infraestrutura intacta).
- Container `Up (health: starting → healthy)`; `/auth/health` e `/auth/health/ready` → 200.
- Sem alteração de env no compose VPS: `AUTH_GATEWAY_RATE_LIMIT_API` usa o default 60.

## Arquivos alterados

**auth-service (novos):**
- `src/main/java/com/becommerce/auth/infrastructure/gateway/ApiRateLimitFilter.java`
- `src/main/java/com/becommerce/auth/infrastructure/gateway/ClientIpResolver.java`
- `src/main/java/com/becommerce/auth/infrastructure/gateway/RateLimitErrorResponse.java`
- `src/test/java/com/becommerce/auth/infrastructure/gateway/ApiRateLimitFilterTest.java`
- `src/test/java/com/becommerce/auth/infrastructure/gateway/ClientIpResolverTest.java`

**auth-service (modificados):**
- `src/main/java/com/becommerce/auth/infrastructure/gateway/GatewayRateLimitFilter.java`
  (usa `ClientIpResolver` + `RateLimitErrorResponse`)
- `src/main/java/com/becommerce/auth/infrastructure/gateway/OidcGatewayProperties.java`
  (`rateLimitApi`)
- `src/main/java/com/becommerce/auth/infrastructure/config/GatewayConfig.java`
  (registro do `ApiRateLimitFilter`)
- `src/main/resources/application.yml` (`rate-limit-api`)
- `.env.example` (`AUTH_GATEWAY_RATE_LIMIT_API`)
- `src/test/java/com/becommerce/auth/infrastructure/gateway/GatewayRateLimitFilterTest.java`
  (construtor)

## Commits

- `7215290` — `feat(auth): add api relay rate limiting by authenticated user`
  (inclui `ClientIpResolver` + `RateLimitErrorResponse` + testes)

## Pendências

- Sub-buckets por rota do relay (`api:{identity}:{rota}`) não implementados (decisão: bucket único;
  viável sem mudar o mecanismo).
- Teste de concorrência do bucket `/api/*` em nível de integração (cobrir em sprint futura junto com
  o teste de concorrência existente do `GatewayRateLimiter`).
- Inspeção DevTools de headers continua validada por análise estática + verificação HTTP.

## Status

**CONCLUÍDA**
