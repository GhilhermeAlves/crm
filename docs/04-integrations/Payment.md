# Payment — Integração de Pagamento

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Configuração](#configuração)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar a integração com gateways de pagamento para billing do SaaS.

## Descrição

Pagamentos são processados via Stripe para assinaturas do CRM. Suporta cartão de crédito, boleto e PIX.

## Configuração

```properties
payment.provider=stripe
payment.stripe.secret-key=${STRIPE_SECRET_KEY}
payment.stripe.webhook-secret=${STRIPE_WEBHOOK_SECRET}
payment.currency=BRL
```

## Dependências

- [01-backend/Companies.md](../01-backend/Companies.md) — Planos de assinatura
- [02-frontend/Settings.md](../02-frontend/Settings.md) — Billing UI

## Regras

- Pagamentos são processados via Stripe Checkout
- Webhooks atualizam status de assinatura
- Cartão não é armazenado no sistema (tokenizado)
- Falha de pagamento bloqueia acesso após 3 dias

## Futuras Melhorias

- Múltiplos métodos de pagamento
- Notificação de pagamento via WhatsApp
- Notas fiscais automáticas
- Dashboard de receita
- Dunning management

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
