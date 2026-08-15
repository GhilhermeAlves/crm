# CHANGELOG

## [6.1.0] - 2026-08-15 - Storage: CRUD completo (download/exclusão) + tela de Arquivos

### Added
- **Backend** (`application/storage/`): completados os casos de uso do módulo de
  armazenamento — `list` (metadados da empresa, mais recentes primeiro), `download`
  (retorna `StorageDownload` com bytes) e `delete` — adicionados a `StorageUseCase`,
  `StorageRepository`, `StorageJpaRepository`, `StorageRepositoryImpl` e
  `StorageService`. Todos escopados por tenant (`TenantContext` + RLS FORCE) e com
  auditoria via `TenantAuditRecorder`.
- **DTO**: `StorageDownload` (id, fileName, contentType, sizeBytes, data).
- **Exceção**: `StorageObjectNotFoundException` (`domain/storage/exception/`) mapeada
  no `GlobalExceptionHandler` → HTTP **404** quando o objeto não existe/não pertence
  à empresa.
- **Endpoints** (`/api/v1/companies/{companyId}/storage`):
  `GET` (listar), `GET /{objectId}` (download com `Content-Type` e
  `Content-Disposition: attachment`), `DELETE /{objectId}` (no-content);
  acesso restrito à própria empresa (SUPER_ADMIN cross-tenant permitido).
- **Frontend** (`features/storage/`): `types`, `service`, hook `useStorage`
  (`useStorageObjects`/`useUploadFile`/`useDownloadFile`/`useDeleteFile`) e testes;
  nova página `/storage` ("Arquivos") com upload, listagem, download e exclusão
  (ConfirmDialog) + `formatBytes`; Sidebar e `ROUTES.STORAGE` atualizados.

### Qualidade
- Backend: **366 testes** unitários passando (BUILD SUCCESS; +`StorageServiceTest`
  para list/download/delete e 404 cross-tenant).
- Frontend: **133 testes** (26 arquivos) passando; typecheck/lint OK; build prod OK
  com rota `/storage` gerada.

## [6.0.0] - 2026-08-15 - Sprint 16: Omnichannel - WhatsApp (base)

### Added
- **Domínio** (`domain/omnichannel/`): `Channel`, `Conversation`, `Message` + enums
  (`ChannelType`, `ChannelProvider`, `ChannelStatus`, `ConversationStatus`,
  `MessageDirection`, `MessageStatus`, `MessageType`), fábricas create/reconstitute
  e transições de estado (touch/markRead/markSent/markStatus/close/reopen/assignContact).
- **Aplicação** (`application/omnichannel/`): `OmnichannelChannelService` (CRUD + setStatus),
  `OmnichannelInboxService` (listar/detalhar/enviar/markRead, RLS FORCE via GUC,
  falha de provider marca a mensagem como FAILED), `WhatsAppWebhookService`
  (verificação GET, mensagem recebida com resolução de empresa pelo canal via
  SECURITY DEFINER, idempotência por `external_message_id`, status update, e
  publicação de `WorkflowTriggerEvent.whatsAppMessageReceived`).
- **DTOs**: `ChannelRequest/Response`, `ConversationResponse`, `ConversationDetailResponse`,
  `MessageResponse`, `SendMessageRequest`.
- **Persistência/Infra**: repositories omnichannel (RLS FORCE), `OmnichannelCompanyResolver`,
  `WhatsAppWebhookParser`/`WhatsAppProvider` (portas), `WhatsAppCloudApiProvider`/parser
  (Meta) e `FakeWhatsAppProvider` (dev), seed de permissões `omnichannel:*`
  (SUPER_ADMIN/MANAGER CRUD, AGENT leitura, VIEWER sem acesso).
- **DB/Migrações**: `V044__omnichannel_tables.sql` (channels/conversations/messages + RLS +
  GUC + idempotência `external_message_id`) e `V045__omnichannel_permissions.sql`
  (permissões `omnichannel:*`).
- **Controllers** (`/api/v1/omnichannel/channels` e `/api/v1/omnichannel/inbox`,
  `@PreAuthorize('omnichannel:*')`) + webhook de WhatsApp.
