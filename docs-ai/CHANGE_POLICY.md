# Change Policy — Política de Atualização Obrigatória

## Objetivo

Garantir que toda mudança de código seja acompanhada de atualização documental correspondente.

## Escopo

Qualquer alteração em código, configuração ou estrutura do projeto.

## Como utilizar

Após implementar qualquer mudança, consulte esta tabela para saber quais documentos atualizar.

## Política

### Regra Fundamental

> **Toda mudança de código DEVE ser acompanhada de atualização na documentação oficial em `docs/`.**

### O que atualizar por tipo de mudança

| Tipo de Mudança | Docs Atualizados |
|-----------------|------------------|
| Novo endpoint API | `docs/01-backend/[Modulo].md` |
| Alterar endpoint existente | `docs/01-backend/[Modulo].md` |
| Nova entidade/tabela | `docs/03-database/Entities.md`, `docs/03-database/ERD.md`, `docs/03-database/Migrations.md` |
| Alterar schema existente | `docs/03-database/Entities.md`, `docs/03-database/Migrations.md` |
| Nova migration | `docs/03-database/Migrations.md` |
| Adicionar índice | `docs/03-database/Indexes.md` |
| Nova regra de negócio | `docs/05-business-rules/[Modulo].md` |
| Alterar regra existente | `docs/05-business-rules/[Modulo].md` |
| Nova rota frontend | `docs/02-frontend/Routing.md` |
| Novo componente | `docs/02-frontend/Components.md` |
| Alterar layout | `docs/02-frontend/Layout.md` |
| Novo webhook | `docs/01-backend/Webhooks.md`, `docs/04-integrations/Webhooks.md` |
| Nova integração | `docs/04-integrations/[Servico].md` |
| Alterar Docker | `docs/06-devops/Docker.md` |
| Alterar CI/CD | `docs/06-devops/CI.md`, `docs/06-devops/CD.md` |
| Novo evento | `docs/01-backend/Events.md` |
| Alterar permissão | `docs/01-backend/Permissions.md`, `docs/05-business-rules/Permissions.md` |
| Nova automação/trigger | `docs/01-backend/Automations.md`, `docs/05-business-rules/Automation.md` |
| Alterar cache strategy | `docs/01-backend/Cache.md`, `docs/CACHE_STRATEGY.md` |
| Alterar autenticação | `docs/01-backend/Auth.md` |

### Fluxo de atualização

```
1. Implementar mudança no código
2. Identificar tipo de mudança (tabela acima)
3. Atualizar TODOS os documentos listados
4. Verificar se há dependências afetadas (DEPENDENCIES_INDEX.md)
5. Atualizar documentos de módulos dependentes se necessário
6. Atualizar docs-ai/ se novos módulos ou endpoints foram adicionados
```

### O que NÃO fazer

- **NÃO** adicionar detalhes de implementação em `docs-ai/`
- **NÃO** pular a atualização de documentação
- **NÃO** duplicar conteúdo entre `docs/` e `docs-ai/`
- **NÃO** criar novos arquivos em `docs/` sem seguir `DOCUMENTATION_POLICY.md`

### Checklist pós-implementação

- [ ] Código implementado e testado
- [ ] Docs oficiais em `docs/` atualizados
- [ ] `docs-ai/` atualizado se necessário
- [ ] Dependências verificadas
- [ ] `AI_ROUTER.md` reflete mudanças (se novo módulo/endpoint)

## Referências

- Padrões de documentação: [DOCUMENTATION_POLICY.md](DOCUMENTATION_POLICY.md)
- Dependências: [DEPENDENCIES_INDEX.md](DEPENDENCIES_INDEX.md)
- Roteador: [AI_ROUTER.md](AI_ROUTER.md)

## Histórico de Revisão

| Versão | Data | Autor | Alteração |
|--------|------|-------|-----------|
| 1.0 | 2026-07-15 | AI Agent | Criação inicial |
