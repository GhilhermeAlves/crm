# Kanban — Quadro Kanban

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Responsabilidades](#responsabilidades)
- [Fluxo](#fluxo)
- [Endpoints](#endpoints)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar a funcionalidade de visualização kanban do pipeline de vendas.

## Descrição

O kanban é uma representação visual do pipeline onde cada coluna representa um estágio. Oportunidades são exibidas como cards que podem ser arrastados entre colunas. Suporta drag-and-drop, filtros e visualização personalizada.

## Responsabilidades

- Renderizar oportunidades organizadas por estágio
- Suportar drag-and-drop para movimentação
- Filtrar por responsável, período, valor
- Exibir métricas por coluna (quantidade, valor total)
- Atualização em tempo real via WebSocket

## Fluxo

### Drag and Drop

```
1. Usuário arrasta card de uma coluna para outra
        │
2. Frontend envia request POST /move
        │
3. Backend valida transição
        │
4. Oportunidade é atualizada
        │
5. WebSocket notifica outros usuários
        │
6. Dashboard é recalculado
```

### Atualização em Tempo Real

```
1. Oportunidade é movida por outro usuário
        │
2. Evento OpportunityMoved é publicado
        │
3. WebSocket broadcast para empresa
        │
4. Frontend atualiza kanban automaticamente
```

## Endpoints

| Método | Endpoint | Descrição | Permissão |
|---|---|---|---|
| GET | `/api/v1/kanban?pipeline={id}` | Dados do kanban | `pipeline:read` |
| POST | `/api/v1/kanban/move` | Mover oportunidade | `pipeline:write` |
| GET | `/api/v1/kanban/metrics?pipeline={id}` | Métricas por coluna | `pipeline:read` |

## Dependências

- [Pipeline.md](./Pipeline.md) — Dados do pipeline
- [Stages.md](./Stages.md) — Estágios como colunas
- [02-frontend/Kanban.md](../02-frontend/Kanban.md) — Componente frontend

## Regras

- Movimentação só permite próximo estágio ou anterior
- Apenas responsável pode mover (ou admin)
- Cards exibem: nome, valor, responsável, dias no estágio
- Colunas mostram: quantidade total, valor total
- Cores por prioridade: vermelho (hot), amarelo (warm), cinza (cold)

## Futuras Melhorias

- Kanban para outras áreas (suporte, projetos)
- WIP limits (work in progress) por coluna
- Sub-status dentro de estágios
- Automações no drag-and-drop
- Kanban com swimlanes por responsável
- Modo lista alternativo ao kanban

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
