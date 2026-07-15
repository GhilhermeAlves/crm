# Scheduler — Agendamento de Tarefas

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Responsabilidades](#responsabilidades)
- [Fluxo](#fluxo)
- [Tarefas Agendadas](#tarefas-agendadas)
- [Endpoints](#endpoints)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar o sistema de agendamento de tarefas (job scheduler) do backend.

## Descrição

O scheduler gerencia tarefas que devem ser executadas em horários específicos ou com frequência definida. Usa Spring Task Scheduler com suporte a distributed locking via Redis para evitar execução duplicada em múltiplas instâncias.

## Responsabilidades

- Agendar tarefas recorrentes
- Evitar execução duplicada (distributed lock)
- Monitorar execuções e falhas
- Retry automático em caso de falha
- Logs de cada execução

## Tarefas Agendadas

| Tarefa | Frequência | Descrição |
|---|---|---|
| `calculate-lead-score` | A cada 15 min | Recalcula score dos leads |
| `close-inactive-conversations` | A cada hora | Fecha conversas inativas |
| `generate-daily-report` | Diário às 02:00 | Gera relatório diário |
| `cleanup-expired-tokens` | Diário às 03:00 | Remove tokens expirados |
| `sync-whatsapp-status` | A cada 5 min | Sincroniza status de entrega |
| `send-email-digest` | Diário às 08:00 | Envia resumo por email |
| `archive-old-logs` | Semanal (dom) | Arquiva logs antigos |
| `calculate-metrics` | A cada 5 min | Calcula métricas do dashboard |

## Fluxo

```
1. Scheduler dispara tarefa
        │
2. Distributed lock é adquirido (Redis)
        │
3. Se lock adquirido → Executa tarefa
   Se não → Próxima instância executa
4. Resultado é logado
        │
5. Lock é liberado
```

## Endpoints

| Método | Endpoint | Descrição | Permissão |
|---|---|---|---|
| GET | `/api/v1/scheduler/jobs` | Listar tarefas | `scheduler:read` |
| GET | `/api/v1/scheduler/jobs/{name}` | Status da tarefa | `scheduler:read` |
| POST | `/api/v1/scheduler/jobs/{name}/trigger` | Executar manualmente | `scheduler:write` |
| GET | `/api/v1/scheduler/executions` | Histórico de execuções | `scheduler:read` |

## Dependências

- [Cache.md](./Cache.md) — Distributed lock via Redis
- [Reports.md](./Reports.md) — Geração de relatórios
- [Automations.md](./Automations.md) — Triggers de tempo

## Regras

- Tarefa deve ser idempotente
- Distributed lock com TTL de 5 minutos
- Retry: máximo 3 tentativas
- Timeout máximo por tarefa: 30 minutos
- Logging de cada início e fim de execução
- Alerta se tarefa falhar 3 vezes consecutivas

## Futuras Melhorias

- Dashboard de monitoramento de jobs
- UI para agendar tarefas manualmente
- Retry com backoff exponencial
- Fila de prioridade para jobs
- Metrics de performance por job

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
