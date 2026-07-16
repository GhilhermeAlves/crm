# Checklist — Sprint 4.1

## ✅ Concluído (4.1A — Planejamento)

### Validação de Documentação
- [x] IMPLEMENTATION_QUEUE.md validado — Sprint 4.1 🚧 Em andamento
- [x] AI_ROUTER.md validado — Auth roteado corretamente
- [x] auth.context.md validado — Contexto completo e preciso
- [x] implement-auth.md validado — Playbook com 121 itens
- [x] Auth.md validado — Documentação oficial suficiente
- [x] Permissions.md validado — Matriz RBAC documentada

### Validação de Arquitetura
- [x] Clean Architecture sendo seguida (Domain → Application → Infrastructure → Presentation)
- [x] Dependências no sentido correto
- [x] Nenhuma dependência externa não contemplada
- [x] Nenhum blocker identificado

### Registros
- [x] SPRINT.md atualizado com planejamento
- [x] CHECKLIST.md gerado
- [x] REVIEW.md criado
- [x] RETROSPECTIVE.md criado
- [x] REPORT.md gerado com planejamento
- [x] .ai/CURRENT_SPRINT.md atualizado
- [x] .ai/CURRENT_MODULE.md atualizado
- [x] .ai/CURRENT_TASK.md atualizado
- [x] .ai/WORKLOG.md atualizado
- [x] .ai/LAST_SESSION.md atualizado
- [x] .ai/NEXT_STEPS.md atualizado

## 📝 Pendente para 4.1B — Desenvolvimento

### Spring Security
- [ ] SecurityConfig.java — SecurityFilterChain
- [ ] JwtAuthenticationFilter.java — Filtro de validação JWT
- [ ] Rotas públicas configuradas (/auth/login, /auth/refresh, /auth/forgot-password, /auth/reset-password)
- [ ] Rotas protegidas configuradas (/auth/logout, /auth/me, /auth/change-password, /users/**)
- [ ] CORS configurado

### Configuração
- [ ] application.yml — jwt.secret
- [ ] application.yml — jwt.access-token-expiry
- [ ] application.yml — jwt.refresh-token-expiry
- [ ] application-dev.yml — JWT config
- [ ] application-prod.yml — JWT config

### Exception Handling
- [ ] GlobalExceptionHandler — Tratamento de exceções
- [ ] InvalidCredentialsException handler
- [ ] TokenExpiredException handler
- [ ] UserNotFoundException handler
- [ ] Validation errors handler
- [ ] HttpStatus corretos (400, 401, 403, 404, 500)

### RBAC
- [ ] @PreAuthorize nos endpoints
- [ ] Verificação de roles no controller
- [ ] Permissions por endpoint

### OpenAPI/Swagger
- [ ] springdoc-openapi configurado (já no pom.xml)
- [ ] Auth endpoints documentados
- [ ] Security scheme JWT configurado no Swagger

### Testes Unitários
- [ ] PasswordEncoder test
- [ ] JwtTokenProvider test
- [ ] AuthService test
- [ ] UserService test
- [ ] User domain test
- [ ] Email value object test
- [ ] Password value object test
- [ ] Domain events test

### Testes de Integração
- [ ] AuthController login flow test
- [ ] AuthController refresh flow test
- [ ] AuthController logout test
- [ ] UserRepository test
- [ ] Spring Security filter test
- [ ] Rate limiting test

### Verificação
- [ ] mvn compile sem erros
- [ ] Checkstyle (se configurado)
- [ ] Dependências circulares verificadas

### Documentação
- [ ] docs/CHANGELOG.md atualizado
- [ ] backend/IMPLEMENTATION_REPORT.md atualizado
- [ ] .ai/LAST_SESSION.md atualizado
- [ ] .ai/WORKLOG.md atualizado
- [ ] .ai/CURRENT_TASK.md atualizado
- [ ] sprints/4.1/REVIEW.md preenchido
- [ ] sprints/4.1/RETROSPECTIVE.md preenchido
- [ ] sprints/4.1/REPORT.md finalizado

---

*Atualizado em: 2026-07-15*
