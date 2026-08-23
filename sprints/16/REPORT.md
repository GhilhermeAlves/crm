# Sprint 16 — Omnichannel · WhatsApp (base)

> **Status: ✅ CONCLUÍDA** — hardening aplicado, CI verde, deploy na VPS validado via SSH
> (migrations V053/V054 aplicadas, containers saudáveis, smoke test executado).

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
  - `OmnichannelInboxService` — listar/detalhar conversas, enviar mensagem, `markRead`.
  - `OmnichannelMessagePersister` — ciclo de vida da mensagem em transações `REQUIRES_NEW`
    (padrão `WorkflowActionRunner`): a falha do provider persiste `FAILED` mesmo com rollback.
  - `WhatsAppWebhookService` — verificação GET, mensagem recebida (resolve a empresa pelo canal
    via SECURITY DEFINER, cria conversa/contato se necessário, idempotente por
    `external_message_id`, publica `WorkflowTriggerEvent.whatsAppReceived`), status update
    com filtro de tenant (`external_message_id + company_id`).
- **DTOs**: `ChannelRequest/Response`, `ConversationResponse`, `ConversationDetailResponse`,
  `MessageResponse`, `SendMessageRequest`.
- **Portas**: `OmnichannelChannelRepository`, `OmnichannelConversationRepository`,
  `OmnichannelMessageRepository`, `OmnichannelCompanyResolver`, `WhatsAppWebhookParser`,
  `WhatsAppProvider`.
- **Infra**: `WhatsAppCloudApiProvider` (Meta) e `FakeWhatsAppProvider` (dev,
  `@ConditionalOnProperty` sem ambiguidade de beans), repositórios com RLS FORCE,
  seed de permissões `omnichannel:*`.
- **Controllers** `/api/v1/omnichannel/channels` e `/api/v1/omnichannel/inbox`
  (`@PreAuthorize('omnichannel:*')`) + webhook WhatsApp com validação HMAC
  (`X-Hub-Signature-256`).
- **Migrações**: `V044__omnichannel_tables.sql`, `V045__omnichannel_permissions.sql`,
  `V053__grant_omnichannel_permissions_to_roles.sql`,
  `V054__omnichannel_tenant_fk_constraints.sql`.

### Frontend
- Feature `features/omnichannel/` (types, service, hooks React Query, componentes
  `ConversationList`, `ChatThread`, `ChannelFormDialog`, `ChannelStatusBadge`).
- Páginas `/inbox` (lista de conversas + chat + envio) e `/channels` (configuração de canais),
  gated por `omnichannel:*`; Sidebar atualizada.

## Fechamento (hardening)

1. **Permissões omnichannel** — `V053` concede `omnichannel:*` conforme matriz de autorização
   (ADMIN: todas; MANAGER: read/update/send; AGENT: read/send; VIEWER: read) para todas as
   empresas; `RoleSeedService` atualizado para novos tenants. Validado na VPS.
2. **Failsafe/Testcontainers** — `maven-failsafe-plugin` configurado (`*IT`, `*ITCase`);
   bootstraps SQL dos ITs estavam bloqueados pelo `.gitignore` (`*.sql`) — corrigido;
   CI executa os ITs de verdade.
3. **FAILED persistente** — PENDING → SENT ou PENDING → FAILED sobrevive ao rollback.
4. **Beans de provider** — `fake` (default) vs `cloud-api` exclusivos via `@ConditionalOnProperty`.
5. **HMAC do webhook** — SHA-256 padrão Meta sobre o body bruto; modo sem secret só com
   `webhook-allow-unsigned=true`. Testes: válida/inválida/ausente/payload alterado.
6. **StatusRequest.status** — `@NotNull` + `@Valid`; body inválido retorna erro de validação.
7. **Tenant WHERE** — `updateStatusByExternalId` filtra `external_message_id + company_id`.
8. **Defesa em profundidade (DB)** — `V054`: FKs compostas `(conversation_id, company_id)` /
   `(channel_id, company_id)` impedem mensagem referenciar conversa/canal de outro tenant.

## Qualidade

- Backend: **502 unit tests** verdes + **6 classes de IT** (Testcontainers PostgreSQL 17)
  verdes no `mvn verify`, incluindo `OmnichannelIsolationIT` (isolamento cross-tenant A↔B
  em channels/conversations/messages + idempotência).
- Frontend: lint/typecheck/build OK (CI).
- CI Pipeline: **GREEN** (`d07ea8f`); CD Pipeline: **GREEN**, imagens no GHCR e implantadas
  na VPS.

## Validação VPS (ssh crm-vps)

- Containers: backend/auth-service/frontend/postgres/redis/rabbitmq/minio/keycloak UP/healthy.
- Flyway: V053 e V054 aplicadas (`flyway_schema_history` success=t); FKs presentes;
  permissões omnichannel concedidas por papel.
- Provider: startup sem ambiguidade de beans.
- Smoke test: frontend 200; `/actuator/health` UP; inbox/channels sem auth → 401 (sem 500);
  webhook GET handshake token correto → challenge 200 / errado → 403; webhook POST sem
  assinatura → 401.

## Pendências / débitos (futuros — fora do escopo desta sprint)

- N+1 em `listConversations`; rate limit do webhook; token global ignorando `secretsRef`
  por canal; bounds de paginação; `docs/WHATSAPP.md`; WebSocket no inbox.
- E2E autenticado manual herdado (depende de credenciais reais indisponíveis ao agente).
- Achado durante validação (fora do escopo): módulo de auditoria retorna 500 em busca
  (`lower(bytea) does not exist` — binding de parâmetro como bytea em `audit_logs`);
  registrar para correção futura.
