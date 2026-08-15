# Sprint 14 - Workflows (Automação de Tarefas e Atividades)

**Data:** 2026-08-14 **Status:** Concluída **Responsável:** AI Agent **Dependência:** 12

## Resumo

Sprint de **automação de negócio** dentro do modelo SaaS/RLS estabelecido. Construiu um motor de
**workflows** de ponta a ponta (banco → backend → frontend) que responde, de forma **determinística
(sem IA)**, à pergunta "o que fazer automaticamente quando um evento do CRM acontece?":

- **Workflows**: regra configurável que combina um **disparo** (evento de domínio) + **condições**
  opcionais + **ações** (criar tarefa / criar atividade);
- **Execução automática e idempotente**: ao consumir eventos, o motor avalia condições e executa as
  ações em transações isoladas, com **chave de idempotência** no banco para nunca duplicar efeitos;
- **Disparo por inatividade**: varredura periódica detecta oportunidades paradas (`OPPORTUNITY_STALE`)
  e dispara regras de follow-up (ex.: criar tarefa) mesmo sem evento do usuário;
- **Histórico de execução**: cada ação registrada com status (PROCESSING/SUCCESS/FAILED/SKIPPED),
  resultado ou erro — visível no frontend.

Está alinhado com a preparação arquitetural iniciada no Sprint 12 para IA/Inbox/Workflow: os eventos
de domínio (`WorkflowTriggerEvent`) carregam contexto fechado (`opportunity.stage/value`,
`opportunity.daysWithoutActivity`, `task.priority`, `activity.type`) — sem avaliação de código arbitrário.

## Visão geral da estrutura

### Modelo de domínio (`domain/workflow/`)
- **Workflow**: `name`, `description`, `trigger`, `active`, lista de `conditions` e `actions`.
  Tem métodos de transição `activate()/deactivate()`.
- **WorkflowCondition**: campo fechado + operador + valor esperado + `sortOrder`.
- **WorkflowAction**: `ActionType` (CREATE_TASK | CREATE_ACTIVITY) + `sortOrder` + `config` (JSON).
- **WorkflowExecution**: registro de execução de uma ação para um evento (idempotência).
- **Enums**: `TriggerEvent`, `ActionType`, `ExecutionStatus`, `ConditionOperator`.
- **Evento de disparo**: `WorkflowTriggerEvent` — carrega `companyId`, `eventId`, `trigger`,
  `contactId/opportunityId/taskId/activityId` e o `context` fechado de campos para condições.

### Aplicação (`application/workflow/`)
- **WorkflowService** (UseCase): CRUD, `activate/deactivate`, `listByCompany`, `listExecutions`.
  Valida: pelo menos uma ação, operadores/valores de condição, e campos fechados por trigger.
- **WorkflowConditionEvaluator**: compara o `context` do evento com os valores configurados
  (igualdade insensível a caixa para strings; comparação numérica para `opportunity.value` e
  `opportunity.daysWithoutActivity`).
- **WorkflowActionRunner**: executa **uma** ação em `@Transactional(REQUIRES_NEW)` — falha de uma
  ação nunca contamina a transação do evento originador. Aplica idempotência via
  `insertNew(... ON CONFLICT ...)`: se a mesma ação/evento já foi registrada, pula.
- **WorkflowExecutor**: ao receber `WorkflowTriggerEvent`, filtra workflows ativos cujo `trigger`
  coincide, avalia condições e roda cada ação; **guarda de recursão** impede loops
  (ex.: ação que cria tarefa dispara `TASK_CREATED` e reavalia a mesma regra — o segundo disparo
  é ignorado pela chave de idempotência).
- **WorkflowTemplateSeeder**: seeds determinísticos por empresa — follow-up de proposta,
  contato inicial, agradecimento pós-venda e **follow-up de oportunidade parada**.

### Persistência (`infrastructure/workflow/persistence/`)
- `WorkflowJpaEntity`/`WorkflowConditionJpaEntity`/`WorkflowActionJpaEntity` com **RLS FORCE**
  (mesma política das demais entidades multi-tenant). Ações e condições com cascade + ordenação.
