# Playbook: Implementação do Módulo Users

## Objetivo
Implementar o gerenciamento completo de usuários do sistema: CRUD, atribuição de roles, ativação/desativação, e perfil do usuário logado.

## Pré-requisitos
- Módulo Auth implementado (autenticação e RBAC funcionando)
- Tabela users existente no banco de dados
- Sistema de permissões definido

## Documentos que DEVEM ser lidos
- `docs/Users.md`
- `docs/Auth.md`
- `docs/Permissions.md`
- `contexts/identity-context.md`

## Arquivos que poderão ser alterados
### Backend
- `packages/backend/src/domain/identity/` — Entidades: User, UserRole
- `packages/backend/src/application/identity/` — Casos de uso: CreateUserUseCase, UpdateUserUseCase, ListUsersUseCase, DeleteUserUseCase, AssignRoleUseCase
- `packages/backend/src/infrastructure/persistence/` — UserRepository (expandir)
- `packages/backend/src/presentation/rest/controller/UserController.ts`
- `packages/backend/src/presentation/rest/dto/` — UserDTOs

### Frontend
- `packages/frontend/src/components/users/` — UserList, UserForm, UserCard, RoleSelector
- `packages/frontend/src/hooks/useUsers.ts`
- `packages/frontend/src/app/(auth)/settings/users/` — Páginas de gestão de usuários

## Arquivos proibidos
- `packages/backend/src/infrastructure/security/` — Não alterar serviços de auth
- `packages/backend/src/presentation/rest/controller/AuthController.ts` — AuthController não deve ser alterado
- `packages/frontend/src/contexts/AuthContext.tsx` — AuthContext é do módulo Auth

## Ordem de implementação
1. Definir/expandir entidades de domínio User
2. Implementar casos de uso CRUD de usuários
3. Implementar repositório de usuários (operações de listagem, busca, paginação)
4. Implementar UserController com endpoints REST
5. Implementar DTOs de request/response
6. Implementar validação de dados (email único, etc.)
7. Criar componentes frontend: UserList com DataTable
8. Criar componente UserForm (criar/editar)
9. Criar hook useUsers com operações CRUD
10. Criar páginas: lista, detalhe, criação, edição
11. Integrar com sistema de permissões (quem pode gerenciar usuários)

## Checklist Backend
- [ ] Entidade User expandida com campos: id, email, name, phone, avatar, companyId, isActive, lastLoginAt
- [ ] CreateUserUseCase com validação de email único por empresa
- [ ] UpdateUserUseCase com permissão de edição (próprio perfil ou admin)
- [ ] ListUsersUseCase com paginação, filtros (name, email, role, status)
- [ ] DeleteUserUseCase (soft delete — marca is_active = false)
- [ ] AssignRoleUseCase para atribuir/remover roles
- [ ] UserController com endpoints: GET /users, GET /users/:id, POST /users, PUT /users/:id, DELETE /users/:id, POST /users/:id/roles
- [ ] Validação: email único dentro da empresa
- [ ] Validação: não permitir deletar a si mesmo
- [ ] Validação: só admin pode atribuir roles
- [ ] Paginação com cursor ou offset
- [ ] Filtros por status (ativo/inativo) e role

## Checklist Frontend
- [ ] UserList com tabela paginada (nome, email, roles, status, ações)
- [ ] UserForm para criar/editar usuário (name, email, phone, roles)
- [ ] RoleSelector para atribuir múltiplas roles
- [ ] Botão de ativar/desativar usuário
- [ ] Página de perfil do usuário logado (edição própria)
- [ ] Hook useUsers com: list, get, create, update, delete, assignRole
- [ ] States de loading e error tratados
- [ ] Confirmação antes de deletar/desativar
- [ ] Feedback visual (toast) para sucesso/erro

## Checklist Banco
- [ ] Índice único em users.email por empresa (se não existir)
- [ ] Tabela users com todos os campos necessários
- [ ] Tabela user_roles com FKs corretas
- [ ] Soft delete implementado (is_active flag)

## Checklist Testes
- [ ] Testes unitários: CreateUserUseCase (validações de negócio)
- [ ] Testes unitários: UpdateUserUseCase
- [ ] Testes de integração: CRUD completo de usuários
- [ ] Testes de integração: Atribuição de roles
- [ ] Testes de integração: Validação de email único
- [ ] Testes de integração: Permissão de acesso (admin vs comum)
- [ ] Testes E2E: Fluxo admin cria → edita → desativa usuário

## Checklist Documentação
- [ ] Atualizar `docs/Users.md` com endpoints e exemplos
- [ ] Documentar regras de negócio (quem pode criar/editar/deletar)
- [ ] Documentar formato dos DTOs

## Checklist Final
- [ ] CRUD de usuários funciona completamente
- [ ] Roles são atribuídas e refletem no acesso
- [ ] Usuário pode editar próprio perfil
- [ ] Admin pode gerenciar todos os usuários da empresa
- [ ] Soft delete funciona (usuário desaparece mas dados permanecem)
- [ ] Paginação e filtros funcionam
- [ ] Todos os testes passam
