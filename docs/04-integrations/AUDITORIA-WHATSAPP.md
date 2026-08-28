# AUDITORIA WHATSAPP — CRM

> **Tipo:** Auditoria técnica **somente leitura** — nenhum código, migration, endpoint, config, commit, push ou alteração em VPS foi feito.
> **Data:** 2026-08-28
> **Escopo:** Integração WhatsApp no CRM SaaS Omnichannel (monolito `crm-backend` + `auth-service` gateway + `frontend` Next.js).
> **Legenda:** `REUTILIZAR` = pronto para reuso · `PARCIAL` = existe mas com lacunas · `CRIAR` = não existe, precisa ser construído · `NÃO ENCONTRADO` = ausente · `NÃO CONFIRMADO` = não verificado no ambiente · `[PROPOSTA]` = proposta minha, não existe no código.

---

## 1. Resumo executivo

O CRM **já possui um módulo omnichannel/WhatsApp substancial e funcional** (Sprint 16, "status CONCLUÍDA"), cobrindo: canal, conversa, mensagem (domínio + persistência + RLS), Inbox REST (listar/detalhar/enviar/marcar lida), Webhook Meta com verificação de assinatura HMAC, resolução de tenant sem sessão (SECURITY DEFINER), evento de workflow `WHATSAPP_MESSAGE_RECEIVED`, permissões por papel, e frontend (páginas `/inbox` e `/channels`).

**Porém, em PRODUÇÃO hoje:**
- O provider ativo é o **Fake** (`WHATSAPP_PROVIDER` não setado na VPS → default `fake`). Não há envio real para a WhatsApp Cloud API.
- O **webhook POST é rejeitado** (sem `CRM_WHATSAPP_APP_SECRET` e sem `WHATSAPP_WEBHOOK_ALLOW_UNSIGNED=true` → o verifier devolve 401 para todo evento). Só o handshake GET funciona.
- Existe **1 canal** (ALVES) marcado como `WHATSAPP_CLOUD_API`/ACTIVE, mas o adapter em runtime é o Fake — **mismatch** entre intenção configurada e comportamento real.
- **0 conversas, 0 mensagens** no banco de produção — nenhum tráfego real ainda.

A auditoria mapeia o que reutilizar/estender/criar. O caminho crítico para "ligar de verdade" é: **habilitar Cloud API + configurar secret/token + decidir a URL do webhook + validar o parser (única porta sem teste unitário)**.

---

## 2. Arquitetura geral (como a stack se encaixa)

```
Browser (Next.js 14)
   │  cookie HttpOnly crm_session (sem Bearer no browser, sem X-Tenant-ID)
   ▼
Nginx (prod)  ── /api/ → auth-service :8082  (gateway/BFF relay)
   │                ── /auth/, /realms/ → keycloak/auth-service
   │                ── / → frontend :3000
   ▼
auth-service (gateway)  ── /api/** → crm-backend :8081  (injeta access token + correlation)
   │
   ▼
crm-backend  Symfony?? Não — Spring Boot 3.5.16 / Java 25, context-path /api/v1
   ├── Keycloak JWT (resource server) → CurrentUser (companyId vindo do SecurityContext)
   ├── TenantContext (GUC) + RLS FORCE (PostgreSQL)
   └── Módulo omnichannel/whatsapp (canal/inbox/webhook)
```

**Pontos críticos de roteamento (NÃO CONFIRMADO — ver §31):**
- Backend tem `server.servlet.context-path: /api/v1` **e** os controllers declaram `@RequestMapping("/api/v1/...")`. A combinação exata do caminho efetivo do webhook (o que a Meta deve chamar) precisa ser validada por teste real, sob `https://<domínio>/api/v1/omnichannel/whatsapp/webhook`.

---

## 3. Descoberta-chave: módulo omnichannel JÁ EXISTE

Localização no backend (`backend/src/main/java/com/becommerce/crm/`):

