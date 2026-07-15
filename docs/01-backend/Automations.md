# Automations — Automações

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Responsabilidades](#responsabilidades)
- [Fluxo](#fluxo)
- [Endpoints](#endpoints)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar o módulo de automações, incluindo triggers, actions e workflows.

## Descrição

Automações permitem executar ações automaticamente quando um evento ocorre. São compostas por um trigger (gatilho) e uma ou mais actions (ações). Utcom para nurturmento, follow-up, atribuição automática e processos repetitivos.

## Responsabilidades

- Criar e gerenciar automações
- Processar triggers e executar actions
- Gerenciar filas de processamento
- Rastrear execuções e erros
- Suportar conditionais (if/else)

## Componentes

### Triggers (Gatilhos)

| Trigger | Descrição |
|---|---|
| CONTACT_CREATED | Novo contato criado |
| LEAD_CREATED | Novo lead criado |
| MESSAGE_RECEIVED | Mensagem recebida |
| MESSAGE_SENT | Mensagem enviada |
| OPPORTUNITY_MOVED | Oportunidade movida |
| OPPORTUNITY_WON | Oportunidade ganha |
| OPPORTUNITY_LOST | Oportunidade perdida |
| TIME_BASED | Baseado em tempo (cron) |
| TAG_ADDED | Tag adicionada |
| FORM_SUBMITTED | Formulário preenchido |

### Actions (Ações)

| Action | Descrição |
|---|---|
| SEND_MESSAGE | Enviar mensagem |
| SEND_EMAIL | Enviar email |
| SEND_SMS | Enviar SMS |
| ADD_TAG | Adicionar tag |
| REMOVE_TAG | Remover tag |
| MOVE_OPPORTUNITY | Mover oportunidade |
| ASSIGN_TO | Atribuir a agente |
| UPDATE_FIELD | Atualizar campo |
| WAIT | Aguardar tempo |
| CONDITION | Verificar condição |

## Fluxo

```
1. Trigger é acionado (evento ou cron)
        │
2. Backend busca automações com esse trigger
        │
3. Para cada automação:
   a. Verifica condições
   b. Executa actions em sequência
   c. Registra resultado
4. Erros são logados e retried
```

## Endpoints

| Método | Endpoint | Descrição | Permissão |
|---|---|---|---|
| GET | `/api/v1/automations` | Listar automações | `automation:read` |
| POST | `/api/v1/automations` | Criar automação | `automation:write` |
| PUT | `/api/v1/automations/{id}` | Atualizar automação | `automation:write` |
| DELETE | `/api/v1/automations/{id}` | Deletar automação | `automation:delete` |
| POST | `/api/v1/automations/{id}/toggle` | Ativar/desativar | `automation:write` |
| GET | `/api/v1/automations/{id}/executions` | Histórico de execuções | `automation:read` |

## Dependências

- [Events.md](./Events.md) — Eventos como triggers
- [Messages.md](./Messages.md) — Action de envio
- [Contacts.md](./Contacts.md) — Dados do contato
- [Pipeline.md](./Pipeline.md) — Actions de pipeline
- [Scheduler.md](./Scheduler.md) — Triggers de tempo

## Condições (If/Else)

### Operadores Disponíveis

| Operador | Descrição | Exemplo |
|---|---|---|
| `equals` | Igualdade estrita | `lead.origin == "WHATSAPP"` |
| `not_equals` | Diferente | `lead.status != "DISQUALIFIED"` |
| `greater_than` | Maior que | `lead.score > 50` |
| `less_than` | Menor que | `lead.score < 20` |
| `contains` | Contém texto | `contact.name.contains("CEO")` |
| `in` | Na lista | `lead.origin in ["WHATSAPP", "FORM"]` |
| `is_empty` | Vazio | `lead.company.isEmpty()` |
| `is_not_empty` | Não vazio | `contact.email.isNotEmpty()` |
| `days_since` | Dias desde evento | `lead.created_at.days_since() > 7` |

### Estrutura de Condition

```json
{
  "type": "CONDITION",
  "config": {
    "field": "lead.score",
    "operator": "greater_than",
    "value": 50,
    "if_true": "action_send_message",
    "if_false": "action_wait_3_days"
  }
}
```

### Exemplos de Uso

**Exemplo 1:** Enviar mensagem diferente baseado no score
```
IF lead.score > 80 THEN
  → SEND_MESSAGE "Olá! Vi que você tem interesse..."
ELSE IF lead.score > 50 THEN
  → SEND_MESSAGE "Olá! Como posso ajudar?"
ELSE
  → ADD_TAG "low-priority"
```

**Exemplo 2:** Atribuir agente baseado na origem
```
IF lead.origin == "WHATSAPP" THEN
  → ASSIGN_TO "agent-whatsapp-team"
ELSE
  → ASSIGN_TO "agent-general"
```

## Regras

| # | Regra | Justificativa |
|---|---|---|
| A-001 | Automação deve ter pelo menos 1 trigger | Gatilho obrigatório |
| A-002 | Automação deve ter pelo menos 1 action | Ação obrigatória |
| A-003 | Automação pode ter múltiplos triggers (OR) | Flexibilidade |
| A-004 | Actions são executadas em sequência (não paralelo) | Ordem |
| A-005 | Wait máximo: 30 dias | Limite |
| A-006 | Máximo de 20 actions por automação | Complexidade |
| A-007 | Máximo de 5 níveis de condition aninhados | Legibilidade |
| A-008 | Automação não pode entrar em loop infinito | Prevenir loops |
| A-009 | Máximo de 100 execuções simultâneas por empresa | Performance |
| A-010 | Automação desativada não perde execuções em andamento | Consistência |
| A-011 | Erros são retried 3 vezes com backoff exponencial | Resiliência |

## Futuras Melhorias

- Editor visual de workflows (drag-and-drop)
- IA para sugerir automações
- Automações compartilhadas entre empresas
- Webhooks como action
- Conditional branches (if/else/switch)
- Variables e expressions
- Debug mode com logs detalhados

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
