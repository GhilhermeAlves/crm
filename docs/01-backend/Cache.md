# Cache — Sistema de Cache (Redis)

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Responsabilidades](#responsabilidades)
- [Estratégias de Cache](#estratégias-de-cache)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar o sistema de cache, incluindo estratégias, key patterns e invalidação.

## Descrição

Redis é usado como camada de cache para dados frequentemente acessados, reduzindo a carga no PostgreSQL e melhorando a latência. O cache é organizado por tenant com prefixo de company_id.

## Responsabilidades

- Cache de dados frequentemente lidos
- Rate limiting por tenant
- Sessões e tokens
- Distributed locks
- Filas leves (pub/sub)

## Estratégias de Cache

### Cache-Aside (Lazy Loading)

```
1. App verifica cache
        │
2. Se hit → Retorna dados
   Se miss → Busca no DB → Cacheia → Retorna
3. TTL define expiração
```

### Write-Through

```
1. App escreve no DB
        │
2. App atualiza cache
        │
3. Dados sempre consistentes
```

### Cache Invalidation

```
1. Dados são atualizados no DB
        │
2. Cache é invalidado (delete, não update)
        │
3. Próxima leitura re-popula o cache
```

## Key Patterns

```
tenant:{companyId}:lead:{leadId}              → Dados do lead
tenant:{companyId}:contacts:page:{page}       → Lista paginada
tenant:{companyId}:dashboard:kpis             → KPIs do dashboard
tenant:{companyId}:user:{userId}:session      → Sessão do usuário
tenant:{companyId}:rate:{endpoint}:{userId}   → Rate limiting
global:whatsapp:templates                     → Templates aprovados
lock:scheduler:{jobName}                      → Distributed lock
```

## TTLs

| Dado | TTL | Justificativa |
|---|---|---|
| Dashboard KPIs | 5 min | Dados semi-realtime |
| Dados de lead | 15 min | Frequência moderada |
| Sessão de usuário | 24h | Sessão longa |
| Rate limit | 1 min | Janela de 1 minuto |
| Templates WhatsApp | 1h | Raramente mudam |
| Token JWT blacklist | 15 min | Duração do token |

## Dependências

- [06-devops/Docker.md](../06-devops/Docker.md) — Redis infrastructure
- [Auth.md](./Auth.md) — Sessões e blacklist de tokens
- [Dashboard.md](./Dashboard.md) — Cache de KPIs

## Regras

- Nunca cachear dados sensíveis (senhas, tokens)
- TTL máximo: 24 horas
- Cache hit rate monitorado (meta: > 80%)
- Keys devem ser padronizadas (ver Key Patterns)
- Redis deve ter pelo menos 256MB de memória
- Eviction policy: allkeys-lru

## Futuras Melhorias

- Redis Cluster para alta disponibilidade
- Cache warming para dados críticos
- Métricas de cache hit/miss por endpoint
- Cache distribuído entre instâncias
- Read replicas para cache de leitura

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
