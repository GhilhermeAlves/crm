# Cache Strategy

## Objetivo

Documentar a estratégia de cache completa do CRM SaaS Omnichannel, incluindo padrões de utilização, convenções de nomenclatura, políticas de TTL, estratégias de invalidação, aquecimento, monitoramento e comportamento de fallback.

## Escopo

- Padrões de cache: Cache-Aside, Write-Through, Write-Behind
- Convenções de nomenclatura de chaves
- Políticas de TTL por entidade
- Estratégias de invalidação
- Cache warming (aquecimento)
- Monitoramento e métricas de cache
- Comportamento de fallback quando o cache falha

## Responsabilidades

| Papel | Responsabilidade |
|---|---|
| Backend Developer | Implementar lógica de cache nos repositórios e services |
| DevOps | Configurar Redis, monitorar memória e performance |
| Architect | Definir padrões e convenções de cache |
| QA | Testar cenários de cache hit, miss, invalidação e fallback |

## Fluxos

### Padrões de Cache

```mermaid
mindmap
  root((Cache Patterns))
    Cache-Aside
      Ler: Check cache → miss → DB → populate cache
      Escrever: DB → invalidate cache
      Mais usado no projeto
    Write-Through
      Escrever: Application → Cache → DB simultaneamente
      Consistência garantida
      Maior latência de escrita
    Write-Behind
      Escrever: Application → Cache → DB assíncrono
      Alta performance de escrita
      Risco de perda de dados
```

### Cache-Aside (Padrão Principal)

```mermaid
sequenceDiagram
    participant App as Application
    participant Cache as Redis
    participant DB as PostgreSQL

    App->>Cache: GET cache:key
    alt Cache Hit
        Cache-->>App: Dados serializados
    else Cache Miss
        Cache-->>App: null
        App->>DB: SELECT * FROM entity WHERE id = ?
        DB-->>App: Resultado
        App->>Cache: SET cache:key TTL
    end
    App-->>App: Retornar dados
```

### Write-Through

```mermaid
sequenceDiagram
    participant App as Application
    participant Cache as Redis
    participant DB as PostgreSQL

    App->>Cache: SET cache:key dados
    App->>DB: INSERT/UPDATE entity
    DB-->>App: Confirmação
    Note over Cache,DB: Cache e DB atualizados sincronamente
```

### Write-Behind (para métricas e analytics)

```mermaid
sequenceDiagram
    participant App as Application
    participant Cache as Redis
    participant Queue as RabbitMQ
    participant Worker as Background Worker
    participant DB as PostgreSQL

    App->>Cache: INCR metrics:daily:msg_count
    App->>Cache: LPUSH metrics:events payload
    Cache-->>App: OK
    App-->>App: Operação assíncrona, resposta imediata
    Queue->>Worker: Consumir eventos pendentes
    Worker->>DB: Batch INSERT/UPDATE
    DB-->>Worker: Confirmação
```

### Convenções de Nomenclatura de Chaves

```
Formato: {namespace}:{entity}:{identifier}:{field}

Exemplos:
  crm:contact:12345              → Dados de contato
  crm:contact:12345:interactions  → Interações do contato
  crm:tenant:67890:contacts:list  → Lista de contatos do tenant
  crm:user:11111:permissions      → Permissões do usuário
  crm:plan:pro:limits            → Limites do plano Professional
  crm:session:abc123             → Sessão do usuário
  crm:metrics:daily:msg_count    → Contador diário de mensagens
  crm:flag:maintenance_mode      → Feature flag
```

### Políticas de TTL por Entidade

| Entidade | TTL | Padrão | Justificativa |
|---|---|---|---|
| Dados de contato | 5 min | Cache-Aside | Dados mudam frequentemente |
| Interações do contato | 2 min | Cache-Aside | Alto volume de atualizações |
| Lista de contatos | 3 min | Cache-Aside | Lista reativa a novos registros |
| Permissões do usuário | 15 min | Cache-Aside | Mudam raramente |
| Dados do tenant | 30 min | Cache-Aside | Configurações estáveis |
| Limites do plano | 1 hora | Write-Through | Raramente alterados |
| Sessão do usuário | 24 horas | Cache-Aside | Expiração natural |
| Métricas diárias | 1 hora | Write-Behind | Batch de escrita |
| Feature flags | 30 seg | Cache-Aside | Precisam ser near-realtime |
| Dados de busca | 10 min | Cache-Aside | Queries pesadas |

### Estratégias de Invalidação

