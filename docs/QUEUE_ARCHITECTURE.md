# Queue Architecture

## Objetivo

Documentar a arquitetura de filas do CRM SaaS Omnichannel utilizando RabbitMQ, incluindo tipos de exchanges, convenções de nomenclatura, routing keys, dead letter queues, mecanismos de retry, filas de prioridade, grupos de consumers e monitoramento.

## Escopo

- Tipos de exchanges (direct, topic, fanout, headers)
- Convenções de nomenclatura de filas e routing keys
- Dead letter queues (DLQ) e tratamento de mensagens falhas
- Mecanismos de retry com backoff exponencial
- Filas de prioridade
- Grupos de consumers e balanceamento de carga
- Monitoramento e alertas

## Responsabilidades

| Papel | Responsabilidade |
|---|---|
| Backend Developer | Definir publishers e consumers das filas |
| Architect | Projetar topologia de exchanges e filas |
| DevOps | Instalar, configurar e monitorar RabbitMQ |
| QA | Testar cenários de retry, DLQ e prioridade |

## Fluxos

### Visão Geral da Topologia

```mermaid
mindmap
  root((RabbitMQ Topology))
    Exchanges
      Direct
        crm.events.direct
        Rotas exatas por entity
      Topic
        crm.events.topic
        Padrões flexíveis com wildcards
      Fanout
        crm.events.broadcast
        Broadcast para todos consumers
      Headers
        crm.events.headers
        Match por atributos do header
    Filas
      crm.queue.contacts
      crm.queue.messages
      crm.queue.notifications
      crm.queue.billing
      crm.queue.analytics
      crm.queue.email
      DLQ
        crm.dlq.contacts
        crm.dlq.messages
        crm.dlq.notifications
```

### Tipos de Exchange e Uso

```mermaid
flowchart TD
    A[Mensagem publicada] --> B{Tipo do Exchange}
    B -->|Direct| C[Routing key exata]
    C --> C1[crm.contact.created → crm.queue.contacts]
    C --> C2[crm.message.sent → crm.queue.messages]
    B -->|Topic| D[Routing key com wildcards]
    D --> D1[crm.contact.* → todas filas de contatos]
    D --> D2[crm.*.created → todas filas de criação]
    B -->|Fanout| E[Broadcast para todas filas]
    E --> E1[crm.queue.notifications]
    E --> E2[crm.queue.analytics]
    E --> E3[crm.queue.audit]
    B -->|Headers| F[Match por headers]
    F --> F1[priority: high → crm.queue.priority]
    F --> F2[region: br → crm.queue.regional]
```

### Convenções de Nomenclatura

```
Exchange:
  crm.events.{type}
  Exemplos:
    crm.events.direct     → Eventos com routing exato
    crm.events.topic      → Eventos com padrões flexíveis
    crm.events.broadcast  → Eventos broadcast

Queue:
  crm.queue.{domain}
  Exemplos:
    crm.queue.contacts      → Processamento de contatos
    crm.queue.messages      → Envio de mensagens
    crm.queue.notifications → Notificações push/email
    crm.queue.billing       → Processamento de cobrança
    crm.queue.analytics     → Métricas e analytics
    crm.queue.email         → Envio de emails transacionais

Routing Key:
  crm.{entity}.{action}
  Exemplos:
    crm.contact.created
    crm.contact.updated
    crm.message.sent
    crm.invoice.paid

Dead Letter Queue:
  crm.dlq.{domain}
  Exemplos:
    crm.dlq.contacts
    crm.dlq.messages
```

### Fluxo de Publicação e Consumo

```mermaid
sequenceDiagram
    participant Publisher as Publisher (Spring Boot)
    participant Exchange as Exchange
    participant Queue as Queue
    participant Consumer as Consumer
    participant DB as PostgreSQL

    Publisher->>Exchange: publish(routing_key, message)
    Exchange->>Queue: Encaminhar conforme routing
    Queue->>Consumer: Entregar mensagem (push)
    Consumer->>Consumer: Processar mensagem
    alt Sucesso
        Consumer->>Queue: ACK
        Consumer->>DB: Salvar resultado
    else Falha retentável
        Consumer->>Queue: NACK (requeue com delay)
    else Falha definitiva
        Consumer->>Queue: NACK (send to DLQ)
    end
```

### Dead Letter Queue e Retry

```mermaid
flowchart TD
    A[Mensagem na fila principal] --> B[Tenta processar]
    B --> C{Sucesso?}
    C -->|Sim| D[ACK - Mensagem removida]
    C -->|Não| E{Tentativas < 3?}
    E -->|Sim| F[NACK com delay exponencial]
    F --> F1[Retry 1: 5 segundos]
    F1 --> F2[Retry 2: 30 segundos]
    F2 --> F3[Retry 3: 2 minutos]
    F3 --> B
    E -->|Não| G[NACK - Enviar para DLQ]
    G --> H[crm.dlq.{domain}]
    H --> I{Mensagem na DLQ}
    I -->|Investigação manual| J[Reprocessar manualmente]
    I -->|Descartar| K[ACK na DLQ]
    I -->|Reenviar para fila| L[Replay para fila principal]
```