- `WorkflowExecutionJpaEntity` com **chave única** `(company_id, workflow_action_id, event_id)`
  = base da idempotência; inserção via **SQL nativo `ON CONFLICT DO NOTHING`** (retorna 0 se já
  existe) para ser atômica sob concorrência.
- Migrações `V041__workflow_tables.sql` (tabelas + RLS + índices + chave idempotência + políticas)
  e `V042__workflow_permissions.sql` (permissões `workflow:create/read/update/delete`).

### Infraestrutura de eventos
- `WorkflowEventListener` consome `WorkflowTriggerEvent` de forma **transacional** (mesma transação
  do publicador), garantindo que a avaliação de regras e a gravação do histórico são consistentes
  com a operação que originou o evento.
- Publicadores de eventos (módulos existentes, alterados):
  - **OpportunityService**: `OPPORTUNITY_CREATED`, `OPPORTUNITY_STAGE_CHANGED`, `OPPORTUNITY_WON`,
    `OPPORTUNITY_LOST`;
  - **TaskService**: `TASK_CREATED`, `TASK_COMPLETED`;
  - **ActivityService**: `ACTIVITY_CREATED`.
- **Novo — `WorkflowStaleOpportunityScanner`** (`infrastructure/workflow/scheduler/`): varredura
  agendada diária (cron `0 0 7 * * *`, configurável via `workflow.stale.cron`) que, para cada
  empresa, examina as oportunidades em **aberto** e publica `WorkflowTriggerEvent.opportunityStale`
  para aquelas sem atividade há **7+ dias** (mesmo critério do dashboard de atenção). O
  `eventId` determinístico (= `opportunityId`) garante idempotência entre varreduras repetidas:
  cada ação configurada dispara **no máximo uma vez** por oportunidade.
- **Novo — `WorkflowSchedulingConfig`** (`@EnableScheduling`) habilita o cron no backend.

### Segurança
- Controller `WorkflowController` com `@PreAuthorize` por permissão (`workflow:create/read/update/delete`)
  e checagem de empresa ativa.
- **RoleSeedService** atualizado: SUPER_ADMIN e MANAGER → CRUD completo; AGENT e VIEWER → leitura.
- RLS aplicada em todas as tabelas `workflow*` (empresa ativa).
- A varredura fixa o `TenantContext` por empresa (`setCompanyId` + `clear`) antes de ler os repositórios
  tenant-scoped (RLS via GUC `app.current_company_id`); `companies` não tem RLS e é enumerada globalmente.

## API (resumo)

| Método | Rota | Permissão | Descrição |
|---|---|---|---|
| POST | `/api/v1/companies/{companyId}/workflows` | workflow:create | Criar workflow |
| GET | `/api/v1/companies/{companyId}/workflows` | workflow:read | Listar |
| GET | `/api/v1/companies/{companyId}/workflows/{id}` | workflow:read | Detalhe |
| PUT | `/api/v1/companies/{companyId}/workflows/{id}` | workflow:update | Atualizar |
| POST | `/api/v1/companies/{companyId}/workflows/{id}/activate` | workflow:update | Ativar |
| POST | `/api/v1/companies/{companyId}/workflows/{id}/deactivate` | workflow:update | Desativar |
| DELETE | `/api/v1/companies/{companyId}/workflows/{id}` | workflow:delete | Excluir |
| GET | `/api/v1/companies/{companyId}/workflows/{id}/executions` | workflow:read | Histórico |
| GET | `/api/v1/companies/{companyId}/workflow-executions/recent` | workflow:read | Recentes |

## Frontend (`features/workflows/`)

- `types/workflow.types.ts` — tipos, labels e campos de condição por trigger.
- `services/workflow.service.ts` — client da API.
- `hooks/useWorkflows.ts` — queries/mutations com invalidação e toasts.
- `schemas/workflow.schema.ts` — validação Zod, builder de payload e mapper de edição.
- `components/` — `WorkflowTable` (lista + toggle ativo + ações), `WorkflowForm` (editor com
  condições/ações dinâmicas), `WorkflowExecutionsPanel` (histórico), `DeleteWorkflowDialog`.
