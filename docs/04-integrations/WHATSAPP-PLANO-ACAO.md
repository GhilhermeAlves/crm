# PLANO DE AÇÃO — INTEGRAÇÃO WHATSAPP (CRM)

> **Tipo:** Plano de ação derivado da auditoria `docs/04-integrations/AUDITORIA-WHATSAPP.md`.
> **Natureza:** O plano descreve **o que será feito** — NADA FOI ALTERADO ainda. Cada item está marcado **[PROPOSTA]** até ser executado.
> **Data:** 2026-08-28
> **Legenda:** `[PROPOSTA]` = decidido, ainda não executado · `[PRONTO]` = já existe/reutilizável · `[DÉBITO]` = dívida técnica registrada em `sprints/16/REPORT.md`.

---

## Visão geral

O núcleo omnichannel/WhatsApp **já está pronto para reuso**. O plano é em **3 fases**:

- **Fase A — Ligar o que já existe** (curto, ~1–2 dias): fazer o webhook + envio reais funcionarem em produção, e fechar a maior lacuna de teste (parser Cloud API).
- **Fase B — Robustez** (médio): contato/lead automáticos, suporte a mídia, realtime no frontend, secrets por canal.
- **Fase C — Automatização/valor** (médio): ações de workflow WhatsApp (resposta/auto-reply), campanha via canal.

Cada tarefa traz: contexto, mudanças de código/config, arquivos, verificação e critério de aceite.

---

## FASE A — Ligar o que já existe (curto)

### A1. [PROPOSTA] Configurar env real na VPS (Cloud API)

**Contexto:** Hoje `WHATSAPP_PROVIDER` não é setado → `FakeWhatsAppProvider` ativo; canal ALVES diz `CLOUD_API` mas envia pelo Fake. `CRM_WHATSAPP_APP_SECRET` ausente + `WHATSAPP_WEBHOOK_ALLOW_UNSIGNED=false` → webhook POST 401.

**Ações (config, sem código):**
1. No `docker-compose.yml` (root) / env da VPS, definir para o serviço `crm-backend`:
   - `WHATSAPP_PROVIDER=cloud-api`
   - `CRM_WHATSAPP_ACCESS_TOKEN=<token de envio da app Meta>` (não exibir; deixar em secret/env da VPS)
   - `CRM_WHATSAPP_APP_SECRET=<App Secret da Meta>` (webhook; exigido p/ HMAC)
   - `WHATSAPP_WEBHOOK_ALLOW_UNSIGNED` → manter `false` (seguro) — o `app-secret` passa a valer.
2. **Sem expor secrets em commit** — apenas variáveis referenciadas.

**Verificação:** `docker exec crm-backend printenv | grep WHATSAPP` na VPS, conferindo `WHATSAPP_PROVIDER=cloud-api`, `CRM_WHATSAPP_APP_SECRET` e `CRM_WHATSAPP_ACCESS_TOKEN` presentes.

**Critério de aceite:** O bean `FakeWhatsAppProvider` deixa de ser o selecionado; `WhatsAppCloudApiProvider` assume (envio real), e `WhatsAppWebhookSignatureVerifier.isEnforced()==true` (assinatura obrigatória).

---

### A2. [PROPOSTA] Validar a rota efetiva do webhook

**Contexto:** Backend tem `server.servlet.context-path=/api/v1` **e** controllers `@RequestMapping("/api/v1/...")`; nginx repassa `/api/` → gateway → backend; `ApiRelayController` expõe `/api/**`. A URL exata que a Meta deve chamar precisa de validação real (não presumir).

**Ações:**
1. Testar por baixo do domínio real:
   - Handshake: `GET https://<domínio>/api/v1/omnichannel/whatsapp/webhook?hub.mode=subscribe&hub.verify_token=<v>&hub.challenge=challenge123` → esperado **200** com body `challenge123`.
   - Body: `POST .../webhook` com payload Meta de teste + `X-Hub-Signature-256` válida → 200.
2. Registrar o resultado (200/404/500) e o **caminho efetivo** corrigido. Se houver prefixo duplicado `/api/v1/api/v1/...`, ajustar a rota (ver A-risco §).

