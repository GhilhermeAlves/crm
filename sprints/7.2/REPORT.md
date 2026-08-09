# Sprint 7.2 — Account Linking (Caso B/C: vínculo seguro entre conta local CRM e identidade Google)

**Data:** 2026-08-07/08 — **Ambiente:** local + VPS (produção `srv1348261.hstgr.cloud`) — **Status:** ✅ Concluída

## Identificação

- **Sprint:** 7.2
- **Nome:** Account Linking — vinculação segura entre a conta local CRM e a identidade Google
- **Responsável:** AI Agent
- **Fase:** Segurança — Identidade / Autenticação
- **Dependência:** Sprint 7.1 (Google IdP) + Sprints 6.x (Gateway/OIDC/sessão/RLS)

## Objetivo

Permitir a **vinculação segura** entre a **conta local CRM** e a **identidade Google** quando o
e-mail da identidade externa coincide com uma conta local sem `keycloak_sub`. Regra central:
**nunca vincular automaticamente por e-mail** — a identidade é derivada do JWT do Keycloak e a
senha da conta local é verificada antes do vínculo efetivo.

## Entregue em código

- **Gateway (auth-service):**
  - `PendingLink` (domínio), `PendingLinkStore`, `InMemoryPendingLinkStore` e (fechamento)
    `RedisPendingLinkStore` — armazenamento da pendência alinhado ao `AUTH_GATEWAY_SESSION_STORE=redis`.
  - `BackendIdentityClient` — `POST /internal/auth/provision` (Caso C) e `/internal/auth/link` (Caso B).
  - `CurrentUserResolution.LinkingRequired` — nunca resolve por e-mail para provedor externo.
  - `/auth/link-status` + `POST /auth/link` — CSRF cookie-to-header (`GatewayCsrfFilter`) e rate limit.
- **Backend (crm):** `LinkingRequiredException`, `linkKeycloakIdentity` (prova de senha local),
  `provisionKeycloakUser`, `IdentityInternalController` (`/internal/auth/provision`, `/internal/auth/link`),
  RLS V025 (`app.current_identity_email`).
- **Frontend:** `/link-account` + `LinkAccountForm` (exibe o e-mail do vínculo encontrado),
  `gateway-auth.ts` (`PENDING_LINK_COOKIE="crm_pending_link"`).

### Commits da Sprint

- Implementação original (2026-08-07): `0a4c83c` (backend), `def1ad5` (auth-service pending link),
  `874ca4d` (frontend link-account), `84f8922` (google provider), `ca96e58` (lockfile).
- Fechamento (2026-08-08): `15ffcf2` (RedisPendingLinkStore), `4b28975` (testes unitários),
  `8851595` (fix CSRF `/auth/link`).

## Débito registrado e resolução (fechamento 2026-08-08)

O índice registrou (2026-08-08) que a 7.2 **não** estava concluída. Itens e resolução:

| Débito | Resolução |
|--------|-----------|
| 🚧 `sprints/7.2/REPORT.md` inexistente | ✅ Criado neste fechamento (este documento). |
| 🚧 Sem validação E2E em produção | ✅ E2E na VPS (seção abaixo), incluindo sobrevivência em Redis. |
| 🚧 `PendingLinkStore` in-memory vs `AUTH_GATEWAY_SESSION_STORE=redis` | ✅ `RedisPendingLinkStore` (chave `gateway:pending-link:<token>`, TTL nativo, uso único) ativado com `session-store=redis`; `InMemoryPendingLinkStore` condicional (`memory`, `matchIfMissing`). |
| 🚧 `/auth/link-status` e `/auth/link` sem testes unitários | ✅ `GatewayOidcLinkingTest` (serviço, 11 testes) + `OidcGatewayControllerTest` (7 novos) + `RedisPendingLinkStoreTest` (11) + CSRF `GatewayCsrfFilterTest`. |
| 🚧 Reports 7.3/7.4 registram 7.2 pendente | ✅ Referências atualizadas. |

## E2E em produção (VPS — evidências)

