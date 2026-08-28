# PROVISIONING — Provisionamento de Usuários e Sincronização Keycloak ↔ CRM

## Objetivo

Definir como o `crm-auth-service` provisiona usuários automaticamente no primeiro login e como os dados são sincronizados entre o Keycloak (fonte de identidade e autorização de tokens) e o banco CRM (fonte de autorização de negócio e perfil).

## Índice

- [1. Direção da Sincronização](#1-direção-da-sincronização)
- [2. Mapeamento de Atributos](#2-mapeamento-de-atributos)
- [3. Regras de Vinculação](#3-regras-de-vinculação)
- [4. Fluxo de Provisionamento](#4-fluxo-de-provisionamento)
- [5. Políticas de Empresa e Role Default](#5-políticas-de-empresa-e-role-default)
- [6. Atualizações de Perfil](#6-atualizações-de-perfil)
- [7. Reconciliação Periódica](#7-reconciliação-periódica)
- [8. Desprovisionamento](#8-desprovisionamento)
- [9. Idempotência e Consistência](#9-idempotência-e-consistência)
- [Referências](#referências)
- [Histórico de Revisão](#histórico-de-revisão)

---

## 1. Direção da Sincronização

| Fonte | Dados | Direção |
|---|---|---|
| **Keycloak** | Identidade: `sub`, `email`, `email_verified`, `preferred_username`, `name`, `given_name`, `family_name` | Keycloak → CRM (espelho na entrada) |
| **CRM DB** | Autorização: roles, permissions, `companyId`/tenant | CRM → `CurrentUser` (fonte de verdade de RBAC) |
| **CRM DB** | Perfil CRM: avatar, timezone, idioma, cargo, telefone | CRM → app (sem sincronizar de volta ao Keycloak) |

Princípios:

- **Keycloak é a fonte de verdade de identidade e o único emissor de JWT** (credenciais, e-mail, verificação de e-mail).
- **CRM é a fonte de verdade de autorização de negócio** (roles e permissões residem no banco CRM).
- A sincronização automática é **unidirecional (Keycloak → CRM)** para dados de identidade. Alterações de RBAC são feitas no CRM e resolvidas pelo auth-service no `CurrentUser`; opcionalmente, mapeadas para claims do JWT via *role mapper* no Keycloak (ver AUTHORIZATION.md).
- **Nenhum token é emitido pelo auth-service** durante a autenticação; o JWT oficial é sempre do Keycloak.

---

## 2. Mapeamento de Atributos

### Claims do Keycloak → Campos do usuário CRM

| Claim (id/access token) | Campo no banco CRM | Comportamento |
|---|---|---|
| `sub` | `users.keycloak_sub` | Chave de vínculo principal; gravada no primeiro login |
| `email` | `users.email` | Chave de vínculo secundária; atualizado se verificado |
| `email_verified` | — | Gate para usar e-mail como vínculo |
| `preferred_username` | `users.email` (fallback) | Usado quando `email` ausente |
| `given_name` / `family_name` / `name` | `users.first_name` / `last_name` / `name` | Atualizados se vazios ou alterados |
| `realm_access.roles` | mapeamento → roles CRM (ver AUTHORIZATION.md) | Apenas mapeamento opcional de bootstrapping |

### Exemplo de claims considerados

```json
{
  "sub": "78490eac-150e-44db-b2c4-d7999c1c3801",
  "email": "ghilherme007@gmail.com",
  "email_verified": true,
  "preferred_username": "ghilherme007@gmail.com",
  "given_name": "Ghilherme",
  "family_name": "Pereira",
  "realm_access": { "roles": ["ADMIN"] }
}
```

---

## 3. Regras de Vinculação

Resolução de usuário, em ordem:

1. **Por `keycloak_sub`** (`users.keycloak_sub = sub`): vínculo forte — usuário já autenticou antes.
2. **Por e-mail verificado** (`users.email = email` e `email_verified = true`): vínculo por e-mail para usuários pré-existentes.
3. **Não encontrado**: provisiona novo usuário (seção 4).

### Conflitos

| Cenário | Política |
|---|---|
| E-mail existe, `keycloak_sub` diferente | Vincula o `sub` ao usuário existente (same-email claim) |
| E-mail existe e pertence a outra empresa | Rejeita com erro 409 + fluxo de suporte/convite |
| Múltiplos usuários com o mesmo e-mail | Usa o primeiro ativo; registra evento de ambiguidade |
| `email` ausente e `preferred_username` também | Falha com 401 + instrução de atualizar o perfil no Keycloak |

---

## 4. Fluxo de Provisionamento

```mermaid
flowchart TD
    A[JWT do Keycloak recebido (login/refresh)] --> B[Extrair claims]
    B --> C{findByKeycloakSub?}
    C -->|Sim| D[Atualizar dados de identidade se alterados]
    C -->|Não| E{findByEmail verificado?}
    E -->|Sim| F[Vincular keycloak_sub]
    E -->|Não| G[Criar usuário CRM]
    F --> D
    G --> H[Definir empresa/tenant + role default]
    H --> I[Persistir usuário + vínculo]
    D --> J[Resolver RBAC do banco CRM]
    I --> J
    J --> K[Montar CurrentUser - sem emitir token]
    K --> L[Publicar auth.user.authenticated / auth.user.provisioned]
```

### Passos do provisionamento de um usuário novo

1. Cria `users` com `status = ACTIVE` (ou `PENDING` se exigir confirmação/convite).
2. Define `companyId`/tenant conforme política (seção 5).
3. Atribui a **role default** (ex.: `AGENT`) — veja seção 5.
4. Persiste `keycloak_sub`, `email`, nome, `last_login_at`.
5. Publica `auth.user.provisioned` (EVENTS.md).

---

## 4.1 Implementação Sprint 1 (estabilização no crm-backend)

O Sprint 1 (MIGRATION_PLAN.md §4) implementa o fluxo acima **no caminho atual do `crm-backend`**, sem criar o `crm-auth-service`. Detalhes desta etapa de estabilização:

- **Onde**: `KeycloakJwtAuthenticationConverter` → `AuthService.provisionKeycloakUser` → `CrmPrincipal` (a migração desta lógica para o `crm-auth-service` está planejada para um sprint futuro — **não** faz parte do Sprint 2; ver §4.2).
- **Resolução em ordem**: `keycloak_sub` → `email` → criação. Um usuário pré-existente encontrado por e-mail é **vinculado** ao `sub` (nunca duplicado); `given_name`/`family_name`/`name` são sincronizados apenas quando vazios.
- **Empresa/role default**: `app.auth.provisioning.default-company-id` (`AUTH_DEFAULT_COMPANY_ID`) e `app.auth.provisioning.default-role` (`AUTH_DEFAULT_ROLE`, default `AGENT`). **Sem empresa configurada, o provisionamento NÃO escolhe tenant arbitrário**: o cadastro self-service (Sprint 8.3) cria o usuário com `company_id NULL` (sem empresa/membership/role) e o direciona ao onboarding para criar a própria empresa; o login via OIDC (`createProvisionedUser`) faz o mesmo. Quando `AUTH_DEFAULT_COMPANY_ID` está configurada, o provisioning ainda provisiona na empresa padrão (fallback opcional, D4). A role default é buscada com `Role.SYSTEM_COMPANY_ID` (roles de sistema).
- **Flag de rollback**: `app.auth.provisioning.enabled` (`AUTH_PROVISIONING_ENABLED`, default `true`). Desligada, usuários existentes seguem autenticando; usuários desconhecidos recebem **401** com mensagem clara (sem o 500 antigo).
- **Concorrência**: dois primeiros logins simultâneos do mesmo usuário resultam em um único registro — a criação ocorre em transação `REQUIRES_NEW` isolada; em corrida de `UNIQUE (email)`, o perdedor reabre o vencedor e o vincula ao `sub`.
- **Falhas previsíveis**: token sem `sub`/e-mail válido ou sem empresa ativa lançam `UserProvisioningException` → **401** no resource server (via `AuthenticationServiceException` na cadeia de segurança) ou pelo `GlobalExceptionHandler`; nunca mais o `InvalidDataAccessApiUsageException` com `userId = null`.
- **Usuário desativado**: usuário encontrado (por `sub` ou e-mail) com status `INACTIVE`/`LOCKED` (`!user.isActive()`) é rejeitado no provisionamento (`rejectIfInactive` → `UserProvisioningException` → **401**), mesmo com JWT válido. Novo em 1.2.0 (antes a checagem só existia no login legado).
- **Diferenças vs. arquitetura alvo**: o vínculo por e-mail não exige `email_verified`; `users.keycloak_sub` não tem `UNIQUE` (V009 — exclusividade garantida apenas por e-mail).

---

## 4.2 Fundação Sprint 2 — contrato `PROVISIONING_REQUIRED` no crm-auth-service

O Sprint 2 criou a **fundação** do `crm-auth-service` **sem migrar o provisionamento**. A única fonte de verdade do provisionamento continua sendo o `crm-backend` (`AuthService.provisionKeycloakUser` — §4.1). O que a fundação estabelece:

- O `CurrentUserResolutionService` do auth-service resolve na ordem `keycloak_sub` → e-mail verificado → **sem criação**: identidade autenticada sem usuário CRM retorna `CurrentUserResolution.ProvisioningRequired`, exposto na API como **`PROVISIONING_REQUIRED`** (200, discriminado) — um contrato pronto para o provisionamento que migrará para o auth-service em sprint futuro.
- Usuário CRM encontrado com status `INACTIVE`/`LOCKED` → rejeitado na resolução (`UserInactiveException` → **401 `USER_INACTIVE`**), espelhando a política do §4.1.
- Nenhuma lógica de criação/vínculo/empresa-default/role-default foi duplicada no auth-service; a migração acontece quando o fluxo for absorvido (removendo o provisionamento do backend) — evitando dois caminhos de provisionamento concorrentes.
- **Relação com a V009**: enquanto a migração não ocorre, `users.keycloak_sub` permanece sem `UNIQUE`; o vínculo único do `sub` continua garantido pela regra de negócio do backend. A migração do provisionamento deverá incluir a evolução do schema (índice único) e o tratamento de corrida, preservando a idempotência do §9.

---

## 5. Políticas de Empresa e Role Default

| Contexto | Política |
|---|---|
| Usuário com `company_id` no claim / client role | Usa a empresa indicada |
| Convite pendente (`users.invite_token` + e-mail) | Vincula o `sub`, ativa via `acceptInvite` |
| Primeiro usuário da plataforma | Provisiona com a empresa default + `SUPER_ADMIN` (bootstrap) |
| Novo usuário sem convite (cadastro self-service, Sprint 8.3) | Criado **sem empresa** (`company_id NULL`), sem membership/role; direcionado ao **onboarding** para criar a própria empresa (Company → Membership OWNER → role ADMIN → `crm_enabled`) |

A **role default** e a **empresa default** devem ser configuráveis por tenant (variável de ambiente ou tabela de configuração). O bootstrap de `SUPER_ADMIN` é documentado em AUTHORIZATION.md.

---

## 6. Atualizações de Perfil

A cada autenticação/refresh, o auth-service **espelha** os atributos de identidade alterados no Keycloak:

| Campo | Regra |
|---|---|
| `email` | Atualizado somente se `email_verified = true` |
| `given_name`/`family_name`/`name` | Atualizados se alterados e não sobrescritos por edição manual no CRM |
| `avatar_url` | Não sincronizado (perfil CRM é fonte) |
| `language`, `timezone` | Não sincronizados (perfil CRM) |

Alterações de **RBAC** nunca vêm do Keycloak: são feitas no CRM (`roles`/`permissions`) e refletidas no próximo `CurrentUser` resolvido (ver seção 7 para atraso de propagação).

---

## 7. Reconciliação Periódica

Sincronização de identidade é feita na entrada (login/refresh). Um job de **reconciliação opcional** (futuro, Sprint 6) compara:

- usuários ativos no Keycloak × usuários CRM com `keycloak_sub`;
- usuários CRM com `keycloak_sub` cujo usuário foi desativado no Keycloak.

O job é **não-destrutivo**: apenas marca `status`/flag, nunca apaga registros.

---

## 8. Desprovisionamento

| Ação | Keycloak | CRM |
|---|---|---|
| Desativar usuário | Desativar no Keycloak (IdP) | `users.status = INACTIVE` (soft) |
| Remover acesso | — | Invalidar cache de `CurrentUser` + evento `auth.session.revoked` |
| Exclusão definitiva | — | Soft delete (padrão atual do CRM) |

O auth-service responde com **erro/401** ao resolver o `CurrentUser` de usuários desativados, mesmo que o JWT ainda seja válido (checagem de status na resolução, com cache invalidado).

---

## 9. Idempotência e Consistência

- Provisionamento é **idempotente**: múltiplos logins simultâneos do mesmo usuário resultam em um único registro (constraint única em `email`; `keycloak_sub` sem `UNIQUE` na V009 — corrida tratada com transação `REQUIRES_NEW` + reabertura do vencedor, ver §4.1).
- O vínculo `keycloak_sub` é único **por aplicação/regra de negócio** (primeiro `sub` vence; a exclusividade no banco via índice único é prevista para a arquitetura alvo no Sprint 2).
- A resolução do `CurrentUser` depende do provisionamento concluído; se a resolução de RBAC falhar, o acesso falha (fail-closed) com retry no próximo login.
- Sincronização é eventual: alterações de RBAC refletem no próximo `CurrentUser` resolvido (cache com TTL curto ou invalidação por evento).

## Referências

| Documento | Relação |
|---|---|
| [AUTH_FLOWS.md](./AUTH_FLOWS.md) | Fluxo do primeiro login |
| [CURRENT_USER.md](./CURRENT_USER.md) | Modelo de dados propagado |
| [AUTHORIZATION.md](./AUTHORIZATION.md) | Roles/permissões e bootstrap |
| [EVENTS.md](./EVENTS.md) | Eventos de provisionamento |
| [MIGRATION_PLAN.md](./MIGRATION_PLAN.md) | Sprints que implementam o provisionamento |
| [04-integrations/KEYCLOAK_INTEGRATION.md](../04-integrations/KEYCLOAK_INTEGRATION.md) | Estado atual (vinculação condicional) |

## Histórico de Revisão

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-31 | Architect | Sprint 0 — provisionamento automático e sincronização Keycloak ↔ CRM |
| 1.1.0 | 2026-07-31 | Architect | Ajuste: auth-service não emite token; RBAC resolvido no CurrentUser a partir do JWT do Keycloak |
| 1.2.0 | 2026-07-31 | Architect | Sprint 1 — implementação da estabilização no crm-backend (provisionamento, flag de rollback, concorrência REQUIRES_NEW, falhas 401, rejeição de usuário desativado); ajustes em §9 |
| 1.3.0 | 2026-07-31 | Architect | Sprint 2 — fundação do auth-service com contrato `PROVISIONING_REQUIRED` (sem migrar o provisionamento, que permanece no backend); novo §4.2 |
