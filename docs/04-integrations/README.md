# 04-Integrations — Documentação de Integrações

## Índice

| Documento | Descrição |
|---|---|
| [WhatsApp.md](./WhatsApp.md) | Integração WhatsApp |
| [EvolutionAPI.md](./EvolutionAPI.md) | Evolution API |
| [OpenAI.md](./OpenAI.md) | OpenAI API |
| [Google.md](./Google.md) | Google APIs |
| [Email.md](./Email.md) | Integração Email |
| [SMS.md](./SMS.md) | Integração SMS |
| [Payment.md](./Payment.md) | Integração de Pagamento |
| [Webhooks.md](./Webhooks.md) | Webhooks |
| [Storage.md](./Storage.md) | Armazenamento (S3) |

---

## Objetivo

Documentar todas as integrações externas do CRM SaaS Omnichannel.

## Descrição

Integrações são pontos de conexão com sistemas externos. Cada integração é documentada com fluxo, configuração, erros e retry.

## Regras

- Integrações devem ser desacopladas do domínio
- Circuit breaker para chamadas externas
- Retry com backoff exponencial
- Logs de todas as chamadas
- Timeout configurável

## Dependências

- [01-backend/Overview.md](../01-backend/Overview.md) — Visão geral do backend
- [00-core/TechStack.md](../00-core/TechStack.md) — Stack

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
