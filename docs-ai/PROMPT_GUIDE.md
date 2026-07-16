# Prompt Guide — Guia de Biblioteca de Prompts

## Objetivo

Explicar como usar os prompts disponíveis no diretório `prompts/` e como estruturar prompts eficazes para este projeto.

## Escopo

Prompts para geração de código, testes, documentação e tarefas comuns.

## Como utilizar

1. Identifique a tarefa desejada
2. Consulte a tabela abaixo para o prompt correto
3. Use o prompt como ponto de partida, adaptando ao contexto
4. Sempre consulte `AI_ROUTER.md` antes para obter os documentos necessários

## Prompts Disponíveis

| Prompt | Uso | Arquivo |
|--------|-----|---------|
| Code Generation | Gerar código para novo módulo | `prompts/` |
| Testing | Criar testes | `prompts/` |
| Documentation | Gerar documentação | `prompts/` |
| Code Review | Revisar código | `prompts/` |
| Bug Fix | Corrigir bugs | `prompts/` |
| Refactoring | Refatorar código | `prompts/` |

## Como Estruturar Prompts Eficazes

### Template base

```
Contexto: [Descrição da tarefa]
Módulo: [Nome do módulo]
Docs necessárias: [Consultar AI_ROUTER.md]
Restrições: [Regras específicas]
Saída esperada: [O que produzir]
```

### Exemplo: Criar novo endpoint

```
Contexto: Criar endpoint de listagem de leads qualificados
Módulo: Leads
Docs necessárias: docs/01-backend/Leads.md, docs/05-business-rules/Lead.md
Restrições: Seguir padrão CRUD existente, usar soft delete, incluir paginação
Saída esperada: Controller, Service, Repository, DTO, Testes
```

### Exemplo: Criar componente frontend

```
Contexto: Criar componente de formulário de lead
Módulo: Leads
Docs necessárias: docs/02-frontend/Forms.md, docs/02-frontend/Validation.md, docs/01-backend/Leads.md
Restrições: Usar React Hook Form, Yup validation, design system existente
Saída esperada: Componente React com validação
```

### Exemplo: Configurar automação

```
Contexto: Criar automação de follow-up pós-reunião
Módulo: Automations
Docs necessárias: docs/01-backend/Automations.md, docs/05-business-rules/Automation.md, docs/01-backend/Events.md
Restrições: Usar event bus existente, respeitar limites de rate
Saída esperada: Trigger, Action, Configuração
```

## Dicas

- **Sempre** comece consultando `AI_ROUTER.md` para obter os documentos necessários
- **Nunca** inclua mais contexto do que o necessário
- **Especifique** a saída esperada (arquivos, endpoints, componentes)
- **Mencione** restrições e convenções do projeto
- **Inclua** exemplos de código existente quando relevante

## Referências

- Prompts: `prompts/`
- Playbooks: `playbooks/`
- Roteador: [AI_ROUTER.md](AI_ROUTER.md)
- Árvore de decisão: [DECISION_TREE.md](DECISION_TREE.md)

## Histórico de Revisão

| Versão | Data | Autor | Alteração |
|--------|------|-------|-----------|
| 1.0 | 2026-07-15 | AI Agent | Criação inicial |
