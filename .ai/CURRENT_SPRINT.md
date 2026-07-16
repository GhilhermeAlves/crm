# Sprint Atual

## Identificação
- **Sprint:** 4.3
- **Fase:** Login
- **Nome:** Autenticação JWT
- **Data Início:** 2026-07-15
- **Data Fim:** 2026-07-15
- **Status:** ✅ Concluída

## Objetivo
Implementar autenticação funcional: emissão de JWT, refresh token rotation, endpoint /me e logout.

## Progresso
- [x] Eventos criados (TokenRefreshed, PasswordResetRequested, UserLoggedOut)
- [x] @Service adicionados (AuthService, UserService)
- [x] SecurityConfig — rotas protegidas + JwtAuthenticationFilter no chain
- [x] JwtAuthenticationFilter — validação de token implementada
- [x] AuthController — logout/changePassword corrigidos, /me endpoint adicionado
- [x] UserRepository — findAllByCompanyId adicionado + implementado
- [x] AuthService.changePassword — bug fix (senha não era salva)
- [x] Review — nota 93/100, 3 correções aplicadas
- [x] Close — documentação, .ai, milestones atualizados

## Próxima Sprint
- **Sprint:** 4.4 — Frontend Auth
- **Status:** ⏳ Pronta para iniciar
- **Módulo:** Frontend Authentication
- **Playbook:** `implement-auth.md`
- **Contexto:** `auth.context.md`

## Módulos Envolvidos
- `domain/identity/` — Domain entities (17 arquivos, +3 events em 4.3)
- `application/identity/` — Application services (19 arquivos)
- `infrastructure/identity/` — Infrastructure (12 arquivos)
- `infrastructure/security/` — Security (6 arquivos, +1 JwtUserPrincipal)
- `presentation/rest/identity/` — REST controllers (5 arquivos)

---

*Atualizado em: 2026-07-15*
