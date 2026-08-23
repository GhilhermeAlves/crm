# Entrega Funcional — Notificações In-app

> **Status: ✅ IMPLEMENTADA** — entrega funcional sem numeração formal de sprint
> (não há sprint dedicada no roadmap; registrada aqui para fins de governança).
> Código implantado em produção (VPS) desde as releases de 2026-08-16+.

## Escopo

Notificações in-app por usuário: sino com badge no header, página `/notifications`,
push via WebSocket/STOMP no backend e polling no frontend.

## Backend

- Domínio/aplicação: `application/notification/` (`NotificationService`, `NotificationUseCase`,
  DTOs, porta `NotificationRepository` e `NotificationPusher`).
- Persistência: `infrastructure/notification/persistence/` (JPA entity/repo/impl), RLS FORCE
  por `company_id` no padrão dos demais módulos.
- Push real: `infrastructure/notification/websocket/StompNotificationPusher`
  (broker relay RabbitMQ/STOMP configurado na aplicação).
- Migrações: `V047__notifications.sql` (tabela) e `V048__notification_permissions.sql`
  (permissões `notification:*`); grants de papel via `RoleSeedService`.
- Endpoints REST para listagem/marcação de leitura, gated por `notification:*`.
- E-mail: interface `EmailSender` com implementação atual `ConsoleEmailSender`
  (**débito**: envio de e-mail real ainda não conectado às notificações; existe provider
  SMTP real no módulo de mail/convites desde `fc6c778`).

## Frontend

- `frontend/src/features/notifications/` — `NotificationBell` (sino + badge),
  hooks React Query, service, types.
- Página `(dashboard)/notifications` listando notificações com marcação de leitura;
  entrada na Sidebar.

## Qualidade

- Testes backend do módulo notification verdes (`NotificationServiceTest`).
- CI/CD GREEN; funcionalidade ativa na VPS.

## Débitos conhecidos (confirmados no código)

- **E-mail real**: apenas `ConsoleEmailSender` ligado às notificações.
- **Frontend usa polling** (`refetchInterval` em `useNotifications.ts`): a infraestrutura
  de push (STOMP) existe no backend, mas o frontend ainda não consome o websocket.
