# Search Index

## Objetivo

Índice pesquisável de todos os módulos com metadados completos para localização rápida.

## Escopo

Cada módulo listado com: Nome, Docs Oficiais, Contexto, Playbook, Prompt Recomendado, Dependências, Tecnologias.

## Como utilizar

Busque pelo módulo desejado. Use a coluna "Prompt Recomendado" como ponto de partida para interações com IA.

## Índice

### Auth
- **Docs Oficiais:** `docs/01-backend/Auth.md`, `docs/05-business-rules/Permissions.md`
- **Contexto:** Autenticação, autorização, controle de acesso
- **Playbook:** Consultar `AI_ROUTER.md` → Auth
- **Prompt Recomendado:** "Implementar autenticação para o módulo [X]. Consultar docs/01-backend/Auth.md e docs/05-business-rules/Permissions.md"
- **Dependências:** Users, Permissions
- **Tecnologias:** JWT, bcrypt, middleware

### Users
- **Docs Oficiais:** `docs/01-backend/Users.md`
- **Contexto:** Gerenciamento de usuários, perfis, papéis
- **Playbook:** Consultar `AI_ROUTER.md` → Users
- **Prompt Recomendado:** "Gerenciar usuários do sistema. Consultar docs/01-backend/Users.md"
- **Dependências:** Auth, Permissions
- **Tecnologias:** CRUD, validação

### Company
- **Docs Oficiais:** `docs/01-backend/Companies.md`, `docs/03-database/Overview.md`
- **Contexto:** Multi-tenancy, configuração de empresas
- **Playbook:** Consultar `AI_ROUTER.md` → Company
- **Prompt Recomendado:** "Configurar empresa/tenant. Consultar docs/01-backend/Companies.md e docs/03-database/Overview.md"
- **Dependências:** Database, Auth
- **Tecnologias:** Tenant isolation, schema

### Contacts
- **Docs Oficiais:** `docs/01-backend/Contacts.md`, `docs/03-database/Entities.md`
- **Contexto:** Gestão de contatos, relacionamentos
- **Playbook:** Consultar `AI_ROUTER.md` → Contacts
- **Prompt Recomendado:** "Gerenciar contatos. Consultar docs/01-backend/Contacts.md e docs/03-database/Entities.md"
- **Dependências:** Company, Database
- **Tecnologias:** CRUD, entidades

### Leads
- **Docs Oficiais:** `docs/01-backend/Leads.md`, `docs/05-business-rules/Lead.md`
- **Contexto:** Captação, qualificação, scoring, conversão
- **Playbook:** Consultar `AI_ROUTER.md` → Leads
- **Prompt Recomendado:** "Implementar módulo de leads com qualificação. Consultar docs/01-backend/Leads.md e docs/05-business-rules/Lead.md"
- **Dependências:** Pipeline, Contacts, Events
- **Tecnologias:** Scoring, state machine

### Customers
- **Docs Oficiais:** `docs/01-backend/Customers.md`, `docs/05-business-rules/Customer.md`
- **Contexto:** Gestão de clientes, regras de negócio
- **Playbook:** Consultar `AI_ROUTER.md` → Customers
- **Prompt Recomendado:** "Gerenciar clientes. Consultar docs/01-backend/Customers.md e docs/05-business-rules/Customer.md"
- **Dependências:** Contacts, Pipeline
- **Tecnologias:** CRUD, validação

### Pipeline
- **Docs Oficiais:** `docs/01-backend/Pipeline.md`, `docs/01-backend/Stages.md`, `docs/05-business-rules/Pipeline.md`
- **Contexto:** Funil de vendas, estágios, transições
- **Playbook:** Consultar `AI_ROUTER.md` → Pipeline
- **Prompt Recomendado:** "Configurar pipeline de vendas. Consultar docs/01-backend/Pipeline.md, docs/01-backend/Stages.md e docs/05-business-rules/Pipeline.md"
- **Dependências:** Leads, Kanban, Events
- **Tecnologias:** State machine, drag-and-drop