- Páginas (rota `/workflows` no menu CRM): lista, `new`, `[id]` (detalhe + histórico + toggle),
  `[id]/edit`.
- `lib/constants.ts` → `ROUTES.WORKFLOWS`; `Sidebar.tsx` → item "Workflows".

## Qualidade

- **Backend**: **318 testes** passando (BUILD SUCCESS), incluindo `WorkflowServiceTest`,
  `WorkflowConditionEvaluatorTest`, `WorkflowActionRunnerTest`, `WorkflowExecutorTest` e o novo
  `WorkflowStaleOpportunityScannerTest` (3 testes: publica stale, ignora fresca, ignora não-aberta).
- **IT** `WorkflowIsolationIT` (Testcontainers): valida RLS + idempotência contra Postgres real
  (executado em ambiente com Docker/CI).
- **Frontend**: `tsc --noEmit` limpo para o código novo, `next lint` sem warnings, 128 testes
  (incluindo `workflow.schema.test.ts`) passando.
- **Nota**: os 6 erros de tipo pré-existentes em `useLeads.test.ts` (drift de versão do
  react-query) permanecem e **não** são deste sprint.

## Decisões de design

- **Idempotência por chave única** `(company, action, event)` no banco, com `ON CONFLICT DO NOTHING`
  — robusta a retry/concorrência (Item 6).
- **Ação em transação própria** (`REQUIRES_NEW`) — isolamento transacional (Item 5/Item 7):
  falha de uma ação nunca reverte o evento originador; falha fica registrada no histórico.
- **Campos de condição fechados** — sem avaliação de código arbitrário (segurança).
- **Execução transacional do listener** — avaliação + histórico consistentes com a operação origem.
- **Disparo por inatividade via varredura + `eventId` determinístico** — detecta oportunidades paradas
  sem depender de interação do usuário e mantém idempotência entre varreduras (não duplica follow-ups).

## Documentação

- `docs/CHANGELOG.md` — entrada da Sprint 14 (inclui `OPPORTUNITY_STALE`/scanner)
- `docs/PROJECT_INDEX.md` — `docs/WORKFLOW_AUTOMATION.md` registrado
- `docs/WORKFLOW_AUTOMATION.md` — doc dedicada do motor de automação
- `sprints/14/REPORT.md` — este relatório

## Produção / VPS

- **Validado em produção nesta etapa.** Deploy real executado na VPS `crm-vps` (`git pull --ff-only`
  + rebuild `docker compose build backend frontend` + `up -d`). **Migrations V041/V042 aplicadas**
  (Flyway `now at version v042`). Backend iniciou sem erro (`Started CrmApplication`).
- **Descoberta de topologia**: o deploy canônico de produção é **`docker/docker-compose.yml`**
  (rede externa `crm-network`, porta `127.0.0.1:8081` backend / `127.0.0.1:3000` frontend, servidos
  por nginx no host). O `docker/docker-compose.prod.yml` é legado e não é o alvo de deploy.
- **Smoke tests**: `/actuator/health` 200 (UP); `/api/v1/.../workflows` e `/api/v1/.../tasks` com
  token inválido → 401 (endpoints protegidos); página `/workflows` → 307 para login. Sem regressão.
- **Estado final**: `crm-backend` (8081), `crm-frontend` (3000), `crm-auth-service` (8082),
  `crm-postgres` (5432) saudáveis; artefatos legados (`crm-*-prod`) removidos.
- **Nota sobre `OPPORTUNITY_STALE`**: o trigger usa o mesmo critério (7 dias) já validado no
  dashboard do Sprint 12 e não exige nova migration (as tabelas V041/V042 já existem).

## Roadmap próximo

Sprint 14 +1: Inbox de eventos e integração IA (recomendação de ações); notificações em tempo real
(WebSocket) quando um workflow executar; rate limits e métricas de automação.