# Module Index

## Objetivo

Índice central de todos os módulos do sistema CRM com links para documentação oficial.

## Escopo

Todos os módulos funcionais, de infraestrutura e transversais do projeto.

## Como utilizar

Localize o módulo desejado e acesse os links de documentação oficial na coluna "Docs".

## Índice

### Módulos Funcionais

| Módulo | Docs Oficiais | Contexto |
|--------|---------------|----------|
| Auth | [Auth](../docs/01-backend/Auth.md), [Permissions](../docs/05-business-rules/Permissions.md) | Autenticação e autorização |
| Users | [Users](../docs/01-backend/Users.md) | Gerenciamento de usuários |
| Company | [Companies](../docs/01-backend/Companies.md), [DB Overview](../docs/03-database/Overview.md) | Multi-tenancy |
| Contacts | [Contacts](../docs/01-backend/Contacts.md), [Entities](../docs/03-database/Entities.md) | Gestão de contatos |
| Leads | [Leads](../docs/01-backend/Leads.md), [Lead Rules](../docs/05-business-rules/Lead.md) | Captação e qualificação |
| Customers | [Customers](../docs/01-backend/Customers.md), [Customer Rules](../docs/05-business-rules/Customer.md) | Gestão de clientes |
| Pipeline | [Pipeline](../docs/01-backend/Pipeline.md), [Stages](../docs/01-backend/Stages.md), [Pipeline Rules](../docs/05-business-rules/Pipeline.md) | Funil de vendas |
| Kanban | [Kanban BE](../docs/01-backend/Kanban.md), [Kanban FE](../docs/02-frontend/Kanban.md) | Quadro visual |
| Conversations | [Conversations](../docs/01-backend/Conversations.md) | Comunicação |
| Chat | [Chat BE](../docs/01-backend/Chat.md), [Chat FE](../docs/02-frontend/Chat.md) | Chat em tempo real |
| Messages | [Messages](../docs/01-backend/Messages.md) | Mensageria |
| Templates | [Templates](../docs/01-backend/Templates.md) | Modelos reutilizáveis |
| Campaigns | [Campaigns](../docs/01-backend/Campaigns.md), [Campaign Rules](../docs/05-business-rules/Campaign.md) | Marketing |
| Automations | [Automations](../docs/01-backend/Automations.md), [Automation Rules](../docs/05-business-rules/Automation.md) | Workflows |
| Webhooks | [Webhooks](../docs/01-backend/Webhooks.md) | Integração externa |
| Notifications | [Notifications BE](../docs/01-backend/Notifications.md), [Notifications FE](../docs/02-frontend/Notifications.md) | Push/in-app |
| Dashboard | [Dashboard BE](../docs/01-backend/Dashboard.md), [Dashboard FE](../docs/02-frontend/Dashboard.md) | Métricas |
| Reports | [Reports BE](../docs/01-backend/Reports.md), [Reports FE](../docs/02-frontend/Reports.md) | Analytics |
| AI | [AI BE](../docs/01-backend/AI.md), [AI Rules](../docs/05-business-rules/AI.md) | Inteligência artificial |
| Events | [Events](../docs/01-backend/Events.md) | Event bus |
| Audit | [Audit](../docs/01-backend/Audit.md) | Rastreabilidade |
| Cache | [Cache](../docs/01-backend/Cache.md) | Redis/cache |
| File Storage | [FileStorage](../docs/01-backend/FileStorage.md) | Upload/download |
| Scheduler | [Scheduler](../docs/01-backend/Scheduler.md) | Agendamento |
| Permissions | [Permissions BE](../docs/01-backend/Permissions.md), [Permissions FE](../docs/02-frontend/Permissions.md), [Permissions Rules](../docs/05-business-rules/Permissions.md) | RBAC |

### Módulos de Infraestrutura

| Módulo | Docs Oficiais | Contexto |
|--------|---------------|----------|
| Database | [Overview](../docs/03-database/Overview.md), [ERD](../docs/03-database/ERD.md), [Entities](../docs/03-database/Entities.md), [Migrations](../docs/03-database/Migrations.md), [Indexes](../docs/03-database/Indexes.md), [Performance](../docs/03-database/Performance.md), [Relationships](../docs/03-database/Relationships.md), [SoftDelete](../docs/03-database/SoftDelete.md), [UUID](../docs/03-database/UUID.md), [Backup](../docs/03-database/Backup.md), [Audit](../docs/03-database/Audit.md) | Banco de dados |
| Backend Architecture | [Architecture](../docs/00-core/Architecture.md), [Overview](../docs/01-backend/Overview.md), [Modules](../docs/01-backend/Modules.md) | Arquitetura backend |
| Frontend Architecture | [Overview](../docs/02-frontend/Overview.md), [Routing](../docs/02-frontend/Routing.md), [Layout](../docs/02-frontend/Layout.md) | Arquitetura frontend |

### Módulos de Integração

| Módulo | Docs Oficiais | Contexto |
|--------|---------------|----------|
| WhatsApp | [WhatsApp](../docs/04-integrations/WhatsApp.md), [EvolutionAPI](../docs/04-integrations/EvolutionAPI.md) | WhatsApp |
| OpenAI | [OpenAI](../docs/04-integrations/OpenAI.md) | IA externa |
| Email | [Email](../docs/04-integrations/Email.md) | SMTP/Email |
| Integrations (outras) | [SMS](../docs/04-integrations/SMS.md), [Google](../docs/04-integrations/Google.md), [Payment](../docs/04-integrations/Payment.md), [Storage](../docs/04-integrations/Storage.md) | Diversas |

### Módulos DevOps

| Módulo | Docs Oficiais | Contexto |
|--------|---------------|----------|
| Docker | [Docker](../docs/06-devops/Docker.md) | Containers |
| CI | [CI](../docs/06-devops/CI.md) | Integração contínua |
| CD | [CD](../docs/06-devops/CD.md) | Deploy contínuo |
| Kubernetes | [Kubernetes](../docs/06-devops/Kubernetes.md) | Orquestração |
| Monitoring | [Monitoring](../docs/06-devops/Monitoring.md), [Logs](../docs/06-devops/Logs.md), [Metrics](../docs/06-devops/Metrics.md) | Observabilidade |

## Referências

- Busca detalhada: [SEARCH_INDEX.md](SEARCH_INDEX.md)
- Roteador: [AI_ROUTER.md](AI_ROUTER.md)

## Histórico de Revisão

| Versão | Data | Autor | Alteração |
|--------|------|-------|-----------|
| 1.0 | 2026-07-15 | AI Agent | Criação inicial |
