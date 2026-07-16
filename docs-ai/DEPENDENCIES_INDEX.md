# Dependencies Index

## Objetivo

Mapear dependências entre módulos para avaliar impacto de mudanças.

## Escopo

Todos os módulos do sistema com suas dependências diretas e indiretas.

## Como utilizar

1. Localize o módulo que será modificado
2. Verifique quais módulos dependem dele (impactados)
3. Verifique de quais módulos ele depende (pré-requisitos)
4. Consulte `CHANGE_POLICY.md` antes de implementar

## Mapa de Dependências

### Auth
- **Depende de:** Users, Permissions
- **Impacta:** Todos os módulos (autenticação global)
- **Docs:** `docs/01-backend/Auth.md`, `docs/05-business-rules/Permissions.md`

### Users
- **Depende de:** Auth, Company
- **Impacta:** Permissions, Conversations, Notifications
- **Docs:** `docs/01-backend/Users.md`

### Company
- **Depende de:** Database
- **Impacta:** Users, Contacts, Leads, Customers (multi-tenancy)
- **Docs:** `docs/01-backend/Companies.md`

### Contacts
- **Depende de:** Company, Database
- **Impacta:** Leads, Customers, Conversations, Campaigns
- **Docs:** `docs/01-backend/Contacts.md`

### Leads
- **Depende de:** Contacts, Pipeline, Events
- **Impacta:** Customers (conversão), Pipeline
- **Docs:** `docs/01-backend/Leads.md`, `docs/05-business-rules/Lead.md`

### Customers
- **Depende de:** Contacts, Pipeline, Leads
- **Impacta:** Conversations, Campaigns
- **Docs:** `docs/01-backend/Customers.md`, `docs/05-business-rules/Customer.md`

### Pipeline
- **Depende de:** Database, Events
- **Impacta:** Leads, Kanban, Customers
- **Docs:** `docs/01-backend/Pipeline.md`, `docs/01-backend/Stages.md`

### Kanban
- **Depende de:** Pipeline, Frontend
- **Impacta:** Nenhum (visualização)
- **Docs:** `docs/01-backend/Kanban.md`, `docs/02-frontend/Kanban.md`

### Conversations
- **Depende de:** Contacts, Messages
- **Impacta:** Chat, Notifications
- **Docs:** `docs/01-backend/Conversations.md`

### Chat
- **Depende de:** Conversations, Messages, WebSocket
- **Impacta:** Notifications
- **Docs:** `docs/01-backend/Chat.md`, `docs/02-frontend/Chat.md`

### Messages
- **Depende de:** Conversations
- **Impacta:** Chat, Notifications
- **Docs:** `docs/01-backend/Messages.md`

### Templates
- **Depende de:** Nenhum
- **Impacta:** Campaigns
- **Docs:** `docs/01-backend/Templates.md`

### Campaigns
- **Depende de:** Templates, Contacts, Scheduler
- **Impacta:** Automations
- **Docs:** `docs/01-backend/Campaigns.md`

### Automations
- **Depende de:** Events, Scheduler
- **Impacta:** Campaigns, Notifications
- **Docs:** `docs/01-backend/Automations.md`

### Webhooks
- **Depende de:** Events
- **Impacta:** Integrações externas
- **Docs:** `docs/01-backend/Webhooks.md`

### Notifications
- **Depende de:** Users, Events
- **Impacta:** Frontend (push/in-app)
- **Docs:** `docs/01-backend/Notifications.md`

### Dashboard
- **Depende de:** Reports, Cache
- **Impacta:** Frontend
- **Docs:** `docs/01-backend/Dashboard.md`

### Reports
- **Depende de:** Cache, Database
- **Impacta:** Dashboard, Frontend
- **Docs:** `docs/01-backend/Reports.md`

### AI
- **Depende de:** OpenAI integration
- **Impacta:** Automations, Chat
- **Docs:** `docs/01-backend/AI.md`

### Events
- **Depende de:** Nenhum
- **Impacta:** Leads, Pipeline, Automations, Webhooks, Notifications
- **Docs:** `docs/01-backend/Events.md`

### Audit
- **Depende de:** Database
- **Impacta:** Nenhum (somente leitura)
- **Docs:** `docs/01-backend/Audit.md`

### Cache
- **Depende de:** Redis
- **Impacta:** Dashboard, Reports, Todos (performance)
- **Docs:** `docs/01-backend/Cache.md`

### File Storage
- **Depende de:** S3/minio
- **Impacta:** Messages (anexos), Contacts
- **Docs:** `docs/01-backend/FileStorage.md`

### Scheduler
- **Depende de:** Redis, Queue
- **Impacta:** Campaigns, Automations
- **Docs:** `docs/01-backend/Scheduler.md`

### Permissions
- **Depende de:** Auth, Users
- **Impacta:** Todos os módulos (controle de acesso)
- **Docs:** `docs/01-backend/Permissions.md`, `docs/05-business-rules/Permissions.md`

## Referências

- Arquitetura: `docs/00-core/Architecture.md`
- Roteador: [AI_ROUTER.md](AI_ROUTER.md)
- Política de mudança: [CHANGE_POLICY.md](CHANGE_POLICY.md)

## Histórico de Revisão

| Versão | Data | Autor | Alteração |
|--------|------|-------|-----------|
| 1.0 | 2026-07-15 | AI Agent | Criação inicial |
