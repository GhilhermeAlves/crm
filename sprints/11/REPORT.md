# Sprint 11 — Pipeline

**Data:** 2026-08-13 · **Status:** ✅ Concluída · **Responsável:** AI Agent · **Dependência:** 10

## Resumo

Sprint de **funil de vendas (pipeline)** por empresa dentro do modelo SaaS/RLS já
estabelecido. Construiu o módulo `pipeline` de ponta a ponta (banco → backend → frontend),
adicionando os DDLs/índices/RLS (V033+V034+V037, herdados das sprints 8.x/10), as
permissões `pipeline:*`/`stage:*`/`opportunity:*` (V038 + `RoleSeedService`) e a camada de
aplicação + UI sobre essa base, reutilizando o padrão dos módulos `contact`/`lead`.

## Visão geral da estrutura

- **Pipelines** por empresa: um funil com N estágios ordenados.
- **Etapas (stages)** em cada pipeline: `id`, `name`, `position`, `probability`.
- **Oportunidades**: pertencentes a um `pipeline` + `stage`; movimentação `ADVANCE`/`REGRESS`
  (só ±1 dentro do funil), conclusão (`WON`/`LOST`) somente no **último estágio**,
  cancelamento (`CANCELLED`), reabertura (`WON/LOST/CANCELLED → OPEN`).
- **Histórico imutável** por oportunidade (`opportunity_stage_history`),
  conservado nas transições de estágio/status — registra `stage_from/stage_to`,
  `status_from/status_to` e quem mudou.

## Banco de dados

- **Base** (já existente das sprints 8.x/10): tabelas `pipelines`, `stages`, `opportunities`,
  `opportunity_stage_history`, `lead_stage_history`, `opportunity_activities`, índices
  `(pipeline_id, position)` em `stages` e `(pipeline_id, stage_id)` em `opportunities`
  (V033/V034/V037) e **RLS FORCE + `tenant_isolation_policy`** para `pipelines`, `stages`
  e `opportunities` (V034/V037) — isolamento multi-tenant garantido pelo banco.
- **Nova migration `V038__pipeline_opportunity_permissions.sql`**: registra as permissões
  do módulo — `pipeline:create/read/update/delete` (read reusa a existente `pipeline:read`),
  `stage:create/read/update/delete`, `opportunity:read/create/update/delete/move/win/lose/
  reopen/cancel` e `pipeline.metrics:read`.

## Backend

Novo módulo `com.becommerce.crm.pipeline` espelhando `lead`:

- **Domínio**:
  - `domain/pipeline/Pipeline.java`, `Stage.java`, `Opportunity.java`, enums
    `OpportunityStatus` (OPEN/WON/LOST/CANCELLED) e `CurrentStage` auxiliar;
    `OpportunityHistoryEntry` (tínha + transitions).
  - Exceções: `PipelineNotFoundException`, `StageNotFoundException`,
    `OpportunityNotFoundException`, `OpportunityDomainException` (regra de domínio → 409).
- **Aplicação**:
  - Portas de entrada `port/input/PipelineUseCase.java`, `StageUseCase.java`,
    `OpportunityUseCase.java` — CRUD + `move/win/lose/cancel/reopen` + métricas.
  - Portas de saída `port/output/PipelineRepository.java`, `StageRepository.java`,
    `OpportunityRepository.java` (todos retornando `Page<T>` + isolamento por tenant).
  - DTOs `dto/*Request.java`, `*Response.java`, `PipelineMetricsResponse.java`
    (contagem/valor por status, win rate, ticket médio, tempo médio no funil).
  - `service/PipelineService.java`, `StageService.java`, `OpportunityService.java`
    (`OpportunityMetricsService`) — cada operação isola no `TenantContext`
    (`finally clear()`), valida que oportunidade/pipeline pertencem à mesma empresa
    (`PipelineRepository` de saída) como defense-in-depth além do RLS, e audita
    via `TenantAuditRecorder` (`AuditModule.PIPELINES` + `AuditAction.*`).
  - Regras de negócio implementadas: unidade do funil (±1, sem pular estágio),
    conclusão/cancelamento **apenas no último estágio**, movimentação recusa estágio
    inexistente (404), histórico imutável (transições nunca atualizam, só acrescentam),
    métricas somam por estágio/pipeline de forma tenant-isolada.
- **Apresentação**:
  - `presentation/rest/pipeline/PipelineController.java` (`/api/v1/companies/{companyId}/pipelines`),
    `StageController.java` (`.../pipelines/{pipelineId}/stages`),
    `OpportunityController.java` (`.../pipelines/{pipelineId}/opportunities`) e
    `PipelineMetricsController.java` (`.../pipelines/{pipelineId}/metrics`) com
    `@PreAuthorize('pipeline:read/create/...')` + `requireCompanyAccess`
    (SUPER_ADMIN cross-tenant preservado; demais restritos à empresa ativa).
- **Infraestrutura**:
  - `infrastructure/pipeline/persistence/` — `PipelineJpaEntity`, `StageJpaEntity`,
    `OpportunityJpaEntity`, `OpportunityHistoryJpaEntity` + `*JpaRepository` (paginados) +
    `*RepositoryImpl` ligando domínio↔JPA.
- **Handlers**: `GlobalExceptionHandler` recebe handlers para as novas exceções de domínio
  (404/409).

## Frontend

Nova feature `features/pipeline` + página `app/(dashboard)/pipeline`:

- **Tipos** `types/pipeline.types.ts` — `Pipeline`, `Stage`, `Opportunity`,
  `OpportunityStatus`, `StageStats`, `PipelineMetrics` etc.
- **Schema** `schemas/pipeline.schema.ts` — validação Zod (nomes/posições, probabilidade
  0–100) + rótulos pt-BR.
