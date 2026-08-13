# Sprint 9 — User & Permission Management

**Data:** 2026-08-12 · **Status:** ✅ Concluída · **Responsável:** AI Agent · **Dependência:** 8.6

## Resumo

Sprint de **gestão de usuários e permissões por papel** dentro do modelo SaaS já
estabelecido (USUÁRIO → MEMBERSHIP → ROLE/PERFIL → PERMISSIONS), com autorização **real**
no backend (403), permissões efetivas expostas ao frontend via `/auth/me` e re-derivadas
a cada Company Switcher, telas de administração (`/settings/users`, `/settings/roles`),
proteção de UI (menus/botões/páginas) e conclusão do CRUD de contatos. Não criou
estruturas duplicadas — reutilizou o RBAC, `CurrentUser`, RLS e o Company Switcher
existentes.

## Contexto de permissões (reúso)

- Papel efetivo = papel do **membership da empresa ativa**; comportamento e permissões
  mudam ao alternar a empresa (Company Switcher, 8.4) sem relogar.
- Permissões no formato **`resource:action`** já estabelecido (`contact:create`,
  `user:read`, `role:read`, `membership:view/manage`) — mantidas, apenas consolidadas.
- `CurrentUser` (record) já carregava `roles`/`permissions`/`membershipRole` por requisição
  via `LocalCurrentUserResolver`; `KeycloakJwtAuthenticationConverter` mapeia cada
  permission → authority (`hasAuthority('contact:read')`); `@EnableMethodSecurity`.
- Roles canônicas preservadas: SUPER_ADMIN, ADMIN, MANAGER, AGENT, VIEWER; OWNER exclusivo
  do onboarding. RBAC backend e frontend completos nas iterações anteriores.

## Backend — Contact CRUD completo

- Antes a feature `Contact` tinha apenas **create/read**. Completada com **update/delete**:
  - `UpdateContactRequest` (novo DTO);
  - `ContactUseCase.update/delete`; `ContactService.update/delete`, `requireOwnedActive`
    (valida pertencimento à empresa ativa + `isActive()`) e auditoria
    (`AuditAction.UPDATE/DELETE`, `AuditModule.CONTACTS`);
  - `Contact` ganhou `touch()`, `delete()`, `isActive()` e setters;
  - `ContactController` `PUT /{contactId}` (`@PreAuthorize('contact:update')`) e
    `DELETE /{contactId}` (`@PreAuthorize('contact:delete')`) + `requireCompanyAccess`.

## Backend — `/auth/me` com permissões

- `/api/v1/auth/me` passa a retornar `roles`, `membershipRole` e `permissions` da empresa
  ativa (antes expunha só o perfil base sem permissões):
  - `UserResponse` ganhou 3 campos (`roles`, `membershipRole`, `permissions`) com
    `List.of()` default e fábrica `withCurrentUser(...)`;
  - `AuthController.me` combina `UserResponse` + `principal.roles()/membershipRole()/
    permissions()`;
  - `UserService.mapToResponse` atualizado (listas vazias default).

## Backend — autorização real (403)

- Guardas por permission em todo o caminho de gestão de usuários/papéis (base já existente
  `user:read/role:read/membership:view/membership:manage`, `settings:view/update`,
  `contact:update/delete`).
- Isolamento multi-empresa provado por teste: usuário ADMIN na empresa A com
  `user:create`+`user:read` vira VIEWER na empresa B apenas com `user:read` — sem vazar
  roles/permissões entre empresas (`LocalCurrentUserResolverTest` multi-company).

## Frontend — permissões na identidade

- `User` (auth.types) ganhou `roles`, `membershipRole`, `permissions` (opcionais).
- `useAuth()` deixa de expor `roles`/`permissions` vazios e passa a derivar do
  `/auth/me` (contexto `roles: user?.roles ?? []`, `permissions: user?.permissions ?? []`).
