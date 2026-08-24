# SPRINT 17 — CAMPANHAS — PLANEJAMENTO

> Status: ⏳ PLANEJADA (não implementada)
> Base: commit `bb10b8e` (LOCAL == origin/main, CI/CD GREEN)
> Este documento é SOMENTE planejamento. Nenhum código, migration ou endpoint foi criado nesta etapa.

---

## 1. OBJETIVO

Transformar Campanhas em uma capacidade real do CRM SaaS Omnichannel:

- CRUD completo de campanhas com ciclo de vida controlado;
- definição de público (segmentação simples, arquitetura extensível);
- seleção de canal via abstração sobre o Omnichannel existente;
- templates de mensagem (módulo novo, base para WhatsApp e IA futura);
- agendamento e execução controlada (sem invadir o escopo da Sprint 18);
- eventos de execução que viabilizem Analytics na Sprint 19;
- tenant isolation total (company_id + RLS), herdando os aprendizados da Sprint 16.

---

## 2. CONTEXTO TÉCNICO (estado real em `bb10b8e`)

### Backend (Spring Boot 3.5.16, hexagonal, `com.becommerce.crm`)

- Arquitetura: `application/` (use-cases + ports + DTOs), `domain/`, `infrastructure/`, `presentation/rest`, `shared/`.
- Multi-tenancy: `company_id UUID NOT NULL REFERENCES companies(id)` em todas as tabelas; RLS Postgres (`FORCE ROW LEVEL SECURITY`, V019–V027); app-side `TenantContext` / `TenantFilter` / `TenantAwareDataSource`. Testes de isolamento já existentes: `TenantContextTest`, `TenantFilterTest`, `TenantIsolationConcurrencyIT`.
- Permissões: padrão `<recurso>:<acao>` (ex.: `contact:create`) via `@PreAuthorize`; seed em migrations RBAC.
- Auditoria: AOP `@Auditable(action, module, entityId...)` → `AuditLogAspect` → tabela `audit_log`. **Reutilizar; não criar infra paralela.**
- Messaging: RabbitMQ no classpath, mas apenas `SpringEventPublisher`; sem consumers reais. Async config e `WorkflowSchedulingConfig` disponíveis.
- Notificações: `Notification` + STOMP push (`StompNotificationPusher`) — reutilizar para feedback interno (ex.: campanha concluída).
- Migrations: `backend/src/main/resources/db/migration/`, convenção `Vnnn__snake_case.sql`. **Última = V054. Próxima disponível = V055.**
- Pacotes `campaign` e `communication` existem como scaffolding vazio. **Módulo Templates NÃO existe** — precisará ser criado.

### Frontend (Next.js 14 App Router, React 18, TS)

- Tailwind + Radix/shadcn-style (`src/components/ui/*`), react-hook-form + zod, TanStack Query v5, sonner, recharts.
- Padrões: listas server-side paginadas (padrão Leads: `{content,totalElements,totalPages}`, filtros resetam página), detail pages `[id]` (padrão Customer 360).
- `ROUTES.CAMPAIGNS = "/campaigns"` **já existe** (`src/lib/constants.ts:26`); breadcrumb "Campanhas" existe; resource label RBAC "Campanhas" existe; `Lead.campaignId` já referencia campanhas conceitualmente.
- Permissões UI: `useAuthorization().can()` + hooks por módulo (`useContactPermissions()` como modelo).

### Débitos existentes — impacto em Campanhas

| Débito | Impacto |
|---|---|
| N+1 em `listConversations` | Sem impacto direto; evitar N+1 equivalente em listagem de campanhas desde o início (fetch batch de contadores). |
| Rate limit webhook | Impacta entrega WhatsApp de alto volume → limitar throughput do dispatcher. |
| E-mail real (não implementado) | Canal e-mail na Sprint 17 deve ser simulado/abstrato ou marcado como SHOULD HAVE dependente deste débito. |
| Pagination bounds | Aplicar bounds nos novos endpoints. |
| WebSocket inbox / polling notificações | Sem bloqueio; usar mecanismo existente. |

---

## 3. ARQUITETURA PROPOSTA