### Kanban
- **Docs Oficiais:** `docs/01-backend/Kanban.md`, `docs/02-frontend/Kanban.md`
- **Contexto:** Quadro visual, drag-and-drop
- **Playbook:** Consultar `AI_ROUTER.md` → Kanban
- **Prompt Recomendado:** "Implementar quadro kanban. Consultar docs/01-backend/Kanban.md e docs/02-frontend/Kanban.md"
- **Dependências:** Pipeline, Frontend
- **Tecnologias:** DnD, WebSocket

### Conversations
- **Docs Oficiais:** `docs/01-backend/Conversations.md`
- **Contexto:** Comunicação, histórico, timeline
- **Playbook:** Consultar `AI_ROUTER.md` → Conversations
- **Prompt Recomendado:** "Implementar conversas. Consultar docs/01-backend/Conversations.md"
- **Dependências:** Contacts, Messages
- **Tecnologias:** WebSocket, timeline

### Chat
- **Docs Oficiais:** `docs/01-backend/Chat.md`, `docs/02-frontend/Chat.md`
- **Contexto:** Chat em tempo real, interface
- **Playbook:** Consultar `AI_ROUTER.md` → Chat
- **Prompt Recomendado:** "Implementar chat em tempo real. Consultar docs/01-backend/Chat.md e docs/02-frontend/Chat.md"
- **Dependências:** Conversations, Messages, WebSocket
- **Tecnologias:** WebSocket, React

### Messages
- **Docs Oficiais:** `docs/01-backend/Messages.md`
- **Contexto:** Mensageria, tipos de mensagem
- **Playbook:** Consultar `AI_ROUTER.md` → Messages
- **Prompt Recomendado:** "Implementar mensagens. Consultar docs/01-backend/Messages.md"
- **Dependências:** Conversations
- **Tecnologias:** CRUD, WebSocket

### Templates
- **Docs Oficiais:** `docs/01-backend/Templates.md`
- **Contexto:** Modelos reutilizáveis
- **Playbook:** Consultar `AI_ROUTER.md` → Templates
- **Prompt Recomendado:** "Criar templates. Consultar docs/01-backend/Templates.md"
- **Dependências:** Campaigns
- **Tecnologias:** CRUD, preview

### Campaigns
- **Docs Oficiais:** `docs/01-backend/Campaigns.md`, `docs/05-business-rules/Campaign.md`
- **Contexto:** Marketing, segmentação, envio
- **Playbook:** Consultar `AI_ROUTER.md` → Campaigns
- **Prompt Recomendado:** "Criar campanha de marketing. Consultar docs/01-backend/Campaigns.md e docs/05-business-rules/Campaign.md"
- **Dependencies:** Templates, Contacts, Scheduler
- **Tecnologias:** Batch, fila

### Automations
- **Docs Oficiais:** `docs/01-backend/Automations.md`, `docs/05-business-rules/Automation.md`
- **Contexto:** Workflows, triggers, ações
- **Playbook:** Consultar `AI_ROUTER.md` → Automations
- **Prompt Recomendado:** "Criar automação. Consultar docs/01-backend/Automations.md e docs/05-business-rules/Automation.md"
- **Dependências:** Events, Scheduler
- **Tecnologias:** State machine, fila

### Webhooks
- **Docs Oficiais:** `docs/01-backend/Webhooks.md`
- **Contexto:** Integração externa, payloads
- **Playbook:** Consultar `AI_ROUTER.md` → Webhooks
- **Prompt Recomendado:** "Configurar webhook. Consultar docs/01-backend/Webhooks.md"
- **Dependências:** Events
- **Tecnologias:** HTTP, retry

### Notifications
- **Docs Oficiais:** `docs/01-backend/Notifications.md`, `docs/02-frontend/Notifications.md`
- **Contexto:** Push, in-app, preferências
- **Playbook:** Consultar `AI_ROUTER.md` → Notifications
- **Prompt Recomendado:** "Implementar notificações. Consultar docs/01-backend/Notifications.md e docs/02-frontend/Notifications.md"
- **Dependências:** Users, Events
- **Tecnologias:** WebSocket, push

