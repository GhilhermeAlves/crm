# MODULES_MAP — Inventário Completo de Módulos

## Objetivo

Listar todos os módulos do sistema com objetivo, responsabilidades, dependências, status e documentação relacionada.

## Índice

- [Módulos Principais](#módulos-principais)
- [Matriz de Dependências](#matriz-de-dependências)
- [Referências](#referências)
- [Histórico de Revisão](#histórico-de-revisão)

---

## Módulos Principais

### 1. Identity

| Campo | Descrição |
|---|---|
| **Nome** | Identity |
| **Objetivo** | Autenticação, autorização e gestão de usuários |
| **Responsabilidades** | Login/logout, JWT, RBAC, perfis, convites |
| **Dependências** | Nenhuma (contexto raiz) |
| **Documentação** | [01-backend/Auth.md](./01-backend/Auth.md), [01-backend/Users.md](./01-backend/Users.md) |
| **Status** | Documentado |
| **Prioridade** | P0 |

### 2. Company

| Campo | Descrição |
|---|---|
| **Nome** | Company |
| **Objetivo** | Multi-tenancy e configurações de empresa |
| **Responsabilidades** | Gestão de tenants, schema isolation, billing, settings |
| **Dependências** | Identity |
| **Documentação** | [01-backend/Companies.md](./01-backend/Companies.md) |
| **Status** | Documentado |
| **Prioridade** | P0 |

### 3. Contact

| Campo | Descrição |
|---|---|
| **Nome** | Contact |
| **Objetivo** | Gestão de contatos e segmentação |
| **Responsabilidades** | CRUD contatos, tags, segmentos, campos customizados, importação |
| **Dependências** | Company, Identity |
| **Documentação** | [01-backend/Contacts.md](./01-backend/Contacts.md) |
| **Status** | Documentado |
| **Prioridade** | P0 |

### 4. Pipeline

| Campo | Descrição |
|---|---|
| **Nome** | Pipeline |
| **Objetivo** | Pipeline de vendas e gestão de oportunidades |
| **Responsabilidades** | Pipelines, estágios, oportunidades, kanban, métricas |
| **Dependências** | Company, Contact |
| **Documentação** | [01-backend/Pipeline.md](./01-backend/Pipeline.md), [01-backend/Stages.md](./01-backend/Stages.md), [01-backend/Kanban.md](./01-backend/Kanban.md) |
| **Status** | Documentado |
| **Prioridade** | P0 |

### 5. Communication

| Campo | Descrição |
|---|---|
| **Nome** | Communication |
| **Objetivo** | Comunicação multicanal (chat, mensagens) |
| **Responsabilidades** | Chat, conversas, mensagens, status de entrega |
| **Dependências** | Company, Contact, Integration (WhatsApp) |
| **Documentação** | [01-backend/Chat.md](./01-backend/Chat.md), [01-backend/Conversations.md](./01-backend/Conversations.md), [01-backend/Messages.md](./01-backend/Messages.md) |
| **Status** | Documentado |
| **Prioridade** | P0 |

### 6. Campaign

| Campo | Descrição |
|---|---|
| **Nome** | Campaign |
| **Objetivo** | Campanhas de marketing e automações |
| **Responsabilidades** | Campanhas, templates, automações, workflows |
| **Dependências** | Company, Contact, Communication, Integration |
| **Documentação** | [01-backend/Campaigns.md](./01-backend/Campaigns.md), [01-backend/Templates.md](./01-backend/Templates.md), [01-backend/Automations.md](./01-backend/Automations.md) |
| **Status** | Documentado |
| **Prioridade** | P1 |

### 7. Analytics

| Campo | Descrição |
|---|---|
| **Nome** | Analytics |
| **Objetivo** | Relatórios, dashboards e métricas de negócio |
| **Responsabilidades** | Dashboard, relatórios, métricas, KPIs |
| **Dependências** | Company, Contact, Pipeline, Communication, Campaign |
| **Documentação** | [01-backend/Dashboard.md](./01-backend/Dashboard.md), [01-backend/Reports.md](./01-backend/Reports.md) |
| **Status** | Documentado |
| **Prioridade** | P1 |

### 8. Integration

| Campo | Descrição |
|---|---|
| **Nome** | Integration |
| **Objetivo** | Integrações externas (WhatsApp, Email, APIs) |
| **Responsabilidades** | WhatsApp, Email, SMS, OpenAI, Google, Webhooks |
| **Dependências** | Company, Communication |
| **Documentação** | [04-integrations/README.md](./04-integrations/README.md) |
| **Status** | Documentado |
| **Prioridade** | P0 |

---

## Matriz de Dependências

| Módulo | Depende de | Dependem dele |
|---|---|---|
| Identity | — | Company, Contact, Pipeline, Communication, Campaign, Analytics |
| Company | Identity | Contact, Pipeline, Communication, Campaign, Analytics, Integration |
| Contact | Company, Identity | Pipeline, Communication, Campaign, Analytics |
| Pipeline | Company, Contact | Analytics |
| Communication | Company, Contact, Integration | Campaign, Analytics |
| Campaign | Company, Contact, Communication, Integration | Analytics |
| Analytics | Todos | — |
| Integration | Company | Communication, Campaign |

---

## Referências

| Documento | Caminho |
|---|---|
| Módulos detalhados | [01-backend/Modules.md](./01-backend/Modules.md) |
| Context Map | [01-backend/Modules.md](./01-backend/Modules.md#context-map) |
| Arquitetura | [00-core/Architecture.md](./00-core/Architecture.md) |
| SUMMARY | [SUMMARY.md](./SUMMARY.md) |

---

## Histórico de Revisão

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial do mapa de módulos |
