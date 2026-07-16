# Task Index

## Objetivo

Mapear tarefas comuns de desenvolvimento aos módulos e documentos oficiais correspondentes.

## Escopo

Tarefas típicas que um desenvolvedor ou agente de IA pode precisar executar.

## Como utilizar

1. Localize a tarefa desejada
2. Identifique o módulo associado
3. Acesse os documentos oficiais listados

## Índice de Tarefas

### CRUD Básico
| Tarefa | Módulo | Docs Oficiais |
|--------|--------|---------------|
| Criar entidade | Contacts, Leads, Customers | `docs/01-backend/Contacts.md`, `docs/01-backend/Leads.md`, `docs/01-backend/Customers.md` |
| Listar entidades | Qualquer CRUD | `docs/01-backend/[Modulo].md` |
| Atualizar entidade | Qualquer CRUD | `docs/01-backend/[Modulo].md` |
| Deletar entidade (soft delete) | Qualquer | `docs/03-database/SoftDelete.md` |

### Autenticação e Autorização
| Tarefa | Módulo | Docs Oficiais |
|--------|--------|---------------|
| Implementar login | Auth | `docs/01-backend/Auth.md` |
| Adicionar rota protegida | Auth, Permissions | `docs/01-backend/Auth.md`, `docs/05-business-rules/Permissions.md` |
| Criar permissão | Permissions | `docs/01-backend/Permissions.md`, `docs/05-business-rules/Permissions.md` |
| Gerenciar roles | Users, Permissions | `docs/01-backend/Users.md`, `docs/01-backend/Permissions.md` |

### Pipeline e Vendas
| Tarefa | Módulo | Docs Oficiais |
|--------|--------|---------------|
| Criar estágio | Pipeline | `docs/01-backend/Pipeline.md`, `docs/01-backend/Stages.md` |
| Mover negócio | Pipeline, Kanban | `docs/01-backend/Pipeline.md`, `docs/01-backend/Kanban.md` |
| Configurar kanban | Kanban | `docs/01-backend/Kanban.md`, `docs/02-frontend/Kanban.md` |
| Qualificar lead | Leads | `docs/01-backend/Leads.md`, `docs/05-business-rules/Lead.md` |
| Converter lead | Leads, Pipeline | `docs/01-backend/Leads.md`, `docs/05-business-rules/Lead.md` |

### Comunicação
| Tarefa | Módulo | Docs Oficiais |
|--------|--------|---------------|
| Criar conversa | Conversations | `docs/01-backend/Conversations.md` |
| Enviar mensagem | Messages | `docs/01-backend/Messages.md` |
| Implementar chat | Chat | `docs/01-backend/Chat.md`, `docs/02-frontend/Chat.md` |
| Configurar WhatsApp | Integrations | `docs/04-integrations/WhatsApp.md`, `docs/04-integrations/EvolutionAPI.md` |

### Marketing
| Tarefa | Módulo | Docs Oficiais |
|--------|--------|---------------|
| Criar template | Templates | `docs/01-backend/Templates.md` |
| Criar campanha | Campaigns | `docs/01-backend/Campaigns.md`, `docs/05-business-rules/Campaign.md` |
| Agendar envio | Scheduler, Campaigns | `docs/01-backend/Scheduler.md`, `docs/01-backend/Campaigns.md` |
| Configurar automação | Automations | `docs/01-backend/Automations.md`, `docs/05-business-rules/Automation.md` |

### Infraestrutura
| Tarefa | Módulo | Docs Oficiais |
|--------|--------|---------------|
| Criar migration | Database | `docs/03-database/Migrations.md` |
| Adicionar índice | Database | `docs/03-database/Indexes.md` |
| Configurar cache | Cache | `docs/01-backend/Cache.md` |
| Upload de arquivo | File Storage | `docs/01-backend/FileStorage.md` |
| Agendar job | Scheduler | `docs/01-backend/Scheduler.md` |
| Configurar webhook | Webhooks | `docs/01-backend/Webhooks.md` |

### Frontend
| Tarefa | Módulo | Docs Oficiais |
|--------|--------|---------------|
| Criar rota | Frontend | `docs/02-frontend/Routing.md` |
| Criar componente | Frontend | `docs/02-frontend/Components.md` |
| Criar formulário | Frontend | `docs/02-frontend/Forms.md`, `docs/02-frontend/Validation.md` |
| Criar layout | Frontend | `docs/02-frontend/Layout.md` |
| Criar dashboard | Dashboard | `docs/01-backend/Dashboard.md`, `docs/02-frontend/Dashboard.md` |
| Criar relatório | Reports | `docs/01-backend/Reports.md`, `docs/02-frontend/Reports.md` |

### Observabilidade
| Tarefa | Módulo | Docs Oficiais |
|--------|--------|---------------|
| Adicionar log de auditoria | Audit | `docs/01-backend/Audit.md` |
| Criar evento | Events | `docs/01-backend/Events.md` |
| Configurar notificação | Notifications | `docs/01-backend/Notifications.md`, `docs/02-frontend/Notifications.md` |

### DevOps
| Tarefa | Módulo | Docs Oficiais |
|--------|--------|---------------|
| Configurar Docker | DevOps | `docs/06-devops/Docker.md` |
| Criar pipeline CI | DevOps | `docs/06-devops/CI.md` |
| Configurar deploy | DevOps | `docs/06-devops/CD.md` |

## Referências

- Roteador: [AI_ROUTER.md](AI_ROUTER.md)
- Índice de módulos: [MODULE_INDEX.md](MODULE_INDEX.md)

## Histórico de Revisão

| Versão | Data | Autor | Alteração |
|--------|------|-------|-----------|
| 1.0 | 2026-07-15 | AI Agent | Criação inicial |
