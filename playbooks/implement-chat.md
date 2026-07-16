# Playbook: Implementação do Módulo Chat

## Objetivo
Implementar o sistema de chat em tempo real: conversas, mensagens, WebSocket para atualizações ao vivo, integração com canais (WhatsApp, web chat, etc.).

## Pré-requisitos
- Módulo Auth implementado (usuários autenticados)
- Módulo WhatsApp implementado (canal de comunicação)
- Módulo Contact implementado (conversas são com contatos)
- Servidor WebSocket configurado

## Documentos que DEVEM ser lidos
- `docs/Chat.md`
- `docs/Conversations.md`
- `docs/Messages.md`
- `docs/WhatsApp.md`
- `contexts/communication-context.md`

## Arquivos que poderão ser alterados
### Backend
- `packages/backend/src/domain/communication/` — Entidades: Conversation, Message, MessageAttachment, ConversationParticipant
- `packages/backend/src/application/communication/` — Casos de uso: CreateConversationUseCase, ListConversationsUseCase, SendMessageUseCase, ListMessagesUseCase, MarkAsReadUseCase, CloseConversationUseCase, AssignConversationUseCase
- `packages/backend/src/infrastructure/persistence/` — ConversationRepository, MessageRepository
- `packages/backend/src/presentation/rest/controller/ConversationController.ts`
- `packages/backend/src/presentation/rest/controller/MessageController.ts`
- `packages/backend/src/presentation/websocket/` — ChatGateway, ChatEvents

### Frontend
- `packages/frontend/src/components/chat/` — ChatWindow, MessageList, MessageInput, ConversationList, ChatHeader, TypingIndicator
- `packages/frontend/src/hooks/useConversations.ts`
- `packages/frontend/src/hooks/useMessages.ts`
- `packages/frontend/src/lib/websocket/` — WebSocket client
- `packages/frontend/src/app/(auth)/chat/` — Páginas: index, [conversationId]

## Arquivos proibidos
- `packages/backend/src/infrastructure/integration/whatsapp/` — WhatsApp integration não deve ser alterada
- `packages/backend/src/domain/company/` — Company entities não devem ser alteradas
- `packages/frontend/src/components/whatsapp/` — WhatsApp components não devem ser alterados

## Ordem de implementação
1. Definir entidades: Conversation, Message, MessageAttachment, ConversationParticipant
2. Implementar repositórios de persistência
3. Implementar casos de uso CRUD de conversas
4. Implementar casos de uso de mensagens (envio, listagem, marcação como lida)
5. Implementar WebSocket Gateway (ChatGateway) com eventos: new-message, typing, read, new-conversation
6. Implementar lógica de atribuição de conversas a atendentes
7. Implementar contadores de mensagens não lidas
8. Implementar controllers REST
9. Criar WebSocket client no frontend
10. Criar componente ConversationList (sidebar)
11. Criar componente ChatWindow (messages + input)
12. Criar componente MessageList com auto-scroll
13. Criar componente MessageInput com upload de anexos
14. Integrar com hooks useConversations e useMessages

## Checklist Backend
- [ ] Entidade Conversation: id, contactId, channel (web/whatsapp/email), status (open/closed/pending), assignedTo, companyId, lastMessageAt, unreadCount, createdAt, updatedAt
- [ ] Entidade Message: id, conversationId, senderId, senderType (user/contact/system), content, contentType (text/image/file/audio), metadata, createdAt
- [ ] Entidade MessageAttachment: id, messageId, fileName, fileUrl, fileSize, mimeType
- [ ] Entidade ConversationParticipant: id, conversationId, userId, joinedAt
- [ ] CreateConversationUseCase: cria conversa vinculada a contato
- [ ] SendMessageUseCase: cria mensagem + atualiza lastMessageAt + incrementa unreadCount
- [ ] ListConversationsUseCase: lista com filtros (status, assignedTo, channel) + ordenação por lastMessageAt
- [ ] ListMessagesUseCase: paginado com cursor (anteriores primeiro)
- [ ] MarkAsReadUseCase: zera unreadCount do usuário na conversa
- [ ] AssignConversationUseCase: atribui conversa a atendente
- [ ] CloseConversationUseCase: fecha conversa
- [ ] ChatGateway: eventos WebSocket — join-room, leave-room, send-message, typing, message-read
- [ ] ChatGateway: broadcast para participantes da sala
- [ ] ChatGateway: autenticação via token JWT
- [ ] Contadores de não lidos por conversa (por usuário)
- [ ] Multi-tenancy: conversas filtradas por company_id

## Checklist Frontend
- [ ] ConversationList: sidebar com conversas (avatar, nome, última mensagem, timestamp, badge não lidas)
- [ ] Filtros: por status (abertas, fechadas, pendentes), por canal, por responsável
- [ ] ChatWindow: header com info do contato + lista de mensagens + input
- [ ] MessageList: mensagens agrupadas por data, bolha do usuário à direita, contato à esquerda
- [ ] Auto-scroll para mensagem mais recente
- [ ] MessageInput: campo de texto + botão enviar + upload de arquivo
- [ ] TypingIndicator: indicador "digitando..." em tempo real
- [ ] WebSocket client: conecta, recebe mensagens, envia mensagens
- [ ] Reconnection automática em caso de desconexão
- [ ] Contadores de não lidos atualizam em tempo real
- [ ] Hook useConversations: list, get, create, assign, close
- [ ] Hook useMessages: list, send, markAsRead
- [ ] Notificação sonora para nova mensagem (opcional)

## Checklist Banco
- [ ] Tabela `conversations`: id, contact_id (FK), channel, status, assigned_to (FK users), company_id (FK), last_message_at, unread_count, created_at, updated_at
- [ ] Tabela `messages`: id, conversation_id (FK), sender_id, sender_type, content, content_type, metadata (JSON), created_at
- [ ] Tabela `message_attachments`: id, message_id (FK), file_name, file_url, file_size, mime_type
- [ ] Tabela `conversation_participants`: id, conversation_id (FK), user_id (FK), joined_at
- [ ] Índices: conversations.company_id, conversations.status, conversations.assigned_to, messages.conversation_id + created_at
- [ ] Foreign keys com ON DELETE CASCADE

## Checklist Testes
- [ ] Testes unitários: CreateConversationUseCase
- [ ] Testes unitários: SendMessageUseCase
- [ ] Testes de integração: CRUD de conversas
- [ ] Testes de integração: Envio e recebimento de mensagens
- [ ] Testes de integração: Marcar como lido
- [ ] Testes de integração: Atribuição de conversa
- [ ] Testes de integração: WebSocket — mensagem entregue a participantes corretos
- [ ] Testes de integração: Contadores de não lidos atualizados
- [ ] Testes E2E: Abrir conversa → enviar mensagem → receber resposta → fechar

## Checklist Documentação
- [ ] Atualizar `docs/Chat.md` com endpoints e eventos WebSocket
- [ ] Atualizar `docs/Conversations.md` com fluxo de conversas
- [ ] Atualizar `docs/Messages.md` com formato e tipos
- [ ] Documentar eventos WebSocket (payload, resposta)

## Checklist Final
- [ ] Conversas são criadas e listadas corretamente
- [ ] Mensagens são enviadas e recebidas em tempo real
- [ ] WebSocket funciona com autenticação
- [ ] Typing indicator funciona
- [ ] Contadores de não lidos são precisos
- [ ] Atribuição de conversas funciona
- [ ] Multi-tenancy isola conversas por empresa
- [ ] Todos os testes passam
