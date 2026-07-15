# Chat — Módulo de Chat (Frontend)

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Layout](#layout)
- [Componentes](#componentes)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar o módulo de chat, incluindo interface, componentes e funcionalidades de mensagens em tempo real.

## Descrição

O chat é a interface principal de comunicação. Mostra conversas em lista, janela de mensagens com envio/recebimento em tempo real via WebSocket, e sidebar com informações do contato.

## Layout

```
┌────────────┬──────────────────────────────┬────────────┐
│            │                              │            │
│ Conversas  │     Janela de Mensagens      │  Detalhes  │
│            │                              │  Contato   │
│ [Busca]    │  ┌──────────────────────┐    │            │
│            │  │ Mensagem do contato  │    │ Nome       │
│ Conv 1 ●   │  └──────────────────────┘    │ Email      │
│ Conv 2     │  ┌──────────────────────┐    │ Phone      │
│ Conv 3     │  │ Sua mensagem         │    │ Tags       │
│            │  └──────────────────────┘    │ Status     │
│            │                              │            │
│            │  [Campo de mensagem] [Enviar]│ [Ações]    │
└────────────┴──────────────────────────────┴────────────┘
```

## Componentes

| Componente | Descrição |
|---|---|
| ChatSidebar | Lista de conversas com busca |
| ChatWindow | Janela de mensagens |
| MessageBubble | Bolha de mensagem (in/out) |
| MessageInput | Campo de envio com anexos |
| ChatHeader | Info do contato + ações |
| TypingIndicator | Indicador de digitando |
| MessageStatus | Status (sent, delivered, read) |
| ChatActions | Transferir, fechar, etc. |

## Responsabilidades

- Exibir conversas em tempo real
- Enviar e receber mensagens via WebSocket
- Suportar anexos (imagens, documentos)
- Notificar novas mensagens
- Transferir conversas entre agentes

## Dependências

- [01-backend/Chat.md](../01-backend/Chat.md) — API de chat
- [01-backend/WebSocket](../01-backend/Chat.md) — WebSocket

## Regras

- Mensagens são exibidas em ordem cronológica
- Scroll automático para nova mensagem
- Typing indicator é exibido quando contato digita
- Status de leitura é exibido (✓✓)
- Mensagens com mais de 24h mostram data
- Campo de mensagem suporta Enter para enviar, Shift+Enter para nova linha

## Futuras Melhorias

- Respostas rápidas
- Pesquisa em mensagens
- Mensagens agendadas
- Surveys de satisfação
- Chatbot com IA
- Modo picture-in-picture

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
