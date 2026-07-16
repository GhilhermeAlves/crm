# WhatsApp Context

## Resumo do Módulo
Integração com WhatsApp via Evolution API + Meta Business API. Janela de 24h para sessão. Template messages para outbound. Rate limit: 250 templates/min.

## Objetivo
Gerenciar comunicação bidirecional via WhatsApp com conformidade às regras da Meta.

## Responsabilidades
- Conexão via QR Code (Evolution API)
- Envio/recebimento de mensagens
- Janela de sessão de 24h (após templates)
- Template messages para mensagens proativas
- Rate limiting: 250 templates/minuto

## Regras Meta
- **24h window** - Após última mensagem do contato, só pode enviar com template
- **Templates** - Pré-aprovados pela Meta para mensagens proativas
- **Rate limit** - 250 templates/min por número

## APIs Relacionadas
- `POST /whatsapp/connect` - Gerar QR Code
- `GET /whatsapp/status` - Status da conexão
- `POST /whatsapp/send` - Enviar mensagem
- `POST /whatsapp/send-template` - Enviar template
- `GET /whatsapp/templates` - Listar templates
- `POST /whatsapp/webhook` - Webhook da Meta/Evolution
- `DELETE /whatsapp/disconnect` - Desconectar número

## Banco Relacionado
- `message_templates` - Templates pré-aprovados
- Integra com `messages` (status de entrega)

## Componentes Frontend
- WhatsAppConnect (QR Code scanner)
- WhatsAppStatus (conexao online/offline)
- TemplateManager (CRUD de templates)
- MessageComposer (seleção de template)

## Componentes Backend
- `whatsapp` module (Controllers, Services, Domain)
- `evolution-api` client (conexão, envio)
- `meta-business` client (templates, webhooks)
- `webhook` handler (status updates)
- `rate-limiter` (250 templates/min)

## Eventos
- `WhatsAppConnected` - Conexão estabelecida
- `WhatsAppDisconnected` - Conexão perdida
- `WhatsAppMessageSent` - Mensagem enviada
- `WhatsAppMessageReceived` - Mensagem recebida
- `WhatsAppMessageDelivered` - Entregue (webhook)
- `WhatsAppTemplateApproved` - Template aprovado

## Permissões
- `whatsapp:connect` - ADMIN
- `whatsapp:send` - AGENT (próprias conversas)
- `whatsapp:templates` - ADMIN, MANAGER
- `whatsapp:webhook` - SYSTEM

## Dependências
- **Messages** - Mensagens enviadas/recebidas
- **Companies** - Configuração por empresa
- **Templates** - Templates de mensagem

## Fluxo Resumido
1. Admin conecta número via QR Code → Evolution API → WebSocket ativo
2. Mensagem recebida → webhook processa → cria/atualiza conversa + mensagem
3. Agente responde → envia via Evolution API → webhooks atualizam status

## Checklist de Implementação
- [ ] Conexão via QR Code (Evolution API)
- [ ] Envio/recebimento bidirecional
- [ ] Janela 24h com templates proativos
- [ ] Rate limiting 250 templates/min
- [ ] Webhook handler para status
- [ ] Reconexão automática
- [ ] Templates Management (CRUD)
- [ ] Suporte a 9 tipos de mídia

## Checklist de Testes
- [ ] QR Code conecta corretamente
- [ ] Mensagem enviada e recebida
- [ ] Rate limit respeitado (250/min)
- [ ] Webhook processa status corretamente
- [ ] Reconexão automática funciona

## Documentação Oficial Relacionada
- `docs/whatsapp/EVOLUTION-API-SETUP.md`
- `docs/whatsapp/META-TEMPLATES.md`
- `docs/whatsapp/WEBHOOK-HANDLER.md`

## Histórico de Revisão
| Data | Autor | Descrição |
|------|-------|-----------|
| 2026-07-15 | System | Criação do contexto |
