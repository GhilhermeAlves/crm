# Regras Permanentes para Agentes de IA

## Objetivo

Definir regras inquebráveis que todo agente de IA deve seguir ao trabalhar neste projeto.

## Escopo

Aplicável a qualquer sessão de IA que leia, modifique ou crie código neste repositório.

## Como utilizar

Leia este arquivo **antes de qualquer outra ação**. Estas regras têm prioridade máxima.

## Regras

### R1 — Nunca ler todos os documentos
- **Sempre** consulte `AI_ROUTER.md` antes de ler qualquer documentação
- Use o conjunto mínimo de documentos necessários para a tarefa
- Se precisar de mais, expanda incrementalmente

### R2 — Consultar AI_ROUTER primeiro
- O roteador mapeia cada tipo de solicitação para os documentos exatos
- Não pule esta etapa, mesmo que ache que já sabe quais documentos ler

### R3 — Atualizar docs após qualquer mudança
- Após modificar código, **sempre** atualize a documentação oficial em `docs/`
- Siga `CHANGE_POLICY.md` para saber quais arquivos atualizar
- Nunca adicione implementação em arquivos `docs-ai/`

### R4 — Nunca duplicar conteúdo
- Arquivos `docs-ai/` são apenas navegação e referência
- O conteúdo real vive em `docs/`
- Se encontrar duplicação, remova e aponte para o original

### R5 — Seguir IMPLEMENTATION_GUIDE.md
- Todo código novo segue o fluxo oficial de implementação
- Não invente padrões; siga o que está documentado

### R6 — Verificar dependências
- Antes de modificar um módulo, consulte `DEPENDENCIES_INDEX.md`
- Verifique impactos em módulos dependentes

### R7 — Respeitar convenções
- Consulte `docs/00-core/CodingStandards.md` para estilo de código
- Consulte `docs/00-core/NamingConvention.md` para nomenclatura
- Consulte `docs/00-core/DesignPatterns.md` para padrões

## Referências

- Roteador: [AI_ROUTER.md](AI_ROUTER.md)
- Política de mudança: [CHANGE_POLICY.md](CHANGE_POLICY.md)
- Fluxo de implementação: [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md)
- Convenções: `docs/00-core/CodingStandards.md`, `docs/00-core/NamingConvention.md`

## Histórico de Revisão

| Versão | Data | Autor | Alteração |
|--------|------|-------|-----------|
| 1.0 | 2026-07-15 | AI Agent | Criação inicial |