Validação direta na VPS (`crm-auth-service`, profile `prod`, `AUTH_GATEWAY_SESSION_STORE=redis`,
Redis com senha, `dbsize` inicial = 0 sem chaves `gateway:*`):

1. **Store Redis ativo (não in-memory):** injetada uma `PendingLink` realista em
   `gateway:pending-link:e2e-pending-token-001`; `GET /auth/link-status` com o cookie
   `crm_pending_link` → `{"pending":true,"email":"e2e.local@example.com"}` (lido do Redis).
2. **Sobrevivência a reinício:** `docker compose restart auth-service`; mesmo cookie →
   `{"pending":true,"email":"e2e.local@example.com"}` (o in-memory teria perdido o estado).
3. **Expiração lógica:** injetada `PendingLink` com `expiresAt` no passado;
   `link-status` → `{"pending":false}` **e** a chave foi removida do Redis (`EXISTS` → 0).
4. **CSRF:** `POST /auth/link` sem header `X-XSRF-TOKEN` → **403 `CSRF_INVALID`**.
5. **Senha incorreta (retry permitido):** `POST /auth/link` com CSRF válido e senha errada →
   **401 `INVALID_CREDENTIALS`** e `link-status` ainda `{"pending":true}` (não consumido).
6. `GET /auth/link-status` sem cookie → `{"pending":false}`; `/auth/health` → `{"status":"UP"}`.

> Limpeza dos tokens E2E (`DEL`) executada após a validação.

## Problemas e correções (encontrados no E2E)

1. **Bug de segurança CSRF (crítico):** `GatewayConfig.gatewayCsrfFilter` registrava o
   `GatewayCsrfFilter` apenas para `/auth/refresh` (`setUrlPatterns(List.of("/auth/refresh"))`),
   embora a lógica `isProtected()` já contemplasse `/auth/link`. Em produção, `POST /auth/link`
   ficava **sem proteção CSRF** (o 500 no teste era o handler de JSON, não CSRF). Corrigido:
   `setUrlPatterns(List.of("/auth/refresh", "/auth/link"))` + teste `GatewayConfigTest`
   `shouldRegisterCsrfFilterForBothRefreshAndLink` (commit `8851595`). Revalidado na VPS →
   403.

## Testes locais (regressão)

- **Auth-service:** `mvn test` → **279 testes, 0 falhas, 0 erros** (inclui os novos
  `RedisPendingLinkStoreTest`, `GatewayOidcLinkingTest`, casos de `OidcGatewayControllerTest`,
  `GatewayCsrfFilterTest` e `GatewayConfigTest`).
- **Backend:** `mvn test` → **111 testes, 0 falhas** (inclui `AuthServiceLinkingTest` 9/9).
- **Frontend:** `vitest run` → **56/56 PASS**; `tsc --noEmit` limpo; `next lint` sem erros.

## Pendências / recomendações

- **Provedor SMS real** no lugar do `DisabledOtpSender` em produção (7.3 segue).
- **Auto-provisionamento por telefone** (criar conta via telefone, sem senha Keycloak).
- **IdP Microsoft/Apple** fora de escopo (7.0); Meta/Facebook fora de escopo.

## Resultado

STATUS: **CONCLUÍDA**.

- Vínculo seguro conta local ↔ identidade Google (Caso B) com verificação de senha local.
- `PendingLinkStore` em Redis em produção (TTL nativo, uso único, sobrevive a reinício).
- CSRF cookie-to-header efetivo em `/auth/link` (bug de registro corrigido e revalidado).
- E2E em produção validado (Redis, reinício, expiração, CSRF, senha incorreta).
- Suítes verdes: auth-service 279, backend 111, frontend 56 + typecheck/lint.

## Próxima sprint (roadmap)

- **8 — Empresas** (SaaS; dependência 7.5) — não iniciada nesta etapa, conforme planejamento.

---

*Nota de segurança:* a senha do Redis da VPS (`REDIS_PASSWORD`) não foi commitada nem
referenciada em código — utilizada apenas nos comandos de validação E2E.
