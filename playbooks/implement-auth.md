# Playbook: Implementação do Módulo Auth

## Objetivo
Implementar o sistema de autenticação, autorização e controle de acesso (RBAC) do CRM, incluindo login, registro, refresh token rotation, e validação JWT.

## Pré-requisitos
- Módulo Companies implementado (base de multi-tenancy)
- Estrutura de domínio, aplicação e infraestrutura configurada
- Configurações de banco de dados e variáveis de ambiente prontas

## Documentos que DEVEM ser lidos
- `docs/Auth.md`
- `docs/Permissions.md`
- `docs/Security.md`
- `docs/03-database/Overview.md`
- `contexts/identity-context.md`
- `contexts/security-context.md`

## Arquivos que poderão ser alterados
### Backend
- `packages/backend/src/domain/identity/` — Entidades: User, Role, Permission, RefreshToken
- `packages/backend/src/application/identity/` — Casos de uso: LoginUseCase, RegisterUseCase, RefreshTokenUseCase, LogoutUseCase
- `packages/backend/src/infrastructure/security/` — JWT service, password hasher, token generator
- `packages/backend/src/infrastructure/persistence/` — Repositórios: UserRepository, RoleRepository, RefreshTokenRepository
- `packages/backend/src/presentation/rest/controller/AuthController.ts` — Endpoints REST
- `packages/backend/src/presentation/rest/middleware/` — AuthMiddleware, RBACMiddleware
- `packages/backend/src/config/auth.ts` — Configurações de JWT/segurança

### Frontend
- `packages/frontend/src/contexts/AuthContext.tsx`
- `packages/frontend/src/hooks/useAuth.ts`
- `packages/frontend/src/lib/api/auth.ts`
- `packages/frontend/src/middleware.ts` — Rotas protegidas

## Arquivos proibidos
- `packages/backend/src/infrastructure/database/migrations/` — NÃO alterar migrations já criadas
- `packages/frontend/src/app/layout.tsx` — Layout raiz não deve ser alterado aqui
- `packages/backend/src/config/database.ts` — Config de DB não pertence a este módulo

## Ordem de implementação
1. Definir entidades de domínio (User, Role, Permission, RefreshToken) em `domain/identity/`
2. Criar enums de permissões e roles no domínio
3. Implementar repositórios de persistência em `infrastructure/persistence/`
4. Implementar PasswordHasher (bcrypt/argon2) em `infrastructure/security/`
5. Implementar JWT Service (access + refresh tokens) em `infrastructure/security/`
6. Implementar casos de uso: Register, Login, RefreshToken, Logout
7. Implementar AuthController com endpoints REST
8. Implementar AuthMiddleware (validação JWT)
9. Implementar RBACMiddleware (verificação de permissões)
10. Configurar rotas de autenticação
11. Implementar AuthContext no frontend
12. Implementar hook useAuth
13. Configurar middleware de rotas protegidas no frontend
14. Integrar refresh token automático no cliente API

## Checklist Backend
- [ ] Entidade User com campos: id, email, passwordHash, name, companyId, isActive, createdAt, updatedAt
- [ ] Entidade Role com campos: id, name, permissions[], companyId
- [ ] Entidade Permission com enum de todas as permissões do sistema
- [ ] Entidade RefreshToken com campos: id, userId, token, expiresAt, isRevoked
- [ ] PasswordHasher com bcrypt (12 rounds) ou argon2
- [ ] JWT Service gera access token (15min) e refresh token (7d)
- [ ] Refresh token rotation: cada uso invalida o anterior
- [ ] LoginUseCase valida credenciais e retorna tokens
- [ ] RegisterUseCase cria usuário com role padrão
- [ ] RefreshTokenUseCase valida e rotaciona tokens
- [ ] LogoutUseCase revoga refresh token
- [ ] AuthMiddleware extrai e valida JWT do header Authorization
- [ ] RBACMiddleware verifica permissões do usuário para a rota
- [ ] Rate limiting no endpoint de login (5 tentativas/min)
- [ ] Tratamento de erros padronizado com códigos HTTP corretos
- [ ] Logs de tentativas de login (sucesso e falha)

## Checklist Frontend
- [ ] AuthContext com providers: user, tokens, login, logout, register
- [ ] Hook useAuth expõe: user, isAuthenticated, login, logout, register, loading
- [ ] Interceptador de requisição que anexa Bearer token
- [ ] Interceptador que renova token automaticamente no 401
- [ ] Middleware de rotas protegidas redireciona para /login
- [ ] Página de login com formulário (email + password)
- [ ] Página de registro (se aplicável)
- [ ] Proteção de rotas baseada em roles/permissões

## Checklist Banco
- [ ] Tabela `users`: id, email (unique), password_hash, name, company_id (FK), is_active, created_at, updated_at
- [ ] Tabela `roles`: id, name, company_id (FK), created_at
- [ ] Tabela `permissions`: id, name, description
- [ ] Tabela `user_roles`: user_id (FK), role_id (FK), created_at
- [ ] Tabela `role_permissions`: role_id (FK), permission_id (FK)
- [ ] Tabela `refresh_tokens`: id, user_id (FK), token (unique), expires_at, is_revoked, created_at
- [ ] Índices em: users.email, refresh_tokens.token, refresh_tokens.user_id
- [ ] Foreign keys configuradas com ON DELETE CASCADE/RESTRICT conforme regra

## Checklist Testes
- [ ] Testes unitários: PasswordHasher (hash + verify)
- [ ] Testes unitários: JWT Service (generate + validate + decode)
- [ ] Testes unitários: Refresh Token rotation
- [ ] Testes de integração: Login com credenciais válidas/inválidas
- [ ] Testes de integração: Registro de novo usuário
- [ ] Testes de integração: Refresh token flow completo
- [ ] Testes de integração: Logout revoga token
- [ ] Testes de integração: RBAC bloqueia acesso sem permissão
- [ ] Testes de integração: Rate limiting funciona
- [ ] Testes E2E: Fluxo completo login → acesso protegido → refresh → logout

## Checklist Documentação
- [ ] Atualizar `docs/Auth.md` com endpoints e exemplos
- [ ] Atualizar `docs/Permissions.md` com lista de permissões
- [ ] Atualizar `docs/Security.md` com práticas de segurança
- [ ] Documentar variáveis de ambiente necessárias
- [ ] Documentar fluxo de refresh token rotation

## Checklist Final
- [ ] Login com email/senha funciona
- [ ] JWT é validado em rotas protegidas
- [ ] Refresh token rotation funciona (token antigo é invalidado)
- [ ] RBAC bloqueia usuários sem permissão correta
- [ ] Rate limiting funciona no login
- [ ] Todos os testes passam
- [ ] Nenhum secret/key hardcoded no código
- [ ] Logs de auditoria são gerados
