# SPRINT 18 — AUTOMAÇÕES OMNICHANNEL — REPORT

> Status: ✅ CONCLUÍDA
> Commits: ver Git abaixo

## DECISÃO ARQUITETURAL PRINCIPAL (fonte de verdade: código real)

A leitura obrigatória do estado do backend revelou que o projeto **já possui um
motor de automações completo**: o módulo **Workflow** (Sprint 14/15, estendido na
16), com:

- triggers por evento (`WorkflowTriggerEvent` publicado via `EventPublisher`
  → `@EventListener` → `WorkflowExecutor`);
- condições `field/operator/value` avaliadas sem código arbitrário;
- ações idempotentes (`UNIQUE (company_id, workflow_action_id, event_id)`);
- histórico duplo (`workflow_runs` + `workflow_executions`);
- lifecycle ativo/inativo, REST CRUD + activate/deactivate + dry-run;
- RLS FORCE + permissões `workflow:*` + auditoria;
- scheduler existente (`WorkflowStaleOpportunityScanner`).

**Decisão:** em vez de duplicar um módulo "automation" paralelo (violando a regra
"não criar segundo CampaignService/dispatcher"), a Sprint 18 **estendeu o motor
existente** com os gatilhos e ações omnichannel previstos. Nenhuma migration foi
necessária: os enums são persistidos como VARCHAR e nenhuma estrutura nova de
banco foi exigida.

## IMPLEMENTAÇÃO

### Backend (extensões)

- **Novos triggers** (`TriggerEvent`):
  - `CONTACT_CREATED` — publicado em `ContactService.create`;
  - `LEAD_STATUS_CHANGED` — publicado em `LeadService.update` quando o status muda;
  - `CAMPAIGN_COMPLETED` — publicado em `CampaignExecutionService.finishIfDone`
    com `eventId = executionId` (determinístico ⇒ idempotente).
- **Novos operadores** (`ConditionOperator`): `CONTAINS`, `IS_NULL`, `IS_NOT_NULL`,
  implementados em `WorkflowConditionEvaluator` (sem SpEL/código dinâmico).
- **Novas ações** (`ActionType`):
  - `SEND_NOTIFICATION` — notificação in-app via `NotificationUseCase` (config:
    userId obrigatório, title, body);
  - `EXECUTE_CAMPAIGN` — executa campanha SCHEDULED via `CampaignUseCase.executeNow`
    (reutiliza dispatcher/infraestrutura da Sprint 17 sem duplicá-la).
- Idempotência, retries/falha por ação (`recordFailure`), guarda anti-loop,
  TenantContext e transações `REQUIRES_NEW` permanecem os mecanismos já
  validados do motor.

### Frontend

- Tipos/schemas do builder atualizados: novos triggers, operadores e ações
  (com labels pt-BR e campos de condição por trigger);
- Sidebar: item renomeado para **"Automações"** (mesma rota `/workflows`,
  reaproveitando lista/builder/detalhes/histórico existentes).

## TESTES

- Backend: **520 unit tests verdes** (inclui novos
  `WorkflowTriggerEventSprint18Test`, `WorkflowConditionEvaluatorSprint18Test`);
  checkstyle OK; `WorkflowIsolationIT` e demais ITs executados no CI (verify);
- Frontend: lint/typecheck/format/build OK; **208 Vitest verdes**.

## GIT

- Commits: feat + docs; hash final ver seção final; working tree limpa;
  `LOCAL == origin/main`.

## CI/CD

- CI Pipeline GREEN · GHCR GREEN · CD GREEN (deploy automático crm-vps).

## VPS

- Flyway: schema inalterado (62 migrations validadas);
- Containers UP/healthy; `/actuator/health` = UP; frontend HTTP 200;
- API de automações (`/workflows`) protegida → 401 sem sessão;
- Scheduler: sem erros recorrentes nos logs após deploy.

## LIMITAÇÕES

- E2E autenticado na VPS continua débito herdado (validação de CRUD real
  coberta pelos testes unitários/integração do motor + smoke 401);
- `EXECUTE_CAMPAIGN` exige campanha SCHEDULED (sem auto-schedule pela ação);
- `SEND_NOTIFICATION` exige `userId` explícito no config (destinatário).

## DÉBITOS (permanecem)

RabbitMQ real; canal e-mail real; backoff exponencial; export CSV; E2E
autenticado na VPS; provider WhatsApp real conforme configuração.

## FUTURE (fora do escopo)

Analytics Sprint 19; IA autônoma em automações; SMS/push; BPMN visual.
