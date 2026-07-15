# Reports — Regras de Relatórios

## Índice

- [Objetivo](#objetivo)
- [Regras](#regras)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar regras de relatórios.

## Regras

| # | Regra | Justificativa |
|---|---|---|
| R-001 | Relatórios são gerados assincronamente | Performance |
| R-002 | Exportações ficam disponíveis por 7 dias | Armazenamento |
| R-003 | Período máximo: 12 meses | Performance |
| R-004 | Máximo de 10 filtros por relatório | Complexidade |
| R-005 | Relatórios agendados rodam diariamente/semanalmente | Automatização |
| R-006 | Cache de relatórios: 1 hora | Performance |

## Responsabilidades

- Gerar relatórios com dados precisos
- Respeitar limites de performance
- Manter exportações por período definido

## Dependências

- [01-backend/Reports.md](../01-backend/Reports.md) — Implementação
- [02-frontend/Reports.md](../02-frontend/Reports.md) — UI

## Futuras Melhorias

- Builder visual
- IA para insights
- Dashboards compartilháveis

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
