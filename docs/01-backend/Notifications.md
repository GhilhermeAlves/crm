# Notifications — Sistema de Notificações

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Responsabilidades](#responsabilidades)
- [Fluxo](#fluxo)
- [Endpoints](#endpoints)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar o sistema de notificações internas do CRM.

## Descrição

O sistema de notificações mantém os usuários informados sobre eventos relevantes: novas mensagens, atribuições, SLAs vencidos, automações executadas, etc. Suporta notificações in-app, push e email.

## Responsabilidades

- Enviar notificações in-app em tempo real
- Enviar notificações push (browser/mobile)
- Enviar notificações por email (resumo diário)
- Gerenciar preferências de notificação por usuário
- Controlar frequência e agrupamento

## Tipos de Notificação

| Tipo | Canal | Exemplo |
|---|---|---|
| Nova mensagem | In-app + Push | "Nova mensagem de João" |
| Lead atribuído | In-app + Push | "Lead atribuído a você" |
| Oportunidade movida | In-app | "Oportunidade avançou para Negociação" |
| SLA vencido | In-app + Email | "Conversa aberta há 2h sem resposta" |
| Automação executada | In-app | "Automação 'Boas-vindas' executada" |
| Relatório pronto | In-app + Email | "Relatório semanal disponível" |

## Fluxo

```
1. Evento ocorre no sistema
        │
2. Backend identifica usuários a notificar
        │
3. Backend verifica preferências de notificação
        │
4. Notificação é criada
        │
5. WebSocket envia in-app notification
        │
6. Push notification é enviada (se habilitada)
        │
7. Email é enfileirado (se habilitado, modo resumo)
```

## Endpoints

| Método | Endpoint | Descrição | Permissão |
|---|---|---|---|
| GET | `/api/v1/notifications` | Listar notificações | Autenticado |
| PUT | `/api/v1/notifications/{id}/read` | Marcar como lida | Autenticado |
| PUT | `/api/v1/notifications/read-all` | Marcar todas como lidas | Autenticado |
| GET | `/api/v1/notifications/preferences` | Preferências | Autenticado |
| PUT | `/api/v1/notifications/preferences` | Atualizar preferências | Autenticado |
| WS | `/ws/notifications` | WebSocket para tempo real | Autenticado |

## Dependências

- [Events.md](./Events.md) — Eventos que geram notificações
- [Users.md](./Users.md) — Preferências por usuário
- [04-integrations/Email.md](../04-integrations/Email.md) — Envio de email

## Regras

- Notificações são agrupadas (máx 1 por tipo por 5 min)
- Notificações antigas (30 dias) são arquivadas
- Push notification só para eventos críticos
- Email resumo: máximo 1 por dia (configurável)
- Usuário pode silenciar por 1h, 8h ou 24h
- Notificações marcadas como lidas são mantidas por 30 dias

## Futuras Melhorias

- Notificações com ações inline (responder, atribuir)
- Modo "não perturbe" com horários
- Notificações por WhatsApp (canal próprio)
- Resumo semanal via email
- Métricas de engajamento com notificações

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