| Camada | Artefatos |
|---|---|
| `domain/omnichannel/` | `Channel`, `Conversation`, `Message` + enums `ChannelProvider` (`WHATSAPP_CLOUD_API`/`FAKE`), `ChannelType` (`WHATSAPP`), `ChannelStatus` (`ACTIVE/INACTIVE/ERROR`), `ConversationStatus` (`OPEN/CLOSED`), `MessageDirection` (`INBOUND/OUTBOUND`), `MessageStatus` (`PENDING/SENT/DELIVERED/READ/FAILED`), `MessageType` (só `TEXT`), exceções |
| `application/omnichannel/` | ports input/output, DTOs, serviços (`OmnichannelChannelService`, `OmnichannelInboxService`, `WhatsAppWebhookService`, `OmnichannelMessagePersister`) |
| `infrastructure/omnichannel/` | JPA entities/repositories, `OmnichannelCompanyResolverImpl`, `whatsapp/` (`WhatsAppCloudApiProvider`, `FakeWhatsAppProvider`, `WhatsAppCloudApiWebhookParser`) |
| `presentation/rest/omnichannel/` | `OmnichannelChannelController`, `OmnichannelInboxController`, `WhatsAppWebhookController`, `WhatsAppWebhookSignatureVerifier` |

**Veredito:** NÚCLEO REUTILIZAR (ver detalhe §§4–12).

---

## 4. Canal (Channel) — REUTILIZAR

- Modelo: `Channel` (companyId, type, provider, name, status, externalId, config, **secretsRef**). Segurança: guarda apenas **referência** a secret externo, nunca o token.
- Migração `V044__omnichannel_tables.sql` → tabela `omnichannel_channels`, RLS FORCE + policy `tenant_isolation_policy`, índice único `(company_id, external_id)`.
- Service `OmnichannelChannelService`: CRUD scoped {TenantContext + requireOwned}. Controller `OmnichannelChannelController` `@PreAuthorize` (`omnichannel:read/create/update/delete`).
- **Lacuna:** o `WhatsAppCloudApiProvider.resolveToken` ignora `secretsRef` do canal e usa token **global** `CRM_WHATSAPP_ACCESS_TOKEN` (débito registrado no sprint). Ver §15.

---

## 5. Conversa (Conversation) — REUTILIZAR

- Modelo: `Conversation` (companyId, channelId, **contactId** opcional, externalPhone, status, lastMessageAt, unreadCount; `touch`/`markRead`/`close`/`reopen`/`assignContact`).
- Tabela `omnichannel_conversations`, UNIQUE `(company_id, channel_id, external_phone)`, RLS FORCE.
- **Valioso:** `contactId` referencia a entidade `contacts` quando o matching por telefone é bem-sucedido — **sem duplicar** cadastro.

---

## 6. Mensagem (Message) — REUTILIZAR (base) + CRIAR (mídia)

- Modelo `Message`: direction, senderPhone, recipientPhone, type, body, status, **externalMessageId** (wamid), **clientMessageId** (UUID de idempotência).
- `omnichannel_messages`: UNIQUE `(company_id, client_message_id)` e `(company_id, external_message_id)` p/ idempotência. Repo `saveByExternalId` faz `INSERT ... ON CONFLICT (company_id, external_message_id) DO NOTHING`.
- **Nosso app só suporta `MessageType.TEXT`.** WhatsApp envia IMAGE/AUDIO/VIDEO/DOCUMENT/STICKER — **CRIAR** tipos + vínculo com `storage_objects` (ver §23) + parsing de mídia (ver parser §15 gap).

---

## 7. Inbox (listar/detalhar/enviar/marcar lida) — REUTILIZAR

- `OmnichannelInboxController` `/api/v1/omnichannel/inbox`:
  - `GET /inbox` (paginado) — `omnichannel:read`
  - `GET /inbox/{conversationId}` — `omnichannel:read`
  - `POST /inbox/{conversationId}/messages` — `omnichannel:send` (envia via provider; PENDING→SENT/FAILED em REQUIRES_NEW p/ sobreviver a rollback)
  - `POST /inbox/{conversationId}/read` — `omnichannel:update`
