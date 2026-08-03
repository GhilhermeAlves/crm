# Sprint 6.2 — Auth Service Gateway OIDC (Logout + Ciclo de Vida da Sessão)

## Identificação
- **Sprint:** 6.2
- **Nome:** Completing gateway session lifecycle
- **Data:** 2026-08-02
- **Status:** ✅ **Concluída**
- **Responsável:** AI Agent
- **Fase:** Segurança (Sprint 6 — Access Gateway, fase 2c ciclo de vida)

## Escopo
Completar o ciclo de vida da sessão de browser do Access Gateway **exclusivamente no
`auth-service`**, sobre a baseline da Sprint 6.1 (commit `9ffd7bc`):

- **Logout** (`GET /auth/logout`) — RP-Initiated Logout via `end_session_endpoint` do
  Keycloak (descoberta OIDC), invalidação local **idempotente** e independente da
  disponibilidade do provedor.
- **Refresh** (`POST /auth/refresh`) — renovação de tokens **server-side** com rotação,
  lock por sessão, respeito ao TTL absoluto e resposta vazia (tokens nunca voltam ao
  browser).
- **Ciclo de vida** — TTL absoluto + idle timeout; resolução de sessão distinguindo
  `ACTIVE / EXPIRED / REVOKED / NOT_FOUND`; tombstones para sessões revogadas.
- **CSRF** cookie-to-header em `POST /auth/refresh`.

**Fora de escopo (fases posteriores):** frontend, nginx, Keycloak, backend CRM, VPS,
Sprints 6.3/6.4. Não foi reconstruído Authorization Code/PKCE/state/nonce/token
exchange/validação/CRM Access da 6.1. Redis continua pendente (documentado abaixo).

---

## Arquitetura

```
Browser
   |
   | GET /auth/logout?post_logout_redirect_uri=...   (cookie crm_session)
   v
Auth Service
   |  resolve sessão (SessionLookup: ACTIVE/EXPIRED/REVOKED/NOT_FOUND)
   |  revoga sessão local (tombstone) — SEMPRE, mesmo se IdP indisponível
   |  descoberta OIDC (/.well-known/openid-configuration) → end_session_endpoint
   |  monta URL: client_id + post_logout_redirect_uri (validado) + id_token_hint
   | 302 + Set-Cookie crm_session=; Max-Age=0
   v
Keycloak (end_session_endpoint) → redirect pós-logout validado

   | POST /auth/refresh   (cookie sessão + cookie CSRF + header X-XSRF-TOKEN)
   v
Auth Service
   |  GatewayCsrfFilter (cookie CSRF == header)
   |  resolve sessão ACTIVE → lock por sessão
   |  token endpoint: grant_type=refresh_token (rotação) — server-side
   |  atualiza tokens/lastAccessedAt na sessão; respeita TTL absoluto
   | 204 No Content (nunca tokens para o browser)
```

## Endpoints

| Endpoint | Acesso | Função |
|----------|--------|--------|
| `GET /auth/logout` | Público (cookie + SameSite=Lax) | Invalida a sessão local (idempotente), limpa o cookie e redireciona ao `end_session_endpoint` |
| `POST /auth/refresh` | Público na cadeia + `GatewayCsrfFilter` | Renova tokens no servidor (rotação, lock por sessão); resposta `204` |
| `GET /auth/authorize`, `GET /auth/callback` | Público | Inalterados da Sprint 6.1 |
| `GET /auth/health` | Público | Inalterado |
| `GET /internal/auth/current-user` | Autenticado (JWT Keycloak) | Inalterado |

## Fluxo logout

`GET /auth/logout?post_logout_redirect_uri=...`

1. Valida `post_logout_redirect_uri` pelo **mesmo** `RedirectUriValidator` da 6.1
   (allowlist; relativo de mesmo origin aceito; `//evil` bloqueado). Ausente → default.
2. Alvo relativo é absolutizado com `auth.gateway.app-base-url` (o Keycloak exige URI
   absoluta registrada no client).
3. Resolve a sessão e lê o `id_token_hint` **server-side** (nunca do browser).
4. **Revoga a sessão local** (tombstone) — ocorre **sempre**, mesmo que o provedor esteja
   indisponível; um replay do cookie antigo passa a resolver `REVOKED`.
5. Busca `end_session_endpoint` via descoberta OIDC (`OidcProviderMetadata`, cache 5m).
6. Monta a URL: `client_id` + `post_logout_redirect_uri` (validado) + `id_token_hint`.
7. Responde `302` para a URL e `Set-Cookie: crm_session=; Max-Age=0` (mesmo Path/Domain/
   SameSite/Secure).

