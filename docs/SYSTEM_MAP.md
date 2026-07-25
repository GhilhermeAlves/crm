# SYSTEM_MAP — Mapa Completo da Arquitetura

## Objetivo

Apresentar a arquitetura completa do sistema em diagramas, mostrando como todos os componentes se conectam.

## Índice

- [1. Visão de Alto Nível](#1-visão-de-alto-nível)
- [2. Arquitetura de Camadas](#2-arquitetura-de-camadas)
- [3. Mapa de Serviços](#3-mapa-de-serviços)
- [4. Mapa de Integrações](#4-mapa-de-integrações)
- [5. Mapa de Mensageria](#5-mapa-de-mensageria)
- [6. Mapa de Dados](#6-mapa-de-dados)
- [7. Mapa de Segurança](#7-mapa-de-segurança)
- [8. Mapa de Monitoramento](#8-mapa-de-monitoramento)
- [Referências](#referências)
- [Histórico de Revisão](#histórico-de-revisão)

---

## 1. Visão de Alto Nível

```mermaid
graph TB
    subgraph "USUÁRIOS"
        U[Usuário]
        A[Agente]
        M[Manager]
        AD[Admin]
    end

    subgraph "FRONTEND"
        FE[Next.js 14<br/>React + TypeScript<br/>Tailwind + Shadcn]
    end

    subgraph "BACKEND"
        BE[Spring Boot 3<br/>Java 25<br/>Clean Architecture]
    end

    subgraph "BANCO DE DADOS"
        PG[PostgreSQL 16<br/>Schema per Tenant]
        RD[Redis 7<br/>Cache + Sessions]
    end

    subgraph "MENSAGERIA"
        RM[RabbitMQ 3<br/>Event-Driven]
    end

    subgraph "INTEGRAÇÕES EXTERNAS"
        WA[WhatsApp<br/>Evolution API]
        EM[Email<br/>SMTP/SendGrid]
        SM[SMS<br/>Twilio]
        OAI[OpenAI<br/>GPT-4/GPT-3.5]
        GG[Google<br/>Calendar/Contacts]
        ST[Stripe<br/>Pagamentos]
    end

    subgraph "ARMAZENAMENTO"
        S3[S3/MinIO<br/>Arquivos + Mídia]
    end

    subgraph "MONITORAMENTO"
        PR[Prometheus<br/>Métricas]
        GF[Grafana<br/>Dashboards]
        LK[Loki<br/>Logs]
    end

    U --> FE
    A --> FE
    M --> FE
    AD --> FE
    FE --> BE
    BE --> PG
    BE --> RD
    BE --> RM
    BE --> WA
    BE --> EM
    BE --> SM
    BE --> OAI
    BE --> GG
    BE --> ST
    BE --> S3
    BE --> PR
    PR --> GF
    BE --> LK
```

**Fonte:** [00-core/Architecture.md](./00-core/Architecture.md), [00-core/TechStack.md](./00-core/TechStack.md)

---

## 2. Arquitetura de Camadas

```mermaid
graph TB
    subgraph "PRESENTATION LAYER"
        CTRL[REST Controllers]
        REQ[Request DTOs]
        RES[Response DTOs]
    end

    subgraph "APPLICATION LAYER"
        SVC[Application Services]
        CMD[Commands]
        QRY[Queries]
        MAP[DTO Mappers]
    end

    subgraph "DOMAIN LAYER"
        ENT[Entities]
        VO[Value Objects]
        EVT[Domain Events]
        PORT[Repository Interfaces<br/>Ports]
    end

    subgraph "INFRASTRUCTURE LAYER"
        JPA[JPA Repositories]
        CACH[Redis Cache]
        MQ[RabbitMQ Publisher/Consumer]
        EXT[External API Clients]
        SEC[Security Filter]
    end

    CTRL --> SVC
    SVC --> ENT
    ENT --> PORT
    JPA --> PORT
    CACH --> PORT
    MQ --> EVT
    SEC --> CTRL
```

**Regra de Dependência:** As dependências sempre apontam de fora para dentro. A camada Domain nunca depende de nenhuma outra camada.

**Fonte:** [00-core/Architecture.md](./00-core/Architecture.md)

---

## 3. Mapa de Serviços

```mermaid
graph LR
    subgraph "SERVIÇOS APLICACIONAIS"
        AUTH[Auth Service]
        USER[User Service]
        COMP[Company Service]
        CONT[Contact Service]
        LEAD[Lead Service]
        CUST[Customer Service]
        PIPE[Pipeline Service]
        CHAT[Chat Service]
        MSG[Message Service]
        CAMP[Campaign Service]
        AUTO[Automation Service]
        DASH[Dashboard Service]
        RPT[Report Service]
        AUD[Audit Service]
        AI[AI Service]
        NOTIF[Notification Service]
        SCHED[Scheduler Service]
        WH[Webhook Service]
        FS[FileStorage Service]
    end

    AUTH --> USER
    USER --> COMP
    COMP --> CONT
    CONT --> LEAD
    LEAD --> CUST
    LEAD --> PIPE
    CONT --> CHAT
    CHAT --> MSG
    CAMP --> MSG
    AUTO --> MSG
    DASH --> PIPE
    DASH --> CHAT
    RPT --> PIPE
    RPT --> CHAT
    AUD --> USER
    AI --> MSG
    NOTIF --> USER
    SCHED --> AUTO
    WH --> MSG
    FS --> MSG
```

**Fonte:** [01-backend/Modules.md](./01-backend/Modules.md)

---

## 4. Mapa de Integrações

```mermaid
graph TB
    subgraph "CRM SaaS"
        BE[Backend]
    end

    subgraph "COMUNICAÇÃO"
        WA[WhatsApp<br/>Evolution API]
        EM[Email<br/>SMTP/SendGrid]
        SM[SMS<br/>Twilio/Vonage]
    end

    subgraph "INTELIGÊNCIA ARTIFICIAL"
        OAI[OpenAI<br/>GPT-4/GPT-3.5]
    end

    subgraph "ECOSSISTEMA GOOGLE"
        GC[Google Calendar]
        GCT[Google Contacts]
        GO[Google OAuth]
    end

    subgraph "PAGAMENTOS"
        STRIPE[Stripe<br/>Checkout + Webhooks]
    end

    subgraph "ARMAZENAMENTO"
        S3[S3/MinIO<br/>Files + Media]
    end

    BE -->|Send/Receive Messages| WA
    BE -->|Send Emails| EM
    BE -->|Send SMS| SM
    BE -->|AI Features| OAI
    BE -->|Calendar Sync| GC
    BE -->|Contact Sync| GCT
    BE -->|OAuth Login| GO
    BE -->|Payments| STRIPE
    BE -->|File Upload/Download| S3
```

**Fonte:** [04-integrations/README.md](./04-integrations/README.md)

---

## 5. Mapa de Mensageria

```mermaid
graph TB
    subgraph "PRODUTORES"
        P1[Lead Service]
        P2[Contact Service]
        P3[Pipeline Service]
        P4[Message Service]
        P5[Campaign Service]
        P6[Automation Service]
    end

    subgraph "RABBITMQ"
        EX1[Exchange: domain.events]
        Q1[Queue: lead.events]
        Q2[Queue: contact.events]
        Q3[Queue: pipeline.events]
        Q4[Queue: message.events]
        Q5[Queue: campaign.events]
        DLQ[Dead Letter Queue]
    end

    subgraph "CONSUMIDORES"
        C1[Cache Invalidation Handler]
        C2[Notification Handler]
        C3[Audit Handler]
        C4[Analytics Handler]
        C5[Webhook Handler]
    end

    P1 --> EX1
    P2 --> EX1
    P3 --> EX1
    P4 --> EX1
    P5 --> EX1
    P6 --> EX1

    EX1 --> Q1
    EX1 --> Q2
    EX1 --> Q3
    EX1 --> Q4
    EX1 --> Q5
    EX1 --> DLQ

    Q1 --> C1
    Q1 --> C2
    Q1 --> C3
    Q2 --> C1
    Q2 --> C2
    Q3 --> C4
    Q4 --> C1
    Q4 --> C2
    Q5 --> C5
```

**Fonte:** [01-backend/Events.md](./01-backend/Events.md)

---

## 6. Mapa de Dados

```mermaid
graph TB
    subgraph "POSTGRESQL"
        SCHEMA[Tenant Schema]
        
        subgraph "IDENTITY"
            U[users]
            RT[refresh_tokens]
            R[roles]
            UR[user_roles]
        end

        subgraph "COMPANY"
            CO[companies]
            CS[company_settings]
            SUB[subscriptions]
        end

        subgraph "CONTACT"
            CT[contacts]
            CA[contact_addresses]
            T[tags]
            CTT[contact_tags]
        end

        subgraph "PIPELINE"
            PL[pipelines]
            ST[stages]
            OP[opportunities]
            OH[opportunity_history]
        end

        subgraph "COMMUNICATION"
            CV[conversations]
            MG[messages]
            MT[message_templates]
            MA[message_attachments]
        end

        subgraph "CAMPAIGN"
            CM[campaigns]
            CS2[campaign_steps]
            AU[automations]
            AT[automation_triggers]
            AA[automation_actions]
        end

        subgraph "AUDIT"
            AL[audit_logs]
            EV[events]
        end
    end

    subgraph "REDIS"
        CACHE[Cache Layer]
        SESS[Sessions]
        LOCK[Distributed Locks]
        RATE[Rate Limiting]
    end

    U --> RT
    U --> UR
    UR --> R
    CO --> CS
    CO --> SUB
    CO --> CT
    CT --> CA
    CT --> CTT
    CTT --> T
    CO --> PL
    PL --> ST
    PL --> OP
    OP --> CT
    OP --> ST
    CO --> CV
    CV --> CT
    CV --> MG
    MG --> MA
    CO --> CM
    CM --> CS2
    CO --> AU
    AU --> AT
    AU --> AA
    CO --> AL
    AL --> U
```

**Fonte:** [03-database/ERD.md](./03-database/ERD.md), [03-database/Entities.md](./03-database/Entities.md)

---

## 7. Mapa de Segurança

```mermaid
graph TB
    subgraph "CLIENTE"
        REQ[HTTP Request]
    end

    subgraph "SECURITY FILTERS"
        CORS[CORS Filter]
        JWT[JWT Filter]
        TENANT[Tenant Filter]
        RATE[Rate Limit Filter]
        AUD[Audit Filter]
    end

    subgraph "AUTENTICAÇÃO"
        LOGIN[Login Endpoint]
        ACCESS[Access Token<br/>15 min]
        REFRESH[Refresh Token<br/>7 dias]
    end

    subgraph "AUTORIZAÇÃO"
        ROLE[RBAC Roles]
        PERM[Permissions]
        GUARD[Method Security<br/>@PreAuthorize]
    end

    subgraph "DADOS"
        ENC[Encryption at Rest]
        TLS[TLS in Transit]
        HASH[Bcrypt Passwords]
    end

    REQ --> CORS
    CORS --> JWT
    JWT --> TENANT
    TENANT --> RATE
    RATE --> AUD
    AUD --> GUARD

    LOGIN --> ACCESS
    LOGIN --> REFRESH
    ACCESS --> JWT
    REFRESH --> LOGIN

    ROLE --> PERM
    PERM --> GUARD
```

**Fonte:** [01-backend/Auth.md](./01-backend/Auth.md), [05-business-rules/Permissions.md](./05-business-rules/Permissions.md)

---

## 8. Mapa de Monitoramento

```mermaid
graph TB
    subgraph "APLICAÇÃO"
        BE[Backend<br/>/actuator/prometheus]
        FE[Frontend<br/>Web Vitals]
    end

    subgraph "COLETA"
        PROM[Prometheus<br/>Scrape every 15s]
        PROMTAIL[Promtail<br/>Log Collection]
    end

    subgraph "ARMazenAMENTO"
        TSDB[Prometheus TSDB<br/>Metrics]
        LOKI[Loki<br/>Logs]
    end

    subgraph "VISUALIZAÇÃO"
        GRAFANA[Grafana<br/>Dashboards]
    end

    subgraph "ALERTAS"
        AM[Alertmanager]
        SLACK[Slack Notifications]
        EMAIL[Email Alerts]
    end

    BE --> PROM
    FE --> PROM
    BE --> PROMTAIL
    PROM --> TSDB
    PROMTAIL --> LOKI
    TSDB --> GRAFANA
    LOKI --> GRAFANA
    TSDB --> AM
    AM --> SLACK
    AM --> EMAIL
```

**Fonte:** [06-devops/Monitoring.md](./06-devops/Monitoring.md), [06-devops/Metrics.md](./06-devops/Metrics.md)

---

## Referências

| Documento | Caminho |
|---|---|
| Arquitetura | [00-core/Architecture.md](./00-core/Architecture.md) |
| TechStack | [00-core/TechStack.md](./00-core/TechStack.md) |
| Módulos | [01-backend/Modules.md](./01-backend/Modules.md) |
| Integrações | [04-integrations/README.md](./04-integrations/README.md) |
| Events | [01-backend/Events.md](./01-backend/Events.md) |
| Database | [03-database/ERD.md](./03-database/ERD.md) |
| Security | [01-backend/Auth.md](./01-backend/Auth.md) |
| DevOps | [06-devops/Monitoring.md](./06-devops/Monitoring.md) |
| SUMMARY | [SUMMARY.md](./SUMMARY.md) |

---

## Histórico de Revisão

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial do mapa do sistema |
