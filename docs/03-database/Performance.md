# Performance — Performance e Otimização

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Estratégias de Otimização](#estratégias-de-otimização)
- [Queries Lentas](#queries-lentas)
- [Monitoramento](#monitoramento)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar estratégias de otimização de performance do banco de dados.

## Descrição

Performance é garantida via índices apropriados, queries otimizadas, caching e monitoramento contínuo.

## Estratégias de Otimização

### Índices

- Índices em colunas de WHERE, JOIN e ORDER BY
- Índices compostos para queries frequentes
- Índices parciais para filtros específicos

### Connection Pooling

```properties
# HikariCP
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000
```

### Query Optimization

- Evitar SELECT *
- Usar EXISTS ao invés de COUNT
- Paginação com cursor (não OFFSET)
- Batch inserts/updates

### Caching

- Redis para dados frequentes
- Cache-Aside pattern
- TTL baseado na frequência de atualização

## Queries Lentas

### Identificação

```sql
-- pg_stat_statements
SELECT query, calls, mean_time, total_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 20;
```

### Log

```sql
-- Log queries > 1s
log_min_duration_statement = 1000
```

## Monitoramento

| Métrica | Ferramenta | Alerta |
|---|---|---|
| Query latency | pg_stat_statements | > 1s |
| Connection count | pg_stat_activity | > 80% pool |
| Cache hit ratio | pg_stat_user_tables | < 95% |
| Deadlocks | pg_stat_database | > 0 |
| Table bloat | pg_stat_user_tables | > 20% |

## Responsabilidades

- Monitorar queries lentas
- Otimizar índices regularmente
- Analisar explain analyze
- Manter estatísticas atualizadas

## Dependências

- [Indexes.md](./Indexes.md) — Índices
- [01-backend/Cache.md](../01-backend/Cache.md) — Cache Redis
- [06-devops/Monitoring.md](../06-devops/Monitoring.md) — Monitoramento

## Regras

- Queries devem ser < 200ms em 95% dos casos
- N+1 queries são proibidas
- Full table scans são proibidos em produção
- EXPLAIN ANALYZE antes de commitar queries complexas
- VACUUM ANALYZE semanal

## Futuras Melhorias

- Query plan analysis automatizado
- Index recommendation tools
- Read replicas para analytics
- Database partitioning
- Query caching a nível de database

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