**Decisão GET:** logout permanece **GET** por ser navegação (RP-Initiated Logout OIDC
padrão). O efeito colateral (destruir a própria sessão) é aceitável: o cookie de sessão é
`SameSite=Lax`, então uma requisição **cross-site** não o envia, e CSRF cookie-to-header
é aplicado apenas a endpoints mutáveis com autenticação por cookie (`/auth/refresh`).
Coberto nos testes: `shouldNotInterceptGetRequests` / `shouldNotInterceptOtherPaths`.

**Degradação:** se a descoberta/`end_session_endpoint` falhar (`OIDC_PROVIDER_UNAVAILABLE`),
a sessão local já foi revogada e o redirecionamento segue para o alvo local validado —
nunca o logout fica refém da rede.

## Fluxo refresh

`POST /auth/refresh` (cookie sessão + cookie CSRF + header `X-XSRF-TOKEN`)

1. `GatewayCsrfFilter`: compara cookie `XSRF-TOKEN` com o header `X-XSRF-TOKEN`
   (idênticos obrigatórios); divergência/ausência → `403 CSRF_INVALID`.
2. Resolve a sessão pelo `sessionToken` do cookie:
   `NOT_FOUND → 401 SESSION_NOT_FOUND`, `EXPIRED → 401 SESSION_EXPIRED`,
   `REVOKED → 401 SESSION_REVOKED`.
3. **Lock por sessão** (`GatewaySessionStore.lockFor`) serializa a rotação — nunca lock
   global.
4. Re-resolve sob o lock (re-verifica ACTIVE/revogada/expirada).
5. `grant_type=refresh_token` no token endpoint do Keycloak (server-side, `client_secret`
   do servidor, **sem** `redirect_uri`).
6. Rotaciona tokens na sessão (`withRotatedTokens`), renova `lastAccessedAt`.
7. **Nunca estende o TTL absoluto** — se a sessão não estiver mais ativa após a rotação,
   é revogada e retorna `401 SESSION_EXPIRED`.
8. Confirma que a sessão não foi revogada durante a chamada de rede (race com logout).
9. Responde `204 No Content` — nenhum token vai para o browser.

**Erros de refresh** (propagados e com a sessão **revogada** — rotação inválida ⇒ possível
roubo ⇒ derruba a sessão): `REFRESH_TOKEN_INVALID` (provedor rejeita o refresh token,
ex.: `invalid_grant`), `REFRESH_FAILED` (outros erros do endpoint),
`OIDC_PROVIDER_UNAVAILABLE` (rede/timeout).

## Ciclo de vida da sessão

- **TTL absoluto** (`expiresAt` = login + `session-ttl`, default 8h) e **idle timeout**
  (`lastAccessedAt` + `session-idle-timeout`, default desabilitado via `0s`).
- **Expiração efetiva** = `min(expiresAt, lastAccessedAt + idleTimeout)`
  (`GatewaySession.effectiveExpiration`).
- Resolução via `SessionLookup` (`ACTIVE/EXPIRED/REVOKED/NOT_FOUND`); sessão ativa renova
  `lastAccessedAt` a cada acesso; expirada é removida na leitura e purgada por
  `@Scheduled`.
- **Revogação** deixa tombstone (`revokedAt`) retido por 5m — distingue `REVOKED` de
  `NOT_FOUND` para replay de cookie antigo.
- `id_token_hint` é mantido **server-side** na sessão e some com ela.
- Sessão serializável (apenas String/UUID/Instant/List/boolean) — pronta para Redis.

## CSRF (cookie-to-header)

| Tema | Decisão |
|------|---------|
| Estratégia | Cookie `XSRF-TOKEN` (não HttpOnly — o JS lê e envia no header) + header `X-XSRF-TOKEN` |
| Onde | Somente `POST /auth/refresh` (`GatewayCsrfFilter` + `FilterRegistrationBean`, URL `/auth/refresh`, depois do chain Spring Security) |
| Código de erro | `403 CSRF_INVALID` (JSON no padrão do projeto) |
| `SecurityConfig` | `/auth/logout` e `/auth/refresh` públicos na cadeia; `anyRequest().permitAll()` continua proibido; resource server preservado |
| Token CSRF | Nunca é sessionToken/access/refresh/state/nonce; gerado por login (SecureRandom, 32 bytes) e guardado na sessão |
| GET | Não interceptado (logout/authorize/callback) — decisão documentada acima |

## Security decisions

