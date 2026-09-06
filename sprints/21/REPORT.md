# Sprint 21 — IA: AgentConfig Administrativo + Human Takeover

> **Status: 🚧 IMPLEMENTADA E VALIDADA LOCALMENTE — sem commit/push/deploy (por instrução).**
> Este é o "Sprint 3" da sequência de IA (Sprint 20 = IA base; Sprint 21 = próxima na numeração canônica).
> Código e testes concluídos em 2026-09-06; commit/deploy/VPS pendentes de instrução.

## Escopo

Duas entregas do módulo de IA/Omnichannel sobre a base já existente (Sprint 16 WhatsApp + Sprint 20 IA):

- **A) AgentConfig Administrativo** — administrador da empresa configura o agente de IA autônomo
  (GET/PUT `agent_config` por empresa) com autorização dedicada `ai:agent-config`.
- **B) Human Takeover** — atendente assume/libera uma conversa, suspendendo a IA autônoma
  (modo `HUMAN` persistido na conversa). Ao liberar, a IA autônoma retoma.

## Migrações

### V072__agent_config_admin_and_human_takeover.sql

- **Permissões novas** (catálogo, `ON CONFLICT (name) DO NOTHING`):
  - `ai:agent-config` (`ai` / `agent-config` / `manage`);
  - `omnichannel:takeover` (`omnichannel`/`conversation`/`takeover`).
- **Grants por empresa existente** via DO block com `app.current_company_id` (RLS exige tenant no
  INSERT de `role_permissions`, mesmo padrão da V049/V052/V053):
  - SUPER_ADMIN e ADMIN → `ai:agent-config` + `omnichannel:takeover`;
  - MANAGER e AGENT → `omnichannel:takeover`;
  - VIEWER permanece somente-leitura.
- **Estado da conversa**: coluna `handoff_mode VARCHAR(20) NOT NULL DEFAULT 'AUTOMATIC'`
  + CHECK `IN ('AUTOMATIC','HUMAN')` em `omnichannel_conversations`.
  `DEFAULT 'AUTOMATIC'` preserva o comportamento atual de todas as conversas.

> Empresas novas são cobertas no startup pelo `RoleSeedService` (V052/V053); o loop da V072 cobre
> as empresas já existentes no momento da aplicação.

## Backend

### A) AgentConfig Administrativo

- `domain/ai/AgentConfig.withSettings(...)` — preserva `id`/`companyId`/`createdAt`, atualiza
  `updatedAt` (insumo para upsert sem recriar entidade).
- `application/ai/dto/AgentConfigRequest` (`@Valid`) e `AgentConfigResponse` — contratos do admin.
- `application/ai/port/input/AgentConfigUseCase` + `application/ai/service/AgentConfigAdminService`
  — usa o `AgentConfigRepository` existente (V070/V071, sem duplicar camada):
  - **Safe defaults**: ausência de linha é estado válido (agente desabilitado). GET retorna
    default de fábrica com `id = null` (`aiEnabled=false`, `allowAutoReply=false`, `systemPrompt/model/
    temperature/maxTokens/updatedAt` nulos, `cooldownMinutes=60`, `maxChars=1000`);
  - **PUT = upsert**: cria a linha se não existir, senão `withSettings(...)`; nunca lança por ausência;
  - **Tenant**: `TenantContext.setCompanyId(companyId)` a partir do usuário autenticado (RLS FORCE),
    limpo em `finally`;
  - **Auditoria**: `TenantAuditRecorder` com `AuditAction.CREATE/UPDATE` e `AuditModule.AI`
    (AuditModule ganhou a constante `AI`); falha de auditoria é `log.warn`, não derruba a operação.
- `presentation/rest/ai/AiAgentConfigController` — `GET/PUT /api/v1/ai/agent-config`:
  - `@PreAuthorize("hasAuthority('ai:agent-config')")`;
  - empresa ativa sempre resolvida do `@AuthenticationPrincipal CurrentUser` (nunca do payload);
  - sem empresa ativa → `CrmAccessDeniedException` (403 `CRM_ACCESS_DENIED`).

