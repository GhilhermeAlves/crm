# Sprint 6.8 — Result

**Data:** 2026-08-05 · **Ambiente:** `https://srv1348261.hstgr.cloud` (VPS `76.13.237.238`)

## Objetivo

**Hardening final do Gateway, validação de concorrência do rate limiting, revisão de
segurança e fechamento técnico da camada de autenticação.** A Sprint é tratada como
validação/fechamento, não reescrita: preservar a arquitetura das Sprints 6.5/6.6/6.7 e provar
que é segura, consistente, concorrente, resiliente, observável e testável — resolvendo as
pendências abertas da 6.7 (sub-buckets por rota — avaliados; teste de concorrência do
bucket `/api/*` em nível de integração — implementado).

## Escopo

- Auditoria técnica de todo o Gateway (`GatewayRateLimiter`, Lua, `ApiRateLimitFilter`,
  `GatewayRateLimitFilter`, `ClientIpResolver`, `GatewaySession*`, relay, cookies, CSRF,
  `SecurityConfig`, `GatewayConfig`, Redis, docker, nginx).
- Testes de concorrência em nível de integração (mesmo bucket, usuários A/B, IP fallback,
  expiração de janela, Redis down/up, Redis connection-failure).
- Auditoria de segurança: headers de proxy, identidade server-side, tokens/logs, cookies,
  CORS/CSRF, relay, erros HTTP.
- E2E VPS incluindo cenários de concorrência contra Redis real + regressões 6.6/6.7.
- Decisão técnica sobre sub-buckets por rota (registrada, **não implementados**).

## Auditoria realizada

### Concorrência do `GatewayRateLimiter` (Lua)

- Script `INCR` + `EXPIRE` (no primeiro incremento) é **atômico e distribuído**; chave
  `gateway:ratelimit:<bucket>:<identity>:<windowStart>`. Não há contador local — válido com
  multi-instância.
- **Race:** nenhuma — a atomicidade é garantida pelo Redis; a lógica de decisão do
  `enforce` (`count > limit → 429`) e o `Retry-After` estão corretos. Nenhum bug funcional
  encontrado.
- **TTL:** nativo (janela) → as chaves de rate limit são removidas pelo próprio Redis ao fim
  da janela (confirmado: após o E2E, `gateway:ratelimit:*` = 0 chaves residuais — sem
  crescimento anormal de memória/chaves).

### Identidade server-side

- `ApiRateLimitFilter.resolveKey`: `sessionToken` do cookie HttpOnly `crm_session` →
  `GatewaySessionResolver` → `RedisGatewaySessionStore.findByToken` → `userId` (UUID) da
  sessão **server-side**. A identidade **nunca** vem de header/query/cookie arbitrário; um
  cookie sem sessão ativa cai para o bucket por IP. Usuário A não seleciona o bucket de B
  (o `sessionToken` é opaco, url-safe 32, validado contra o store). **Confirmado.**
- Fallback IP via `ClientIpResolver`.

### `ClientIpResolver` / headers de proxy

- nginx da VPS (auditado): `X-Real-IP $remote_addr` (sobrescreve — confiável) e
  `X-Forwarded-For $proxy_add_x_forwarded_for` (**REMOTE_ADDR anexado ao final**).
- `ClientIpResolver` prioriza `X-Real-IP` → último valor plausível do XFF → `remoteAddr`.
  **Nunca** aceita o primeiro valor do XFF (controlável pelo cliente). `Host`/`Origin`/
  `Referer`/`Forwarded` não são usados para autenticação nem rate limiting. **Confirmado.**

### Tokens / logs

- Busca em todo o `auth-service` (main + test) por logs de access/refresh/session token,
  `Authorization`, cookies e credenciais: **nenhum vazamento**. Logs contêm apenas `userId`,
  `correlationId`/`state`, código de erro, bucket e status HTTP.
- `RestClientOidcTokenClient` nunca loga corpo/resposta de token.
- Observação de baixa severidade (registrada, não alterada): `JwtAuthenticationEntryPoint`
  devolve `authException.getMessage()` no corpo do 401 — em endpoints protegidos por
  Security é uma mensagem genérica do Spring; não expõe segredos e está fora do escopo do
  rate limiting. Ajustar exigiria alterar contrato de respostas anterior a esta sprint.

### Cookies / CORS / CSRF

