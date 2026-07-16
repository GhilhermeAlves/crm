# Checklist — Sprint 4.3

## Eventos (3)
- [x] TokenRefreshedEvent.java
- [x] PasswordResetRequestedEvent.java
- [x] UserLoggedOutEvent.java

## Application (2)
- [x] AuthService — @Service adicionado
- [x] UserService — @Service adicionado
- [x] UserRepository — findAllByCompanyId adicionado

## Infrastructure (3)
- [x] SecurityConfig — FilterChain com JwtAuthenticationFilter + rotas protegidas
- [x] JwtAuthenticationFilter — Implementação completa de validação
- [x] JwtUserPrincipal.java

## Presentation (2)
- [x] AuthController — logout/changePassword corrigidos, /me adicionado
- [ ] Tests (Sprint 4.5)

## Repository Fixes (2)
- [x] SpringDataUserRepository — findByCompanyId adicionado
- [x] UserRepositoryImpl — findAllByCompanyId implementado

## Bug Fix
- [x] AuthService.changePassword — agora salva a nova senha
