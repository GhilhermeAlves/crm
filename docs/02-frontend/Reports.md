# Reports — Módulo de Relatórios (Frontend)

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Páginas](#páginas)
- [Componentes](#componentes)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar as páginas e componentes do módulo de relatórios.

## Descrição

O módulo de relatórios permite visualizar e exportar dados do CRM em formatos estruturados. Suporta filtros avançados, agendamento e múltiplos formatos de exportação.

## Páginas

### Lista de Relatórios (`/reports`)

- Cards com tipos de relatório disponíveis
- Relatórios recentes
- Relatórios agendados

### Geração de Relatório (`/reports/generate`)

- Seleção do tipo de relatório
- Filtros avançados (período, equipe, canal)
- Preview antes de gerar
- Export em PDF, CSV, Excel

## Componentes

| Componente | Descrição |
|---|---|
| ReportCard | Card do tipo de relatório |
| ReportBuilder | Builder de filtros |
| ReportPreview | Preview antes de exportar |
| ReportCharts | Gráficos do relatório |
| ExportButton | Botões de export |
| ScheduledReports | Relatórios agendados |

## Responsabilidades

- Gerar relatórios sob demanda
- Exportar em múltiplos formatos
- Agendar relatórios recorrentes
- Salvar relatórios favoritos

## Dependências

- [01-backend/Reports.md](../01-backend/Reports.md) — API de relatórios
- [Charts.md](./Charts.md) — Gráficos

## Regras

- Relatórios são gerados assincronamente
- Exportações ficam disponíveis por 7 dias
- Período máximo: 12 meses
- Preview limitado a 100 linhas

## Futuras Melhorias

- Builder visual de relatórios
- Dashboards compartilháveis
- IA para insights automáticos
- Export para BI tools

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
