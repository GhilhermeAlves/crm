# Dashboard — Dados do Dashboard

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

Documentar o backend do dashboard, incluindo dados, métricas e cache.

## Descrição

O dashboard é a primeira tela que o usuário vê ao acessar o CRM. Ele exibe KPIs, gráficos e indicadores em tempo real. Os dados são calculados via queries otimizadas e cacheados para performance.

## Responsabilidades

- Fornecer KPIs do dashboard
- Calcular métricas de vendas, comunicação e equipe
- Atualizar dados em tempo real
- Cache de métricas para performance
- Suportar filtros por período e equipe

## KPIs Principais

| KPI | Descrição | Atualização |
|---|---|---|
| Leads hoje | Novos leads do dia | Tempo real |
| Conversas ativas | Conversas abertas agora | Tempo real |
| Oportunidades no pipeline | Total em valor | 5 min |
| Win rate | % de oportunidades ganhas | 5 min |
| Receita do mês | Valor total ganho | 5 min |
| Mensagens enviadas | Total no período | 5 min |
| Tempo médio de resposta | SLA | 15 min |
| Agentes online | Total agora | Tempo real |

## Fluxo

```
1. Frontend solicita dados do dashboard
        │
2. Backend verifica cache
        │
3. Se cache hit → Retorna dados cacheados
   Se cache miss → Calcula e cacheia
4. Dados são retornados ao frontend
        │
5. WebSocket atualiza dados em tempo real
```

## Endpoints

| Método | Endpoint | Descrição | Permissão |
|---|---|---|---|
| GET | `/api/v1/dashboard/kpis` | KPIs principais | `dashboard:read` |
| GET | `/api/v1/dashboard/sales` | Métricas de vendas | `dashboard:read` |
| GET | `/api/v1/dashboard/conversations` | Métricas de chat | `dashboard:read` |
| GET | `/api/v1/dashboard/team` | Métricas da equipe | `dashboard:read` |
| GET | `/api/v1/dashboard/charts` | Dados para gráficos | `dashboard:read` |
| WS | `/ws/dashboard` | Atualizações tempo real | Autenticado |

## Dependências

- [Cache.md](./Cache.md) — Cache de dados
- [Events.md](./Events.md) — Eventos de atualização
- [02-frontend/Dashboard.md](../02-frontend/Dashboard.md) — Componente frontend

## Regras

- Dados do dashboard são cacheados por 5 minutos
- KPIs de tempo real usam WebSocket (sem cache)
- Filtros padrão: último mês, equipe completa
- Dados históricos mantidos por 12 meses
- Dashboard deve carregar em < 2 segundos

## Futuras Melhorias

- Dashboard customizável por usuário
- KPIs customizáveis
- Alertas visuais para anomalias
- Comparativo com período anterior
- Export de dashboard em PDF
- IA para insights automáticos

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
