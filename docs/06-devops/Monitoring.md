# Monitoring — Monitoramento

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Stack](#stack)
- [Dashboards](#dashboards)
- [Alertas](#alertas)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar a stack de monitoramento e observabilidade.

## Descrição

Monitoramento usa Prometheus para métricas, Grafana para dashboards e alertas, e Loki para logs.

## Stack

| Componente | Tecnologia | Finalidade |
|---|---|---|
| Metrics | Prometheus | Coleta de métricas |
| Dashboards | Grafana | Visualização |
| Logs | Loki + Promtail | Logs centralizados |
| Tracing | Jaeger / Tempo | Distributed tracing |
| Alerting | Alertmanager | Notificações |

## Dashboards

### Application Dashboard

- Request rate (req/s)
- Response time (p50, p95, p99)
- Error rate (4xx, 5xx)
- Active connections

### Infrastructure Dashboard

- CPU usage por container
- Memory usage por container
- Disk I/O
- Network I/O

### Database Dashboard

- Query latency
- Connection pool usage
- Cache hit ratio
- Replication lag

### Business Dashboard

- Messages sent/received
- Active conversations
- Lead conversion rate
- Campaign delivery rate

## Alertas

| Alerta | Condição | Severidade |
|---|---|---|
| HighErrorRate | 5xx > 5% por 5min | Critical |
| HighLatency | p95 > 2s por 5min | Warning |
| DatabaseDown | Connection refused | Critical |
| HighMemory | Memory > 90% | Warning |
| DiskSpaceLow | Disk > 85% | Warning |
| QueueBacklog | Messages > 10000 | Warning |
| SSLExpiring | Cert < 30 days | Info |

## Responsabilidades

- Monitorar saúde do sistema
- Detectar e alertar sobre problemas
- Fornecer visibilidade de performance
- Suportar troubleshooting

## Dependências

- [Logs.md](./Logs.md) — Logs
- [Metrics.md](./Metrics.md) — Métricas
- [06-devops/Kubernetes.md](../06-devops/Kubernetes.md) — Infraestrutura

## Regras

- Todos os serviços devem expor métricas
- Alertas devem ter runbook associado
- Dashboards devem ser revisados trimestralmente
- Métricas de negócio e sistema são equally importantes

## Futuras Melhorias

- AIOps para detecção de anomalias
- SLO/SLI tracking
- Cost monitoring
- Chaos engineering

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
