# Email — Integração Email

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Responsabilidades](#responsabilidades)
- [Configuração](#configuração)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar a integração com email (envio e recebimento).

## Descrição

Email é usado para comunicação assíncrona, notificações, campanhas e templates. Suporta SMTP para envio e IMAP/POP3 para recebimento.

## Responsabilidades

- Enviar emails transacionais (notificações)
- Enviar emails de campanha
- Receber emails (inbound)
- Rastrear aberturas e cliques
- Gerenciar bounce e spam

## Configuração

```properties
email.smtp.host=${SMTP_HOST}
email.smtp.port=587
email.smtp.username=${SMTP_USER}
email.smtp.password=${SMTP_PASSWORD}
email.from=noreply@crm.com
email.reply-to=support@crm.com
```

## Dependências

- [01-backend/Messages.md](../01-backend/Messages.md) — Envio
- [01-backend/Campaigns.md](../01-backend/Campaigns.md) — Campanhas
- [01-backend/Notifications.md](../01-backend/Notifications.md) — Notificações

## Regras

- Emails transacionais usam templates
- Rate limit: 100 emails/hora por empresa
- Bounce é rastreado e contato é marcado
- Unsubscribe link é obrigatório
- SPF/DKIM configurados para domínio

## Futuras Melhorias

- Email tracking (abertura, cliques)
- A/B testing de subject lines
- Email builder visual
- Integração com SendGrid/Mailgun
- Email scheduling

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
