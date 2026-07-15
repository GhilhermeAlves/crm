# Overview — Visão Geral do Banco de Dados

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Stack](#stack)
- [Estratégia Multi-Tenant](#estratégia-multi-tenant)
- [Convenções](#convenções)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Fornecer visão geral do banco de dados, incluindo stack, convenções e estratégias.

## Descrição

PostgreSQL 16 como database principal. Redis para cache. RabbitMQ para messaging. Estratégia multi-tenant via schemas separados.

## Stack

| Componente | Tecnologia | Finalidade |
|---|---|---|
| Primary DB | PostgreSQL 16 | Dados transacionais |
| Cache | Redis 7 | Cache e sessões |
| Message Broker | RabbitMQ 3 | Eventos assíncronos |
| ORM | Hibernate/JPA 6 | Mapeamento |
| Migration | Flyway 10+ | Versionamento |

## Estratégia Multi-Tenant

### Schema per Tenant

```sql
-- Schema padrão
CREATE SCHEMA tenant_abc123;

-- Cada schema contém todas as tabelas do CRM
-- Row-Level Security como camada adicional
```

### Configuração

```properties
# application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/crm
spring.jpa.properties.hibernate.default_schema=tenant_${TENANT_ID}
```

## Convenções

| Elemento | Padrão | Exemplo |
|---|---|---|
| Tabela | snake_case, plural | `leads`, `contacts` |
| Coluna | snake_case | `first_name`, `created_at` |
| Primary Key | `id` (UUID) | `uuid` |
| FK | `fk_{table}_{ref}` | `fk_contacts_company_id` |
| Índice | `idx_{table}_{columns}` | `idx_leads_email` |
| Unique | `uk_{table}_{columns}` | `uk_users_email` |

## Responsabilidades

- Manter integridade referencial
- Garantir performance com índices apropriados
- Suportar multi-tenancy com isolamento
- Facilitar migrations backward-compatible

## Dependências

- [00-core/Architecture.md](../00-core/Architecture.md) — Arquitetura
- [00-core/TechStack.md](../00-core/TechStack.md) — Stack

## Regras

- Nenhuma tabela pode ser deletada sem migration
- Colunas nunca são removidas (apenas deprecated)
- Constraints devem ser nomeadas
- Índices devem ser testados em staging antes de produção
- Backup diário é obrigatório

## Futuras Melhorias

- Database partitioning para tabelas grandes
- Read replicas para analytics
- CockroachDB para distribuição geográfica
- Connection pooling com PgBouncer
- Monitoramento com pg_stat_statements

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
