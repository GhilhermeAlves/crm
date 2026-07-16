# API Index

## Objetivo

Índice de endpoints da API organizados por módulo.

## Escopo

Todos os endpoints REST do backend, agrupados por domínio.

## Como utilizar

Localize o módulo e identifique os endpoints na documentação oficial.

## Endpoints por Módulo

### Autenticação
| Endpoint | Docs |
|----------|------|
| POST /auth/login | `docs/01-backend/Auth.md` |
| POST /auth/register | `docs/01-backend/Auth.md` |
| POST /auth/refresh | `docs/01-backend/Auth.md` |
| POST /auth/forgot-password | `docs/01-backend/Auth.md` |
| POST /auth/reset-password | `docs/01-backend/Auth.md` |

### Usuários
| Endpoint | Docs |
|----------|------|
| GET /users | `docs/01-backend/Users.md` |
| GET /users/:id | `docs/01-backend/Users.md` |
| POST /users | `docs/01-backend/Users.md` |
| PUT /users/:id | `docs/01-backend/Users.md` |
| DELETE /users/:id | `docs/01-backend/Users.md` |

### Empresas
| Endpoint | Docs |
|----------|------|
| GET /companies | `docs/01-backend/Companies.md` |
| GET /companies/:id | `docs/01-backend/Companies.md` |
| POST /companies | `docs/01-backend/Companies.md` |
| PUT /companies/:id | `docs/01-backend/Companies.md` |

### Contatos
| Endpoint | Docs |
|----------|------|
| GET /contacts | `docs/01-backend/Contacts.md` |
| GET /contacts/:id | `docs/01-backend/Contacts.md` |
| POST /contacts | `docs/01-backend/Contacts.md` |
| PUT /contacts/:id | `docs/01-backend/Contacts.md` |
| DELETE /contacts/:id | `docs/01-backend/Contacts.md` |

### Leads
| Endpoint | Docs |
|----------|------|
| GET /leads | `docs/01-backend/Leads.md` |
| GET /leads/:id | `docs/01-backend/Leads.md` |
| POST /leads | `docs/01-backend/Leads.md` |
| PUT /leads/:id | `docs/01-backend/Leads.md` |
| POST /leads/:id/convert | `docs/01-backend/Leads.md` |

### Clientes
| Endpoint | Docs |
|----------|------|
| GET /customers | `docs/01-backend/Customers.md` |
| GET /customers/:id | `docs/01-backend/Customers.md` |
| POST /customers | `docs/01-backend/Customers.md` |
| PUT /customers/:id | `docs/01-backend/Customers.md` |

### Pipeline
| Endpoint | Docs |
|----------|------|
| GET /pipeline | `docs/01-backend/Pipeline.md` |
| GET /pipeline/stages | `docs/01-backend/Stages.md` |
| POST /pipeline/stages | `docs/01-backend/Stages.md` |
| PUT /pipeline/deals/:id/move | `docs/01-backend/Pipeline.md` |

### Kanban
| Endpoint | Docs |
|----------|------|
| GET /kanban/:boardId | `docs/01-backend/Kanban.md` |
| PUT /kanban/cards/:id | `docs/01-backend/Kanban.md` |

### Conversas
| Endpoint | Docs |
|----------|------|
| GET /conversations | `docs/01-backend/Conversations.md` |
| GET /conversations/:id | `docs/01-backend/Conversations.md` |
| POST /conversations | `docs/01-backend/Conversations.md` |

### Chat
| Endpoint | Docs |
|----------|------|
| WebSocket /chat | `docs/01-backend/Chat.md` |

### Mensagens
| Endpoint | Docs |
|----------|------|
| GET /conversations/:id/messages | `docs/01-backend/Messages.md` |
| POST /conversations/:id/messages | `docs/01-backend/Messages.md` |

### Templates
| Endpoint | Docs |
|----------|------|
| GET /templates | `docs/01-backend/Templates.md` |
| POST /templates | `docs/01-backend/Templates.md` |
| PUT /templates/:id | `docs/01-backend/Templates.md` |

### Campanhas
| Endpoint | Docs |
|----------|------|
| GET /campaigns | `docs/01-backend/Campaigns.md` |
| POST /campaigns | `docs/01-backend/Campaigns.md` |
| POST /campaigns/:id/send | `docs/01-backend/Campaigns.md` |

### Automações
| Endpoint | Docs |
|----------|------|
| GET /automations | `docs/01-backend/Automations.md` |
| POST /automations | `docs/01-backend/Automations.md` |
| PUT /automations/:id | `docs/01-backend/Automations.md` |

### Webhooks
| Endpoint | Docs |
|----------|------|
| GET /webhooks | `docs/01-backend/Webhooks.md` |
| POST /webhooks | `docs/01-backend/Webhooks.md` |

### Notificações
| Endpoint | Docs |
|----------|------|
| GET /notifications | `docs/01-backend/Notifications.md` |
| PUT /notifications/:id/read | `docs/01-backend/Notifications.md` |

### Dashboard
| Endpoint | Docs |
|----------|------|
| GET /dashboard | `docs/01-backend/Dashboard.md` |
| GET /dashboard/metrics | `docs/01-backend/Dashboard.md` |

### Relatórios
| Endpoint | Docs |
|----------|------|
| GET /reports | `docs/01-backend/Reports.md` |
| POST /reports/generate | `docs/01-backend/Reports.md` |

### IA
| Endpoint | Docs |
|----------|------|
| POST /ai/chat | `docs/01-backend/AI.md` |
| POST /ai/suggest | `docs/01-backend/AI.md` |

### Permissões
| Endpoint | Docs |
|----------|------|
| GET /permissions | `docs/01-backend/Permissions.md` |
| POST /permissions | `docs/01-backend/Permissions.md` |

### Auditoria
| Endpoint | Docs |
|----------|------|
| GET /audit | `docs/01-backend/Audit.md` |

### Arquivos
| Endpoint | Docs |
|----------|------|
| POST /files/upload | `docs/01-backend/FileStorage.md` |
| GET /files/:id | `docs/01-backend/FileStorage.md` |

## Referências

- Versãoamento: `docs/API_VERSIONING.md`
- Mapa de API: `docs/API_MAP.md`
- Roteador: [AI_ROUTER.md](AI_ROUTER.md)

## Histórico de Revisão

| Versão | Data | Autor | Alteração |
|--------|------|-------|-----------|
| 1.0 | 2026-07-15 | AI Agent | Criação inicial |
