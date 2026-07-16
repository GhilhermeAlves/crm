# Knowledge Layer for AI Agents

## Objetivo

Camada de navegação que permite a qualquer agente de IA encontrar rapidamente os documentos oficiais do projeto CRM, sem duplicar conteúdo.

## Escopo

Este diretório contém 17 arquivos de navegação que mapeiam módulos, regras, dependências e fluxos para o diretório oficial `crm/docs/`.

**NENHUM arquivo neste diretório contém detalhes de implementação.** Todos apontam para `docs/`.

## Como utilizar

1. **Leia `AI_RULES.md`** — regras permanentes para qualquer agente
2. **Consulte `AI_ROUTER.md`** — mapa de cada solicitação para os documentos necessários
3. **Siga `DECISION_TREE.md`** — árvore de decisão para determinar quais arquivos ler
4. **Execute `IMPLEMENTATION_GUIDE.md`** — fluxo oficial de implementação
5. **Consulte índices** conforme necessário: `MODULE_INDEX.md`, `SEARCH_INDEX.md`, `TASK_INDEX.md`, etc.

### Fluxo rápido

```
Solicitação → AI_ROUTER.md → documentos oficiais em docs/ → implementação → CHANGE_POLICY.md
```

### Regra de ouro

> **NUNCA leia todos os documentos.** Use o roteador para obter o conjunto mínimo necessário.

## Estrutura de arquivos

| Arquivo | Função |
|---------|--------|
| `AI_RULES.md` | Regras permanentes |
| `AI_ROUTER.md` | Roteador principal |
| `MODULE_INDEX.md` | Índice de módulos |
| `SEARCH_INDEX.md` | Índice pesquisável |
| `TASK_INDEX.md` | Índice de tarefas |
| `BACKEND_INDEX.md` | Índice backend |
| `FRONTEND_INDEX.md` | Índice frontend |
| `DATABASE_INDEX.md` | Índice banco de dados |
| `API_INDEX.md` | Índice de endpoints |
| `RULES_INDEX.md` | Índice de regras |
| `DEPENDENCIES_INDEX.md` | Índice de dependências |
| `CHANGE_POLICY.md` | Política de atualização |
| `DOCUMENTATION_POLICY.md` | Padrões de documentação |
| `PROMPT_GUIDE.md` | Guia de prompts |
| `DECISION_TREE.md` | Árvore de decisão |
| `IMPLEMENTATION_GUIDE.md` | Fluxo de implementação |

## Referências

- Documentação oficial: `crm/docs/`
- Estrutura: `crm/docs/00-core/` até `crm/docs/07-roadmap/`

## Histórico de Revisão

| Versão | Data | Autor | Alteração |
|--------|------|-------|-----------|
| 1.0 | 2026-07-15 | AI Agent | Criação inicial |