```text
Campaign (aggregate root)
 ├── CampaignAudience  (público: tipo de segmento + critérios JSONB)
 ├── CampaignChannel   (canal + template + provider config)
 └── CampaignExecution (execução/dispatch de uma campanha)
       └── CampaignMessageEvent (1 linha por destinatário x evento)
```

- **Campanha ≠ mensagem individual.** A campanha define público + canal/template + agendamento. A execução materializa destinatários em eventos.
- Abstração de canal: `CampaignChannelDispatcher` (port) com implementações por provider reaproveitando adapters do Omnichannel (WhatsApp primeiro). Campanha nunca fala direto com provider.
- Templates: módulo novo `template` reutilizável por campanhas e por automações (Sprint 18).

---

## 4. CICLO DE VIDA DA CAMPANHA

Estados propostos:

```text
DRAFT → SCHEDULED → RUNNING ⇄ PAUSED → COMPLETED
                        ↓
                    CANCELLED
DRAFT → CANCELLED
```

| Transição | Quem | Regras |
|---|---|---|
| DRAFT→SCHEDULED | `campaign:execute` | Público ≥ 1 destinatário válido; canal ativo; template válido p/ canal; data futura (se agendado). |
| SCHEDULED→RUNNING | scheduler OU usuário (`campaign:execute`) | Idempotente; só se `scheduled_at <= now` (scheduler) ou disparo manual. |
| RUNNING⇄PAUSED | `campaign:update`/`execute` | Pausa interrompe dispatch novo; mensagens já despachadas seguem. |
| PAUSED→RUNNING | idem | Retomada do ponto onde parou (cursor por evento pendente). |
| RUNNING/SCHEDULED→CANCELLED | `campaign:delete`(ou execute) | Cancela pendentes; enviados permanecem; auditoria obrigatória. |
| *→COMPLETED | sistema | Todos destinatários processados (sucesso/falha final). |

- Edição de conteúdo/público: permitida apenas em DRAFT (e PAUSED com nova validação + re-auditoria).
- Cancelamento é soft-state (status), não delete físico. Delete físico apenas em DRAFT/CANCELLED.
- Conclusão dispara `Notification` interna ao criador.

---

## 5. SEGMENTAÇÃO

**Sprint 17 (simples):**
- Tipo de público: `CONTACTS`, `LEADS`;
- Critérios: status, tags, origem, empresa, presença em pipeline (leads), faixa básica;
- Armazenar critérios em JSONB (`criteria`) + snapshot de contagem no momento do schedule (`estimated_recipients`).

**Arquitetura extensível:** campo `audience_type` enum + `criteria JSONB` versionado por um `AudienceResolverStrategy` (port). Segmentação composta/comportamental/scoring/IA = novas strategies sem mudar schema. **Segmentação avançada fica para FUTURE.**

---

## 6. CANAIS

```text
Campaign → CampaignChannel (channel_type, provider_channel_id FK omnichannel.channel)
         → CampaignChannelDispatcher (port, application/port/output)
             ├── WhatsAppDispatcher  (usa adapter WhatsApp da Sprint 16, respeita rate limit)
             └── EmailDispatcher     (stub/log até débito "e-mail real" ser resolvido)
```

- Sprint 17 MUST: WhatsApp (via infra Omnichannel existente). SHOULD: e-mail stub estruturado. FUTURE: SMS, push, multicanal por campanha (vários CampaignChannel).

---

## 7. TEMPLATES

Módulo novo (não duplicar nada — hoje não existe):

- Entidade `MessageTemplate`: name, channel_type, subject, body, variables (JSONB/lista), status (`ACTIVE/ARCHIVED`), version (int), company_id, timestamps.
- Variáveis: sintaxe `{{nome}}`, validadas contra campos do contato/lead no render; render failure = evento `FAILED` com motivo.
- Versionamento: novas edições incrementam versão; campanha referencia `template_id` + fixa a versão usada na execução (snapshot em `CampaignExecution.template_snapshot`).
- WhatsApp: alinhar com templates aprovados Meta quando aplicável (campo `external_template_id` opcional). Integração com IA para geração de conteúdo = FUTURE.

---

