# Sprint 6.1 — Auth Service Gateway OIDC (Authorization Code + PKCE)

## Identificação
- **Sprint:** 6.1
- **Nome:** Auth Service Gateway OIDC
- **Data:** 2026-08-02
- **Status:** ✅ **Concluída**
- **Responsável:** AI Agent
- **Fase:** Segurança (Sprint 6 — Access Gateway, fase 2b núcleo)

## Escopo
Transformar o `auth-service` de apenas Resource Server stateless em um **Gateway OIDC**
(client Authorization Code + PKCE S256) capaz de iniciar o login no Keycloak, receber o
callback, trocar o código **server-side**, validar os tokens, aplicar o **CRM Access Gate**
existente e criar uma **sessão de browser** (cookie HttpOnly/SameSite/Secure).

**Fora de escopo nesta sprint (fases posteriores):** frontend, nginx, configuração
definitiva do client `crm-gateway` no Keycloak, logout (6.2), migração de storage (6.4),
Keycloak VPS (6.3).

---

## Arquitetura

```
Browser
   |
   | GET /auth/authorize            (redirect opcional, allowlist)
   v
Auth Service (Gateway OIDC — OAuth2 Client)
   |  gera state + nonce + PKCE S256
   |  estado transitório em memória (OidcAuthorizationRequestStore)
   | 302
   v
Keycloak (Authorization Server / IdP exclusivo)
   |  login
   | 302 /auth/callback?code=...&state=...
   v
Auth Service
   |  valida state (single-use, expiração)
   |  token exchange server-side (code + code_verifier + client_secret)
   |  valida ID token (JWKS, issuer, aud, azp, exp, nonce) e access token
   |  extrai AuthenticatedIdentity (KeycloakIdentityConverter)
   |  CRM Access Gate reutilizado (CurrentUserResolutionUseCase)
   |  cria sessão server-side (GatewaySessionStore) + cookie opaco
   | 302
   v
Frontend (redirect previamente validado)
```

- O **Keycloak** permanece como emissor exclusivo de tokens (não foi criado JWKS próprio).
- O fluxo atual do frontend (keycloak-js direto) **não foi alterado** — o Gateway existe
  em paralelo nesta fase (Sprint 6.1, regra de compatibilidade).

## Endpoints

| Endpoint | Acesso | Função |
|----------|--------|--------|
| `GET /auth/authorize` | Público | Inicia o fluxo OIDC (valida redirect, gera state/nonce/PKCE, 302 → Keycloak) |
| `GET /auth/callback` | Público | Recebe `code`+`state`, troca no servidor, valida tokens, CRM Access, cria sessão |
| `GET /auth/health` | Público | Healthcheck (`200 {"status":"UP"}`) |
| `GET /internal/auth/current-user` | Autenticado (JWT Keycloak) | Resolução de CurrentUser (inalterado) |

## Fluxo authorize

`GET /auth/authorize?redirect=/dashboard`

1. Valida o `redirect` contra a allowlist (`RedirectUriValidator`) — relativo de mesmo
   origin aceito; absoluto exige https (ou http localhost) e origem na allowlist.
2. Gera `state` (SecureRandom, 32 bytes, URL-safe) — correlation da tentativa.
3. Gera `nonce` (32 bytes) — anti-replay para o ID token.
4. Gera PKCE: `code_verifier` (48 bytes) + `code_challenge` S256.
5. Armazena o estado transitório (`OidcAuthorizationRequest`) com TTL
   (`auth.gateway.authorization-request-ttl`, default 10m).
6. Monta a URL de autorização do Keycloak com `client_id` e `redirect_uri` **sempre da
   configuração do servidor** (nunca aceitos do browser) e responde `302 Found`.

## Fluxo callback

`GET /auth/callback?code=...&state=...`

1. Rejeita callback sem `code`/`state` ou com `error` do IdP (`OIDC_ERROR`).
2. **Consome** o `state` (single-use): desconhecido, expirado ou reutilizado → `INVALID_STATE`.
3. Troca o código no token endpoint do Keycloak **server-side** (`RestClientOidcTokenClient`)
   com `code_verifier` do estado + `client_secret` da configuração.
