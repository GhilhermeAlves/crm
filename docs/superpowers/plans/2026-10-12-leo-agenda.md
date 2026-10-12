# Léo — Fase 1: conhecer o CRM + Agenda (consultar, desmarcar, remarcar) — Plano de implementação

> Executor: siga as tasks em ordem, um commit por task, sem push. Worktree: `.worktrees/feat-leo-agenda`, branch `feat/leo-agenda` (base `crm-improvements-deploy-phase` @ 2ddcc521).

**Objetivo:** o assistente interno (Léo, chat do CRM) passa a (1) saber em que tela o usuário está e quais telas existem, (2) consultar a agenda e (3) **propor** desmarcar/remarcar consultas — a escrita só acontece quando o usuário clica em **Confirmar** no card de proposta (fluxo AI-05 já existente).

**Fora de escopo (próximas fases, nesta ordem):** agente do WhatsApp desmarcando/remarcando (Fase 2), lançamentos financeiros (Fase 3), estoque (Fase 4, depois da feat/inventory).

## Global Constraints

- Reutilize o fluxo existente: write tool → `AiActionService.propose(...)` → card `AiActionProposalCard` → `confirm` → `AiActionExecutor`. **Nenhuma tool de escrita grava direto.**
- Parâmetros executados vêm SEMPRE da proposta persistida, nunca do LLM no momento da confirmação.
- Tenant/usuário/permissões vêm do `AiToolContext` (CurrentUser), nunca de argumentos.
- Permissões reais: leitura `appointment:read`; desmarcar/remarcar `appointment:update`.
- Fuso da clínica: `America/Sao_Paulo`. O LLM recebe e envia horários **locais** (`yyyy-MM-dd'T'HH:mm`); a conversão para `Instant` é feita no backend.
- Textos para o usuário em pt-BR. Padrão de código igual a `application/tool/write/CreateTaskTool.java` e `tool/tools/SearchTasksTool.java`.
- Verificação: `cd backend && mvn -q test` (sem Docker → NÃO rodar `mvn verify`); frontend `cd frontend-refine && npx vitest run --pool=threads <arquivos>` e `npx tsc --noEmit`.

Caminho base backend: `backend/src/main/java/com/becommerce/crm/` (abaixo `B/`). Testes: `backend/src/test/java/com/becommerce/crm/` (`T/`).

---

## Task 1 — Backend: `cancel` com motivo no AppointmentUseCase

Hoje `changeStatus(CANCELED)` chama `appointment.cancel(null)` (sem motivo).

**Arquivos:** `B/sales/scheduling/application/port/in/AppointmentUseCase.java`, `B/sales/scheduling/application/service/AppointmentService.java`, teste em `T/sales/scheduling/application/service/AppointmentServiceTest.java` (crie/estenda no padrão existente).

1. Na interface:
   ```java
   /** Cancela registrando o motivo (ex.: desmarcado pelo assistente Léo). */
   AppointmentResponse cancel(UUID companyId, UUID appointmentId, String reason);
   ```
2. No service, copie a estrutura de `changeStatus` (TenantContext, `requireOwned`, save, audit):
   ```java
   @Override
   @Transactional
   public AppointmentResponse cancel(UUID companyId, UUID appointmentId, String reason) {
       try {
           TenantContext.setCompanyId(companyId);
           Appointment appointment = requireOwned(companyId, appointmentId);
           String trimmed = reason == null || reason.isBlank() ? null
                   : reason.trim().substring(0, Math.min(reason.trim().length(), 500));
           appointment.cancel(trimmed);
           appointmentRepository.save(appointment);
           auditor.record(companyId, AuditAction.UPDATE, AuditModule.SCHEDULING, "Appointment",
                   appointment.getId().toString(), "Agendamento cancelado: " + appointment.getTitle(),
                   null, null);
           return toResponse(appointment);
       } finally {
           TenantContext.clear();
       }
   }
   ```
   Confira em `Appointment.cancel(String)` se cancelar algo já cancelado lança `SchedulingValidationException`; se não lançar, adicione a guarda no domínio (mensagem "Agendamento já está cancelado.").
3. Testes: cancela com motivo (status CANCELED, `cancelReason` gravado); motivo em branco → null; agendamento de outra empresa → exceção de não encontrado.
4. Commit: `feat(scheduling): cancel appointment with reason`.

## Task 2 — Backend: read tool `search_appointments`

**Arquivo novo:** `B/automation/ai/application/tool/tools/SearchAppointmentsTool.java` (extends `AbstractAiReadTool`, permissão `appointment:read`).