- companyId vem do SecurityContext (JWT Keycloak).

---

## 8. Webhook Meta (recebimento) — REUTILIZAR (código) + PARCIAL (config)

- `WhatsAppWebhookController` `/api/v1/omnichannel/whatsapp/webhook`:
  - `GET` handshake → devolve `challenge` se `hub.verify_token` == `omnichannel.whatsapp.webhook-verify-token`; senão 403.
  - `POST` → valida `X-Hub-Signature-256` (HMAC SHA-256) e chama `WebhookUseCase.handleEvent`. `permitAll` no SecurityConfig (protegido por HMAC próprio).
- **Config em produção (VPS) — deve ser corrigida:**
  - `OMNICHANNEL_WHATSAPP_WEBHOOK_VERIFY_TOKEN` ✅ setado.
  - `CRM_WHATSAPP_APP_SECRET` ❌ **não setado** → `isEnforced()==false`.
  - `WHATSAPP_WEBHOOK_ALLOW_UNSIGNED` ❌ **não setado** (default `false`).
  - **Resultado:** `WhatsAppWebhookSignatureVerifier.isValid` retorna `false` (sem secret + allow-unsigned=false) → **todo POST recebe 401**. O webhook está, na prática, desligado para eventos reais.
  - **[PROPOSTA]** Definir na VPS `CRM_WHATSAPP_APP_SECRET` (secreto da app Meta) e expor a URL do webhook; ou, em dev, `WHATSAPP_WEBHOOK_ALLOW_UNSIGNED=true` conscientemente.

---

## 9. Resolução de tenant sem sessão (webhook) — REUTILIZAR

- `OmnichannelCompanyResolverImpl` → `SELECT app.resolve_channel_company(?)`.
- Função `SECURITY DEFINER` `app.resolve_channel_company(p_external_id)` (V044): mapeia `external_id` do canal → `company_id`, rodando como owner para **apenas** essa consulta de mapeamento; o restante da persistência continua sob RLS via GUC `app.current_tenant_id()`.
- `WhatsAppWebhookService.handleEvent` faz `TenantContext.setCompanyId(companyId)` / `clear()` em finally.
- Seguro e correto. **NÃO CONFIRMADO** no ambiente (sem teste dedicado unitário; coberto indiretamente por `OmnichannelIsolationIT`).

---

## 10. Idempotência — REUTILIZAR

- Recebimento: UNIQUE `(company_id, external_message_id)` + `ON CONFLICT DO NOTHING` + guarda `findByExternalMessageId`.
- Envio: `clientMessageId` (UUID) UNIQUE `(company_id, client_message_id)`.
- Evento de workflow: `eventId` determinístico = messageId.

---

## 11. Provider WhatsApp (envio) — REUTILIZAR (porta) + PARCIAL (real)

- Porta `WhatsAppProvider.send(SendRequest) → SendResult(wamid)`.
- `FakeWhatsAppProvider` (`provider=fake`, default, `matchIfMissing=true`) — dev teste, gera `FAKE_...`.
- `WhatsAppCloudApiProvider` (`provider=cloud-api`, `@ConditionalOnProperty`) — chama Meta Graph `/{phone_number_id}/messages`, `Authorization: Bearer` com token de env. Token via env `CRM_WHATSAPP_ACCESS_TOKEN` (nunca logado/persistido).
- **Prod: `WHATSAPP_PROVIDER` não setado → usa Fake.** O canal ALVES está marcado `WHATSAPP_CLOUD_API`, mas o bean ativo é `FakeWhatsAppProvider` → **mismatch** (enviar não atinge a Meta).

---

## 12. Permissões / RBAC — REUTILIZAR

- `V045` seed: `omnichannel:read/create/update/delete/send`.
- `V053` vincula por papel (ADMIN=all; MANAGER=read/update/send; AGENT=read/send; VIEWER=read).
- Controllers com `@PreAuthorize`. Tabela `role_permissions`, binding no startup via `RoleSeedService`.