### Mecanismo de Retry com Backoff Exponencial

```mermaid
sequenceDiagram
    participant P as Publisher
    participant Q as Fila Principal
    participant C as Consumer
    participant DLQ as Dead Letter Queue
    participant Retry as Retry Queue (x-delayed-message)

    P->>Q: Publicar mensagem
    Q->>C: Entregar (1ª tentativa)
    C->>C: Processar - Falha
    C->>Q: NACK (requeue)
    Q->>Retry: delay=5000ms
    Note over Retry: Aguardar 5s
    Retry->>Q: Re-entregar (2ª tentativa)
    Q->>C: Entregar
    C->>C: Processar - Falha
    C->>Q: NACK (requeue)
    Q->>Retry: delay=30000ms
    Note over Retry: Aguardar 30s
    Retry->>Q: Re-entregar (3ª tentativa)
    Q->>C: Entregar
    C->>C: Processar - Falha
    C->>DLQ: NACK (send to DLQ)
    Note over DLQ: Mensagem preservada para investigação
```

### Filas de Prioridade

```mermaid
flowchart TD
    A[Mensagem publicada com prioridade] --> B{Exchange headers}
    B -->|x-priority: high| C[crm.queue.priority_high]
    B -->|x-priority: normal| D[crm.queue.priority_normal]
    B -->|x-priority: low| E[crm.queue.priority_low]
    C --> F[Consumer prioritário - processa primeiro]
    D --> G[Consumer normal]
    E --> H[Consumer batch - processa em background]

    style C fill:#ff6b6b
    style D fill:#ffd93d
    style E fill:#6bcb77
```

### Consumer Groups e Load Balancing

```mermaid
flowchart LR
    A[Queue: crm.queue.messages] --> B[Consumer Instance 1]
    A --> C[Consumer Instance 2]
    A --> D[Consumer Instance 3]
    B --> E[Mensagens 1, 4, 7, 10...]
    C --> F[Mensagens 2, 5, 8, 11...]
    D --> G[Mensagens 3, 6, 9, 12...]

    Note1[Round-Robin Distribution] -.-> A
```

### Monitoramento

```mermaid
flowchart TD
    A[Métricas RabbitMQ] --> B[Queue Depth]
    A --> C[Consumer Count]
    A --> D[Message Rate pub/sub]
    A --> E[Memory Usage]
    A --> F[Connection Count]
    A --> G[DLQ Message Count]
    B --> H{Prometheus + Grafana}
    C --> H
    D --> H
    E --> H
    F --> H
    G --> H
    H --> I{Alertas}
    I -->|Queue depth > 1000| J[Alerta: fila acumulando]
    I -->|Consumer count = 0| K[Alerta: sem consumers]
    I -->|DLQ count > 0| L[Alerta: mensagens falhas]
    I -->|Memory > 80%| M[Alerta: memória Redis]
```

## Dependências

| Dependência | Tipo | Uso |
|---|---|---|
| RabbitMQ 3 | Infra | Broker de mensageria |
| PostgreSQL 16 | Infra | Persistência dos dados processados |
| Redis 7 | Infra | Cache e controle de deduplicação |
| Spring AMQP | Lib | Integração RabbitMQ com Spring Boot |
| Prometheus | Infra | Coleta de métricas do RabbitMQ |
| Grafana | Infra | Dashboards de monitoramento |

## Boas Práticas

- **Idempotência**: Todos os consumers devem ser idempotentes. Usar message ID para deduplicação.
- **DLQ sempre presente**: Toda fila deve ter uma DLQ associada.
- **Dead lettering com TTL**: Configurar TTL nas filas DLQ para evitar acumulação infinita.
- **Prefetch limit**: Configurar prefetch count adequado por consumer para não sobrecarregar.
- **Ack/Nack explícito**: Sempre confirmar processamento explicitamente. Nunca auto-ack.
- **Message TTL**: Definir TTL nas mensagens para evitar processamento de dados obsoletos.
- **Durable queues**: Todas as filas e exchanges devem ser duráveis (durable=true).
- **Monitoring ativo**: Monitorar depth de filas, taxa de mensagens e DLQ em tempo real.
- **Batch para alta volumetria**: Usar batch consumers para filas de analytics e métricas.
- **Segregação por domínio**: Cada domínio do negócio deve ter suas filas dedicadas.

## Referências

- [RabbitMQ Documentation](https://www.rabbitmq.com/docs)
- [RabbitMQ Dead Letter Exchanges](https://www.rabbitmq.com/docs/dlx)
- [Spring AMQP Documentation](https://docs.spring.io/spring-amqp/reference/)
- [RabbitMQ Monitoring](https://www.rabbitmq.com/docs/monitoring)

## Histórico de Revisão

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0 | 15/07/2026 | Paulo Alves | Criação inicial do documento |
