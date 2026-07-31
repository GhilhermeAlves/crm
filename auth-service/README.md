# crm-auth-service

Camada de identidade da aplicação (Sprint 2 do auth-refactor). Traduz o JWT
oficial do Keycloak para o contexto interno do CRM (`CurrentUser`), resolvendo
usuário, empresa/tenant, roles e permissions. **Não emite JWT, não possui JWKS
próprio e não implementa refresh token** — o Keycloak é o único Identity
Provider / Authorization Server.

## Arquitetura

Clean Architecture + DDD + Hexagonal, no mesmo padrão do `crm-backend`:

```
com.becommerce.auth
├── application        # use cases (port.input) e portas de saída (port.output)
│   └── identity
├── domain             # modelo de identidade (CurrentUser, AuthenticatedIdentity,
│   └── identity         User read model, exceções)
├── infrastructure     # persistência (JPA leitura do schema CRM), security
│   ├── config           (resource server + JWKS do Keycloak), persistence
│   ├── persistence
│   └── security
└── presentation       # REST (API interna + healthcheck) e erros
    └── rest
```

## Responsabilidades (fronteira do serviço)

| Dentro | Fora (permanecem no Keycloak / crm-backend) |
|---|---|
| Resolução do `CurrentUser` | Emissão de JWT / refresh token |
| Resolução de usuário/empresa/tenant | Login, credenciais, SSO, MFA |
| Resolução de roles/permissions | JWKS próprio |
| Identidade derivada do contexto autenticado | Provisionamento de usuários (crm-backend, Sprint 1) |

O provisionamento **não é duplicado**: identidades sem usuário CRM retornam o
contrato `PROVISIONING_REQUIRED` (única fonte de verdade permanece no
`AuthService.provisionKeycloakUser` do backend; a migração é planejada para
sprint posterior).

## Endpoints

| Método | Caminho | Descrição | Autenticação |
|---|---|---|---|
| GET | `/auth/health` | Healthcheck | — |
| GET | `/actuator/health` | Healthcheck (actuator) | — |
| GET | `/internal/auth/current-user` | Resolve o `CurrentUser` da identidade autenticada | Bearer JWT do Keycloak |

### GET /internal/auth/current-user

Resposta `200` (usuário existente):

```json
{
  "status": "RESOLVED",
  "currentUser": {
    "userId": "974bbedb-298d-4ec6-a037-514b24c248e4",
    "email": "ghilherme007@gmail.com",
    "companyId": "11111111-2222-3333-4444-555555555555",
    "tenantId": "11111111-2222-3333-4444-555555555555",
    "roles": ["AGENT"],
    "permissions": ["contact:read"],
    "keycloakSub": "78490eac-150e-44db-b2c4-d7999c1c3801",
    "provider": "keycloak",
    "displayName": "Ghilherme Santos"
  }
}
```

Resposta `200` (usuário inexistente — contrato preparado para provisionamento):

```json
{
  "status": "PROVISIONING_REQUIRED",
  "identity": {
    "keycloakSub": "78490eac-150e-44db-b2c4-d7999c1c3801",
    "email": "ghilherme007@gmail.com",
    "displayName": "Ghilherme Santos"
  }
}
```

Resposta `401` (usuário desativado):

```json
{ "status": 401, "code": "USER_INACTIVE", "error": "Unauthorized", "message": "Usuário desativado: contate o administrador.", "timestamp": "..." }
```

## Segurança

- Resource server com JWKS do **Keycloak** (`spring.security.oauth2.resourceserver.jwt.*`).
- A identidade é **sempre** derivada do JWT (claims `sub`, `email`, `name`, `sid`).
  O endpoint interno não aceita `userId`/`companyId`/`roles`/`permissions` como entrada.
- Banco CRM lido apenas (schema de propriedade do crm-backend; `flyway.enabled=false`).

## Testes

```bash
./mvnw test
```

## Ambiente

Variáveis em `.env.example`. Porta `8080` (Compose: `8082:8080`).
