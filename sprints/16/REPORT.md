# Sprint 16 — Omnichannel · WhatsApp (base)

> **Status: 🚧 Em andamento** — código e testes concluídos; deploy/VPS e IT Testcontainers pendentes
> (requerem Docker na VPS). A Sprint só será marcada **✅ Concluída** após o DoD (deploy + VPS +
> commit + working tree limpo).

## Escopo

Base do Omnichannel de WhatsApp: canais (configuração), inbox (conversas + chat) e webhook
(mensagens recebidas/status), com isolamento multi-tenant por RLS FORCE.

## Entregas

### Backend
- **Domínio** (`domain/omnichannel/`): `Channel`, `Conversation`, `Message` + enums
  (`ChannelType`, `ChannelProvider`, `ChannelStatus`, `ConversationStatus`, `MessageDirection`,
  `MessageStatus`, `MessageType`) com fábricas `create`/`reconstitute` e transições de estado.
- **Aplicação** (`application/omnichannel/`):
  - `OmnichannelChannelService` — CRUD + `setStatus`, scoped à empresa ativa.
  - `OmnichannelInboxService` — listar/detalhar conversas, enviar mensagem (falha do provider
    marca a mensagem `FAILED` e relança `OmnichannelProviderException`), `markRead`.
  - `WhatsAppWebhookService` — verificação GET, mensagem recebida (resolve a empresa pelo canal
    via SECURITY DEFINER, cria conversa/contato se necessário, idempotente por
    `external_message_id`, publica `WorkflowTriggerEvent.whatsAppMessageReceived`), status update.
- **DTOs**: `ChannelRequest/Response`, `ConversationResponse`, `ConversationDetailResponse`,
  `MessageResponse`, `SendMessageRequest`.
- **Portas**: `OmnichannelChannelRepository`, `OmnichannelConversationRepository`,
  `OmnichannelMessageRepository`, `OmnichannelCompanyResolver`, `WhatsAppWebhookParser`,
  `WhatsAppProvider`.
- **Infra**: `WhatsAppCloudApiProvider`/parser (Meta) e `FakeWhatsAppProvider` (dev),
  repositórios com RLS FORCE, seed de permissões `omnichannel:*`.
- **Controllers** `/api/v1/omnichannel/channels` e `/api/v1/omnichannel/inbox`
  (`@PreAuthorize('omnichannel:*')`) + webhook WhatsApp.
- **Migrações**: `V044__omnichannel_tables.sql`, `V045__omnichannel_permissions.sql`.

### Frontend
- Feature `features/omnichannel/` (types, service, hooks React Query, componentes
  `ConversationList`, `ChatThread`, `ChannelFormDialog`, `ChannelStatusBadge`).
- Páginas `/inbox` (lista de conversas + chat + envio) e `/channels` (configuração de canais),
  gated por `omnichannel:*`; Sidebar atualizada.

## Qualidade
- Backend: **+33 testes** omnichannel verdes — domínio (Channel/Conversation/Message) e serviços
  (`OmnichannelInboxServiceTest`, `WhatsAppWebhookServiceTest`, `OmnichannelChannelServiceTest`).
- Frontend: typecheck/lint sem erros nos novos arquivos; **build prod OK** com `/inbox` e
  `/channels` geradas.
- IT `OmnichannelIsolationIT` (Testcontainers/PostgreSQL + RLS cross-tenant em
  channels/conversations/messages) escrita — **a rodar na VPS** (Docker disponível lá).

## Pendências / débitos
- Deploy + validação na VPS (Docker/Testcontainers) e rodada da `OmnichannelIsolationIT`.
- E2E autenticado manual (herdado).
- Campanhas (Sprint 17), automações (18), WebSocket de notificações (futuro).
