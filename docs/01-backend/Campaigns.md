# Campaigns — Campanhas de Marketing

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

Documentar o módulo de campanhas de marketing, incluindo criação, segmentação, disparo e métricas.

## Descrição

Campanhas permitem enviar mensagens em massa para segmentos específicos de contatos. Suportam múltiplos canais (WhatsApp, email, SMS), agendamento e acompanhamento de métricas em tempo real.

## Responsabilidades

- Criar e gerenciar campanhas multicanal
- Segmentar público-alvo
- Agendar disparos
- Monitorar métricas em tempo real (enviados, entregues, lidos, clicados)
- Gerenciar opt-out e compliance

## Lifecycle

```
DRAFT → SCHEDULED → RUNNING → PAUSED/COMPLETED
  │        │          │           │
  │        │          │           └── Métricas finais
  │        │          └── Processando envios
  │        └── Aguardando horário de disparo
  └── Campanha em criação
```

## Endpoints

| Método | Endpoint | Descrição | Permissão |
|---|---|---|---|
| GET | `/api/v1/campaigns` | Listar campanhas | `campaign:read` |
| POST | `/api/v1/campaigns` | Criar campanha | `campaign:write` |
| PUT | `/api/v1/campaigns/{id}` | Atualizar campanha | `campaign:write` |
| POST | `/api/v1/campaigns/{id}/schedule` | Agendar | `campaign:write` |
| POST | `/api/v1/campaigns/{id}/start` | Iniciar | `campaign:write` |
| POST | `/api/v1/campaigns/{id}/pause` | Pausar | `campaign:write` |
| GET | `/api/v1/campaigns/{id}/metrics` | Métricas | `campaign:read` |
| DELETE | `/api/v1/campaigns/{id}` | Deletar campanha | `campaign:delete` |

## Dependências

- [Contacts.md](./Contacts.md) — Segmentação de contatos
- [Templates.md](./Templates.md) — Templates de mensagem
- [Messages.md](./Messages.md) — Envio de mensagens
- [Automations.md](./Automations.md) — Automações vinculadas
- [04-integrations/WhatsApp.md](../04-integrations/WhatsApp.md) — Envio WhatsApp

## Regras

- Campanha precisa de pelo menos 1 template aprovado
- Disparo em massa: máximo 100 mensagens/minuto (configurável)
- Contatos com opt-out são excluídos automaticamente
- Campanha pode ser pausada e retomada
- Métricas são atualizadas a cada 5 minutos
- WhatsApp Business API: rate limits da Meta devem ser respeitados

## Futuras Melhorias

- A/B testing automático
- IA para melhor horário de envio
- Segmentação comportamental
- Campanhas recorrentes
- Multi-canal com orçamento
- Integração com Google Analytics (UTM tracking)

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
