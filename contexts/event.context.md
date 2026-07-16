# Event Context

## Resumo do Módulo
Domain events (LeadCreated, OpportunityMoved, etc.) e integration events (WhatsAppMessageDelivered, EmailBounced) via RabbitMQ. Idempotência garantida. Max 256KB payload. 90 dias retenção.

## Objetivo
Desacoplar módulos via event-driven architecture com garantia de idempotência.

## Responsabilidades
- Domain events doDDD
- Integration events entre contextos
- RabbitMQ como broker
- Idempotência em todos handlers
- Retenção de 90 dias

## Domain Events
| Evento | Contexto | Trigger |
|--------|----------|---------|
| `LeadCreated` | Lead | Novo lead capturado |
| `LeadScoreChanged` | Lead | Score recalculado |
| `ContactCreated` | Contact | Novo contato |
| `OpportunityCreated` | Pipeline | Oportunidade criada |
| `OpportunityMoved` | Pipeline | Estágio alterado |
| `OpportunityWon` | Pipeline | Marcada como ganha |
| `OpportunityLost` | Pipeline | Marcada como perdida |
| `ConversationCreated` | Conversation | Nova conversa |
| `MessageSent` | Message | Mensagem enviada |
| `CampaignStarted` | Campaign | Campanha iniciada |
| `AutomationTriggered` | Automation | Trigger disparado |

## Integration Events
| Evento | Fonte | Consumidor |
|--------|-------|-----------|
| `WhatsAppMessageDelivered` | WhatsApp | Messages |
| `WhatsAppMessageReceived` | WhatsApp | Conversations |
| `EmailBounced` | Email | Contacts |
| `PaymentProcessed` | Billing | Subscriptions |

## APIs Relacionadas
- `GET /events` - Listar eventos (admin/debug)
- `GET /events/:id` - Detalhes do evento
- `POST /events/replay` - Replay de evento (admin)

## Banco Relacionado
- `domain_events` - Events persistidos (90 dias retenção)
- `event_outbox` - Transactional outbox pattern
- `event_dead_letter` - Falhas de processamento

## Componentes Backend
- `event` module (Publisher, Consumer, Domain Event base)
- `rabbitmq` config (exchanges, queues, routing)
- `outbox` module (transactional outbox)
- `dead-letter` handler (DLQ processing)
- `idempotency` module (checagem de duplicatas)

## Garantias
- **Idempotência**: Handler verifica `event_id` antes de processar
- **At-least-once delivery**: Retry com exponential backoff
- **Ordering**: Por aggregate (usando routing key)
- **Max payload**: 256KB

## Eventos
- `EventPublished` - Evento publicado
- `EventProcessed` - Evento processado com sucesso
- `EventFailed` - Falha no processamento
- `EventDeadLettered` - Movido para DLQ
- `EventReplayed` - Evento reprocessado

## Permissões
- `event:read` - ADMIN (debug)
- `event:replay` - SUPER_ADMIN
- `event:publish` - SYSTEM

## Dependências
- **Todos os módulos** - Publicam e consomem eventos
- **RabbitMQ** - Broker de mensagens

## Fluxo Resumido
1. Ação do domínio → evento criado → publicado no outbox (transação)
2. Outbox publisher → RabbitMQ → consumer verifica idempotência → processa
3. Falha → retry (3x) → DLQ → alerta para admin

## Checklist de Implementação
- [ ] RabbitMQ configurado (exchanges, queues)
- [ ] Domain events base class
- [ ] Transactional outbox pattern
- [ ] Idempotência em todos handlers
- [ ] Dead letter queue configurada
- [ ] Retenção 90 dias
- [ ] Max 256KB payload
- [ ] Replay capability

## Checklist de Testes
- [ ] Evento publicado chega ao consumer
- [ ] Idempotência previne duplicatas
- [ ] Falha vai para DLQ
- [ ] Retry funciona com backoff
- [ ] Payload > 256KB rejeitado

## Documentação Oficial Relacionada
- `docs/event/EVENT-ARCHITECTURE.md`
- `docs/event/RABBITMQ-SETUP.md`
- `docs/event/IDEMPOTENCY.md`

## Histórico de Revisão
| Data | Autor | Descrição |
|------|-------|-----------|
| 2026-07-15 | System | Criação do contexto |
