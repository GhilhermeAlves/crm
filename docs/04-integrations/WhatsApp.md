# WhatsApp — Integração WhatsApp

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Responsabilidades](#responsabilidades)
- [Fluxo](#fluxo)
- [Configuração](#configuração)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar a integração com WhatsApp via provedores (Evolution API, Meta Business API).

## Descrição

WhatsApp é o canal primário de comunicação do CRM. A integração suporta envio e recebimento de mensagens, mídia, templates e status de entrega.

## Responsabilidades

- Enviar mensagens de texto, mídia e templates
- Receber mensagens via webhook
- Rastrear status (sent, delivered, read)
- Gerenciar números de telefone
- Processar mensagens de template aprovadas

## Fluxo

### Envio

```
1. Agente digita mensagem
        │
2. Backend seleciona gateway (Evolution API ou Meta)
        │
3. Mensagem é enviada via API
        │
4. External ID é salvo
        │
5. Webhook de status atualiza status
```

### Recebimento

```
1. Webhook recebe mensagem do WhatsApp
        │
2. Backend valida assinatura
        │
3. Mensagem é identificada (contato + conversa)
        │
4. Mensagem é armazenada
        │
5. WebSocket notifica agente
```

## Configuração

### Evolution API

```properties
whatsapp.evolution.api-url=http://localhost:8080
whatsapp.evolution.api-key=${EVOLUTION_API_KEY}
whatsapp.evolution.instance=crm-main
```

### Meta Business API

```properties
whatsapp.meta.api-version=v17.0
whatsapp.meta.phone-number-id=${META_PHONE_NUMBER_ID}
whatsapp.meta.access-token=${META_ACCESS_TOKEN}
whatsapp.meta.verify-token=${META_VERIFY_TOKEN}
```

## Dependências

- [EvolutionAPI.md](./EvolutionAPI.md) — Detalhes da Evolution API
- [01-backend/Messages.md](../01-backend/Messages.md) — Envio de mensagens
- [01-backend/Conversations.md](../01-backend/Conversations.md) — Gestão de conversas

## WhatsApp Template Messages

### O que são

Template Messages são mensagens pré-aprovadas pela Meta necessárias para iniciar conversas com contatos que não entraram em contato primeiro (session window de 24h).

### Fluxo de Aprovação

```
1. Usuário cria template no CRM
        │
2. Template é enviado para aprovação da Meta
        │
3. Meta revisa (1-24 horas)
        │
4. Se aprovado → Template fica ativo
   Se rejeitado → Motivo da rejeição é exibido
5. Template ativo pode ser usado em campanhas
```

### Estrutura do Template

| Campo | Descrição | Obrigatório |
|---|---|---|
| Nome | Identificador único (snake_case) | Sim |
| Categoria | AUTHENTICATION, MARKETING, UTILITY | Sim |
| Idioma | Código ISO (ex: pt_BR) | Sim |
| Corpo | Texto da mensagem | Sim |
| Variáveis | `{{1}}`, `{{2}}`, etc. | Não |
| Botões | CTA, Quick Reply | Não |
| Mídia | Imagem, vídeo, documento | Não |

### Categorias

| Categoria | Uso | Exemplo |
|---|---|---|
| AUTHENTICATION | Verificação de identidade | Códigos de verificação |
| MARKETING | Promoções, ofertas | "Confira nossa oferta!" |
| UTILITY | Atualizações, lembretes | "Seu pedido foi enviado" |

### Variáveis

```
Olá {{1}}, bem-vindo à {{2}}! Seu pedido {{3}} está pronto.

Variáveis:
  {{1}} = "João" (nome do contato)
  {{2}} = "Empresa X" (nome da empresa)
  {{3}} = "#12345" (número do pedido)
```

### Botões

| Tipo | Descrição | Exemplo |
|---|---|---|
| CTA URL | Abre link | "Visite nosso site" |
| Quick Reply | Resposta rápida | "Sim", "Não", "Ajuda" |
| Phone Number | Ligar | "Ligar para suporte" |

### Rate Limits da Meta

| Limite | Valor |
|---|---|
| Template messages/minuto | 250 |
| Utility messages/dia | 100 |
| Marketing messages/dia | Configurável |
| Authentication messages/dia | Configurável |

## Regras

| # | Regra | Justificativa |
|---|---|---|
| WA-001 | Primeira mensagem deve usar template aprovado | Meta policy |
| WA-002 | Rate limits da Meta devem ser respeitados | Compliance |
| WA-003 | Webhooks devem ser validados com assinatura | Segurança |
| WA-004 | Mensagens não entregues são retried (3x) | Resiliência |
| WA-005 | Número de telefone deve estar verificado | Requisito Meta |
| WA-006 | Session window de 24h após última mensagem do contato | Meta policy |
| WA-007 | Templates rejeitados podem ser reenviados após correção | Flexibilidade |

## Futuras Melhorias

- Multi-number support
- WhatsApp Business Profile API
- Catalog API
- Payments via WhatsApp
- WhatsApp Communities
- Multi-device support

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