- **Frontend** (`features/omnichannel/`): types/service/hooks/componentes
  (`ConversationList`, `ChatThread`, `ChannelTable` via página, `ChannelFormDialog`,
  `ChannelStatusBadge`) e páginas `/inbox` (lista + chat + envio) e `/channels`
  (configuração de canais); Sidebar atualizada.

### Qualidade
- Backend: +33 testes (domínio Channel/Conversation/Message, `OmnichannelInboxServiceTest`,
  `WhatsAppWebhookServiceTest`, `OmnichannelChannelServiceTest`) — suíte omnichannel verde.
- Frontend: typecheck/lint sem erros nos novos arquivos; build prod OK com rotas
  `/inbox` e `/channels` geradas.

### Pendente (do sprint)
- Deploy + validação na VPS (Docker/Testcontainers) e IT `OmnichannelIsolationIT`
  (RLS cross-tenant em channels/conversations/messages) a rodar na VPS com Docker.

## [5.1.0] - 2026-08-14 - Sprint 14: Workflows - disparo por inatividade (OPPORTUNITY_STALE)

### Added
- **Novo trigger** OPPORTUNITY_STALE no enum TriggerEvent.
- **WorkflowTriggerEvent.opportunityStale(...)**: factory com eventId **deterministico** (= opportunityId)
  para idempotencia entre varreduras, e campo de contexto opportunity.daysWithoutActivity.
- **WorkflowStaleOpportunityScanner** (infrastructure/workflow/scheduler/): varredura diaria
  (cron 0 0 7 * * *, configuravel via workflow.stale.cron) que publica o evento para oportunidades em
  **aberto** sem atividade ha 7+ dias (mesmo criterio do dashboard de atencao).
- **WorkflowSchedulingConfig**: habilita @EnableScheduling no backend.
- **Template seed** "Follow-up apos oportunidade parada" (WorkflowTemplateSeeder) - condicao
  opportunity.daysWithoutActivity >= 7 + acao criar tarefa.

### Qualidade
- Backend: 318 testes unitarios passando (BUILD SUCCESS; +3 WorkflowStaleOpportunityScannerTest).

## [5.0.0] - 2026-08-14 — Sprint 13: Workflows (Automação de Tarefas e Atividades)

### Added
- **Domínio** (`domain/workflow/`): `Workflow`, `WorkflowCondition`, `WorkflowAction`,
  `WorkflowExecution`, enums (`TriggerEvent`, `ActionType`, `ExecutionStatus`,
  `ConditionOperator`) e `event/WorkflowTriggerEvent`.
- **Aplicação** (`application/workflow/`): `WorkflowService` (UseCase CRUD + activate/deactivate +
  listExecutions), `WorkflowExecutor`, `WorkflowConditionEvaluator`, `WorkflowActionRunner`,
  `WorkflowTemplateSeeder` (seeds por empresa).
- **Persistência**: JPA entities/repos com RLS; `WorkflowExecutionJpaEntity` com chave única
  `(company_id, workflow_action_id, event_id)` e insert nativo `ON CONFLICT DO NOTHING`
  (idempotência atômica).
- **Migrações**: `V041__workflow_tables.sql` (tabelas + RLS + índices + chave idempotência),
  `V042__workflow_permissions.sql` (permissões workflow:*).
- **Infraestrutura**: `WorkflowEventListener` (transacional), `WorkflowController`
  (CRUD + toggle + executions, `@PreAuthorize` por permissão).
- **Publicadores de eventos** (alterados): `OpportunityService` (created/stage_changed/won/lost),
  `TaskService` (created/completed), `ActivityService` (created).
- **Permissões** (`RoleSeedService`): SUPER_ADMIN e MANAGER → CRUD; AGENT e VIEWER → leitura.
- **Frontend** (`features/workflows/`): types, service, hooks, schemas, `WorkflowForm`,
  `WorkflowTable`, `WorkflowExecutionsPanel`, `DeleteWorkflowDialog`; rotas `/workflows`
  (lista/new/[id]/[id]/edit) e item no menu CRM.

### Fixed
- Guard de recursão por `eventId` + idempotência: ações que re-disparam o trigger não re-executam.

### Qualidade
- Backend: 315 testes unitários passando (BUILD SUCCESS).
- Frontend: `tsc` limpo (código novo), lint sem warnings, 128 testes passando.
- `WorkflowIsolationIT` (Testcontainers) para RLS + idempotência em Postgres real (CI).

