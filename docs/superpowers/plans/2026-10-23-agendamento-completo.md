# Novo agendamento completo + link de cadastro — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development. Execute na ordem, um commit por task, sem push/merge/PR. Se o código real não bater com o plano, siga o padrão existente mais próximo e registre no relatório.

**Goal:** Deixar o "Novo agendamento" igual ao do Capim (status gravado, recorrência real inclusive personalizada, procedimentos do catálogo, celular, título pré-preenchido, aviso de horário, cadeira/marcador na aba do paciente) e criar o link público de cadastro do paciente (botão no agendamento + ferramenta do agente do WhatsApp).

**Spec:** `docs/superpowers/specs/2026-10-23-agendamento-completo-design.md` — autoridade. Leia antes de cada task.

**Tech Stack:** Spring Boot hexagonal (`backend/`), Flyway, JUnit 5 + Mockito + AssertJ, Testcontainers (CI); Next.js + react-hook-form + zod + TanStack Query + shadcn/ui (`frontend-refine/`), Vitest.

## Global Constraints

1. Branch `feature/agendamento-completo`, pasta `C:\Users\ghilh\Desktop\PROJETO\crm\crm-deploy`. Não troque de branch.
2. Migration única `V108__appointment_recurrence_procedures_registration.sql` (última existente V107). Nunca editar migrations antigas. Conferir colisão de nomes com `git grep`. Sem Docker local.
3. RLS em toda tabela nova (ENABLE + FORCE + `tenant_isolation_policy` + `GRANT SELECT, INSERT, UPDATE, DELETE ... TO crm_app`), padrão do fim da V107.
4. Código de agenda vive em `backend/src/main/java/com/becommerce/crm/sales/scheduling/**` (AppointmentService, BlockService, DTOs, domínio Appointment/ScheduleBlock). Contato em `masterdata/contact/**`. Agente do WhatsApp em `communication/omnichannel/application/service/WhatsAppSchedulingTools.java` e providers `*ToolProvider.java`. Rotas públicas: siga o convite (`/api/v1/invitations/preview|register`): `SecurityConfig` permitAll + `docker/nginx/crm.conf` location própria para 127.0.0.1:8081.
5. Mensagens e textos em pt-BR. Exceções de agenda seguem as já usadas no módulo (`SchedulingValidationException`, `SlotUnavailableException` etc.); de contato, `ContactValidationException`.
6. Frontend: só tokens do tema; `useMutationDefaults`; testes com `fireEvent` + `screen.*` + `toBeTruthy()/toBeNull()`, sem user-event/jest-dom; Prettier `--check --end-of-line auto` em todo arquivo alterado. Local, `SidebarGroup.test.tsx` falha por ambiente (ignorar).
7. Verificação: backend `cd backend && mvn -q test`; frontend `cd frontend-refine && npm run lint && npx tsc --noEmit && npx vitest run --pool=threads`.
8. Commits Conventional pt-BR + linha em branco + `Co-Authored-By: <modelo> <noreply@anthropic.com>`.

---

### Task 1: Migration V108
- [ ] Criar `backend/src/main/resources/db/migration/V108__appointment_recurrence_procedures_registration.sql` exatamente conforme a seção "Migration" do spec: colunas `recurrence_group_id` (+ índices `idx_appointments_recurrence_group`, `idx_schedule_blocks_recurrence_group`), tabelas `appointment_procedures` e `patient_registration_links` (índice único em `token`, índice em `(company_id, contact_id)`), RLS/GRANT.
- [ ] Rota pública sem tenant: descubra como o convite público resolve a empresa (procure `invitations/preview` no backend e na migration correspondente). Se usar função `SECURITY DEFINER`, crie uma equivalente `app.find_registration_link(token)` que devolve `(id, company_id, contact_id, expires_at, used_at)`; senão siga o padrão encontrado. Registre a escolha no relatório.
- [ ] Commit `feat(agenda): migration de recorrência, procedimentos e link de cadastro`.

