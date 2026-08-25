# SPRINT 17 — CAMPANHAS — REPORT

> Status: ✅ CONCLUÍDA
> Planejamento: `sprints/17/PLAN.md`
> Commits: `156b2d9` (implementação), `e77dea8` (V062 grants + scheduler multi-tenant)

## Objetivo

Implementar o módulo de Campanhas do CRM SaaS Omnichannel conforme `PLAN.md`:
CRUD + ciclo de vida controlado, segmentação simples extensível, templates,
canal WhatsApp sobre a infraestrutura Omnichannel da Sprint 16, execução
manual/agendada idempotente, eventos por destinatário, tenant isolation
(company_id + RLS), permissões, auditoria e frontend completo.

## Backend

### Migrations (Flyway)

| Migration | Conteúdo |
|---|---|
| V055 | `message_templates` (templates por canal, variáveis, versionamento) + RLS FORCE |
| V056 | `campaigns` (aggregate root, CHECK de status) + RLS FORCE |
| V057 | `campaign_channels` (canal Omnichannel + template congelado) + RLS FORCE |
| V058 | `campaign_executions` (batch + cursor persistido) + RLS FORCE |
| V059 | `campaign_message_events` — **UNIQUE (execution_id, recipient_id)** (idempotência) + RLS FORCE |
| V060 | Permissões `campaign:*` e `template:*` |
| V061 | Grants para roles (ADMIN/MANAGER/AGENT/VIEWER) de todas as companies |

### Arquitetura (hexagonal, padrão do projeto)

- **Domain** (`domain.campaign`, `domain.template`): `Campaign` (state machine
  `DRAFT→SCHEDULED→RUNNING⇄PAUSED→COMPLETED/CANCELLED` com transições
  controladas), `CampaignChannel`, `CampaignExecution`, `CampaignMessageEvent`,
  `MessageTemplate` (variáveis `{{var}}`, versionamento por edição),
  `CampaignNotFoundException`, `TemplateNotFoundException`.
- **Application**: ports in/out (`CampaignUseCase`, `TemplateUseCase`,
  repositories, `AudienceResolver`, `CampaignChannelDispatcher`), services
  (`CampaignService`, `TemplateService`, `CampaignExecutionService`).
- **Infrastructure**:
  - Persistência JPA (campanha/execução/template) + JDBC (`CampaignEventRepositoryImpl`)
    usando `ON CONFLICT DO NOTHING` para idempotência real em banco;
  - `AudienceResolverImpl`: contatos/leads ativos com telefone, determinístico
    (`ORDER BY id`), tenant-safe;
  - `WhatsAppCampaignDispatcher`: reutiliza o `WhatsAppProvider` da Sprint 16
    (sem duplicar auth/secrets/HMAC);
  - `CampaignScheduler`: `@Scheduled` a cada 60s para campanhas agendadas
    vencidas — idempotente pelo claim atômico `UPDATE ... WHERE status='SCHEDULED'`;
  - Executor dedicado (`campaign-dispatch`) para o loop batch+cursor, com
    `TenantContext` restaurado por lote. Throttling configurável
    (`campaign.dispatch.throttle-ms`, default 200ms).
- **API REST**:
  - `/api/v1/companies/{companyId}/campaigns`
    (`GET` lista paginada/filtrada, `POST`, `GET/{id}`, `PUT/{id}`, `DELETE/{id}`,
    `POST /{id}/channel`, `POST /{id}/schedule`, `POST /{id}/execute`,
    `POST /{id}/pause`, `POST /{id}/resume`, `POST /{id}/cancel`,
    `GET /{id}/execution`);
  - `/api/v1/companies/{companyId}/templates` (CRUD).
- **Permissões**: `campaign:read/create/update/delete/execute/view_metrics`,
  `template:read/create/update/delete` — `@PreAuthorize` em todos os endpoints +
  `requireCompanyAccess` + RLS.
- **Auditoria**: via `TenantAuditRecorder` existente (módulo novo `CAMPAIGNS`
  em `AuditModule`) — criação, atualização, agendamento, execução, pausa,
  retomada, cancelamento, conclusão, canal/template.