**Arquivos consultados:** `backend/.../security/config/SecurityConfig.java:82` (permitAll), `WhatsAppWebhookController.java`, `auth-service/.../ApiRelayController.java`, `docker/nginx/crm.conf` (`location /api/` → 8082).

**Critério de aceite:** Handshake e POST respondem 200/200 no domínio público; a URL fica documentada para cadastro na Meta (WhatsApp Cloud API → Webhook).

---

### A3. [PROPOSTA] Teste unitário do `WhatsAppCloudApiWebhookParser` — **maior lacuna**

**Contexto:** É a única porta de entrada de inbound sem teste unitário (exercitada só via mock). Fixtures padrão Meta.

**Tarefa:** Criar `WhatsAppCloudApiWebhookParserTest` em `backend/src/test/.../infrastructure/omnichannel/whatsapp/`, cobrindo:
- `isInboundMessage`/`isStatusUpdate` verdadeiros e falsos;
- `parseInboundMessage`: `entry[].changes[].value.messages[0]` → wamid, `from`, `metadata.phone_number_id` (to), `text.body`;
- `parseStatusUpdate`: `statuses[0].id` + `.status` → mapping `sent/delivered/read/failed` e erro `errors[0].message`;
- `parseVerification` (hub.*) e `providerChannelReference` (metadata.phone_number_id);
- tolerância a payloads malformados (vazios, tipos inesperados).

**Verificação:** `mvn test -Dtest=WhatsAppCloudApiWebhookParserTest` (rodar no `backend`).

**Critério de aceite:** suite nova verde; parser validado com fixtures Meta reais (ex.: payload de mensagem de texto e de status de entrega/leitura).

---

### A4. [PROPOSTA] Testes de `WhatsAppCloudApiProvider` / `FakeWhatsAppProvider` e `OmnichannelCompanyResolverImpl`

**Contexto:** fabricantes não testados; token global (débito).

**Tarefa:**
- `WhatsAppCloudApiProviderTest`: com `RestClient` mockado (ou `MockRestServiceServer`), validar: sucesso devolve wamid; resposta sem wamid → `OmnichannelProviderException`; erro HTTP → exceção; token ausente → exceção clara.
- `FakeWhatsAppProviderTest`: devolve `FAKE_...` sintético.
- `OmnichannelCompanyResolverImplTest`: com `JdbcTemplate`/mock, retorna `Optional<UUID>` correto e vazio quando não acha.

**Verificação:** rodar os novos testes + `mvn test` no backend (531 atuais preservados).

---

### A5. [PROPOSTA] Sobrescrever débitos de segurança/robustez antes de abrir ao público

**Contexto (`sprints/16/REPORT.md`):** token global ignora `secretsRef` por canal; sem rate limit do webhook.

**Tarefas:**
- **[PROPOSTA]** Fazer `resolveToken` respeitar `secretsRef` por canal (buscar secret no vault/env por canal) em vez de token global único — manter fallback global para compatibilidade.
- **[PROPOSTA]** Adicionar rate limit no webhook (reuso do padrão `ApiRateLimitFilter`/Redis já do gateway, ou `InvitationRateLimiter`).

**Critério de aceite:** canais distintos podem ter tokens distintos; webhook com burst não estoura quota/Meta.

---

## FASE B — Robustez (médio)

### B1. [PROPOSTA] Auto-criação de contato (e Lead) a partir de número inbound

**Contexto:** Webhook só linka `contactId` se já existir conta com o telefone; nunca cria. Leads reconhecem `LeadSource.WHATSAPP` (V016), mas nenhum serviço cria lead de WhatsApp.

**Tarefas:**
1. **Normalização E.164** de telefone (helper) + **índice único** de phone (migration nova) — hoje coluna `phone` é VARCHAR simples sem unique/validação.
2. Em `WhatsAppWebhookService.handleInbound`, quando não achou contato: **criar** `Contact` (via `ContactRepository.save` + `CompanyQuotaService`) e vincular `conversation.contactId`.
3. **[Opcional/PROPOSTA]** Criar serviço/acao para gerar `Lead` com `source=WHATSAPP` a partir de inbound — decisão de negócio (todo inbound vira lead? só com critério?).

