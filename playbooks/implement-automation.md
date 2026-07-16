# Playbook: Implementação do Módulo Automation

## Objetivo
Implementar o sistema de automações: triggers (10 tipos), ações (10 tipos), filas de processamento via RabbitMQ, e gerenciamento de campanhas automatizadas.

## Pré-requisitos
- Módulo Events implementado (sistema de eventos do domínio)
- Módulo Scheduler implementado (agendamento de tarefas)
- Módulo Messages implementado (envio de mensagens)
- RabbitMQ configurado e rodando
- Filas declaradas: automation.triggers, automation.actions, automation.scheduled

## Documentos que DEVEM ser lidos
- `docs/Automations.md`
- `docs/05-business-rules/Automation.md`
- `docs/Events.md`
- `contexts/campaign-context.md`

## Arquivos que poderão ser alterados
### Backend
- `packages/backend/src/domain/campaign/` — Entidades: Automation, Trigger, Action, AutomationLog, Campaign
- `packages/backend/src/application/campaign/` — Casos de uso: CreateAutomationUseCase, UpdateAutomationUseCase, ListAutomationsUseCase, ExecuteTriggerUseCase, ProcessActionUseCase, GetAutomationLogsUseCase
- `packages/backend/src/infrastructure/queue/` — RabbitMQConnection, TriggerConsumer, ActionConsumer, ScheduledConsumer
- `packages/backend/src/infrastructure/persistence/` — AutomationRepository, AutomationLogRepository
- `packages/backend/src/presentation/rest/controller/AutomationController.ts`

### Frontend
- `packages/frontend/src/components/automations/` — AutomationList, AutomationBuilder, AutomationForm, AutomationLog, TriggerConfig, ActionConfig, AutomationToggle
- `packages/frontend/src/hooks/useAutomations.ts`
- `packages/frontend/src/app/(auth)/automations/` — Páginas: index, [id], create, edit

## Arquivos proibidos
- `packages/backend/src/domain/communication/` — Entidades de comunicação não devem ser alteradas
- `packages/backend/src/infrastructure/integration/` — Integrações externas não devem ser alteradas
- `packages/frontend/src/components/chat/` — Chat components não devem ser alterados

## Ordem de implementação
1. Definir entidades: Automation, Trigger, Action, AutomationLog
2. Definir os 10 tipos de triggers
3. Definir os 10 tipos de ações
4. Implementar repositórios de persistência
5. Implementar RabbitMQConnection e consumers
6. Implementar ExecuteTriggerUseCase (avalia condições e dispara)
7. Implementar ProcessActionUseCase (executa ação)
8. Implementar CreateAutomationUseCase (cria automação com triggers e ações)
9. Implementar ListAutomationsUseCase com filtros
10. Implementar AutomationController
11. Criar AutomationBuilder no frontend (visual builder)
12. Integrar com hook useAutomations

## Checklist Backend
- [ ] Entidade Automation: id, name, description, isActive, companyId, createdBy, createdAt, updatedAt
- [ ] Entidade Trigger: id, automationId, type, config (JSON), conditions (JSON)
- [ ] Entidade Action: id, automationId, type, config (JSON), delay (seconds), order
- [ ] Entidade AutomationLog: id, automationId, triggerId, actionId, status (success/failed/skipped), input, output, error, executedAt
- [ ] **10 Tipos de Triggers:**
  1. lead.created — Novo lead criado
  2. lead.status_changed — Status do lead mudou
  3. lead.score_reached — Lead atingiu score mínimo
  4. opportunity.created — Nova oportunidade criada
  5. opportunity.stage_changed — Oportunidade mudou de estágio
  6. opportunity.lost — Oportunidade perdida
  7. contact.created — Novo contato criado
  8. message.received — Mensagem recebida
  9. schedule.cron — Agendamento por cron expression
  10. manual — Execução manual
- [ ] **10 Tipos de Ações:**
  1. send_email — Enviar email
  2. send_whatsapp — Enviar mensagem WhatsApp
  3. send_sms — Enviar SMS
  4. update_lead_status — Atualizar status do lead
  5. update_lead_score — Modificar score do lead
  6. assign_user — Atribuir lead/oportunidade a usuário
  7. create_task — Criar tarefa
  8. add_tag — Adicionar tag ao contato
  9. webhook — Chamar webhook externo
  10. notify_user — Notificar usuário (in-app notification)
