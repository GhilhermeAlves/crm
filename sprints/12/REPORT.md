# Sprint 12 — CRM Orientado à Ação (Activities, Tasks, Dashboard Operacional)

**Data:** 2026-08-13 · **Status:** ✅ Concluída · **Responsável:** AI Agent · **Dependência:** 11

## Resumo

Sprint de **CRM orientado à ação** dentro do modelo SaaS/RLS estabelecido. Construiu três
módulos de ponta a ponta (banco → backend → frontend) que respondem, de forma **determinística
(sem IA)**, à pergunta central "o que merece minha atenção hoje?":

- **Activities (Timeline)** — registro de interações/acontecimentos comerciais, com timeline
  unificada por contato, por oportunidade ou por empresa;
- **Tasks (Follow-up)** — tarefas com prioridade/status e transições de estado;
- **Dashboard operacional** — inteligência determinística do pipeline (oportunidades paradas,
  follow-up recomendado, priorização por score) + métricas orientadas à ação.

Preparação arquitetural para **IA/Inbox/Workflow** (Sprints 16/17): o vínculo contact/opportunity
de Activities e Tasks é **opcional e não-enumerado** (permite associar a outras entidades/eventos
do Inbox sem alterar o modelo). **Não** foram criadas abstrações vazias de IA/Workflow/Inbox —
apenas decisões de design documentadas.

## Visão geral da estrutura

- **Activity**: interação/acontecimento comercial (CALL, MEETING, EMAIL, MESSAGE, NOTE, PROPOSAL,
  FOLLOW_UP, OTHER). Pertence a uma empresa (RLS FORCE) e pode estar vinculada **opcionalmente** a
  um contact e/ou a uma opportunity da MESMA empresa (validado no serviço — defense-in-depth).
- **Task**: ação a executar com `priority` (LOW/MEDIUM/HIGH) e `status`
  (PENDING/IN_PROGRESS/COMPLETED/CANCELLED). Transições de estado protegidas
  (`markInProgress/complete/cancel/reopen`); tarefa **concluída não pode ser alterada**.
- **Dashboard operacional**: agrega oportunidades de atenção (score = valor + probabilidade do
  estágio + tempo parado), tarefas vencendo hoje e últimos registros de atividade.

## Banco de dados

- **Nova migration `V039__activities_tasks_tables.sql`**: tabelas `activities` (company_id,
  contact_id/opportunity_id nullable, type, subject, description, activity_at) e `tasks`
  (company_id, contact_id/opportunity_id nullable, title, description, assignee_id, due_at,
  priority, status, completed_at), CHECKs separadas, índices `idx_*` e **RLS FORCE +
  `tenant_isolation_policy`** (`company_id = app.current_tenant_id()`) para ambas.
  Grants NÃO incluídos manualmente — o loop dinâmico da V034 concede DML a `crm_app` para
  todas as tabelas do schema `public` automaticamente.
- **Nova migration `V040__activity_task_permissions.sql`**: permissões `activity:create/read/
  update/delete`, `task:create/read/update/delete` e `dashboard:operational`
  (`INSERT ... ON CONFLICT (name) DO NOTHING`). O vínculo papel → permissão é feito no startup
  pelo `RoleSeedService` (reexecutado a cada deploy para todos os tenants).

## Backend

Novo módulo espelhando o padrão `contact`/`pipeline`:

- **Activities** — `domain/activity/` (`Activity`, `ActivityType`, exceptions),
  `application/activity/` (DTOs `Create/Update/ActivityRequest` + `ActivityResponse`, portas
  `ActivityUseCase`/`ActivityRepository`, `ActivityService` com `TenantContext` isolado,
  validação de ownership de contact/opportunity e auditoria via `AuditModule.ACTIVITIES`),
  `infrastructure/activity/persistence/` (`ActivityJpaEntity`, `ActivityJpaRepository`,
  `ActivityRepositoryImpl`), `presentation/rest/activity/ActivityController` — endpoints
  `/api/v1/companies/{companyId}/activities`, `/recent`, `/contacts/{contactId}/activities`,
  `/opportunities/{opportunityId}/activities` com `@PreAuthorize('activity:*')` +
  `requireCompanyAccess`.
