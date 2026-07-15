# Automation — Regras de Automações

## Índice

- [Objetivo](#objetivo)
- [Regras de Criação](#regras-de-criação)
- [Regras de Execução](#regras-de-execução)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar regras de negócio de automações.

## Regras de Criação

| # | Regra | Justificativa |
|---|---|---|
| A-001 | Automação deve ter pelo menos 1 trigger | Gatilho obrigatório |
| A-002 | Automação deve ter pelo menos 1 action | Ação obrigatória |
| A-003 | Máximo 20 actions por automação | Complexidade |
| A-004 | Wait máximo: 30 dias | Limite |

## Regras de Execução

| # | Regra | Justificativa |
|---|---|---|
| A-010 | Actions são executadas em sequência | Ordem |
| A-011 | Automação não pode entrar em loop | Prevenir loops |
| A-012 | Máximo 100 execuções simultâneas por empresa | Performance |
| A-013 | Erros são retried 3 vezes | Resiliência |
| A-014 | Automação desativada não perde execuções em andamento | Consistência |

## Responsabilidades

- Garantir execução confiável
- Prevenir loops infinitos
- Monitorar performance

## Dependências

- [01-backend/Automations.md](../01-backend/Automations.md) — Implementação
- [01-backend/Events.md](../01-backend/Events.md) — Eventos

## Futuras Melhorias

- Editor visual
- IA para sugestões
- Branches condicionais
- Debug mode

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
