# Agenda de agendamentos (sales/scheduling) — Design

Data: 2026-10-02 · Branch: `feature/sales-agenda` (a partir de `feature/frontend-refine`)
Referência: agenda da Capim (`dash.capim.com.br/#/schedule`) + link de agendamento estilo Calendly.

## Objetivo

Agenda para **marcar horários** com clientes:
1. A equipe marca horários dentro do CRM.
2. O cliente escolhe um horário livre por um **link público de agendamento**.
3. **Sincronização em duas vias com o Google Calendar** por usuário: o que é marcado no
   CRM vai para o Google (com link do Meet), e eventos ocupados no Google bloqueiam
   horários no CRM.

O escopo é grande, então a entrega é em **3 fases**, cada uma com plano e PR próprios:

| Fase | Entrega |
|---|---|
| 1 | Agenda interna: tipos de agendamento, disponibilidade, agendamentos, página `/agenda` |
| 2 | Link público de agendamento (página sem login + confirmação/cancelamento) |
| 3 | Google Calendar: conexão OAuth por usuário, envio CRM→Google, leitura Google→CRM |

Fora do escopo: tarefas na agenda, recorrência, pagamento antecipado, lembretes por
WhatsApp (fica o gancho de eventos para automação), Outlook.

## Módulo

Backend: `backend/.../sales/scheduling/{domain,application,infrastructure,web}`,
no padrão de `sales/task` (portas in/out, `@CurrentCompanyId`, `@PreAuthorize`, RLS FORCE).
Frontend: `frontend-refine/src/features/sales/scheduling/*` + rotas
`(dashboard)/agenda` e pública `/agendar/[slug]`.

## Modelo de domínio

**AppointmentType** (serviço que pode ser agendado, ex.: "Reunião de apresentação 30 min")
- companyId, name, slug (único na empresa), durationMinutes (5–480), bufferBeforeMinutes,
  bufferAfterMinutes, minNoticeHours (antecedência mínima), maxDaysAhead,
  slotIntervalMinutes (15/30/60), color, location (`GOOGLE_MEET` | `PHONE` | `IN_PERSON` + texto),
  hosts (lista de usuários que atendem), assignment (`ROUND_ROBIN` | `CHOOSE_HOST`),
  publicBookingEnabled, active.

**Availability** (por usuário)
- Janelas semanais: `(weekday, start, end)`, várias por dia (ex.: 09–12, 13–18).
- Exceções por data: dia indisponível ou janelas diferentes.
- timezone do usuário (default `America/Sao_Paulo`).

**Appointment** (horário marcado)
- companyId, appointmentTypeId (nullable para compromisso avulso), hostId, contactId,
  opportunityId (opcional), title, startAt, endAt (UTC), status
  (`SCHEDULED` | `CONFIRMED` | `CANCELED` | `COMPLETED` | `NO_SHOW`), source
  (`INTERNAL` | `PUBLIC_LINK`), location/meetingUrl, notes, cancelReason,
  publicToken (para o cliente cancelar/remarcar), googleEventId, createdBy.

**Block** (bloqueio de agenda): hostId, startAt, endAt, reason. Também guarda os
"ocupados" vindos do Google (`source=GOOGLE`, `externalId`), que nunca são editáveis no CRM.

### Cálculo de horários livres (núcleo, função pura `SlotCalculator`)
`slots = janelas de disponibilidade do host no dia (tz do host)`
`− agendamentos não cancelados (com buffers) − bloqueios (internos + Google)`
`− antes de agora + minNotice − depois de maxDaysAhead`, fatiado em `slotIntervalMinutes`
com duração do tipo. Para ROUND_ROBIN, o slot está livre se algum host estiver livre;
ao reservar, escolhe o host livre com menos agendamentos na semana.

### Regras
- Criar/remarcar valida **na transação** que o slot ainda está livre (lock por host com
  `SELECT ... FOR UPDATE` nas linhas do host no intervalo, mais restrição de exclusão
  `EXCLUDE USING gist (host_id WITH =, tstzrange(start_at,end_at) WITH &&) WHERE status <> 'CANCELED'`).
  Conflito → 409.
- A equipe pode forçar um horário fora da disponibilidade (`force=true`, com aviso), mas
  nunca sobrepor outro agendamento ou bloqueio.
- Agendamento pelo link cria ou reaproveita o contato pelo e-mail/telefone (normalização
  existente de contatos) e registra uma Activity no contato.

## Persistência
Migrations `V079__scheduling.sql` (fase 1) e `V080__google_calendar.sql` (fase 3).
Tabelas: `appointment_types`, `appointment_type_hosts`, `availability_rules`,
`availability_overrides`, `appointments`, `schedule_blocks`, (fase 3) `calendar_connections`.
Extensão `btree_gist` para a restrição de exclusão. Permissões
`appointment:create|read|update|delete` e `scheduling:configure`, semeadas como em V040.

