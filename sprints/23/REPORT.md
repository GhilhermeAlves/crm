# Sprint 23 — Omnichannel · Pipeline Assíncrono (RabbitMQ)

> **Status: ✅ Concluída (2026-09-08).**
> Auditoria → implementação da fila assíncrona → CI/GREEN → CD + validação na VPS.

## Escopo

Sprint de Omnichannel/Arquitetura para tornar assíncrono o fluxo do WhatsApp (inbound → auto-reply →
outbound → provedor) e de Follow-up (scheduler → executor → outbound), trocando a injeção de
**RabbitMQ como 2ª infra** (única adição de infraestrutura) no lugar de chamadas retries síncronas.
Decisões de escopo registradas:

1. **RabbitMQ only** — nenhuma outra infra/introdução; nada de `RabbitTemplate`/`@RabbitListener`
   nas camadas de domínio/aplicação; consumidores são **infra fina** que delega para a aplicação.
2. **Nenhuma migration** — o pipeline reutiliza tabelas/índices/RLS de idempotência existentes
   (V044), sem tocar no schema.
3. **Outbox NÃO implementado** nesta etapa — risco documentado (§27) de gap entre commit no banco e
   publicação na fila; mitigado por idempotência de consumidores + retry/DLQ.
4. Fora de escopo (débitos/etapas anteriores): Inbox UI, mídia, Pipeline/Dashboard/WebSocket,
   troca de provedor, AgentConfig/Human Takeover, reescrita de `FollowUp...`, RLS.
5. Contrato UAZAPI V2 mantido (Sprint 22) — este sprint conecta o envio pela fila ao
   `UazapiWhatsAppProvider` já existente.

## Arquitetura (novo fluxo)

### Topologia RabbitMQ (declarada por `WhatsAppRabbitTopology`)

- **Exchange** `crm.whatsapp` (topic) — rotas `whatsapp.inbound`, `whatsapp.auto-ai`,
  `whatsapp.sender`, `followup.executor`; **DLX** `crm.whatsapp.dlx` (direct).
- **Queues primárias** (durable): `crm.whatsapp.inbound`, `crm.whatsapp.auto-ai`,
  `crm.whatsapp.sender`, `crm.followup.executor`.
- **DLQs**: `<q>.dlq` para cada primária, ligadas ao `crm.whatsapp.dlx` com routing key = nome da
  primária; `dead-letter-exchange` + `dead-letter-routing-key` por queue.
- **Retry/esgotamento**: retriers configurados nos listeners (max-attempts 3, backoff) +
  `default-requeue-rejected: false` → mensagem rejeitada após esgotar vai para a DLQ (filas mortas
  não têm consumer; permanecem para inspeção).
- **Consumidores** gated por `@ConditionalOnProperty(name="crm.messaging.consumers.enabled",
  matchIfMissing=true)`; 1 consumer por queue primária.

### Eventos (camada de aplicação — pacotes `application.omnichannel.event` / `application.followup.event`)

- `WhatsAppInboundReceivedEvent` (webhook → persistência) — rck `whatsapp.inbound`.
- `WhatsAppAutoReplyRequestedEvent` (persistência → auto-reply) — rck `whatsapp.auto-ai`.
- `WhatsAppOutboundSendRequestEvent` (auto-reply → envio) — rck `whatsapp.sender`.
- `FollowUpExecutionDueEvent` (scheduler → execução) — rck `followup.executor`.
- Cada evento carrega `companyId` (usado pelo consumidor para re-construir o `TenantContext`) e
  idempotência (ex.: `clientMessageId` no outbound = `messageId` da mensagem).

### Cadeia

- **Webhook** (HTTP, deve continuar rápido): persiste a mensagem recebida e publica
  `WhatsAppInboundReceivedEvent`; **não** dispara processamento no request.
- **`WhatsAppInboundConsumer`** → `WhatsAppInboundProcessor` (persiste conversa/mensagem no message
  broker com idempotência) → publica `WhatsAppAutoReplyRequestedEvent`.
- **`WhatsAppAutoAiConsumer`** → `WhatsAppInboundAutoReplyProcessor` (regras de HUMAN block /
  auto-reply; persiste PENDING ou nada quando bloqueado) → publica `WhatsAppOutboundSendRequestEvent`.
