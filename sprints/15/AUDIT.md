# Sprint 15 — Auditoria do Workflow Automation (FASE 1)

**Data:** 2026-08-14 **Responsável:** AI Agent

Auditoria de tudo que já existe em Workflow (Sprints 13/14) antes de implementar melhorias de
observabilidade. Categorias: `JÁ EXISTE`, `PARCIALMENTE`, `NÃO EXISTE`, `NÃO É NECESSÁRIO`.

## 1. Domínio

| Item | Situação |
|---|---|
| `Workflow` (name, description, trigger, active, conditions, actions) | ✅ JÁ EXISTE |
| `WorkflowCondition` (field + operator + value + sortOrder) | ✅ JÁ EXISTE |
| `WorkflowAction` (ActionType + sortOrder + config JSON) | ✅ JÁ EXISTE |
| `WorkflowExecution` (per-action) | ✅ JÁ EXISTE |
| `TriggerEvent` (7 triggers) | ✅ JÁ EXISTE |
| `ActionType` (CREATE_TASK / CREATE_ACTIVITY) | ✅ JÁ EXISTE |
| `ExecutionStatus` (PROCESSING/SUCCESS/FAILED/SKIPPED) | ✅ JÁ EXISTE |
| `ConditionOperator` (6 operadores) | ✅ JÁ EXISTE |
| `WorkflowTriggerEvent` (evento + context fechado) | ✅ JÁ EXISTE |

## 2. Aplicação

| Item | Situação |
|---|---|
| `WorkflowService` (CRUD + activate/deactivate + listExecutions) | ✅ JÁ EXISTE |
| `WorkflowExecutor` (filtra ativos por trigger, avalia condições, executa ações) | ✅ JÁ EXISTE |
| `WorkflowConditionEvaluator` (avalia condições) | ⚠️ PARCIALMENTE — só retorna booleano agregado; **não expõe resultado por condição** |
| `WorkflowActionRunner` (ação em REQUIRES_NEW + idempotência) | ✅ JÁ EXISTE |
| `WorkflowTemplateSeeder` (seeds por empresa) | ✅ JÁ EXISTE |

## 3. Infraestrutura / Persistência / Eventos / Scheduler

| Item | Situação |
|---|---|
| `WorkflowController` (`@PreAuthorize` + empresa ativa) | ✅ JÁ EXISTE |
| RLS FORCE nas 4 tabelas `workflow*` | ✅ JÁ EXISTE |
| JPA entities/repos + inserts nativos `ON CONFLICT DO NOTHING` | ✅ JÁ EXISTE |
| Idempotência por `(company, action, event)` | ✅ JÁ EXISTE |
| Guard contra recursão | ✅ JÁ EXISTE |
| `WorkflowEventListener` (transacional) | ✅ JÁ EXISTE |
| Publicadores de eventos (Opportunity/Task/Activity) | ✅ JÁ EXISTE |
| `WorkflowStaleOpportunityScanner` + `@EnableScheduling` | ✅ JÁ EXISTE |
| Migrations V041 (tabelas+RLS) e V042 (permissões) | ✅ JÁ EXISTE |

## 4. Observabilidade da execução

| Pergunta | Situação |
|---|---|
| Qual workflow executou? | ✅ JÁ EXISTE (workflow_id na execution) |
| Para qual empresa? | ✅ JÁ EXISTE (company_id) |
| Qual evento disparou? | ✅ JÁ EXISTE (event_type) |
| Qual entidade originou? | ✅ JÁ EXISTE (entity_id) |
| Quando começou/terminou? | ⚠️ PARCIALMENTE — `created_at`/`updated_at` existem, mas `updated_at` **não é exposto no DTO** |
| Qual o resultado? | ✅ JÁ EXISTE (status + result_text) |
| Qual condição foi avaliada? | ❌ **NÃO EXISTE** — não há registro de condição avaliada |
| Valor esperado × encontrado? | ❌ **NÃO EXISTE** |
| Resultado de cada condição? | ❌ **NÃO EXISTE** |
| A ação teve sucesso? | ✅ JÁ EXISTE (status SUCCESS/FAILED) |
| Motivo da falha? | ✅ JÁ EXISTE (error_message) |
| Ignorado por condição? | ❌ **NÃO EXISTE** — quando uma condição falha, **nenhuma linha é gravada** |
| Bloqueado por idempotência? | ⚠️ PARCIALMENTE — `SKIPPED` existe no enum mas **nunca é persistido** (insertNew=0 → skip silencioso) |
| Contexto do evento (stage/value/priority/type/days)? | ❌ **NÃO EXISTE** — `event.context` não é persistido |