### Documentação
- `sprints/13/REPORT.md`, `docs/WORKFLOW_AUTOMATION.md`, `docs/CHANGELOG.md`,
  `docs/PROJECT_INDEX.md`.

## [4.3.0] - 2026-07-15 — Sprint 4.3: Login (Review + Close)

### Added
- **Events (3)**
  - `domain/identity/event/TokenRefreshedEvent.java`
  - `domain/identity/event/PasswordResetRequestedEvent.java`
  - `domain/identity/event/UserLoggedOutEvent.java`
- **Security (1)**
  - `infrastructure/security/filter/JwtUserPrincipal.java` — Record com userId + companyId

### Changed
- `SecurityConfig.java` — Rotas protegidas + JwtAuthenticationFilter adicionado ao chain
- `JwtAuthenticationFilter.java` — Validação de token JWT funcional (validateToken, extractUserId, set SecurityContext)
- `AuthController.java` — @AuthenticationPrincipal no lugar de UUID.randomUUID(); /me endpoint; logout/changePassword corrigidos
- `AuthService.java` — @Service; changePassword agora persiste a senha; login/refresh tokens com hardcoded roles (ADR-012)
- `UserService.java` — @Service
- `UserRepository.java` — findAllByCompanyId adicionado
- `UserRepositoryImpl.java` — findAllByCompanyId implementado
- `SpringDataUserRepository.java` — findByCompanyId adicionado

### Fixed
- `AuthController.java` — Unused `java.util.UUID` import removed
- `UserService.java` — `java.util.stream.Collectors` import restored
- `AuthService.logout()` — Evento agora usa companyId correto (não userId)
- `AuthService.changePassword()` — Senha agora é salva no banco

### Review
- Sprint 4.3C — Revisão completa: nota 93/100
- Problemas: 3 baixa complexidade (corrigidos)
- ADRs novos: ADR-012 (Roles Hardcoded), ADR-013 (Password Reset Placeholder)
- Resultado: ✅ Sprint 4.3 APROVADA

### Documentação
- `docs/ARCHITECTURE_DECISIONS.md` — ADR-012, ADR-013
- `.ai/` — 10 arquivos atualizados
- `sprints/4.3/` — REVIEW, RETROSPECTIVE, REPORT

## [4.1.1] - 2026-07-15 — Sprint 4.1B + 4.1C: Desenvolvimento + Review

### Added
- **Security Infrastructure (7 arquivos)**
  - `infrastructure/security/config/SecurityConfig.java` — SecurityFilterChain, CORS, PasswordEncoder, AuthenticationManager
  - `infrastructure/security/config/JwtProperties.java` — @ConfigurationProperties JWT
  - `infrastructure/security/config/JwtAuthenticationEntryPoint.java` — 401 JSON handler
  - `infrastructure/security/config/JwtAccessDeniedHandler.java` — 403 JSON handler
  - `infrastructure/security/filter/JwtAuthenticationFilter.java` — Skeleton (estrutura)
  - `infrastructure/config/web/OpenApiConfig.java` — OpenAPI com Bearer JWT security scheme
  - `presentation/rest/handler/GlobalExceptionHandler.java` — @RestControllerAdvice

### Changed
- `application.yml` — Adicionado jwt.secret, jwt.access-token-expiry, jwt.refresh-token-expiry

### Fixed
- `JwtAuthenticationEntryPoint.java` — ObjectMapper injetado via construtor (removido static)
- `JwtAccessDeniedHandler.java` — ObjectMapper injetado via construtor (removido static)
- `GlobalExceptionHandler.java` — Adicionados handlers 401 (AuthenticationException) e 403 (AccessDeniedException)

### Review
- Sprint 4.1C — Revisão completa: nota 93/100
- Problemas: 3 médios (corrigidos)
- Resultado: ✅ Sprint 4.1 APROVADA

## [4.1.0] - 2026-07-15 — Sprint 4.1: Infraestrutura Auth