- **`WhatsAppSenderConsumer`** → `WhatsAppSendService`/envio pelo `WhatsAppProvider` (UAZAPI ativo) —
  idempotência por `clientMessageId` (unique V044), resultado persistido.
- **`FollowUpExecutorConsumer`** → `FollowUpExecutionService` com **claim atômico**
  (status PENDING → PROCESSING + `processing_started_at`), execução via o mesmo envio outbound e
  **outcome handler** (`FollowUpSendOutcomeHandler`): re-backs com backoff 15m → 1h → 4h,
  `MAX_ATTEMPTS = 3`, marca `FAILED`/`SENT`/`CANCELLED` (HUMAN_MODE, SUPERSEDED_BY_NEW_MESSAGE).

### Regras dos consumidores (padrão aplicado a todos)

- Re-construir tenant a partir de `event.companyId()` e **limpar no `finally`**;
- `companyId` ausente → **fail-safe**: log, sem retry;
- Erros **permanentes** → engolidos + logados (sem retry); **transitórios** → rethrown para retry;
- Após 3 tentativas → DLQ.

## Backend

- `RabbitTopics` — constantes de exchange/routing keys/queues/DLQ.
- `WhatsAppRabbitTopology` — beans de exchanges/queues/bindings/DLQ (declaradas no startup).
- `RabbitMessagingEventPublisher` — porta infra de publicação (omnichannel + followup) via
  `RabbitTemplate`, mapeando evento → (exchange, routing key).
- `RabbitConfig` — `CachingConnectionFactory` + `Jackson2JsonMessageConverter` +
  **`DefaultJackson2JavaTypeMapper` com `TRUSTED_PACKAGES` exatos**.

  > **Detalhe técnico de produção (bug real pego pela CI):**
  > `DefaultJackson2JavaTypeMapper.isTrustedPackage` compara o nome do pacote com os pacotes
  > confiáveis via **exact match (`String.equals`) — sem suporte a aspas/curinga** (`com.becommerce.crm`
  > e `com.becommerce.crm.*` **falham**). A constante final é:
  > `TRUSTED_PACKAGES = { "com.becommerce.crm.application.omnichannel.event",
  > "com.becommerce.crm.application.followup.event" }`.
  > **Novos pacotes de eventos precisam ser adicionados aqui.**

- Consumidores (infra fina): `WhatsAppInboundConsumer`, `WhatsAppAutoAiConsumer`,
  `WhatsAppSenderConsumer`, `FollowUpExecutorConsumer`.
- `application.yml` — bloco `spring.rabbitmq` (host/porta/username/vhost) + `listener.simple.retry`
  e configuração de consumidores.

## Testes

- **Suíte unitária completa offline**: `./mvnw test` → **BUILD SUCCESS, 0 falhas** (747 verdes +
  **`RabbitConfigTest`** novo — round-trip do conversor JSON sem broker, pina a regressão de
  trusted-packages).
- **Checkstyle**: `./mvnw checkstyle:check` → **0 violações**.
- **CI** (`./mvnw clean verify` + `checkstyle:check`, serviços postgres:16/redis:7/rabbitmq:3):
  **Backend CI GREEN** — incluindo **`RabbitMessagingFlowIT`** (Testcontainers RabbitMQ, 3 testes):
  topologia declarada num broker real; **round-trip JSON via o conversor do `RabbitConfig`**
  (foi este teste que quebrou na 1ª rodada pelo trusted-packages e validou a correção); e
  **rota de mensagem venenosa → DLQ**. Frontend CI, Auth-service CI e Docker Build **GREEN**.
  (Local não roda Testcontainers — Docker indisponível na máquina — então o IT é validado na CI.)

## CI/CD + Deploy + Validação VPS

- Commits em `main`: `d6955de` (feature pipeline assíncrono), `ade88ba` (1ª tentativa de trusted
  packages, `com.becommerce.crm.*` — corrigida após leitura do bytecode do Spring AMQP),
  `9e1406d` (**fix final**: pacotes exatos + `RabbitConfigTest`). Todos **pushed**.
- **CI e CD verdes em `9e1406d`**; CD build+push `ghcr.io/ghilhermealves/crm/*` e
  **deploy-staging auto-executado** — o container do backend na VPS foi recriado com a imagem
  construída a partir desse commit (image `...2951e616` criada 05:02:40Z; container recriado
  05:06:50Z; `Started CrmApplication in 24.6s`).
