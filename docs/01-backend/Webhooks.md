# Webhooks — Sistema de Webhooks

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

Documentar o sistema de webhooks, incluindo configuração, envio e retry.

## Descrição

Webhooks permitem que sistemas externos recebam notificações em tempo real quando eventos ocorrem no CRM. Também permitem que o CRM receba notificações de sistemas externos.

## Responsabilidades

- Permitir configuração de webhooks outbound (CRM → Externo)
- Processar webhooks inbound (Externo → CRM)
- Garantir entrega com retry
- Fornecer logs de entrega
- Validar assinatura dos webhooks

## Fluxo

### Outbound

```
1. Evento ocorre no sistema
        │
2. Backend busca webhooks configurados para o evento
        │
3. Backend prepara payload com dados do evento
        │
4. Backend envia POST para a URL configurada
        │
5. Se sucesso → Registra como delivered
   Se falha → Retry com backoff exponencial
```

### Inbound

```
1. Webhook externo chega na URL do CRM
        │
2. Backend valida assinatura
        │
3. Backend processa payload
        │
4. Ação correspondente é executada
```

## Endpoints

| Método | Endpoint | Descrição | Permissão |
|---|---|---|---|
| GET | `/api/v1/webhooks` | Listar webhooks | `webhook:read` |
| POST | `/api/v1/webhooks` | Criar webhook | `webhook:write` |
| PUT | `/api/v1/webhooks/{id}` | Atualizar webhook | `webhook:write` |
| DELETE | `/api/v1/webhooks/{id}` | Deletar webhook | `webhook:delete` |
| GET | `/api/v1/webhooks/{id}/logs` | Logs de entrega | `webhook:read` |
| POST | `/api/v1/webhooks/{id}/test` | Testar webhook | `webhook:write` |
| POST | `/webhook/incoming/{token}` | Endpoint inbound | Validação |

## Eventos Disponíveis

| Evento | Descrição |
|---|---|
| `contact.created` | Contato criado |
| `contact.updated` | Contato atualizado |
| `lead.created` | Lead criado |
| `lead.qualified` | Lead qualificado |
| `message.received` | Mensagem recebida |
| `message.sent` | Mensagem enviada |
| `campaign.completed` | Campanha concluída |
| `opportunity.won` | Oportunidade ganha |
| `opportunity.lost` | Oportunidade perdida |

## Dependências

- [Events.md](./Events.md) — Eventos do sistema
- [Companies.md](./Companies.md) — Webhooks por empresa

## Regras

- Webhook URL deve ser HTTPS
- Timeout de 30 segundos
- Retry: 3 tentativas com backoff exponencial (1min, 5min, 30min)
- Payload máximo: 1MB
- Header `X-Webhook-Signature` para validação
- Logs mantidos por 30 dias
- Máximo de 20 webhooks por empresa

## Futuras Melhorias

- Webhooks com filtros (só eventos específicos)
- Webhook marketplace (integrações prontas)
- Transform de payload (customizar dados enviados)
- Mock server para testes
- Analytics de webhooks (taxa de sucesso)

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
