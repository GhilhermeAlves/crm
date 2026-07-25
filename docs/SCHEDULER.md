# Scheduler

## Objetivo

Documentar a arquitetura do agendador distribuído do CRM SaaS Omnichannel, responsável pela execução de jobs agendados (cron, one-time, delayed), coordenação em cluster, locking distribuído via Redis, políticas de retry e monitoramento.

## Escopo

- Tipos de jobs: cron (recorrente), one-time (execução única), delayed (execução com atraso)
- Locking distribuído via Redis para execução em cluster
- Fluxo de execução de jobs com idempotência
- Políticas de retry com backoff exponencial
- Priorização de jobs (CRITICAL, HIGH, NORMAL, LOW)
- Coordenação em cluster (leader election)
- Monitoramento e alertas
- Persistência de estado de jobs no PostgreSQL
- Integração com filas RabbitMQ para jobs assíncronos

## Responsabilidades

| Responsável | Responsabilidade |
|---|---|
| SchedulerService | Orquestração central: agendamento, execução, reexecução de jobs |
| JobRepository | Persistência de jobs e execuções no PostgreSQL |
| DistributedLockService | Locking distribuído via Redis; prevenção de execução concorrente |
| JobExecutionService | Execução efetiva do job; tratamento de timeout; registro de resultado |
| RetryPolicyService | Cálculo de próximo retry; aplicação de backoff exponencial |
| ClusterCoordinator | Leader election; distribuição de carga entre nós |
| JobPriorityQueue | Filas de prioridade para jobs pendentes |
| MonitoringService | Métricas de execução; alertas de falha; dashboard de jobs |

## Fluxos

### Fluxo Geral de Execução

```mermaid
sequenceDiagram
    participant Cron as Cron Trigger
    participant SS as SchedulerService
    participant JR as JobRepository
    participant DL as DistributedLockService
    participant JE as JobExecutionService
    participant RQ as RabbitMQ
    participant MS as MonitoringService

    Cron->>SS: Trigger agendado (ex: todo dia 08:00)
    SS->>JR: Busca jobs agendados para este horário
    JR-->>SS: Lista de jobs pendentes

    loop Para cada job
        SS->>DL: Tenta adquirir lock (job_id + timestamp)
        alt Lock adquirido
            DL-->>SS: LOCK_ACQUIRED
            SS->>JR: Atualiza status para RUNNING
            SS->>JE: Executa job com timeout configurado
            JE-->>SS: Resultado (SUCCESS / FAILED / TIMEOUT)
            alt Sucesso
                SS->>JR: Status: COMPLETED
                SS->>MS: Registra métrica de sucesso
            else Falha
                SS->>JR: Status: FAILED + incrementa retry_count
                SS->>MS: Alerta de falha
            end
            SS->>DL: Libera lock
        else Lock não adquirido
            DL-->>SS: LOCK_DENIED (outro nó executando)
            SS->>SS: Pula este job
        end
    end
```

### Fluxo de Job com Retry

```mermaid
flowchart TD
    A[Job executado] --> B{Sucesso?}
    B -->|Sim| C[Status: COMPLETED]
    B -->|Não| D{retry_count < max_retries?}
    D -->|Sim| E[Calcula delay: backoff exponencial]
    E --> F[Reagenda execução]
    F --> G[Status: RETRY_PENDING]
    D -->|Não| H[Status: DEAD]
    H --> I[Alerta admin]
    I --> J[Dead Letter:.job queue]

    subgraph Backoff Exponencial
        K[1ª tentativa: 10s]
        L[2ª tentativa: 30s]
        M[3ª tentativa: 90s]
        N[4ª tentativa: 270s]
    end
```

### Coordenação em Cluster

