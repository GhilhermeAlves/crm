# 01-Backend — Documentação do Backend

## Índice

| Documento | Descrição |
|---|---|
| [Overview.md](./Overview.md) | Visão geral do backend |
| [Modules.md](./Modules.md) | Módulos e bounded contexts |
| [Auth.md](./Auth.md) | Autenticação e autorização |
| [Users.md](./Users.md) | Gestão de usuários |
| [Companies.md](./Companies.md) | Gestão de empresas (multi-tenancy) |
| [Contacts.md](./Contacts.md) | Gestão de contatos |
| [Leads.md](./Leads.md) | Gestão de leads |
| [Customers.md](./Customers.md) | Gestão de clientes |
| [Pipeline.md](./Pipeline.md) | Pipeline de vendas |
| [Stages.md](./Stages.md) | Estágios do pipeline |
| [Kanban.md](./Kanban.md) | Quadro kanban |
| [Chat.md](./Chat.md) | Sistema de chat |
| [Conversations.md](./Conversations.md) | Gestão de conversas |
| [Messages.md](./Messages.md) | Gestão de mensagens |
| [Templates.md](./Templates.md) | Templates de mensagens |
| [Campaigns.md](./Campaigns.md) | Campanhas de marketing |
| [Automations.md](./Automations.md) | Automações |
| [Webhooks.md](./Webhooks.md) | Webhooks |
| [Notifications.md](./Notifications.md) | Sistema de notificações |
| [Dashboard.md](./Dashboard.md) | Dados do dashboard |
| [Reports.md](./Reports.md) | Relatórios |
| [Audit.md](./Audit.md) | Auditoria |
| [Logs.md](./Logs.md) | Logs |
| [Events.md](./Events.md) | Eventos de domínio |
| [Scheduler.md](./Scheduler.md) | Agendamento de tarefas |
| [Cache.md](./Cache.md) | Cache (Redis) |
| [FileStorage.md](./FileStorage.md) | Armazenamento de arquivos |
| [AI.md](./AI.md) | Inteligência Artificial |
| [Permissions.md](./Permissions.md) | RBAC e permissões |

---

## Objetivo

Documentar todos os módulos, funcionalidades e fluxos do backend do CRM SaaS Omnichannel.

## Descrição

O backend é construído com Java 25, Spring Boot 3, seguindo Clean Architecture e DDD. Cada documento nesta pasta representa um módulo ou funcionalidade do sistema.

## Regras

- Todo documento deve seguir o template padrão com: título, objetivo, descrição, responsabilidades, fluxo, dependências, regras, futuras melhorias
- Mudanças em módulos devem ser documentadas antes da implementação
- Referências cruzadas entre documentos são obrigatórias

## Dependências

- [00-core/Architecture.md](../00-core/Architecture.md) — Arquitetura base
- [00-core/TechStack.md](../00-core/TechStack.md) — Stack tecnológico
- [00-core/NamingConvention.md](../00-core/NamingConvention.md) — Convenções de nomenclatura

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial da documentação backend |
