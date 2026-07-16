# Dashboard Context

## Resumo do Módulo
Dashboard com 7 KPIs principais: leads hoje, conversas ativas, valor do pipeline, win rate, revenue, msgs enviadas, tempo médio de resposta. Cache 5min + WebSocket.

## Objetivo
Fornecer visão consolidada da performance do CRM em tempo real.

## Responsabilidades
- 7 KPIs principais atualizados
- Cache com TTL de 5 minutos
- Updates em tempo real via WebSocket
- Filtros por período e equipe
- Widget layout configurável

## KPIs
| KPI | Descrição |
|-----|-----------|
| Leads Hoje | Leads capturados no dia |
| Conversas Ativas | Conversas com status Open |
| Valor Pipeline | Soma das oportunidades ativas |
| Win Rate | % de oportunidades ganhas |
| Revenue | Receita do período |
| Msgs Enviadas | Mensagens enviadas no período |
| Tempo Resposta | Média de primeira resposta |

## APIs Relacionadas
- `GET /dashboard` - KPIs consolidados
- `GET /dashboard/leads` - Métricas de leads
- `GET /dashboard/pipeline` - Métricas do pipeline
- `GET /dashboard/conversations` - Métricas de conversas
- `GET /dashboard/revenue` - Métricas de receita
- `GET /dashboard/team` - Métricas por equipe

## Componentes Frontend
- DashboardPage, DashboardWidget
- KPICard, ChartWidget
- FilterBar (período, equipe)
- RealTimeIndicator (WebSocket status)

## Componentes Backend
- `dashboard` module (Controllers, Services)
- `metrics` module (cálculo de KPIs)
- `cache` module (Redis, TTL 5min)
- WebSocket handler (push de updates)

## Eventos
- `DashboardMetricsUpdated` - KPIs recalculados
- `DashboardWidgetConfigChanged` - Layout alterado

## Permissões
- `dashboard:read` - Todos
- `dashboard:configure` - ADMIN, MANAGER
- `dashboard:team` - ADMIN, MANAGER

## Dependências
- **Cache** - Redis para cache de 5min
- **Events** - Eventos que atualizam KPIs

## Fluxo Resumido
1. Usuário acessa dashboard → cache verifica TTL
2. Cache hit → retorna dados; cache miss → calcula KPIs → armazena
3. Evento dispara → WebSocket notifica → KPIs atualizados

## Checklist de Implementação
- [ ] 7 KPIs implementados
- [ ] Cache Redis com TTL 5min
- [ ] WebSocket para updates real-time
- [ ] Filtros por período e equipe
- [ ] Layout configurável (widgets)
- [ ] Loading states e skeletons
- [ ] Responsivo (mobile)
- [ ] Export de dados (PDF/CSV)

## Checklist de Testes
- [ ] KPIs calculados corretamente
- [ ] Cache expira após 5min
- [ ] WebSocket entrega updates
- [ ] Filtros refletem nos KPIs
- [ ] Performance com muitos dados

## Documentação Oficial Relacionada
- `docs/dashboard/KPI-DEFINITIONS.md`
- `docs/dashboard/WIDGET-CONFIG.md`
- `docs/dashboard/CACHE-STRATEGY.md`

## Histórico de Revisão
| Data | Autor | Descrição |
|------|-------|-----------|
| 2026-07-15 | System | Criação do contexto |