## 8. AGENDAMENTO E EXECUÇÃO

- Execução imediata (manual) e agendada (`scheduled_at` com timezone da empresa — armazenar UTC + tz).
- Scheduler: Spring `@Scheduled` (polling de campanhas SCHEDULED vencidas) usando a async config existente. **Sem motor de automação.**
- Dispatch: fila RabbitMQ (`crm.campaign.dispatch`) com consumer idempotente — ou, se RabbitMQ ainda estiver inativo no projeto, fallback: dispatch em lote assíncrono com cursor persistido em `CampaignExecution` (decisão final na implementação, preferindo RabbitMQ pois já está no classpath).
- Idempotência: `CampaignMessageEvent` único por `(execution_id, recipient_id)`; retries com contador + backoff; falha definitiva após N tentativas → evento `FAILED`.
- Limite Sprint 17: **campanha = comunicação única pontual (ou imediata ou agendada)**. Fluxos recorrentes, triggers por evento, drip/jornadas, multi-etapas → **Sprint 18 (Automações Omnichannel)**, que consumirá o dispatcher e os templates aqui construídos.

---

## 9. MÉTRICAS / EVENTOS

`CampaignMessageEvent`: execution_id, recipient_id, recipient_type, status (`PENDING, SENT, DELIVERED, READ, FAILED, RESPONDED, OPTED_OUT`), error_reason, provider_message_id (link ao Message do Omnichannel), occurred_at.

Eventos mínimos gerados na Sprint 17: SENT, FAILED (+reason), DELIVERED/READ quando o provider reportar (webhook existente), OPTED_OUT. Contadores agregados na campanha/execução (recalculados por query, evitando N+1). Dashboards analíticos → Sprint 19.

---

## 10. MULTI-TENANCY E SEGURANÇA (OBRIGATÓRIO)

- Todas as tabelas novas com `company_id UUID NOT NULL REFERENCES companies(id)` + índice.
- RLS `FORCE ROW LEVEL SECURITY` nas tabelas de campanha/template/eventos (padrão V019–V027).
- Todo use-case valida ownership via `TenantContext`; repos filtram por company_id.
- Dispatcher/execução assíncrona: propagar company_id na mensagem da fila e restaurar TenantContext no consumer (crítico — mesmo risco aprendido na Sprint 16).
- Endpoints de métricas/detalhes também isolados por tenant.
- Testes de isolamento obrigatórios: Tenant A cria campanha → Tenant B não lista, não lê, não executa, não vê eventos/métricas; IT de concorrência seguindo `TenantIsolationConcurrencyIT`.

---

## 11. PERMISSÕES (propostas, coerentes com padrão `<recurso>:<acao>`)

```text
campaign:read
campaign:create
campaign:update
campaign:delete
campaign:execute      (agendar, pausar, retomar, cancelar)
campaign:view_metrics
template:read / template:create / template:update / template:delete
```

Seed em migration RBAC seguindo padrão V052/V053. Frontend: hook `useCampaignPermissions()` + item de sidebar com `permission`.

---

## 12. AUDITORIA

Via `@Auditable` existente (module="campaign"): CREATED, UPDATED, SCHEDULED, EXECUTION_STARTED, PAUSED, RESUMED, CANCELLED, COMPLETED, AUDIENCE_CHANGED, CHANNEL_CHANGED, TEMPLATE_CHANGED. Nenhuma infra paralela.

---

## 13. BANCO DE DADOS (planejado; migrations a partir de V055)

| Migration | Conteúdo |
|---|---|
| V055 | `message_templates` (id PK, company_id FK, name, channel_type, subject, body, variables jsonb, status, version, external_template_id, timestamps; idx(company_id), idx(company_id, channel_type)) |
| V056 | `campaigns` (id PK, company_id FK, name, description, status enum, audience_type, criteria jsonb, estimated_recipients, scheduled_at timestamptz, timezone, started_at, completed_at, created_by FK users, timestamps; idx(company_id,status), idx(company_id,scheduled_at)) |
| V057 | `campaign_channels` (id, campaign_id FK CASCADE, company_id, channel_type, provider_channel_id FK omnichannel channels, template_id FK message_templates, template_version, config jsonb) |
| V058 | `campaign_executions` (id, campaign_id FK, company_id, status, template_snapshot text/jsonb, total_recipients, processed_count, failed_count, cursor, started_at, finished_at) |
| V059 | `campaign_message_events` (id, execution_id FK, campaign_id, company_id, recipient_id, recipient_type, status, error_reason, provider_message_id, attempts, occurred_at; unique(execution_id, recipient_id); idx(company_id,campaign_id,status)) |
| V060 | Seed permissões campaign:* e template:* + RLS policies |