Argumentos:
- `date` (string `yyyy-MM-dd`, opcional; padrão = hoje em São Paulo)
- `days` (integer 1–14, padrão 1)
- `patient` (string opcional: nome/telefone do paciente — resolver via `ContactUseCase.search(companyId, patient, 5)`)
- `includeCanceled` (boolean, padrão false)

Lógica:
- `from = date.atStartOfDay(SP).toInstant()`, `to = date.plusDays(days)...`; `appointmentUseCase.list(companyId, from, to, null)`.
- Se `patient` informado: filtra por `contactId` ∈ ids encontrados; se nenhum contato encontrado → `AiToolResult.fail(NAME, "Nenhum paciente encontrado para '<patient>'.")`.
- Exclui `CANCELED` salvo `includeCanceled`.
- Ordena por `startAt`, limita a 50 e devolve lista enxuta (record interno `AppointmentSummary(UUID id, String title, String start, String end, String status, String patientName)`), com `start`/`end` em horário local `dd/MM/yyyy HH:mm` e `patientName` (busque o nome do contato pelo `contactId`; use o mesmo `ContactUseCase`; confira o nome do campo em `ContactResponse`).
- Data inválida → `IllegalArgumentException("Data inválida: use o formato AAAA-MM-DD.")`.

Teste `T/automation/ai/application/tool/tools/SearchAppointmentsToolTest.java` (Mockito, sem Spring): janela padrão = hoje; filtro por paciente; cancelados excluídos; paciente inexistente → fail; sem permissão → registry nega (já coberto pelo registry, um teste basta).

Commit: `feat(ai): search_appointments read tool`.

## Task 3 — Backend: write tools `cancel_appointment` e `reschedule_appointment`

**Arquivos novos:** `B/automation/ai/application/tool/write/CancelAppointmentTool.java`, `RescheduleAppointmentTool.java` (padrão de `CreateTaskTool`: `@Lazy AiActionService`, `risk()="WRITE"`, `requiredPermission()="appointment:update"`).

### cancel_appointment
- Descrição para o modelo: "Propõe DESMARCAR (cancelar) uma consulta. Não executa: cria uma proposta que o usuário confirma. Antes, use search_appointments para obter o appointmentId."
- Args: `appointmentId` (uuid, obrigatório), `reason` (string, opcional, máx 500).
- Valida com `appointmentUseCase.getById(ctx.companyId(), id)` (garante tenant); status `CANCELED`/`COMPLETED` → `IllegalArgumentException("Esta consulta não pode ser desmarcada (status: …).")`.
- `parameters`: `appointmentId`, `reason`.
- `propose(companyId, userId, conversationId, "cancel_appointment", "APPOINTMENT", appointmentId, parameters, "Desmarcar consulta: <title> em dd/MM às HH:mm" + (reason != null ? " — motivo: " + reason : ""))`.

### reschedule_appointment
- Args: `appointmentId` (uuid, obrigatório), `newStart` (string local `yyyy-MM-ddTHH:mm`, obrigatório, horário de São Paulo).
- Valida: consulta existe e não está `CANCELED`/`COMPLETED`; `newStart` no futuro; mantém a duração original (`newEnd = newStart + (endAt - startAt)`).
- `parameters`: `appointmentId`, `startAt` e `endAt` como `Instant.toString()`.
- Descrição: "Remarcar consulta: <title> de dd/MM HH:mm para dd/MM HH:mm".
- Conflito de horário: deixe o `reschedule` do domínio decidir na confirmação (force=false); se falhar, a ação vira FAILED com a mensagem — já tratado pelo `AiActionService`.

### Executor
Em `B/automation/ai/application/action/AiActionExecutor.java` injete `AppointmentUseCase` e adicione:
```java
case "cancel_appointment" -> appointmentUseCase.cancel(
        action.getCompanyId(), uuid(p.get("appointmentId")), str(p.get("reason")));
case "reschedule_appointment" -> appointmentUseCase.reschedule(
        action.getCompanyId(), uuid(p.get("appointmentId")),
        new RescheduleRequest(Instant.parse(str(p.get("startAt"))),
                Instant.parse(str(p.get("endAt"))), false));
```
Atualize os testes que constroem `AiActionExecutor` manualmente (procure `new AiActionExecutor(`).

Testes: estenda `T/automation/ai/application/tool/write/AiWriteToolsTest.java` — propõe com parâmetros corretos e NÃO chama `appointmentUseCase.cancel/reschedule`; consulta cancelada → rejeita; `newStart` no passado → rejeita; duração preservada. Em teste do executor: os dois `case` chamam o use case com os valores persistidos.

Commit: `feat(ai): propose cancel/reschedule appointment with confirmation`.

## Task 4 — Backend: Léo conhece as telas, a data e as novas capacidades

