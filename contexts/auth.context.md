# Auth Context

## Resumo do Módulo
Autenticação e autorização do sistema CRM. Implementa JWT com Access Token (15min) + Refresh Token (7d) rotation e RBAC com 5 roles.

## Objetivo
Gerenciar autenticação, autorização e ciclo de vida de sessões dos usuários.

## Responsabilidades
- Login/logout com JWT access + refresh token rotation
- RBAC com 5 roles: SUPER_ADMIN, ADMIN, MANAGER, AGENT, VIEWER
- Recuperação de senha (forgot/reset password)
- Validação e renovação de tokens
- Gestão de refresh tokens (blacklist/revoke)

## Entidades Relacionadas
- User, Role, UserRole, RefreshToken

## APIs Relacionadas
- `POST /auth/login` - Login
- `POST /auth/refresh` - Renovar access token
- `POST /auth/logout` - Logout (revoga refresh)
- `POST /auth/forgot-password` - Solicita reset
- `POST /auth/reset-password` - Reset com token
- `GET /auth/me` - Dados do usuário logado
- `PUT /auth/change-password` - Alterar senha

## Banco Relacionado
- `users` - Credenciais e dados do usuário
- `refresh_tokens` - Tokens de refresh ativos
- `roles` - Catálogo de roles
- `user_roles` - Relação usuário-role por empresa

## Componentes Frontend
- Login page, Forgot/Reset password pages
- AuthGuard, ProtectedRoute
- Token refresh interceptor (Axios)

## Componentes Backend
- `auth` module (Presentation: Controllers, Application: Services, Domain: Entities, Infrastructure: JWT/TokenRepository)
- `security` package (filters, JWT provider, password encoder)

## Eventos
- `UserLoggedIn` - Login bem-sucedido
- `UserLoggedOut` - Logout
- `PasswordResetRequested` - Solicitação de reset
- `PasswordChanged` - Senha alterada
- `TokenRefreshed` - Refresh token rotacionado

## Permissões
- `auth:login` - Público (3 rotas públicas)
- `auth:manage` - SUPER_ADMIN
- `auth:change-password` - Todos os usuários autenticados

## Dependências
- **Companies** (company_id no JWT claim)
- **Notifications** (envio de email de reset)
- **Users** (validação de credenciais)

## Fluxo Resumido
1. Usuário envia credenciais → `POST /auth/login` → valida → gera access+refresh tokens
2. Requisições subsequentes usam access token no header → middleware valida → permite acesso
3. Access token expira → `POST /auth/refresh` → rotaciona tokens → acesso continua

## Checklist de Implementação
- [ ] JWT provider com access (15min) + refresh (7d) rotation
- [ ] RBAC middleware com 5 roles
- [ ] Password encoder (BCrypt)
- [ ] Refresh token rotation com blacklist
- [ ] Forgot password com token de 1h
- [ ] Rate limiting no login (5 tentativas/15min)
- [ ] Logout revoga refresh token
- [ ] Company isolation no JWT claim

## Checklist de Testes
- [ ] Login com credenciais válidas/inválidas
- [ ] Token refresh e rotação
- [ ] Logout revoga refresh token
- [ ] RBAC impede acesso não autorizado
- [ ] Rate limiting em brute force

## Documentação Oficial Relacionada
- `docs/auth/JWT-IMPLEMENTATION.md`
- `docs/auth/RBAC-GUIDE.md`
- `docs/security/PASSWORD-POLICY.md`

## Histórico de Revisão
| Data | Autor | Descrição |
|------|-------|-----------|
| 2026-07-15 | System | Criação do contexto |