---

## 13. Segurança multi-tenant / RLS — REUTILIZAR

- `V044`: RLS **FORCE** + `tenant_isolation_policy` nas 3 tabelas omnichannel (USING/WITH CHECK = `company_id = app.current_tenant_id()`).
- `V054`: FKs compostas `(id, company_id)` p/ impedir referência cruzada entre tenants (defesa em profundidade).
- `OmnichannelIsolationIT` (Testcontainers) valida A↔B, sem-contexto, cross-tenant bloqueado, idempotência.
- **Nota:** Lado webhook usa GUC `app.current_tenant_id()` — consistente com o resto do app (X-Tenant-ID **não** controla tenant; tenant vem do SecurityContext/sessão).

---

## 14. Frontend — REUTILIZAR

- Framework **Next.js 14.2.21** (App Router), TypeScript, **axios + @tanstack/react-query**, react-hook-form + zod.
- **`src/features/omnichannel/`** completo (REST/MVP):
  - `services/omnichannel.service.ts` — endpoints channels/inbox.
  - `hooks/useOmnichannel.ts` — hooks + permissões `omnichannel:*`.
  - `components/` — `ConversationList`, `ChatThread`, `ChannelStatusBadge`, `ChannelFormDialog`.
  - Tipos em `types/omnichannel.types.ts` (Channel/Conversation/Message).
- Páginas: **`app/(dashboard)/inbox/page.tsx`** (inbox + thread + envio + markRead) e **`app/(dashboard)/channels/page.tsx`** (CRUD).
- API client `src/lib/api.ts`: `baseURL "/api/v1"`, cookie HttpOnly `crm_session`, sem Bearer no browser, **sem X-Tenant-ID** (tenant resolvido no backend).
- Métricas: `reports/page.tsx` + `analytics.types.ts` (`omnichannelMessagesIn/Out`). Workflow trigger `WHATSAPP_MESSAGE_RECEIVED` tipado. Campanhas usam `useChannels()`.
- IA no inbox: `ChatThread` usa `useSuggestReply()`.

**NÃO ENCONTRADO no frontend:**
- Conexão de canal por QR code/otp/OAuth (canal criado só com `externalId`/`config`/`secretsRef` por texto).
- Contato-a-contato end-to-end.
- Testes automatizados do módulo omnichannel (zero `*.test.*` na pasta).

---

## 15. WhatsApp Cloud API — provider/parser (gap de robustez)

- `WhatsAppCloudApiWebhookParser` — **REUTILIZAR**, porém:
  - Só lê `text.body`. **Mídia (image/audio/video/document) NÃO é tratada** → CRIAR parsing de mídia + download + link `storage_objects`.
  - **Sem teste unitário** (única porta de entrada não validada; exercitada só indiretamente via mock no `WhatsAppWebhookServiceTest`). **[PROPOSTA]** adicionar testes do parser com fixtures reais do Meta.
- `WhatsAppCloudApiProvider` — **REUTILIZAR**, porém: token global ignora `secretsRef` por canal (débito). **Sem teste** (só fake coberto implicitamente). **[PROPOSTA]** testes de mock RestClient.

---

## 16. Contato (Contact) — PARCIAL (matching OK; auto-creation a CRIAR)

- `domain/contact/Contact`: id, companyId, firstName/lastName, **email, phone**, notes, soft-delete, quota.
- Porta `ContactRepository.findByCompanyIdAndPhone(...)` ([V015] `contacts`, RLS [V021]).
- Webhook já usa esse matching: se achar, vincula `conversation.contactId`; **se não achar, cria a conversa com `contactId=null` e NÃO cria o contato** (ver §18).
- **CRIAR:** upsert/auto-create de contato a partir do número recebido (normalização E.164 + índice único de phone) — reusando `ContactRepository.save` + `CompanyQuotaService`.

---

## 17. Client / Lead — PARCIAL (sem "Customer" separado)

