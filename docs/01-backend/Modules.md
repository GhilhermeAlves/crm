# Modules — Módulos e Bounded Contexts

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Mapa de Módulos](#mapa-de-módulos)
- [Identity](#identity)
- [Company](#company)
- [Contact](#contact)
- [Pipeline](#pipeline)
- [Communication](#communication)
- [Campaign](#campaign)
- [Analytics](#analytics)
- [Integration](#integration)
- [Context Map](#context-map)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar todos os módulos (bounded contexts) do backend, incluindo suas responsabilidades, limites e relacionamentos.

## Descrição

O sistema é dividido em bounded contexts segundo DDD. Cada bounded context encapsula um domínio da empresa com seu próprio model, repository interface, events e exceptions.

## Mapa de Módulos

```
                    ┌─────────────┐
                    │  Identity    │
                    └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
       ┌──────▼──────┐ ┌──▼───────┐ ┌──▼──────────┐
       │   Company    │ │  Contact │ │  Pipeline   │
       └──────┬──────┘ └──┬───────┘ └──┬──────────┘
              │            │            │
              └────────────┼────────────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
       ┌──────▼──────────┐│ ┌──────────▼────┐
       │  Communication  ││ │   Campaign    │
       └─────────────────┘│ └───────────────┘
                          │
                   ┌──────▼──────┐
                   │  Analytics  │
                   └─────────────┘
```

## Identity

**Responsabilidade**: Autenticação, autorização, gestão de usuários e permissões.

| Entidade | Descrição |
|---|---|
| User | Usuário do sistema |
| Role | Papel/função (Admin, Manager, Agent) |
| Permission | Permissão específica |
| Token | JWT Access e Refresh Token |

**Events**: `UserCreated`, `UserUpdated`, `RoleChanged`, `PasswordReset`

**Dependências**: Nenhuma (contexto raiz)

## Company

**Responsabilidade**: Gestão de empresas (tenants), configurações e billing.

| Entidade | Descrição |
|---|---|
| Company | Empresa/tenant |
| Subscription | Plano de assinatura |
| Setting | Configurações da empresa |
| InvitedUser | Convite pendente |

**Events**: `CompanyCreated`, `CompanyUpdated`, `PlanChanged`, `UserInvited`

**Dependências**: Identity (usuários da empresa)

## Contact

**Responsabilidade**: Gestão de contatos, segmentação e campos customizados.

| Entidade | Descrição |
|---|---|
| Contact | Contato principal |
| ContactAddress | Endereço do contato |
| ContactCustomField | Campo customizado |
| Segment | Segmento/filtro |
| Tag | Etiqueta |

**Events**: `ContactCreated`, `ContactUpdated`, `ContactTagged`, `ContactSegmented`

**Dependências**: Company, Identity

## Pipeline

**Responsabilidade**: Gestão de oportunidades de vendas, pipeline, estágios e kanban.

| Entidade | Descrição |
|---|---|
| Pipeline | Pipeline de vendas |
| Stage | Estágio do pipeline |
| Opportunity | Oportunidade/Oportunidade |
| OpportunityHistory | Histórico de mudanças |

**Events**: `OpportunityCreated`, `OpportunityMoved`, `OpportunityWon`, `OpportunityLost`

**Dependências**: Company, Contact

## Communication

**Responsabilidade**: Gestão de conversas, mensagens e templates de comunicação.

| Entidade | Descrição |
|---|---|
| Conversation | Conversa |
| Message | Mensagem |
| MessageTemplate | Template de mensagem |
| MessageAttachment | Anexo |

**Events**: `MessageSent`, `MessageReceived`, `ConversationCreated`, `ConversationClosed`

**Dependências**: Company, Contact, Integration (WhatsApp)

## Campaign

**Responsabilidade**: Campanhas de marketing, automações e workflows.

| Entidade | Descrição |
|---|---|
| Campaign | Campanha |
| CampaignStep | Passo da campanha |
| Automation | Automação |
| AutomationTrigger | Gatilho da automação |
| AutomationAction | Ação da automação |

**Events**: `CampaignCreated`, `CampaignSent`, `AutomationTriggered`, `AutomationCompleted`

**Dependências**: Company, Contact, Communication, Integration

## Analytics

**Responsabilidade**: Relatórios, dashboards e métricas de negócio.

| Entidade | Descrição |
|---|---|
| Report | Relatório |
| Dashboard | Dashboard |
| Metric | Métrica calculada |

**Events**: `ReportGenerated`, `MetricCalculated`

**Dependências**: Company, Contact, Pipeline, Communication, Campaign

## Integration

**Responsabilidade**: Integrações externas (WhatsApp, Email, SMS, APIs).

| Entidade | Descrição |
|---|---|
| IntegrationConfig | Configuração de integração |
| ExternalMessage | Mensagem externa |
| Webhook | Webhook configurado |

**Events**: `IntegrationConnected`, `MessageDelivered`, `WebhookTriggered`

**Dependências**: Company, Communication

## Context Map

| Relacionamento | Tipo | Descrição |
|---|---|---|
| Identity → Company | Customer-Supplier | Identity fornece autenticação para Company |
| Company → Contact | Customer-Supplier | Company define contexto para Contacts |
| Contact → Pipeline | Customer-Supplier | Contacts alimentam Pipeline |
| Communication → Integration | Conformist | Communication depende de Integration |
| Campaign → Communication | Customer-Supplier | Campaign usa Communication para enviar |
| Analytics → Todos | Free Rider | Analytics consome dados de todos os contextos |

## Responsabilidades

- Documentar limites de cada bounded context
- Manter o context map atualizado
- Definir interfaces entre contextos
- Identificar dependências e acoplamentos

## Dependências

- [00-core/Architecture.md](../00-core/Architecture.md) — Arquitetura base
- [00-core/Decisions.md](../00-core/Decisions.md) — Decisões de bounded contexts

## Regras

- Cada bounded context deve ter seu próprio model, events e repository interfaces
- Nunca acessar database de outro contexto diretamente
- Comunicação entre contextos via eventos ou interfaces
- Um contexto não pode expor entidades internas para outro

## Futuras Melhorias

- Implementar Context Map visual com ferramenta dedicada
- Adicionar métricas de acoplamento entre contextos
- Avaliar decomposição em microsserviços por contexto
- Adicionar Contratos de Serviço (SLA) entre contextos

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial dos módulos |