- Novo hook **`useAuthorization`** (+ `usePermission`): `can(permission)`,
  `cannot(permission)`, `hasRole(role)`, `isSuperAdmin`, `permissions`, `roles`. Sem
  permissões carregadas → libera por conveniência (UX evita "blink" de menus); o backend
  continua sendo a autoridade final.

## Frontend — telas de administração

- **`/settings/users`**: lista membros da empresa ativa (nome, e-mail, papel, status,
  entrada), alterar papel e remover membro (reusa `useMembers`/`useUpdateMemberRole`/
  `useRemoveMember` + `InviteMemberDialog`); ações gated por `membership:manage`.
- **`/settings/roles`**: lista papéis (ADMIN/MANAGER/AGENT/VIEWER/...) e mostra as
  permissões de cada um via `PermissionMatrix` (reusa RBAC); edição/toggle de permissões
  gated por `role:manage`. Não cria roles duplicadas.
- Sidebar: para a rota não ser a única entrada, `ROUTES.SETTINGS_USERS`/`SETTINGS_ROLES`
  adicionados com gate por permission (`membership:view`, `role:read`).

## Frontend — proteção de UI + Company Switcher

- `Sidebar` já filtra itens por permission (`hasPermission`); os novos itens de settings
  seguem o mesmo padrão.
- **Company Switcher (8.4)**: `useSwitchCompany` já invalida `["me"]`, `["roles"]`,
  `["permissions"]`, `["users"]` etc. — logo a troca de empresa re-deriva o CurrentUser, o
  `/auth/me` e, por consequência, `roles`/`permissions`/`membershipRole` e as telas de
  administração (query `["members", companyId]` muda de chave por empresa).

## Testes

- **Backend: 215 testes PASS** (antes 210; novos `ContactControllerTest` update/delete —
  própria empresa OK e cross-company 403 `CRM_ACCESS_DENIED` — e
  `LocalCurrentUserResolverTest` multi-company sem vazamento). Compile OK.
- **Frontend: 74 testes PASS** (antes 69; `useAuth.test.ts` atualizado para refletir
  permissões vindas de `/auth/me` + novo `useAuthorization.test.ts`). Typecheck OK,
  lint sem erros novos (warnings pré-existentes mantidos).

## Migração

- Nenhuma migração nova necessária nesta sprint (estruturas de permissoes e de usuários já
  existem das sprints 8.x). Alterações são de camada de aplicação/frontend.

## Documentação

- `sprints/9/REPORT.md` (este arquivo).
- `sprints/SPRINT_INDEX.md` atualizado (Sprint 9 ✅ Concluída, resumo e "última atualização").

## Pendências / débitos técnicos

- **E2E autenticado manual** (herdado 8.x): fluxo real de gestão de usuários/papéis e troca
  de empresa no browser — sem credenciais de teste automatizáveis.
- **Envio de e-mail real** (herdado): `ConsoleEmailSender` placeholder.
- **`InvitationRateLimiter` em memória** (herdado): Redis/DB em multi-instância.
- **Auditoria geral** de eventos não-tenant permanece fora de escopo (coberto apenas o que
  a sprint precisou).

## Artefatos

- Backend: `presentation/rest/contact/ContactController.java`,
  `application/contact/service/ContactService.java`, `application/contact/port/input/ContactUseCase.java`,
  `application/contact/dto/UpdateContactRequest.java`, `domain/contact/Contact.java`,
  `application/identity/dto/UserResponse.java`, `presentation/rest/identity/AuthController.java`,
  `application/identity/service/UserService.java`
- Testes backend: `ContactControllerTest.java`, `LocalCurrentUserResolverTest.java`
- Frontend: `features/auth/types/auth.types.ts`, `features/auth/hooks/{useAuth.tsx,useAuthMutations.ts}`,
  `features/auth/hooks/useAuthorization.ts`, `features/auth/hooks/useAuthorization.test.ts`,
  `features/auth/hooks/useAuth.test.ts`, `app/(dashboard)/settings/users/page.tsx`,
  `app/(dashboard)/settings/roles/page.tsx`, `components/layout/Sidebar.tsx`, `lib/constants.ts`