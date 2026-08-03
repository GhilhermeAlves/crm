# Sprint 6.3 — Auth Service Gateway OIDC (Integração de Produção / E2E)

## Identificação
- **Sprint:** 6.3
- **Nome:** Integrate OIDC gateway infrastructure
- **Data:** 2026-08-03
- **Status:** ✅ **Concluída**
- **Responsável:** AI Agent
- **Fase:** Segurança (Sprint 6 — Access Gateway, fase 3 integração de produção)

## Escopo
Levar o Gateway OIDC das Sprints 6.1/6.2 para o ambiente real:

- **Client Keycloak `crm-gateway`** — standard flow, PKCE S256, redirect URIs, web
  origins, `post.logout.redirect.uris`, secret regenerado e persistido só no `.env`.
- **nginx** — rota `/auth/*` → `auth-service:8082`; `/realms/` `/resources/` `/js/` →
  Keycloak; `/api/` `/docs/` `/actuator/` → `8081`; `/` → `3000`.
- **Redis como store de sessão do gateway** — `RedisGatewaySessionStore` (JSON,
  TTL nativo, tombstones, lock distribuído), em substituição ao
  `InMemoryGatewaySessionStore`.
- **Compose + `.env` de produção** — envs do gateway, Redis (`host`/`port`/`password`),
  `extra_hosts` para hairpin do issuer.
- **E2E real** — 58 checks / 18 cenários passando por nginx → auth-service → Keycloak
  + Redis (usuário real `validacao.tester@crm.local`, usuário fake `nao.linkado@crm.local`
  criado e removido pelo próprio E2E).

**Fora de escopo:** frontend, backend CRM, recursão Sprint 5, Sprint 6.4 (não iniciada).

---

## Arquitetura (topologia real)

```
Internet →  https://srv1348261.hstgr.cloud  (76.13.237.238)
                  │
                  v
              nginx (host)
   ┌──────────────┼──────────────────────────────┐
   │              │                              │
 /realms/ /resources/ /js/        /auth/*            /api/ /docs/ /actuator/     /
   │              │                              │         │
   v              v                              v         v
Keycloak 26.3  (8080)                auth-service 8082   backend 8081   frontend 3000
127.0.0.1:8080 (loopback)              │       │
   (realm CRM, client crm-gateway)     │       └─ Redis 7 (gateway:session:*)
                                       └─ Keycloak (token/jwks/descoberta via
                                          issuer público → hairpin → nginx → keycloak)
```

## Configuração de produção

### Client `crm-gateway` (Keycloak 26.3)
- id `ce2e4892-c253-43e9-a3dc-7fd786e37c42`; `standardFlow=true`, `implicit=false`,
  `directAccessGrants=false`, `publicClient=false`; PKCE **S256** via attributes
  (`pkce.code.challenge.method`).
- `redirectUris=["https://srv1348261.hstgr.cloud/auth/callback"]`,
  `webOrigins=["https://srv1348261.hstgr.cloud"]`.
- `post.logout.redirect.uris` → **wildcard same-origin**
  `https://srv1348261.hstgr.cloud/*` (ver descobertas).
- Secret regenerado (32 chars), persistido como `AUTH_GATEWAY_CLIENT_SECRET` em
  `/opt/crm/docker/.env` e `/opt/crm/.env`; backup `crm-gateway-client.backup.json`.

### nginx
Backup `crm.bak-6.3`; `nginx -t` OK; reload OK. Mapas de localização conforme
topologia acima. `/auth/health` 200, `openid-configuration` 200.

### Compose / `.env`
- `auth-service`: `AUTH_GATEWAY_ENABLED=true`, `CLIENT_ID=crm-gateway`,
  `REDIRECT_URI=https://srv1348261.hstgr.cloud/auth/callback`,
  `ALLOWED_REDIRECT_URIS=https://srv1348261.hstgr.cloud/login,https://srv1348261.hstgr.cloud/dashboard,https://srv1348261.hstgr.cloud`,
  `ISSUER_URI=https://srv1348261.hstgr.cloud/realms/CRM`, `SESSION_STORE=redis`,
  `SESSION_TTL=8h`, `APP_BASE_URL=https://srv1348261.hstgr.cloud`, `REDIS_HOST=redis`,
  `REDIS_PORT=6379`, `REDIS_PASSWORD=...`, `extra_hosts:
  ["srv1348261.hstgr.cloud:76.13.237.238"]`.
- `pom.xml`: dependência `commons-pool2` (pool do Lettuce).
- `application.yml`: `spring.data.redis.host/port/password` (não URL — ver descobertas).

## Decisões de projeto

| Tema | Decisão |
|------|---------|
| Storage de sessão | `GatewaySessionStore` é interface; `RedisGatewaySessionStore` serializa a `GatewaySession` completa em JSON com TTL nativo do Redis (expiração + tombstone de 5m) |
| Expiração lógica | Na leitura, sessão expirada resolve `EXPIRED` (não `NOT_FOUND`); remoção também via TTL nativo |
| Tombstone | Revogação grava `revokedAt`; `@JsonIgnore` em `GatewaySession.isRevoked()`; sessão ativa = `revokedAt == null` + `expiresAt` futuro (não há campo `status`) |
| Lock distribuído | `gateway:refresh-lock:<token>` — `SET NX TTL 30s`, acquire 5s, release via Lua |
| PKCE server metadata | Keycloak admin API v2 **rejeita** `pkceCodeChallengeMethod`/`postLogoutRedirectUris` no PUT (`400 Unrecognized field`); usar `attributes`: `pkce.code.challenge.method=S256`, `post.logout.redirect.uris` |
| Post-logout URI | Wildcard same-origin (ver descobertas) |
| Logs | Eventos de diagnóstico; nunca tokens/cookies/client_secret (varredura confirma auth-service limpo) |

