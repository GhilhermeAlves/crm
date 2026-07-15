# Multi-Tenancy

## Objetivo

Documentar a arquitetura de multi-tenancy do CRM SaaS Omnichannel, definindo isolamento de dados por schema, resolução de tenant, contexto propagado via JWT, pool de conexões, estratégia de migrações e backup por tenant.

## Escopo

Cobre toda a infraestrutura de isolamento entre tenants, desde a camada de API até o banco de dados, incluindo Redis, RabbitMQ e camadas de cache.

## Responsabilidades

| Componente | Responsabilidade |
|---|---|
| **Tenant Filter** | Intercepta requisições e extrai tenantId do JWT |
| **Schema Router** | Redireciona conexões PostgreSQL para o schema correto |
| **JWT Provider** | Emite e valida tokens com claim `tenantId` |
| **Flyway Migrator** | Aplica migrações individualmente por schema de tenant |
| **Connection Pooler** | Gerencia pool de conexões com isolamento por tenant |
| **Redis Prefixer** | Adiciona prefixo de tenant nas chaves Redis |
| **Backup Service** | Executa backups individualizados por tenant |

## Fluxos

### Diagrama de Resolução de Tenant

```mermaid
sequenceDiagram
    participant C as Cliente
    participant GW as API Gateway
    participant AUTH as Auth Filter
    participant JWT as JWT Provider
    participant TF as Tenant Filter
    participant SC as Schema Context
    participant CP as Connection Pool
    participant DB as PostgreSQL
    participant RD as Redis

    C->>GW: HTTP Request + Authorization: Bearer JWT
    GW->>AUTH: Extrai token do header
    AUTH->>JWT: Valida token e assinatura

    alt Token válido
        JWT-->>AUTH: Claims (userId, tenantId, roles)
        AUTH->>TF: Propaga tenantId
        TF->>SC: Define TenantContext (ThreadLocal)
        SC->>CP: Obtém conexão do schema correto
        CP->>DB: SET search_path TO tenant_{tenantId}
        DB-->>CP: Conexão configurada

        TF->>RD: Redis com prefixo tenant:{tenantId}:

        Note over SC: Execução da requisição com contexto de tenant

        SC->>SC: Limpa TenantContext no finally
    else Token inválido
        JWT-->>AUTH: Erro de validação
        AUTH-->>GW: 401 Unauthorized
        GW-->>C: 401 Unauthorized
    end

    GW-->>C: Response
```

### Diagrama de Estrutura de Schemas

```mermaid
flowchart TB
    subgraph PostgreSQL ["PostgreSQL 16"]
        PG_PUBLIC["public schema<br/>(shared data)"]
        PG_T1["tenant_abc123 schema<br/>(Tenant A)"]
        PG_T2["tenant_def456 schema<br/>(Tenant B)"]
        PG_T3["tenant_ghi789 schema<br/>(Tenant C)"]

        PG_PUBLIC --> PG_T1
        PG_PUBLIC --> PG_T2
        PG_PUBLIC --> PG_T3
    end

    subgraph Shared ["Tabelas Shared (public)"]
        SH1[companies]
        SH2[plans]
        SH3[users]
        SH4[tenants]
        SH5[global_configs]
    end

    subgraph Tenant ["Tabelas por Tenant"]
        TT1[contacts]
        TT2[leads]
        TT3[opportunities]
        TT4[pipelines]
        TT5[conversations]
        TT6[messages]
        TT7[campaigns]
        TT8[reports]
        TT9[integrations]
        TT10[audit_logs]
    end

    PG_PUBLIC --- Shared
    PG_T1 --- Tenant
    PG_T2 --- Tenant
    PG_T3 --- Tenant
```

## Dependências

### Resolução de Tenant

O tenant é resolvido em cada requisição através do seguinte fluxo:

1. **JWT Header**: Token contém claim `tenantId` no payload
2. **Tenant Filter (OncePerRequestFilter)**: Intercepts every request
3. **TenantContext (ThreadLocal)**: Armazena tenantId para toda a duração da requisição
4. **Schema Router**: Configura `search_path` na conexão PostgreSQL