- **NÃO ENCONTRADO** pacote `domain/customer` / tabela `customers`. O "Customer 360" é uma **visão agregada** (`application/customer360/service/Customer360Service`) sobre contact + opportunity + tasks + activity.
- **Lead existe e já reconhece WhatsApp:** `domain/lead/Lead` (contactId NOT NULL, status, score, source), `LeadSource.WHATSAPP` (V016 `leads`, UNIQUE `(contact_id, company_id)`, CHECK source inclui WHATSAPP).
- **CRIAR:** serviço que cria Lead a partir de WhatsApp inbound (reusando `LeadRepository`, `LeadSource.WHATSAPP`) — hoje o webhook não cria Lead.

---

## 18. Fluxo inbound atual (o que ocorre hoje)

`Meta POST` → webhook (se não for 401) → parser → resolve channel/company → dedupe por wamid → acha/cria conversa (linka contato se match) → salva mensagem (idempotente) → touch conversa (unread++) → publica `WorkflowTriggerEvent.whatsAppMessageReceived`.

**Lacunas:** não cria contato/lead automaticamente; só persiste e dispara evento.

---

## 19. Workflow engine & trigger WHATSAPP — REUTILIZAR (cadeia genérica) + CRIAR (ações específicas)

- `TriggerEvent.WHATSAPP_MESSAGE_RECEIVED` + `WorkflowTriggerEvent.whatsAppMessageReceived(companyId, contactId, conversationId, messageId, from, body)` (eventId determinístico).
- Publicação: `WhatsAppWebhookService` → `EventPublisher` (Spring in-process) → `WorkflowEventListener` → `WorkflowExecutor.process` (busca por `trigger + active` de forma **genérica**) → `WorkflowActionRunner`.
- Ações existentes (genéricas por trigger): `CREATE_TASK`, `CREATE_ACTIVITY`, `SEND_NOTIFICATION`, `EXECUTE_CAMPAIGN`.
- **NÃO ENCONTRADO:** nenhuma ação **específica** de WhatsApp (resposta automática, adiamento, envio de template) nem `WorkflowTemplateSeeder` com trigger WHATSAPP, nem teste do trigger.
- **[PROPOSTA]** criar ação `SEND_WHATSAPP_MESSAGE`/template + template seed + teste.

---

## 20. Notificações — REUTILIZAR

- Módulo `notification` (V047/V048): `NotificationType` inclui **`MESSAGE`**; tabela `notifications` (user_id, company_id, RLS FORCE); REST list/mark-read/unread.
- **Push realtime:** `infrastructure/notification/websocket/StompNotificationPusher` → STOMP `/user/{userId}/queue/notifications`.
- Frontend: `NotificationBell` (badge, poll 15s), `useNotifications` (`refetchInterval: 15000`), página `/notifications`.
- **[PROPOSTA]** notificar agente ao chegar nova mensagem WhatsApp (reuso in-app + STOMP). FCM/APNS e e-mail real: NÃO ENCONTRADO (só fake/console).

---

## 21. Realtime/WebSocket — REUTILIZAR (infra) 

- `WebSocketConfig`: STOMP endpoint `/api/v1/ws`, SockJS fallback, broker `/topic`/`/queue`, user prefix `/user`, app prefix `/app`.
- `StompAuthChannelInterceptor`: valida JWT no CONNECT (mesmo RBAC/tenant do REST).
- Frontend tem `socket.io-client@^4.8.1` declarado, **mas sem nenhum uso** (grep zero em `src/`). Inbox não é realtime (react-query sob demanda + `invalidateQueries`; sem refetchInterval no omnichannel).
- **[PROPOSTA]** dedicar um destination de eventos de inbox (ex.: `/user/{id}/queue/inbox` ou `/topic/company/{cid}/inbox`) sobre a infra STOMP existente, e consumir no frontend.

---

## 22. Fila / mensageria — NÃO ENCONTRADO (somente in-process)