### B) Human Takeover

- `domain/omnichannel/ConversationMode {AUTOMATIC, HUMAN}`;
  `Conversation.create()` → `AUTOMATIC`; novos `takeover()`/`releaseAutomation()`/`isInHumanMode()`/`getMode()`.
- `OmnichannelConversationJpaEntity` + `OmnichannelConversationRepositoryImpl` — campo `handoff_mode`
  mapeado (`NOT NULL DB`); `toDomain` usa sobrecarga `reconstitute` de 11 argumentos; a sobrecarga de
  10 argumentos delega com `AUTOMATIC` (preserva call sites existentes).
- `ConversationResponse` / `ConversationDetailResponse` — campo `mode`.
- `OmnichannelInboxUseCase` + `OmnichannelInboxService` — `takeover(companyId, conversationId)` e
  `release(...)`, **idempotentes** (assumir assumido / liberar liberado são no-ops), com
  `TenantAuditRecorder` (`AuditModule.OMNICHANNEL`, `AuditAction.CREATE/UPDATE`) e `log.warn` em
  falha de auditoria; devolvem a `ConversationResponse` atualizada.
- `OmnichannelInboxController` — `POST /api/v1/omnichannel/inbox/{conversationId}/takeover` e
  `.../release`, ambos `@PreAuthorize("hasAuthority('omnichannel:takeover')")`.
- `WhatsAppInboundAutoReplyProcessor` — **gate HUMAN** logo após a validação de ownership da
  conversa e antes de cooldown/reserva/LLM/envio: `if (conversation.isInHumanMode()) { log.debug; return; }`.
  A IA autônoma fica suspensa apenas naquela conversa, sem tocar nas demais nem no global.

### Permissões (`RoleSeedService`)

| Permissão | SUPER_ADMIN | ADMIN | MANAGER | AGENT | VIEWER |
|---|---|---|---|---|---|
| `ai:agent-config` | ✅ (`*`) | ✅ | — | — | — |
| `omnichannel:takeover` | ✅ | ✅ | ✅ | ✅ | — |

## Frontend

### A) AgentConfig Administrativo

- `ai.types.ts` — `AgentConfig`/`AgentConfigRequest`;
- `ai.service.ts` — `AiService.getAgentConfig()` / `updateAgentConfig()`;
- `hooks/useAi.ts` — `useAgentConfig` (queryKey `["ai","agent-config"]`), `useUpdateAgentConfig`
  (toast "Configuração do agente de IA salva.") e `canManageAgentConfig` em `useAiPermissions`;
- `constants.ts` — `ROUTES.SETTINGS_AGENT_CONFIG = "/settings/agent-config"`;
- `Sidebar.tsx` — item "Agente de IA" gated por `ai:agent-config`;
- nova página `app/(dashboard)/settings/agent-config/page.tsx` — formulário com switches
  (IA habilitada / auto-resposta), system prompt, modelo/temperatura/maxTokens,
  cooldown e maxChars.

### B) Human Takeover

- `omnichannel.types.ts` — `ConversationMode`, `mode` em `Conversation`/`ConversationDetail`,
  `CONVERSATION_MODE_LABELS` (AUTOMATIC: "IA autônoma" / HUMAN: "Atendimento humano");
