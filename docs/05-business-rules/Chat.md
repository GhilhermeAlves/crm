# Chat — Regras de Chat

## Índice

- [Objetivo](#objetivo)
- [Regras de Conversa](#regras-de-conversa)
- [Regras de Mensagem](#regras-de-mensagem)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar regras de negócio de chat.

## Regras de Conversa

| # | Regra | Justificativa |
|---|---|---|
| CH-001 | Conversa pode ter no máximo 1 agente | Foco |
| CH-002 | Conversa fechada pode ser reaberta em 7 dias | Flexibilidade |
| CH-003 | Conversa sem atividade por 24h é fechada | Limpeza |
| CH-004 | Fila segue round-robin | Equilíbrio |

## Regras de Mensagem

| # | Regra | Justificativa |
|---|---|---|
| CH-010 | Mensagens são imutáveis | Integridade |
| CH-011 | Status só avança (PENDING → SENT → DELIVERED → READ) | Fluxo |
| CH-012 | Anexos máximos: 10MB | Limite |
| CH-013 | Formatos: imagens, docs, áudio, vídeo | Suporte |
| CH-014 | Mensagens mantidas por 2 anos | Retenção |

## Responsabilidades

- Garantir entrega de mensagens
- Manter histórico completo
- Respeitar rate limits

## Dependências

- [01-backend/Chat.md](../01-backend/Chat.md) — Implementação
- [01-backend/Messages.md](../01-backend/Messages.md) — Mensagens
- [04-integrations/WhatsApp.md](../04-integrations/WhatsApp.md) — WhatsApp

## Futuras Melhorias

- Chatbot com IA
- Respostas rápidas
- Sentiment analysis
- Surveys de satisfação

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