## Frontend

- `src/features/campaigns/{types,services,hooks,components}` seguindo o padrão Leads;
- `/campaigns` — lista com busca local, filtros status/público, paginação,
  badges de ciclo de vida, ações (executar/pausar/retomar/cancelar/excluir)
  gated por permissão, empty/loading states, dialogs de confirmação;
- `/campaigns/new` — wizard em 6 etapas (Informações → Público → Canal →
  Mensagem/Template → Agendamento → Revisão) com validação por etapa e
  criação inline de template;
- `/campaigns/[id]` — Campaign 360: informações, execução com progresso
  (polling 5s quando RUNNING/PAUSED), falhas, ações de ciclo de vida;
- Sidebar "Campanhas" com permissão `campaign:read`; rota já existente em
  `ROUTES.CAMPAIGNS`.

## Testes

- **Unit** (backend): `CampaignTest` (state machine/transições/edição),
  `MessageTemplateTest` (variáveis/render/versionamento) — 12 testes;
- **Integration/Testcontainers**: `CampaignIsolationIT` (padrão Sprint 16,
  PostgreSQL 17 + usuário NOSUPERUSER NOBYPASSRLS): cross-tenant read/update/
  delete = 0 linhas, WITH CHECK bloqueia insert cross-tenant, UNIQUE de
  idempotência, ON CONFLICT idempotente, claim atômico do START;
- Suite backend completa: **514 unit tests verdes**; checkstyle OK;
- Frontend: lint OK, typecheck OK, format OK, build prod OK, **208 testes
  vitest verdes (32 arquivos)**.

## CI/CD e VPS (validação real)

- **CI Pipeline: GREEN** (`156b2d9`, `e77dea8`) — backend `mvnw clean verify`
  (inclui `CampaignIsolationIT` com Testcontainers/Docker no runner),
  auth-service, frontend lint/typecheck/format/build, Docker build;
- **GHCR: GREEN** — imagens backend/frontend/auth publicadas;
- **CD Pipeline: GREEN** — deploy automático na `crm-vps`;
- **VPS (`/opt/crm`)**:
  - Flyway: 62 migrations validadas; V055–V062 aplicadas com `success = true`;
  - RLS confirmado em produção: `rowsecurity = true` para as 5 tabelas novas;
  - Permissões seedadas: 10 (`campaign:*`, `template:*`);
  - Containers: frontend/backend/auth UP; postgres/redis/rabbitmq/minio/keycloak healthy;
  - `/actuator/health` = `{"status":"UP"}`; frontend HTTP 200;
  - Smoke: `/campaigns` API → **401 sem sessão** (protegida); `/templates` API → **401**;
    frontend redireciona `/campaigns` → login quando não autenticado (307);
  - Zero erros `permission denied for table campaigns` após V062
    (correção do padrão V046) e scheduler multi-tenant operacional.

## Limitações

- E2E autenticado na VPS não executado (credenciais Keycloak não disponíveis
  nesta sessão) — débito já conhecido do projeto. Cobertura equivalente obtida
  via `CampaignIsolationIT` (Testcontainers, CI GREEN) + smoke de proteção 401.
- Envio WhatsApp real depende do provider configurado na VPS (`FakeWhatsAppProvider`
  é o default); dispatcher validado até a fronteira do provider.
- DELIVERED/READ/RESPONDED/OPTED_OUT dependem de webhooks do provider
  (infraestrutura da Sprint 16); estrutura de eventos pronta para recebê-los.

## Débitos registrados

- E2E autenticado na VPS (débito herdado do projeto);
- RabbitMQ ainda sem producers/consumers reais → execução usa batch+cursor
  persistido (fallback previsto no PLAN.md);
- E-mail canal: não implementado (débito pré-existente "e-mail real");
- Retry com backoff simples por ciclos do executor (sem delay exponencial);
- Export CSV de destinatários/erros: FUTURE.

*(Status final atualizado após validação na VPS.)*