- **Serviço** `services/pipeline.service.ts` — `listPipelines/findPipeline/createPipeline/
  updatePipeline/deletePipeline`, stages (CRUD+reorder), oportunidades (CRUD + move/win/lose/
  cancel/reopen) e métricas — todos scoped por `companyId`.
- **Hooks** `hooks/usePipelines.ts`, `hooks/useOpportunities.ts`, `hooks/useStageMetrics.ts`
  — React Query com invalidação por `["pipeline", companyId]`.
- **Componentes** `components/`:
  - `PipelineBoard` — colunas por estágio (wip), cards arrastáveis/movíveis.
  - `OpportunityCard` — valor/estágio/probabilidade, ações mover/concluir/cancelar.
  - `PipelineMetricsStrip` — win rate, ticket médio, tempo médio no funil, distribuição.
  - Diálogos: `CreatePipelineDialog`, `CreateStageDialog`, `RenameStageDialog`,
    `OpportunityFormDialog`, `ConfirmDialog` (concluir/cancelar/excluir).
- **Página** `app/(dashboard)/pipeline/page.tsx` — board + métricas + gating por
  permissão (`pipeline:read`); ações de escrita gated por `pipeline:*`/`opportunity:*`.
- **Permissões** — `Sidebar` item Pipeline gated por `pipeline:read`; backend continua a
  autoridade final (403).

## Testes

> **Validado em 2026-08-13.** Contagens refletem execução real.

- **Backend: 267 testes PASS** (antes 228). Novos:
  - `PipelineServiceTest`, `StageServiceTest`, `OpportunityServiceTest`,
    `OpportunityMetricsServiceTest` (~24): CRUD de pipeline/etapas/oportunidades,
    regras de domínio (fim do funil, ±1, conclusão só no último estágio, histórico
    imutável), métricas.
  - `PipelineControllerTest`, `OpportunityControllerTest` (~10): 201/200/400/404/409,
    403 cross-company, autorização por permissão e isolamento.
  - `PipelineIsolationIT` (Testcontainers PostgreSQL + RLS, **executado PASS**):
    isolamento cross-tenant real nas tabelas `pipelines`/`stages`/`opportunities`.
- **Frontend: 106 testes PASS** (antes 96). Novos:
  - `pipeline.schema.test.ts` (5): validação Zod do pipeline.
  - `OpportunityCard.test.tsx` (5): renderização de valor/estágio/probabilidade,
    desabilitação de movimento nas pontas, ações do card, gating por permissão.
- Typecheck OK (backend); frontend `tsc --noEmit` OK nos novos arquivos — erro pré-existente
  em dart-de `useLeads.test.ts` (Sprint 10, fora de escopo) mantido; lint sem erros novos;
  build production OK (rota `/pipeline` gerada).

## Build e integração

- Backend: `mvnw test` BUILD SUCCESS (267 testes), compile OK.
- Frontend: `tsc --noEmit` (novos arquivos OK), `next lint` OK, `next build` OK.
- Integração: `/api/v1/companies/{companyId}/pipelines*`, `.../stages`, `.../opportunities`,
  `.../metrics` com RLS + `@PreAuthorize` + `requireCompanyAccess` + `TenantContext`;
  UI refletindo `pipeline:*`/`opportunity:*` permissions.

## E2E

- **E2E autenticado manual não realizado** (limitação herdada das sprints 8.x/9/10): fluxo
  real de criação/edição de pipeline/oportunidade no browser exige credenciais de teste não
  automatizáveis. Testes unitários de controller/serviço cobrem autorização/validação/regras;
  não inventado resultado de E2E.

## Produção / VPS

- **Validado em produção nesta etapa.** Deploy real executado na VPS `crm-vps`
  (`docker compose build` + `up -d`: rebuild backend+frontend, **migration V038 aplicada**
  — Flyway `now at version v038`). Smoke tests: backend iniciou sem erro (Tomcat 8080,
  `Started CrmApplication`, **0 ERROR/Exception** nos logs), endpoints
  `/api/v1/companies/{companyId}/pipelines*` registrados e idempotentes à autorização
  (retornam 404 unauthenticated, idêntico ao `/leads` — sem regressão), rota
  `/pipeline` 307 → `/login?redirect=%2Fpipeline` no frontend (build contém
  `pipeline.html`). Nota: `/actuator/health` não expõe corpo (gestão desabilitada —
  comportamento pré-existente, não alterado nesta sprint).

## Débitos

- **E2E autenticado manual** (herdado): fluxo real no browser sem credenciais.
- **Envio de e-mail real** (herdado): `ConsoleEmailSender` placeholder.
- **Scoring/distribuição/conversão** (Lead.md L-020/030/040, Pipeline.md P-0xx avançadas):
  regras avançadas de qualificação e scoring ficam para sprints 17 (IA); esta sprint
  entregou CRUD + movimentação ±1 + conclusão/cancelamento + histórico + métricas.

## Artefatos

- Backend: `domain/pipeline/*`, `application/pipeline/{dto,port,service}/*`,
  `presentation/rest/pipeline/*`, `infrastructure/pipeline/persistence/*`,
  `GlobalExceptionHandler.java`, `RoleSeedService.java`, `AuditModule.java`,
  `db/migration/V038__pipeline_opportunity_permissions.sql`.
- Testes backend: `PipelineServiceTest.java`, `StageServiceTest.java`,
  `OpportunityServiceTest.java`, `OpportunityMetricsServiceTest.java`,
  `PipelineControllerTest.java`, `OpportunityControllerTest.java`, `PipelineIsolationIT.java`.
- Frontend: `features/pipeline/{types,schemas,services,hooks,components}/*`,
  `app/(dashboard)/pipeline/page.tsx`, `components/layout/Sidebar.tsx`.