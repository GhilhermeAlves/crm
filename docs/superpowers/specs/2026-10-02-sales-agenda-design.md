# Agenda de vendas (sales/appointment) — Design

Data: 2026-10-02 · Branch: `feature/sales-agenda` (a partir de `feature/frontend-refine`)
Referência visual/funcional: agenda da Capim (`dash.capim.com.br/#/schedule`).

## Objetivo

Dar aos vendedores uma agenda de compromissos (reuniões, demos, ligações marcadas)
com visão semanal/diária/mensal, filtro por responsável e vínculo com contato e
oportunidade. Tarefas existentes aparecem na agenda como marcadores pontuais.

## Decisões

- **Entidade nova `Appointment`** em `backend/.../sales/appointment` (não estende `Task`).
- Tarefas com `dueAt` são exibidas na agenda (somente leitura, sem duração), vindas do endpoint de tasks já existente.
- **Fora do escopo v1 (YAGNI):** link público de agendamento, aba "Retornos",
  recorrência personalizada, lembretes automáticos via follow-up, sincronização com Google Calendar.
  Repetição v1: nenhuma / diária / semanal / mensal, materializada como ocorrências
  (ver abaixo).

## Modelo de domínio

`Appointment`:

| Campo | Tipo | Regra |
|---|---|---|
| id, companyId | UUID | RLS por empresa (FORCE), igual a `tasks` |
| type | `MEETING` \| `BLOCK` | BLOCK = bloqueio de agenda (sem contato) |
| title | String ≤ 200 | obrigatório; para MEETING sem título usa nome do contato |
| contactId, opportunityId | UUID nullable | validados como pertencentes à empresa |
| ownerId | UUID | responsável (usuário da empresa); obrigatório |
| startAt, endAt | OffsetDateTime | `endAt > startAt`; allDay ajusta para 00:00–23:59 |
| allDay | boolean | |
| status | `PENDING` \| `CONFIRMED` \| `CANCELED` | default PENDING |
| attendance | `ATTENDED` \| `NO_SHOW` \| null | só pode ser marcado quando `startAt` já passou e status ≠ CANCELED |
| location | String nullable | texto livre ou link de reunião |
| notes | String nullable | |
| color | String nullable | hex `#RRGGBB` (marcador) |
| seriesId | UUID nullable | agrupa ocorrências de uma repetição |
| createdBy, createdAt, updatedAt | | |

Regras:
- Conflito de horário: o serviço **rejeita** um MEETING que sobreponha um BLOCK do
  mesmo responsável (409). Sobreposição entre MEETINGs é permitida, mas a resposta traz
  `warnings: ["OVERLAP"]` para o front avisar.
- Repetição: no create, `recurrence {freq: DAILY|WEEKLY|MONTHLY, until: date}` gera
  N ocorrências com o mesmo `seriesId` (máx. 100). Editar/excluir aceita
  `scope=THIS|FOLLOWING|ALL`.

## Persistência

Migration `V079__appointments.sql`: tabela `appointments` com RLS FORCE por
`company_id`, índices `(company_id, owner_id, start_at)` e `(company_id, series_id)`,
FKs para `contacts`, `opportunities`, `users`. Permissões `appointment:create|read|update|delete`
semeadas e atribuídas aos mesmos papéis que têm `task:*` (padrão V040).

## API

Base: `/api/v1/companies/{companyId}/appointments`

- `GET ?from=&to=&ownerIds=a,b&includeCanceled=false` — intervalo obrigatório (máx. 62 dias).
- `POST` — cria (com `recurrence` opcional) → 201 `{appointments:[...], warnings:[...]}`.
- `PUT /{id}?scope=` — edita.
- `PATCH /{id}/reschedule` `{startAt,endAt}` — usado pelo arrastar/redimensionar.
- `POST /{id}/status/{status}` e `POST /{id}/attendance/{ATTENDED|NO_SHOW}`.
- `DELETE /{id}?scope=`.
- `GET /api/v1/companies/{companyId}/opportunities/{id}/appointments` — para a tela da oportunidade.

Segue o padrão de `TaskController`: `@CurrentCompanyId`, `@PreAuthorize`, use case em
`application/port/in`, repositório em `application/port/out` + `infrastructure/persistence`.
Eventos `AppointmentCreated/Canceled/AttendanceMarked` publicados no publisher existente
(sem consumidores na v1, deixando o gancho para automação).

## Frontend (`frontend-refine`)

- Rota `src/app/(dashboard)/agenda/page.tsx`; item "Agenda" no menu de Vendas.
- Feature `src/features/sales/appointments/{components,hooks,schemas,services,types}`.
- Calendário **próprio** com CSS grid + `date-fns` (já é dependência), sem lib nova:
  - `AgendaToolbar`: Hoje, ‹ ›, título do período, seletor Dia/Semana/Mês.
  - `AgendaSidebar`: botão "Novo" (Compromisso / Bloqueio), mini-calendário, filtro por responsável com "Selecionar todos".
  - `WeekView`/`DayView`: grade de horas (slots de 15 min, rola para 08:00), fim de semana sombreado, hoje destacado, linha do "agora", eventos posicionados por `startAt/endAt` com cálculo de colunas para sobreposição; tarefas como chips no topo do horário.
  - `MonthView`: células com até 3 eventos + "+N".
  - `AppointmentDialog` (react-hook-form + zod, padrão das outras features): tipo, contato (busca), oportunidade, título, responsável, data, de/até, dia inteiro, repetição, local, observações, cor.
  - `AppointmentPopover`: detalhes, editar, excluir, status, "compareceu?", links "Abrir conversa" (WhatsApp/inbox do contato) e "Abrir oportunidade".
  - Clique em slot vazio abre o diálogo pré-preenchido; arrastar/redimensionar chama `reschedule` com atualização otimista e rollback em erro.
- Estado de período/visão/responsáveis na URL (`?view=week&date=2026-10-02&owners=`).

## Erros

- 400 validação (mensagens em PT-BR, padrão das exceções de domínio), 404 contato/oportunidade de outra empresa, 409 conflito com bloqueio.
- Front: toast com a mensagem do backend; aviso não bloqueante para `OVERLAP`.

## Testes

- Domínio: `AppointmentTest` (validações, allDay, attendance, geração de série).
- Serviço: conflito com BLOCK, escopo THIS/FOLLOWING/ALL, validação de relacionamentos.
- `AppointmentIsolationIT` (RLS entre empresas), igual a `FollowUpIsolationIT`.
- Controller: permissões e intervalo máximo.
- Front (vitest): cálculo de layout de sobreposição, navegação de período, schema do formulário.
