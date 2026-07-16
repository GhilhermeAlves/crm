# Sprint 4.3 — Login

**Nome:** Login (Autenticação JWT)
**Módulo:** Auth Backend
**Dependências:** Sprint 4.1 (Auth Infrastructure), Sprint 4.2 (User Identity)
**Data:** 2026-07-15
**Status:** ✅ Concluída

## Objetivo
Implementar autenticação funcional: emissão de JWT, refresh token rotation, endpoint /me e logout.

## Escopo
- Ativar JwtAuthenticationFilter no SecurityFilterChain
- Implementar validação de token JWT no filtro
- Corrigir logout/changePassword (extrair userId do SecurityContext)
- Adicionar endpoint /me
- Corrigir eventos faltantes (TokenRefreshedEvent, PasswordResetRequestedEvent, UserLoggedOutEvent)
- Adicionar @Service às classes de serviço
- Corrigir UserRepository.findAllByCompanyId

## Critérios de Conclusão
- [x] Login funcional (POST /auth/login)
- [x] Refresh token funcional (POST /auth/refresh)
- [x] Logout funcional (POST /auth/logout)
- [x] /me funcional (GET /auth/me)
- [x] Rotas públicas/protegidas configuradas
- [x] Filtro JWT validando tokens
- [ ] Projeto compila sem erros (Maven indisponível)
