# Sprint 6 — SECURITY.md (Matriz de Segurança)

> Aplicável ao **Access Gateway** (Auth Service), **frontend**, **backend** e **Keycloak**.
> Estado atual = auditado na fase 1; Alvo = implementação da Sprint 6.

## 1. Matriz Resumo

| # | Item | Estado atual | Alvo (Sprint 6) | Ação |
|---|---|---|---|---|
| S1 | **CSRF** | Backend: resource server stateless, CSRF desabilitado (sem cookies de sessão no backend). Fluxo OIDC usa `state` | Garantir `state` aleatório+validado em `/auth/callback`; se houver cookie de sessão, exigir proteção CSRF (SameSite + token/referer) nos endpoints de estado mutante | Revisar config + fluxo |
| S2 | **XSS** | Tokens em `localStorage` (`kc_accessToken`/`kc_refreshToken`) | Tokens fora do JS (memória/cookie HttpOnly); sanitização/output encoding do frontend | Migração de storage |
| S3 | **Token leakage** | Access/refresh no `localStorage` | Cookies `HttpOnly`/`SameSite`/`Secure`; access em memória | Migração de storage |
| S4 | **Refresh storage** | `localStorage` | Cookie `HttpOnly` (nunca legível por JS) | Migração de storage |
| S5 | **Cookies** | `kc_authenticated`: `SameSite=Lax`, 7d (só flag SSR) | Sessão do gateway: `HttpOnly` + `SameSite=Strict/Lax` + `Secure` (https); flag SSR sem dados sensíveis | Config do Auth Service |
| S6 | **CORS** | Configurado no backend (origem do frontend) | Permitir somente origens/domínios controlados do CRM (https), nunca `*`; validar allowlist de redirect URIs | Config + teste |
| S7 | **Redirect URI** | Keycloak `redirect_uri` do frontend (`/auth/callback`) | `redirect_uri` fixa do **Auth Service** na allowlist do client OIDC; rejeitar qualquer outra | Keycloak + Auth Service |
| S8 | **Open redirect** | `redirect=...` propagado no query do fluxo | Validar `redirect` contra allowlist de destinos internos; `post_logout_redirect_uri` na allowlist | Auth Service |
| S9 | **Session fixation** | Sessão criada após autenticação no Keycloak | Sempre **regenerar/emitir nova sessão** após o login; nunca reutilizar id pré-login; `state` impede login fixation | Auth Service |
| S10 | **Replay** | — | `state` + `nonce` únicos por fluxo; validar `nonce` no ID token; `exp`/`iat`; rejeitar reutilização | Auth Service |
| S11 | **Logout coerente** | Logout via `keycloak-js` (`kc.logout`) | RP-initiated logout (`end_session_endpoint` + `id_token_hint`) encerrando Keycloak + sessão do gateway + estado do frontend; **sem logout parcial** | Auth Service + Frontend |
| S12 | **Token expiration** | `onTokenExpired` → refresh (30s); 401 → retry + refresh | Re-autorização via Auth Service; refresh token em cookie HttpOnly; expiração de sessão com redirect limpo | Frontend + Auth Service |
| S13 | **Validação JWT** | Backend/auth-service validam issuer, assinatura (JWKS), exp | Manter; Auth Service valida também `aud`/`azp`/`nonce` no fluxo de callback | Auth Service |

---

## 2. Detalhamento por Componente

### 2.1 Keycloak (não alterar pontos existentes)
- OIDC, PKCE, JWT, issuer validation, JWKS, roles: **mantidos** (regra absoluta).
- **Alterações**: client OIDC do Auth Service com redirect URIs na allowlist (ex.:
  `https://.../auth-service/callback` ou path definido), PKCE habilitado; `end_session_endpoint` para logout coerente.
- Senhas/credentials permanecem exclusivamente no Keycloak.

