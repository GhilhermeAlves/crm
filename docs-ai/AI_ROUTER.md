# AI Router — O Cérebro

## Objetivo

Mapear cada solicitação ao conjunto mínimo de documentos oficiais necessários para implementá-la.

## Escopo

Todos os módulos do sistema CRM. Cada entrada segue o formato:
**Solicitação → Contexto → Documentos Oficiais → Implementar**

## Como utilizar

1. Identifique o módulo relacionado à solicitação
2. Consulte a tabela abaixo
3. Leia apenas os documentos listados na coluna "Documentos Oficiais"
4. Siga o fluxo em `IMPLEMENTATION_GUIDE.md`

---

## Módulos e Roteamento

### Autenticação (Auth)
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| Login/logout | Fluxo de autenticação | `docs/01-backend/Auth.md` |
| Registro | Criação de conta | `docs/01-backend/Auth.md` |
| Refresh token | Sessão | `docs/01-backend/Auth.md` |
| Recuperação de senha | Fluxo de reset | `docs/01-backend/Auth.md` |
| Controle de acesso | Permissões | `docs/05-business-rules/Permissions.md` |

### Usuários (Users)
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| CRUD de usuários | Gerenciamento | `docs/01-backend/Users.md` |
| Profile | Perfil do usuário | `docs/01-backend/Users.md` |
| Roles | Papéis | `docs/01-backend/Users.md`, `docs/05-business-rules/Permissions.md` |

### Empresa (Company)
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| CRUD de empresas | Multi-tenancy | `docs/01-backend/Companies.md` |
| Configuração da empresa | Settings | `docs/01-backend/Companies.md` |
| Estrutura de dados | Schema | `docs/03-database/Overview.md` |

### Contatos (Contacts)
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| CRUD de contatos | Gestão de contatos | `docs/01-backend/Contacts.md` |
| Relacionamentos | Entidades | `docs/03-database/Entities.md` |

### Leads
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| CRUD de leads | Captura de leads | `docs/01-backend/Leads.md` |
| Qualificação | Regras de negócio | `docs/05-business-rules/Lead.md` |
| scoring | Pontuação | `docs/05-business-rules/Lead.md` |
| Conversão de lead | Pipeline | `docs/01-backend/Leads.md`, `docs/05-business-rules/Lead.md` |

### Clientes (Customers)
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| CRUD de clientes | Gestão de clientes | `docs/01-backend/Customers.md` |
| Regras de negócio | Validações | `docs/05-business-rules/Customer.md` |

### Pipeline
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| Configurar pipeline | Estágios | `docs/01-backend/Pipeline.md`, `docs/01-backend/Stages.md` |
| Mover negócios | Fluxo | `docs/01-backend/Pipeline.md` |
| Regras de transição | Validações | `docs/05-business-rules/Pipeline.md` |

### Kanban
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| Quadro kanban | Visualização | `docs/01-backend/Kanban.md`, `docs/02-frontend/Kanban.md` |
| Drag and drop | Interatividade | `docs/02-frontend/Kanban.md` |

### Conversas (Conversations)
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| Criar/conversar | Comunicação | `docs/01-backend/Conversations.md` |
| Histórico | Timeline | `docs/01-backend/Conversations.md` |

### Chat
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| Chat em tempo real | WebSocket | `docs/01-backend/Chat.md`, `docs/02-frontend/Chat.md` |
| Interface de chat | UI | `docs/02-frontend/Chat.md` |

### Mensagens (Messages)
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| Enviar/receber | Mensageria | `docs/01-backend/Messages.md` |
| Tipos de mensagem | Formatos | `docs/01-backend/Messages.md` |

### Templates
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| Criar/editar templates | Modelos | `docs/01-backend/Templates.md` |
| Usar em campanhas | Integração | `docs/01-backend/Templates.md` |

### Campanhas (Campaigns)
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| Criar campanha | Marketing | `docs/01-backend/Campaigns.md` |
| Regras de envio | Validações | `docs/05-business-rules/Campaign.md` |
| Segmentação | Filtros | `docs/05-business-rules/Campaign.md` |

### Automações (Automations)
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| Criar automação | Workflows | `docs/01-backend/Automations.md` |
| Regras de trigger | Eventos | `docs/05-business-rules/Automation.md` |
| Ações | Execução | `docs/01-backend/Automations.md` |

### Webhooks
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| Configurar webhook | Integração externa | `docs/01-backend/Webhooks.md` |
| Payload | Formato | `docs/01-backend/Webhooks.md` |

### Notificações (Notifications)
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| Criar notificação | Push/in-app | `docs/01-backend/Notifications.md` |
| Preferências | Configuração | `docs/02-frontend/Notifications.md` |
| Regras | Validações | `docs/05-business-rules/Notification.md` |

### Dashboard
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| Criar dashboard | Métricas | `docs/01-backend/Dashboard.md` |
| Visualização | UI | `docs/02-frontend/Dashboard.md` |

### Relatórios (Reports)
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| Gerar relatório | Analytics | `docs/01-backend/Reports.md` |
| Visualização | UI | `docs/02-frontend/Reports.md` |
| Regras de negócio | Métricas | `docs/05-business-rules/Reports.md` |