### Estrutura do JWT

```json
{
  "sub": "user-uuid",
  "tenantId": "tenant-uuid",
  "roles": ["admin", "agent"],
  "schema": "tenant_abc123",
  "iat": 1752595800,
  "exp": 1752682200
}
```

### Tenant Context Filter

```
Request → JWT Validation → Extract tenantId → Set TenantContext → Process → Clear TenantContext
```

O TenantContext é implementado como ThreadLocal com limpeza automática no finally block para evitar vazamento entre threads.

## Fluxos (continuação)

### Pool de Conexões por Tenant

```mermaid
flowchart LR
    subgraph App ["Application"]
        SVC1[Service A]
        SVC2[Service B]
        SVC3[Service C]
    end

    subgraph Pool ["HikariCP Pool"]
        POOL1["Pool: tenant_abc<br/>max=10, min=2"]
        POOL2["Pool: tenant_def<br/>max=10, min=2"]
        POOL3["Pool: tenant_ghi<br/>max=10, min=2"]
    end

    subgraph DB ["PostgreSQL"]
        SCHEMA1[tenant_abc schema]
        SCHEMA2[tenant_def schema]
        SCHEMA3[tenant_ghi schema]
    end

    SVC1 --> POOL1
    SVC2 --> POOL2
    SVC3 --> POOL3

    POOL1 --> SCHEMA1
    POOL2 --> SCHEMA2
    POOL3 --> SCHEMA3
```

| Configuração | Valor | Descrição |
|---|---|---|
| maximumPoolSize | 10 por tenant | Máximo de conexões simultâneas por tenant |
| minimumIdle | 2 por tenant | Mínimo de conexões em idle |
| connectionTimeout | 30000ms | Tempo máximo para obter conexão |
| idleTimeout | 600000ms | Tempo para liberar conexão idle |
| maxLifetime | 1800000ms | Vida máxima de uma conexão |
| leakDetectionThreshold | 60000ms | Alerta de leak de conexão |

### Migrações por Tenant (Flyway)

```mermaid
flowchart TB
    START([Início da Migração]) --> CHECK{Novo tenant?}

    CHECK -->|Sim| CREATE[Aplica schema base<br/>todas as tabelas]
    CREATE --> VERSION[Registra versão<br/>flyway_schema_history]

    CHECK -->|Não| VERIFY{Pendências<br/>de migração?}
    VERIFY -->|Sim| MIGRATE[Aplica migrations pendentes<br/>no schema do tenant]
    MIGRATE --> VERSION
    VERIFY -->|Não| SKIP[Migração ignorada]

    VERSION --> LOG[Registra log de migração]
    SKIP --> LOG
    LOG --> END([Fim])

    style CREATE fill:#e1f5fe
    style MIGRATE fill:#fff3e0
    style SKIP fill:#e8f5e9
```

| Etapa | Descrição |
|---|---|
| 1. Migrations compartilhadas | Arquivos em `db/migration/shared/` aplicados a todos os schemas |
| 2. Migrations por tenant | Flyway itera sobre cada schema de tenant e aplica pendências |
| 3. Novo tenant | Schema criado a partir de migrations completas ( bootstrap ) |
| 4. Rollback | Operação manual com script de reversão; não automático |
| 5. Versionamento | Tabela `flyway_schema_history` isolada por schema |

### Estrutura de Diretórios Flyway

```
db/
  migration/
    shared/
      V1__create_plans_table.sql
      V2__create_companies_table.sql
      V3__create_users_table.sql
    tenant/
      V1__create_contacts_table.sql
      V2__create_leads_table.sql
      V3__create_pipelines_table.sql
      V4__create_opportunities_table.sql
      V5__create_conversations_table.sql
      V6__create_messages_table.sql
      V7__create_campaigns_table.sql
      V8__create_reports_table.sql
      V9__create_integrations_table.sql
      V10__create_audit_logs_table.sql
```