## Descobertas críticas

1. **`post.logout.redirect.uris` no Keycloak 26.3 é entrada única** — lista separada por
   vírgula rejeitada (`LOGOUT_ERROR invalid_redirect_uri`). Adotado o **wildcard
   same-origin** `https://srv1348261.hstgr.cloud/*` (mesmo padrão do `crm-frontend`),
   validado em `/login`, `/dashboard`, `/`, `/auth/callback`.
2. **Hairpin/DNS na VPS** — `/etc/hosts` com `127.0.1.1 srv1348261.hstgr.cloud` impedia o
   auth-service de alcançar o issuer público (`TOKEN_EXCHANGE_FAILED` → 502). Fix:
   `extra_hosts: ["srv1348261.hstgr.cloud:76.13.237.238"]` (hairpin pelo IP público).
3. **`spring.data.redis.url` + `password`** — o Spring Boot ignora `password` quando a
   `url` está presente. Trocado para `host`/`port`/`password`; `.env.example`/compose
   ajustados (`REDIS_URL` → `REDIS_HOST`/`REDIS_PORT`).
4. **Keycloak 26.3: health vai para a porta de gerenciamento 9000** (não mais na 8080).
   O healthcheck do compose (`/auth/health` + grep `UP`) **nunca funcionou** nesta versão
   (pré-existente). Corrigido para `GET /health` em `127.0.0.1:9000`.
5. **Hardening (achado + correção)**: o master admin do Keycloak aceitava `admin`/`admin`
   (o `.env` tinha senha de 64 chars que nunca foi aplicada) e a porta 8080 era publicada
   em `0.0.0.0` com ufw inativo → console admin exposto na Internet. **Corrigido**:
   senha rotacionada via admin API (valor forte 64 hex, persistido nos `.env`; `admin`
   agora retorna 401) e porta restrita a `127.0.0.1` (recreate preservando volume).
   Outras portas publicadas (5432/6379/5672/15672/9000/9001) têm credenciais, mas
   permanecem expostas — **recomendação** de bind em loopback/ufw em sprint de infra.

## Segurança / limpeza

- `/var/log/nginx/access.log` truncado (47 entradas com authorization codes /
  `id_token_hint` dos testes E2E). Varredura: auth-service 0 ocorrências sensíveis;
  keycloak 1 ocorrência = só o **nome** do evento `REFRESH_TOKEN_ERROR`.
- Removidos no VPS: `/tmp/auth-service.tar.gz`, `/tmp/check_kc*.py`,
  `/tmp/debug_kc.py`, `/tmp/pkce_test.py`, `/root/.e2e-test-pw`.
- Removidos localmente (Temp/opencode): `kc_setup*.py`, `kc_attrs.py`, `kc_keys.py`,
  `kc_probe.py`, `kc_wildcard.py`, `kc_logout_*.py`, `kc_frontend_logout.py`,
  `compose_env.py`, `patch_compose.py`, `pkce_test.py`, `e2e_gateway.py`,
  `e2e_smoke.py` (continham credenciais hardcoded).
- Backups mantidos: `/opt/crm/{.env,docker/.env,docker-compose.yml}.bak-6.3-sec`,
  `docker-compose.yml.bak-6.3-healthcheck`, `crm-gateway-client.backup.json`,
  nginx `crm.bak-6.3`.
- Usuário fake `nao.linkado@crm.local` removido pelo cleanup do próprio E2E.

## Testes

**Unit/integração** (`mvn clean verify` no auth-service): **146 testes, 0 falhas, 0
erros** (BUILD SUCCESS), incluindo `RedisGatewaySessionStoreTest` (12), locks,
serialização, deserialização corrompida (remove key), regressão das Sprints 6.1/6.2.

**E2E real** (nginx → auth-service → Keycloak + Redis): **58 PASS / 0 FAIL** (18
cenários). Cobertura: health; descoberta OIDC; authorize 302 com state/nonce/PKCE S256 e
sem segredos na URL; open redirect negado; login completo; cookies
(HttpOnly/Secure/SameSite=Lax, não-JWT, XSRF não-HttpOnly); sessão no Redis com TTL
nativo/token/roles; refresh 204 com CSRF, 403 sem CSRF, 403 sem sessão (CSRF-first),
204 em reuse; `403 PROVISIONING_REQUIRED` para usuário sem CRM access; logout (302 +
`end_session` com `post_logout_redirect_uri=/login`, tombstone `revokedAt` no Redis);
rotação de refresh_token; logout default para `APP_BASE_URL`; provisioning intacto.

`git diff --check` limpo. `.env.example` sem segredos (placeholders `CHANGE_ME_*`).

## Limitações conhecidas

- Portas não-Keycloak publicadas em `0.0.0.0` (postgres/redis/rabbitmq/minio) — têm
  autenticação, mas recomenda-se bind em loopback ou ufw (sprint de infra).
- `id_token_hint` permanece visível (como parâmetro de query) no `end_session_endpoint`
  do Keycloak — comportamento padrão do protocolo; nginx access log é truncado após os
  testes.
- Frontend ainda não consome o Gateway (Sprint 6.4 não iniciada).

## Próxima fase

**Sprint 6.4** — integração do frontend (`/login`, `/auth/callback`) com o Gateway OIDC.

---

*Data: 2026-08-03 — Sprint 6.3 concluída.*
