# Implementation Guide — Fluxo Oficial de Implementação

## Objetivo

Definir o fluxo padrão que todo agente de IA deve seguir ao implementar qualquer mudança no projeto.

## Escopo

Qualquer tarefa de desenvolvimento: novo código, correção, refatoração, configuração.

## Como utilizar

Siga os passos na ordem apresentada. Cada passo é obrigatório.

## Fluxo de Implementação

### Passo 1 — Consultar AI_RULES.md

Leia as regras permanentes antes de qualquer ação.

**→ Continuar para Passo 2**

---

### Passo 2 — Consultar AI_ROUTER.md

1. Identifique o módulo da solicitação
2. Localize a tabela do módulo
3. Obtenha a lista de documentos oficiais necessários

**→ Continuar para Passo 3**

---

### Passo 3 — Seguir DECISION_TREE.md (opcional)

Se a solicitação for ambígua, use a árvore de decisão para determinar os documentos exatos.

**→ Continuar para Passo 4**

---

### Passo 4 — Ler Documentos Oficiais

Leia **apenas** os documentos identificados no Passo 2. Não leia mais do que o necessário.

Documentos típicos por tipo de tarefa:

| Tipo de Tarefa | Documentos Típicos |
|----------------|-------------------|
| Novo endpoint | `docs/01-backend/[Modulo].md` |
| Nova entidade | `docs/03-database/Entities.md`, `docs/03-database/Migrations.md` |
| Nova regra | `docs/05-business-rules/[Modulo].md` |
| Novo componente | `docs/02-frontend/Components.md`, `docs/02-frontend/Routing.md` |
| Nova integração | `docs/04-integrations/[Servico].md` |
| Configuração | `docs/06-devops/[Ferramenta].md` |

**→ Continuar para Passo 5**

---

### Passo 5 — Verificar Dependências

1. Consulte `DEPENDENCIES_INDEX.md`
2. Identifique módulos que dependem do que será modificado
3. Verifique se há impactos colaterais
4. Adicione documentos de módulos afetados se necessário

**→ Continuar para Passo 6**

---

### Passo 6 — Verificar Convenções

Consulte conforme necessário:
- `docs/00-core/CodingStandards.md` — Estilo de código
- `docs/00-core/NamingConvention.md` — Nomenclatura
- `docs/00-core/DesignPatterns.md` — Padrões
- `docs/00-core/FolderStructure.md` — Estrutura de pastas

**→ Continuar para Passo 7**

---

### Passo 7 — Implementar

1. Crie ou modifique os arquivos necessários
2. Siga as convenções e padrões documentados
3. Inclua testes quando aplicável
4. Não adicione comentários desnecessários

**→ Continuar para Passo 8**

---

### Passo 8 — Atualizar Documentação

Siga `CHANGE_POLICY.md`:
1. Atualize os documentos oficiais em `docs/`
2. Atualize `docs-ai/` se novos módulos foram adicionados
3. Verifique se o `AI_ROUTER.md` reflete as mudanças

**→ Continuar para Passo 9**

---

### Passo 9 — Verificar

1. Execute lint/typecheck se disponível
2. Verifique se os links em `docs/` estão corretos
3. Confirme que nenhum conteúdo foi duplicado entre `docs/` e `docs-ai/`

---

### Passo 10 — Concluir

A tarefa está completa quando:
- [ ] Código implementado e funcionando
- [ ] Testes passando (se aplicável)
- [ ] Documentação em `docs/` atualizada
- [ ] `docs-ai/` atualizado (se necessário)
- [ ] `CHANGE_POLICY.md` seguido
- [ ] Nenhum conteúdo duplicado

## Referências

- Regras: [AI_RULES.md](AI_ROUTER.md)
- Roteador: [AI_ROUTER.md](AI_ROUTER.md)
- Árvore de decisão: [DECISION_TREE.md](DECISION_TREE.md)
- Política de mudança: [CHANGE_POLICY.md](CHANGE_POLICY.md)
- Convenções: `docs/00-core/CodingStandards.md`

## Histórico de Revisão

| Versão | Data | Autor | Alteração |
|--------|------|-------|-----------|
| 1.0 | 2026-07-15 | AI Agent | Criação inicial |