- `spring-boot-starter-amqp` (RabbitMQ), `starter-data-redis`, `starter-webflux` **declarados no pom, sem uso** (Rabbit sem wiring; Redis só em `InvitationRateLimiter`).
- Desacoplamento atual = **Spring `ApplicationEventPublisher`** (síncrono in-process) via porta `EventPublisher`.
- **[PROPOSTA]** se for necessário desacoplar/durabilizar/retry do webhook→envio (e/ou status), **criar** camada assíncrona reusando a porta `EventPublisher` como seam e o starter AMQP já presente. Hoje: suficiente para MVP.

---

## 23. Armazenamento / arquivo / mídia — PARCIAL (blob genérico) + CRIAR (vínculo)

- `StorageService`/`storage_objects` (V037): blob **no Postgres** (`data bytea`), quota `max_storage_mb`, RLS FORCE. Migration nota: permite trocar por object-store externo (MinIO/S3) no futuro. **NÃO ENCONTRADO** MinIO/S3; sem tabela `attachment`.
- **CRIAR:** `omnichannel_messages.type` só tem TEXT; sem coluna/vínculo mídia→`storage_objects`. Ver §6/§15.

---

## 24. IA — REUTILIZAR (para auto-resposta/sugestão)

- Módulo AI maduro (V049–V052): `AiSuggestionService`, `AiAssistantService`, `AiChatProvider` (OpenAI), tool registry (Customer360, UpdateOpportunity, CreateTask…), 5 controllers.
- Inbox já usa `useSuggestReply()`. Nível de esforço para auto-reply: **médio** (falta ponte inbound `omnichannel_message` → assistente; existem bots de resposta).
- **PDF/relatório:** NÃO ENCONTRADO (só perms `report:*` + `analytics` summary).

---

## 25. Campanhas (CRM "campaign") — REUTILIZAR

- V055–V062: `message_templates`, `campaigns`, `campaign_channels` (→ `omnichannel_channels` + templates), `campaign_executions`, `campaign_message_events` (idempotência UNIQUE `(execution_id, recipient_id)`).
- Frontend: `campaigns` usa `useChannels()`; envio via canal (`campaign_channels`) → dá para fazer **campanha via WhatsApp** reusando canal/provider. **[PROPOSTA]** verificar se o disparo de campanha já usa `WhatsAppProvider` ou ficou em template genérico.

---

## 26. Módulos auxiliares compartilhados — REUTILIZAR

- `TenantContext`/GUC + `TenantAwareDataSource` + RLS FORCE (padrão do app p/ multi-tenancy).
- `PageResponse` (paginação), `EventPublisher` (porta), `CompanyQuotaService` (quota), `AuditEventListener` (auditoria), Keycloak JWT resource server + `CurrentUser` (companyId do SecurityContext).

---

## 27. Testes existentes — REUTILIZAR (parcialmente); lacunas a CRIAR

Existem (verdes): `ChannelTest`(3), `ConversationTest`(6), `MessageTest`(5), `OmnichannelChannelServiceTest`(8), `OmnichannelInboxServiceTest`(5), `WhatsAppWebhookServiceTest`(5), `WhatsAppWebhookControllerTest`(4), `WhatsAppWebhookSignatureVerifierTest`(6), `OmnichannelIsolationIT`(7, Testcontainers).

**NÃO ENCONTRADO (lacunas):**
- ⬜ `WhatsAppCloudApiWebhookParser` (unit) — **mais crítico**.
- ⬜ `WhatsAppCloudApiProvider` / `FakeWhatsAppProvider`.
- ⬜ `OmnichannelCompanyResolverImpl`.
- ⬜ Controllers Channel/Inbox (MockMvc) — só webhook tem.
- ⬜ `OmnichannelMessageRepositoryImpl` (upsert/updateStatus) unit.
- ⬜ Teste/seed do trigger `WHATSAPP_MESSAGE_RECEIVED`.

---

## 28. Documentação — PARCIAL (dispersa/desatualizada)