- `omnichannel.service.ts` — `takeover(companyId, conversationId)` / `release(...)`;
- `hooks/useOmnichannel.ts` — `useTakeoverConversation` (toast "Você assumiu a conversa. A IA
  autônoma foi suspensa."), `useReleaseConversation` e `canTakeover` em `useOmnichannelPermissions`;
- `ChatThread.tsx` — badge "Atendimento humano" (destructive) quando `HUMAN`; botão
  "Assumir manualmente" / "Retomar IA" (ícones `UserCheck`/`Bot`, `Loader2` enquanto pende),
  visível conforme `canTakeover`.

## Testes

### Backend — **648 PASS** (era 620 na linha de base do Sprint 2; **+28**), 94 suítes, 0 falhas/erros

- `domain/omnichannel/ConversationTest` — modo default, takeover/release idempotentes,
  `reconstitute` 11 args restaura HUMAN e 10 args default AUTOMATIC;
- `domain/ai/AgentConfigTest` — `withSettings(...)`;
- `application/ai/service/AgentConfigAdminServiceTest` — safe defaults, upsert create/update,
  auditoria CREATE/UPDATE, falha de auditoria não derruba a operação;
- `application/omnichannel/service/OmnichannelInboxServiceTest` — takeover/release + auditoria;
- `application/omnichannel/service/WhatsAppInboundAutoReplyProcessorTest` — modo HUMAN não
  responde; liberada, responde normalmente;
- `presentation/rest/ai/AiAgentConfigControllerTest` — GET default, PUT, 400 validation, 403 sem
  empresa ativa;
- `presentation/rest/omnichannel/OmnichannelInboxControllerTest` — list/detail/takeover/release
  (detail default `pageSize=30`);
- `infrastructure/omnichannel/persistence/OmnichannelIsolationIT` — +4 testes de isolamento RLS
  (default AUTOMATIC, takeover persiste HUMAN na própria empresa, cross-tenant bloqueado com 0
  linhas, `handoff_mode` inválido rejeitado pelo CHECK).

> **Nota**: ITs `*IT.java` (Testcontainers) rodam na fase `verify` e exigem Docker — indisponível
> no ambiente local; foram compilados e ficam para CI/VPS. `mvn test` (surefire) não executa `*IT.java`.

### Frontend — **36 arquivos / 246 testes PASS** (era 234; **+12**)

- `ai.service.test.ts` (+3: get/update agent config);
- `useAi.test.ts` (+5: `canManageAgentConfig`, `useAgentConfig` enabled/disabled, `useUpdateAgentConfig`);
- `ChatThread.test.tsx` (novo, 4 testes: badge HUMAN, toggle assumir/retomar);

## Validação

- `mvnw.cmd compile test-compile` ✅ / `mvnw.cmd test` ✅ **BUILD SUCCESS** (648 testes);
- typecheck ✅ (`tsc --noEmit`);
- lint ✅ (exit 0, apenas warnings preexistentes);
- vitest ✅ 246/246;
- build de produção: não executado nesta Sprint (sem deploy).

## Débitos conhecidos

- **Commit/push/deploy pendente** — por instrução, Sprint 21 NÃO commita; working tree contém as
  alterações acima (ver `git status --short`).
- **ITs Testcontainers** (`OmnichannelIsolationIT` ampliado) — dependem de Docker/CI.
- **E2E autenticado manual** no browser — herdado das Sprints anteriores (sem credenciais de teste).
- **Verificação VPS** — pendente junto do deploy (aplicação de `V072`, endpoints de admin/takeover).
- Reuso de `omnichannel:takeover` para o `release` (decisão de least privilege: mesmo poder de
  operação de handoff para frente e para trás).

## Arquivos Principais

- Migração: `backend/src/main/resources/db/migration/V072__agent_config_admin_and_human_takeover.sql`
- Backend: `domain/ai/AgentConfig`, `domain/omnichannel/Conversation` + `ConversationMode`,
  `domain/audit/AuditModule.AI`, `application/ai/{dto,port/input,service/AgentConfigAdminService}`,
  `presentation/rest/ai/AiAgentConfigController`, `application/omnichannel/...{OmnichannelInboxUseCase,
  OmnichannelInboxService, WhatsAppInboundAutoReplyProcessor}`,
  `presentation/rest/omnichannel/OmnichannelInboxController`,
  `infrastructure/omnichannel/persistence/{OmnichannelConversationJpaEntity,OmnichannelConversationRepositoryImpl}`,
  `infrastructure/identity/persistence/RoleSeedService`
- Frontend: `features/ai/{types,services/hooks,page settings/agent-config}`,
  `features/omnichannel/{types,services,hooks,components/ChatThread}`,
  `components/layout/Sidebar`, `lib/constants`