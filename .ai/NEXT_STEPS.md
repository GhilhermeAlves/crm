# Próximos Passos

## 1. 🔴 Módulo de Notificações (próximo)

### Status
⏳ Próximo passo — roadmap atualizado em 2026-08-17

### Backend (greenfield)
- Migração Flyway: tabela `notifications` (id, company_id, user_id, type, title, body, metadata JSON,
  read_at, created_at) + RLS tenant + grants `crm_app`.
- Domain: `Notification`, `NotificationType` (enum), exceções.
- Application: `NotificationService` (create, listMy, markAsRead, markAllRead, unreadCount),
  ports (repository), DTOs.
- Infrastructure: entidade JPA, repositório, mapper.
- Presentation: `NotificationController` (`GET /api/v1/notifications`, `POST /{id}/read`,
  `POST /read-all`, `GET /unread-count`).
- Push: **WebSocket/STOMP** (hoje não existe nenhum) OU SSE — decidir transporte.
- Provider de e-mail real (hoje `ConsoleEmailSender` fake).

### Frontend
- Feature `notifications` (types, service, hook `useNotifications`).
- Página `/notifications` (listar, marcar como lida).
- Sino real no `Header.tsx` com badge de não-lidas + dropdown (hoje hardcoded decorativo).
- Consumir WebSocket/SSE para atualização em tempo real.

## 2. 🔴 Módulo IA / Sugestão de Resposta (depois)

### Status
⏳ Após Notificações

- Integração LLM (OpenAI) — pasta `infrastructure/integration/openai` já existe (vazia).
- Endpoint de sugestão de resposta no chat/omnichannel.
- Botão "Sugerir resposta" no `ChatThread.tsx`.
- Hint/fallback: hoje o dashboard usa `suggestion` determinístico (regras) — manter como fallback.

## 3. 🟠 Backlog
- Campanhas (17) → Automações (18) → Analytics (19)
- Fechamento Sprint 16 (WhatsApp deploy)
- Reports (`/reports`)

---

*Última atualização: 2026-08-17*