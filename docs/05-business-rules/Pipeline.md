# Pipeline — Regras de Pipeline

## Índice

- [Objetivo](#objetivo)
- [Regras de Pipeline](#regras-de-pipeline)
- [Regras de Oportunidade](#regras-de-oportunidade)
- [Regras de Transição](#regras-de-transição)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar regras de negócio do pipeline de vendas.

## Regras de Pipeline

| # | Regra | Justificativa |
|---|---|---|
| P-001 | Company pode ter múltiplos pipelines | Diferentes produtos/linhas |
| P-002 | Pipeline deve ter no mínimo 2 estágios | Fluxo básico |
| P-003 | Pipeline deve ter no máximo 15 estágios | Complexidade |
| P-004 | Estágios são ordenados sequencialmente | Fluxo linear |

## Regras de Oportunidade

| # | Regra | Justificativa |
|---|---|---|
| P-010 | Oportunidade deve ter título e valor | Dados mínimos |
| P-011 | Valor é obrigatório para métricas | Cálculos |
| P-012 | Oportunidade pode ser atribuída a um agente | Controle |
| P-013 | Probability é definida por estágio | Previsão |
| P-014 | Expected close date é opcional | Flexibilidade |
| P-015 | Oportunidade pode ter tags e notas | Contexto |

## Regras de Transição

| # | Regra | Justificativa |
|---|---|---|
| P-020 | Oportunidade só pode avançar ou retroceder 1 estágio | Controle |
| P-021 | Won/Lost só no último estágio | Fluxo |
| P-022 | Motivo da perda é obrigatório ao marcar LOST | Análise |
| P-023 | Won converte Lead em Customer | Fluxo |
| P-024 | Histórico de transições é imutável | Auditoria |

## Regras de Métricas

| # | Regra | Justificativa |
|---|---|---|
| P-030 | Win rate = Won / (Won + Lost) | KPI |
| P-031 | Ciclo médio = média de dias entre criação e fechamento | KPI |
| P-032 | Pipeline value = soma dos valores por estágio | KPI |
| P-033 | Forecast = valor * probability | Previsão |

## Responsabilidades

- Validar transições no backend
- Atualizar métricas em tempo real
- Manter histórico completo

## Dependências

- [01-backend/Pipeline.md](../01-backend/Pipeline.md) — Implementação
- [01-backend/Stages.md](../01-backend/Stages.md) — Estágios
- [01-backend/Kanban.md](../01-backend/Kanban.md) — Kanban

## Futuras Melhorias

- Aprovações em estágios
- SLA por estágio
- Previsão com IA
- Pipeline de renovação

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