- `crm_session`: `Path=/; SameSite=Lax; Secure` (prod), `HttpOnly`, `Max-Age=TTL`. ✓
- `XSRF-TOKEN`: não-HttpOnly (intencional, cookie-to-header), `SameSite=Lax`, `Secure`. ✓
- CSRF cookie-to-header em `POST /auth/refresh` (`GatewayCsrfFilter`). O relay `/api/*` é
  mitigado por `SameSite=Lax` (cookies não são enviados em POST cross-site) + sessão opaca.
  **Avaliado OK.**
- CORS: auth-service é same-origin via nginx (sem CORS próprio). **Avaliado OK.**
- Observação de higiene: o logout limpa o cookie `crm_session` mas não o `XSRF-TOKEN`;
  inócuo (o token CSRF está atrelado à sessão revogada) — registrado, não alterado para
  evitar risco/regressão fora de escopo.

### Relay / erros

- Método HTTP, query string, headers whitelist preservados; `Authorization` injetado
  server-side (whitelist nunca inclui `Authorization`/`Cookie`, logo o browser não substitui
  o token); `X-Correlation-Id` propagado; resposta via whitelist de headers; erro upstream
  → `502 UPSTREAM_UNAVAILABLE` genérico. **Confirmado.**
- Formato de erro consistente (`status/code/error/message/timestamp/correlationId`) para
  401/403/404/405/429/500/503, sem stack trace nem internos. `429` filtrado antes do Spring
  Security com `Retry-After` + `X-Correlation-Id`. **Confirmado.**

## Alterações

**Apenas teste** — nenhum código de produção alterado (auditoria não encontrou bug/race
que exija correção).

- **novo** `src/test/java/com/becommerce/auth/infrastructure/gateway/GatewayRateLimitConcurrencyTest.java`
  (8 testes) — concorrência em nível de integração:
  1. mesmo bucket: 200 requisições concorrentes, limite 50 → **exatamente** 50 aceitas e
     150 com `RateLimitExceededException` (sem over-admission);
  2. sem contagem perdida/duplicada: 500 chamadas concorrentes → contagem total == chamadas;
  3. TTL da janela propagado como `EXPIRE` em toda chamada;
  4. usuários A/B concorrentes → buckets independentes (exatamente 25/25 excedentes);
  5. IP fallback concorrente → buckets por IP independentes (exatamente 10/10);
  6. expiração de janela sob concorrência → nova janela permite (sem bloqueio permanente,
     TTL correto, contagem não persiste);
  7. Redis down sob carga → fail-controlled (0× 429, sem 500) e recuperação automática
     (limite volta a valer);
  8. conexão Redis falha contínua → permite com warning, sem loop de 429.

O Dockerfile da imagem usa `mvn package -DskipTests`, portanto o artefato de produção não
muda com a adição de testes. **Não houve redeploy** (confirmado byte a byte: `src/main` local
== `src/main` da VPS, 70 arquivos), o que valida que o código ativo é exatamente o auditado.

## Concorrência

### Mesmo bucket (Redis real, E2E VPS)

80 requisições concorrentes de um único usuário contra `/api/v1/users` (janela 60s, limite 60):

- **exatamente 60 aceitas** e **exatamente 20× 429** (sem over-admission, sem contagem
  perdida — a atomicidade do `INCR`+`EXPIRE` foi validada contra o Redis real);
- todos os 429 com `Retry-After` (ex.: 48) e `X-Correlation-Id`;
- após exaurir o bucket, o usuário permanece 429.

### Usuários A/B

`validacao.tester@crm.local` disparou 80 concorrentes **depois** do bucket de A exaurido:
exatamente 60 aceitas e 20× 429 (orçamento próprio; A não bloqueia B).

### IP fallback

Requisições anônimas (sem cookie) → bucket por IP: 30 concorrentes, todas aceitas
(0× 429), **não** bloqueadas pelos buckets de A/B (isolamento por IP).

### Expiração sob concorrência

Após 60s (fim da janela), o usuário A voltou a obter 403 (relay ok) — nova janela, sem
janela duplicada/TTL incorreto/contagem persistente/bloqueio permanente.

### Redis down/up durante carga

Redis parado durante 40 requisições concorrentes: 100% `503` (falha controlada de sessão),
**0× 429, 0× 500, sem loop**; segunda chamada sequencial também `503` (sem avalanche).
Redis de volta → `403` (recuperação automática, sem restart do app).

## Rate limiting

- Estratégia preservada (6.7): bucket único `api:{identity}` com fallback por IP; políticas
  `authorize` 20 / `callback` 20 / `refresh` 30 / `logout` 20 (por IP/sessão) e `api` 60/60s.

## Segurança