1. **Novo** `B/automation/ai/application/context/AiScreenCatalog.java`: mapa estático rota → (nome em pt-BR, o que o usuário faz ali). Use as rotas de `frontend-refine/src/lib/constants.ts` (`ROUTES`) que aparecem no menu: `/crm` Painel, `/agenda` Agenda, `/contacts` Pacientes/Contatos, `/pipeline` Funil, `/tasks` Tarefas, `/activities` Atividades, `/inbox` Caixa de entrada (WhatsApp), `/catalog` Catálogo, `/finance` Financeiro (+ `/finance/control`, `/finance/reconciliation`, `/finance/commissions`), `/reports` Relatórios (+ subrotas), `/settings/agenda` Configurações da agenda, `/settings/agent-config` Configuração do agente, `/members`, `/audit`. Confirme os rótulos reais no `Sidebar.tsx` e use-os. Métodos:
   - `String describe(String route)` → nome da tela pelo prefixo mais longo (ou null);
   - `String renderCatalog()` → bloco "Telas do CRM: - Agenda (/agenda): …".
2. `AiApplicationContext.moduleOf`: adicionar `agenda`/`appointment` → `AGENDA`, `finance` → `FINANCE`, `catalog` → `CATALOG`, `inbox` → `INBOX`.
3. `AiAssistantService.systemPrompt()` vira método com `(ResolvedAiContext/AiApplicationContext app)` e o texto:
   - mantém identidade (Léo), pt-BR, não inventar dados;
   - troca "Você tem acesso de LEITURA" por: "Você consulta dados com as ferramentas. Para desmarcar ou remarcar consultas, criar tarefas/atividades ou alterar oportunidades, use a ferramenta correspondente: ela cria uma PROPOSTA que o usuário confirma no card. Nunca diga que já executou — diga que preparou para confirmação. Antes de desmarcar/remarcar, localize a consulta com search_appointments e, se houver mais de uma possível, pergunte qual.";
   - "Agora são <dia da semana>, dd/MM/yyyy HH:mm (horário de Brasília)." (use `Clock` injetável para testes);
   - "O usuário está na tela: <nome> (<rota>)." quando houver;
   - `AiScreenCatalog.renderCatalog()`; instrução: "Para indicar uma tela, cite o nome e a rota (ex.: Agenda — /agenda)".
4. Testes: `AiScreenCatalogTest` (prefixo mais longo, rota desconhecida → null); teste do prompt contendo data, tela atual e a regra de proposta.

Commit: `feat(ai): Léo knows CRM screens, current date and write-with-confirmation rule`.

## Task 5 — Frontend: enviar a tela atual sempre + links de rota no chat

1. `frontend-refine/src/features/automation/ai/hooks/useAiContext.ts`: `resolveAiContext` passa a devolver `{ screen, route }` para QUALQUER rota do dashboard (screen = primeiro segmento, ex.: `agenda`), mantendo `recordType/recordId` só quando o padrão de registro casar (comportamento atual preservado). Rotas `/login`, `/register` etc. → null.
2. Testes em `useAiContext.test.ts`: `/agenda` → `{screen:"agenda", route:"/agenda"}` sem record; `/contacts/<uuid>` continua como hoje; `/contacts/abc` → `{screen:"contacts", route:"/contacts/abc"}` sem record.
3. Na renderização da resposta do assistente (`AiChatAssistant.tsx`/componente de mensagem): transformar rotas internas citadas no texto que existam em `ROUTES` (regex `` /(?:^|\s)(\/[a-z][a-z0-9/-]*)/ ``) em `<Link>` do Next. Só rotas presentes em `Object.values(ROUTES)`; nada de `dangerouslySetInnerHTML`. Teste: texto "Abra a Agenda — /agenda" renderiza link `href="/agenda"`; `/xyz` não vira link.
4. O `AiActionProposalCard` já é genérico (usa `description`) — conferir visualmente que as propostas novas aparecem; após confirmar `cancel_appointment`/`reschedule_appointment`, invalidar as queries da agenda (procure a query key da agenda em `features/` e adicione no `onSuccess` do `useAiConfirmAction` quando `action.tool` terminar em `_appointment`).

Verificação: `npx vitest run --pool=threads src/features/automation/ai` e `npx tsc --noEmit`.

Commit: `feat(ai): send current screen to Léo and link routes in replies`.

## Task 6 — Verificação final

- `cd backend && mvn -q test` verde; frontend vitest + tsc verdes.
- Teste manual local (se backend rodando): "quais consultas tenho amanhã?", "desmarca a consulta da Maria amanhã, ela viajou" → card de proposta → Confirmar → consulta some da agenda com motivo; "remarca para quinta às 15h" → card → Confirmar.
- Sem push. Relatar arquivos, testes e hashes.
