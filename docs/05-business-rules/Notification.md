# Notification — Regras de Notificações

## Índice

- [Objetivo](#objetivo)
- [Regras](#regras)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar regras de notificações.

## Regras

| # | Regra | Justificativa |
|---|---|---|
| N-001 | Notificações são agrupadas (máx 1 por tipo por 5 min) | Não spam |
| N-002 | Notificações antigas (30 dias) são arquivadas | Limpeza |
| N-003 | Push notification só para eventos críticos | Relevância |
| N-004 | Email resumo: máximo 1 por dia | Frequência |
| N-005 | Usuário pode silenciar: 1h, 8h, 24h | Flexibilidade |
| N-006 | Notificações marcadas como lidas são mantidas por 30 dias | Histórico |

## Responsabilidades

- Respeitar preferências do usuário
- Não spam
- Manter relevância

## Dependências

- [01-backend/Notifications.md](../01-backend/Notifications.md) — Implementação
- [02-frontend/Notifications.md](../02-frontend/Notifications.md) — UI

## Futuras Melhorias

- Notificações com ações inline
- Modo "não perturbe"
- Métricas de engajamento

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