### Dashboard
- **Docs Oficiais:** `docs/01-backend/Dashboard.md`, `docs/02-frontend/Dashboard.md`
- **Contexto:** Métricas, visualização
- **Playbook:** Consultar `AI_ROUTER.md` → Dashboard
- **Prompt Recomendado:** "Criar dashboard. Consultar docs/01-backend/Dashboard.md e docs/02-frontend/Dashboard.md"
- **Dependências:** Reports, Cache
- **Tecnologias:** Charts, cache

### Reports
- **Docs Oficiais:** `docs/01-backend/Reports.md`, `docs/02-frontend/Reports.md`, `docs/05-business-rules/Reports.md`
- **Contexto:** Analytics, métricas, visualização
- **Playbook:** Consultar `AI_ROUTER.md` → Reports
- **Prompt Recomendado:** "Gerar relatório. Consultar docs/01-backend/Reports.md, docs/02-frontend/Reports.md e docs/05-business-rules/Reports.md"
- **Dependências:** Dashboard, Cache
- **Tecnologias:** Charts, export

### AI
- **Docs Oficiais:** `docs/01-backend/AI.md`, `docs/05-business-rules/AI.md`, `docs/04-integrations/OpenAI.md`
- **Contexto:** Inteligência artificial, integração OpenAI
- **Playbook:** Consultar `AI_ROUTER.md` → AI
- **Prompt Recomendado:** "Integrar IA. Consultar docs/01-backend/AI.md, docs/05-business-rules/AI.md e docs/04-integrations/OpenAI.md"
- **Dependências:** OpenAI integration
- **Tecnologias:** OpenAI, embeddings

### Events
- **Docs Oficiais:** `docs/01-backend/Events.md`
- **Contexto:** Event bus, publicação, subscrição
- **Playbook:** Consultar `AI_ROUTER.md` → Events
- **Prompt Recomendado:** "Implementar eventos. Consultar docs/01-backend/Events.md"
- **Dependências:** Nenhum módulo específico
- **Tecnologias:** Pub/sub, queue

### Audit
- **Docs Oficiais:** `docs/01-backend/Audit.md`
- **Contexto:** Rastreabilidade, logs de auditoria
- **Playbook:** Consultar `AI_ROUTER.md` → Audit
- **Prompt Recomendado:** "Implementar auditoria. Consultar docs/01-backend/Audit.md"
- **Dependências:** Database
- **Tecnologias:** Logs, soft delete

### Cache
- **Docs Oficiais:** `docs/01-backend/Cache.md`
- **Contexto:** Redis, invalidação
- **Playbook:** Consultar `AI_ROUTER.md` → Cache
- **Prompt Recomendado:** "Configurar cache. Consultar docs/01-backend/Cache.md"
- **Dependências:** Redis
- **Tecnologias:** Redis, TTL

### File Storage
- **Docs Oficiais:** `docs/01-backend/FileStorage.md`
- **Contexto:** Upload, download, lifecycle
- **Playbook:** Consultar `AI_ROUTER.md` → File Storage
- **Prompt Recomendado:** "Implementar armazenamento. Consultar docs/01-backend/FileStorage.md"
- **Dependências:** S3/minio
- **Tecnologias:** S3, multipart

### Scheduler
- **Docs Oficiais:** `docs/01-backend/Scheduler.md`
- **Contexto:** Agendamento, jobs, filas
- **Playbook:** Consultar `AI_ROUTER.md` → Scheduler
- **Prompt Recomendado:** "Configurar agendador. Consultar docs/01-backend/Scheduler.md"
- **Dependências:** Redis, Queue
- **Tecnologias:** Bull, cron

