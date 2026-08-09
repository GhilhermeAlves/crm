# Incidente — Login "não acessa" / INVALID_STATE (após Sprint 8.2)

**Data:** 2026-08-09 - **Ambiente:** VPS (produção `srv1348261.hstgr.cloud`) - **Status:** ✅ Resolvido

## Contexto

Após o deploy da Sprint 8.2 (Membership) o usuário reportou não conseguir acessar o CRM. A
investigação mostrou **duas causas independentes**, ambas corrigidas.

## Causa 1 — Conta `ghilherme007@gmail.com` sem vínculo Google

### Diagnóstico

- No Keycloak (realm CRM) apenas `paulo.alves@praiaclube.org.br` possui vínculo federado
  (`federated_identity` → `google`).
- `ghilherme007@gmail.com` existe como usuário Keycloak **com senha local** (hash), mas **sem**
  vínculo Google — o único caminho de login é e-mail+senha no formulário do Keycloak.
- O valor da senha era desconhecido → `Invalid username or password`.

### Correção

- Reset da senha da conta via Keycloak Admin REST API (service account `crm-keycloak-admin`).
- Verificação positiva: token de acesso emitido via password grant (teste real; `directAccessGrantsEnabled`
  habilitado temporariamente no client `crm-frontend` e **revertido** logo em seguida).
- Observação: conta **não usa** o botão "Entrar com Google".

## Causa 2 — `INVALID_STATE` no `/auth/callback`

Dois eventos distintos, ambos do mesmo mecanismo: o `state` do fluxo OIDC não estava mais no store.

| Horário (UTC) | Evento | Causa |
|---|---|---|
| 18:43:48 | callback com state emitido **antes** do restart do auth-service (16:21) | `OidcAuthorizationRequestStore` é **em memória** (`ConcurrentHashMap`); o restart do deploy apagou o state pendente |
| 19:02:36 | authorize às **18:52:19**, callback às **19:02:36** (10min17s) | `authorizationRequestTtl` = **10 min** → state expirou |

### Correção

- **TTL do state aumentado para 30 min:**
  - Código: `OidcGatewayProperties.authorizationRequestTtl` default `10m → 30m`.
  - Produção: `AUTH_GATEWAY_AUTHORIZATION_REQUEST_TTL=30m` no compose do auth-service
    (`docker/docker-compose.yml`, sincronizado com `/opt/crm/docker`); container recriado e **healthy**.
- **Débito registrado:** o store de `state` continua **em memória** — um restart/deploy do
  auth-service invalida logins em andamento. Migração para Redis (paridade com o session store)
  fica como follow-up.

## Verificação final

- Login completo do usuário (`ghilherme007@gmail.com` + nova senha) realizado com sucesso na UI.
- `crm-auth-service` healthy; fluxo OIDC com `state` validado dentro do novo TTL.
- Nenhuma credencial de infraestrutura (Postgres/Redis/Keycloak admin/secrets) foi alterada.

## Arquivos alterados

- `auth-service/src/main/java/com/becommerce/auth/infrastructure/gateway/OidcGatewayProperties.java`
- `docker/docker-compose.yml`
- `sprints/8.2/INCIDENT_LOGIN_ACCESS.md` (este relatório)

## Follow-ups

1. Migrar `OidcAuthorizationRequestStore` para Redis (sobreviver a restarts).
2. Definir senha definitiva da conta `ghilherme007@gmail.com` (a atual é provisória e pode ser
   trocada em "Forgot Password").