4. Valida o **ID token** e o **access token** criptograficamente (JWKS) + claims.
5. Converte o ID token em `AuthenticatedIdentity` (mesmo conversor do resource server).
6. Executa o **CRM Access Gate** reutilizado (`CurrentUserResolutionUseCase.resolve`).
7. Cria a sessão (`GatewaySession`) e emite o cookie opaco (`GatewayCookieFactory`).
8. Responde `302` para o redirect previamente validado.

## State

- Gerado com `SecureRandom` (32 bytes) — imprevisível.
- Associado ao estado transitório (`OidcAuthorizationRequest`) que guarda `nonce`,
  `code_verifier` e o `redirectTarget` permitido.
- **Single-use**: `consume()` é atômico (`AtomicBoolean.compareAndSet` + `remove` do mapa).
- **Expira**: TTL de 10m (configurável); `@Scheduled` purga expirados.
- Rejeição em qualquer falha → `400 INVALID_STATE` (mensagem genérica, sem detalhes internos).

## Nonce

- Gerado por tentativa, enviado ao Keycloak como query param.
- Recuperado do estado transitório no callback (o browser nunca envia o nonce esperado).
- Validado contra o claim `nonce` do ID token. Divergência → rejeição
  (`TOKEN_VALIDATION_FAILED`).

## PKCE (RFC 7636, S256)

- `code_verifier`: 48 bytes URL-safe sem padding (unreserved, 43–128 chars).
- `code_challenge = Base64URL( SHA-256(code_verifier) )` sem padding.
- Enviado na URL de autorização: `code_challenge` + `code_challenge_method=S256`.
- No callback, o `code_verifier` original (do estado transitório) é usado no token
  exchange. O verifier **nunca** chega ao frontend, **nunca** é logado e é descartado
  junto com o estado.

## Token exchange

`POST {token-endpoint}` com `application/x-www-form-urlencoded`:

```
grant_type=authorization_code
code=<authorization_code>
redirect_uri=<redirect_uri fixa do gateway>
client_id=crm-gateway
client_secret=<secret da configuração>
code_verifier=<verifier do estado>
```

- Ocorre **exclusivamente no Auth Service** (o browser nunca recebe tokens).
- Timeout configurável (`token-exchange-timeout`, default 10s) + connect timeout 5s.
- Falhas mapeadas: HTTP 400/401/5xx → `TOKEN_EXCHANGE_FAILED`; timeout →
  `TOKEN_EXCHANGE_TIMEOUT`; resposta sem `access_token`/`id_token` →
  `TOKEN_RESPONSE_INVALID`. Nenhuma stack trace sensível chega ao cliente.

## Token validation

- Assinatura/exp/issuer verificados pelo `JwtDecoder` (JWKS do Keycloak via
  `jwk-set-uri`). Não se confia em decode sem validação criptográfica.
- **ID token**: `iss` == issuer configurado; `aud` contém o clientId; `azp` (quando
  presente) == clientId; `exp`/`iat` com tolerância de clock skew; **`nonce`** == estado.
- **Access token**: `iss`; `aud` contém clientId ou audience configurada
  (`token-audiences`); `exp`/`iat`.
- Rejeição → `401 TOKEN_VALIDATION_FAILED`.

## Session

- Criada **somente** após CRM Access positivo.
- Dados server-side (`GatewaySessionStore` em memória): sessionToken opaco, userId, email,
  companyId, tenantId, roles, permissions, keycloakSub, keycloakSessionId, provider,
  displayName, createdAt, expiresAt (TTL `session-ttl`, default 8h).
- Cookie (`crm_session`): valor = `sessionToken` opaco, `HttpOnly`, `Secure` (default),
  `SameSite=Lax`, `Max-Age` = TTL.
- Sessão expirada é invalidada na leitura e purgada por agendamento.

## CRM Access

