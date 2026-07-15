# Webhooks — Integração Webhooks

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar a integração de webhooks inbound e outbound.

## Descrição

Webhooks permitem comunicação entre sistemas. Outbound: CRM envia eventos para sistemas externos. Inbound: sistemas externos enviam dados para o CRM.

## Responsabilidades

- Configurar webhooks outbound por evento
- Processar webhooks inbound
- Garantir entrega com retry
- Fornecer logs de entrega
- Validar assinatura

## Dependências

- [01-backend/Webhooks.md](../01-backend/Webhooks.md) — Sistema de webhooks
- [01-backend/Events.md](../01-backend/Events.md) — Eventos

## Regras

- URLs devem ser HTTPS
- Timeout: 30 segundos
- Retry: 3 tentativas
- Payload máximo: 1MB
- Assinatura via HMAC-SHA256

## Futuras Melhorias

- Webhook marketplace
- Transform de payload
- Mock server para testes
- Analytics de webhooks

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
