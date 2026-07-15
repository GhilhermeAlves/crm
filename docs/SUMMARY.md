# SUMMARY — Resumo Executivo do Projeto

## Objetivo

Consolidar toda a arquitetura do CRM SaaS Omnichannel em um único documento que permita compreender o projeto completo em menos de 15 minutos.

## Índice

- [1. Objetivo do CRM](#1-objetivo-do-crm)
- [2. Visão Geral](#2-visão-geral)
- [3. Stack Tecnológica](#3-stack-tecnológica)
- [4. Arquitetura](#4-arquitetura)
- [5. Organização do Projeto](#5-organização-do-projeto)
- [6. Módulos (Bounded Contexts)](#6-módulos-bounded-contexts)
- [7. Banco de Dados](#7-banco-de-dados)
- [8. Backend](#8-backend)
- [9. Frontend](#9-frontend)
- [10. Integrações](#10-integrações)
- [11. Segurança](#11-segurança)
- [12. Inteligência Artificial](#12-inteligência-artificial)
- [13. Roadmap](#13-roadmap)
- [14. Escalabilidade](#14-escalabilidade)
- [15. Fluxo Geral do Sistema](#15-fluxo-geral-do-sistema)
- [16. Principais Decisões Arquiteturais](#16-principais-decisões-arquiteturais)
- [17. Resumo dos Módulos](#17-resumo-dos-módulos)
- [18. Referências Cruzadas](#18-referências-cruzadas)
- [Histórico de Revisão](#histórico-de-revisão)

---

## 1. Objetivo do CRM

> Uma plataforma CRM SaaS Omnichannel com foco principal em WhatsApp, projetada para empresas de todos os tamanhos que precisam gerenciar relacionamentos com clientes, automação de vendas e comunicação multicanal de forma centralizada e escalável.

**Referência:** [00-core/Vision.md](./00-core/Vision.md)

---

## 2. Visão Geral

O sistema é um CRM SaaS com foco em WhatsApp que centraliza comunicação multicanal, gestão de vendas, automação e IA integrada. Opera como monolito modular com boundaries claros, preparado para decomposição em microsserviços.

| Aspecto | Descrição |
|---|---|
| **Tipo** | SaaS B2B |
| **Canal Primário** | WhatsApp |
| **Multi-tenant** | Sim (Schema per tenant) |
| **Arquitetura** | Modular Monolith → Microservices Ready |
| **Prazo de vida planejado** | 10+ anos |

**Referência:** [00-core/Vision.md](./00-core/Vision.md), [00-core/Architecture.md](./00-core/Architecture.md)

---

## 3. Stack Tecnológica

### Frontend

| Tecnologia | Versão | Finalidade |
|---|---|---|
| React | 18.x | Biblioteca UI |
| Next.js | 14.x | Framework (App Router) |
| TypeScript | 5.x | Tipagem estática |
| Tailwind CSS | 3.x | CSS utility-first |
| Shadcn UI | latest | Component library |

### Backend

| Tecnologia | Versão | Finalidade |
|---|---|---|
| Java | 21 LTS | Linguagem principal |
| Spring Boot | 3.x | Framework principal |
| PostgreSQL | 16 | Database relacional |
| Redis | 7 | Cache e sessões |
| RabbitMQ | 3 | Message broker |
| Flyway | 10+ | Database migrations |
| JWT | — | Autenticação |
| Docker | 24+ | Containerização |
| OpenAPI | 3.1 | API documentation |

### Infraestrutura

| Componente | Tecnologia |
|---|---|
| Container | Docker |
| Orchestration | Kubernetes (futuro) |
| CI/CD | GitHub Actions |
| Monitoring | Prometheus + Grafana |
| Logs | Loki + Promtail |
| Storage | MinIO / S3 |

**Referência:** [00-core/TechStack.md](./00-core/TechStack.md)

---

## 4. Arquitetura

### Padrões Arquiteturais

| Padrão | Aplicação |
|---|---|
| Clean Architecture | Separação de camadas |
| Hexagonal Architecture | Portas e adaptadores |
| DDD | Bounded contexts |
| SOLID | Princípios de design |
| Repository Pattern | Acesso a dados |
| DTO Pattern | Transferência de dados |
| Mapper Pattern | Conversão entre camadas |
| Event-Driven | Comunicação assíncrona |

### Camadas

```
Presentation → Application → Domain → Infrastructure
```

| Camada | Responsabilidade |
|---|---|
| **Presentation** | REST Controllers, Request/Response DTOs |
| **Application** | Use Cases, Commands, Queries, Services |
| **Domain** | Entities, Value Objects, Domain Events, Repository Interfaces |
| **Infrastructure** | JPA Repositories, Cache, MQ, External APIs |

**Referência:** [00-core/Architecture.md](./00-core/Architecture.md), [00-core/DesignPatterns.md](./00-core/DesignPatterns.md)

---

## 5. Organização do Projeto

```
docs/
├── 00-core/           → Fundação (9 docs)
├── 01-backend/        → API Backend (27 docs)
├── 02-frontend/       → Frontend (22 docs)
├── 03-database/       → Banco de dados (11 docs)
├── 04-integrations/   → Integrações (10 docs)
├── 05-business-rules/ → Regras de negócio (11 docs)
├── 06-devops/         → DevOps (9 docs)
├── 07-roadmap/        → Roadmap (6 docs)
└── (arquivos de síntese)
```

**Referência:** [00-core/FolderStructure.md](./00-core/FolderStructure.md)

---

## 6. Módulos (Bounded Contexts)

| Módulo | Responsabilidade Principal |
|---|---|
| **Identity** | Autenticação, autorização, usuários |
| **Company** | Multi-tenancy, configurações |
| **Contact** | Contatos, segmentação, tags |
| **Pipeline** | Pipeline de vendas, oportunidades |
| **Communication** | Chat, mensagens, conversas |
| **Campaign** | Campanhas, automações |
| **Analytics** | Relatórios, dashboards |
| **Integration** | WhatsApp, Email, APIs externas |

**Referência:** [01-backend/Modules.md](./01-backend/Modules.md)

---

## 7. Banco de Dados

| Aspecto | Decisão |
|---|---|
| Engine | PostgreSQL 16 |
| Primary Key | UUID v4 |
| Multi-tenant | Schema per tenant |
| Soft Delete | Coluna `deleted_at` |
| Migrations | Flyway |
| Auditing | Tabela `audit_logs` (imutável) |
| Cache | Redis 7 |

### Principais Tabelas

| Contexto | Tabelas |
|---|---|
| Identity | `users`, `refresh_tokens`, `roles`, `user_roles` |
| Company | `companies`, `company_settings`, `subscriptions` |
| Contact | `contacts`, `contact_addresses`, `tags`, `contact_tags` |
| Pipeline | `pipelines`, `stages`, `opportunities`, `opportunity_history` |
| Communication | `conversations`, `messages`, `message_templates` |
| Campaign | `campaigns`, `campaign_steps`, `automations` |
| Audit | `audit_logs`, `events` |

**Referência:** [03-database/Overview.md](./03-database/Overview.md), [03-database/ERD.md](./03-database/ERD.md), [03-database/Entities.md](./03-database/Entities.md)

---

## 8. Backend

### Arquitetura Interna

```
Controller → Service → Domain Entity → Repository → Database
     │                                          │
     │         Event Publisher → RabbitMQ        │
     │                                          │
     └── Cache (Redis) ←────────────────────────┘
```

### Módulos Backend

| Módulo | Documentação |
|---|---|
| Auth | [01-backend/Auth.md](./01-backend/Auth.md) |
| Users | [01-backend/Users.md](./01-backend/Users.md) |
| Companies | [01-backend/Companies.md](./01-backend/Companies.md) |
| Contacts | [01-backend/Contacts.md](./01-backend/Contacts.md) |
| Leads | [01-backend/Leads.md](./01-backend/Leads.md) |
| Customers | [01-backend/Customers.md](./01-backend/Customers.md) |
| Pipeline | [01-backend/Pipeline.md](./01-backend/Pipeline.md) |
| Chat | [01-backend/Chat.md](./01-backend/Chat.md) |
| Conversations | [01-backend/Conversations.md](./01-backend/Conversations.md) |
| Messages | [01-backend/Messages.md](./01-backend/Messages.md) |
| Templates | [01-backend/Templates.md](./01-backend/Templates.md) |
| Campaigns | [01-backend/Campaigns.md](./01-backend/Campaigns.md) |
| Automations | [01-backend/Automations.md](./01-backend/Automations.md) |
| Webhooks | [01-backend/Webhooks.md](./01-backend/Webhooks.md) |
| Notifications | [01-backend/Notifications.md](./01-backend/Notifications.md) |
| Dashboard | [01-backend/Dashboard.md](./01-backend/Dashboard.md) |
| Reports | [01-backend/Reports.md](./01-backend/Reports.md) |
| Audit | [01-backend/Audit.md](./01-backend/Audit.md) |
| Events | [01-backend/Events.md](./01-backend/Events.md) |
| Scheduler | [01-backend/Scheduler.md](./01-backend/Scheduler.md) |
| Cache | [01-backend/Cache.md](./01-backend/Cache.md) |
| FileStorage | [01-backend/FileStorage.md](./01-backend/FileStorage.md) |
| AI | [01-backend/AI.md](./01-backend/AI.md) |

**Referência:** [01-backend/Overview.md](./01-backend/Overview.md)

---

## 9. Frontend

### Stack

| Camada | Tecnologia |
|---|---|
| Framework | Next.js 14 (App Router) |
| UI | React 18 + Shadcn UI |
| Estilo | Tailwind CSS |
| State (Server) | React Query (TanStack) |
| State (Client) | React Context + useState/useReducer |
| Forms | React Hook Form + Zod |
| Gráficos | Recharts |

### Páginas Principais

| Rota | Página |
|---|---|
| `/dashboard` | Dashboard |
| `/leads` | Leads |
| `/customers` | Clientes |
| `/chat` | Chat |
| `/pipeline` | Pipeline Kanban |
| `/campaigns` | Campanhas |
| `/reports` | Relatórios |
| `/settings` | Configurações |

**Referência:** [02-frontend/Overview.md](./02-frontend/Overview.md), [02-frontend/Routing.md](./02-frontend/Routing.md)

---

## 10. Integrações

| Integração | Tecnologia | Status |
|---|---|---|
| WhatsApp | Evolution API / Meta Business API | Documentado |
| Email | SMTP / SendGrid | Documentado |
| SMS | Twilio / Vonage | Documentado |
| OpenAI | API REST | Documentado |
| Google | OAuth 2.0 / Calendar / Contacts | Documentado |
| Pagamento | Stripe | Documentado |
| Storage | S3 / MinIO | Documentado |

**Referência:** [04-integrations/README.md](./04-integrations/README.md)

---

## 11. Segurança

| Aspecto | Implementação |
|---|---|
| Autenticação | JWT (Access + Refresh Token) |
| Autorização | RBAC (Roles + Permissions) |
| Senhas | Bcrypt hashing |
| HTTPS | Obrigatório em produção |
| Rate Limiting | Por IP e por tenant |
| Auditoria | Tabela imutável `audit_logs` |
| LGPD | Compliance desde o dia 1 |
| Dados sensíveis | Criptografia em repouso |

**Referência:** [01-backend/Auth.md](./01-backend/Auth.md), [05-business-rules/Permissions.md](./05-business-rules/Permissions.md)

---

## 12. Inteligência Artificial

| Feature | Modelo | Status |
|---|---|---|
| Sugestão de respostas | GPT-4 | Documentado |
| Classificação de leads | GPT-3.5 | Documentado |
| Análise de sentimento | GPT-3.5 | Documentado |
| Resumo de conversas | GPT-4 | Documentado |
| Geração de conteúdo | GPT-4 | Documentado |

**Referência:** [01-backend/AI.md](./01-backend/AI.md), [04-integrations/OpenAI.md](./04-integrations/OpenAI.md)

---

## 13. Roadmap

| Fase | Duração | Status |
|---|---|---|
| MVP | 14 semanas | Planejado |
| v1.0 | 17 semanas | Planejado |
| v2.0 | 34 semanas | Planejado |
| v3.0 | Não documentado | Futuro |
| 10 anos | Visão de longo prazo | Planejado |

**Referência:** [07-roadmap/README.md](./07-roadmap/README.md)

---

## 14. Escalabilidade

| Aspecto | Estratégia |
|---|---|
| Database | Schema per tenant + partitioning futuro |
| Cache | Redis com eviction LRU |
| Messaging | RabbitMQ com clustering |
| Backend | Horizontal scaling (Kubernetes) |
| Frontend | CDN + Static Generation |
| Storage | S3 (escala infinita) |

---

## 15. Fluxo Geral do Sistema

```
1. Usuário acessa o CRM (Frontend)
        │
2. Autenticação via JWT
        │
3. Request chega ao Backend (REST API)
        │
4. Validação + Autorização (RBAC)
        │
5. Application Service processa
        │
6. Domain Entity aplica regras
        │
7. Repository persiste no PostgreSQL
        │
8. Event é publicado no RabbitMQ
        │
9. Cache é atualizado no Redis
        │
10. Response retorna ao Frontend
        │
11. WebSocket notifica mudanças em tempo real
```

---

## 16. Principais Decisões Arquiteturais

| ADR | Decisão | Justificativa |
|---|---|---|
| ADR-001 | Hexagonal + Clean Architecture | Flexibilidade e testabilidade |
| ADR-002 | Multi-tenant via Separate Schema | Isolamento + custo |
| ADR-003 | RabbitMQ como Message Broker | Simplicidade + AMQP padrão |
| ADR-004 | Flyway para Migrations | Simplicidade + Java nativo |
| ADR-005 | Next.js App Router | Performance + RSC |
| ADR-006 | UUID v4 como Primary Key | Distribuível + seguro |
| ADR-007 | JWT com Refresh Token | Stateless + UX |

**Referência:** [00-core/Decisions.md](./00-core/Decisions.md)

---

## 17. Resumo dos Módulos

| # | Módulo | Backend | Frontend | Database | Integração |
|---|---|---|---|---|---|
| 1 | Identity | Auth, Users | Login, Settings | users, tokens | — |
| 2 | Company | Companies | Settings | companies | — |
| 3 | Contact | Contacts | Leads, Customers | contacts, tags | — |
| 4 | Pipeline | Pipeline, Stages, Kanban | Pipeline, Kanban | pipelines, stages, opportunities | — |
| 5 | Communication | Chat, Conversations, Messages | Chat | conversations, messages | WhatsApp, Email |
| 6 | Campaign | Campaigns, Templates | Campaigns | campaigns | WhatsApp, Email |
| 7 | Analytics | Dashboard, Reports | Dashboard, Reports | (queries) | — |
| 8 | Integration | Webhooks, AI | — | integration_configs | WhatsApp, OpenAI, Google, Stripe |

---

## 18. Referências Cruzadas

### Documentos Fundação

| Documento | Caminho |
|---|---|
| Visão | [00-core/Vision.md](./00-core/Vision.md) |
| Constituição | [00-core/Constitution.md](./00-core/Constitution.md) |
| Stack | [00-core/TechStack.md](./00-core/TechStack.md) |
| Arquitetura | [00-core/Architecture.md](./00-core/Architecture.md) |
| Decisões | [00-core/Decisions.md](./00-core/Decisions.md) |

### Documentos de Síntese

| Documento | Caminho |
|---|---|
| Mapa do Sistema | [SYSTEM_MAP.md](./SYSTEM_MAP.md) |
| Mapa de Módulos | [MODULES_MAP.md](./MODULES_MAP.md) |
| Mapa de Dependências | [DEPENDENCIES.md](./DEPENDENCIES.md) |
| Fluxo de Dados | [DATA_FLOW.md](./DATA_FLOW.md) |
| Mapa de APIs | [API_MAP.md](./API_MAP.md) |
| Mapa do Banco | [DATABASE_MAP.md](./DATABASE_MAP.md) |
| Mapa do Frontend | [FRONTEND_MAP.md](./FRONTEND_MAP.md) |
| Mapa do Backend | [BACKEND_MAP.md](./BACKEND_MAP.md) |
| Mapa de Segurança | [SECURITY_MAP.md](./SECURITY_MAP.md) |
| Resumo do Roadmap | [ROADMAP_SUMMARY.md](./ROADMAP_SUMMARY.md) |

---

## Histórico de Revisão

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial do resumo executivo |
