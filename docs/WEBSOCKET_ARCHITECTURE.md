# WebSocket Architecture

## Objetivo

Documentar a arquitetura WebSocket do CRM SaaS Omnichannel, cobrindo ciclo de vida da conexão, autenticação, salas/canais, tipos de eventos, estratégia de reconexão, heartbeat, escalabilidade com Redis Pub/Sub e fallback para polling.

## Escopo

- Ciclo de vida da conexão WebSocket
- Autenticação via JWT
- Rooms/Canais (multi-tenant)
- Tipos de eventos e protocolo de mensagens
- Estratégia de reconexão com backoff
- Heartbeat e detecção de conexões mortas
- Escalabilidade horizontal com Redis Pub/Sub
- Fallback para Server-Sent Events e polling

## Responsabilidades

| Papel | Responsabilidade |
|---|---|
| Backend Developer | Implementar WebSocket server e handler de eventos |
| Frontend Developer | Implementar cliente WebSocket com reconexão |
| DevOps | Configurar infraestrutura para conexões persistentes |
| QA | Testar cenários de reconexão, escalabilidade e fallback |

## Fluxos

### Ciclo de Vida da Conexão

```mermaid
stateDiagram-v2
    [*] --> Desconectado
    Desconectado --> Conectando : Abrir conexao
    Conectando --> Autenticando : TCP estabelecido
    Autenticando --> Conectado : JWT valido
    Autenticando --> Erro_Auth : JWT invalido
    Erro_Auth --> Desconectado : Fechar conexao
    Conectado --> Em_Sala : Join room
    Em_Sala --> Conectado : Leave room
    Conectado --> Reconectando : Conexao perdida
    Reconectando --> Conectando : Retry com backoff
    Conectando --> Desconectado : Max retries excedido
    Conectado --> Desconectado : Disconnect voluntario
    Desconectado --> [*]
```

### Fluxo de Autenticacao

```mermaid
sequenceDiagram
    participant C as Cliente (Next.js)
    participant S as WebSocket Server
    participant Auth as Auth Service
    participant Redis as Redis

    C->>S: WebSocket Connect (token=jwt)
    S->>Auth: Validar JWT
    alt JWT valido
        Auth-->>S: {userId, tenantId, role}
        S->>S: Criar sessao WebSocket
        S->>Redis: SET ws:session:{connId} userData TTL=24h
        S-->>C: connection_established
    else JWT invalido ou expirado
        Auth-->>S: Erro
        S-->>C: connection_rejected unauthorized
        S->>S: Fechar conexao
    end
```

### Salas e Canais (Multi-Tenant)

```mermaid
mindmap
  root((WebSocket Rooms))
    Tenant Room
      tenant:{tenantId}:all
      Todos usuarios do tenant
    User Room
      user:{userId}
      Notificacoes pessoais
    Contact Room
      tenant:{tenantId}:contact:{contactId}
      Interacoes de contato
    Channel Room
      tenant:{tenantId}:channel:{channelId}
      Mensagens de canal
    Broadcast Room
      broadcast:system
      Anuncios do sistema
```

### Join e Leave de Sala

```mermaid
sequenceDiagram
    participant C as Cliente
    participant S as WebSocket Server
    participant Redis as Redis Pub/Sub

    C->>S: join_room {room: tenant:123:contact:456}
    S->>S: Adicionar conexao a sala
    S->>Redis: SUBSCRIBE ws:room:tenant:123:contact:456
    S-->>C: room_joined {room, memberCount}
    Note over C,S: Cliente recebe eventos desta sala
    C->>S: leave_room {room: tenant:123:contact:456}
    S->>S: Remover conexao da sala
    S->>Redis: UNSUBSCRIBE ws:room:tenant:123:contact:456
    S-->>C: room_left {room}
```

### Tipos de Eventos

```mermaid
mindmap
  root((Event Types))
    bidirectional
      join_room
      leave_room
      ping
      pong
    server para client
      contact.created
      contact.updated
      message.received
      message.status.delivered
      message.status.read
      notification.new
      typing.indicator
      presence.update
      billing.plan.changed
    client para server
      message.send
      typing.start
      typing.stop
      contact.note.add
      room.subscribe
      room.unsubscribe
```

### Fluxo de Mensagem em Tempo Real

```mermaid
sequenceDiagram
    participant U1 as Usuario 1
    participant S as WebSocket Server
    participant Redis as Redis Pub/Sub
    participant MQ as RabbitMQ
    participant U2 as Usuario 2

    U1->>S: message.send {to: contact:456, content: text}
    S->>S: Validar permissao e rate limit
    S->>MQ: Publicar message.sent event
    MQ->>MQ: Processar (salvar, notificar canais)
    MQ->>Redis: PUBLICAR ws:room:tenant:123:contact:456
    Redis->>S: Entregar para subscribers
    S->>U2: message.received {from: user:111, content: text}
    S->>U1: message.status.delivered {msgId, status: delivered}
```

### Estrategia de Reconexao

```mermaid
flowchart TD
    A[Conexao perdida] --> B[Estado: Reconectando]
    B --> C[Tentativa 1 - aguardar 1s]
    C --> D{Sucesso?}
    D -->|Sim| E[Reautenticar com JWT valido]
    E --> F[Rejoin todas as salas]
    F --> G[Conexao restaurada]
    D -->|Nao| H[Tentativa 2 - aguardar 2s]
    H --> I{Sucesso?}
    I -->|Sim| E
    I -->|Nao| J[Tentativa 3 - aguardar 4s]
    J --> K{Sucesso?}
    K -->|Sim| E
    K -->|Nao| L[Tentativa 4 - aguardar 8s]
    L --> M{Sucesso?}
    M -->|Sim| E
    M -->|Nao| N[Tentativa 5 - aguardar 16s]
    N --> O{Sucesso?}
    O -->|Sim| E
    O -->|Nao| P[Max retries atingido]
    P --> Q[Fallback para polling]
    Q --> R[Tentar reconexao WebSocket a cada 30s]
```

