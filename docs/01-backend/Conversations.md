# Conversations — Gestão de Conversas

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Responsabilidades](#responsabilidades)
- [Endpoints](#endpoints)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar o módulo de gestão de conversas, incluindo lifecycle, atribuição e transferência.

## Descrição

Uma conversa representa uma sessão de comunicação com um contato. Pode ser iniciada pelo contato (inbound) ou pelo agente (output). Uma conversa pode ter múltiplas mensagens e pode ser transferida entre agentes.

## Responsabilidades

- Criar e gerenciar lifecycle de conversas
- Atribuir conversas a agentes
- Transferir conversas entre agentes
- Controlar status (open, pending, closed)
- Gerenciar fila de conversas

## Lifecycle

```
Created → Open → Pending → Closed
   │       │       │         │
   │       │       │         └── Conversa finalizada
   │       │       └── Aguardando resposta do contato
   │       └── Agente atribuído, aguardando ação
   └── Mensagem recebida/criada
```

### Status

| Status | Descrição |
|---|---|
| OPEN | Conversa ativa, agente atribuído |
| PENDING | Aguardando resposta do contato |
| CLOSED | Conversa finalizada |

## Endpoints

| Método | Endpoint | Descrição | Permissão |
|---|---|---|---|
| GET | `/api/v1/conversations` | Listar conversas | `conversation:read` |
| GET | `/api/v1/conversations/{id}` | Detalhes da conversa | `conversation:read` |
| POST | `/api/v1/conversations/{id}/assign` | Atribuir agente | `conversation:assign` |
| POST | `/api/v1/conversations/{id}/transfer` | Transferir | `conversation:transfer` |
| PUT | `/api/v1/conversations/{id}/status` | Mudar status | `conversation:write` |
| GET | `/api/v1/conversations/{id}/messages` | Mensagens | `conversation:read` |
| GET | `/api/v1/conversations/queue` | Fila de conversas | `conversation:read` |

## Dependências

- [Contacts.md](./Contacts.md) — Contato da conversa
- [Messages.md](./Messages.md) — Mensagens da conversa
- [Users.md](./Users.md) — Agente atribuído

## Regras

- Uma conversa pode ter no máximo 1 agente atribuído
- Transferência preserva histórico completo
- Conversa fechada pode ser reaberta em até 7 dias
- Fila de conversas segue distribuição round-robin
- Conversas sem atividade por 24h são fechadas automaticamente
- Uma conversa está vinculada a um canal específico

## Futuras Melhorias

- Fila inteligente baseada em skill do agente
- SLA por tipo de conversa
- Automação de atribuição baseada em histórico
- Conversas internas (agent-to-agent)
- Múltiplos canais na mesma conversa

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
