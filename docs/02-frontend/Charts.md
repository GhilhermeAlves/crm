# Charts — Gráficos

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Tipos de Gráfico](#tipos-de-gráfico)
- [Componentes](#componentes)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar os componentes de gráficos utilizados no dashboard e relatórios.

## Descrição

Gráficos são construídos com Recharts, uma biblioteca React para gráficos baseada em D3. Todos os gráficos são responsivos e suportam dark/light mode.

## Tipos de Gráfico

| Tipo | Uso | Componente |
|---|---|---|
| Bar Chart | Pipeline funnel, comparações | `BarChart` |
| Line Chart | Tendências ao longo do tempo | `LineChart` |
| Area Chart | Volume acumulado | `AreaChart` |
| Pie Chart | Distribuição percentual | `PieChart` |
| Donut Chart | KPI circular | `PieChart innerRadius` |
| Funnel | Funil de vendas | Custom |

## Componentes

| Componente | Descrição |
|---|---|
| MetricChart | Gráfico de métricas genérico |
| PipelineFunnel | Funil do pipeline |
| TrendChart | Gráfico de tendências |
| DistributionChart | Gráfico de distribuição |
| Sparkline | Mini gráfico inline |

## Responsabilidades

- Visualizar dados de forma clara e intuitiva
- Suportar interação (hover, click)
- Ser responsivo
- Suportar dark/light mode
- Performar com muitos dados

## Dependências

- [01-backend/Dashboard.md](../01-backend/Dashboard.md) — Dados
- [Theme.md](./Theme.md) — Cores do gráfico

## Regras

- Gráficos devem ter legenda
- Cores devem ser acessíveis (contraste)
- Dados devem ser formatados (R$, %, etc.)
- Loading state é obrigatório
- Empty state quando sem dados
- Tooltips informativos no hover

## Futuras Melhorias

- Gráficos interativos (zoom, pan)
- Drill-down em gráficos
- Export de gráficos como imagem
- Animações suaves
- Gráficos 3D (quando necessário)

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
