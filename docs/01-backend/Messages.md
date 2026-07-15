# Messages — Gestão de Mensagens

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

Documentar o módulo de gestão de mensagens, incluindo envio, recebimento, status e armazenamento.

## Descrição

Mensagens são a unidade básica de comunicação. Cada mensagem pertence a uma conversa e pode conter texto, mídia, templates e anexos. O sistema rastreia o status de cada mensagem (sent, delivered, read, failed).

## Responsabilidades

- Enviar mensagens via múltiplos canais
- Receber mensagens de canais externos
- Armazenar mensagens com metadados
- Rastrear status de entrega
- Suportar diferentes tipos de mensagem
- Processar mídia (upload/download)

## Tipos de Mensagem

| Tipo | Descrição |
|---|---|
| TEXT | Mensagem de texto simples |
| IMAGE | Imagem com legenda opcional |
| DOCUMENT | Documento (PDF, DOC, etc) |
| AUDIO | Mensagem de áudio |
| VIDEO | Mensagem de vídeo |
| TEMPLATE | Mensagem baseada em template |
| LOCATION | Localização geográfica |
| CONTACT | Contato compartilhado |
| STICKER | Sticker |

## Fluxo

### Envio

```
1. Agente seleciona/monta mensagem
        │
2. Backend valida mensagem (template, tamanho, etc)
        │
3. Mensagem é armazenada como PENDING
        │
4. Backend envia para canal externo (WhatsApp API)
        │
5. Status atualiza para SENT
        │
6. Webhook de delivery atualiza para DELIVERED
        │
7. Webhook de read atualiza para READ
```

### Recebimento

```
1. Webhook recebe mensagem externa
        │
2. Backend valida e identifica conversa
        │
3. Mensagem é armazenada como RECEIVED
        │
4. Conversa é criada/reaberta se necessário
        │
5. WebSocket notifica agente
```

## Endpoints

| Método | Endpoint | Descrição | Permissão |
|---|---|---|---|
| GET | `/api/v1/messages?conversation={id}` | Listar mensagens | `message:read` |
| POST | `/api/v1/messages` | Enviar mensagem | `message:write` |
| GET | `/api/v1/messages/{id}` | Detalhes da mensagem | `message:read` |
| POST | `/api/v1/messages/{id}/read` | Marcar como lida | `message:write` |
| DELETE | `/api/v1/messages/{id}` | Deletar mensagem | `message:delete` |

## Dependências

- [Conversations.md](./Conversations.md) — Conversa pai
- [Templates.md](./Templates.md) — Templates de mensagem
- [04-integrations/WhatsApp.md](../04-integrations/WhatsApp.md) — Envio via WhatsApp
- [04-integrations/Email.md](../04-integrations/Email.md) — Envio via Email
- [FileStorage.md](./FileStorage.md) — Armazenamento de mídia

## Regras

- Mensagens são imutáveis após envio
- Status só pode avançar (PENDING → SENT → DELIVERED → READ)
- Mensagens com erro podem ser reenviadas
- Limite de caracteres: 4096 (WhatsApp), ilimitado (email)
- Mídia máxima: 10MB (WhatsApp), 25MB (email)
- Mensagens são mantidas por 2 anos (configurável)
- Mensagens deletadas são soft deleted

## Futuras Melhorias

- Edição de mensagens enviadas
- Reação a mensagens (emojis)
- Citação de mensagens anteriores
- Mensagens agendadas
- Backup de mensagens em storage externo
- Pesquisa full-text em mensagens
- Tradução automática de mensagens

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