### Added
- **Domain Layer (14 arquivos)**
  - `domain/identity/User.java` — Entidade usuário com factory method
  - `domain/identity/RefreshToken.java` — Entidade refresh token com lifecycle
  - `domain/identity/Role.java` — Entidade role com permissões
  - `domain/identity/UserRole.java` — Relação usuário-role
  - `domain/identity/valueobject/Email.java` — Value object email com validação
  - `domain/identity/valueobject/Password.java` — Value object password com regex
  - `domain/identity/valueobject/Token.java` — Value object token
  - `domain/identity/valueobject/RoleName.java` — Enum roles (SUPER_ADMIN, ADMIN, MANAGER, AGENT, VIEWER)
  - `domain/identity/event/UserCreatedEvent.java` — Evento usuário criado
  - `domain/identity/event/UserLoggedInEvent.java` — Evento login
  - `domain/identity/event/PasswordChangedEvent.java` — Evento senha alterada
  - `domain/identity/exception/InvalidCredentialsException.java` — Exceção credenciais inválidas
  - `domain/identity/exception/TokenExpiredException.java` — Exceção token expirado
  - `domain/identity/exception/UserNotFoundException.java` — Exceção usuário não encontrado

- **Application Layer (19 arquivos)**
  - `application/identity/port/input/AuthUseCase.java` — Porta de entrada auth
  - `application/identity/port/input/UserUseCase.java` — Porta de entrada users
  - `application/identity/port/output/UserRepository.java` — Porta saída usuários
  - `application/identity/port/output/RefreshTokenRepository.java` — Porta saída refresh tokens
  - `application/identity/port/output/RoleRepository.java` — Porta saída roles
  - `application/identity/port/output/PasswordEncoder.java` — Porta saída senha
  - `application/identity/port/output/JwtProvider.java` — Porta saída JWT
  - `application/identity/port/output/EventPublisher.java` — Porta saída eventos
  - `application/identity/service/AuthService.java` — Serviço auth
  - `application/identity/service/UserService.java` — Serviço users
  - 9 DTOs (LoginRequest, LoginResponse, RefreshTokenRequest, RegisterRequest, UserResponse, UpdateUserRequest, ForgotPasswordRequest, ResetPasswordRequest, ChangePasswordRequest)

- **Infrastructure Layer (12 arquivos)**
  - `infrastructure/identity/persistence/UserJpaEntity.java` — JPA entity users
  - `infrastructure/identity/persistence/RefreshTokenJpaEntity.java` — JPA entity refresh tokens
  - `infrastructure/identity/persistence/RoleJpaEntity.java` — JPA entity roles
  - `infrastructure/identity/persistence/SpringDataUserRepository.java` — Repository users
  - `infrastructure/identity/persistence/SpringDataRefreshTokenRepository.java` — Repository refresh tokens
  - `infrastructure/identity/persistence/SpringDataRoleRepository.java` — Repository roles
  - `infrastructure/identity/persistence/UserRepositoryImpl.java` — Implementação repository users
  - `infrastructure/identity/persistence/RefreshTokenRepositoryImpl.java` — Implementação repository refresh tokens
  - `infrastructure/identity/persistence/RoleRepositoryImpl.java` — Implementação repository roles
  - `infrastructure/identity/persistence/UserMapper.java` — Mapper users
  - `infrastructure/identity/security/BcryptPasswordEncoder.java` — BCrypt encoder (strength 12)
  - `infrastructure/identity/security/JwtTokenProvider.java` — JWT provider (jjwt library)

- **Presentation Layer (5 arquivos)**
  - `presentation/rest/identity/AuthController.java` — Controller auth (login, refresh, logout, register, forgot-password, reset-password, change-password)
  - `presentation/rest/identity/UserController.java` — Controller users (CRUD)
  - `presentation/rest/identity/dto/LoginRequestDto.java` — DTO login
  - `presentation/rest/identity/dto/RegisterRequestDto.java` — DTO registro
  - `presentation/rest/identity/dto/UserResponseDto.java` — DTO resposta

- **Database (1 arquivo)**
  - `db/migration/V002__auth_tables.sql` — Migration (users, roles, user_roles, refresh_tokens)

## [3.2.1] - 2026-07-15 — Sprint 3.2: AI Runtime Layer (Atualização)

