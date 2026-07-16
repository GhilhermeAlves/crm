# Database Context

## Resumo do Módulo
PostgreSQL 16 com schema-per-tenant. UUID v4 PKs, soft delete (90 dias), Flyway migrations. 29 tabelas em 8 bounded contexts. Naming: snake_case plural.

## Objetivo
Gerenciar esquema de banco, migrations, isolamento de dados e convenções de modelagem.

## Responsabilidades
- Schema-per-tenant com PostgreSQL schemas
- UUID v4 como PK em todas tabelas
- Soft delete com 90 dias de retenção
- Flyway para migrations versionadas
- Convenções de naming e indexação

## Convenções
| Regra | Exemplo |
|-------|---------|
| Tabelas | `snake_case` plural: `users`, `contacts` |
| Colunas | `snake_case`: `created_at`, `company_id` |
| PKs | UUID v4: `id UUID PRIMARY KEY` |
| FKs | `{table}_id`: `user_id`, `company_id` |
| Índices | Todo FK indexado |
| Soft delete | `deleted_at TIMESTAMP NULL` |
| Timestamps | `created_at`, `updated_at` |

## 29 Tabelas (8 Bounded Contexts)
| Contexto | Tabelas |
|----------|---------|
| Auth | `users`, `refresh_tokens`, `roles`, `user_roles` |
| Tenant | `companies`, `company_settings`, `subscriptions` |
| Contact | `contacts`, `contact_addresses`, `contact_custom_fields`, `tags`, `contact_tags`, `segments` |
| Lead | `leads` |
| Pipeline | `pipelines`, `stages`, `opportunities`, `opportunity_history` |
| Conversation | `conversations`, `messages`, `message_attachments`, `message_templates` |
| Campaign | `campaigns`, `campaign_steps` |
| Automation | `automation_rules`, `automation_triggers`, `automation_actions`, `automation_executions` |

## Componentes Backend
- `database` module (connection pool, schema resolver)
- `migration` package (Flyway configs)
- `repository` pattern (base repository com soft delete)

## Migrations
- Flyway com naming: `V{version}__{description}.sql`
- Uma migration por schema (executada em todos tenants)
- Rollback manual (não automático)

## Soft Delete
```sql
-- Query automática: WHERE deleted_at IS NULL
-- Hard delete: cron job após 90 dias
-- Restore: UPDATE SET deleted_at = NULL
```

## Performance
- **Connection pooling**: HikariCP (max 20 connections/schema)
- **Índices**: Todo FK indexado automaticamente
- **Query optimization**: EXPLAIN ANALYZE obrigatório para queries lentas
- **Partitioning**: Considerado para messages (>1M rows)

## Fluxo Resumido
1. Nova migration criada → Flyway detecta → executa em todos schemas
2. Query recebida → schema resolver (JWT claim) → search_path configurado
3. Soft delete → `deleted_at` preenchido → hard delete após 90 dias

## Checklist de Implementação
- [ ] PostgreSQL 16 configurado
- [ ] Schema-per-tenant implementado
- [ ] UUID v4 em todas PKs
- [ ] Soft delete com 90 dias
- [ ] Flyway migrations funcionando
- [ ] 29 tabelas criadas
- [ ] Todo FK indexado
- [ ] Connection pooling configurado

## Checklist de Testes
- [ ] Migrations rodam em todos schemas
- [ ] Soft delete não remove dados
- [ ] Hard delete após 90 dias funciona
- [ ] Queries usam schema correto
- [ ] Connection pool não esgota

## Documentação Oficial Relacionada
- `docs/database/SCHEMA-DESIGN.md`
- `docs/database/MIGRATIONS.md`
- `docs/database/SOFT-DELETE.md`

## Histórico de Revisão
| Data | Autor | Descrição |
|------|-------|-----------|
| 2026-07-15 | System | Criação do contexto |