## API (autenticada)
Base `/api/v1/companies/{companyId}`:
- `appointment-types` CRUD.
- `users/{userId}/availability` GET/PUT (usuário edita a própria; admin edita de todos).
- `appointments?from&to&hostIds` (intervalo ≤ 62 dias), POST, PUT `/{id}`,
  PATCH `/{id}/reschedule`, POST `/{id}/status/{status}`, DELETE.
- `blocks` CRUD.
- `appointment-types/{id}/slots?from&to` — slots livres (reaproveitado pelo público).

## API pública (fase 2, `permitAll` + rate limit por IP)
- `GET /api/v1/public/booking/{companySlug}/{typeSlug}` — dados do tipo (nome, duração, local).
- `GET .../slots?from&to&tz` — slots livres no fuso do visitante.
- `POST .../book` `{name,email,phone,startAt,notes,tz}` → cria agendamento, retorna `publicToken`.
- `GET|POST /api/v1/public/appointments/{publicToken}` — ver/cancelar/remarcar.
- Proteção: honeypot + rate limit; sem expor dados de outros agendamentos (só slots).
- E-mail de confirmação com arquivo `.ics` e link de cancelar/remarcar (serviço de
  e-mail já usado em forgot-password).

## Google Calendar (fase 3)
- **Conexão:** em "Configurações > Agenda", o usuário clica em "Conectar Google".
  OAuth 2.0 (authorization code + PKCE, `access_type=offline`), escopos
  `calendar.events` e `calendar.freebusy`/`calendar.readonly`. Refresh token guardado
  em `calendar_connections` **criptografado** (AES-GCM, chave em variável de ambiente).
  Usuário escolhe o calendário de destino e quais calendários contam como ocupado.
- **CRM → Google:** ao criar/remarcar/cancelar um agendamento, um evento de domínio vai
  para a fila (RabbitMQ existente) e um worker cria/atualiza/apaga o evento no Google
  (com `conferenceData` para gerar o Meet e o cliente como convidado). Falhas são
  reenviadas com backoff; status de sincronização visível no agendamento.
- **Google → CRM:** canal de notificações (`events.watch`) por calendário apontando para
  `POST /api/v1/integrations/google/calendar/webhook`, mais sincronização incremental
  com `syncToken`. Eventos ocupados viram `schedule_blocks (source=GOOGLE)`. Eventos que
  o próprio CRM criou (identificados por `extendedProperties.private.crmAppointmentId`)
  são ignorados para não duplicar; se o usuário mover/apagar esse evento no Google, o
  CRM atualiza/cancela o agendamento.
- Renovação dos canais `watch` (expiram) por job agendado; job de ressincronização a
  cada 15 min como rede de segurança.
- Desconectar: revoga token, apaga blocos de origem Google e para os canais.

## Frontend
**`/agenda`** (estilo Capim):
- Barra: Hoje, ‹ ›, período, Dia/Semana/Mês, engrenagem (configurações da agenda).
- Lateral: botão "Novo" (Agendamento / Bloqueio / Copiar link de agendamento),
  mini-calendário, filtro por responsável com "Selecionar todos".
- Grade semanal/diária em CSS grid + `date-fns` (sem lib nova): horas fora da
  disponibilidade sombreadas, bloqueios hachurados (Google com ícone), linha do agora,
  sobreposição em colunas, clicar em horário vazio abre o formulário já preenchido,
  arrastar/redimensionar para remarcar (otimista + desfaz em erro).
- Formulário: tipo de agendamento, contato (buscar ou criar), oportunidade, responsável,
  data/hora (sugere próximos slots livres), local, observações.
- Painel de detalhes: status, compareceu/faltou, abrir conversa, abrir oportunidade,
  copiar link do Meet, status da sincronização com o Google.

**Configurações > Agenda:** tipos de agendamento, minha disponibilidade (grade semanal +
exceções), conexão com o Google.

**Página pública `/agendar/[companySlug]/[typeSlug]`:** calendário do mês com dias
disponíveis, lista de horários no fuso do visitante, formulário curto, tela de
confirmação, página de cancelar/remarcar via token. Responsiva para celular.

## Erros
400 validação (mensagens PT-BR), 404 recurso de outra empresa, 409 horário ocupado
(front recarrega os slots e avisa "esse horário acabou de ser ocupado"), 429 no público.
Falha do Google nunca impede o agendamento no CRM; fica marcado "pendente de sincronizar".

## Testes
- `SlotCalculatorTest`: janelas, buffers, antecedência, fuso horário e horário de verão, round robin.
- Serviço: conflito concorrente (dois POSTs no mesmo slot → um 409), forçar fora da
  disponibilidade, criação de contato pelo link.
- `SchedulingIsolationIT`: RLS entre empresas; público não vaza dados.
- Google: cliente HTTP mockado (WireMock) para push, webhook + syncToken, eventos do
  próprio CRM ignorados, refresh de token.
- Front (vitest): layout de sobreposição, conversão de fuso nos slots, schemas dos formulários.
