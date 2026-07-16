# Playbook: Implementação do Módulo Pipeline

## Objetivo
Implementar o pipeline de vendas: pipelines, estágios, oportunidades, movimentação entre estágios, histórico de alterações, e visualização Kanban.

## Pré-requisitos
- Módulo Contact implementado (oportunidades referenciam contatos)
- Módulo Auth implementado (usuários autenticados)
- Módulo Company implementado (multi-tenancy ativo)

## Documentos que DEVEM ser lidos
- `docs/Pipeline.md`
- `docs/Stages.md`
- `docs/Kanban.md`
- `docs/Entities.md`
- `contexts/pipeline-context.md`

## Arquivos que poderão ser alterados
### Backend
- `packages/backend/src/domain/pipeline/` — Entidades: Pipeline, Stage, Opportunity, OpportunityHistory
- `packages/backend/src/application/pipeline/` — Casos de uso: CreatePipelineUseCase, UpdatePipelineUseCase, CreateStageUseCase, UpdateStageUseCase, ReorderStagesUseCase, CreateOpportunityUseCase, MoveOpportunityUseCase, ListOpportunitiesUseCase, GetOpportunityHistoryUseCase
- `packages/backend/src/infrastructure/persistence/` — PipelineRepository, StageRepository, OpportunityRepository
- `packages/backend/src/presentation/rest/controller/PipelineController.ts`
- `packages/backend/src/presentation/rest/controller/OpportunityController.ts`

### Frontend
- `packages/frontend/src/components/pipeline/` — PipelineBoard, PipelineColumn, OpportunityCard, OpportunityDetail, StageConfig, DragDropContext
- `packages/frontend/src/hooks/usePipeline.ts`
- `packages/frontend/src/app/(auth)/pipeline/` — Páginas: board, [id], stages

## Arquivos proibidos
- `packages/backend/src/domain/contact/` — Entidades de contato não devem ser alteradas
- `packages/backend/src/presentation/rest/controller/LeadController.ts` — Lead controller não deve ser alterado
- `packages/frontend/src/components/leads/` — Componentes de lead não devem ser alterados

## Ordem de implementação
1. Definir entidades: Pipeline, Stage, Opportunity, OpportunityHistory
2. Implementar repositórios (com ordenação de stages)
3. Implementar CRUD de Pipelines
4. Implementar CRUD de Stages com ordenação
5. Implementar criação de oportunidades
6. Implementar movimentação de oportunidades entre stages
7. Implementar histórico de movimentações
8. Implementar controllers REST
9. Criar componente PipelineBoard com drag-and-drop
10. Criar componente OpportunityCard (draggable)
11. Criar componente OpportunityDetail
12. Integrar com hook usePipeline

## Checklist Backend
- [ ] Entidade Pipeline: id, name, description, companyId, isActive, createdAt, updatedAt
- [ ] Entidade Stage: id, pipelineId, name, color, order, probability (0-100), companyId
- [ ] Entidade Opportunity: id, title, value, contactId, pipelineId, stageId, assignedTo, expectedCloseDate, companyId, createdAt, updatedAt
- [ ] Entidade OpportunityHistory: id, opportunityId, fromStageId, toStageId, changedBy, changedAt, note
- [ ] CreatePipelineUseCase: cria pipeline + stages padrão (Prospecção, Qualificação, Proposta, Negociação, Fechamento)
- [ ] UpdatePipelineUseCase: editar nome/descrição
- [ ] CreateStageUseCase: criar novo estágio com posição correta
- [ ] ReorderStagesUseCase: reordenar estágios (atualiza campo order)
- [ ] CreateOpportunityUseCase: cria oportunidade vinculada a contato e pipeline
- [ ] MoveOpportunityUseCase: move oportunidade para stage diferente + registra histórico
- [ ] MoveOpportunityUseCase: valida se stage de destino existe no mesmo pipeline
- [ ] ListOpportunitiesUseCase: filtrar por pipeline, stage, assignedTo, value range
- [ ] GetOpportunityHistoryUseCase: retorna timeline de movimentações
- [ ] PipelineController com endpoints CRUD
- [ ] OpportunityController com endpoints: GET, POST, PUT, POST /move
- [ ] Multi-tenancy: pipelines filtrados por company_id

## Checklist Frontend
- [ ] PipelineBoard: visualização Kanban com colunas (stages)
- [ ] PipelineColumn: coluna com header (nome, cor, contagem) + lista de cards
- [ ] OpportunityCard: título, valor, contato, data prevista, avatar do responsável
- [ ] Drag-and-drop: mover cards entre colunas com optimistic update
- [ ] OpportunityDetail: informações completas + editar + histórico
- [ ] StageConfig: adicionar, editar, reordenar, remover estágios
- [ ] Criar nova oportunidade (modal/formulário)
- [ ] Resumo do pipeline: valor total, quantidade de oportunidades, valor por stage
- [ ] Hook usePipeline: listPipelines, getPipeline, createOpportunity, moveOpportunity, getHistory
- [ ] Responsividade: mobile friendly (scroll horizontal ou lista)

## Checklist Banco
- [ ] Tabela `pipelines`: id, name, description, company_id (FK), is_active, created_at, updated_at
- [ ] Tabela `stages`: id, pipeline_id (FK), name, color, "order", probability, company_id (FK), created_at
- [ ] Tabela `opportunities`: id, title, value (decimal), contact_id (FK), pipeline_id (FK), stage_id (FK), assigned_to (FK users), expected_close_date, company_id (FK), created_at, updated_at
- [ ] Tabela `opportunity_history`: id, opportunity_id (FK), from_stage_id (FK), to_stage_id (FK), changed_by (FK users), changed_at, note
- [ ] Índices: opportunities.pipeline_id, opportunities.stage_id, opportunities.company_id, stages.pipeline_id + "order"
- [ ] Foreign keys com ON DELETE CASCADE para stages, opportunities, history

## Checklist Testes
- [ ] Testes unitários: CreatePipelineUseCase (validações)
- [ ] Testes unitários: MoveOpportunityUseCase (validações de stage)
- [ ] Testes de integração: CRUD de pipelines e stages
- [ ] Testes de integração: Criar oportunidade vinculada a pipeline/stage
- [ ] Testes de integração: Mover oportunidade registra histórico
- [ ] Testes de integração: Reordenar stages mantém consistência
- [ ] Testes de integração: Validação — stage destino deve pertencer ao mesmo pipeline
- [ ] Testes E2E: Criar pipeline → adicionar stages → criar oportunidade → mover entre stages → verificar histórico

## Checklist Documentação
- [ ] Atualizar `docs/Pipeline.md` com endpoints e exemplos
- [ ] Atualizar `docs/Stages.md` com regras de estágios
- [ ] Atualizar `docs/Kanban.md` com comportamento do board
- [ ] Documentar stages padrão de novo pipeline

## Checklist Final
- [ ] Pipelines são criados com stages padrão
- [ ] Estágios podem ser reordenados
- [ ] Oportunidades são criadas e vinculadas corretamente
- [ ] Drag-and-drop move oportunidades entre stages
- [ ] Histórico de movimentações é registrado
- [ ] Multi-tenancy isola pipelines por empresa
- [ ] Resumo do pipeline mostra valores corretos
- [ ] Todos os testes passam
