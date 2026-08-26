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

## FUTURE (decidir antes da Fase 2)

- `user_permissions` (override individual) + política ALLOW/DENY/precedência;
- expandir page:view para todos os módulos;
- field-level nos demais módulos e campos (cpf, birth_date quando existirem);
- UI hierárquica completa de permissões na tela de Perfis (PermissionMatrix evolui);
- cache das autoridades se volume justificar.