### Changed
- `.ai/IMPLEMENTATION_QUEUE.md`: Transformado em Gerenciador Mestre de Execução
  - Adicionada visão geral das sprints em formato de tabela
  - Adicionado agrupamento por fases (8 fases)
  - Adicionada matriz de dependências visual
  - Adicionados critérios obrigatórios de conclusão
  - Adicionado bloco "Próxima Sprint Automática" com detalhes completos
  - Adicionados status padronizados (⏳ 🚧 ✅ ⛔ ❌)
  - Adicionadas métricas de progresso
  - Adicionado histórico de alterações

- `.ai/PROJECT_STATUS.md`: Atualizado com novo progresso
  - Adicionada seção "Próxima Sprint"
  - Adicionada tabela de progresso por fase
  - Atualizado contador de arquivos .ai/ (18 → 19)

- `.ai/NEXT_STEPS.md`: Reescrito com Sprint 4.1 completa
  - Adicionados arquivos que serão criados (árvore de diretórios)
  - Adicionado checklist detalhado (Domain, Application, Infrastructure, Presentation, Database, Testes, Documentação)
  - Adicionados arquivos proibidos
  - Adicionadas sprints seguintes (4.2, 4.3)

## [3.2.0] - 2026-07-15 — Sprint 3.2: AI Runtime Layer

### Added
- **AI Runtime Layer (.ai/)**
  - `.ai/README.md`: Visão geral e como usar a memória do projeto
  - `.ai/LAST_SESSION.md`: Última sessão de trabalho (PRIMEIRO a ler)
  - `.ai/CURRENT_SPRINT.md`: Sprint atual e checklist
  - `.ai/CURRENT_MODULE.md`: Módulo atual
  - `.ai/CURRENT_TASK.md`: Tarefa atual com arquivos proibidos
  - `.ai/CURRENT_DECISIONS.md`: 8 decisões de arquitetura
  - `.ai/KNOWN_ISSUES.md`: Problemas conhecidos e limitações
  - `.ai/NEXT_STEPS.md`: Próximos passos detalhados (Sprint 4.1)
  - `.ai/WORKLOG.md`: Diário cronológico de trabalho
  - `.ai/PROJECT_TIMELINE.md`: Linha do tempo com 16 sprints
  - `.ai/PROJECT_STATUS.md`: Status geral por camada
  - `.ai/PROJECT_STRUCTURE.md`: Estrutura de pastas do projeto
  - `.ai/ACTIVE_DEPENDENCIES.md`: Dependências ativas backend/frontend
  - `.ai/OPEN_TASKS.md`: 16 tarefas abertas com prioridades
  - `.ai/BLOCKERS.md`: 4 riscos identificados
  - `.ai/IMPLEMENTATION_QUEUE.md`: Fila oficial de 16 sprints
  - `.ai/SESSION_TEMPLATE.md`: Modelo para novas sessões
  - `.ai/AI_MEMORY_RULES.md`: 10 regras obrigatórias para agentes IA
  - `.ai/AI_RUNTIME_REPORT.md`: Relatório completo da camada

### Documentation
- Fluxo de leitura: LAST_SESSION → CURRENT_SPRINT → CURRENT_MODULE → CURRENT_TASK → AI_ROUTER → Context → Playbook → Docs → Code → Update
- Redução de contexto estimada: ~95% para retomar sessão

---

## [3.0.0] - 2026-07-15 — Sprint 3.1: Camada de Conhecimento para IA

### Added
- **Knowledge Layer**
  - `docs-ai/README.md`: Visão geral e como usar a camada de conhecimento
  - `docs-ai/AI_RULES.md`: 7 regras permanentes para agentes IA
  - `docs-ai/AI_ROUTER.md`: Roteador central - mapeia 26 módulos
  - `docs-ai/MODULE_INDEX.md`: Índice de todos os módulos com links
  - `docs-ai/SEARCH_INDEX.md`: Índice pesquisável com metadados completos
  - `docs-ai/TASK_INDEX.md`: Tarefas comuns mapeadas a módulos
  - `docs-ai/BACKEND_INDEX.md`: Todos os módulos backend
  - `docs-ai/FRONTEND_INDEX.md`: Todos os módulos frontend
  - `docs-ai/DATABASE_INDEX.md`: Todos os documentos de banco de dados
  - `docs-ai/API_INDEX.md`: Endpoints REST por módulo
  - `docs-ai/RULES_INDEX.md`: Regras de negócio por módulo
  - `docs-ai/DEPENDENCIES_INDEX.md`: Mapa de dependências entre módulos
  - `docs-ai/CHANGE_POLICY.md`: Política obrigatória de atualização documental
  - `docs-ai/DOCUMENTATION_POLICY.md`: Padrões de documentação
  - `docs-ai/PROMPT_GUIDE.md`: Guia de uso de prompts
  - `docs-ai/DECISION_TREE.md`: Árvore de decisão em 10 passos
  - `docs-ai/IMPLEMENTATION_GUIDE.md`: Fluxo oficial de implementação em 10 passos

