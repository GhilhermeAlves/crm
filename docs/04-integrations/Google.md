# Google — Integração Google APIs

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Configuração](#configuração)
- [APIs](#apis)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar integrações com Google APIs (Calendar, Contacts, OAuth).

## Descrição

Google APIs são usadas para autenticação (OAuth 2.0), sincronização de calendário e contatos.

## APIs

### Google OAuth 2.0

- Login com Google
- Consentimento de escopos
- Refresh token para acesso offline

### Google Calendar

- Criar/atualizar eventos
- Sincronizar follow-ups
- Disponibilidade de agentes

### Google Contacts

- Importar contatos
- Sincronizar dados

## Configuração

```properties
google.client-id=${GOOGLE_CLIENT_ID}
google.client-secret=${GOOGLE_CLIENT_SECRET}
google.redirect-uri=http://localhost:3000/auth/google/callback
google.scopes=calendar,contacts,profile,email
```

## Dependências

- [01-backend/Auth.md](../01-backend/Auth.md) — OAuth login
- [02-frontend/Calendar.md](../02-frontend/Calendar.md) — Calendar sync

## Regras

- Tokens são armazenados criptografados
- Escopos mínimos necessários
- Refresh token é válido por 7 dias
- Erros de API são retried (1x)

## Futuras Melhorias

- Google Sheets para import/export
- Google Drive para documentos
- Google Meet para videochamadas
- Gmail para envio de emails

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