```mermaid
flowchart TD
    subgraph Cluster
        N1[Nó 1]
        N2[Nó 2]
        N3[Nó 3]
    end

    N1 -->|Heartbeat| Redis[(Redis)]
    N2 -->|Heartbeat| Redis
    N3 -->|Heartbeat| Redis

    Redis -->|Lease renewal| N1
    Redis -->|Lease renewal| N2
    Redis -->|Lease renewal| N3

    N1 -->|Leader election| N1
    N2 -->|Follower| N2
    N3 -->|Follower| N3

    N1 -->|Distribui jobs| RQ[RabbitMQ]
    N1 -->|Distribui jobs| RQ
```

### Job Prioritization

```mermaid
flowchart LR
    subgraph Filas de Prioridade
        P1[CRITICAL - SLA, segurança]
        P2[HIGH - automações, follow-ups]
        P3[NORMAL - relatórios, limpeza]
        P4[LOW - analytics, backup]
    end

    P1 -->|Pull| Exec[Job Executor]
    P2 -->|Pull| Exec
    P3 -->|Pull| Exec
    P4 -->|Pull| Exec

    Exec --> Result{Resultado}
    Result -->|OK| Done[COMPLETED]
    Result -->|Fail| Retry[Retry Policy]
```

### Monitoramento

```mermaid
flowchart TD
    A[Job Execution] --> B[Metricas]
    B --> C[Prometheus]
    C --> D[Grafana Dashboard]

    B --> E[Alertas]
    E --> F{Condição}
    F -->|Taxa de falha > 5%| G[Alerta CRITICAL]
    F -->|Job timeout > threshold| H[Alerta WARNING]
    F -->|DLQ > 10 items| I[Alerta WARNING]
    F -->|Lock renewal failed| J[Alerta CRITICAL]

    G --> K[PagerDuty / Slack]
    H --> K
    I --> K
    J --> K
```

## Dependências

| Dependência | Finalidade |
|---|---|
| PostgreSQL 16 | Persistência de jobs, execuções e configurações de agendamento |
| Redis 7 | Locking distribuído; leader election; rate limiting de jobs |
| RabbitMQ 3 | Filas de jobs assíncronos; dead letter queue |
| Spring Boot 3 | Framework de orquestração e scheduling |
| Java 25 | Linguagem de implementação |
| Spring Scheduler / Quartz | Agendamento de triggers cron e one-time |
| Prometheus + Grafana | Métricas e monitoramento de jobs |

## Boas Práticas

- **Idempotência**: todo job deve ser idempotente; reexecução do mesmo job com mesmo parâmetro não deve causar efeitos colaterais
- **Timeout configurável**: cada job deve ter um timeout máximo de execução; jobs que excedem o timeout devem ser interrompidos e marcados como TIMEOUT
- **Max retries**: definir número máximo de retries por tipo de job; excedido o limite, mover para DLQ
- **Backoff exponencial**: delay entre retries deve crescer exponencialmente (10s, 30s, 90s, 270s) com jitter
- **Distributed locking**: usar Redis SETNX com TTL para prevenir execução concorrente em cluster
- **Leader election**: apenas o nó leader deve executar jobs de manutenção (limpeza, backup, migração)
- **Logging estruturado**: logar job_id, tenant_id, duration, status em cada execução; não logar dados sensíveis
- **Monitoramento ativo**: alertar quando taxa de falha > 5%, DLQ > 10 items, ou lock renewal falhar
- **Priorização**: jobs CRITICAL (SLA, segurança) sempre têm prioridade sobre jobs de manutenção
- **Tenant isolation**: jobs devem ser sempre executados no contexto de um tenant; não executar cross-tenant
- **Cleanup automático**: jobs ONE_TIME completados devem ser removidos após 30 dias; job logs após 90 dias

## Referências

- Quartz Scheduler: https://www.quartz-scheduler.org/
- Redis Distributed Locks: https://redis.io/docs/manual/patterns/distributed-locks/
- Spring Task Scheduling: https://docs.spring.io/spring-framework/reference/integration/scheduling.html
- RabbitMQ Priority Queues: https://www.rabbitmq.com/docs/priority

## Histórico de Revisão

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0 | 2026-07-15 | Paulo Alves | Criação inicial do documento |
