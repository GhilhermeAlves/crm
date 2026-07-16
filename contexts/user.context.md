# User Context

## Resumo do Módulo
Gerenciamento completo do ciclo de vida de usuários com sistema de convite (7 dias expiração), roles por empresa e status de conta.

## Objetivo
Gerenciar cadastro, convites, perfis e status dos usuários dentro de cada empresa.

## Responsabilidades
- CRUD de usuários com dados pessoais
- Sistema de convite com expiração de 7 dias
- Atribuição de roles por empresa
- Gestão de status: active, inactive, blocked
- Perfil do usuário logado (self-service)

## Entidades Relacionadas
- User, UserRole, Company, Role

## APIs Relacionadas
- `GET /users` - Listar usuários da empresa
- `POST /users` - Criar usuário
- `POST /users/invite` - Enviar convite
- `GET /users/:id` - Detalhes do usuário
- `PUT /users/:id` - Atualizar usuário
- `DELETE /users/:id` - Remover usuário
- `GET /users/me` - Perfil do logado
- `PUT /users/me` - Atualizar perfil

## Banco Relacionado
- `users` - Dados pessoais, email, senha, status
- `user_roles` - Roles por empresa (N:N)

## Status
- **active** - Conta ativa e operacional
- **inactive** - Desativado pelo admin
- **blocked** - Bloqueado por segurança

## Componentes Frontend
- UsersList, UserForm, UserInviteModal
- UserStatusBadge, RoleSelect
- Profile page (self-service)

## Componentes Backend
- `user` module (Controllers, Services, Domain, Repository)
- `invitation` module (token generation, email sending)

## Eventos
- `UserCreated` - Novo usuário criado
- `UserInvited` - Convite enviado
- `InviteAccepted` - Convite aceito (7 dias)
- `UserDeactivated` - Usuário desativado
- `UserRoleChanged` - Role alterada
- `UserBlocked` - Usuário bloqueado

## Permissões
- `user:create` - ADMIN, MANAGER
- `user:read` - ADMIN, MANAGER, AGENT
- `user:update` - ADMIN
- `user:delete` - ADMIN
- `user:invite` - ADMIN, MANAGER
- `user:role:assign` - ADMIN

## Dependências
- **Auth** (login, JWT, password hashing)
- **Companies** (empresa do usuário)
- **Notifications** (email de convite)

## Fluxo Resumido
1. Admin convida usuário → `POST /users/invite` → email com token (7d expira)
2. Usuário acessa link → aceita convite → cria senha → conta active
3. Usuário é autenticado via Auth → dados carregados do schema da empresa

## Checklist de Implementação
- [ ] CRUD completo com validações
- [ ] Sistema de convite com token de 7 dias
- [ ] Roles por empresa (N:N)
- [ ] Status: active/inactive/blocked
- [ ] Self-service (GET/PUT /users/me)
- [ ] Validação de email único por empresa
- [ ] Soft delete com 90 dias retenção
- [ ] Notificação por email em convites

## Checklist de Testes
- [ ] Convite expira após 7 dias
- [ ] Roles são isoladas por empresa
- [ ] Usuário bloqueado não autentica
- [ ] Self-service não permite escalation
- [ ] Email único por empresa

## Documentação Oficial Relacionada
- `docs/user/INVITE-FLOW.md`
- `docs/user/ROLE-MATRIX.md`
- `docs/user/STATUS-MANAGEMENT.md`

## Histórico de Revisão
| Data | Autor | Descrição |
|------|-------|-----------|
| 2026-07-15 | System | Criação do contexto |