```mermaid
flowchart TD
    A[Evento de mudança] --> B{Tipo de mudança}
    B -->|Escrita em entidade| C[Invalidação direta]
    C --> C1[DEL crm:contact:{id}]
    C --> C2[DEL crm:tenant:{id}:contacts:list]
    B -->|Mudança de plano| D[Invalidação em cascata]
    D --> D1[DEL crm:plan:{plan_id}:limits]
    D --> D2[DEL crm:user:{id}:permissions]
    D --> D3[DEL crm:tenant:{id}:*]
    B -->|Logout / revogação| E[Invalidação de sessão]
    E --> E1[DEL crm:session:{token}]
    B -->|Manutenção| F[Flush seletivo]
    F --> F1[DEL crm:flag:*]
    F --> F2[DEL crm:metrics:*]
```

### Invalidação via RabbitMQ

```mermaid
sequenceDiagram
    participant ServiceA as Service A
    participant MQ as RabbitMQ
    participant CacheInvalidator as Cache Invalidator
    participant Cache as Redis

    ServiceA->>MQ: Publicar evento contact.updated
    MQ->>CacheInvalidator: Consumir evento
    CacheInvalidator->>Cache: DEL crm:contact:12345
    CacheInvalidator->>Cache: DEL crm:tenant:67890:contacts:list
    CacheInvalidator-->>CacheInvalidator: Invalidação concluída
```

### Cache Warming

```mermaid
sequenceDiagram
    participant Startup as Application Startup
    participant Warmup as Cache Warming Service
    participant DB as PostgreSQL
    participant Cache as Redis

    Startup->>Warmup: Iniciar aquecimento
    Warmup->>DB: SELECT * FROM plans
    DB-->>Warmup: Planos ativos
    Warmup->>Cache: SET crm:plan:* TTL=1h
    Warmup->>DB: SELECT * FROM tenants WHERE active = true
    DB-->>Warmup: Tenants ativos
    Warmup->>Cache: SET crm:tenant:*:limits TTL=30m
    Warmup->>DB: SELECT * FROM feature_flags
    DB-->>Warmup: Flags ativas
    Warmup->>Cache: SET crm:flag:* TTL=30s
    Warmup-->>Startup: Cache aquecido
```

### Comportamento de Fallback

```mermaid
flowchart TD
    A[Request ao sistema] --> B[Tentar ler do cache]
    B --> C{Cache disponível?}
    C -->|Sim| D{Cache Hit?}
    D -->|Sim| E[Retornar dados do cache]
    D -->|Miss| F[Ler do PostgreSQL]
    F --> G[Popular cache com TTL]
    G --> H[Retornar dados]
    C -->|Não - Cache down| I[Log warning]
    I --> J[Ler diretamente do PostgreSQL]
    J --> H
    I --> K[Alertar equipe via monitoring]
    H --> L[Request concluída]
    E --> L
```

### Monitoramento de Cache

```mermaid
flowchart LR
    A[Métricas coletadas] --> B[Hit Rate %]
    A --> C[Miss Rate %]
    A --> D[Eviction Rate]
    A --> E[Memory Usage]
    A --> F[Latência Read/Write]
    B --> G{Dashboard}
    C --> G
    D --> G
    E --> G
    F --> G
    G --> H{Alertas}
    H -->|Hit rate < 70%| I[Investigar chaves ou TTL]
    H -->|Memory > 80%| J[Revisar TTLs e eviction]
    H -->|Latência > 5ms| K[Verificar rede e Redis]
```

## Dependências

| Dependência | Tipo | Uso |
|---|---|---|
| Redis 7 | Infra | Cache principal com suporte a Redis Cluster |
| PostgreSQL 16 | Infra | Fonte de verdade (source of truth) |
| RabbitMQ 3 | Infra | Eventos de invalidação de cache |
| Spring Boot Cache | Lib | Integração de cache via Spring Cache |
| Lettuce | Lib | Cliente Redis para Java |
| Micrometer | Lib | Métricas de cache para observabilidade |

## Boas Práticas

- **TTL sempre definido**: Nunca criar chaves sem TTL para evitar memory leak.
- **Serialização**: Usar JSON (Jackson) para serialização de objetos em cache.
- **Compressão**: Comprimir valores maiores que 1KB antes de armazenar.
- **Key size**: Manter chaves compactas para economizar memória.
- **Batch operations**: Usar pipeline Redis para operações em lote (mget, mset).
- **Singleflight**: Evitar thundering herd com bloqueio de cache warming concorrente.
- **Nunca confiar no cache como source of truth**: Cache é sempre revogável.
- **Monitoramento**: Instrumentar hit rate, miss rate e latência de cache em todos os endpoints.
- **Max memory policy**: Configurar Redis com `allkeys-lru` ou `volatile-lru`.
- **Connection pooling**: Usar pool de conexões Lettuce com tamanho adequado.

## Referências

- [Redis Documentation](https://redis.io/docs/)
- [Spring Cache Abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html)
- [Lettuce Client](https://lettuce.io/)
- [Cache-Aside Pattern](https://learn.microsoft.com/en-us/azure/architecture/cache-patterns/cache-aside)

## Histórico de Revisão

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0 | 15/07/2026 | Paulo Alves | Criação inicial do documento |
