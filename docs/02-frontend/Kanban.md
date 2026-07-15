# Kanban — Quadro Kanban (Frontend)

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Layout](#layout)
- [Componentes](#componentes)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar o componente de kanban para visualização do pipeline de vendas.

## Descrição

O kanban mostra oportunidades organizadas por estágio em colunas. Suporta drag-and-drop para movimentação, filtros e métricas por coluna.

## Layout

```
┌──────────┬──────────┬──────────┬──────────┬──────────┐
│  Novo    │ Contato  │ Qualif.  │ Proposta │ Ganho    │
│  Lead    │ Inicial  │          │ Enviada  │          │
├──────────┼──────────┼──────────┼──────────┼──────────┤
│ ┌──────┐ │ ┌──────┐ │          │ ┌──────┐ │ ┌──────┐ │
│ │Card 1│ │ │Card 3│ │          │ │Card 5│ │ │Card 7│ │
│ └──────┘ │ └──────┘ │          │ └──────┘ │ └──────┘ │
│ ┌──────┐ │          │          │ ┌──────┐ │          │
│ │Card 2│ │          │          │ │Card 6│ │          │
│ └──────┘ │          │          │ └──────┘ │          │
│          │          │          │          │          │
│ Qtd: 2   │ Qtd: 1   │ Qtd: 0   │ Qtd: 2   │ Qtd: 1   │
│ R$ 5.000 │ R$ 3.000 │ R$ 0     │ R$ 12.000│ R$ 8.000 │
└──────────┴──────────┴──────────┴──────────┴──────────┘
```

## Componentes

| Componente | Descrição |
|---|---|
| PipelineBoard | Container do kanban |
| StageColumn | Coluna de estágio |
| OpportunityCard | Card da oportunidade |
| OpportunityDetail | Detalhes (modal/sheet) |
| KanbanMetrics | Métricas por coluna |

## Responsabilidades

- Renderizar oportunidades por estágio
- Suportar drag-and-drop (@dnd-kit/core)
- Exibir métricas por coluna
- Filtrar por responsável, período
- Atualização em tempo real

## Dependências

- [01-backend/Kanban.md](../01-backend/Kanban.md) — API do kanban
- [01-backend/Pipeline.md](../01-backend/Pipeline.md) — API de pipeline

## Regras

- Cards mostram: nome, valor, responsável, dias no estágio
- Cores por prioridade: vermelho (hot), amarelo (warm), cinza (cold)
- Drag-and-drop só permite próximo/anterior estágio
- Métricas atualizadas após cada movimentação
- Loading skeleton enquanto carrega

## Futuras Melhorias

- Modo lista alternativo
- WIP limits por coluna
- Swimlanes por responsável
- Filtros salvos
- Keyboard shortcuts para drag-and-drop
- Modo fullscreen

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
