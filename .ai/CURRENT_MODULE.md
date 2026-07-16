# Módulo Atual

## Identificação
- **Nome:** Auth Backend
- **Tipo:** Backend Authentication
- **Status:** ✅ Concluído (Sprints 4.1 + 4.2 + 4.3)

## Objetivo
Módulo de autenticação completo: domain entities, infrastructure, JWT, Spring Security, RBAC, Login, /me, Logout.

## Dependências
- Nenhuma dependência externa

## Documentação Relacionada
- `docs/01-backend/Auth.md` — Documentação oficial Auth
- `docs/05-business-rules/Permissions.md` — Regras de permissões
- `docs-ai/AI_ROUTER.md` — Roteador central

## Context Relacionado
- `contexts/auth.context.md`

## Playbook
- `playbooks/implement-auth.md`

## Arquivos Criados (70+ Java)

### Domain (17) ✅
- User, RefreshToken, Role, UserRole, Permission, RolePermission
- Email, Password, Token, RoleName
- 6 events (UserCreated, UserLoggedIn, PasswordChanged, TokenRefreshed, PasswordResetRequested, UserLoggedOut)
- 3 exceptions (InvalidCredentials, TokenExpired, UserNotFound)

### Application (19) ✅
- 8 ports, 2 services, 9 DTOs

### Infrastructure (19) ✅
- 5 JPA entities, 5 Spring repos, 5 implementations
- BcryptPasswordEncoder, JwtTokenProvider, UserMapper, PermissionMapper
- SecurityConfig, JwtProperties, JwtAuthenticationEntryPoint, JwtAccessDeniedHandler
- JwtAuthenticationFilter, JwtUserPrincipal

### Presentation (5) ✅
- AuthController, UserController, 3 DTOs

### Database (3) ✅
- V001__initial_schema.sql, V002__auth_tables.sql, V003__rbac_tables.sql

## Pendências Técnicas
- [ ] mvn compile (Maven indisponível no ambiente)
- [ ] Testes unitários (Sprint 4.5)
- [ ] Testes de integração (Sprint 4.5)

## Próxima Etapa
**Sprint 4.4 — Frontend Auth** (login page, auth provider, protected routes)

---

*Atualizado em: 2026-07-15*
