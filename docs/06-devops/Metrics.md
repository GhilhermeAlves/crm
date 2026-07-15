# Metrics — Métricas

## Índice

- [Objetivo](#objetivo)
- [Métricas de Sistema](#métricas-de-sistema)
- [Métricas de Negócio](#métricas-de-negócio)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar as métricas coletadas e monitoradas.

## Métricas de Sistema

### The Four Golden Signals (Google SRE)

| Sinal | Métrica | Target |
|---|---|---|
| Latency | Response time p95 | < 200ms |
| Traffic | Requests per second | Monitorar |
| Errors | Error rate (5xx) | < 0.1% |
| Saturation | CPU/Memory usage | < 80% |

### Métricas JVM

| Métrica | Descrição |
|---|---|
| jvm_memory_used_bytes | Uso de memória heap |
| jvm_gc_pause_seconds | Tempo de garbage collection |
| jvm_threads_live_threads | Threads ativas |
| jvm_buffer_pool_used_bytes | Buffer pool |

### Métricas HTTP

| Métrica | Descrição |
|---|---|
| http_server_requests_seconds | Duração de requests |
| http_server_requests_bytes_received | Tamanho recebido |
| http_server_requests_bytes_sent | Tamanho enviado |

### Métricas Database

| Métrica | Descrição |
|---|---|
| hikaricp_connections_active | Conexões ativas |
| hikaricp_connections_pending | Conexões pendentes |
| hikaricp_connections_timeout_total | Timeouts de conexão |

### Métricas Redis

| Métrica | Descrição |
|---|---|
| redis_commands_duration_seconds | Duração de comandos |
| redis_connections_active | Conexões ativas |

## Métricas de Negócio

| Métrica | Descrição | Target |
|---|---|---|
| messages_sent_total | Total de mensagens enviadas | Monitorar |
| messages_received_total | Total de mensagens recebidas | Monitorar |
| active_conversations | Conversas ativas agora | Monitorar |
| leads_created_total | Leads criados hoje | Monitorar |
| opportunities_won_total | Oportunidades ganhas | Monitorar |
| campaign_delivery_rate | Taxa de entrega de campanha | > 95% |
| whatsapp_connection_status | Status da conexão WhatsApp | 1 (conectado) |

## Responsabilidades

- Coletar métricas de todos os serviços
- Criar dashboards para visualização
- Configurar alertas para anomalias
- Revisar métricas regularmente

## Dependências

- [Monitoring.md](./Monitoring.md) — Stack de monitoramento
- [06-devops/Kubernetes.md](../06-devops/Kubernetes.md) — Infraestrutura

## Regras

- Todas as métricas devem ter labels de tenant
- Métricas devem ser expostas via /actuator/prometheus
- Retenção de métricas: 30 dias (raw), 1 ano (aggregated)
- Dashboards são versionados em código

## Futuras Melhorias

- SLO/SLI tracking automatizado
- Custom business metrics
- Cost per tenant metrics
- Performance benchmarking automatizado

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