- **Contexts (21 arquivos)**
  - `contexts/auth.context.md`: JWT + RBAC
  - `contexts/tenant.context.md`: Multi-tenancy
  - `contexts/user.context.md`: Usuários + Convite
  - `contexts/contact.context.md`: Contatos + Tags
  - `contexts/lead.context.md`: Lead Scoring
  - `contexts/customer.context.md`: Ciclo de Vida Cliente
  - `contexts/pipeline.context.md`: Pipeline + Estágios
  - `contexts/kanban.context.md`: Board Drag-and-drop
  - `contexts/conversation.context.md`: Conversas
  - `contexts/message.context.md`: Mensagens
  - `contexts/whatsapp.context.md`: Integração WhatsApp
  - `contexts/campaign.context.md`: Campanhas
  - `contexts/automation.context.md`: Automações
  - `contexts/notification.context.md`: Notificações
  - `contexts/dashboard.context.md`: KPIs do Dashboard
  - `contexts/report.context.md`: Relatórios + Exportação
  - `contexts/ai.context.md`: IA/Chatbot
  - `contexts/database.context.md`: Schema PostgreSQL
  - `contexts/backend.context.md`: Clean Architecture
  - `contexts/frontend.context.md`: Next.js + React
  - `contexts/event.context.md`: Eventos RabbitMQ

- **Playbooks (12 arquivos)**
  - `playbooks/implement-auth.md`: Playbook de implementação Auth
  - `playbooks/implement-users.md`: Playbook de implementação Users
  - `playbooks/implement-company.md`: Playbook de implementação Company
  - `playbooks/implement-contact.md`: Playbook de implementação Contact
  - `playbooks/implement-lead.md`: Playbook de implementação Lead
  - `playbooks/implement-pipeline.md`: Playbook de implementação Pipeline
  - `playbooks/implement-chat.md`: Playbook de implementação Chat
  - `playbooks/implement-whatsapp.md`: Playbook de implementação WhatsApp
  - `playbooks/implement-dashboard.md`: Playbook de implementação Dashboard
  - `playbooks/implement-report.md`: Playbook de implementação Report
  - `playbooks/implement-automation.md`: Playbook de implementação Automation
  - `playbooks/implement-ai.md`: Playbook de implementação AI

- **Prompts (11 arquivos)**
  - `prompts/backend.prompt.md`: Prompt para código Java backend
  - `prompts/frontend.prompt.md`: Prompt para componentes React/Next.js
  - `prompts/database.prompt.md`: Prompt para migrações Flyway + JPA
  - `prompts/api.prompt.md`: Prompt para endpoints REST
  - `prompts/documentation.prompt.md`: Prompt para atualização de documentação
  - `prompts/review.prompt.md`: Prompt para code review
  - `prompts/bugfix.prompt.md`: Prompt para correção de bugs
  - `prompts/refactor.prompt.md`: Prompt para refatoração
  - `prompts/testing.prompt.md`: Prompt para testes
  - `prompts/deployment.prompt.md`: Prompt para deploy
  - `prompts/feature.prompt.md`: Prompt para feature completa

### Documentation
- `docs-ai/KNOWLEDGE_LAYER_REPORT.md`: Relatório completo da Camada de Conhecimento
- Cobertura: 26 módulos, 21 contexts, 12 playbooks, 11 prompts
- Fluxo de utilização: AI_ROUTER → Context → Playbook → Official Docs → Code

---

## [2.0.0] - 2026-07-15 — Sprint 1: Fundação do Projeto

