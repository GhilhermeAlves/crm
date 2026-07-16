# Notification Context

## Resumo do Módulo
Sistema de notificações com 3 tipos: in-app, push e email digest. Agrupamento (max 1 por tipo/5min), mute (1h/8h/24h), retenção 30 dias. WebSocket real-time.

## Objetivo
Notificar usuários sobre eventos relevantes de forma não intrusiva e configurável.

## Responsabilidades
- 3 tipos: in-app, push, email digest
- Agrupamento: max 1 notificação por tipo a cada 5 minutos
- Mute: 1h, 8h, 24h
- Retenção: 30 dias (cleanup automático)
- WebSocket para notificações in-app em tempo real

## Tipos
- **In-app** - Badge + dropdown no header
- **Push** - Notificação do navegador/mobile
- **Email digest** - Resumo periódico por email

## APIs Relacionadas
- `GET /notifications` - Listar notificações (paginado)
- `GET /notifications/unread` - Contagem não lidas
- `PUT /notifications/:id/read` - Marcar como lida
- `PUT /notifications/read-all` - Marcar todas como lidas
- `PUT /notifications/mute` - Mutar (1h/8h/24h)
- `PUT /notifications/preferences` - Preferências

## Banco Relacionado
- `notifications` - Notificações geradas
- `user_notifications` - Status por usuário (lida, mutada)

## Componentes Frontend
- NotificationBell (badge + dropdown)
- NotificationList (in-app)
- PushManager (registro de push)
- NotificationPreferences

## Componentes Backend
- `notification` module (Controllers, Services, Domain)
- `aggregator` module (agrupamento 5min)
- `push` module (web push notifications)
- `email-digest` module (resumo periódico)
- WebSocket handler (real-time)

## Eventos
- `NotificationCreated` - Notificação gerada
- `NotificationGrouped` - Agrupada com similar
- `NotificationSent` - Entregue (in-app/push/email)
- `NotificationRead` - Marcada como lida
- `NotificationMuted` - Usuário mutou

## Permissões
- `notification:read` - Todas (próprias)
- `notification:mute` - Todas (próprias)
- `notification:preferences` - Todas (próprias)
- `notification:send` - SYSTEM

## Dependências
- **Events** - Eventos que geram notificações
- **Users** - Destinatários
- **Email** - Envio de digest

## Fluxo Resumido
1. Evento do domínio dispara → notificação criada
2. Agregador verifica duplicatas (5min) → agrupa ou cria nova
3. Envia via WebSocket (in-app) + push + agenda digest email

## Checklist de Implementação
- [ ] 3 tipos: in-app, push, email digest
- [ ] Agrupamento 1/tipo/5min
- [ ] Mute 1h/8h/24h
- [ ] WebSocket real-time
- [ ] Retenção 30 dias
- [ ] Preferências por usuário
- [ ] Contagem de não lidas
- [ ] Push registration (web/mobile)

## Checklist de Testes
- [ ] Agrupamento funciona (2 msgs iguais em 5min = 1 notificação)
- [ ] Mute impede notificações por período
- [ ] WebSocket entrega em tempo real
- [ ] Cleanup remove notificações > 30 dias
- [ ] Preferências respeitadas

## Documentação Oficial Relacionada
- `docs/notification/NOTIFICATION-SYSTEM.md`
- `docs/notification/PUSH-SETUP.md`
- `docs/notification/EMAIL-DIGEST.md`

## Histórico de Revisão
| Data | Autor | Descrição |
|------|-------|-----------|
| 2026-07-15 | System | Criação do contexto |