### Task 2: Status e recorrência no backend
- [ ] `CreateAppointmentRequest` ganha `AppointmentStatus status` e `RecurrenceRequest recurrence` (record novo em `application/dto`: `frequency` enum `RecurrenceFrequency {DAILY, WEEKLY, MONTHLY, YEARLY}`, `@Min(1) @Max(99) int interval`, `endType` enum `RecurrenceEnd {NEVER, ON_DATE, AFTER_COUNT}`, `LocalDate until`, `Integer count` 2–366). `CreateBlockRequest` ganha `RecurrenceRequest recurrence`. Atualize TODOS os `new CreateAppointmentRequest(`/`new CreateBlockRequest(` existentes (main e test, inclusive `WhatsAppSchedulingTools`) passando `null`.
- [ ] Domínio: `RecurrenceExpander` puro (ex.: `sales/scheduling/domain/RecurrenceExpander.java`) que, dado `start`, `end`, `RecurrenceRequest`, zona `America/Sao_Paulo`, devolve a lista de intervalos (o primeiro é o original). Regras do spec: data local + mesmo horário; MONTHLY/YEARLY usam `plusMonths/plusYears` a partir do original (dia 31 → último dia do mês); `NEVER` até 1 ano; teto 366; `ON_DATE` inclusivo; validações (`until` ≥ data inicial, `count` 2–366) → `SchedulingValidationException` pt-BR.
- [ ] `AppointmentService.create`: expande; checa conflitos (host, hard, cadeira) de TODAS as ocorrências antes de salvar; se houver, lança a exceção de conflito do módulo com mensagem listando até 5 datas `dd/MM HH:mm`; senão cria todas com o mesmo `recurrenceGroupId` (novo campo em `Appointment` + JPA), aplica tags a cada uma e o status: `CONFIRMED` → `confirm()`, `CANCELED` → `cancel(null)`, `SCHEDULED` → nada. `patientAutoMarker` e auditoria uma vez (mensagem com o nº de ocorrências). Retorna a primeira.
- [ ] `BlockService.create`: mesma expansão e `recurrenceGroupId`.
- [ ] Testes: `RecurrenceExpanderTest` (diário 3 ocorrências, semanal a cada 2, mensal do dia 31, anual, ON_DATE inclusivo, NEVER limitado a 1 ano, teto 366, validações) e em `AppointmentServiceTest`/`BlockServiceTest` (status CONFIRMED aplicado; recorrência cria N com mesmo grupo; conflito em uma ocorrência → nada salvo). TDD.
- [ ] `mvn -q test`; commit `feat(agenda): grava status e recorrência de agendamentos e bloqueios`.

### Task 3: Procedimentos no agendamento (backend)
- [ ] `CreateAppointmentRequest` e `UpdateAppointmentRequest` ganham `List<UUID> catalogItemIds`; `AppointmentResponse` ganha `List<ProcedureRef> procedures` (`record ProcedureRef(UUID id, String name)`).
- [ ] Porta + JPA para `appointment_procedures` (replace por agendamento, como `tagRepository.replace`). Validar cada item: mesma empresa, `SERVICE`, ativo → senão `SchedulingValidationException("Procedimento inválido para o agendamento.")`. Aplicar em todas as ocorrências da recorrência. Preencher `procedures` nas respostas (lista e detalhe) sem N+1 grosseiro (buscar por lote de ids de agendamento).
- [ ] Testes de service (grava, valida PRODUCT/inativo/outra empresa, update substitui). `mvn -q test`; commit `feat(agenda): procedimentos do catálogo no agendamento`.

### Task 4: Link de cadastro (backend)
- [ ] Pacote `masterdata/contact/registration` (ou o mais próximo do padrão): domínio `RegistrationLink` (token 32 bytes `SecureRandom` base64url, expira em 7 dias), porta/JPA, `RegistrationLinkService`:
  - `create(companyId, contactId, actorId)` → invalida links não usados do contato, cria novo, devolve `{url, expiresAt}` com `url = <APP_PUBLIC_URL>/cadastro/<token>` (procure a propriedade de URL pública do frontend já usada em convites/e-mails; registre qual).
  - `preview(token)` → `{companyName, firstName, lastName, phone}`; inexistente → 404, expirado/usado → 410 (exceções novas mapeadas no `GlobalExceptionHandler`).
  - `submit(token, request)` → valida como o cadastro de contato (CPF, e-mail, telefone), atualiza o contato (reaproveite o caso de uso de update de contato), marca `used_at`, auditoria "Cadastro preenchido pelo paciente pelo link.".
- [ ] Controllers: autenticado `POST /api/v1/companies/{companyId}/patients/{contactId}/registration-link` (`contact:update`); público `GET` e `POST /api/v1/public/registration/{token}` (honeypot `website` vazio obrigatório; limite de taxa por IP seguindo o mecanismo já usado em rotas públicas, se houver; senão um limitador simples em memória 20 req/10 min por IP).
- [ ] `SecurityConfig`: permitAll para `/api/v1/public/registration/**`. `docker/nginx/crm.conf`: `location ^~ /api/v1/public/registration/` → `http://127.0.0.1:8081` com os mesmos headers do convite, antes do `/api/` genérico.
- [ ] Testes: service (cria/invalida anterior, 404/410, submit atualiza contato e marca usado, CPF inválido 400), controller unitário, `RegistrationLinkIsolationIT` (RLS das tabelas novas da V108 — `appointment_procedures` e `patient_registration_links` — no padrão `BudgetIsolationIT`).
- [ ] `mvn -q test`; commit `feat(pacientes): link público de cadastro do paciente`.

