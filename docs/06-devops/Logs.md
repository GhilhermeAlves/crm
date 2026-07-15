# Logs — Sistema de Logs (DevOps)

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Stack](#stack)
- [Formato](#formato)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar a infraestrutura de logs centralizados.

## Descrição

Logs são coletados via Promtail, armazenados em Loki e visualizados no Grafana.

## Stack

| Componente | Tecnologia | Finalidade |
|---|---|---|
| Collection | Promtail | Agent de coleta |
| Storage | Loki | Armazenamento |
| Visualization | Grafana | Busca e dashboards |
| Alerting | Grafana Alerting | Alertas baseados em logs |

## Formato

```json
{
  "timestamp": "2026-07-15T10:30:00.000Z",
  "level": "INFO",
  "service": "crm-backend",
  "traceId": "abc-123",
  "message": "Request processed",
  "httpMethod": "POST",
  "httpUrl": "/api/v1/leads",
  "httpStatusCode": 201,
  "duration": 45
}
```

## Responsabilidades

- Coletar logs de todos os serviços
- Centralizar para busca e análise
- Gerar alertas baseados em logs
- Manter retenção adequada

## Dependências

- [Monitoring.md](./Monitoring.md) — Stack de monitoramento
- [01-backend/Logs.md](../01-backend/Logs.md) — Logs do backend

## Regras

- Logs em formato JSON estruturado
- Retenção: 30 dias online, 1 ano archive
- Logs nunca devem conter dados sensíveis
- Trace ID propagado em todas as camadas
- Log level configurável por ambiente

## Futuras Melhorias

- OpenTelemetry para unified observability
- IA para detecção de anomalias
- Log-based metrics
- Real-time log streaming

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