- `docs/04-integrations/WhatsApp.md` — doc real (v1.0), fluxos, regras WA-001..007, **menciona Evolution API e Meta Business API**; mas o backend só implementa **Meta Cloud API + Fake** ← divergência.
- `docs/04-integrations/EvolutionAPI.md` — descreve gateway Evolution; **sem adapter no backend** (NÃO ENCONTRADO impl).
- `docs/WHATSAPP.md` (referenciado na V044) — **NÃO ENCONTRADO**.
- `sprints/16/REPORT.md` — status CONCLUÍDA + débitos: N+1 em listConversations, rate limit webhook, token global ignora secretsRef, bounds de paginação, `docs/WHATSAPP.md` pendente, **WebSocket no inbox pendente**.
- `playbooks/implement-whatsapp.md` — caminhos antigos (`packages/backend/...`) <não condizentes> com a estrutura real (`src/main/java/...`).
- **NÃO ENCONTRADO** documento consolidado de auditoria/decisão de arquitetura para a integração real (não há `docs/WHATSAPP.md`).

---

## 29. Config atual (application.yml + docker + VPS) — PARCIAL

`application.yml` (linhas 112–117):
```yaml
omnichannel:
  whatsapp:
    provider: ${WHATSAPP_PROVIDER:fake}
    app-secret: ${CRM_WHATSAPP_APP_SECRET:}
    webhook-verify-token: ${CRM_WHATSAPP_WEBHOOK_VERIFY_TOKEN:}
    webhook-allow-unsigned: ${WHATSAPP_WEBHOOK_ALLOW_UNSIGNED:false}
```
`docker/docker-compose.yml` linha 56: `OMNICHANNEL_WHATSAPP_WEBHOOK_VERIFY_TOKEN=${OMNICHANNEL_WHATSAPP_WEBHOOK_VERIFY_TOKEN:-}`.

**VPS (produção) — estado real:**
| Var | Valor | Impacto |
|---|---|---|
| `WHATSAPP_PROVIDER` | ❌ não setado | provider = **Fake** (não envia para Meta) |
| `CRM_WHATSAPP_ACCESS_TOKEN` | ❌ não setado | Cloud API falharia "credencial não configurada" |
| `OMNICHANNEL_WHATSAPP_WEBHOOK_VERIFY_TOKEN` | ✅ setado | handshake GET OK |
| `CRM_WHATSAPP_APP_SECRET` | ❌ não setado | assinatura não obrigatória |
| `WHATSAPP_WEBHOOK_ALLOW_UNSIGNED` | ❌ não setado | **default false → POST 401** |

---

## 30. Banco de dados (produção) — estado

Existe **1 canal** (empresa ALVES `db8115d1…`), `type=WHATSAPP`, **`provider=WHATSAPP_CLOUD_API`**, `status=ACTIVE`. **0 conversas, 0 mensagens.**
- **Observação importante:** o canal diz `CLOUD_API`, mas o runtime usa `FakeWhatsAppProvider` (env) → a operação de envio não chega à Meta.
- 17 usuários/empresas (ALVES + Default) conforme contexto anterior — sem relação direta com WhatsApp (nenhum canal além do ALVES).

---

## 31. Riscos / pontos a validar (NÃO CONFIRMADO)

1. **URL/rota efetiva do webhook** (context-path `/api/v1` + controllers `/api/v1/...` + nginx + gateway) — precisa de teste real por baixo de `https://<domínio>/api/v1/omnichannel/whatsapp/webhook`.
2. **Webhook POST bloqueado** em prod (sem app-secret) — não processa inbound.
3. **Mismatch provider** (canal CLOUD_API ↔ runtime Fake).
4. **Mídia WhatsApp** não tratada (só TEXT).
5. **E.164 / normalização de telefone** não validada (campo `phone` VARCHAR simples; sem índice único).
6. **Rate limit** do webhook/Cloud API não tratado (débito sprint).
7. **N+1** em `listConversations` (débito sprint).

---

## 32. Roadmap / MVP proposto — [PROPOSTA]