### Heartbeat

```mermaid
sequenceDiagram
    participant C as Cliente
    participant S as WebSocket Server

    loop A cada 30 segundos
        C->>S: ping {timestamp}
        S->>S: Atualizar last_heartbeat
        S-->>C: pong {timestamp, serverTime}
    end
    Note over C,S: Se 3 sem pong, cliente reconecta
    Note over C,S: Se server nao recebe ping em 60s, fecha conexao
```

### Escalabilidade com Redis Pub/Sub

```mermaid
flowchart TD
    subgraph Instances
        I1[WS Server Instance 1]
        I2[WS Server Instance 2]
        I3[WS Server Instance 3]
    end

    subgraph Redis Cluster
        PS[Redis Pub/Sub]
        Sessions[Redis Sessions Store]
    end

    U1[Usuario 1 - Instance 1] -->|message.send| I1
    I1 -->|PUBLICAR| PS
    PS -->|SUBSCRIBE| I2
    PS -->|SUBSCRIBE| I3
    I2 -->|Entregar| U2[Usuario 2 - Instance 2]
    I3 -->|Entregar| U3[Usuario 3 - Instance 3]
    I1 --> Sessions
    I2 --> Sessions
    I3 --> Sessions
```

### Fallback para Polling

```mermaid
sequenceDiagram
    participant C as Cliente
    participant BE as Backend REST API

    Note over C,BE: WebSocket indisponivel
    C->>BE: GET /api/events?since=lastEventId (polling)
    BE-->>C: {events: [], latestId: 123}
    Note over C,BE: Aguardar 5 segundos
    C->>BE: GET /api/events?since=123
    BE-->>C: {events: [msg1, msg2], latestId: 125}
    C->>C: Processar eventos
    Note over C,BE: WebSocket volta disponivel
    C->>C: Reconectar WebSocket e abandonar polling
```

### Fluxo Completo de Conexao

```mermaid
flowchart TD
    A[App inicia] --> B[Tentar conectar WebSocket]
    B --> C{Sucesso?}
    C -->|Sim| D[Autenticar com JWT]
    D --> E{JWT valido?}
    E -->|Sim| F[Conexao ativa]
    F --> G[Join salas do usuario]
    G --> H[Escutar eventos]
    H --> I{Conexao ativa?}
    I -->|Sim| J[Heartbeat periodicamente]
    J --> I
    I -->|Nao| K[Tentar reconexao com backoff]
    K --> L{Max retries?}
    L -->|Nao| B
    L -->|Sim| M[Fallback para polling]
    M --> N[Tentar WebSocket a cada 30s]
    N --> B
    C -->|Nao| O[Tentar reconexao com backoff]
    O --> P{Max retries?}
    P -->|Nao| B
    P -->|Sim| M
    E -->|Nao| Q[Exibir erro de autenticacao]
    Q --> R[Redirecionar para login]
```

## Dependencias

| Dependencia | Tipo | Uso |
|---|---|---|
| Spring WebSocket | Lib | Servidor WebSocket no backend |
| STOMP | Protocolo | Protocolo de mensageria sobre WebSocket |
| Redis 7 | Infra | Pub/Sub para escalabilidade entre instancias |
| PostgreSQL 16 | Infra | Persistencia de mensagens e estado |
| RabbitMQ 3 | Infra | Eventos assincronos (entrega de mensagens) |
| Next.js WebSocket | Lib | Cliente WebSocket no frontend |
| Redis sessions store | Infra | Sessoes WebSocket para reconexao |

## Boas Praticas

- **JWT no connect**: Enviar JWT como query param ou header na conexao inicial. Nunca confiar em conexao sem autenticacao.
- **Rejoin automatico**: Cliente deve rejoin todas as salas apos reconexao bem-sucedida.
- **Heartbeat bidirecional**: Tanto cliente quanto server devem enviar ping periodicamente.
- **Rate limiting**: Limitar taxa de mensagens por conexao para evitar abuso.
- **Message size limit**: Limitar tamanho das mensagens (ex: 64KB max).
- **Graceful shutdown**: Notificar clientes antes de desligar uma instancia do servidor.
- **Connection pooling**: Limitar numero maximo de conexoes por instancia.
- **Multi-tenant isolation**: Salas sempre namespaceadas por tenant para evitar vazamento de dados.
- **Fallback obrigatorio**: Todo cliente deve ter fallback para polling quando WebSocket falhar.
- **Monitoring**: Monitorar numero de conexoes ativas, latencia de entrega e taxa de reconexao.

## Referencias

- [WebSocket RFC 6455](https://datatracker.ietf.org/doc/html/rfc6455)
- [Spring WebSocket Documentation](https://docs.spring.io/spring-framework/reference/web/websocket.html)
- [STOMP Protocol](https://stomp.github.io/stomp-specification-1.2.html)
- [Redis Pub/Sub](https://redis.io/docs/interact/pubsub/)
- [Socket.IO Fallback](https://socket.io/docs/v4/)

## Historico de Revisao

| Versao | Data | Autor | Descricao |
|---|---|---|---|
| 1.0 | 15/07/2026 | Paulo Alves | Criacao inicial do documento |
