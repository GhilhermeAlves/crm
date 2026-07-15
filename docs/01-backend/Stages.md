# Stages — Estágios do Pipeline

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Responsabilidades](#responsabilidades)
- [Endpoints](#endpoints)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar os estágios do pipeline de vendas, incluindo configuração, ordem e regras de transição.

## Descrição

Estágios são as fases por uma oportunidade passa até o fechamento. Cada pipeline possui seus próprios estágios, ordenados sequencialmente. Estágios definem a probabilidade de fechamento e ações automáticas.

## Responsabilidades

- Criar e gerenciar estágios de cada pipeline
- Definir ordem e probabilidade de cada estágio
- Configurar ações automáticas ao entrar/sair de um estágio
- Validar transições permitidas

## Estágios Padrão

| Ordem | Estágio | Probabilidade | Ação Automática |
|---|---|---|---|
| 1 | Novo Lead | 10% | Notificar agente |
| 2 | Contato Inicial | 20% | — |
| 3 | Qualificação | 40% | — |
| 4 | Proposta Enviada | 60% | Template de proposta |
| 5 | Negociação | 80% | — |
| 6 | Fechamento | 95% | — |

## Endpoints

| Método | Endpoint | Descrição | Permissão |
|---|---|---|---|
| GET | `/api/v1/pipelines/{id}/stages` | Listar estágios | `pipeline:read` |
| POST | `/api/v1/pipelines/{id}/stages` | Criar estágio | `pipeline:write` |
| PUT | `/api/v1/stages/{id}` | Atualizar estágio | `pipeline:write` |
| DELETE | `/api/v1/stages/{id}` | Remover estágio | `pipeline:write` |
| PUT | `/api/v1/pipelines/{id}/stages/reorder` | Reordenar estágios | `pipeline:write` |

## Dependências

- [Pipeline.md](./Pipeline.md) — Pipeline ao qual pertence
- [05-business-rules/Pipeline.md](../05-business-rules/Pipeline.md) — Regras de negócio

## Regras

- Pipeline deve ter no mínimo 2 e máximo 15 estágios
- Estágio não pode ser removido se tem oportunidades ativas
- Probabilidade é definida por estágio (0% a 100%)
- Reordenação afeta todos os estágios subsequentes
- Primeiro estágio é sempre "Novo Lead"
- Último estágio é sempre "Ganho" ou "Perdido" (configurável)
- Ações automáticas são executadas via eventos

## Futuras Melhorias

- Gates de aprovação em estágios específicos
- SLA por estágio (tempo máximo)
- Ações automáticas customizáveis
- Notificações ao atingir SLA
- Bloqueio de retrocesso em estágios específicos

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