### Task 5: Ferramenta do agente do WhatsApp
- [ ] Novo `RegistrationLinkToolProvider implements AgentToolProvider` (padrão de `HumanTransferToolProvider`): ferramenta `enviar_link_cadastro` sem argumentos; disponível quando a conversa tem contato; gera o link via `RegistrationLinkService.create(companyId, contactId, null)` e devolve "Link de cadastro: <url> (válido por 7 dias). Envie ao paciente." ; `guidance`: "Se o paciente precisar informar ou corrigir dados cadastrais (CPF, data de nascimento, e-mail, endereço), use enviar_link_cadastro e envie o link; não peça CPF pela conversa."
- [ ] Teste unitário do provider. Commit `feat(whatsapp): agente envia link de cadastro ao paciente`.

### Task 6: Formulário de novo agendamento (frontend)
Arquivos: `frontend-refine/src/features/sales/scheduling/{schemas/scheduling.schema.ts,components/CreateAppointmentDialog.tsx,types/*,services/*}`, `features/clinical/patients/components/PatientAppointments.tsx`, `app/(dashboard)/agenda/page.tsx`, novo `components/RecurrenceDialog.tsx`, testes em `src/__tests__/features/sales/scheduling/`.
- [ ] Schema/tipos: `recurrence` vira objeto `{frequency, interval, endType, until?, count?} | null` (presets do spec); `status` padrão `SCHEDULED`; `catalogItemIds?: string[]`; `contactPhone?: string`. O payload enviado inclui `status`, `recurrence`, `catalogItemIds` (o mesmo para bloqueio: `recurrence`).
- [ ] Diálogo: status inicia "Pendente"; título pré-preenchido com o nome do paciente quando `defaultContactId` vem definido (e ao escolher paciente, se o título estiver vazio); campo "Celular do(a) paciente (opcional)" com validação BR quando há paciente (se alterado, o pai atualiza o contato antes de criar — use o hook de update de contato existente); multi-select "Procedimentos" (serviços ativos do catálogo via `useCatalog(companyId, { type: "SERVICE", active: true, pageSize: 200 })`) com botão "Adicionar procedimentos" abrindo Configurações → Catálogo em nova aba (confira a rota real); aviso "Evento fora da faixa de horário" quando De/Até fora de `workHoursStart/End`; "Repetir agendamento" com presets + "Personalizado" abrindo `RecurrenceDialog` ("Configurar recorrência": Repetir a cada [−N+] [Dia|Semana|Mês|Ano]; Termina em: Nunca / Em [data] / Após [N] ocorrências; Cancelar/Concluir); mesmo seletor no bloqueio; Marcadores com "+ Novo marcador" (nome + cor) via `useCreateTag`.
- [ ] `PatientAppointments`: passa `chairs` (ativas), `tags`, `workHoursStart/End` (mesma origem da página da Agenda) e não passa `onSubmitBlock`; o diálogo esconde a aba "Bloqueio de Agenda" quando `onSubmitBlock` não é fornecido.
- [ ] Testes: status padrão Pendente e enviado no payload; título pré-preenchido; recorrência semanal e personalizada no payload; aviso fora do horário; aba Bloqueio oculta sem `onSubmitBlock`; procedimentos no payload.
- [ ] Verificação frontend; commit `feat(agenda): novo agendamento completo como no Capim`.

### Task 7: Link de cadastro (frontend)
- [ ] Hook/serviço `useCreateRegistrationLink(companyId, contactId)`; no diálogo, abaixo do celular: botão "Enviar link de cadastro" (ícone WhatsApp do lucide `MessageCircle`) → gera e abre `https://wa.me/55<dígitos>?text=` com "Olá, <nome>! Para agilizar seu atendimento, complete seu cadastro: <url>" (`window.open(..., "_blank", "noopener")`) e botão "Copiar link" (clipboard + toast). Sem telefone: só copiar.
- [ ] Página pública `src/app/(public)/cadastro/[token]/page.tsx` + `layout.tsx` sem `ProtectedRoute`: busca o preview via `fetch` direto a `/api/v1/public/registration/{token}` (sem o cliente autenticado), formulário (nome, sobrenome, CPF com máscara, nascimento, e-mail, celular, CEP, rua, número, complemento, bairro, cidade, UF; honeypot `website` oculto), envio, telas de sucesso, link expirado/usado (410) e não encontrado (404). Confirme no `middleware.ts` do frontend (se existir) que `/cadastro` é público.
- [ ] Testes da página (preview carregado, 410 mostra "Este link expirou ou já foi usado.", envio chama POST com os campos) e do botão (gera e chama `window.open` com wa.me).
- [ ] Verificação frontend; commit `feat(pacientes): página pública de cadastro e envio do link`.

### Task 8: Fechamento
- [ ] Spec: status "Implementada" + ajustes reais; registrar no fim deste plano "Divergências encontradas" e "Resultado final" (saídas resumidas das verificações).
- [ ] Commit `docs(agenda): marca o novo agendamento completo como implementado`.

## Divergências encontradas
(preencher)

## Resultado final
(preencher)