- **Reutiliza** o `CurrentUserResolutionUseCase` existente — não foi criado gate paralelo.
- Cadeia: JWT/OIDC → `users.keycloak_sub` → usuário → empresa →
  `is_active=true AND crm_enabled=true AND company.status.canOperate()`.
- Falha → `403 CRM_ACCESS_DENIED` (sem sessão).
- Identidade sem usuário CRM → `403 PROVISIONING_REQUIRED` (provisionamento continua no
  crm-backend; não é desta sprint).

## Security decisions

| Tema | Decisão |
|------|---------|
| `client_id` | Sempre da configuração (`auth.gateway.client-id`), nunca do browser |
| `redirect_uri` do token exchange | Fixa do servidor (`auth.gateway.redirect-uri`) |
| Open redirect | Bloqueado (`//evil`, `https://evil`, http não-localhost, malformed) |
| Cookie | HttpOnly + SameSite=Lax + Secure; **JWT nunca** no cookie (valor opaco) |
| Storage | Estado/sessão server-side (memória, nó único; Redis em sprint futura) |
| `code_verifier`/secrets | Nunca logados, nunca enviados ao frontend |
| Logs | Eventos de diagnóstico com correlation=state; nunca tokens/codes/verifier/cookie |
| Prod inseguro | `secure-cookie=false` + profile `prod` → falha de startup (guard em `GatewayConfig`) |
| SecurityConfig | Apenas `/auth/health`, `/auth/authorize`, `/auth/callback` (e actuator health) são públicos; resto autenticado; sem `anyRequest().permitAll()` |

## Testes

`mvn test` no `auth-service`: **83 testes, 0 falhas, 0 erros** (BUILD SUCCESS).

Cobertura dos 30 cenários obrigatórios da Sprint 6.1:

| Área | Cenários |
|------|----------|
| Authorization | 302 + URL com response_type=code, client_id, redirect_uri, scope=openid, state, nonce, code_challenge, S256; state único; nonce único; PKCE S256 |
| Redirect security | `/dashboard` aceito; `https://evil.example` rejeitado; `//evil.example` rejeitado |
| Callback | state válido; inválido; expirado; reutilizado; nonce válido; inválido |
| Token exchange | code+verifier OK; 400 controlado; timeout controlado |
| Token validation | issuer OK/incorreto; aud incorreta; assinatura inválida; expirado; nonce incorreto |
| CRM Access | ativo+crm_enabled+ACTIVE → permitido; crm_enabled=false → 403; inativo → 403; empresa não-operável → 403 |
| Session | cookie HttpOnly/Secure/SameSite; sem JWT puro; expiração; estado consumido após callback |
| Regressão | `/internal/auth/current-user` e `/auth/health` seguem funcionando |

## Limitações conhecidas

- **Estado e sessão em memória** (`ConcurrentHashMap`): adequado a nó único; a migração
  para Redis (estado compartilhado entre réplicas) pertence à Sprint 6.2/6.3.
- **Validação runtime local**: impossível nesta máquina (sem Postgres/Docker local; VPS
  fora de escopo na 6.1). Os comportamentos de `/auth/health` e `/auth/authorize` foram
  validados por testes de slice (MockMvc) que carregam o SecurityConfig real.
- **Client `crm-gateway` no Keycloak** ainda incompleto/desabilitado — a ativação e o
  ajuste de redirect URIs pertencem à **Sprint 6.3**.
- **Bug de recursão da Sprint 5** (`AuthServiceCurrentUserResolver` → fallback → 
  `AuthenticationManager` → StackOverflowError): **não foi tocado** nesta sprint
  (não bloqueia o Gateway, que resolve identidade diretamente via use case).

## Próxima fase

**Sprint 6.2 — Logout + sessão**: `end_session_endpoint` do Keycloak (logout OIDC
coerente), revogação de sessão do gateway, exposição de estado de sessão para o
frontend, e (se necessário) migração do armazenamento de sessão para Redis.

---

*Data: 2026-08-02 — Sprint 6.1 concluída.*