| Tema | Decisão |
|------|---------|
| Logout idempotente | Sessão local é revogada sempre; repetir logout não falha |
| IdP indisponível no logout | Não bloqueia: invalidação local + redirect local (log `OIDC_PROVIDER_UNAVAILABLE`) |
| `post_logout_redirect_uri` | Mesma política de allowlist da 6.1 (`RedirectUriValidator`) |
| `id_token_hint` | Somente do servidor (sessão); nunca aceito como parâmetro |
| Tokens no refresh | Rotações server-side; resposta `204` — tokens jamais retornam ao browser |
| Rotação de refresh token | Lock por sessão; falha ⇒ sessão revogada (rotação comprometida) |
| TTL absoluto | Refresh nunca estende `expiresAt` |
| Tombstone | `REVOKED` distinguível de `NOT_FOUND` por retenção curta (5m) |
| Logs | Eventos de diagnóstico (started/success/failed, session invalidated, csrf rejected, expired); nunca tokens/sessionToken/cookie/csrf/client_secret |
| Prod inseguro | Guard da 6.1 preservado (`secure-cookie=false` + `prod` ⇒ falha de startup) |

## Testes

`mvn clean test` no `auth-service`: **134 testes, 0 falhas, 0 erros** (BUILD SUCCESS).
`mvn verify` também BUILD SUCCESS. `git diff --check` sem erros de whitespace. Varredura
estática do diff sem segredos vazados.

Cobertura dos cenários obrigatórios da Sprint 6.2:

| Área | Cenários |
|------|----------|
| Sessions (10) | store/retrieve; unknown/null → NOT_FOUND; expiração absoluta (EXPIRED + remoção); idle timeout; touch de `lastAccessedAt`; revogação com tombstone (REVOKED); idempotência de revogação; retenção de tombstone (5m) e purga |
| Cookie (5) | sessão HttpOnly/Secure/SameSite; Secure desabilitado; CSRF legível por JS; cookie expirado (Max-Age=0); leitura session/csrf do request |
| Logout (9) | end_session redirect com client_id/post_logout/id_token_hint; default sem parâmetro; degradação com IdP indisponível; idempotência (sessão desconhecida/revogada); open redirect bloqueado; allowlist absoluta; rejeição fora da allowlist; absolutização com app-base-url |
| Refresh (10) | rotação server-side; refresh token correto enviado; lastAccessedAt atualizado; TTL absoluto não estendido; SESSION_NOT_FOUND/EXPIRED/REVOKED; REFRESH_TOKEN_INVALID (provedor e sem refresh token) com revogação; OIDC_PROVIDER_UNAVAILABLE com revogação; concorrência serializada por lock |
| CSRF (6) | cookie==header ok; header ausente; cookie ausente; tokens divergentes; GET não interceptado; outros paths não interceptados |
| Token client (refresh) | grant_type/refresh_token sem redirect_uri; invalid_grant → REFRESH_TOKEN_INVALID; invalid_scope → REFRESH_FAILED; timeout → OIDC_PROVIDER_UNAVAILABLE; sem access_token → REFRESH_FAILED |
| Metadata | descoberta end_session_endpoint; cache (1 hit); 404 → OIDC_PROVIDER_UNAVAILABLE; endpoint ausente → OIDC_PROVIDER_UNAVAILABLE |
| Controller | logout 302 + limpa cookie; logout sem sessão (idempotente); refresh 204; refresh sem sessão → 401 SESSION_NOT_FOUND; callback agora também emite cookie CSRF |
| Regressão | `/internal/auth/current-user` autenticado; authorize/callback públicos; testes da 6.1 intactos |

## Limitações conhecidas

- **Sessão/estado em memória** (`ConcurrentHashMap`): adequado a nó único. A migração para
  Redis (compartilhada entre réplicas, com expiração nativa e locks distribuídos) fica
  **pendente** — o `GatewaySession` já é serializável e o `GatewaySessionStore` é a única
  abstração de storage, então a troca é localizada.
- **Validação runtime local**: impossível nesta máquina (sem Postgres/Docker/Keycloak
  locais; VPS fora de escopo). Endpoints validados por testes de slice (MockMvc) e unit.
- **E2E real (Keycloak + nginx + frontend)**: **PENDENTE — Sprint 6.3 / E2E**.
- **Client `crm-gateway` no Keycloak**: `post_logout_redirect_uri` precisa ser registrado
  como redirect URI válida no client (configuração da Sprint 6.3).
- **Bug de recursão da Sprint 5** (`AuthServiceCurrentUserResolver` → fallback →
  `AuthenticationManager` → StackOverflowError): **não tocado** (não afeta o Gateway).

## Próxima fase

**Sprint 6.3** — Ativação/E2E no Keycloak real (client `crm-gateway`, redirect URIs,
post_logout_redirect_uri) e, se necessário, migração de storage de sessão para Redis.

---

*Data: 2026-08-02 — Sprint 6.2 concluída.*
