# Automation Context

## Resumo do Módulo
Automações com 10 triggers e 10 actions. Max 20 actions por automação, wait de até 30 dias, 100 execuções concorrentes/empresa. Execução sequencial.

## Objetivo
Automatizar workflows de CRM com triggers, ações e conditions configuráveis.

## Responsabilidades
- 10 tipos de triggers (LeadCreated, MessageReceived, etc.)
- 10 tipos de ações (SendEmail, MoveStage, etc.)
- Max 20 actions por automação
- Wait de até 30 dias entre ações
- Max 100 execuções concorrentes por empresa
- Execução sequencial (action por vez)

## Triggers (10 tipos)
LeadCreated, ContactCreated, OpportunityMoved, MessageReceived, ConversationClosed, CampaignCompleted, FormSubmitted, TagAdded, ScheduleTrigger, WebhookTrigger

## Actions (10 tipos)
SendEmail, SendWhatsApp, CreateTask, MoveStage, UpdateField, AddTag, NotifyAgent, WebhookCall, Delay, Condition

## APIs Relacionadas
- `GET /automations` - Listar automações
- `POST /automations` - Criar automação
- `GET /automations/:id` - Detalhes + execuções
- `PUT /automations/:id` - Atualizar
- `DELETE /automations/:id` - Remover
- `POST /automations/:id/test` - Testar automação
- `GET /automations/:id/executions` - Histórico de execuções

## Banco Relacionado
- `automation_rules` - Configuração da automação
- `automation_triggers` - Triggers configurados
- `automation_actions` - Actions e ordem de execução
- `automation_executions` - Histórico de execuções

## Componentes Frontend
- AutomationsList, AutomationBuilder (visual)
- TriggerSelector, ActionSelector
- ExecutionHistory, TestRunner

## Componentes Backend
- `automation` module (Controllers, Services, Domain)
- `engine` module (execução de workflows)
- `scheduler` module (triggers temporizados)
- `queue` module (execução assíncrona)

## Eventos
- `AutomationCreated/Updated` - Automação configurada
- `AutomationActivated/Deactivated` - Ativada/desativada
- `AutomationTriggered` - Trigger disparado
- `AutomationExecutionStarted` - Execução iniciada
- `AutomationExecutionCompleted` - Execução concluída
- `AutomationExecutionFailed` - Falha na execução

## Permissões
- `automation:create` - ADMIN, MANAGER
- `automation:read` - Todos
- `automation:update` - ADMIN, MANAGER
- `automation:delete` - ADMIN
- `automation:test` - ADMIN, MANAGER

## Dependências
- **Events** - Triggers baseados em eventos do domínio
- **Messages** - Actions de envio
- **Contacts** - Ações sobre contatos
- **Pipeline** - Actions de movimentação
- **Scheduler** - Triggers temporizados

## Fluxo Resumido
1. Usuário cria automação → define trigger + conditions + actions sequenciais
2. Trigger disparado → engine valida conditions → executa actions em sequência
3. Wait actions usam scheduler → max 30 dias → retoma execução

## Checklist de Implementação
- [ ] 10 triggers implementados
- [ ] 10 actions implementadas
- [ ] Max 20 actions por automação
- [ ] Wait de até 30 dias
- [ ] 100 execuções concorrentes/empresa
- [ ] Execução sequencial
- [ ] Histórico de execuções
- [ ] Modo teste

## Checklist de Testes
- [ ] Trigger dispara corretamente
- [ ] Conditions validam antes das actions
- [ ] Wait usa scheduler corretamente
- [ ] Limite 100 execuções/empresa respeitado
- [ ] Falha em action não quebra automação

## Documentação Oficial Relacionada
- `docs/automation/TRIGGERS.md`
- `docs/automation/ACTIONS.md`
- `docs/automation/EXECUTION-ENGINE.md`

## Histórico de Revisão
| Data | Autor | Descrição |
|------|-------|-----------|
| 2026-07-15 | System | Criação do contexto |
