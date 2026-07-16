# Pipeline Context

## Resumo do Módulo
Múltiplos pipelines com 2-15 estágios cada. Movimentação de oportunidades restrita a ±1 estágio. Won/Lost apenas no último estágio.

## Objetivo
Gerenciar o fluxo de vendas com pipelines configuráveis e métricas de performance.

## Responsabilidades
- CRUD de pipelines com 2-15 estágios
- Movimentação de oportunidades (±1 estágio)
- Won/Lost apenas no último estágio
- Métricas: win rate, ciclo médio, forecast
- Histórico de movimentações (audit trail)

## Entidades Relacionadas
- Pipeline, Stage, Opportunity, OpportunityHistory

## APIs Relacionadas
- `GET /pipelines` - Listar pipelines
- `POST /pipelines` - Criar pipeline
- `GET /pipelines/:id` - Pipeline com estágios
- `POST /pipelines/:id/stages` - Adicionar estágio
- `GET /pipelines/:id/opportunities` - Oportunidades do pipeline
- `POST /opportunities` - Criar oportunidade
- `PUT /opportunities/:id/stage` - Mover estágio (±1)
- `PUT /opportunities/:id/won` - Marcar como ganha
- `PUT /opportunities/:id/lost` - Marcar como perdida
- `GET /opportunities/:id/history` - Histórico de movimentação

## Banco Relacionado
- `pipelines` - Configuração do pipeline
- `stages` - Estágios do pipeline (ordem, cor, probabilidade)
- `opportunities` - Dados da oportunidade, valor, probabilidade
- `opportunity_history` - Audit trail de movimentações

## Métricas
- **Win Rate** - % de oportunidades ganhas
- **Avg Cycle** - Tempo médio de ciclo de venda
- **Forecast** - Valor × Probabilidade de todas oportunidades ativas

## Componentes Frontend
- PipelinesList, PipelineEditor
- OpportunityForm, OpportunityDetail
- StageConfigurator (drag-and-drop reorder)

## Componentes Backend
- `pipeline` module (Controllers, Services, Domain, Repository)
- `opportunity` module (Services, Domain, Repository)
- `forecast` module (cálculo de forecast)

## Eventos
- `PipelineCreated/Updated` - Pipeline criado/atualizado
- `OpportunityCreated` - Nova oportunidade
- `OpportunityMoved` - Estágio alterado (audit trail)
- `OpportunityWon` - Ganha (cria Customer)
- `OpportunityLost` - Perdida
- `ForecastUpdated` - Forecast recalculado

## Permissões
- `pipeline:create` - ADMIN, MANAGER
- `pipeline:read` - Todos
- `pipeline:update` - ADMIN
- `opportunity:create` - ADMIN, MANAGER, AGENT
- `opportunity:move` - ADMIN, MANAGER, AGENT
- `opportunity:win/lost` - ADMIN, MANAGER

## Dependências
- **Stages** - Estágios do pipeline
- **Contacts** - Contato da oportunidade
- **Leads** - Conversão cria oportunidade
- **Customers** - WON cria cliente

## Fluxo Resumido
1. Pipeline criado com 2-15 estágios (probabilidade automática)
2. Oportunidade criada no primeiro estágio → movimenta ±1 estágio por vez
3. Último estágio → marcar WON (cria customer) ou LOST → audit trail registrado

## Checklist de Implementação
- [ ] CRUD pipelines com 2-15 estágios
- [ ] Movimentação ±1 estágio
- [ ] Won/Lost apenas último estágio
- [ ] Audit trail completo (OpportunityHistory)
- [ ] Métricas: win rate, ciclo médio, forecast
- [ ] Forecast = Σ(valor × probabilidade)
- [ ] Probabilidade automática por estágio
- [ ] Múltiplos pipelines por empresa

## Checklist de Testes
- [ ] Movimentação ±1 funciona
- [ ] Won/Lost bloqueado em estágios intermediários
- [ ] Histórico registra todas movimentações
- [ ] Forecast calculado corretamente
- [ ] Win rate atualiza em tempo real

## Documentação Oficial Relacionada
- `docs/pipeline/PIPELINE-CONFIG.md`
- `docs/pipeline/OPPORTUNITY-MANAGEMENT.md`
- `docs/pipeline/METRICS.md`

## Histórico de Revisão
| Data | Autor | Descrição |
|------|-------|-----------|
| 2026-07-15 | System | Criação do contexto |
