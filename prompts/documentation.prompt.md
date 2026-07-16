# Documentation Prompt

## Quando utilizar

- Atualizando documentação após mudanças de código
- Criando nova documentação de feature
- Atualizando CHANGELOG e relatórios

## Objetivo

Manter documentação do projeto sincronizada com o código implementado.

## Entrada esperada

- O que foi alterado (feature, bugfix, refactor)
- Arquivos modificados
- Decisões de design relevantes

## Resultado esperado

- Documentação atualizada em docs/ e docs-ai/
- CHANGELOG atualizado
- Context files atualizados

## Arquivos normalmente envolvidos

```
docs/
  └── {module}/
      └── README.md

docs-ai/
  ├── CONTEXT.md
  ├── AI_ROUTER.md
  ├── ARCHITECTURE.md
  └── {module}.md

contexts/
  └── {module}.context.md

CHANGELOG.md
IMPLEMENTATION_REPORT.md
```

## Boas práticas

- **Sempre atualizar CHANGELOG**: Adicionar entrada para cada mudança significativa.
- **Atualizar context files**: Manter `CONTEXT.md` e context files de módulos atualizados.
- **Manter IMPLEMENTATION_REPORT**: Atualizar relatório de implementação.
- **Não modificar estrutura**: Nunca alterar a estrutura existente de docs/.
- **Linguagem clara**: Documentação deve ser acessível para desenvolvedores e agents de IA.
- **Exemplos práticos**: Incluir exemplos de uso quando relevante.

## Exemplo de uso

```
Atualizar documentação após implementar módulo Categories:
- Criar docs/categories/README.md
- Atualizar docs-ai/CONTEXT.md com nova feature
- Atualizar contexts/categories.context.md
- Adicionar entrada no CHANGELOG.md
```