**Fase A — Ligar o que já existe (curto, ~1 dia):**
- [ ] [PROPOSTA] Definir na VPS: `WHATSAPP_PROVIDER=cloud-api`, `CRM_WHATSAPP_ACCESS_TOKEN`, `CRM_WHATSAPP_APP_SECRET` (secret Meta) — sem exibir valores.
- [ ] [PROPOSTA] Validar rota do webhook end-to-end (handshake GET + recebimento POST) atrás do domínio real.
- [ ] [PROPOSTA] Adicionar teste unitário do `WhatsAppCloudApiWebhookParser` (fixtures Meta reais) — maior risco aberto.
- [ ] [PROPOSTA] Adicionar testes de `WhatsAppCloudApiProvider`/`FakeWhatsAppProvider` e `OmnichannelCompanyResolverImpl`.

**Fase B — Robustez (médio):**
- [ ] [PROPOSTA] Auto-criação de **contato** (e opcionalmente **Lead** com `LeadSource.WHATSAPP`) a partir de número inbound (E.164 + índice de phone).
- [ ] [PROPOSTA] Suporte a **mídia**: novos `MessageType`, vínculo com `storage_objects`, parsing/download de mídia no parser.
- [ ] [PROPOSTA] Notificação/realtime de novas mensagens (`/user/{id}/...`) + consumir no frontend (hoje Zero realtime).
- [ ] [PROPOSTA] Token/secrets por canal (respeitar `secretsRef`), retry/rate-limit, corrigir N+1 e bounds de paginação.

**Fase C — Automatização/valor (médio):**
- [ ] [PROPOSTA] Ação de workflow `SEND_WHATSAPP_MESSAGE` (resposta automática / template) + seed `WHATSAPP_MESSAGE_RECEIVED` + teste do trigger.
- [ ] [PROPOSTA] Auto-reply via módulo AI (`AiSuggestionService`/`AiChatProvider`).
- [ ] [PROPOSTA] Campanha WhatsApp via `campaign_channels` (verificar disparo).

**Fora de escopo atual (avaliar):** adapter Evolution API (nos docs, sem impl no backend), fila Rabbit para desacoplar/durabilizar (hoje in-process), QR-code/OAuth de conexão de canal no frontend.

---

## Apêndice A — Mapa migration × módulo (referência)

| Migration | Módulo |
|---|---|
| V015/V021 | contacts (+RLS) |
| V016 | leads (source WHATSAPP) |
| V037 | storage_objects (blob) |
| V044 | omnichannel tables + `app.resolve_channel_company` (SECURITY DEFINER) |
| V045 / V053 | omnichannel perms / grants por papel |
| V047/V048 | notifications (+perms) |
| V049–V052 | AI |
| V054 | omnichannel tenant FKs |
| V055–V062 | campaign/templates/executions/message events |

## Apêndice B — Classificação consolidada

| Item | Veredito |
|---|---|
| Núcleo omnichannel (domain+service+persistence+controllers) | REUTILIZAR |
| Inbox REST | REUTILIZAR |
| Webhook + assinatura HMAC | REUTILIZAR (código) / CONFIG a corrigir |
| Resolver de tenant (SECURITY DEFINER) | REUTILIZAR |
| Idempotência (DB unique + upsert) | REUTILIZAR |
| Permissões/RBAC + RLS/FK composta | REUTILIZAR |
| Frontend (inbox/channels + API client) | REUTILIZAR |
| Workflow trigger WHATSAPP (cadeia genérica) | REUTILIZAR |
| Ações/automação específica WhatsApp | CRIAR |
| Auto-criação de contato/lead por WhatsApp | CRIAR |
| Mídia/attachment (tipo + storage) | CRIAR |
| Fila assíncrona (Rabbit) | NÃO ENCONTRADO (deps presentes) — decisão |
| Realtime do inbox no frontend | PARCIAL (infra STOMP pronta, zero uso) |
| Adapter Evolution API | NÃO ENCONTRADO (docs apenas) |
| Tests parser/provider/resolver | CRIAR |
| `docs/WHATSAPP.md` | NÃO ENCONTRADO (só `docs/04-integrations/WhatsApp.md`) |