### IA (AI)
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| Integrar IA | OpenAI/similar | `docs/01-backend/AI.md` |
| Regras de uso | Limites | `docs/05-business-rules/AI.md` |
| Integração externa | API | `docs/04-integrations/OpenAI.md` |

### Eventos (Events)
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| Disparar evento | Event bus | `docs/01-backend/Events.md` |
| Assinar evento | Subscrição | `docs/01-backend/Events.md` |

### Auditoria (Audit)
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| Log de auditoria | Rastreabilidade | `docs/01-backend/Audit.md` |
| Consulta | Histórico | `docs/01-backend/Audit.md` |

### Cache
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| Configurar cache | Redis | `docs/01-backend/Cache.md` |
| Estratégia | Invalidação | `docs/01-backend/Cache.md`, `docs/CACHE_STRATEGY.md` |

### Armazenamento de Arquivos (File Storage)
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| Upload/download | S3/minio | `docs/01-backend/FileStorage.md` |
| Lifecycle | Gerenciamento | `docs/FILE_LIFECYCLE.md` |

### Agendador (Scheduler)
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| Agendar tarefas | Cron/bull | `docs/01-backend/Scheduler.md` |
| Jobs | Filas | `docs/01-backend/Scheduler.md` |

### Permissões (Permissions)
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| CRUD permissões | RBAC | `docs/01-backend/Permissions.md` |
| Interface permissões | UI | `docs/02-frontend/Permissions.md` |
| Regras | Validações | `docs/05-business-rules/Permissions.md` |

### Banco de Dados (Database)
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| Schema/modelo | ERD | `docs/03-database/ERD.md`, `docs/03-database/Entities.md` |
| Migration | Alterações | `docs/03-database/Migrations.md` |
| Performance | Índices | `docs/03-database/Indexes.md`, `docs/03-database/Performance.md` |
| Soft delete | Exclusão lógica | `docs/03-database/SoftDelete.md` |
| UUID | Identificadores | `docs/03-database/UUID.md` |
| Relationships | Relacionamentos | `docs/03-database/Relationships.md` |
| Backup | Respaldo | `docs/03-database/Backup.md` |
| Auditoria DB | Audit logs | `docs/03-database/Audit.md` |
| Visão geral | Configuração | `docs/03-database/Overview.md` |

### Arquitetura Backend
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| Estrutura geral | Arquitetura | `docs/00-core/Architecture.md` |
| Módulos | Organização | `docs/01-backend/Modules.md` |
| Visão geral backend | Setup | `docs/01-backend/Overview.md` |
| Padrões | Design patterns | `docs/00-core/DesignPatterns.md` |
| Convenções | Código | `docs/00-core/CodingStandards.md`, `docs/00-core/NamingConvention.md` |

### Arquitetura Frontend
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| Estrutura geral | Arquitetura | `docs/02-frontend/Overview.md` |
| Rotas | Navegação | `docs/02-frontend/Routing.md` |
| Layout | Layouts | `docs/02-frontend/Layout.md` |
| Componentes | UI | `docs/02-frontend/Components.md` |
| Hooks | Custom hooks | `docs/02-frontend/Hooks.md` |
| Forms | Formulários | `docs/02-frontend/Forms.md` |
| Tema | Design system | `docs/02-frontend/Theme.md` |
| Validação | Validações | `docs/02-frontend/Validation.md` |

### Integrações
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| WhatsApp | Evolution API | `docs/04-integrations/WhatsApp.md`, `docs/04-integrations/EvolutionAPI.md` |
| OpenAI | IA | `docs/04-integrations/OpenAI.md` |
| Email | SMTP/Email | `docs/04-integrations/Email.md` |
| SMS | SMS gateway | `docs/04-integrations/SMS.md` |
| Google | OAuth/Calendar | `docs/04-integrations/Google.md` |
| Pagamento | Billing | `docs/04-integrations/Payment.md` |
| Storage | S3/minio | `docs/04-integrations/Storage.md` |
| Webhooks ext | Webhooks | `docs/04-integrations/Webhooks.md` |

### DevOps
| Solicitação | Contexto | Documentos Oficiais |
|-------------|----------|---------------------|
| Container | Docker | `docs/06-devops/Docker.md` |
| CI | Pipeline CI | `docs/06-devops/CI.md` |
| CD | Deploy | `docs/06-devops/CD.md` |
| Kubernetes | Orquestração | `docs/06-devops/Kubernetes.md` |
| Monitoramento | Observabilidade | `docs/06-devops/Monitoring.md`, `docs/06-devops/Logs.md` |
| Backup | Respaldo | `docs/06-devops/Backup.md` |
| Métricas | Metrics | `docs/06-devops/Metrics.md` |

## Referências

- Índice completo: [MODULE_INDEX.md](MODULE_INDEX.md)
- Árvore de decisão: [DECISION_TREE.md](DECISION_TREE.md)
- Fluxo de implementação: [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md)
- Regras: [AI_RULES.md](AI_RULES.md)

## Histórico de Revisão

| Versão | Data | Autor | Alteração |
|--------|------|-------|-----------|
| 1.0 | 2026-07-15 | AI Agent | Criação inicial com todos os módulos |