### Isolamento de Dados

| Camada | Estratégia de Isolamento |
|---|---|
| **PostgreSQL** | Schema separado por tenant (`tenant_{id}`) |
| **Redis** | Prefixo de chave: `tenant:{tenantId}:{key}` |
| **RabbitMQ** | Vhost exclusivo por tenant OU prefixo na routing key |
| **File Storage** | Diretório: `/{tenantId}/attachments/` |
| **Cache (Application)** | Key prefix `cache:{tenantId}:{entity}:{id}` |
| **Logs** | Campo `tenantId` em todas as linhas de log estruturado |
| **Audit Trail** | Tabela `audit_logs` isolada no schema do tenant |
| **Metrics** | Tag `tenant_id` em todas as métricas Micrometer |

### Backup por Tenant

```mermaid
flowchart TB
    SCHEDULE[Cron: 02:00 AM UTC] --> LIST[Lista todos os tenants ativos]

    LIST --> LOOP{Para cada tenant}

    LOOP --> SNAPSHOT[pg_dump schema tenant_{id}]
    SNAPSHOT --> COMPRESS[Comprime com gzip]
    COMPRESS --> UPLOAD[Upload para S3 bucket privado]
    UPLOAD --> VERIFY[Verifica integridade SHA-256]
    VERIFY --> RETAIN[Aplica política de retenção<br/>30 dias]

    RETAIN --> NEXT{Próximo tenant?}
    NEXT -->|Sim| LOOP
    NEXT -->|Não| NOTIFY[Notifica time de ops]
    NOTIFY --> END([Fim])
```

| Configuração | Valor |
|---|---|
| Frequência | Diária às 02:00 UTC |
| Retenção | 30 dias (configurável por tenant) |
| Storage | S3 bucket com AES-256 encryption |
| Compressão | gzip (redução média de 70%) |
| Verificação | SHA-256 checksum após upload |
| Restauração | Script automatizado com target_schema |
| Snapshot compartilhado | Backup do schema `public` separadamente |

### Checklist de Isolamento

| Critério | Implementação |
|---|---|
| Um tenant não acessa dados de outro | Schema isolation + TenantContext filter |
| Tenant ausente na requisição bloqueia acesso | TenantFilter rejeita requests sem tenantId |
| Migrations não afetam outros schemas | Flyway itera por schema independentemente |
| Redis não vaza dados entre tenants | Prefixo obrigatório em todas as operações |
| Logs permitem rastreabilidade por tenant | tenantId em todas as linhas estruturadas |
| Backup é granular e restaurável | pg_dump por schema + verificação de integridade |
| Rate limiting é por tenant | Redis key: `ratelimit:{tenantId}:{endpoint}` |
| File uploads são isolados | Path: `{tenantId}/attachments/{fileId}` |

## Boas práticas

- Nunca executar queries sem `search_path` configurado — usar o TenantContext sempre
- TenantContext deve ser limpo no finally block para evitar vazamento entre threads
- Migrações devem ser testadas individualmente para cada schema antes de produção
- Backups devem ser testados periodicamente com restauração em ambiente de staging
- Rate limiting configurado por tenant para evitar noisy neighbor
- Monitoring com dashboards segregados por tenant para detecção de anomalias
- PII (dados pessoais) isolado com criptografia adicional no schema do tenant
- Hard delete proibido — usar soft delete com `deleted_at` timestamp
- Conexões com search_path devem ser validadas no startup do application
- Pool de conexões dimensionado conforme tier do plano do tenant

## Referências

- PostgreSQL Schema Documentation — postgresql.org
- Multi-Tenancy with Spring Boot — spring.io
- SaaS Multi-Tenancy Patterns — Microsoft Azure Architecture Center
- Flyway Teams Edition — Documentation

## Histórico de Revisão

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0 | 2026-07-15 | Equipe de Arquitetura | Versão inicial da arquitetura multi-tenancy |
