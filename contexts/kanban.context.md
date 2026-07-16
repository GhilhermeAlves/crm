# Kanban Context

## Resumo do Módulo
Board visual drag-and-drop para gestão de oportunidades por estágio. Métricas por coluna e atualizações em tempo real via WebSocket.

## Objetivo
Fornecer visualização intuitiva do pipeline com drag-and-drop e métricas em tempo real.

## Responsabilidades
- Board visual com drag-and-drop entre estágios
- Cards de oportunidade com dados resumidos
- Métricas por coluna (qtd, valor total)
- Real-time via WebSocket
- Cores por prioridade: vermelho=hot, amarelo=warm, cinza=cold

## Entidades Relacionadas
- Opportunity, Stage, Pipeline, Lead (scoring)

## APIs Relacionadas
- `GET /kanban/:pipelineId` - Board do pipeline
- `PUT /kanban/opportunities/:id/move` - Mover card (drag-and-drop)
- `GET /kanban/:pipelineId/metrics` - Métricas por estágio

## Cores por Prioridade
- **Vermelho** - Hot (score 80-100)
- **Amarelo** - Warm (score 50-79)
- **Cinza** - Cold (score 20-49)

## Componentes Frontend
- KanbanBoard, KanbanColumn, KanbanCard
- DragAndDropContext (react-beautiful-dnd)
- ColumnMetrics (qtd, valor)
- RealTimeIndicator (WebSocket status)

## Componentes Backend
- `kanban` module (Controllers, Services)
- WebSocket handler para updates em tempo real
- Integrado com Pipeline/Opportunity modules

## Eventos
- `KanbanCardMoved` - Card movido entre estágios
- `KanbanMetricsUpdated` - Métricas da coluna atualizadas
- `KanbanBoardSynced` - Sincronização WebSocket

## Permissões
- `kanban:read` - Todos
- `kanban:move` - ADMIN, MANAGER, AGENT

## Dependências
- **Pipeline** - Dados de oportunidades e estágios
- **Stages** - Configuração de estágios

## Fluxo Resumido
1. Usuário acessa board → carrega oportunidades por estágio → métricas calculadas
2. Usuário arrasta card → `PUT /kanban/opportunities/:id/move` → validação ±1 estágio
3. WebSocket notifica todos os usuários do board → atualização em tempo real

## Checklist de Implementação
- [ ] Board visual com drag-and-drop
- [ ] Cards com dados resumidos do lead
- [ ] Métricas por coluna (qtd, valor)
- [ ] Cores por prioridade (hot/warm/cold)
- [ ] WebSocket para updates real-time
- [ ] Validação ±1 estágio no move
- [ ] Loading states e skeleton
- [ ] Responsivo (mobile-friendly)

## Checklist de Testes
- [ ] Drag-and-drop move card corretamente
- [ ] Validação ±1 estágio rejeita movimentos inválidos
- [ ] Métricas atualizam após movimentação
- [ ] WebSocket entrega updates em tempo real
- [ ] Cores refletem prioridade correta

## Documentação Oficial Relacionada
- `docs/kanban/KANBAN-BOARD.md`
- `docs/kanban/REAL-TIME.md`

## Histórico de Revisão
| Data | Autor | Descrição |
|------|-------|-----------|
| 2026-07-15 | System | Criação do contexto |
