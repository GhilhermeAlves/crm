# M2 — Authentication Foundation

## Objetivo
Estabelecer a base completa de autenticação, autorização e identidade do CRM, incluindo entidades de domínio, infraestrutura de segurança, RBAC, gerenciamento de usuários e login funcional.

## Sprints Relacionadas
| Sprint | Nome | Status |
|--------|------|--------|
| 4.1A | Planejamento Auth | ✅ Concluída |
| 4.1B | Desenvolvimento (Spring Security, JWT, Exception Handling) | ✅ Concluída |
| 4.1C | Review | ✅ Concluída (93/100) |
| 4.1D | Close | ✅ Concluída |
| 4.2 | User Identity Foundation (Permission, RolePermission) | ✅ Concluída |
| 4.3 | Login (JWT emissão, refresh token, /me, logout) | ✅ Concluída (93/100) |

## Escopo Realizado

### Sprint 4.1 — Auth Infrastructure
- **Domain (14):** User, RefreshToken, Role, UserRole, Email, Password, Token, RoleName + 3 events + 3 exceptions
- **Application (19):** 8 ports (AuthUseCase, UserUseCase, UserRepository, RefreshTokenRepository, RoleRepository, PasswordEncoder, JwtProvider, EventPublisher) + 2 services (AuthService, UserService) + 9 DTOs
- **Infrastructure (12):** 3 JPA entities, 3 Spring repos, 3 impls, BcryptPasswordEncoder, JwtTokenProvider, UserMapper
- **Presentation (5):** AuthController, UserController + 3 DTOs
- **Database (1):** V002__auth_tables.sql (users, roles, user_roles, refresh_tokens)
- **Security Infra (7):** SecurityConfig, JwtProperties, JwtAuthenticationEntryPoint, JwtAccessDeniedHandler, JwtAuthenticationFilter, OpenApiConfig, GlobalExceptionHandler

### Sprint 4.2 — User Identity Foundation
- **Domain (2):** Permission, RolePermission
- **Application (2):** PermissionRepository, RolePermissionRepository ports
- **Infrastructure (7):** PermissionJpaEntity, RolePermissionJpaEntity, SpringDataPermissionRepository, SpringDataRolePermissionRepository, PermissionMapper, PermissionRepositoryImpl, RolePermissionRepositoryImpl
- **Database (1):** V003__rbac_tables.sql

### Sprint 4.3 — Login
- **Domain/Events (3):** TokenRefreshedEvent, PasswordResetRequestedEvent, UserLoggedOutEvent
- **Infrastructure/Security (1):** JwtUserPrincipal
- **Configuração:** SecurityConfig (rotas protegidas), JwtAuthenticationFilter (validação funcional)
- **Controllers:** AuthController (login, /me, logout, changePassword corrigidos)
- **Services:** AuthService (@Service + changePassword bug fix), UserService (@Service)
- **Repositories:** UserRepository + findAllByCompanyId

## Componentes Implementados
| Componente | Status |
|-----------|--------|
| User domain entity | ✅ |
| Role domain entity | ✅ |
| Permission domain entity | ✅ |
| RolePermission domain entity | ✅ |
| Spring Security (SecurityConfig, Filter, CORS) | ✅ |
| JWT (Provider, Properties, EntryPoint, DeniedHandler, Filter) | ✅ |
| PasswordEncoder (BCrypt 12 rounds) | ✅ |
| RBAC structure (permissions, role_permissions) | ✅ |
| Exception Handling (GlobalExceptionHandler + EntryPoint + DeniedHandler) | ✅ |
| OpenAPI (Bearer JWT security scheme) | ✅ |
| Login funcional (JWT emissão, refresh token rotation) | ✅ |
| /me endpoint | ✅ |
| Logout (revogação refresh token + evento) | ✅ |
| Flyway Migrations (V001, V002, V003) | ✅ |

## Dependências
- **Tasks M1** (Sprints 0-3.3) — Toda a fundação do projeto, Knowledge Layer, Runtime, Sprint Management
- **Nenhuma dependência externa** não satisfeita

## Resultado
- **Total de arquivos Java criados:** 70+ (58 Sprint 4.1 + 11 Sprint 4.2 + 4 Sprint 4.3)
- **Total de arquivos infraestrutura segurança:** 7 (Sprint 4.1B)
- **Migration SQL:** 3 (V001, V002, V003)
- **Nota da Revisão 4.1:** 93/100
- **Nota da Revisão 4.3:** 93/100
- **Progresso do Projeto:** 41% (9/22 sprints concluídas)
- **ADRs criados:** ADR-012 (Roles Hardcoded), ADR-013 (Password Reset Placeholder)

## Lições Aprendidas
1. A divisão da Sprint 4.1 em sub-sprints (A/B/C/D) permitiu foco e qualidade
2. @ConfigurationProperties + @EnableConfigurationProperties é o padrão ideal para configs JWT
3. ObjectMapper deve ser injetado, nunca instanciado com `new`
4. Handlers de exceção devem cobrir tanto filter chain quanto controller layer
5. Sprint 4.3 confirma que a abordagem de sub-sprints (Development → Review → Close) funciona bem
6. Import removido incorretamente no UserService (Collectors) — revisão cuidadosa de wildcard imports
7. Evento de logout precisa do companyId correto — sempre validar parâmetros de eventos

## Próximos Passos
1. ✅ Sprint 4.3 — Login (concluído)
2. ⏳ Sprint 4.4 — Frontend Auth (login page, auth provider, protected routes)
3. Sprint 4.5 — Testes Auth
4. Sprint 5 — Tenant (Multi-tenancy)

## Status
✅ **Concluído** — Milestone M2 completo com 3 sprints finalizadas.
