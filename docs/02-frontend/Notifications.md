# Notifications — Notificações UI

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Componentes](#componentes)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar os componentes de notificação no frontend.

## Descrição

Notificações são exibidas como toasts (temporários) e como lista no sino do header. São alimentadas via WebSocket para tempo real.

## Componentes

| Componente | Descrição |
|---|---|
| NotificationBell | Sino no header com contador |
| NotificationDropdown | Lista de notificações |
| NotificationItem | Card de notificação |
| Toast | Notificação temporária |

## Responsabilidades

- Exibir notificações em tempo real
- Toast para eventos imediatos
- Lista no sino para histórico
- Marcar como lida/não lida
- Preferências de notificação

## Dependências

- [01-backend/Notifications.md](../01-backend/Notifications.md) — API de notificações
- [Context.md](./Context.md) — NotificationProvider

## Regras

- Toast desaparece após 5 segundos
- Máximo de 3 toasts visíveis
- Notificações não lidas têm badge vermelho
- Clique na notificação navega para o recurso
- Marcar todas como lidas

## Futuras Melhorias

- Notificações com ações inline
- Modo "não perturbe"
- Som de notificação
- Notificações por canal
- Filtro de notificações

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