## 5. API / Histórico

| Item | Situação |
|---|---|
| Listar execuções de um workflow | ✅ JÁ EXISTE (`GET .../executions`) |
| Execuções recentes | ✅ JÁ EXISTE (`GET .../workflow-executions/recent`) |
| Detalhe de uma execução (endpoint) | ❌ **NÃO EXISTE** |
| Paginação | ❌ **NÃO EXISTE** — retorna lista completa |
| Filtros (status/evento/período/entidade) | ❌ **NÃO EXISTE** |
| Ordenação mais recente primeiro | ✅ JÁ EXISTE (`OrderByCreatedAtDesc`) |

## 6. Frontend

| Tela/UX | Situação |
|---|---|
| Lista `/workflows` (nome, disparo, ativo, atualizado) | ✅ JÁ EXISTE |
| Detalhe `/workflows/[id]` (detalhes + regras + histórico) | ✅ JÁ EXISTE |
| Form `/workflows/new` e `/workflows/[id]/edit` | ✅ JÁ EXISTE |
| Histórico em tabela (evento/ação/status/quando) | ✅ JÁ EXISTE |
| **Detalhe da execução** (condições esperado/encontrado, contexto, erro) | ❌ **NÃO EXISTE** |
| **Filtros + paginação do histórico** | ❌ **NÃO EXISTE** |
| **Dry-run / simulação** | ❌ **NÃO EXISTE** |
| Quantidade de execuções / última execução / último erro na lista | ❌ **NÃO EXISTE** |
| Explicar "por que executou/não executou" | ❌ **NÃO EXISTE** |

## 7. Templates

| Item | Situação |
|---|---|
| 4 templates seeds (proposta, contato inicial, agradecimento, parada) | ✅ JÁ EXISTE |
| "Follow-up após oportunidade parada" = OPPORTUNITY_STALE + daysWithoutActivity>=7 + criar tarefa | ✅ JÁ EXISTE e correto |
| Evitar duplicação de templates | ✅ JÁ EXISTE (idempotente por nome) |

## 8. Segurança

| Item | Situação |
|---|---|
| `@PreAuthorize('workflow:*')` | ✅ JÁ EXISTE |
| `requireCompanyAccess` (cross-tenant 403) | ✅ JÁ EXISTE |
| RLS FORCE nas 4 tabelas | ✅ JÁ EXISTE |
| Isolamento de histórico | ✅ JÁ EXISTE (RLS + query por company) |
| Isolamento cross-tenant provado (IT) | ✅ JÁ EXISTE (`WorkflowIsolationIT`, Docker) |

## 9. Testes

| Item | Situação |
|---|---|
| `WorkflowServiceTest` | ✅ JÁ EXISTE |
| `WorkflowConditionEvaluatorTest` | ✅ JÁ EXISTE |
| `WorkflowActionRunnerTest` | ✅ JÁ EXISTE |
| `WorkflowExecutorTest` | ✅ JÁ EXISTE |
| `WorkflowStaleOpportunityScannerTest` | ✅ JÁ EXISTE |
| `WorkflowIsolationIT` (Docker) | ✅ JÁ EXISTE (ambiental) |
| Testes p/ observabilidade/condição/contexto/filtros/dry-run | ❌ **NÃO EXISTE** |

## Decisões de escopo

1. **NÃO reescrever** o motor, nem tocar na idempotência/guarda/REQUIRES_NEW/RLS.
2. **Observabilidade (núcleo)**: adicionar registro em nível de **workflow-run** com condições
   avaliadas (esperado/encontrado/resultado), contexto seguro e motivo de skip. É a única forma de
   responder "por que executou/não executou" sem duplicar dados nas linhas por ação. **Justifica
   migration V043.**
3. **Não registrar duplicata idempotente como linha nova** (colidiria com a chave única); a unicidade
   já é a prova de que a ação rodou uma vez.
4. **Dry-run**: endpoint que avalia condições/contexto fornecidos e lista ações que seriam executadas,
   **sem** tocar em Task/Activity/Opportunity (reusa `WorkflowConditionEvaluator`; não invoca runner).
5. **Filtros + paginação** do histórico via Spring Data Page, reutilizando o padrão `PageResponse` do Leads.
6. **Frontend**: detalhe de execução (condições/contexto/ações/erro), filtros+paginação, dry-run e
   resumo (execuções/último status) na lista.