**Arquivos:** `ContactService`/`ContactRepository`, `WhatsAppWebhookService.java` (~linhas 128–131), `LeadRepository`/`LeadSource`, nova migration (phone unique).

**Critério de aceite:** uma mensagem de um número desconhecido cria o contato (sem duplicar por email/phone) e a conversa fica com `contactId` preenchido.

---

### B2. [PROPOSTA] Suporte a mídia (imagem/áudio/vídeo/documento)

**Contexto:** `MessageType` só tem `TEXT`; `omnichannel_messages.type` VARCHAR(20); parser só lê `text.body`. WhatsApp manda mídia com `image/audio/video/document` (com URL temporária + `mime_type`/`sha256`).

**Tarefas:**
1. **Migration:** ampliar `omnichannel_messages` com `type` novos suportados e vínculo opcional para `storage_objects` (coluna `attachment_storage_id UUID NULL REFERENCES storage_objects(id)`).
2. **Domínio/DTO:** expandir `MessageType` (IMAGE/AUDIO/VIDEO/DOCUMENT/STICKER) e `Message`/`MessageResponse` para carregar referência de mídia.
3. **Parser:** em `WhatsAppCloudApiWebhookParser`, além de `text.body`, interpretar blocos `image/audio/video/document/sticker` (URL, mime, caption).
4. **Download/armazenamento:** baixar o binário (via URL temporária do Meta) e salvar em `storage_objects` via `StorageService`, guardando a referência na mensagem. Cotas de `max_storage_mb` já são aplicadas pelo `StorageService`.

**Critério de aceite:** mensagem de imagem recebida fica persistida com tipo IMAGE e mídia acessível; envio de mídia pode ser suportado (novos `SendRequest`/provider).

---

### B3. [PROPOSTA] Notificação e realtime de novas mensagens no frontend

**Contexto:** Infra STOMP pronta (`WebSocketConfig`, `StompAuthChannelInterceptor`, `StompNotificationPusher`), `NotificationType.MESSAGE` existe, mas inbox **não é realtime**; `socket.io-client` está no `package.json` sem uso.

**Tarefas:**
1. **Backend:** ao criar mensagem inbound (ou ao `create` de conversa/mensagem), publicar evento de inbox → `StompNotificationPusher` para `/user/{userId}/queue/notifications` (e/ou destination dedicado `/topic/company/{cid}/inbox`). Vincular `user_id` via membros da empresa (membros ativos da company) que possuem `omnichannel:read`.
2. **Frontend:** consumir o canal STOMP. **Atenção:** o backend é STOMP/SockJS — `socket.io-client` (presente no `package.json`) é **incompatível**; usar `@stomp/stompjs` (ou manter react-query + `refetchInterval` curto no `useConversations` como fallback enquanto não há socket).

**Arquivos:** `infrastructure/websocket/config/WebSocketConfig.java`, `StompNotificationPusher.java`, `WhatsAppWebhookService`, `src/features/omnichannel/hooks/useOmnichannel.ts`, `src/app/(dashboard)/inbox/page.tsx`.

**Critério de aceite:** nova mensagem aparece no inbox do agente com badge/unread sem refresh manual (via STOMP push), com fallback de polling.

---

### B4. [PROPOSTA] Corrigir débitos menores (N+1, bounds de paginação)

**Contexto (`sprints/16/REPORT.md`):**
- `listConversations` faz N+1 ao buscar última mensagem por conversa → **batch/fetch join**.
- Sem bounds de `page/pageSize` → validar (`page>=0`, `pageSize` limitado).

**Critério de aceite:** listar 1000 conversas = 1 query de última-mensagens (sem N+1); entrada inválida de paginação rejeitada.

---

## FASE C — Automatização / valor (médio)

### C1. [PROPOSTA] Ação de workflow `SEND_WHATSAPP_MESSAGE` (resposta/adiamento/template)

