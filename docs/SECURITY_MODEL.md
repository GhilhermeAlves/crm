# MODELO DE SEGURANÇA E AUTORIZAÇÃO GRANULAR — Sprint 20 (Fase 1)

## Auditoria (estado atual comprovado)

- **Autoridades por requisição**: `KeycloakJwtAuthenticationConverter` monta a lista de
  authorities = roles Keycloak (`ROLE_*`) **+ permissões do banco CRM** (união dos perfis
  do usuário), consultadas a cada request via
  `AuthServiceCurrentUserResolver` → `CurrentUserResolutionService` (auth-service) ou
  fallback `LocalCurrentUserResolver`.
- **Consequência importante**: alterar `role_permissions` no banco tem efeito imediato,
  sem re-login.
- **Tabelas**: `roles` (por company), `permissions` (global, name único),
  `role_permissions` (único role+permission), `user_roles`, `memberships` (user↔company).
- **Enforcement atual**: `@PreAuthorize("hasAuthority('<recurso>:<acao>')")` nos controllers;
  `requireCompanyAccess` + `TenantContext` + RLS para tenant isolation.
- **Keycloak**: apenas autenticação/identidade (`sub`, email) + `ROLE_*`. Permissões finas
  vivem exclusivamente no banco CRM. Nada do Keycloak precisa mudar.

## Política definida (Fase 1)

1. **Perfil = Role existente** (`roles`). Não criamos entidade nova.
2. **Permissão = `permissions`** (name único global, ex.: `contact:field:email:update`).
3. **Herança**: usuário → perfis → permissões; efetiva = **união (additive ALLOW)**.
4. **Sem DENY e sem override por usuário nesta fase** — `user_permissions` individuais é
   FUTURE (exigiria política de precedência ALLOW/DENY; decidir antes da Fase 2).
5. **Novas granularidades**:
   - `<recurso>:page:view` — acesso à página (menu + rota); backend continua validando as
     ações com as permissões de ação já existentes.
   - `<recurso>:field:<campo>:update` — edição de campo específico, validada no backend.
6. **Naming**: mantém o padrão `<recurso>:<acao>` do CRM.

## Piloto (módulo Contatos)

- `contact:page:view` — gate do menu + página `/contacts`.
- `contact:field:email:update` / `contact:field:phone:update` — validados em
  `ContactService.update`: alteração de valor sem a permissão → `403`
  (`CrmAccessDeniedException`). Valor idêntico não exige permissão.
- `security:page:view` — controla o novo menu **Segurança** (Usuários = `/settings/users`,
  Perfis = `/settings/roles`, ambos reaproveitando telas/RBAC existentes).

## Matriz inicial (V064 + RoleSeedService)

| Permissão | ADMIN | MANAGER | AGENT | VIEWER |
|---|---|---|---|---|
| contact:page:view | ✓ | ✓ | ✓ | ✓ |
| contact:field:email:update | ✓ | ✓ | ✓ | ✗ |
| contact:field:phone:update | ✓ | ✓ | ✓ | ✗ |
| security:page:view | ✓ | ✗ | ✗ | ✗ |

## Fase 2 — Overrides individuais (implementada)

- **Tabela `user_permissions` (V065)**: `(company_id, user_id, permission_id, effect)`
  com `effect ∈ {ALLOW, DENY}` e UNIQUE(user_id, permission_id). INHERIT = ausência de
  linha. RLS FORCE + `tenant_isolation_policy`.
- **Permissão efetiva centralizada** (mesma SQL nos dois serviços):
  ```text
  efetiva = (permissões dos perfis ∪ ALLOW do usuário) − DENY do usuário
  ```
  - backend: `PermissionRepository.findEffectivePermissionNamesByUserIdAndCompanyId`
    (`SpringDataPermissionRepository`, usada por `LocalCurrentUserResolver`);
  - auth-service: `SpringDataUserRepository.findPermissionNamesByUserIdAndCompanyId`
    (usada por `CurrentUserResolutionService`).
  Efeito imediato: sem cache — a mesma característica da Fase 1 é preservada.
- **Precedência**: DENY (usuário) > ALLOW (perfil) > ausência. Sem papel + ALLOW = ALLOW;
  sem papel + DENY = negado.
- **API** (`UserController`, exige `permission:assign`):
  - `GET /api/v1/users/{id}/permissions` → roles, effective[], overrides[];
  - `PUT /api/v1/users/{id}/permissions/{permissionId}` `{ "effect": "ALLOW"|"DENY" }`;
  - `DELETE /api/v1/users/{id}/permissions/{permissionId}` → volta a INHERIT.
  Multi-tenant: alvo precisa pertencer à empresa ativa + TenantContext + RLS.
- **page:view expandido (V066)** para os módulos do menu: dashboard, leads, pipeline,
  tasks, activities, workflows, notifications, campaigns, omnichannel, audit (+ contacts
  e security já da Fase 1). Menu usa as novas permissões; as APIs continuam protegidas
  pelas permissões de ação — proteção real permanece no backend.
- **UI**: `/settings/users` ganhou diálogo "Permissões" por membro (perfis, efetivas e
  overrides com botões Permitir/Negar); PermissionMatrix agrupa permissões por módulo.

## FUTURE (decidir antes da Fase 3)

- expandir field-level para novos campos/módulos (cpf, birth_date quando existirem);
- UI hierárquica completa de permissões na tela de Perfis (subgrupos Página/Campos);
- cache das autoridades se volume justificar;
- auditoria detalhada de cada alteração de override (hoje via TenantAuditRecorder).
