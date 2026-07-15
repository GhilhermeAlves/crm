# DATA_FLOW — Mapa de Fluxo dos Dados

## Objetivo

Documentar o fluxo dos dados em cada funcionalidade principal do sistema, desde a entrada até a persistência e notificação.

## Índice

- [1. Fluxo de Login](#1-fluxo-de-login)
- [2. Fluxo de Lead](#2-fluxo-de-lead)
- [3. Fluxo de Cliente (Conversão Lead → Cliente)](#3-fluxo-de-cliente-conversão-lead--cliente)
- [4. Fluxo de Pipeline (Movimentação Kanban)](#4-fluxo-de-pipeline-movimentação-kanban)
- [5. Fluxo de Mensagem](#5-fluxo-de-mensagem)
- [6. Fluxo de Campanha](#6-fluxo-de-campanha)
- [7. Fluxo de Automação](#7-fluxo-de-automação)
- [8. Fluxo de Dashboard](#8-fluxo-de-dashboard)
- [Referências](#referências)
- [Histórico de Revisão](#histórico-de-revisão)

---

## 1. Fluxo de Login

```mermaid
sequenceDiagram
    participant U as Usuário
    participant FE as Frontend
    participant BE as Backend
    participant DB as PostgreSQL
    participant RD as Redis

    U->>FE: Email + Senha
    FE->>BE: POST /api/v1/auth/login
    BE->>DB: SELECT user WHERE email = ?
    DB-->>BE: User record
    BE->>BE: Validar senha (Bcrypt)
    BE->>BE: Gerar Access Token (15min)
    BE->>BE: Gerar Refresh Token (7 dias)
    BE->>RD: Salvar Refresh Token
    BE-->>FE: {accessToken, refreshToken}
    FE->>FE: Armazenar token em memory
    FE->>FE: Armazenar refresh em HttpOnly cookie
    FE-->>U: Login realizado
```

**Fonte:** [01-backend/Auth.md](./01-backend/Auth.md)

---

## 2. Fluxo de Lead

```mermaid
sequenceDiagram
    participant U as Usuário
    participant FE as Frontend
    participant BE as Backend
    participant DB as PostgreSQL
    participant RM as RabbitMQ
    participant RD as Redis

    U->>FE: Criar lead
    FE->>BE: POST /api/v1/leads
    BE->>BE: Validar dados
    BE->>DB: INSERT INTO leads
    BE->>BE: Calcular score
    BE->>DB: UPDATE lead SET score = ?
    BE->>RM: Publicar LeadCreated
    BE->>RD: Invalidar cache de leads
    BE-->>FE: Lead criado
    RM->>RM: Consumer processa evento
    RM->>RD: Atualizar cache
```

**Fonte:** [01-backend/Leads.md](./01-backend/Leads.md), [05-business-rules/Lead.md](./05-business-rules/Lead.md)

---

## 3. Fluxo de Cliente (Conversão Lead → Cliente)

```mermaid
sequenceDiagram
    participant A as Agente
    participant FE as Frontend
    participant BE as Backend
    participant DB as PostgreSQL
    participant RM as RabbitMQ

    A->>FE: Marcar oportunidade como WON
    FE->>BE: POST /api/v1/pipelines/opportunities/{id}/won
    BE->>DB: UPDATE opportunity SET status = 'WON'
    BE->>DB: INSERT INTO customers (dados do lead)
    BE->>RM: Publicar OpportunityWon
    BE->>RM: Publicar CustomerCreated
    BE-->>FE: Cliente criado
    RM->>RM: Handler inicia onboarding
```

**Fonte:** [01-backend/Customers.md](./01-backend/Customers.md), [01-backend/Pipeline.md](./01-backend/Pipeline.md)

---

## 4. Fluxo de Pipeline (Movimentação Kanban)

```mermaid
sequenceDiagram
    participant A as Agente
    participant FE as Frontend
    participant BE as Backend
    participant DB as PostgreSQL
    participant RM as RabbitMQ
    participant RD as Redis
    participant WS as WebSocket

    A->>FE: Drag card para próxima coluna
    FE->>BE: POST /api/v1/kanban/move
    BE->>BE: Validar transição permitida
    BE->>DB: UPDATE opportunity SET stage_id = ?
    BE->>DB: INSERT INTO opportunity_history
    BE->>RM: Publicar OpportunityMoved
    BE->>RD: Invalidar cache
    BE-->>FE: Oportunidade movida
    RM->>RM: Atualizar métricas
    WS->>FE: Broadcast para outros usuários
```

**Fonte:** [01-backend/Kanban.md](./01-backend/Kanban.md), [01-backend/Pipeline.md](./01-backend/Pipeline.md)

---

## 5. Fluxo de Mensagem

### Envio (Agente → Contato)

```mermaid
sequenceDiagram
    participant A as Agente
    participant FE as Frontend
    participant BE as Backend
    participant DB as PostgreSQL
    participant WA as WhatsApp API
    participant WS as WebSocket

    A->>FE: Digitar mensagem
    FE->>BE: POST /api/v1/messages
    BE->>DB: INSERT INTO messages (status=PENDING)
    BE->>WA: Enviar mensagem
    WA-->>BE: External ID
    BE->>DB: UPDATE message SET external_id = ?, status = 'SENT'
    BE-->>FE: Mensagem enviada
    WA->>BE: Webhook (DELIVERED)
    BE->>DB: UPDATE message SET status = 'DELIVERED'
    WA->>BE: Webhook (READ)
    BE->>DB: UPDATE message SET status = 'READ'
```

### Recebimento (Contato → Agente)

```mermaid
sequenceDiagram
    participant WA as WhatsApp API
    participant BE as Backend
    participant DB as PostgreSQL
    participant RM as RabbitMQ
    participant WS as WebSocket
    participant A as Agente

    WA->>BE: Webhook (nova mensagem)
    BE->>BE: Validar assinatura
    BE->>DB: INSERT INTO messages
    BE->>DB: Criar/reabrir conversa
    BE->>RM: Publicar MessageReceived
    BE->>WS: Notificar agente
    WS->>FE: Nova mensagem
    FE-->>A: Exibir mensagem
```

**Fonte:** [01-backend/Messages.md](./01-backend/Messages.md), [01-backend/Chat.md](./01-backend/Chat.md)

---

## 6. Fluxo de Campanha

```mermaid
sequenceDiagram
    participant M as Manager
    participant FE as Frontend
    participant BE as Backend
    participant DB as PostgreSQL
    participant WA as WhatsApp API
    participant EM as Email API

    M->>FE: Criar campanha
    FE->>BE: POST /api/v1/campaigns
    BE->>DB: INSERT INTO campaigns
    BE-->>FE: Campanha criada

    M->>FE: Agendar/enviar campanha
    FE->>BE: POST /api/v1/campaigns/{id}/start
    BE->>DB: SELECT contacts WHERE segment = ?
    loop Para cada contato
        BE->>DB: INSERT INTO messages
        BE->>WA: Enviar mensagem
        BE->>EM: Enviar email (se multicanal)
    end
    BE->>DB: UPDATE campaign SET status = 'RUNNING'
    BE-->>FE: Campanha em execução
```

**Fonte:** [01-backend/Campaigns.md](./01-backend/Campaigns.md), [05-business-rules/Campaign.md](./05-business-rules/Campaign.md)

---

## 7. Fluxo de Automação

```mermaid
sequenceDiagram
    participant SYS as Sistema
    participant RM as RabbitMQ
    participant BE as Backend
    participant DB as PostgreSQL
    participant WA as WhatsApp API

    SYS->>RM: Evento (ex: LeadCreated)
    RM->>BE: Consumer recebe evento
    BE->>DB: SELECT automations WHERE trigger = 'LEAD_CREATED'
    loop Para cada automação
        BE->>BE: Verificar condições
        loop Para cada action
            alt SEND_MESSAGE
                BE->>DB: INSERT INTO messages
                BE->>WA: Enviar mensagem
            else ADD_TAG
                BE->>DB: INSERT INTO contact_tags
            else WAIT
                BE->>BE: Agendar execução
            end
        end
    end
    BE->>DB: Registrar execução
```

**Fonte:** [01-backend/Automations.md](./01-backend/Automations.md)

---

## 8. Fluxo de Dashboard

```mermaid
sequenceDiagram
    participant U as Usuário
    participant FE as Frontend
    participant BE as Backend
    participant RD as Redis
    participant DB as PostgreSQL
    participant WS as WebSocket

    U->>FE: Acessar dashboard
    FE->>BE: GET /api/v1/dashboard/kpis
    BE->>RD: Verificar cache
    alt Cache Hit
        RD-->>BE: Dados cacheados
    else Cache Miss
        BE->>DB: Queries de métricas
        DB-->>BE: Resultados
        BE->>RD: Cachear resultado (TTL 5min)
    end
    BE-->>FE: KPIs
    FE-->>U: Exibir dashboard

    loop A cada 30 segundos
        WS->>FE: Atualização em tempo real
        FE->>FE: Atualizar KPIs
    end
```

**Fonte:** [01-backend/Dashboard.md](./01-backend/Dashboard.md)

---

## Referências

| Documento | Caminho |
|---|---|
| Backend Overview | [01-backend/Overview.md](./01-backend/Overview.md) |
| Events | [01-backend/Events.md](./01-backend/Events.md) |
| Auth | [01-backend/Auth.md](./01-backend/Auth.md) |
| Leads | [01-backend/Leads.md](./01-backend/Leads.md) |
| Pipeline | [01-backend/Pipeline.md](./01-backend/Pipeline.md) |
| Messages | [01-backend/Messages.md](./01-backend/Messages.md) |
| Campaigns | [01-backend/Campaigns.md](./01-backend/Campaigns.md) |
| Automations | [01-backend/Automations.md](./01-backend/Automations.md) |
| Dashboard | [01-backend/Dashboard.md](./01-backend/Dashboard.md) |
| SUMMARY | [SUMMARY.md](./SUMMARY.md) |

---

## Histórico de Revisão

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial do mapa de fluxos |
