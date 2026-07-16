# Conversation Context

## Resumo do Módulo
Gestão de conversas com lifecycle: Created→Open→Pending→Closed. Max 1 agente por conversa, auto-close 24h inatividade, reabertura em 7 dias.

## Objetivo
Centralizar e gerenciar todas as interações com contatos em conversas organizadas.

## Responsabilidades
- Lifecycle: Created → Open → Pending → Closed
- Atribuição: max 1 agente por conversa
- Fila com distribuição round-robin
- Auto-close após 24h de inatividade
- Reabertura permitida em até 7 dias

## Entidades Relacionadas
- Conversation, Contact, User, Message

## Status
- **Created** - Conversa criada, aguardando atribuição
- **Open** - Atribuída, ativa
- **Pending** - Aguardando resposta do contato
- **Closed** - Finalizada (auto ou manual)

## APIs Relacionadas
- `GET /conversations` - Listar (filtros: status, agente)
- `POST /conversations` - Criar conversa
- `GET /conversations/:id` - Detalhes + mensagens
- `PUT /conversations/:id/assign` - Atribuir agente
- `PUT /conversations/:id/close` - Fechar conversa
- `POST /conversations/:id/reopen` - Reabrir (7 dias)
- `PUT /conversations/:id/status` - Mudar status

## Banco Relacionado
- `conversations` - Dados da conversa, status, agente, timestamps

## Componentes Frontend
- ConversationsList, ConversationDetail
- ChatInterface (mensagens)
- AgentAssignment, StatusBadge
- QueueIndicator (fila de espera)

## Componentes Backend
- `conversation` module (Controllers, Services, Domain, Repository)
- `queue` module (round-robin dispatcher)
- `auto-close` job (cron 24h inatividade)

## Eventos
- `ConversationCreated` - Nova conversa
- `ConversationAssigned` - Agente atribuído
- `ConversationStatusChanged` - Status alterado
- `ConversationClosed` - Fechada (auto/manual)
- `ConversationReopened` - Reaberta (7 dias)
- `ConversationQueued` - Na fila de distribuição

## Permissões
- `conversation:create` - SYSTEM (automático)
- `conversation:read` - Todos (próprias + team)
- `conversation:assign` - ADMIN, MANAGER
- `conversation:close` - ADMIN, MANAGER, AGENT
- `conversation:reopen` - ADMIN, MANAGER, AGENT

## Dependências
- **Contacts** - Contato da conversa
- **Messages** - Mensagens da conversa
- **Users** - Agente atribuído
- **WhatsApp** - Canal de origem

## Fluxo Resumido
1. Mensagem recebida → conversa criada/identificada → entra na fila
2. Round-robin distribui → agente atribuído → status Open
3. Inatividade 24h → auto-close → se reaberta em 7d, reabre

## Checklist de Implementação
- [ ] Lifecycle: Created→Open→Pending→Closed
- [ ] Max 1 agente por conversa
- [ ] Round-robin queue para distribuição
- [ ] Auto-close 24h inatividade
- [ ] Reabertura em até 7 dias
- [ ] Chat interface com mensagens
- [ ] Status indicators visuais
- [ ] Filtro por status/agente

## Checklist de Testes
- [ ] Conversa criada corretamente ao receber mensagem
- [ ] Round-robin distribui igualmente
- [ ] Auto-close após 24h funciona
- [ ] Reabertura bloqueada após 7 dias
- [ ] Max 1 agente por conversa

## Documentação Oficial Relacionada
- `docs/conversation/CONVERSATION-LIFECYCLE.md`
- `docs/conversation/QUEUE-MANAGEMENT.md`
- `docs/conversation/AUTO-CLOSE.md`

## Histórico de Revisão
| Data | Autor | Descrição |
|------|-------|-----------|
| 2026-07-15 | System | Criação do contexto |
