# Customer Context

## Resumo do Módulo
Gestão de clientes criados automaticamente quando oportunidade é marcada como WON. Lifecycle: Active → Premium → Churned (90 dias sem interação).

## Objetivo
Gerenciar relacionamento pós-venda e métricas de retenção dos clientes.

## Responsabilidades
- Criação automática ao ganhar oportunidade
- Lifecycle: Active → Premium → Churned (90 dias inativo)
- Métricas: LTV, CAC, tempo de ciclo
- Monitoramento de engajamento e risco de churn
- Histórico de interações pós-venda

## Entidades Relacionadas
- Customer, Contact, Opportunity, Conversation

## APIs Relacionadas
- `GET /customers` - Listar clientes (filtros: status, score)
- `GET /customers/:id` - Detalhes com métricas
- `PUT /customers/:id` - Atualizar dados
- `GET /customers/:id/metrics` - LTV, CAC, atividade
- `GET /customers/churn-risk` - Clientes com risco de churn
- `POST /customers/:id/reactivate` - Reativar churned

## Banco Relacionado
- `customers` - Dados do cliente, status, LTV, CAC
- Relação com `contacts`, `opportunities`, `conversations`

## Status
- **Active** - Cliente operacional, interações recentes
- **Premium** - Alto valor, plano premium
- **Churned** - 90 dias sem interação (auto)

## Componentes Frontend
- CustomersList, CustomerDetail
- CustomerMetrics (LTV, CAC, timeline)
- ChurnRiskAlert

## Componentes Backend
- `customer` module (Controllers, Services, Domain, Repository)
- `metrics` module (LTV, CAC calculators)
- `churn` job (verificação automática de 90 dias)

## Eventos
- `CustomerCreated` - Criado automaticamente (WON)
- `CustomerUpgraded` - Movido para Premium
- `CustomerChurned` - 90 dias sem interação
- `CustomerReactivated` - Reativado manualmente
- `CustomerMetricsUpdated` - Métricas recalculadas

## Permissões
- `customer:read` - Todos
- `customer:update` - ADMIN, MANAGER
- `customer:reactivate` - ADMIN, MANAGER
- `customer:metrics` - ADMIN, MANAGER

## Dependências
- **Contacts** - Dados do cliente
- **Pipeline** - Conversão WON cria cliente
- **Conversations** - Interações contam para atividade
- **Reports** - Métricas de retenção

## Fluxo Resumido
1. Oportunidade marcada como WON → cliente criado automaticamente
2. Interações (conversas, emails) mantêm status Active
3. 90 dias sem interação → status muda para Churned → alerta enviado

## Checklist de Implementação
- [ ] Criação automática no WON
- [ ] Lifecycle: Active→Premium→Churned (90d)
- [ ] Cálculo de LTV e CAC
- [ ] Detecção de churn (cron job diário)
- [ ] Dashboard de métricas de retenção
- [ ] Reativação manual
- [ ] Integração com conversations para atividade
- [ ] Notificação de churn risk

## Checklist de Testes
- [ ] Cliente criado automaticamente ao WON
- [ ] Churned ativado após 90 dias inativos
- [ ] LTV calculado corretamente
- [ ] Reativação funciona
- [ ] Métricas atualizam com novas interações

## Documentação Oficial Relacionada
- `docs/customer/CUSTOMER-LIFECYCLE.md`
- `docs/customer/METRICS.md`
- `docs/customer/CHURN-DETECTION.md`

## Histórico de Revisão
| Data | Autor | Descrição |
|------|-------|-----------|
| 2026-07-15 | System | Criação do contexto |
