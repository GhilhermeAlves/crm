# Events — Eventos de Domínio

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Responsabilidades](#responsabilidades)
- [Fluxo](#fluxo)
- [Tipos de Evento](#tipos-de-evento)
- [Endpoints](#endpoints)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar o sistema de eventos de domínio, incluindo publicação, consumo e armazenamento.

## Descrição

Eventos de domínio são notificações de que algo significativo aconteceu no sistema. Eles são publicados via RabbitMQ e consumidos por handlers que executam side effects (atualizar cache, enviar notificação, etc).

## Responsabilidades

- Publicar eventos quando algo acontece
- Consumir eventos e executar handlers
- Garantir entrega pelo menos uma vez (at-least-once)
- Armazenar eventos para auditoria
- Suportar retry e dead letter queues

## Fluxo

### Publicação

```
1. Ação no domínio cria evento
        │
2. Evento é serializado em JSON
        │
3. Evento é publicado no RabbitMQ
        │
4. Evento é persistido no Event Store (audit)
```

### Consumo

```
1. Consumer recebe evento do RabbitMQ
        │
2. Consumer desserializa evento
        │
3. Handler correspondente é executado
        │
4. Se sucesso → ACK no RabbitMQ
   Se falha → Retry (até 3x) → Dead Letter Queue
```

## Tipos de Evento

### Domain Events

| Evento | Contexto | Descrição |
|---|---|---|
| `LeadCreated` | Pipeline | Novo lead criado |
| `LeadQualified` | Pipeline | Lead qualificado |
| `ContactCreated` | Contact | Novo contato criado |
| `OpportunityMoved` | Pipeline | Oportunidade movida |
| `OpportunityWon` | Pipeline | Oportunidade ganha |
| `OpportunityLost` | Pipeline | Oportunidade perdida |
| `MessageSent` | Communication | Mensagem enviada |
| `MessageReceived` | Communication | Mensagem recebida |
| `ConversationCreated` | Communication | Nova conversa |
| `CampaignCompleted` | Campaign | Campanha concluída |

### Integration Events

| Evento | Contexto | Descrição |
|---|---|---|
| `WhatsAppMessageDelivered` | Integration | Entrega confirmada |
| `WhatsAppMessageRead` | Integration | Leitura confirmada |
| `EmailBounced` | Integration | Email não entregue |

## Endpoints

| Método | Endpoint | Descrição | Permissão |
|---|---|---|---|
| GET | `/api/v1/events` | Listar eventos | `event:read` |
| GET | `/api/v1/events/{id}` | Detalhes do evento | `event:read` |
| GET | `/api/v1/events/type/{type}` | Eventos por tipo | `event:read` |

## Dependências

- [06-devops/Docker.md](../06-devops/Docker.md) — RabbitMQ
- [Cache.md](./Cache.md) — Handlers de cache
- [Notifications.md](./Notifications.md) — Handlers de notificação

## Regras

- Eventos são imutáveis
- Eventos devem ser idempotentes
- Payload máximo: 256KB
- Retry: 3 tentativas com backoff
- Dead Letter Queue para falhas permanentes
- Eventos são mantidos por 90 dias
- Cada evento deve ter um `eventId` único

## Futuras Melhorias

- Event Sourcing para contextos críticos
- Event catalog centralizado
- Schema registry para eventos
- Event replay para rebuild de estado
- Métricas de eventos (throughput, latency)

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
