# Calendar — Calendário

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Funcionalidades](#funcionalidades)
- [Componentes](#componentes)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar o componente de calendário para visualização de eventos, follow-ups e reuniões.

## Descrição

O calendário permite visualizar e gerenciar eventos, follow-ups de leads, reuniões e prazos. Suporta visualização mensal, semanal e diária.

## Funcionalidades

| Funcionalidade | Descrição |
|---|---|
| Visualização mensal | Calendário em grade |
| Visualização semanal | 7 dias com horas |
| Visualização diária | Agenda do dia |
| Criar evento | Modal de criação |
| Drag-and-drop | Mover eventos |
| Filtros | Por tipo, responsável |

## Componentes

| Componente | Descrição |
|---|---|
| CalendarView | Container principal |
| CalendarHeader | Navegação e filtros |
| MonthView | Visualização mensal |
| WeekView | Visualização semanal |
| DayView | Visualização diária |
| EventCard | Card do evento |
| EventForm | Formulário de evento |

## Responsabilidades

- Exibir eventos do CRM
- Criar follow-ups e lembretes
- Integrar com Google Calendar
- Notificar sobre eventos próximos

## Dependências

- [01-backend/Scheduler.md](../01-backend/Scheduler.md) — Eventos agendados
- [04-integrations/Google.md](../04-integrations/Google.md) — Google Calendar

## Regras

- Eventos são exibidos com cor por tipo
- Hoje é destacado no calendário
- Eventos passados ficam mais transparentes
- Clique no evento abre detalhes

## Futuras Melhorias

- Sincronização bidirecional com Google Calendar
- Convites para reuniões
- Timezone support
- Visualização de agenda da equipe
- Lembretes push

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
