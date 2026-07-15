# Dashboard — Página do Dashboard

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Layout](#layout)
- [Widgets](#widgets)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar a página principal do dashboard, incluindo layout, widgets e dados exibidos.

## Descrição

O dashboard é a primeira tela do CRM. Exibe KPIs, gráficos e indicadores em tempo real. É totalmente responsivo e customizável.

## Layout

```
┌──────────────────────────────────────────────┐
│  KPI Cards (4 colunas)                       │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐        │
│  │Leads │ │Conv. │ │Pipe. │ │Win % │        │
│  └──────┘ └──────┘ └──────┘ └──────┘        │
├──────────────────────────────────────────────┤
│  Gráficos (2 colunas)                        │
│  ┌─────────────────┐ ┌─────────────────┐     │
│  │ Pipeline Funnel  │ │ Leads by Origin │     │
│  └─────────────────┘ └─────────────────┘     │
├──────────────────────────────────────────────┤
│  Tabelas (2 colunas)                         │
│  ┌─────────────────┐ ┌─────────────────┐     │
│  │ Recent Leads    │ │ Active Convos   │     │
│  └─────────────────┘ └─────────────────┘     │
└──────────────────────────────────────────────┘
```

## Widgets

### KPI Cards

| KPI | Descrição | Ícone | Cor |
|---|---|---|---|
| Leads Hoje | Novos leads do dia | Users | Blue |
| Conversas Ativas | Conversas abertas | MessageSquare | Green |
| Pipeline | Valor total no funil | DollarSign | Purple |
| Win Rate | % de oportunidades ganhas | TrendingUp | Green/Red |

### Gráficos

| Gráfico | Tipo | Dados |
|---|---|---|
| Pipeline Funnel | Bar chart | Oportunidades por estágio |
| Leads by Origin | Pie chart | Leads por canal |
| Messages Volume | Line chart | Mensagens por dia |
| Revenue Trend | Area chart | Receita por mês |

### Tabelas

| Tabela | Colunas |
|---|---|
| Recent Leads | Nome, Email, Origem, Score, Data |
| Active Conversations | Contato, Última msg, Status, Agente |

## Responsabilidades

- Exibir dados atualizados em tempo real
- Carregar rapidamente (< 2s)
- Ser responsivo em todos os tamanhos
- Suportar dark/light mode

## Dependências

- [01-backend/Dashboard.md](../01-backend/Dashboard.md) — API de dados
- [Charts.md](./Charts.md) — Componentes de gráfico
- [Tables.md](./Tables.md) — Componentes de tabela

## Regras

- KPIs são cacheados por 5 minutos
- Gráficos usam skeleton loading
- Dados são atualizados via polling (30s) ou WebSocket
- Dashboard deve funcionar sem JavaScript (SSR fallback)

## Futuras Melhorias

- Dashboard customizável (drag-and-drop de widgets)
- KPIs customizáveis por usuário
- Comparativo com período anterior
- Export de dashboard em PDF
- Insights automáticos com IA
- Fullscreen mode

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
