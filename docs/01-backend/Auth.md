# Auth — Autenticação e Autorização

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Responsabilidades](#responsabilidades)
- [Fluxo de Autenticação](#fluxo-de-autenticação)
- [Fluxo de Autorização](#fluxo-de-autorização)
- [Tokens](#tokens)
- [Endpoints](#endpoints)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar o sistema de autenticação e autorização do CRM SaaS Omnichannel, incluindo fluxos de login, controle de acesso e gestão de tokens.

## Descrição

O sistema utiliza JWT (JSON Web Tokens) com Access Token e Refresh Token para autenticação stateless. A autorização é baseada em roles e permissões granulares (RBAC). Cada empresa (tenant) possui suas próprias roles e permissões.

## Responsabilidades

- Autenticar usuários via email/senha ou providers externos
- Gerenciar ciclo de vida dos tokens (criação, rotação, revogação)
- Controlar acesso a recursos via roles e permissões
- Proteger endpoints com filtros de segurança
- Gerenciar multi-tenancy via JWT claims

## Fluxo de Autenticação

### Login

```
1. Usuário envia email + senha
        │
2. Backend valida credenciais no banco
        │
3. Backend gera Access Token (15 min) + Refresh Token (7 dias)
        │
4. Tokens são retornados ao cliente
        │
5. Cliente armazena Access Token em memory
        │
6. Refresh Token armazenado em cookie HttpOnly
```

### Refresh Token

```
1. Access Token expira (401 response)
        │
2. Cliente envia Refresh Token
        │
3. Backend valida Refresh Token no banco
        │
4. Backend gera novos Access Token + Refresh Token
        │
5. Refresh Token antigo é invalidado (rotação)
```

### Logout

```
1. Usuário solicita logout
        │
2. Backend invalida Refresh Token no banco
        │
3. Tokens são removidos do cliente
        │
4. Sessão encerrada
```

## Fluxo de Autorização

```
1. Request chega com JWT
        │
2. Filtro JWT extrai claims (userId, companyId, roles)
        │
3. @PreAuthorize verifica permissão necessária
        │
4. Se autorizado → Processa request
   Se não → 403 Forbidden
```

### RBAC (Role-Based Access Control)

```
User → has → Role → has → Permission → grants → Resource + Action

Exemplo:
User "João" → has → Role "Manager" → has → Permission "lead:write" → grants → Lead + Create/Update/Delete
```

### Hierarquia de Roles

| Role | Descrição | Permissões |
|---|---|---|
| SUPER_ADMIN | Administrador da plataforma | Todas |
| ADMIN | Administrador da empresa | Todas da empresa |
| MANAGER | Gerente | Leitura/escrita de leads, contacts, reports |
| AGENT | Atendente | Leitura/escrita de contacts, chat, conversas |
| VIEWER | Visualizador | Somente leitura |

## Tokens

### Access Token

| Campo | Descrição |
|---|---|
| `sub` | User ID |
| `company_id` | Company ID (tenant) |
| `roles` | Lista de roles |
| `permissions` | Lista de permissões |
| `iat` | Data de emissão |
| `exp` | Data de expiração (15 min) |
| `jti` | Identificador único |

### Refresh Token

| Campo | Descrição |
|---|---|
| `sub` | User ID |
| `jti` | Identificador único |
| `family` | Família do token (para detecção de roubo) |
| `iat` | Data de emissão |
| `exp` | Data de expiração (7 dias) |

## Endpoints

| Método | Endpoint | Descrição | Auth |
|---|---|---|---|
| POST | `/api/v1/auth/login` | Login | Não |
| POST | `/api/v1/auth/refresh` | Refresh token | Refresh Token |
| POST | `/api/v1/auth/logout` | Logout | Sim |
| POST | `/api/v1/auth/forgot-password` | Esqueci senha | Não |
| POST | `/api/v1/auth/reset-password` | Resetar senha | Token |
| GET | `/api/v1/auth/me` | Dados do usuário logado | Sim |
| PUT | `/api/v1/auth/change-password` | Trocar senha | Sim |

## Dependências

- [Users.md](./Users.md) — Gestão de usuários
- [Companies.md](./Companies.md) — Multi-tenancy
- [03-database/Overview.md](../03-database/Overview.md) — Schema de users e tokens
- [Permissions.md](./Permissions.md) — Permissões e RBAC
- [00-core/Security.md](../00-core/Security.md) — Diretrizes de segurança

## Regras

- Access Token expira em 15 minutos
- Refresh Token expira em 7 dias
- Refresh Token é rotacionado a cada uso
- Tokens revogados são mantidos no banco por 30 dias
- Senhas devem ter mínimo 8 caracteres, 1 maiúscula, 1 número, 1 símbolo
- Máximo 5 tentativas de login antes de bloqueio temporário (15 min)
- Tokens não podem ser revogados antes do expiry (stateless)
- Rate limiting: máximo 10 requests/min por IP no endpoint de login

## Futuras Melhorias

- Adicionar autenticação via OAuth 2.0 (Google, Microsoft)
- Implementar MFA (Multi-Factor Authentication)
- Adicionar whitelist de IPs para acessos sensíveis
- Implementar session management (ver dispositivos ativos)
- Adicionar audit log de autenticação
- Suporte a SSO via SAML/OpenID Connect para enterprise

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial da documentação de auth |