- **VPS (`crm-vps`, clock UTC)**:
  - `/actuator/health` → **200** (e endpoints `/auth`, frontend OK);
  - **RabbitMQ = `rabbitmq:4-management-alpine`** (broker RabbitMQ 4) já com a **topologia inteira
    declarada**: exchanges `crm.whatsapp` + `crm.whatsapp.dlx`; queues `crm.whatsapp.inbound`,
    `crm.whatsapp.auto-ai`, `crm.whatsapp.sender`, `crm.followup.executor` + `.dlq` de cada;
    bindings com routing keys `whatsapp.inbound`/`whatsapp.auto-ai`/`whatsapp.sender`/
    `followup.executor` e DLQ-binding via DLX;
  - **1 consumer por queue primária** (o backend) e uma única conexão Rabbit do app (172.28.0.3);
  - Classes do sprint confirmadas no `app.jar` em execução (grep binário no central-directory):
    `WhatsAppInboundConsumer`, `WhatsAppAutoAiConsumer`, `WhatsAppSenderConsumer`,
    `FollowUpExecutorConsumer`, `WhatsAppInboundProcessor`, `WhatsAppSendService`,
    `FollowUpExecutionService`, `FollowUpSendOutcomeHandler`;
  - Varredura de logs ≈5h de uptime: **0 ERROR/WARN** de Rabbit/messaging (apenas warnings
    benignos: Flyway outOfOrder, dialect Hibernate, Netty native);
  - Webhook: `GET` com **verify token errado → 401** (protegido, esperado);
  - Schema `followups` confirmado na VPS como referência do E2E: coluna de execução = **`execute_at`**,
    índice parcial `idx_followups_due` (`WHERE status='PENDING'`), chave única parcial de idempotência
    `(company_id, idempotency_key)`, RLS FORCE (`tenant_isolation_policy`).

## §27 Riscos residuais

1. **Sem Outbox**: publicação da fila acontece fora da transação de persistência → janela de gap
   DB-commit vs publish. Mitigação atual: idempotência nos consumidores (V044) + retry/DLQ.
2. **Corner do executor re-publish**: se o worker morre após o claim (PROCESSING) e antes do
   resultado, o scheduler pode recandidatar/republish o follow-up → reprocessamento. Mitigado por
   claim atômico e `clientMessageId` único no outbound.
3. **Trusted packages = exact match**: qualquer novo pacote de eventos precisa ser adicionado em
   `RabbitConfig.TRUSTED_PACKAGES` (sem wildcard) — protegido por `RabbitConfigTest`.
4. **DLQs sem consumidor**: mensagens mortas ficam retidas (inspeção manual) — não há NACK/crescimento
   silencioso de retries.

## Débitos conhecidos

- **E2E real do happy-path** (mensagem WhatsApp real via UAZAPI: inbound → auto-ai → sender →
  provedor; HUMAN block; follow-up scheduler → executor → sender) **não executado** — requer número
  de teste ativo da instância UAZAPI (validação manual pendente do usuário).
- **Frontend de follow-up / follow-up-sequences** (herdado da Sprint 22).
- **E2E autenticado manual** (browser) — herdado; requer credenciais de teste.

## Arquivos Principais

- Eventos: `backend/src/main/java/com/becommerce/crm/application/omnichannel/event/*`,
  `backend/src/main/java/com/becommerce/crm/application/followup/event/*`
- Serviços de aplicação: `application/omnichannel/service/{WhatsAppInboundProcessor,
  WhatsAppInboundAutoReplyProcessor,WhatsAppSendService}.java`,
  `application/followup/service/{FollowUpExecutionService,FollowUpSendOutcomeHandler}.java`
- Publicação: `backend/src/main/java/com/becommerce/crm/infrastructure/rabbit/
  {RabbitTopics,WhatsAppRabbitTopology,RabbitMessagingEventPublisher}.java`
- Config: `backend/src/main/java/com/becommerce/crm/infrastructure/rabbit/RabbitConfig.java`,
  `backend/src/main/resources/application.yml`
- Consumidores: `infrastructure/omnichannel/messaging/{WhatsAppInboundConsumer,
  WhatsAppAutoAiConsumer,WhatsAppSenderConsumer}.java`,
  `infrastructure/followup/messaging/FollowUpExecutorConsumer.java`
- Testes: `RabbitConfigTest`, `RabbitMessagingFlowIT` (CI), ajustes nos testes das suítes existing