### Added
- **Backend Foundation**
  - `backend/pom.xml`: Maven project with Spring Boot 3.4.1, Java 21, all required dependencies
  - `backend/src/main/java/com/becommerce/crm/CrmApplication.java`: Spring Boot main class
  - `backend/src/main/resources/application.yml`: Main configuration
  - `backend/src/main/resources/application-dev.yml`: Development profile
  - `backend/src/main/resources/application-prod.yml`: Production profile
  - `backend/src/test/resources/application-test.yml`: Test profile
  - `backend/src/main/resources/db/migration/V001__initial_schema.sql`: Initial Flyway migration
  - `backend/Dockerfile`: Multi-stage Docker build
  - `backend/.dockerignore`: Docker ignore rules
  - `backend/.gitignore`: Git ignore rules
  - `backend/.mvn/wrapper/maven-wrapper.properties`: Maven wrapper configuration
  - `backend/README.md`: Backend documentation
  - Clean Architecture directory structure (domain, application, infrastructure, presentation, shared)

- **Frontend Foundation**
  - `frontend/package.json`: Next.js 14 project with all required dependencies
  - `frontend/tsconfig.json`: TypeScript configuration
  - `frontend/next.config.js`: Next.js configuration
  - `frontend/tailwind.config.ts`: Tailwind CSS configuration with Shadcn UI theme
  - `frontend/postcss.config.js`: PostCSS configuration
  - `frontend/.env.local`: Environment variables
  - `frontend/.eslintrc.json`: ESLint configuration
  - `frontend/.prettierrc`: Prettier configuration
  - `frontend/.prettierignore`: Prettier ignore rules
  - `frontend/.editorconfig`: EditorConfig configuration
  - `frontend/.gitignore`: Git ignore rules
  - `frontend/.dockerignore`: Docker ignore rules
  - `frontend/Dockerfile`: Multi-stage Docker build
  - `frontend/README.md`: Frontend documentation
  - `frontend/src/app/layout.tsx`: Root layout with providers
  - `frontend/src/app/page.tsx`: Home page
  - `frontend/src/app/not-found.tsx`: 404 page
  - `frontend/src/styles/globals.css`: Global styles with CSS variables
  - `frontend/src/providers/index.tsx`: Root providers component
  - `frontend/src/providers/QueryProvider.tsx`: React Query provider
  - `frontend/src/providers/ThemeProvider.tsx`: Theme provider
  - `frontend/src/providers/AuthProvider.tsx`: Authentication context
  - `frontend/src/lib/api.ts`: Axios API client
  - `frontend/src/lib/utils.ts`: Utility functions (cn)
  - `frontend/src/lib/validations.ts`: Zod validation schemas
  - `frontend/src/lib/constants.ts`: Application constants
  - `frontend/src/types/models.ts`: TypeScript models
  - `frontend/src/types/api.ts`: API types
  - `frontend/src/types/index.ts`: Types barrel export
  - Feature-based directory structure (app, components, hooks, lib, providers, types, styles)

- **Docker Configuration**
  - `docker/docker-compose.yml`: Full stack compose (backend, frontend, postgres, redis, rabbitmq, minio)
  - `docker/docker-compose.dev.yml`: Development services only
  - `docker/docker-compose.prod.yml`: Production configuration with resource limits
  - `docker/README.md`: Docker documentation

- **Infrastructure**
  - `infra/README.md`: Infrastructure placeholder documentation

- **Scripts**
  - `scripts/setup.sh`: Development environment setup script
  - `scripts/seed.sh`: Database seeding script
  - `scripts/deploy.sh`: Production deployment script
  - `scripts/README.md`: Scripts documentation

- **CI/CD**
  - `.github/workflows/ci.yml`: CI pipeline (backend, frontend, docker build)
  - `.github/workflows/cd.yml`: CD pipeline (build, push, deploy)

- **Root Configuration**
  - `.gitignore`: Root gitignore

### Changed
- `docs/CHANGELOG.md`: Updated with Sprint 1 information

### Documentation
- All documentation in `docs/` remains unchanged as per project rules
- Implementation reports generated in `backend/IMPLEMENTATION_REPORT.md` and `frontend/IMPLEMENTATION_REPORT.md`

---