### 2.2 Auth Service (Access Gateway)
| Controle | Detalhe |
|---|---|
| `state` | Gerado por fluxo, validado no retorno do callback (anti-CSRF, login fixation) |
| `nonce` | No ID token; validado no callback (anti-replay) |
| PKCE (S256) | `code_challenge`/`code_verifier` no servidor (client público seguro) |
| Troca de código | Apenas server-side (`/token`), nunca no browser |
| Sessão | Cookie `HttpOnly`, `SameSite=Strict` (ou `Lax`), `Secure` em https; novo id após login |
| Decisão de acesso | CRM access (is_active + crm_enabled + company ACTIVE) antes de emitir sessão |
| Logout | `end_session_endpoint` + `id_token_hint`; `post_logout_redirect_uri` na allowlist |

### 2.3 Backend (resource server — preservado)
- Validação stateless do JWT via JWKS do Keycloak (Sprint 1/4) — **mantida**.
- `TenantFilter` + `TenantContext` + RLS FORCE (Sprint 5) — **mantidos**.
- Adicionar o gate de CRM access no caminho de resolução (`/auth/me` / `CurrentUserResolver`).
- Não emite tokens; não confia em `companyId` do cliente.

### 2.4 Frontend
| Item | Ação |
|---|---|
| Storage de tokens | Migrar de `localStorage` → memória + cookie `HttpOnly` (sessão) |
| Início de login | Redirecionar para o Auth Service (`/auth/authorize`), não direto ao Keycloak |
| Callback | Não processar `code` no browser (fica no Auth Service) |
| Logout | Chamar `/auth/logout` do Auth Service; limpar estado local após confirmar |
| Middleware SSR | Usar apenas flag de sessão (sem tokens/claims) |

---

## 3. Casos de Abuso a Testar (E2E/segurança)

| # | Ataque | Mitigação | Teste |
|---|---|---|---|
| 1 | Reutilizar `code` do Keycloak | Code é single-use (Keycloak); troca server-side | Replay do code → rejeitado |
| 2 | `state` inválido/faltando | Validar `state` antes de trocar o code | Callback sem state → erro, sem sessão |
| 3 | `nonce` inválido | Validar `nonce` do ID token | Token com nonce errado → rejeitado |
| 4 | `redirect` externo | Allowlist de destinos | `?redirect=https://evil.com` → recusado |
| 5 | `redirect_uri` arbitrária | Allowlist no Keycloak + gateway | URI fora da lista → erro |
| 6 | Open redirect pós-logout | `post_logout_redirect_uri` allowlist | Retorno a origem externa → bloqueado |
| 7 | Token expirado/inválido | JWKS + `exp`/`iat`/issuer | Request com token expirado → 401 |
| 8 | Cookie não-HttpOnly | Cookie de sessão `HttpOnly` | `document.cookie` não expõe sessão |
| 9 | Cross-site request (CSRF) em mutações | SameSite + validação de origem | POST cross-origin → bloqueado |
| 10 | Empresa suspensa tenta login | Gate `companies.status` | Login com tenant SUSPENDED → 403 |

---

## 4. Checklist de Revisão de Segurança (Sprint 6)

- [ ] `state` e `nonce` validados em todo callback (login e logout).
- [ ] PKCE S256 implementado no Auth Service (client público).
- [ ] Troca de `code` somente server-side.
- [ ] Redirect URIs e post-logout redirect na allowlist (Keycloak + Auth Service).
- [ ] Cookies de sessão `HttpOnly` + `SameSite` + `Secure` (https).
- [ ] Tokens removidos do `localStorage`.
- [ ] CORS restrito às origens do CRM.
- [ ] JWT validado (issuer, assinatura/JWKS, exp, iat, aud/azp) no callback e no backend.
- [ ] Logout encerra Keycloak + gateway + frontend (sem sessão reutilizável).
- [ ] Gate de CRM access aplicado antes de qualquer sessão/recurso.
- [ ] `companyId` nunca aceito do cliente.

---

*Data: 2026-08-02*
