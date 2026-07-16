# Message Context

## Resumo do Módulo
Gestão de mensagens com 9 tipos (TEXT/IMAGE/DOCUMENT/AUDIO/VIDEO/TEMPLATE/LOCATION/CONTACT/STICKER). Status de entrega PENDING→SENT→DELIVERED→READ. Imutável após envio.

## Objetivo
Registrar e gerenciar todas as mensagens trocadas em conversas.

## Responsabilidades
- 9 tipos de mensagem suportados
- Status de entrega: PENDING→SENT→DELIVERED→READ
- Imutabilidade após envio (append-only)
- Attachments: max 10MB
- Retenção: 2 anos

## Tipos
`TEXT` | `IMAGE` | `DOCUMENT` | `AUDIO` | `VIDEO` | `TEMPLATE` | `LOCATION` | `CONTACT` | `STICKER`

## Status
`PENDING` → `SENT` → `DELIVERED` → `READ`

## APIs Relacionadas
- `GET /conversations/:id/messages` - Mensagens da conversa
- `POST /conversations/:id/messages` - Enviar mensagem
- `GET /messages/:id` - Detalhes da mensagem
- `GET /messages/:id/status` - Status de entrega
- `POST /messages/:id/read` - Marcar como lida

## Banco Relacionado
- `messages` - Conteúdo, tipo, status, timestamps
- `message_attachments` - Arquivos anexados (max 10MB)

## Componentes Frontend
- MessageBubble, MessageList
- MessageInput (texto + anexos)
- MessageStatus (sent/delivered/read icons)
- AttachmentViewer

## Componentes Backend
- `message` module (Controllers, Services, Domain, Repository)
- `attachment` module (upload, storage, validation)
- `delivery` module (status tracking, webhooks)

## Eventos
- `MessageSent` - Mensagem enviada
- `MessageDelivered` - Entregue ao destinatário
- `MessageRead` - Lida pelo destinatário
- `MessageFailed` - Falha no envio
- `AttachmentUploaded` - Anexo disponível

## Permissões
- `message:create` - AGENT (próprias conversas)
- `message:read` - Todos (próprias conversas)
- `message:status` - SYSTEM (webhooks)

## Dependências
- **Conversations** - Mensagens pertencem a conversas
- **Templates** - Mensagens de template
- **WhatsApp** - Envio/recebimento via WhatsApp
- **Email** - Envio via email

## Fluxo Resumido
1. Agente/envio compõe mensagem → `POST /conversations/:id/messages`
2. Mensagem criada (PENDING) → roteador envia (WhatsApp/Email/SMS)
3. Webhooks atualizam status → SENT→DELIVERED→READ → notifica conversa

## Checklist de Implementação
- [ ] 9 tipos de mensagem suportados
- [ ] Status tracking: PENDING→SENT→DELIVERED→READ
- [ ] Imutabilidade (append-only)
- [ ] Attachments: upload, validação 10MB, storage
- [ ] Retenção 2 anos com cleanup job
- [ ] Webhooks para status updates
- [ ] Paginação de mensagens
- [ ] Search em conteúdo de mensagens

## Checklist de Testes
- [ ] Mensagem criada com status PENDING
- [ ] Status avança corretamente via webhooks
- [ ] Imutabilidade verificada (não edita após envio)
- [ ] Attachments validados (10MB max)
- [ ] Paginação funciona com muitas mensagens

## Documentação Oficial Relacionada
- `docs/message/MESSAGE-TYPES.md`
- `docs/message/DELIVERY-TRACKING.md`
- `docs/message/ATTACHMENTS.md`

## Histórico de Revisão
| Data | Autor | Descrição |
|------|-------|-----------|
| 2026-07-15 | System | Criação do contexto |
