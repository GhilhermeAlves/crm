# Agente do WhatsApp — agenda: motivo do cancelamento e remarcação

> Agente do WhatsApp (identidade configurável em Configurações → Agente de IA) — **não é o Léo**.
> Branch `feat/whatsapp-agenda`, worktree `.worktrees/feat-whatsapp-agenda`.

Arquivo: `backend/src/main/java/com/becommerce/crm/communication/omnichannel/application/service/WhatsAppSchedulingTools.java`.
O paciente age por si mesmo (sem card de confirmação): o agente confirma na conversa antes de chamar a ferramenta,
e só pode mexer em consultas futuras do próprio telefone (`patientAppointments`).

## Task 1 — `cancelar_consulta` grava o motivo ✅
- Usa `AppointmentUseCase.cancel(companyId, id, reason)`; motivo vira `"WhatsApp: <motivo>"`, sem motivo grava
  `"Cancelado pelo paciente via WhatsApp"`. O profissional continua sendo notificado com o motivo.
- Guidance: perguntar o motivo de forma leve (opcional).

## Task 2 — nova ferramenta `remarcar_consulta` ✅
- Args `consulta_id`, `novo_inicio` (`AAAA-MM-DDTHH:mm`, Brasília).
- Checa posse (consulta do próprio paciente), horário no futuro, revalida o horário em `SlotService` para o tipo da
  consulta e chama `reschedule(..., force=false)` mantendo profissional e duração.
- Conflito/indisponível → texto amigável, sem lançar. Notifica o profissional ("Consulta remarcada pelo WhatsApp").
- Guidance: preferir remarcar a cancelar+agendar; só dizer que remarcou depois da confirmação da ferramenta.

## Testes
`WhatsAppSchedulingToolsTest`: cancelamento com e sem motivo, remarcação feliz (duração/instants/notificação),
consulta de outro paciente, horário ocupado e horário no passado. `mvn -q test` verde.

## Próximos passos possíveis (não implementados)
- Confirmação de presença pelo paciente (`confirmar_consulta` → status CONFIRMED) a partir do lembrete.
- Teste manual com WhatsApp real antes de anunciar a funcionalidade.
