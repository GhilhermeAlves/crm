# DATABASE_MAP — Mapa do Banco de Dados

## Objetivo

Oferecer uma visão consolidada do banco de dados PostgreSQL: diagrama ER, entidades reais (do projeto vigente, shared schema + RLS), regras,
tabelas tenant-scoped versus globais e quotas por plano.

> **Nota.** O modelo antigo de schema-por-tenant foi substituído pelo shared schema + RLS FORCE (ver [MULTI_TENANCY.md](./MULTI_TENANCY.md)).

## Índice

- [Diagrama ER](#diagrama-er)
- [Entidades Principais](#entidades-principais)
- [Multi-Tenancy e Quotas](#multi-tenancy-e-quotas)
- [Regras de Esquema](#regras-de-esquema)
- [Referências](#referências)
- [Histórico de Revisão](#histórico-de-revisão)

---

## Diagrama ER

```mermaid
erDiagram
    COMPANY ||--o{ USER : has_members_via
    USER ||--o{ MEMBERSHIP : has
    COMPANY ||--o{ MEMBERSHIP : has
    COMPANY ||--o{ INVITATION : sends
    COMPANY ||--o{ CONTACT : owns
    COMPANY ||--o{ TAG : owns
    COMPANY ||--o{ AUDIT_LOG : generates
    COMPANY ||--o{ STORAGE_OBJECT : stores

    CONTACT ||--o{ CONTACT_ADDRESS : has
    CONTACT ||--o{ CONTACT_CUSTOM_FIELD : has
    CONTACT }o--o{ TAG : tagged (CONTACT_TAG)

    USER ||--o{ INVITATION : invited_by
    USER ||--o{ STORAGE_OBJECT : created_by

    COMPANY {
        uuid id PK
        varchar legal_name
        varchar trading_name
        varchar cnpj
        varchar email
        varchar phone
        varchar plan
        varchar status
        integer max_users
        integer max_contacts
        integer max_storage_mb
        jsonb address
        timestamp created_at
    }

    USER {
        uuid id PK
        uuid company_id FK
        varchar name
        varchar email
        varchar password_hash
        varchar keycloak_sub
        varchar status
        timestamp created_at
    }

    MEMBERSHIP {
        uuid id PK
        uuid user_id FK
        uuid company_id FK
        varchar role
        varchar status
        uuid invited_by FK
        timestamp joined_at
    }

    INVITATION {
        uuid id PK
        uuid company_id FK
        varchar email
        varchar role
        varchar token_hash
        uuid invited_by FK
        varchar status
        timestamp expires_at
    }

    CONTACT {
        uuid id PK
        uuid company_id FK
        varchar first_name
        varchar last_name
        varchar email
        varchar phone
        varchar company_name
        text notes
        timestamp deleted_at
    }

    CONTACT_ADDRESS {
        uuid id PK
        uuid contact_id FK
        varchar type
        varchar city
        varchar state
        varchar zip_code
    }

    CONTACT_CUSTOM_FIELD {
        uuid id PK
        uuid contact_id FK
        varchar field_key
        text field_value
    }

    TAG {
        uuid id PK
        uuid company_id FK
        varchar name
        varchar color
    }

    CONTACT_TAG {
        uuid contact_id FK
        uuid tag_id FK
    }

    AUDIT_LOG {
        uuid id PK
        uuid company_id FK
        uuid actor_user_id FK
        varchar action
        varchar entity
        uuid entity_id
        jsonb details
        timestamp created_at
    }

    STORAGE_OBJECT {
        uuid id PK
        uuid company_id FK
        varchar object_key
        varchar file_name
        varchar content_type
        bigint size_bytes
        bytea data
        uuid created_by FK
    }
```

---

## Entidades Principais

### Identidade e Tenancies (Sprint 8.1–8.6)
- `companies` — **tabela global** (sem RLS): tenants do sistema; carrega `plan`, `status`, `max_users`, `max_contacts`, `max_storage_mb`.
- `users` — usuários; `company_id` = empresa ativa denormalizada (synced pela membership). RLS FORCE por `company_id`.
- `memberships` — **fonte de verdade** da relação usuário↔empresa (1:N): role e status `ACTIVE`/`PENDING`/`REMOVED`. RLS FORCE com `membership_own_policy` (próprias) + `membership_tenant_policy` (tenant).
- `invitations` — convites por e-mail; `token_hash` (SHA-256 hex), status `PENDING`/`ACCEPTED`/`REVOKED`/`EXPIRED`. Acesso admin por `company_id` + acesso por token via GUC.
- `roles` / `user_roles` / `role_permissions` / `permissions` — RBAC. `permissions` é global; roles/user_roles tenant-scoped.
- `subscriptions` / `company_settings` — dados da empresa, tenant-scoped.

### Contato
- `contacts` (V015) — contatos com `company_id`; uniqueness `email+company` onde `deleted_at IS NULL`.
- `contact_addresses` — endereços do contato.
- `contact_custom_fields` — campos customizados (chave/valor).
- `tags` / `contact_tags` — segmentação; tags tenant-scoped, unique `name+company`.

### Pipeline (V016/V017)
- `pipelines`, `stages`, `opportunities`, `opportunity_history` — funil de vendas, tenant-scoped (histórico via join com opportunities).

### Grandes prioridades / extras
- `leads` (V016) — leads tenant-scoped.
- `audit_logs` — auditoria, tenant-scoped (RLS FORCE).
- `storage_objects` (V037) — blob de storage tenant-scoped usado para enforce/uso da quota `max_storage_mb`; único armazenamento (port permite trocar por object-store futuro).

### Identidade (auth / bootstrap)
- `refresh_tokens`, `password_reset_tokens` — global (sem RLS).
- Tabelas de bootstrap de identidade (V022/V024/V025/V026): OTP por telefone/e-mail, seeds de `keycloak_sub`/e-mail/telefone.

---

## Multi-Tenancy e Quotas

| Regra | Descrição |
|---|---|
| Shared schema + RLS | Todas as tabelas tenant-scoped têm `company_id` e RLS FORCE com policy por `company_id = app.current_tenant_id()`. |
| Tabelas globais | `companies`, `permissions`, `role_permissions`, `refresh_tokens`, `password_reset_tokens`. |
| Quotas por plano | `max_users = 5`, `max_contacts = 500`, `max_storage_mb = 1024` (defaults `CompanyService`). |
| Uso | `CompanyQuotaService.usage()` soma ativos + convites pendentes (users), contatos, `SUM(size_bytes)` de `storage_objects`. |
| Exceção | `QuotaExceededException` → HTTP 422 `QUOTA_EXCEEDED`. |

---

## Regras de Esquema

| Regra | Descrição |
|---|---|
| UUID PK | Todas as tabelas usam UUID (v4 / `gen_random_uuid()`) como PK. |
| Soft Delete | `deleted_at` nas tabelas de negócio (ex.: `contacts`). |
| Audit | `created_at`/`updated_at` nas tabelas relevantes. |
| JSONB | `metadata`, `settings`, `address`, `details` em JSONB onde aplicável. |
| RLS | Tenant-scoped: `ENABLE + FORCE ROW LEVEL SECURITY` + policy de isolamento. |
| Grants | Padrão `crm_app` (V031/V034/V037): SELECT/INSERT/UPDATE/DELETE nas tabelas do app. |
| Migrações | Verificação estrutural via `pg_catalog` (imune a RLS) em `DO` blocks. |

---

## Referências

| Documento | Caminho |
|---|---|
| Multi-Tenancy | [MULTI_TENANCY.md](./MULTI_TENANCY.md) |
| Overview | [03-database/Overview.md](./03-database/Overview.md) |

---

## Histórico de Revisão

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial (modelo schema-per-tenant) — **superado** |
| 2.0.0 | 2026-08-12 | Architect | Reescrita para o modelo real: shared schema + RLS, memberships, invitations, quotas, storage |