# Playbook: Implementação do Módulo WhatsApp

## Objetivo
Implementar a integração com WhatsApp via Evolution API e Meta Business API: envio/recebimento de mensagens, gerenciamento de templates, webhook handling, e sincronização com o módulo de chat.

## Pré-requisitos
- Módulo Auth implementado
- Módulo Chat implementado (conversas e mensagens)
- Conta Meta Business API configurada (credenciais)
- Evolution API configurada (servidor de instância)
- Variáveis de ambiente: EVOLUTION_API_URL, EVOLUTION_API_KEY, META_ACCESS_TOKEN, META_PHONE_NUMBER_ID, META_VERIFY_TOKEN

## Documentos que DEVEM ser lidos
- `docs/WhatsApp.md`
- `docs/EvolutionAPI.md`
- `docs/Integration.md`
- `contexts/integration-context.md`

## Arquivos que poderão ser alterados
### Backend
- `packages/backend/src/infrastructure/integration/whatsapp/` — WhatsAppClient, EvolutionApiClient, MetaApiClient, WebhookHandler, TemplateManager
- `packages/backend/src/application/communication/` — SendWhatsAppMessageUseCase, SyncWhatsAppStatusUseCase
- `packages/backend/src/presentation/webhook/controller/WebhookController.ts` — Webhook endpoints
- `packages/backend/src/presentation/rest/controller/WhatsAppController.ts` — Gerenciamento de instância e templates

### Frontend
- `packages/frontend/src/components/whatsapp/` — WhatsAppConfig, WhatsAppTemplates, WhatsAppStatus
- `packages/frontend/src/hooks/useWhatsApp.ts`
- `packages/frontend/src/app/(auth)/settings/whatsapp/` — Páginas de configuração

## Arquivos proibidos
- `packages/backend/src/domain/communication/` — Entidades de comunicação não devem ser alteradas
- `packages/backend/src/presentation/websocket/` — ChatGateway não deve ser alterado
- `packages/frontend/src/components/chat/` — Chat components não devem ser alterados

## Ordem de implementação
1. Configurar EvolutionApiClient para instância/conexão
2. Configurar MetaApiClient para envio via Cloud API
3. Implementar WebhookHandler para receber mensagens e status
4. Implementar sincronização de mensagens recebidas → Conversation + Message
5. Implementar envio de mensagens via WhatsAppClient
6. Implementar TemplateManager (listar, criar, enviar templates)
7. Implementar tratamento de status de entrega (sent, delivered, read)
8. Implementar endpoints REST de configuração
9. Criar componentes de configuração no frontend
10. Integrar envio de mensagens no ChatWindow (canal WhatsApp)

## Checklist Backend
- [ ] WhatsAppClient: interface unificada para envio (abstracts Evolution + Meta)
- [ ] EvolutionApiClient: conectar instância, verificar status, desconectar
- [ ] MetaApiClient: enviar mensagem de texto, imagem, documento, template
- [ ] WebhookHandler: receber mensagens entrantes (texto, imagem, áudio, documento)
- [ ] WebhookHandler: receber atualizações de status (sent, delivered, read, failed)
- [ ] Sincronização: mensagem recebida cria/associa Message na Conversation correta
- [ ] Sincronização: se conversa não existe, cria nova Conversation com canal=whatsapp
- [ ] Envio: SendWhatsAppMessageUseCase envia + registra Message como enviada
- [ ] Status: SyncWhatsAppStatusUseCase atualiza status de entrega na Message
- [ ] TemplateManager: listar templates aprovados da Meta
- [ ] TemplateManager: enviar template com variáveis
- [ ] Validação de webhook (verificar assinatura X-Hub-Signature)
- [ ] Retry automático em caso de falha no envio (max 3 tentativas)
- [ ] Rate limiting por número de telefone
- [ ] Logs de todas as mensagens enviadas/recebidas

## Checklist Frontend
- [ ] WhatsAppConfig: página de configuração (instância, credenciais, verificar conexão)
- [ ] WhatsAppStatus: indicador de status da conexão (conectado/desconectado)
- [ ] WhatsAppTemplates: listar templates, enviar template com variáveis
- [ ] Hook useWhatsApp: connect, disconnect, getStatus, sendTemplate, listTemplates
- [ ] No ChatWindow: botão de envio via WhatsApp (quando conversa é canal whatsapp)
- [ ] Indicador de canal na ConversationList (badge whatsapp/web/email)

## Checklist Banco
- [ ] Tabela `whatsapp_instances`: id, company_id (FK), phone_number, instance_name, status, api_key, created_at, updated_at
- [ ] Tabela `whatsapp_templates`: id, company_id (FK), template_id (meta), name, language, category, status, variables (JSON), created_at
- [ ] Atualização tabela `messages`: adicionar campos delivery_status (pending/sent/delivered/read/failed), external_message_id (whatsapp message id), delivered_at, read_at
- [ ] Atualização tabela `conversations`: adicionar campo external_id (whatsapp conversation id)
- [ ] Índices: whatsapp_instances.company_id, whatsapp_templates.company_id, messages.external_message_id

## Checklist Testes
- [ ] Testes unitários: WebhookHandler (parsing de payloads Meta/Evolution)
- [ ] Testes unitários: TemplateManager (substituição de variáveis)
- [ ] Testes de integração: Envio de mensagem (mock da API)
- [ ] Testes de integração: Recebimento de mensagem cria Conversation + Message
- [ ] Testes de integração: Status de entrega atualiza Message
- [ ] Testes de integração: Webhook com payload inválido é rejeitado
- [ ] Testes de integração: Conversa não existente é criada automaticamente
- [ ] Testes E2E: Enviar mensagem → receber resposta → verificar status

## Checklist Documentação
- [ ] Atualizar `docs/WhatsApp.md` com configuração e uso
- [ ] Atualizar `docs/EvolutionAPI.md` com setup da Evolution API
- [ ] Documentar payload dos webhooks recebidos
- [ ] Documentar variáveis de ambiente necessárias
- [ ] Documentar rate limits e retry

## Checklist Final
- [ ] Conexão com WhatsApp funciona (via Evolution API)
- [ ] Mensagens são recebidas via webhook
- [ ] Mensagens são enviadas corretamente
- [ ] Status de entrega é atualizado (sent → delivered → read)
- [ ] Templates são listados e enviados
- [ ] Webhook é validado (assinatura)
- [ ] Conversas WhatsApp aparecem no Chat
- [ ] Todos os testes passam
