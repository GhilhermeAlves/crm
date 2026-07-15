# Reports — Relatórios

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Responsabilidades](#responsabilidades)
- [Endpoints](#endpoints)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar o módulo de relatórios, incluindo tipos, geração e exportação.

## Descrição

Relatórios permitem analisar dados do CRM de forma estruturada. Suportam filtros avançados, exportação em múltiplos formatos e agendamento automático.

## Responsabilidades

- Gerar relatórios de vendas, comunicação e equipe
- Suportar filtros avançados (período, equipe, canal)
- Exportar em PDF, CSV e Excel
- Agendar relatórios recorrentes
- Salvar relatórios favoritos

## Tipos de Relatório

| Tipo | Descrição |
|---|---|
| Vendas por período | Leads, oportunidades, receita |
| Performance da equipe | Métricas por agente |
| Conversas | Volume, tempo de resposta, satisfação |
| Campanhas | ROI, conversão, opt-out |
| Pipeline | Funil, velocity, previsão |
| Contatos | Crescimento, segmentação, origem |
| Automações | Execuções, erros, eficiência |

## Endpoints

| Método | Endpoint | Descrição | Permissão |
|---|---|---|---|
| GET | `/api/v1/reports` | Listar relatórios | `report:read` |
| POST | `/api/v1/reports/generate` | Gerar relatório | `report:read` |
| GET | `/api/v1/reports/{id}` | Buscar relatório | `report:read` |
| GET | `/api/v1/reports/{id}/export` | Exportar relatório | `report:read` |
| POST | `/api/v1/reports/schedule` | Agendar relatório | `report:write` |
| GET | `/api/v1/reports/scheduled` | Relatórios agendados | `report:read` |

## Dependências

- [Cache.md](./Cache.md) — Cache de relatórios
- [FileStorage.md](./FileStorage.md) — Armazenamento de exportações
- [Scheduler.md](./Scheduler.md) — Relatórios agendados
- [Events.md](./Events.md) — Eventos de relatórios

## Regras

| # | Regra | Justificativa |
|---|---|---|
| R-001 | Relatórios são gerados assincronamente | Performance |
| R-002 | Exportações são mantidas por 7 dias | Armazenamento |
| R-003 | Máximo de filtros: 10 por relatório | Complexidade |
| R-004 | Período máximo: 12 meses | Performance |
| R-005 | Relatórios agendados rodam diariamente/semanalmente | Automatização |
| R-006 | Cache de relatórios: 1 hora | Performance |

## Futuras Melhorias

- Builder visual de relatórios
- Dashboards compartilháveis
- IA para insights automáticos
- Alertas baseados em métricas
- Relatórios com drill-down
- Export para BI tools

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
