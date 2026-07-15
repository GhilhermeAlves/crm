# EvolutionAPI — Evolution API

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Responsabilidades](#responsabilidades)
- [Fluxo](#fluxo)
- [Configuração](#configuração)
- [Endpoints (Evolution API)](#endpoints---evolution-api)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar a integração com Evolution API como gateway WhatsApp.

## Descrição

Evolution API é um gateway open source para WhatsApp Business API. Oferece gerenciamento de instâncias, envio/recebimento de mensagens e webhooks.

## Responsabilidades

- Gerenciar instâncias WhatsApp
- Enviar mensagens via REST API
- Receber mensagens via webhook
- Gerenciar connection status
- Processar mídia (upload/download)

## Fluxo

### Setup

```
1. Instância é criada na Evolution API
        │
2. QR Code é gerado
        │
3. Usuário escaneia QR Code
        │
4. Instância fica conectada
        │
5. Webhooks são configurados
```

## Configuração

```properties
evolution.api.url=http://localhost:8080
evolution.api.key=${EVOLUTION_API_KEY}
evolution.instance.name=crm-main
evolution.instance.number=5511999999999
```

## Endpoints (Evolution API)

| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/instance/create` | Criar instância |
| GET | `/instance/fetchInstances` | Listar instâncias |
| DELETE | `/instance/delete/{instance}` | Deletar instância |
| POST | `/message/sendText/{instance}` | Enviar texto |
| POST | `/message/sendMedia/{instance}` | Enviar mídia |
| GET | `/chat/findMessages/{instance}` | Buscar mensagens |

## Dependências

- [WhatsApp.md](./WhatsApp.md) — Visão geral
- [01-backend/Messages.md](../01-backend/Messages.md) — Mensagens

## Regras

- QR Code expira em 30 segundos
- Reconexão automática em caso de desconexão
- Máximo de 1 instância por empresa (MVP)
- Webhook deve ser validado
- Retry em caso de falha de envio

## Futuras Melhorias

- Multi-instance support
- Load balancing entre instâncias
- Monitoramento de connection status
- Auto-reconnect com fallback
- Métricas de uso por instância

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