## [1.1.0] - 2026-07-15

### Fixed
- Reports.md: Rebuilt corrupted endpoints section
- Dashboard.md: Removed circular dependency with Reports.md
- Reports.md: Removed self-reference and circular dependency
- integrations/README.md: Fixed broken reference to non-existent file
- Lead.md: Fixed Chinese text "业务需求" replaced with Portuguese
- Routing.md: Removed /register route (no backend endpoint)
- Kanban.md: Replaced deprecated react-beautiful-dnd with @dnd-kit/core
- Context.md: Fixed WebSocketProvider and NotificationProvider locations
- Overview.md (frontend): Removed unused Zustand from stack
- Auth.md: Fixed broken reference to non-existent 11-security/ directory
- SECURITY_MAP.md: Fixed broken reference to 01-backend/Permissions.md
- AI.md: Fixed misleading link text [Communication.md] pointing to Conversations.md
- Customers.md: Fixed misleading link text [Communication.md] pointing to Conversations.md
- Contacts.md: Fixed misleading link text [Communication.md] pointing to Conversations.md
- PROJECT_INDEX.md: Corrected file counts, added Security.md, Permissions.md, REVIEW.md
- BACKEND_MAP.md: Replaced Chinese "常量" with "constantes"
- DATABASE_MAP.md: Replaced Chinese "隔离" (2x) with "isolation"
- 00-core/Security.md: Replaced Chinese "最小权限" with "menor privilégio"
- 00-core/Vision.md: Replaced Chinese "部署" with "deploy"
- SUMMARY.md: Removed Zustand from frontend stack (replaced with React Context)
- 02-frontend/Hooks.md: Updated state management guidance (removed Zustand)
- FILE_LIFECYCLE.md: Fixed invalid Mermaid node syntax
- DATA_FLOW.md: Added missing `participant RD as Redis` declaration
- 03-database/Entities.md: Added 8 missing ERD tables (company_settings, tags, contact_tags, pipelines, stages, opportunity_history, conversations, audit_logs)
- 01-backend/README.md: Added Permissions.md to table of contents
- 35 files: Fixed 55 broken TOC anchor links (Unicode normalization + missing headings)
- 8 empty directories removed (08-history through 15-automation-docs)

### Added
- Lead.md: Complete scoring formula with weights and examples
- Automations.md: Complete condition system with operators and examples
- WhatsApp.md: Template Messages documentation with categories, variables, buttons, rate limits
- Overview.md (backend): CORS configuration, security headers, HTTPS rules
- Reports.md: Numbered rules format (R-001 to R-006)
- Automations.md: Numbered rules format (A-001 to A-011)
- 00-core/Security.md: Complete security guidelines (OWASP, LGPD, encryption, RBAC)
- 01-backend/Permissions.md: RBAC backend with roles, permissions matrix, hierarchy
- 03-database/Entities.md: 11 missing tables (message_templates, message_attachments, analytics_events, leads, campaigns, campaign_steps, automation_triggers, automation_actions, roles, user_roles, subscriptions, contact_addresses, contact_custom_fields, events)
- 06-devops/Docker.md: RabbitMQ HA (cluster, quorum queues) and Redis HA (sentinel, cluster mode)

### Changed
- Dashboard.md: Changed dependency from Reports.md to Events.md and Cache.md
- Reports.md: Changed dependencies to Cache.md, FileStorage.md, Scheduler.md, Events.md
- Context.md: Moved WebSocketProvider and NotificationProvider to dedicated provider files

### New Documents
- DOMAIN_MODEL.md
- EVENT_MAP.md
- WORKFLOWS.md
- STATE_MACHINES.md
- MULTI_TENANCY.md
- BILLING_MODEL.md
- FEATURE_FLAGS.md
- CACHE_STRATEGY.md
- QUEUE_ARCHITECTURE.md
- WEBSOCKET_ARCHITECTURE.md
- SEARCH_ARCHITECTURE.md
- OBSERVABILITY.md
- BACKUP_RECOVERY.md
- API_VERSIONING.md
- ERROR_HANDLING.md
- NOTIFICATION_ARCHITECTURE.md
- SCHEDULER.md
- FILE_LIFECYCLE.md
- LGPD.md
- CHANGELOG.md
