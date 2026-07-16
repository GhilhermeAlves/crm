# Sprint 4.1 — Infraestrutura Auth

## Identificação
- **Sprint:** 4.1
- **Fase atual:** 4.1A — Planejamento
- **Próxima fase:** 4.1B — Desenvolvimento
- **Nome:** Infraestrutura Auth
- **Data Planejamento:** 2026-07-15
- **Status Planejamento:** ✅ Concluído
- **Responsável:** AI Agent
- **Fase:** Infraestrutura

## Objetivo
Implementar o sistema de autenticação, autorização e controle de acesso (RBAC) do CRM, incluindo login, JWT, refresh token rotation, Spring Security, filtros de segurança e proteção de endpoints.

## Escopo

### Já Implementado (4.1 original)
- Domain entities: User, RefreshToken, Role, UserRole
- Value objects: Email, Password, Token, RoleName
- Domain events: UserCreated, UserLoggedIn, PasswordChanged
- Domain exceptions: InvalidCredentials, TokenExpired, UserNotFound
- Application ports: AuthUseCase, UserUseCase, 6 output ports
- Application services: AuthService, UserService
- Application DTOs: 9 records
- Infrastructure JPA: 3 entities, 3 repos, 3 implementations
- Infrastructure mapper: UserMapper
- Infrastructure security: BcryptPasswordEncoder, JwtTokenProvider
- Presentation controllers: AuthController, UserController
- Presentation DTOs: LoginRequestDto, RegisterRequestDto, UserResponseDto
- Database: V002__auth_tables.sql (4 tables + seed)

### Pendente para 4.1B
- SecurityConfig.java — Spring Security FilterChain
- JwtAuthenticationFilter.java — Filtro JWT
- application.yml — Propriedades JWT
- GlobalExceptionHandler — Tratamento de exceções
- Testes unitários
- Testes de integração
- OpenAPI/Swagger
- Verificação de compilação (mvn compile)

## Arquivos Previstos (para 4.1B)

### Security
- `infrastructure/identity/security/SecurityConfig.java` — Spring Security chain
- `infrastructure/identity/security/JwtAuthenticationFilter.java` — JWT filter

### Config
- `src/main/resources/application.yml` — JWT properties update

### Handlers
- `presentation/rest/handler/GlobalExceptionHandler.java` — Exception handler

### Testes
- `src/test/unit/domain/identity/...` — Unit tests
- `src/test/integration/identity/...` — Integration tests

## Critérios de Sucesso
1. Spring Security FilterChain configurado
2. JwtAuthenticationFilter validando tokens
3. Rotas públicas e protegidas configuradas
4. AuthController endpoints funcionais
5. mvn compile sem erros
6. Testes unitários ≥80%
7. Testes de integração ≥60%

## Dependências
- Nenhuma (primeira sprint de implementação)

## Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|-------|--------------|---------|-----------|
| Spring Security configuration complexa | Média | Alto | Seguir documentação oficial e padrão Spring |
| JWT secret hardcoded | Baixa | Alto | Usar variável de ambiente |
| Performance do BCrypt (12 rounds) | Baixa | Médio | Testar em cenário de carga |
| Inconsistência entre docs e código | Média | Médio | Validar após implementação |

---

*Atualizado em: 2026-07-15 — Planejamento 4.1A concluído*