- **Tasks** — `domain/task/` (`Task`, `TaskStatus`, `TaskPriority`, exceptions),
  `application/task/` (DTOs, portas `TaskUseCase`/`TaskRepository`, `TaskService` com transições
  de estado + auditoria `AuditModule.TASKS`), `infrastructure/task/persistence/`
  (`TaskJpaEntity`, `TaskJpaRepository`, `TaskRepositoryImpl` com `countPending` + `findDueToday`),
  `presentation/rest/task/TaskController` — endpoints `/tasks`, `/due-today`,
  `/opportunities/{id}/tasks`, `/tasks/{id}/status/{status}` com `@PreAuthorize('task:*')`.
- **Dashboard / inteligência (ITEMS 3 e 4)** — `application/dashboard/` (`OperationalDashboard`,
  `AttentionOpportunity` DTOs + `DashboardService`): agrega oportunidades abertas da empresa,
  calcula `daysInactive` (última atividade via `ActivityRepository.findLatestActivityAtByOpportunityId`),
  marca como `stale` se 7+ dias sem contato, prioriza por score determinístico (valor + probabilidade
  do estágio + tempo parado), gera sugestão de follow-up e agrega tarefas vencendo hoje + atividades
  recentes. `presentation/rest/dashboard/DashboardController` — `GET /companies/{companyId}/
  dashboard/operational` com `@PreAuthorize('dashboard:operational')`.
- **Repositórios ampliados** para agregação por empresa: `OpportunityRepository.findByCompanyId`,
  `StageRepository.findByCompanyId`, `ActivityRepository.findLatestActivityAtByOpportunityId`
  (+ impls JPA).
- **Handlers**: `GlobalExceptionHandler` com 404/400 para `ActivityNotFoundException`,
  `ActivityValidationException`, `TaskNotFoundException`, `TaskValidationException`.

## Frontend

Novas features + páginas + evolução do dashboard:

- **Activities** — `features/activities/{types,services,hooks,schemas,components}`: tipos e rótulos
  pt-BR dos 8 tipos, `ActivityService` (list/recent/byContact/byOpportunity/CRUD), hooks React Query,
  schema Zod, `ActivityTimeline` (timeline vertical com ícone + badge do tipo) e
  `CreateActivityDialog` (form com datetime-local). Página `app/(dashboard)/activities` (Timeline).
- **Tasks** — `features/tasks/{types,services,hooks,schemas,components}`: `TaskService`
  (list/dueToday/byOpportunity/CRUD/changeStatus), hooks React Query, schema Zod, `TaskList`
  (ordenação por status+prioridade, ações iniciar/concluir/cancelar/reabrir com gating) e
  `CreateTaskDialog`. Página `app/(dashboard)/tasks` (Vencendo hoje + Todas).
- **Dashboard (ITEM 4)** — `features/dashboard/{types,services,hooks,components}`:
  `useOperationalDashboard` (gated por `dashboard:operational`) + `AttentionList`. Página
  `app/(dashboard)/dashboard/page.tsx` **reescrita** de estática/hardcoded para orientada à ação:
  saudação real, KPIs (para atenção, tarefas hoje, pipeline em aberto + valor, paradas), lista de
  atenção com sugestão, tarefas vencendo hoje (com transição de status) e atividades recentes.
- **Navegação** — `Sidebar`: grupo CRM ganha "Tarefas" (`task:read`) e "Timeline" (`activity:read`);
  `ROUTES` ganham `TASKS` e `ACTIVITIES`.

## Testes

> **Validado em 2026-08-13.** Contagens refletem execução real.