Constraints: UNIQUE(campaign, company), CHECK de transição tratado em service (não em SQL). Paginação/filtros/ordenação: Pageable padrão do projeto + filtros status/canal/período/busca por nome.

---

## 14. BACKEND — COMPONENTES

- Domain: `Campaign`, `CampaignStatus`, `AudienceType`, `CampaignChannel`, `CampaignExecution`, `MessageTemplate` + exceptions (`CampaignNotFoundException`, `InvalidCampaignTransitionException`...).
- Application: ports in (`CreateCampaignUseCase`, `ScheduleCampaignUseCase`, `ExecuteCampaignUseCase`, `PauseCampaignUseCase`, `CancelCampaignUseCase`...), port out (`CampaignRepository`, `TemplateRepository`, `AudienceResolver`, `CampaignChannelDispatcher`), strategies de audience, DTOs MapStruct.
- Infrastructure: persistence JPA, dispatcher WhatsApp, scheduler, RabbitMQ producer/consumer com TenantContext.
- Presentation: `CampaignController`, `TemplateController` (REST records request/response, GlobalExceptionHandler reutilizado, @PreAuthorize, pagination bounds).

---

## 15. FRONTEND

Rotas (App Router):
- `/campaigns` — lista: tabela (nome, status badge, canal, período, destinatários, progresso), filtros status/canal/período, busca, paginação padrão Leads, empty state com CTA, confirmações em ações destrutivas (cancelar/excluir).
- `/campaigns/new` — wizard em etapas: Informações → Público → Canal → Mensagem/Template → Agendamento → Revisão → Confirmar. React-hook-form + zod por passo.
- `/campaigns/[id]` — Campaign 360: resumo, público, canal/template, agendamento, progresso em tempo aproximado (polling), erros, timeline/auditoria. Métricas avançadas/gráficos → Sprint 19.

UX: skeletons/loading, toasts sonner, badges de status consistentes, `useCampaignPermissions()` gate em ações, responsivo, acessibilidade Radix. Sem novas libs.

---

## 16. TESTES

- **Unit:** state machine de campanha, audience resolver, render de template/variáveis, cálculo de agendamento/timezone.
- **Integration:** CRUD endpoints + paginação + permissões (@PreAuthorize) + validation errors.
- **Tenant isolation (obrigatório):** A cria/B tenta ler/listar/executar/ver eventos → negado; IT de concorrência; consumer de fila restaura contexto correto.
- **Execution:** agendamento→execução, pausa/retoma, cancelamento, falha+retry, idempotência (duplicidade de dispatch não gera evento duplo).
- **Frontend:** lint, typecheck, build, testes vitest existentes, smoke dos fluxos wizard/lista/detalhe.

---

## 17. CI/CD E VPS (fluxo oficial)

Implementação → Testes → Commit → Push origin/main → CI GREEN → GHCR → CD → Deploy VPS → Testes na VPS (`ssh crm-vps`, `/opt/crm`) → Smoke tests → Validação final. Docker local apenas para debug. Migrações V055+ aplicadas pelo deploy; verificar health check e criação/execução de uma campanha de teste no tenant de validação.

---

## 18. ESCOPO

### MUST HAVE
- CRUD de campanhas + ciclo de vida completo (estados/transições/auditoria);
- Segmentação simples (contatos/leads por status/tags/origem/empresa);
- Canal WhatsApp via abstração de dispatcher sobre Omnichannel;
- Módulo de templates com variáveis e versionamento;
- Agendamento (timezone-aware) + execução manual imediata, idempotente, com pausa/cancelamento;
- Eventos por destinatário (base de métricas) e contadores básicos;
- company_id + RLS + permissões + testes de tenant isolation;
- Frontend: lista + wizard de criação + detalhes básicos;
- Migrations V055+, testes, CI/CD GREEN, deploy e validação na VPS, documentação.

