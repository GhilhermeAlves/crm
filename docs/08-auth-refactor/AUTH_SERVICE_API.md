# AUTH_SERVICE_API — APIs do crm-auth-service

## Objetivo

Definir as APIs públicas (consumidas pelo frontend/gateway) e internas (consumidas pelos serviços) do `crm-auth-service`, incluindo contratos de entrada/saída. O auth-service **não expõe fluxo de login/refresh/logout próprio nem JWKS** — a autenticação e a emissão de JWT são de responsabilidade exclusiva do Keycloak.

## Índice

- [1. Visão Geral](#1-visão-geral)
- [2. Endpoints Públicos](#2-endpoints-públicos)
- [3. Endpoints Internos](#3-endpoints-internos)
- [4. Endpoints de Infraestrutura](#4-endpoints-de-infraestrutura)
- [5. Contratos (Exemplos)](#5-contratos-exemplos)
- [6. Autenticação entre Serviços](#6-autenticação-entre-serviços)
- [7. Migração dos Endpoints Atuais](#7-migração-dos-endpoints-atuais)
- [Referências](#referências)
- [Histórico de Revisão](#histórico-de-revisão)

---

## 1. Visão Geral

O `crm-auth-service` expõe:

- **Públicos** (`/auth/...`): consumidos pelo frontend e pelo gateway (perfil/`CurrentUser`).
- **Internos** (`/internal/...`): consumidos pelo gateway e serviços de negócio (resolução de `CurrentUser`, revogação de sessão) — acessíveis apenas na rede interna (network docker) e/ou mTLS.

**Login, refresh, logout e emissão de JWT são feitos diretamente com o Keycloak** (ver AUTH_FLOWS.md). Toda resposta de erro segue o padrão atual (`GlobalExceptionHandler`): `{ status, error, message, timestamp }`.

---

## 2. Endpoints Públicos

| Método | Caminho | Descrição | Autenticação |
|---|---|---|---|
| GET | `/auth/me` | Retorna o `CurrentUser` autenticado (resolve provisionamento/RBAC sob demanda) | Bearer JWT do Keycloak |
| POST | `/auth/current-user` | Resolve/retorna o `CurrentUser` a partir do JWT (usado pelo gateway para enriquecer a requisição) | Bearer JWT do Keycloak |
| GET | `/auth/health` | Healthcheck | — |

> O frontend inicia o login diretamente no Keycloak (OIDC + PKCE) — não há `/auth/authorize` nem `/auth/callback` no auth-service. `/auth/me` e `/auth/current-user` apenas materializam o `CurrentUser`; **não** emitem tokens.

### Resolução do CurrentUser (via gateway)

```mermaid
sequenceDiagram
    participant FE as Frontend
    participant GW as API Gateway
    participant AS as crm-auth-service
    participant KC as Keycloak

    FE->>GW: GET /api/... (Bearer JWT do Keycloak)
    GW->>AS: POST /auth/current-user (Bearer JWT do Keycloak)
    AS->>KC: valida assinatura/issuer (JWKS)
    AS-->>GW: CurrentUser (userId, companyId, roles, permissions)
    GW-->>FE: 200 + CurrentUser propagado aos serviços
```

---

## 3. Endpoints Internos

| Método | Caminho | Descrição | Autenticação |
|---|---|---|---|
| POST | `/internal/sessions/revoke` | Revoga sessões/cache de um usuário (usado por outros serviços em desativação) | mTLS / service token |
| GET | `/internal/users/{userId}/access` | Retorna roles/permissions atuais de um usuário (re-resolução sob demanda) | mTLS / service token |
| GET | `/internal/users/by-keycloak-sub/{sub}` | Resolve usuário interno CRM pelo `sub` do Keycloak | mTLS / service token |

> **Nota:** não existe `/internal/token/introspect` no auth-service — a validação do JWT é feita pelos próprios serviços via JWKS do Keycloak (stateless). O auth-service não é introspeção nem emissor.

---

## 4. Endpoints de Infraestrutura

| Método | Caminho | Descrição |
|---|---|---|
| GET | `/actuator/health` | Healthcheck liveness/readiness |
| GET | `/actuator/info` | Informações da build |
| GET | `/actuator/metrics` | Métricas Prometheus |
| GET | `/docs` | OpenAPI/Swagger (restringido em produção) |

---

## 5. Contratos (Exemplos)

### `POST /auth/current-user`

Request:
```json
{
  "path": "/api/v1/contacts",
  "method": "GET"
}
```

Response `200`:
```json
{
  "sub": "5f0e1c2a-3b4d-4e5f-8a9b-0c1d2e3f4a5b",
  "keycloakSub": "78490eac-150e-44db-b2c4-d7999c1c3801",
  "email": "ghilherme007@gmail.com",
  "companyId": "c0ffee00-0000-0000-0000-000000000001",
  "tenantId": "c0ffee00-0000-0000-0000-000000000001",
  "roles": ["ADMIN"],
  "permissions": ["user:read", "user:write", "dashboard:view"],
  "status": "ACTIVE"
}
```

### `GET /auth/me`

Response `200` (shape compatível com `UserResponse` atual):
```json
{
  "id": "5f0e1c2a-3b4d-4e5f-8a9b-0c1d2e3f4a5b",
  "email": "ghilherme007@gmail.com",
  "firstName": "Ghilherme",
  "lastName": "Pereira",
  "companyId": "c0ffee00-0000-0000-0000-000000000001",
  "roles": ["ADMIN"],
  "permissions": ["user:read", "user:write", "dashboard:view"],
  "keycloakSub": "78490eac-150e-44db-b2c4-d7999c1c3801",
  "status": "ACTIVE"
}
```

---

## 6. Autenticação entre Serviços

| Caminho | Mecanismo |
|---|---|
| Services → Keycloak (JWKS) | Validação stateless do JWT (sem chamada extra) |
| Gateway → `/auth/current-user`, `/auth/me` | Bearer JWT do Keycloak (validado) |
| Services → `/internal/*` | mTLS ou service-to-service token (escopo `auth:internal`) |
| Frontend → Keycloak | OIDC + PKCE (client público), sem segredo no browser |
| Keycloak → auth-service | N/A (auth-service não troca código; apenas valida JWT e resolve) |

---

## 7. Migração dos Endpoints Atuais

| Endpoint atual (crm-backend) | Destino na nova arquitetura |
|---|---|
| `POST /auth/login` (email/senha) | Removido (autenticação via Keycloak) |
| `POST /auth/refresh` | Removido (renovação via Keycloak/SSO no frontend) |
| `POST /auth/logout` | Removido (logout via `end_session_endpoint` do Keycloak) |
| `GET /auth/me` | Movido para `crm-auth-service /auth/me` |
| `POST /auth/keycloak/callback` | Substituído pelo fluxo OIDC + PKCE direto com o Keycloak |
| `POST /auth/register`, `forgot-password`, `reset-password`, `change-password` | Avaliados: senhas não são mais geridas pelo CRM (Keycloak é IdP); decisão documentada no MIGRATION_PLAN.md |
| `POST /auth/register` (conta local) | Não recomendado manter; usuários devem ser provisionados via Keycloak |
| `GET/PUT /users/profile`, `/roles/**`, `/permissions/**` | Permanecem nos serviços de negócio (CRUD de usuários/roles/permissões é administração, não autenticação) |

## Referências

| Documento | Relação |
|---|---|
| [AUTH_FLOWS.md](./AUTH_FLOWS.md) | Fluxos de autenticação (Keycloak) |
| [CURRENT_USER.md](./CURRENT_USER.md) | Shape do `CurrentUser` |
| [AUTHORIZATION.md](./AUTHORIZATION.md) | Endpoints de roles/permissions mantidos |
| [API_MAP.md](../API_MAP.md) | Mapa de APIs atual |
| [ERROR_HANDLING.md](../ERROR_HANDLING.md) | Padrão de erro |

## Histórico de Revisão

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-31 | Architect | Sprint 0 — APIs públicas e internas do auth-service |
| 1.1.0 | 2026-07-31 | Architect | Ajuste: sem login/refresh/logout/JWKS no auth-service; autenticação exclusiva via Keycloak |
