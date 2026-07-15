# DATABASE_MAP — Mapa do Banco de Dados

## Objetivo

Fornecer uma visão consolidada do banco de dados PostgreSQL com diagrama ER em Mermaid, entidades, relacionamentos e regras.

## Índice

- [Diagrama ER](#diagrama-er)
- [Entidades Principais](#entidades-principais)
- [Regras de Esquema](#regras-de-esquema)
- [Referências](#referências)
- [Histórico de Revisão](#histórico-de-revisão)

---

## Diagrama ER

```mermaid
erDiagram
    TENANT ||--o{ USER : contains
    TENANT ||--o{ CONTACT : contains
    TENANT ||--o{ LEAD : contains
    TENANT ||--o{ PIPELINE : contains
    TENANT ||--o{ CAMPAIGN : contains
    TENANT ||--o{ TEMPLATE : contains

    USER ||--o{ CONVERSATION : assigned
    USER ||--o{ OPPORTUNITY : owns
    USER ||--o{ AUDIT_LOG : generates

    CONTACT ||--o{ LEAD : converts_to
    CONTACT ||--o{ CONVERSATION : participates
    CONTACT ||--o{ OPPORTUNITY : linked_to
    CONTACT ||--o{ MESSAGE : sends_receives
    CONTACT }o--o{ TAG : has

    LEAD }o--|| PIPELINE : belongs_to
    LEAD }o--|| STAGE : at_stage
    LEAD ||--o{ LEAD_SCORE : scored

    PIPELINE ||--o{ STAGE : contains
    PIPELINE ||--o{ OPPORTUNITY : tracks
    STAGE ||--o{ OPPORTUNITY : holds

    OPPORTUNITY ||--o{ MESSAGE : linked
    OPPORTUNITY ||--o{ OPPORTUNITY_HISTORY : has

    CONVERSATION ||--o{ MESSAGE : contains
    CONVERSATION }o--|| USER : assigned_to
    CONVERSATION }o--|| CONTACT : with

    CAMPAIGN ||--o{ CAMPAIGN_MESSAGE : sends
    CAMPAIGN }o--o{ CONTACT : targets
    CAMPAIGN }o--o{ TEMPLATE : uses

    TEMPLATE }o--o{ CAMPAIGN : used_in

    AUTOMATION ||--o{ AUTOMATION_EXECUTION : logs
    AUTOMATION }o--o{ CONTACT : triggers_on

    USER ||--o{ NOTIFICATION : receives
    USER ||--o{ USER_PERMISSION : has

    COMPANY ||--o{ BILLING : has
    COMPANY ||--o{ COMPANY_SETTINGS : configured

    %% Entidades
    TENANT {
        uuid id PK
        varchar name
        varchar slug
        varchar schema_name
        varchar plan
        varchar status
        timestamp created_at
    }

    USER {
        uuid id PK
        uuid tenant_id FK
        varchar name
        varchar email
        varchar password_hash
        varchar role
        varchar status
        timestamp created_at
    }

    COMPANY {
        uuid id PK
        uuid tenant_id FK
        varchar name
        varchar cnpj
        varchar email
        varchar phone
        jsonb address
        timestamp created_at
    }

    CONTACT {
        uuid id PK
        uuid tenant_id FK
        varchar name
        varchar email
        varchar phone
        jsonb custom_fields
        varchar status
        timestamp created_at
    }

    LEAD {
        uuid id PK
        uuid tenant_id FK
        uuid contact_id FK
        uuid pipeline_id FK
        uuid stage_id FK
        uuid owner_id FK
        varchar source
        integer score
        varchar status
        timestamp created_at
    }

    LEAD_SCORE {
        uuid id PK
        uuid lead_id FK
        varchar rule_name
        integer points
        timestamp calculated_at
    }

    PIPELINE {
        uuid id PK
        uuid tenant_id FK
        varchar name
        integer position
        boolean active
        timestamp created_at
    }

    STAGE {
        uuid id PK
        uuid pipeline_id FK
        varchar name
        integer position
        varchar color
        integer probability
        timestamp created_at
    }

    OPPORTUNITY {
        uuid id PK
        uuid tenant_id FK
        uuid pipeline_id FK
        uuid stage_id FK
        uuid contact_id FK
        uuid owner_id FK
        varchar title
        decimal value
        varchar currency
        varchar status
        varchar priority
        timestamp expected_close_date
        timestamp created_at
    }

    OPPORTUNITY_HISTORY {
        uuid id PK
        uuid opportunity_id FK
        uuid from_stage_id FK
        uuid to_stage_id FK
        varchar action
        jsonb details
        timestamp created_at
    }

    CONVERSATION {
        uuid id PK
        uuid tenant_id FK
        uuid contact_id FK
        uuid assigned_to FK
        varchar channel
        varchar status
        varchar priority
        timestamp last_message_at
        timestamp created_at
    }

    MESSAGE {
        uuid id PK
        uuid conversation_id FK
        uuid tenant_id FK
        varchar direction
        varchar channel
        text content
        jsonb metadata
        varchar status
        varchar external_id
        timestamp sent_at
        timestamp created_at
    }

    CAMPAIGN {
        uuid id PK
        uuid tenant_id FK
        varchar name
        varchar type
        varchar status
        timestamp scheduled_at
        timestamp started_at
        timestamp completed_at
        jsonb config
        timestamp created_at
    }

    CAMPAIGN_MESSAGE {
        uuid id PK
        uuid campaign_id FK
        uuid contact_id FK
        uuid message_id FK
        varchar status
        timestamp sent_at
    }

    TEMPLATE {
        uuid id PK
        uuid tenant_id FK
        varchar name
        varchar category
        text body
        jsonb variables
        varchar status
        timestamp created_at
    }

    AUTOMATION {
        uuid id PK
        uuid tenant_id FK
        varchar name
        varchar trigger_type
        jsonb trigger_config
        jsonb actions
        boolean active
        timestamp created_at
    }

    AUTOMATION_EXECUTION {
        uuid id PK
        uuid automation_id FK
        uuid tenant_id FK
        varchar status
        jsonb input
        jsonb output
        timestamp executed_at
    }

    TAG {
        uuid id PK
        uuid tenant_id FK
        varchar name
        varchar color
        timestamp created_at
    }

    NOTIFICATION {
        uuid id PK
        uuid user_id FK
        varchar type
        text title
        text body
        jsonb data
        boolean read
        timestamp created_at
    }

    AUDIT_LOG {
        uuid id PK
        uuid tenant_id FK
        uuid user_id FK
        varchar entity
        varchar entity_id
        varchar action
        jsonb old_value
        jsonb new_value
        timestamp created_at
    }

    USER_PERMISSION {
        uuid id PK
        uuid user_id FK
        varchar resource
        varchar action
        varchar effect
    }

    BILLING {
        uuid id PK
        uuid company_id FK
        varchar plan
        decimal amount
        varchar currency
        varchar status
        timestamp next_billing_date
        timestamp created_at
    }

    COMPANY_SETTINGS {
        uuid id PK
        uuid company_id FK
        jsonb settings
        timestamp updated_at
    }
```

---

## Entidades Principais

### Identidade
- `TENANT` — Schema isolation de cada cliente
- `USER` — Usuários com roles (SUPER_ADMIN, ADMIN, MANAGER, AGENT, VIEWER)
- `USER_PERMISSION` — Permissões granulares (RBAC)
- `COMPANY` — Dados da empresa/tenant
- `COMPANY_SETTINGS` — Configurações customizáveis

### Contato
- `CONTACT` — Contatos com campos customizados
- `TAG` — Tags para segmentação
- `LEAD` — Leads com scoring e pipeline
- `LEAD_SCORE` — Histórico de scoring
- `CUSTOMER` (implícito via CONTACT com status CUSTOMER)

### Pipeline
- `PIPELINE` — Pipelines de vendas
- `STAGE` — Estágios do pipeline
- `OPPORTUNITY` — Oportunidades de negócio
- `OPPORTUNITY_HISTORY` — Histórico de movimentação

### Comunicação
- `CONVERSATION` — Conversas multicanal
- `MESSAGE` — Mensagens (texto, mídia, status)

### Campanha
- `CAMPAIGN` — Campanhas de marketing
- `CAMPAIGN_MESSAGE` — Mensagens enviadas por campanha
- `TEMPLATE` — Templates de mensagens

### Automação
- `AUTOMATION` — Regras de automação
- `AUTOMATION_EXECUTION` — Log de execuções

### Sistema
- `NOTIFICATION` — Notificações do usuário
- `AUDIT_LOG` — Log de auditoria (todas as ações)
- `BILLING` — Dados de faturamento

---

## Regras de Esquema

| Regra | Descrição |
|---|---|
| Multi-tenancy | Schema isolation por tenant (`tenant_{id}`) |
| UUID PK | Todas as tabelas usam UUID v4 como PK |
| Soft Delete | `deleted_at` em todas as tabelas de negócio |
| Audit | `created_at`, `updated_at` em todas as tabelas |
| Índices | Índices em FKs, campos de busca e ordenação |
| JSONB | `custom_fields`, `metadata`, `settings` como JSONB |

---

## Referências

| Documento | Caminho |
|---|---|
| Overview | [03-database/Overview.md](./03-database/Overview.md) |
| ERD | [03-database/ERD.md](./03-database/ERD.md) |
| Entities | [03-database/Entities.md](./03-database/Entities.md) |
| Relationships | [03-database/Relationships.md](./03-database/Relationships.md) |
| Indexes | [03-database/Indexes.md](./03-database/Indexes.md) |
| UUID | [03-database/UUID.md](./03-database/UUID.md) |
| SoftDelete | [03-database/SoftDelete.md](./03-database/SoftDelete.md) |
| Audit | [03-database/Audit.md](./03-database/Audit.md) |
| SUMMARY | [SUMMARY.md](./SUMMARY.md) |

---

## Histórico de Revisão

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial do mapa de banco de dados |
