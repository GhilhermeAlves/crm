# Chat — Sistema de Chat

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

Documentar o sistema de chat integrado, incluindo conversas em tempo real, integração com WhatsApp e múltiplos canais.

## Descrição

O chat é o coração da experiência do usuário. Ele centraliza todas as conversas com contatos (leads, clientes) em uma interface unificada. Suporta WhatsApp, email, SMS e chat do site — tudo em um único lugar.

## Responsabilidades

- Exibir todas as conversas ativas em tempo real
- Enviar e receber mensagens via múltiplos canais
- Notificar novas mensagens em tempo real
- Gerenciar status da conversa (aberta, pendente, fechada)
- Suportar anexos, emojis e formatação
- Transferir conversas entre agentes

## Fluxo

### Nova Conversa

```
1. Mensagem recebida de contato (WhatsApp/email/etc)
        │
2. Backend identifica contato e empresa
        │
3. Conversa é criada ou reaberta
        │
4. Mensagem é armazenada
        │
5. WebSocket notifica agentes
        │
6. Conversa aparece na lista do agente
```

### Enviar Mensagem

```
1. Agente digita e envia mensagem
        │
2. Frontend envia via WebSocket/HTTP
        │
3. Backend valida e armazena mensagem
        │
4. Backend envia para canal externo (WhatsApp API)
        │
5. Status é atualizado (sent → delivered → read)
        │
6. WebSocket confirma para o agente
```

## Endpoints

| Método | Endpoint | Descrição | Permissão |
|---|---|---|---|
| GET | `/api/v1/chat/conversations` | Listar conversas | `chat:read` |
| GET | `/api/v1/chat/conversations/{id}` | Detalhes da conversa | `chat:read` |
| POST | `/api/v1/chat/conversations/{id}/messages` | Enviar mensagem | `chat:write` |
| POST | `/api/v1/chat/conversations/{id}/transfer` | Transferir | `chat:transfer` |
| POST | `/api/v1/chat/conversations/{id}/close` | Fechar conversa | `chat:write` |
| POST | `/api/v1/chat/conversations/{id}/reopen` | Reabrir conversa | `chat:write` |
| WS | `/ws/chat` | WebSocket para tempo real | Autenticado |

## Dependências

- [Conversations.md](./Conversations.md) — Gestão de conversas
- [Messages.md](./Messages.md) — Gestão de mensagens
- [04-integrations/WhatsApp.md](../04-integrations/WhatsApp.md) — Gateway WhatsApp
- [04-integrations/Email.md](../04-integrations/Email.md) — Gateway Email

## Regras

- Cada conversa é atribuída a um agente por vez
- Mensagens são entregues em ordem cronológica
- Anexos máximos: 10MB por arquivo
- Formatos aceitos: imagens, documentos, áudio, vídeo
- Typing indicator é enviado via WebSocket
- Status de leitura é rastreado (sent, delivered, read)
- Conversa é fechada automaticamente após 24h sem atividade (configurável)

## Futuras Melhorias

- Chatbot com IA para primeiro atendimento
- Respostas rápidas (quick replies)
- Templates de mensagem inline
- Surveys de satisfação ao fechar conversa
- Gravação de chamadas de voz/vídeo
- Screen sharing
- Chat interno entre agentes
- Sentiment analysis em tempo real

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