- **Backend: 281 testes PASS** (antes 267). Novos:
  - `ActivityServiceTest` (create com vínculos owned, rejeição de contact/opportunity de outra
    empresa, ownership de activity, update, list);
  - `TaskServiceTest` (create com prioridade default, rejeição de contact estrangeiro, ownership,
    complete, rejeição de update em concluída, due-today);
  - `DashboardServiceTest` (dashboard destacando oportunidade parada, contagem de tarefas de hoje
    excluindo concluídas);
  - `ActivityTaskIsolationIT` (Testcontainers PostgreSQL + RLS, mesmo fluxo de `LeadIsolationIT`/
    `PipelineIsolationIT`): isolamento cross-tenant real nas tabelas `activities`/`tasks`
    (select, insert-blocked, update/delete 0 rows, no-context 0 rows).
- **Frontend: 114 testes PASS** (antes 106). Novos:
  - `activity.schema.test.ts` (4): validação Zod do form de activity.
  - `task.schema.test.ts` (4): validação Zod do form de task.
- Typecheck OK (backend); frontend `tsc --noEmit` OK nos novos arquivos — erro pré-existente em
  `useLeads.test.ts` (Sprint 10, fora de escopo) mantido; lint sem erros novos.

## Build e integração

- Backend: `mvnw test` BUILD SUCCESS (281 testes), compile OK.
- Frontend: `tsc --noEmit` (novos arquivos OK), `next lint` OK, `vitest run` 114/114.
- Integração: `/api/v1/companies/{companyId}/activities*`, `.../tasks*`,
  `.../dashboard/operational` com RLS + `@PreAuthorize` + `requireCompanyAccess` +
  `TenantContext`; UI refletindo `activity:*`/`task:*`/`dashboard:operational`.

## E2E

- **E2E autenticado manual não realizado** (limitação herdada das sprints 8.x–11): fluxo real de
  criação de atividade/tarefa e dashboard no browser exige credenciais de teste não automatizáveis.
  Testes unitários de serviço + IT de RLS cobrem autorização/validação/isolamento; não inventado
  resultado de E2E.

## Produção / VPS

- **Deploy pendente desta etapa** (ver "Débitos"): migrations V039/V040 e novo build
  backend+frontend devem ser aplicados na VPS `crm-vps` via `docker compose build` + `up -d`
  (com `SPRING_FLYWAY_OUT_OF_ORDER=true` já configurado), seguido de smoke test (Flyway `v040`,
  backend 0 ERROR, rotas registradas, `/activities`, `/tasks` 307→login no frontend).

## Débitos

- **Deploy VPS pendente**: aplicar V039/V040 + rebuild na VPS `crm-vps` e validar em produção
  (smoke test conforme Sprint 11).
- **E2E autenticado manual** (herdado): fluxo real no browser sem credenciais.
- **Envio de e-mail real** (herdado): `ConsoleEmailSender` placeholder.
- **IA/Inbox/Workflow** (Sprints 16/17): modelo já preparado (contact/opportunity nullable em
  Activities/Tasks) — a ingestão de eventos do Inbox e o scoring/IA ficam para as sprints futuras;
  esta sprint entregou o CRM orientado à ação determinístico.

## Artefatos

- Migrations: `db/migration/V039__activities_tasks_tables.sql`, `V040__activity_task_permissions.sql`.
- Backend: `domain/activity/*`, `domain/task/*`, `application/activity/{dto,port,service}/*`,
  `application/task/{dto,port,service}/*`, `application/dashboard/{dto,service}/*`,
  `presentation/rest/{activity,task,dashboard}/*`, `infrastructure/{activity,task}/persistence/*`,
  `AuditModule.java` (ACTIVITIES), `RoleSeedService.java` (activity:* / task:* / dashboard:operational),
  `GlobalExceptionHandler.java`.
- Testes backend: `ActivityServiceTest.java`, `TaskServiceTest.java`, `DashboardServiceTest.java`,
  `ActivityTaskIsolationIT.java` (+ `activity-task-rls-bootstrap.sql`).
- Frontend: `features/activities/*`, `features/tasks/*`, `features/dashboard/*`,
  `app/(dashboard)/{dashboard,activities,tasks}/page.tsx`, `components/layout/Sidebar.tsx`,
  `lib/constants.ts`.