**Contexto:** Cadeia evento→listener→executor→ações existe e é genérica; mas nenhuma ação específica de WhatsApp; nenhum template `WHATSAPP_MESSAGE_RECEIVED` seedado.

**Tarefas:**
1. Nova ação em `WorkflowActionRunner`: `SEND_WHATSAPP_MESSAGE` (enviar template/mensagem para o `whatsapp.from` com campos `whatsapp.*` do contexto).
2. `WorkflowTemplateSeeder`: seed opcional com trigger `WHATSAPP_MESSAGE_RECEIVED` para demo.
3. Teste: criar workflow com esse trigger e ação, disparar `WorkflowTriggerEvent.whatsAppMessageReceived(...)`, verificar envio/mensagem criada.

**Critério de aceite:** mensagem inbound → regra de workflow → resposta automática via provider (fake em teste; cloud em prod).

---

### C2. [PROPOSTA] Auto-reply via IA

**Contexto:** módulo AI maduro (assistente, sugestões, tools). Inbox já usa `useSuggestReply`.

**Tarefas:** ponte inbound `omnichannel_message` → `AiAssistantService`/`AiChatProvider` (preenchimento de contexto da conversa) para gerar/sugerir resposta; botão "Responder com sugestão" já existe na UI.

**Critério de aceite:** o agente recebe sugestão de resposta gerada por IA inline na thread do inbound.

---

### C3. [PROPOSTA] Campanha via WhatsApp

**Contexto:** `campaign_channels` já liga campanha → `omnichannel_channels`; templates existem (V055). Verificar se o disparo já usa `WhatsAppProvider`.

**Tarefas:**
1. Auditar o disparo de campanha: se ainda não usa `WhatsAppProvider`, plugar (enviar template por mensagem/execução).
2. Respeitar janela de 24h/regras da Meta (WA-001..007 de `docs/04-integrations/WhatsApp.md`) e idempotência `campaign_message_events`.

**Critério de aceite:** campanha lança mensagens via canal WhatsApp com status por destinatário persistido.

---

## Decisões abertas / não decididas (para alinhar com o negócio)

- **[PROPOSTA]** Todo inbound vira **Lead**? Só quando houver critério (ex.: não existe contato)?
- **[PROPOSTA]** Quem envia mídia: suportar envio de arquivo pelo agente (upload) já na Fase B?
- **[PROPOSTA]** Cadastro do canal no Meta: via **QR/OAuth** (feito no frontend) ou via **API/boilerplate** (config `externalId`/`secretsRef` por formulário, como hoje)?
- **[PROPOSTA]** Fila assíncrona (Rabbit) para desacoplar webhook→envio/status: **adiar** (hoje in-process é suficiente para MVP) ou já incluir do plano?

---

## Riscos / pontos a validar (NÃO CONFIRMADO) — abrir antes de ir a produção

1. **Rota/prefixo do webhook** (`context-path` + `@RequestMapping` + nginx + gateway) — validar com teste real (A2).
2. **Mismatch provider** (canal `CLOUD_API` ↔ runtime `Fake`) — resolver em A1.
3. **Normalização E.164** e índice único de phone (B1).
4. **Custo/quota** de mídia no Postgres (`storage_objects`) para arquivos grandes (B2) — avaliar gate de tamanho.
5. **Regras da plataforma Meta** (janela 24h, templates aprovados, rate limits) — seguir `WhatsApp.md` WA-001..007 (C1/C3).

---

## Ordem sugerida de execução (sequência recomendada)

```
A1 → A2 → A3 → A4   (liga prod + fecha maior lacuna de teste)
A5 (segurança)  ──  gate de qualidade antes de expor publicamente
B1 → B3 → B2        (contato/lead, realtime, mídia — dependências frontend)
B4 (débitos)  ──     paralelo, baixo risco
C1 → C2 → C3        (valor: automação, IA, campanha)
```

**Gate de qualidade (todas as fases):** `mvn test` no backend (531+novos verdes), build frontend (`npm run lint`/`build`), CI+CD verdes, smoke na VPS (handshake webhook 200, envio real 1 mensagem de teste, recebimento 200).
