# Playbook: Implementação do Módulo Dashboard

## Objetivo
Implementar o dashboard analítico: KPIs, gráficos, widgets em tempo real via WebSocket, e cache de dados para performance.

## Pré-requisitos
- Módulo Leads implementado (dados de leads)
- Módulo Pipeline implementado (dados de oportunidades)
- Módulo Chat implementado (dados de conversas)
- Módulo Contact implementado (dados de contatos)
- Sistema de cache configurado (Redis)
- WebSocket configurado

## Documentos que DEVEM ser lidos
- `docs/Dashboard.md`
- `docs/Analytics.md`
- `contexts/analytics-context.md`

## Arquivos que poderão ser alterados
### Backend
- `packages/backend/src/application/analytics/` — DashboardUseCase, GetKPIsUseCase, GetChartDataUseCase, GetActivityFeedUseCase
- `packages/backend/src/infrastructure/cache/` — CacheService (Redis), DashboardCacheKeys
- `packages/backend/src/infrastructure/persistence/` — DashboardRepository (queries analíticas)
- `packages/backend/src/presentation/rest/controller/DashboardController.ts`
- `packages/backend/src/presentation/websocket/` — DashboardGateway (atualizações real-time)

### Frontend
- `packages/frontend/src/components/dashboard/` — DashboardPage, KPICard, PipelineChart, LeadsChart, ActivityFeed, ContactStats, ConversionFunnel
- `packages/frontend/src/hooks/useDashboard.ts`
- `packages/frontend/src/app/(auth)/dashboard/` — Página principal do dashboard

## Arquivos proibidos
- `packages/backend/src/domain/` — Nenhuma entidade de domínio deve ser alterada
- `packages/frontend/src/components/pipeline/` — Pipeline components não devem ser alterados
- `packages/frontend/src/components/leads/` — Lead components não devem ser alterados

## Ordem de implementação
1. Definir queries analíticas: leads por status, pipeline por stage, conversas ativas
2. Implementar DashboardRepository com queries de agregação
3. Implementar CacheService com TTL de 5 minutos para KPIs
4. Implementar GetKPIsUseCase: total leads, leads novos (mês), valor pipeline, taxa conversão
5. Implementar GetChartDataUseCase: leads por fonte, pipeline por estágio, funil de conversão
6. Implementar GetActivityFeedUseCase: últimas atividades (mensagens, oportunidades movidas)
7. Implementar DashboardGateway para atualizações via WebSocket
8. Implementar DashboardController
9. Criar componentes frontend: KPI cards, gráficos
10. Criar hook useDashboard com polling + WebSocket
11. Montar layout do dashboard com drag-and-drop de widgets (opcional)

## Checklist Backend
- [ ] DashboardRepository: queries de agregação otimizadas (COUNT, SUM, AVG)
- [ ] KPIs: total de leads, leads novos no mês, leads convertidos no mês
- [ ] KPIs: valor total do pipeline, valor ponderado (score × value)
- [ ] KPIs: taxa de conversão lead → oportunidade
- [ ] KPIs: total de contatos, contatos novos no mês
- [ ] KPIs: conversas abertas, tempo médio de resposta
- [ ] Charts: leads por fonte (gráfico de barras/pizza)
- [ ] Charts: pipeline por estágio (gráfico de funil/kanban stats)
- [ ] Charts: evolução de leads ao longo do tempo (gráfico de linha)
- [ ] Charts: funil de conversão (lead → contacted → qualified → converted)
- [ ] ActivityFeed: últimas 20 atividades (mensagens, mudanças de stage, novos leads)
- [ ] CacheService: KPIs cacheados com TTL de 5 minutos
- [ ] CacheService: invalidação de cache quando dados mudam (lead criado, oportunidade movida)
- [ ] DashboardGateway: evento dashboard:update via WebSocket quando dados mudam
- [ ] DashboardController: GET /dashboard/kpis, GET /dashboard/charts, GET /dashboard/activity
- [ ] Multi-tenancy: todas as queries filtram por company_id
- [ ] Queries usam índices corretamente (EXPLAIN ANALYZE)

## Checklist Frontend
- [ ] DashboardPage: layout com grid de widgets
- [ ] KPICard: card com título, valor, variação (%), ícone
- [ ] PipelineChart: gráfico de barras ou funil do pipeline
- [ ] LeadsChart: gráfico de linha com evolução temporal
- [ ] LeadsBySourceChart: gráfico de pizza/barras por fonte
- [ ] ConversionFunnel: funil visual de conversão
- [ ] ActivityFeed: lista de atividades recentes com timestamps
- [ ] ContactStats: stats de contatos (total, novos, por empresa)
- [ ] Hook useDashboard: getKPIs, getCharts, getActivityFeed
- [ ] Auto-refresh: atualização periódica (polling a cada 30s) + WebSocket
- [ ] Loading states e skeleton screens
- [ ] Responsivo: funciona em desktop e mobile
- [ ] Filtro de período: hoje, 7 dias, 30 dias, 90 dias, personalizado

## Checklist Banco
- [ ] Queries de agregação otimizadas (evitar N+1)
- [ ] Índices para queries de dashboard: leads.company_id + created_at, opportunities.pipeline_id + stage_id, messages.conversation_id + created_at
- [ ] Materialized views para KPIs pesados (se necessário)
- [ ] Particionamento de tabelas por data (se volume justificar)

## Checklist Testes
- [ ] Testes unitários: KPI calculation (diversos cenários)
- [ ] Testes unitários: Cache TTL e invalidação
- [ ] Testes de integração: Dashboard queries retornam dados corretos
- [ ] Testes de integração: Cache funciona (hit/miss/invalidation)
- [ ] Testes de integração: WebSocket envia atualizações
- [ ] Testes de integração: Multi-tenancy filtra dados
- [ ] Testes de performance: queries completam em < 200ms
- [ ] Testes E2E: Dashboard carrega com dados → filtros funcionam

## Checklist Documentação
- [ ] Atualizar `docs/Dashboard.md` com KPIs, gráficos e endpoints
- [ ] Documentar queries de agregação
- [ ] Documentar estratégia de cache
- [ ] Documentar eventos WebSocket do dashboard

## Checklist Final
- [ ] Dashboard carrega com KPIs corretos
- [ ] Gráficos são renderizados com dados reais
- [ ] Cache de 5 minutos funciona (performance)
- [ ] WebSocket atualiza dados em tempo real
- [ ] Activity feed mostra atividades recentes
- [ ] Filtros de período funcionam
- [ ] Multi-tenancy isola dados por empresa
- [ ] Performance aceitável (< 500ms para carga completa)
- [ ] Todos os testes passam