### SHOULD HAVE
- Canal e-mail (mesmo que stub estruturado);
- Retry com backoff configurável;
- Progresso em tempo quase real no detalhe;
- Export CSV de destinatários/erros.

### FUTURE (fora da Sprint 17)
- **Sprint 18:** Automações Omnichannel (fluxos, triggers, drip, multi-etapas, recorrência) — consumirá dispatcher/templates.
- **Sprint 19:** Analytics/dashboard avançado de campanhas (conversões, funil, gráficos).
- Segmentação avançada (comportamental, scoring, IA), geração de conteúdo por IA, SMS/push, opt-out center, multicanal por campanha.

---

## 19. CRITÉRIOS DE ACEITE (verificáveis)

1. Usuário com `campaign:create` cria campanha DRAFT via API e UI; sem a permissão → 403.
2. Transição inválida (ex.: DRAFT→COMPLETED) → 422 com erro de domínio.
3. Agendamento futuro executa automaticamente no horário (tz correta); execução manual imediata funciona.
4. Execução produz 1 evento por destinatário; re-dispatch duplicado não duplica eventos (idempotência testada).
5. Pausa interrompe novos envios; retomada continua; cancelamento marca pendentes como cancelados.
6. Template com variável inválida → evento FAILED com motivo, campanha segue.
7. Tenant B não acessa (read/list/execute/metrics) campanha do Tenant A — coberto por testes automatizados GREEN.
8. RLS habilitado e FORÇADO nas 5 tabelas novas.
9. Auditoria registra todas as transições listadas na seção 12.
10. Frontend: lista filtra/pagina/busca; wizard completa fluxo; detalhe mostra progresso; lint/typecheck/build/testes GREEN.
11. CI GREEN, GHCR publicado, CD GREEN, deploy na crm-vps com containers UP/healthy, `/actuator/health` = UP, frontend HTTP 200, migrations aplicadas (V055+).
12. Smoke test na VPS: criar → agendar → executar campanha de teste em tenant de validação.
13. Documentação (SPRINT_INDEX, REPORT) atualizada.

---

## 20. ORDEM DE IMPLEMENTAÇÃO RECOMENDADA

1. Database: migrations V055–V060 + RLS
2. Domain: entidades, enums, state machine, exceptions
3. Repository/persistence + mappers
4. Security: permissions seed + @PreAuthorize + isolation
5. Services/use-cases: CRUD, audience resolver, templates
6. API REST: controllers + DTOs + error handling
7. Execution: dispatcher WhatsApp, scheduler, queue/consumer, idempotência
8. Testes (unit/integration/isolation/execution)
9. Frontend: services/hooks → lista → wizard → detalhes
10. Integração E2E local (debug)
11. Commit/push → CI/GHCR/CD
12. Validação na VPS + smoke test + documentação

---

## 21. RISCOS

| Risco | Mitigação |
|---|---|
| Consumer de fila sem TenantContext → vazamento cross-tenant | Propagar company_id na mensagem + testes de isolamento do consumer antes do merge. |
| Rate limit do provider WhatsApp em alto volume | Throughput limitado no dispatcher (throttling), alinhado ao débito conhecido. |
| RabbitMQ sem producers/consumers reais no projeto (só classpath) | Se integração mostrar-se arriscada, usar fallback batch+cursor persistido sem bloquear a sprint. |
| E-mail real não implementado (débito) | E-mail permanece stub SHOULD HAVE. |
| Escopo puxando para automações | Limite duro documentado na seção 18; automação = Sprint 18. |
| N+1 em contadores da lista | Contadores agregados por query em lote. |

## 22. DEFINITION OF DONE

Código + testes (incl. tenant isolation) + documentação + CI GREEN + CD GREEN + deploy + validação na VPS + smoke test. **VPS é o ambiente oficial de validação; Docker local não substitui.**
