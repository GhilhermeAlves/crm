# Módulo Atual

## Identificação
- **Nome:** Notificações
- **Tipo:** Backend + Frontend + Real-time
- **Status:** 🚧 Em andamento (próximo módulo a implementar)

## Objetivo
Sistema de notificações in-app completo: tabela + persistência, API (listar minhas, marcar como lida,
contagem de não-lidas), entrega em tempo real (WebSocket/SSE) e integração com o sino do header.

## Estado Atual (antes deste módulo)
- **Backend:** só `application/notification/EmailSender` (interface) + `ConsoleEmailSender` (fake que loga).
- **Frontend:** sino decorativo hardcoded em `components/layout/Header.tsx` (3 itens fixos, sem ação).
- **Banco:** sem tabela de notificações (só coluna `notification_preferences` em company_settings).
- **WebSocket/SSE:** inexistente em backend e frontend.

## Documentação Relacionada
- `docs/01-backend/Notifications.md`
- `docs/02-frontend/Notifications.md`
- `docs/05-business-rules/Notification.md`

## Dependências
- Nenhuma externa (módulo novo).

## Próxima Etapa
Implementar Notificações (backend → push → frontend). Depois: **IA / Sugestão de resposta** (Sprint 20).

---

*Atualizado em: 2026-08-17*