### Permissions
- **Docs Oficiais:** `docs/01-backend/Permissions.md`, `docs/02-frontend/Permissions.md`, `docs/05-business-rules/Permissions.md`
- **Contexto:** RBAC, controle de acesso
- **Playbook:** Consultar `AI_ROUTER.md` → Permissions
- **Prompt Recomendado:** "Gerenciar permissões. Consultar docs/01-backend/Permissions.md, docs/02-frontend/Permissions.md e docs/05-business-rules/Permissions.md"
- **Dependências:** Auth, Users
- **Tecnologias:** RBAC, middleware

### Database
- **Docs Oficiais:** `docs/03-database/Overview.md`, `docs/03-database/ERD.md`, `docs/03-database/Entities.md`, `docs/03-database/Migrations.md`, `docs/03-database/Indexes.md`, `docs/03-database/Performance.md`, `docs/03-database/Relationships.md`, `docs/03-database/SoftDelete.md`, `docs/03-database/UUID.md`, `docs/03-database/Backup.md`, `docs/03-database/Audit.md`
- **Contexto:** Schema, migrations, performance
- **Playbook:** Consultar `AI_ROUTER.md` → Database
- **Prompt Recomendado:** "Modificar banco de dados. Consultar docs/03-database/*"
- **Dependências:** Nenhum
- **Tecnologias:** PostgreSQL, Prisma, migrations

### Backend Architecture
- **Docs Oficiais:** `docs/00-core/Architecture.md`, `docs/01-backend/Overview.md`, `docs/01-backend/Modules.md`
- **Contexto:** Arquitetura, estrutura, módulos
- **Playbook:** Consultar `AI_ROUTER.md` → Backend Architecture
- **Prompt Recomendado:** "Consultar arquitetura backend. Consultar docs/00-core/Architecture.md, docs/01-backend/Overview.md e docs/01-backend/Modules.md"
- **Dependências:** Nenhum
- **Tecnologias:** Node.js, Express, NestJS

### Frontend Architecture
- **Docs Oficiais:** `docs/02-frontend/Overview.md`, `docs/02-frontend/Routing.md`, `docs/02-frontend/Layout.md`
- **Contexto:** Arquitetura, rotas, layouts
- **Playbook:** Consultar `AI_ROUTER.md` → Frontend Architecture
- **Prompt Recomendado:** "Consultar arquitetura frontend. Consultar docs/02-frontend/Overview.md, docs/02-frontend/Routing.md e docs/02-frontend/Layout.md"
- **Dependências:** Nenhum
- **Tecnologias:** React, Next.js, TypeScript

### Integrations
- **Docs Oficiais:** `docs/04-integrations/WhatsApp.md`, `docs/04-integrations/OpenAI.md`, `docs/04-integrations/Email.md`, `docs/04-integrations/SMS.md`, `docs/04-integrations/Google.md`, `docs/04-integrations/Payment.md`, `docs/04-integrations/Storage.md`
- **Contexto:** Integrações externas
- **Playbook:** Consultar `AI_ROUTER.md` → Integrations
- **Prompt Recomendado:** "Integrar serviço externo. Consultar docs/04-integrations/[serviço].md"
- **Dependências:** Varies
- **Tecnologias:** HTTP, OAuth, webhooks

### DevOps
- **Docs Oficiais:** `docs/06-devops/Docker.md`, `docs/06-devops/CI.md`, `docs/06-devops/CD.md`, `docs/06-devops/Kubernetes.md`, `docs/06-devops/Monitoring.md`
- **Contexto:** Containerização, CI/CD, monitoramento
- **Playbook:** Consultar `AI_ROUTER.md` → DevOps
- **Prompt Recomendado:** "Configurar infraestrutura. Consultar docs/06-devops/[ferramenta].md"
- **Dependências:** Nenhum
- **Tecnologias:** Docker, K8s, GitHub Actions

## Referências

- Roteador: [AI_ROUTER.md](AI_ROUTER.md)
- Índice de módulos: [MODULE_INDEX.md](MODULE_INDEX.md)

## Histórico de Revisão

| Versão | Data | Autor | Alteração |
|--------|------|-------|-----------|
| 1.0 | 2026-07-15 | AI Agent | Criação inicial |