- Correção de spoofing de IP da 6.7 confirmada em produção (nginx `$proxy_add_x_forwarded_for`).
- Identidade server-side auditada e confirmada (impossível ao browser escolher o bucket).
- Nenhum vazamento de tokens/credenciais em logs.
- 429 não vaza cookies/session token; apenas código genérico + `correlationId`.
- Cookie: HttpOnly + SameSite=Lax + Secure (prod); validação `secure-cookie=false` falha o
  startup no profile `prod`.

## Redis

- TTL nativo de janela valida a limpeza automática (0 chaves `gateway:ratelimit:*` residuais).
- Fail-controlled preservado (Redis down → permite com warning; sessão → 503; sem derrubar o
  gateway nem gerar cascata de 429).
- Recuperação automática confirmada após Redis down/up sem restart do app.

## Relay

- Auditar mantém método/query/headers; `Authorization` sempre server-side (insubstituível);
  `X-Correlation-Id` propagado; erros sem vazamento interno. Sem alteração.

## Cookies

- `crm_session` HttpOnly/SameSite=Lax/Secure; `XSRF-TOKEN` não-HttpOnly (cookie-to-header).
  Observação de higiene (XSRF-TOKEN não é limpo no logout) registrada como inócua.

## CORS/CSRF

- Same-origin via nginx (sem CORS); CSRF cookie-to-header só no refresh (mutação de sessão) +
  `SameSite=Lax` para o relay. Avaliado como adequado à arquitetura cookie-only.

## Testes

- **Backend:** `mvn clean verify` → **227 testes, 0 falhas, BUILD SUCCESS** (219 → +8 de
  concorrência).
- **Frontend:** lint PASS (0 erros; warnings pré-existentes não-bloqueantes) · typecheck PASS ·
  tests **28/28 PASS** · build PASS.
- **Integração:** `GatewayRateLimitConcurrencyTest` (8/8) — sem over-admission, contagem
  exata, TTL da janela, isolamento A/B e IP, expiração, Redis down/up/carga.

## E2E (VPS)

Fonte de verdade = repositório local; VPS apenas para validação integrada. `src/main` local
== VPS (bytes idênticos) → **sem rebuild/redeploy** (nenhum código de produção mudou).

- **Sprint 6.8 (`e2e_68.py`): 20/20 PASS** — health/readiness; login A; concorrência mesmo
  bucket (60/20); A permanece 429; A/B; IP fallback; expiração (60s); Redis down/up sob carga;
  fluxo completo pós-transições (login → relay → logout).
- **Regressão 6.7 (`e2e_67.py`): 17/17 PASS.**
- **Regressão 6.6 (`e2e_66.py`): 22/22 PASS.**

## Performance

- `/auth/health`: `200` em ~35ms; `/auth/health/ready`: `200` em ~68ms (inclui TLS/nginx).
- `auth-service`: `healthy`, CPU **0,30%**, memória **~392 MiB** (estável, sem crescimento
  anormal).
- `gateway:ratelimit:*` = **0 chaves residuais** após o E2E → TTL da janela limpa os buckets
  (sem retenção/explosão de chaves).
- Nenhum bloqueio/thread presa/timeout observado; Redis estável sob as rajadas do E2E.

## Deploy

- Nenhum artefato reconstruído (alteração apenas de teste): o container `crm-auth-service`
  continua `Up (healthy)` com o código auditado — conferido byte a byte (`src/main`, 70
  arquivos idênticos) antes de decidir não revalidar.
- `docker compose ps`: auth-service `healthy`; demais containers intactos.

## Resultado

STATUS: **CONCLUÍDA**

## Pendências

- **Sub-buckets por rota (`api:{identity}:{rota}`):** avaliados e **NÃO implementados**.
  Decisão: bucket único por identidade é suficiente para o nível de proteção desejado —
  previsível, 1 chave Redis por usuário ativo por janela, atômico com o Lua existente, sem
  bypass abundante (um atacante alternaria paths para variar o orçamento e a explosão de
  chaves cresceria linearmente com rotas). Se uma rota específica exigir orçamento próprio no
  futuro, `GatewayRateLimiter.enforce` já parametriza o bucket — sem mudança de mecanismo.
- Observação de baixa severidade (não alterada, fora de escopo): `JwtAuthenticationEntryPoint`
  expõe `authException.getMessage()` no 401 body (genérico no Spring; sem segredo).
- Observação de higiene (não alterada): logout não limpa o cookie `XSRF-TOKEN` (inócuo —
  token CSRF atrelado à sessão revogada).