- [ ] ExecuteTriggerUseCase: recebe evento, avalia condições, publica na fila
- [ ] ProcessActionUseCase: consome da fila, executa ação, registra log
- [ ] RabbitMQConnection: conexão robusta com retry e dead letter queue
- [ ] TriggerConsumer: consome fila automation.triggers
- [ ] ActionConsumer: consome fila automation.actions
- [ ] ScheduledConsumer: processa automações agendadas (cron)
- [ ] Validação: automation não pode ter triggers duplicados
- [ ] Validação: ações são executadas na ordem definida
- [ ] Rate limiting: máximo 1000 ações/hora/empresa
- [ ] Logs completos de cada execução (input, output, erro)
- [ ] Multi-tenancy: automações filtradas por company_id

## Checklist Frontend
- [ ] AutomationList: lista de automações com toggle ativo/inativo
- [ ] AutomationBuilder: editor visual de automação (trigger → conditions → actions)
- [ ] TriggerConfig: seletor de tipo + configuração de condições
- [ ] ActionConfig: seletor de tipo + configuração de parâmetros + delay
- [ ] AutomationForm: formulário de criação/edição (nome, descrição)
- [ ] AutomationToggle: ativar/desativar automação
- [ ] AutomationLog: histórico de execuções com status, input, output, erro
- [ ] Hook useAutomations: list, get, create, update, toggle, getLogs
- [ ] Validação visual: campos obrigatórios por tipo de trigger/action
- [ ] Preview: mostrar fluxo da automação antes de salvar

## Checklist Banco
- [ ] Tabela `automations`: id, name, description, is_active, company_id (FK), created_by (FK users), created_at, updated_at
- [ ] Tabela `triggers`: id, automation_id (FK), type, config (JSONB), conditions (JSONB), created_at
- [ ] Tabela `actions`: id, automation_id (FK), type, config (JSONB), delay_seconds (integer), "order" (integer), created_at
- [ ] Tabela `automation_logs`: id, automation_id (FK), trigger_id (FK), action_id (FK), status, input (JSONB), output (JSONB), error (TEXT), executed_at
- [ ] Índices: automations.company_id, triggers.automation_id, actions.automation_id + "order", automation_logs.automation_id + executed_at
- [ ] Foreign keys com ON DELETE CASCADE para triggers, actions, logs
- [ ] Particionamento de automation_logs por data (se volume alto)

## Checklist Testes
- [ ] Testes unitários: EvaluateTriggerConditions (diversos cenários)
- [ ] Testes unitários: ProcessActionUseCase (cada tipo de ação)
- [ ] Testes de integração: Criar automação com trigger + ação
- [ ] Testes de integração: Trigger dispara corretamente (evento → fila → ação)
- [ ] Testes de integração: Ação é executada com parâmetros corretos
- [ ] Testes de integração: Log é registrado (sucesso e falha)
- [ ] Testes de integração: Ativar/desativar automação
- [ ] Testes de integração: Dead letter queue captura falhas
- [ ] Testes E2E: Criar automação → disparar trigger → verificar ação executada → verificar log

## Checklist Documentação
- [ ] Atualizar `docs/Automations.md` com tipos de triggers, ações, endpoints
- [ ] Atualizar `docs/05-business-rules/Automation.md` com regras
- [ ] Documentar payload de cada tipo de trigger
- [ ] Documentar configuração de cada tipo de ação
- [ ] Documentar fila RabbitMQ e retry policy

## Checklist Final
- [ ] Automações são criadas com triggers e ações
- [ ] Triggers disparam corretamente quando evento ocorre
- [ ] Ações são executadas via fila (RabbitMQ)
- [ ] Logs de execução são registrados
- [ ] Ativar/desativar automação funciona
- [ ] Delay entre ações funciona
- [ ] Dead letter queue captura falhas
- [ ] Multi-tenancy isola automações por empresa
- [ ] Rate limiting funciona
- [ ] Todos os testes passam
