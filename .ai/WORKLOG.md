# Diário de Trabalho

> ⚠️ **ATENÇÃO — numeração histórica.** Este diário registra o trabalho conforme foi feito, usando a
> numeração **vigente na época de cada entrada**, que **nem sempre coincide com a numeração canônica
> atual** (ex.: a entrada "Sprint 13 — Customer 360" data de antes de a Automação de Workflows ser
> fixada como Sprint 13). Não tratar estes números como fonte de verdade.
>
> **Fonte de verdade atual: [`sprints/SPRINT_INDEX.md`](../sprints/SPRINT_INDEX.md).**
> Sprints reais: CRM 9–15 (Automação de Workflows em 13–15), Omnichannel 16–18, Analytics 19, IA 20.

## Sprint 0 — Planejamento
- **Data:** 2026-07-15
- **Objetivo:** Definir arquitetura, stack tecnológica, estrutura do projeto
- **Arquivos Alterados:** docs/* (43+ arquivos)
- **Tempo Estimado:** 4h
- **Resultado:** Documentação completa do projeto
- **Pendências:** Nenhuma

## Sprint 1 — Fundação do Projeto
- **Data:** 2026-07-15
- **Objetivo:** Criar fundação backend, frontend, Docker, CI/CD, scripts
- **Arquivos Alterados:** backend/*, frontend/*, docker/*, scripts/*, .github/*
- **Tempo Estimado:** 6h
- **Resultado:** Estrutura completa de projeto
- **Pendências:** Nenhuma

## Sprint 2 — Correções e Melhorias
- **Data:** 2026-07-15
- **Objetivo:** Corrigir broken links, remover diretórios vazios, adicionar documentação
- **Arquivos Alterados:** docs/* (35 arquivos corrigidos, 16 novos)
- **Tempo Estimado:** 2h
- **Resultado:** Documentação íntegra
- **Pendências:** Nenhuma

## Sprint 3.1 — Knowledge Layer
- **Data:** 2026-07-15
- **Objetivo:** Criar camada de navegação para agentes IA
- **Arquivos Alterados:** docs-ai/*, contexts/*, playbooks/*, prompts/*
- **Tempo Estimado:** 3h
- **Resultado:** 61 arquivos de navegação
- **Pendências:** Nenhuma

## Sprint 3.2 — AI Runtime Layer
- **Data:** 2026-07-15
- **Objetivo:** Criar memória persistente do projeto
- **Arquivos Alterados:** .ai/* (19 arquivos)
- **Tempo Estimado:** 2h
- **Resultado:** Camada de memória completa
- **Pendências:** Nenhuma

## Sprint 3.3 — Sprint Management Layer
- **Data:** 2026-07-15
- **Objetivo:** Criar camada de gerenciamento de sprints
- **Arquivos Alterados:** sprints/* (13 arquivos)
- **Tempo Estimado:** 2h
- **Resultado:** Camada de gerenciamento completa
- **Pendências:** Nenhuma

## Sprint 4.1A — Planejamento Auth
- **Data:** 2026-07-15
- **Objetivo:** Planejamento formal da Sprint 4.1, validação de documentação, gap analysis
- **Arquivos Alterados:** sprints/4.1/* (5 arquivos), .ai/* (6 arquivos)
- **Tempo Estimado:** 1h
- **Resultado:** Planejamento concluído, pendências registradas para 4.1B
- **Pendências:** 7 itens identificados para 4.1B

## Sprint 4.1 — Infraestrutura Auth
- **Data:** 2026-07-15
- **Objetivo:** Implementar domain entities, application services, infrastructure, presentation, migration para Auth
- **Arquivos Alterados:** backend/src/main/java/com/becommerce/crm/domain/identity/ (14), application/identity/ (19), infrastructure/identity/ (12), presentation/rest/identity/ (5), db/migration/ (1)
- **Tempo Estimado:** 4h
- **Resultado:** 51 arquivos Java + 1 migration criados
- **Pendências:** Testes unitários, testes de integração, lint

## Sprint 4.1B — Desenvolvimento (Infraestrutura Spring Security + JWT + Exception Handling)
- **Data:** 2026-07-15
- **Objetivo:** Implementar infraestrutura técnica do módulo Auth
- **Arquivos Criados:**
  - `infrastructure/security/config/SecurityConfig.java`
  - `infrastructure/security/config/JwtProperties.java`
  - `infrastructure/security/config/JwtAuthenticationEntryPoint.java`
  - `infrastructure/security/config/JwtAccessDeniedHandler.java`
  - `infrastructure/security/filter/JwtAuthenticationFilter.java`
  - `infrastructure/config/web/OpenApiConfig.java`
  - `presentation/rest/handler/GlobalExceptionHandler.java`
- **Arquivos Modificados:** `application.yml`, `.ai/*` (8 arquivos)
- **Tempo Estimado:** 2h
- **Resultado:** 7 novos arquivos de infraestrutura criados
- **Pendências:** Verificar compilação (mvn compile), testes

## Sprint 4.1C — Review (Revisão Técnica)
- **Data:** 2026-07-15
- **Objetivo:** Revisar 7 arquivos da Sprint 4.1B + application.yml
- **Arquivos Revisados:** SecurityConfig, JwtProperties, JwtAuthenticationEntryPoint, JwtAccessDeniedHandler, JwtAuthenticationFilter, OpenApiConfig, GlobalExceptionHandler, application.yml
- **Correções:** 3 (ObjectMapper injection, @Component removal, 401/403 handlers)
- **Nota:** 93/100 — Aprovada
- **Resultado:** Sprint 4.1 APROVADA

## Sprint 4.1D — Close (Encerramento)
- **Data:** 2026-07-15
- **Objetivo:** Encerrar oficialmente a Sprint 4.1, consolidar documentação, preparar Sprint 4.2
- **Arquivos Atualizados:** CHANGELOG.md, PROJECT_STATUS.md, PROJECT_TIMELINE.md, OPEN_TASKS.md, IMPLEMENTATION_QUEUE.md, CURRENT_SPRINT.md, CURRENT_MODULE.md, CURRENT_TASK.md, NEXT_STEPS.md, RETROSPECTIVE.md, REPORT.md
- **Tempo Estimado:** 1h
- **Resultado:** Sprint 4.1 encerrada — todas as 4 fases concluídas (Planning ✅, Development ✅, Review ✅, Close ✅)
- **Pendências:** Nenhuma

## Sprint 4.2 — User Identity Foundation (Permission, RolePermission)
- **Data:** 2026-07-15
- **Objetivo:** Criar entidades de domínio Permission e RolePermission, repositórios, migration, milestone
- **Arquivos Criados:** Permission.java, RolePermission.java, PermissionRepository.java, RolePermissionRepository.java, PermissionJpaEntity.java, RolePermissionJpaEntity.java, SpringDataPermissionRepository.java, SpringDataRolePermissionRepository.java, PermissionMapper.java, PermissionRepositoryImpl.java, RolePermissionRepositoryImpl.java, V003__rbac_tables.sql, M2 - Authentication Foundation.md
- **Tempo Estimado:** 1h
- **Resultado:** 12 arquivos criados, fundação RBAC estabelecida
- **Pendências:** Review + Close

## Sprint 4.3 — Login (Development)
- **Data:** 2026-07-15
- **Objetivo:** Implementar JWT funcional, refresh token rotation, /me, logout, corrigir bugs
- **Arquivos Criados:** TokenRefreshedEvent.java, PasswordResetRequestedEvent.java, UserLoggedOutEvent.java, JwtUserPrincipal.java
- **Arquivos Modificados:** AuthService.java (@Service, bug fix), UserService.java (@Service), UserRepository.java (+findAllByCompanyId), UserRepositoryImpl.java, SpringDataUserRepository.java, SecurityConfig.java (rotas protegidas + filter), JwtAuthenticationFilter.java (validação), AuthController.java (fix logout/changePassword, /me)
- **Correções no Review:** 3 (import não utilizado, import removido incorretamente, logout event companyId)
- **Tempo Estimado:** 2h
- **Resultado:** Login funcional com JWT, /me, logout, refresh token
- **Pendências:** mvn compile, testes (Sprint 4.5)

## Sprint 4.3C — Review (Revisão Técnica)
- **Data:** 2026-07-15
- **Objetivo:** Revisar 8+ arquivos da Sprint 4.3
- **Arquivos Revisados:** AuthService, UserService, AuthController, SecurityConfig, JwtAuthenticationFilter, JwtUserPrincipal, UserRepository, UserRepositoryImpl, SpringDataUserRepository
- **Correções:** 3 (import removido do AuthController, import restaurado no UserService, logout companyId fix)
- **Novos ADRs:** ADR-012 (Roles Hardcoded), ADR-013 (Password Reset Placeholder)
- **Nota:** 93/100 — Aprovada

## Sprint 4.3D — Close (Encerramento)
- **Data:** 2026-07-15
- **Objetivo:** Encerrar oficialmente a Sprint 4.3, atualizar documentação, preparar Sprint 4.4
- **Arquivos Atualizados:** +15 (CHANGELOG, .ai/*, sprints/4.3/*, milestone M2, ARCHITECTURE_DECISIONS, IMPLEMENTATION_REPORT)
- **Tempo Estimado:** 1h
- **Resultado:** Sprint 4.3 encerrada — 4 fases concluídas (Development ✅, Review ✅, Close ✅)
- **Pendências:** Nenhuma

## Sprint 13 — Customer 360 e Contexto Comercial
- **Data:** 2026-08-13
- **Objetivo:** Visão consolidada do contato — contexto comercial, tarefas, linha do tempo unificada e próxima ação recomendada (determinística, sem IA)
- **Backend:**
  - `application/customer360/` — DTOs + `Customer360Service` (agrega dados, `GET /companies/{companyId}/contacts/{contactId}/360`)
  - `presentation/rest/customer360/Customer360Controller` (permissão `contact:read`)
  - Diretório de contatos: `ContactRepository.findByCompanyIdActive` + `ContactUseCase.listByCompany` + `GET /contacts`
  - Consultas por contato: `OpportunityRepository.findByContactId`, `TaskRepository.findByContactId`, `ActivityRepository.findLatestActivityAtByContactId`
  - `GlobalExceptionHandler` — `ContactNotFoundException` → 404
- **Frontend:**
  - `features/contacts/` — `types`, `services/contact.service.ts`, `hooks/useContacts.ts`, componentes (`ContactTable`, `ContactSummaryCard`, `NextActionCard`, `OpportunitiesPanel`, `TasksPanel`, `TimelinePanel`, `CreateContactDialog`)
  - Rotas: `/contacts` (diretório) e `/contacts/[id]` (Customer 360) + ações rápidas (atividade/tarefa)
- **Testes:** backend +8 (Customer360Service/Controller); frontend +(hook + componentes). `mvn verify` 289 testes ✅; frontend 123 testes ✅
- **Resultado:** Sprint 13 implementada e testada

## Sprint 16 - Omnichannel · WhatsApp (base)
- **Data:** 2026-08-15
- **Objetivo:** Base do Omnichannel de WhatsApp — canais, inbox (chat) e webhook, com RLS FORCE.
- **Backend:**
  - `domain/omnichannel/` - `Channel`/`Conversation`/`Message` + enums (create/reconstitute/estados)
  - `application/omnichannel/` - `OmnichannelChannelService`, `OmnichannelInboxService`, `WhatsAppWebhookService`
  - Portas `Omnichannel{Channel,Conversation,Message}Repository`, `OmnichannelCompanyResolver`, `WhatsAppWebhookParser`, `WhatsAppProvider`
  - Infra: `WhatsAppCloudApiProvider`/parser (Meta) + `FakeWhatsAppProvider` (dev), repos RLS FORCE, seed `omnichannel:*`
  - Controllers `/api/v1/omnichannel/channels` + `/inbox`; migrações `V043__omnichannel_tables.sql`, `V044__omnichannel_permissions.sql`
- **Frontend:**
  - Feature `features/omnichannel/` (types/service/hooks/componentes) + páginas `/inbox` (lista + chat) e `/channels` (config)
- **Testes:** backend +33 (domínio + 3 services); typecheck/lint OK; build prod OK. IT `OmnichannelIsolationIT` p/ rodar na VPS.
- **Pendências:** deploy/VPS + IT Testcontainers + E2E manual.

---

*Última atualização: 2026-07-15*
