# Users — Gestão de Usuários

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Responsabilidades](#responsabilidades)
- [Fluxo](#fluxo)
- [Endpoints](#endpoints)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar o módulo de gestão de usuários, incluindo criação, atualização, profile e convites.

## Descrição

O módulo de usuários gerencia todas as operações relacionadas às pessoas que utilizam o sistema. Usuários estão associados a empresas (tenants) e possuem roles e permissões específicas.

## Responsabilidades

- Criar, atualizar e desativar usuários
- Gerenciar profile e preferências
- Convidar novos usuários para a empresa
- Atribuir e revogar roles
- Gerenciar status (ativo, inativo, bloqueado)

## Fluxo

### Criação de Usuário

```
1. Admin solicita criação de usuário
        │
2. Backend valida dados (email único na empresa)
        │
3. Backend cria usuário com role padrão (AGENT)
        │
4. Backend envia convite por email
        │
5. Usuário clica no link e define senha
        │
6. Usuário ativado e pode acessar o sistema
```

### Convite

```
1. Admin convida usuário por email
        │
2. Backend cria registro pendente (InvitedUser)
        │
3. Email de convite enviado com link
        │
4. Usuário aceita convite e completa registro
        │
5. Usuário criado e associado à empresa
```

## Endpoints

| Método | Endpoint | Descrição | Permissão |
|---|---|---|---|
| GET | `/api/v1/users` | Listar usuários da empresa | `user:read` |
| GET | `/api/v1/users/{id}` | Buscar usuário por ID | `user:read` |
| POST | `/api/v1/users` | Criar usuário | `user:write` |
| PUT | `/api/v1/users/{id}` | Atualizar usuário | `user:write` |
| DELETE | `/api/v1/users/{id}` | Desativar usuário | `user:delete` |
| POST | `/api/v1/users/{id}/role` | Atribuir role | `user:manage` |
| POST | `/api/v1/users/invite` | Convidar usuário | `user:invite` |
| GET | `/api/v1/users/me` | Meu profile | Autenticado |
| PUT | `/api/v1/users/me` | Atualizar meu profile | Autenticado |

### DTOs

**CreateUserCommand**:
```json
{
  "email": "string (required)",
  "firstName": "string (required)",
  "lastName": "string (required)",
  "role": "string (required: ADMIN, MANAGER, AGENT, VIEWER)"
}
```

**UpdateUserCommand**:
```json
{
  "firstName": "string (optional)",
  "lastName": "string (optional)",
  "role": "string (optional)",
  "status": "string (optional: ACTIVE, INACTIVE)"
}
```

**UserResponse**:
```json
{
  "id": "uuid",
  "email": "string",
  "firstName": "string",
  "lastName": "string",
  "role": "string",
  "status": "string",
  "lastLoginAt": "datetime",
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

## Dependências

- [Auth.md](./Auth.md) — Autenticação do usuário
- [Companies.md](./Companies.md) — Empresa associada
- [Notifications.md](./Notifications.md) — Envio de emails

## Regras

- Email deve ser único por empresa
- Usuário desativado não pode fazer login
- Admin não pode desativar a si mesmo
- Um usuário pode pertencer a múltiplas empresas
- Roles hierárquicas: SUPER_ADMIN > ADMIN > MANAGER > AGENT > VIEWER
- Convites expiram em 7 dias
- Máximo de usuários por plano: definido na subscription

## Futuras Melhorias

- Adicionar upload de avatar/profile photo
- Implementar SSO (Single Sign-On)
- Adicionar 2FA/MFA
- Gerenciar múltiplas empresas por usuário
- Adicionar audit log de mudanças de perfil
- Suporte a LDAP para enterprise

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
