# SMS — Integração SMS

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

Documentar a integração com SMS via Twilio/Vonage.

## Descrição

SMS é usado como canal de comunicação alternativo para notificações urgentes, verificação e campanhas.

## Configuração

```properties
sms.provider=twilio
sms.twilio.account-sid=${TWILIO_ACCOUNT_SID}
sms.twilio.auth-token=${TWILIO_AUTH_TOKEN}
sms.twilio.from-number=+15551234567
```

## Dependências

- [01-backend/Messages.md](../01-backend/Messages.md) — Envio
- [01-backend/Campaigns.md](../01-backend/Campaigns.md) — Campanhas

## Regras

- SMS é enviado apenas para números verificados
- Rate limit: 10 SMS/minuto por número
- Custo é rastreado por empresa
-.opt-out é respeitado

## Futuras Melhorias

- WhatsApp como canal primário (SMS como fallback)
- SMS bidirecional
- Templates de SMS
- Métricas de entrega

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
